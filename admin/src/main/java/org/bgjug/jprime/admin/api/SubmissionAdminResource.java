package org.bgjug.jprime.admin.api;

import java.util.Map;

import io.quarkus.panache.common.Page;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bgjug.jprime.admin.dto.Dtos;
import org.bgjug.jprime.admin.dto.Dtos.SpeakerFields;
import org.bgjug.jprime.admin.dto.Dtos.SubmissionDto;
import org.bgjug.jprime.admin.dto.Dtos.SubmissionInput;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Submission;
import org.bgjug.jprime.core.model.SubmissionStatus;
import org.bgjug.jprime.core.repository.SubmissionRepository;
import org.bgjug.jprime.core.service.BranchService;
import org.bgjug.jprime.core.service.CfpService;
import org.bgjug.jprime.core.service.CsvService;
import org.bgjug.jprime.core.service.PageResult;
import org.bgjug.jprime.core.service.SpeakerInput;

@Path("/api/admin/submissions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class SubmissionAdminResource {

    @Inject
    SubmissionRepository submissionRepository;

    @Inject
    CfpService cfpService;

    @Inject
    BranchService branchService;

    @Inject
    CsvService csvService;

    /**
     * @param year   the branch (default: current); ignored when {@code all=true}
     * @param status optional status filter
     */
    @GET
    public Dtos.Page<SubmissionDto> list(@QueryParam("year") Integer year, @QueryParam("status") SubmissionStatus status,
        @QueryParam("all") boolean all, @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size) {
        Branch branch = year != null ? branchService.findByYear(year) : branchService.getCurrentBranch();
        var query = all ? submissionRepository.everything() :
                    status != null ? submissionRepository.byBranchAndStatus(branch, status) :
                    submissionRepository.byBranch(branch);
        return Dtos.Page.of(PageResult.of(query, Page.of(page, size)).map(SubmissionDto::of));
    }

    @GET
    @Path("stats")
    public Map<SubmissionStatus, Long> stats(@QueryParam("year") Integer year) {
        Branch branch = year != null ? branchService.findByYear(year) : branchService.getCurrentBranch();
        return submissionRepository.countByStatus(branch);
    }

    @GET
    @Path("{id}")
    public SubmissionDto get(@PathParam("id") long id) {
        return SubmissionDto.of(AdminSupport.found(submissionRepository.findByIdOptional(id), "Submission"));
    }

    @POST
    public SubmissionDto create(SubmissionInput input) {
        Branch branch = input.branchYear() != null ? branchService.findByYear(input.branchYear()) :
                        branchService.getCurrentBranch();
        Submission created = cfpService.submit(toInput(input), branch);
        if (Boolean.TRUE.equals(input.featured())) {
            cfpService.update(created.getId(), toInput(input), branch, true);
        }
        return get(created.getId());
    }

    @PUT
    @Path("{id}")
    public SubmissionDto update(@PathParam("id") long id, SubmissionInput input) {
        Branch branch = input.branchYear() != null ? branchService.findByYear(input.branchYear()) : null;
        cfpService.update(id, toInput(input), branch, input.featured());
        return get(id);
    }

    @POST
    @Path("{id}/accept")
    public Dtos.Message accept(@PathParam("id") long id) {
        return new Dtos.Message(cfpService.accept(id) ? "Accepted, the speakers were notified" :
                                "Accepted, but the e-mail could not be sent");
    }

    @POST
    @Path("{id}/reject")
    public Dtos.Message reject(@PathParam("id") long id) {
        return new Dtos.Message(cfpService.reject(id) ? "Rejected, the speakers were notified" :
                                "Rejected, but the e-mail could not be sent");
    }

    @POST
    @Path("{id}/confirm")
    public SubmissionDto confirm(@PathParam("id") long id) {
        cfpService.changeStatus(id, SubmissionStatus.CONFIRMED);
        return get(id);
    }

    @POST
    @Path("{id}/cancel")
    public SubmissionDto cancel(@PathParam("id") long id) {
        cfpService.changeStatus(id, SubmissionStatus.CANCELED);
        return get(id);
    }

    @POST
    @Path("{id}/notify")
    public Dtos.Message notifySpeakers(@PathParam("id") long id) {
        return new Dtos.Message(cfpService.sendNotificationEmails(id) ? "Notification e-mails sent" :
                                "Could not send notification emails");
    }

    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") long id) {
        cfpService.delete(id);
    }

    /** SUBMITTED proposals of the current branch as CSV (for the voting). */
    @GET
    @Path("export")
    @Produces("text/csv")
    @Transactional
    public Response export() {
        var submissions = submissionRepository.findByBranchAndStatus(branchService.getCurrentBranch(),
            SubmissionStatus.SUBMITTED);
        return AdminSupport.download(csvService.exportSubmissions(submissions), "submissions.csv",
            "text/csv; charset=UTF-8");
    }

    private static org.bgjug.jprime.core.service.SubmissionInput toInput(SubmissionInput input) {
        return new org.bgjug.jprime.core.service.SubmissionInput(input.title(), input.description(), input.level(),
            input.type(), speaker(input.speaker()), speaker(input.coSpeaker()));
    }

    private static SpeakerInput speaker(SpeakerFields fields) {
        if (fields == null) {
            return null;
        }
        return new SpeakerInput(fields.firstName(), fields.lastName(), fields.email(), fields.headline(),
            fields.twitter(), fields.bsky(), fields.bio(), null);
    }
}
