package org.bgjug.jprime.admin.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.admin.dto.Dtos;
import org.bgjug.jprime.admin.dto.Dtos.MailInput;
import org.bgjug.jprime.admin.dto.Dtos.SendResultDto;
import org.bgjug.jprime.admin.dto.Dtos.VisitorDto;
import org.bgjug.jprime.admin.dto.Dtos.VisitorInput;
import org.bgjug.jprime.admin.dto.Dtos.VisitorList;
import org.bgjug.jprime.admin.dto.Dtos.VisitorStats;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Registrant;
import org.bgjug.jprime.core.model.Visitor;
import org.bgjug.jprime.core.model.VisitorStatus;
import org.bgjug.jprime.core.model.VisitorType;
import org.bgjug.jprime.core.repository.RegistrantRepository;
import org.bgjug.jprime.core.repository.VisitorRepository;
import org.bgjug.jprime.core.service.BackgroundJobService;
import org.bgjug.jprime.core.service.BranchService;
import org.bgjug.jprime.core.service.CsvService;
import org.bgjug.jprime.core.service.MailService;
import org.bgjug.jprime.core.service.RegistrantService;
import org.bgjug.jprime.core.service.TicketService;
import org.bgjug.jprime.core.service.ValidationFailedException;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/api/admin/visitors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class VisitorAdminResource {

    @Inject
    VisitorRepository visitorRepository;

    @Inject
    RegistrantRepository registrantRepository;

    @Inject
    RegistrantService registrantService;

    @Inject
    BranchService branchService;

    @Inject
    TicketService ticketService;

    @Inject
    CsvService csvService;

    @Inject
    MailService mailService;

    @Inject
    BackgroundJobService jobService;

    @GET
    @Transactional
    public VisitorList list(@QueryParam("year") Integer year) {
        List<Visitor> visitors = visitorRepository.newestFirst(branch(year));
        VisitorStats stats = new VisitorStats(
            visitors.stream().filter(v -> v.getStatus() == VisitorStatus.REQUESTING).count(),
            visitors.stream().filter(v -> v.getStatus() == VisitorStatus.PAYED).count(),
            visitors.stream().filter(v -> v.getStatus() == VisitorStatus.SPONSORED).count(), visitors.size());
        return new VisitorList(visitors.stream().map(VisitorDto::of).toList(), stats);
    }

    @GET
    @Path("{id}")
    @Transactional
    public VisitorDto get(@PathParam("id") long id) {
        return VisitorDto.of(visitor(id));
    }

    /** A visitor without registrant gets an individual registrant (invoice to the visitor). */
    @POST
    @Transactional
    public VisitorDto create(VisitorInput input) {
        validate(input);
        Visitor visitor = new Visitor(input.name(), input.email(), input.company());
        visitor.setStatus(input.status());
        if (input.registrantId() != null) {
            return VisitorDto.of(registrantService.addVisitor(input.registrantId(), visitor));
        }
        Registrant registrant = new Registrant(input.name(), StringUtils.defaultString(input.email()),
            branchService.getCurrentBranch());
        registrant.addVisitor(visitor);
        registrantRepository.persist(registrant);
        return VisitorDto.of(visitor);
    }

    @PUT
    @Path("{id}")
    @Transactional
    public VisitorDto update(@PathParam("id") long id, VisitorInput input) {
        validate(input);
        Visitor visitor = visitor(id);
        visitor.setName(input.name());
        visitor.setEmail(input.email());
        visitor.setCompany(input.company());
        visitor.setStatus(input.status());
        return VisitorDto.of(visitor);
    }

    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") long id) {
        registrantService.deleteVisitor(id);
    }

    @POST
    @Path("{id}/send-ticket")
    @Transactional
    public SendResultDto sendTicket(@PathParam("id") long id) {
        Visitor visitor = visitor(id);
        if (visitor.getStatus() == VisitorStatus.REQUESTING) {
            throw new IllegalStateException("The visitor has not paid yet");
        }
        var result = ticketService.sendTickets(List.of(id), "Ticket for " + visitor.getName());
        return new SendResultDto(result.sent(), result.failed(), result.jobId());
    }

    /** Tickets for all paid / sponsored visitors of the current branch that have none yet. */
    @POST
    @Path("send-tickets")
    public SendResultDto sendAllTickets() {
        var result = ticketService.sendPendingTickets();
        return new SendResultDto(result.sent(), result.failed(), result.jobId());
    }

    @GET
    @Path("export")
    @Produces("text/csv")
    @Transactional
    public Response export(@QueryParam("year") Integer year) {
        return AdminSupport.download(csvService.exportVisitors(visitorRepository.newestFirst(branch(year))),
            "visitors.csv", "text/csv; charset=UTF-8");
    }

    public static class UploadForm {
        @RestForm
        public FileUpload file;
        @RestForm
        public VisitorType visitorType;
        @RestForm
        public VisitorStatus visitorStatus;
        @RestForm
        public boolean emptyBeforeUpload;
    }

    /** CSV import: jPrime registrations or the jProfessionals Eventbrite export. */
    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public CsvService.ImportResult upload(@BeanParam UploadForm form) throws IOException {
        if (form.file == null || form.file.size() == 0) {
            throw ValidationFailedException.of("file", "Please select a CSV file");
        }
        try (InputStream in = Files.newInputStream(form.file.uploadedFile())) {
            return csvService.importVisitors(in, form.visitorType != null ? form.visitorType : VisitorType.JPRIME,
                form.visitorStatus != null ? form.visitorStatus : VisitorStatus.REQUESTING, form.emptyBeforeUpload);
        }
    }

    /** Sends an e-mail to all visitors of the current branch (background job). */
    @POST
    @Path("mail")
    @Transactional
    public SendResultDto mailAll(MailInput input) {
        if (input == null || StringUtils.isAnyBlank(input.subject(), input.content())) {
            throw ValidationFailedException.of("subject", "Subject and content are required");
        }
        List<String> emails = visitorRepository.newestFirst(branchService.getCurrentBranch()).stream()
            .map(Visitor::getEmail)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        String jobId = jobService.createJob("E-mail to all visitors: " + input.subject());
        jobService.runJob(jobId, emails, email -> mailService.sendQuietly(email, input.subject(), input.content()),
            email -> email);
        return new SendResultDto(0, 0, jobId);
    }

    private Branch branch(Integer year) {
        return year != null ? branchService.findByYear(year) : branchService.getCurrentBranch();
    }

    private Visitor visitor(long id) {
        return AdminSupport.found(visitorRepository.findByIdOptional(id), "Visitor");
    }

    private static void validate(VisitorInput input) {
        if (input == null || StringUtils.isBlank(input.name())) {
            throw ValidationFailedException.of("name", "The name is required");
        }
    }
}
