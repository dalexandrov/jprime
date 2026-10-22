package org.bgjug.jprime.admin.api;

import java.math.BigDecimal;
import java.util.List;

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
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.admin.dto.Dtos;
import org.bgjug.jprime.admin.dto.Dtos.InvoiceDto;
import org.bgjug.jprime.admin.dto.Dtos.InvoiceInput;
import org.bgjug.jprime.admin.dto.Dtos.RegistrantDto;
import org.bgjug.jprime.admin.dto.Dtos.RegistrantInput;
import org.bgjug.jprime.admin.dto.Dtos.SendResultDto;
import org.bgjug.jprime.admin.dto.Dtos.VisitorDto;
import org.bgjug.jprime.admin.dto.Dtos.VisitorInput;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.PaymentType;
import org.bgjug.jprime.core.model.Registrant;
import org.bgjug.jprime.core.model.Visitor;
import org.bgjug.jprime.core.pdf.InvoiceData;
import org.bgjug.jprime.core.pdf.InvoiceDetail;
import org.bgjug.jprime.core.repository.RegistrantRepository;
import org.bgjug.jprime.core.service.BranchService;
import org.bgjug.jprime.core.service.InvoiceService;
import org.bgjug.jprime.core.service.RegistrantService;
import org.bgjug.jprime.core.service.TicketService;
import org.bgjug.jprime.core.service.ValidationFailedException;

/** Registrants (who pays) with their visitors, invoices and ticket sending. */
@Path("/api/admin/registrants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class RegistrantAdminResource {

    @Inject
    RegistrantRepository registrantRepository;

    @Inject
    RegistrantService registrantService;

    @Inject
    BranchService branchService;

    @Inject
    InvoiceService invoiceService;

    @Inject
    TicketService ticketService;

    @GET
    @Transactional
    public Dtos.Page<RegistrantDto> list(@QueryParam("year") Integer year,
        @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("20") int size) {
        Branch branch = year != null ? branchService.findByYear(year) : branchService.getCurrentBranch();
        return Dtos.Page.of(registrantService.findByBranch(branch, page, size).map(r -> RegistrantDto.of(r, false)));
    }

    @GET
    @Path("{id}")
    @Transactional
    public RegistrantDto get(@PathParam("id") long id) {
        return RegistrantDto.of(registrant(id), true);
    }

    @POST
    @Transactional
    public RegistrantDto create(RegistrantInput input) {
        Registrant registrant = new Registrant();
        apply(registrant, input);
        registrantRepository.persist(registrant);
        return RegistrantDto.of(registrant, true);
    }

    @PUT
    @Path("{id}")
    @Transactional
    public RegistrantDto update(@PathParam("id") long id, RegistrantInput input) {
        Registrant registrant = registrant(id);
        apply(registrant, input);
        return RegistrantDto.of(registrant, true);
    }

    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") long id) {
        registrantService.delete(id);
    }

    @POST
    @Path("{id}/visitors")
    @Transactional
    public VisitorDto addVisitor(@PathParam("id") long id, VisitorInput input) {
        if (StringUtils.isBlank(input.name())) {
            throw ValidationFailedException.of("name", "The name is required");
        }
        Visitor visitor = new Visitor(input.name(), input.email(), input.company());
        visitor.setStatus(input.status());
        return VisitorDto.of(registrantService.addVisitor(id, visitor));
    }

    /** E-mails the tickets of all visitors of this registrant (background job for more than 3). */
    @POST
    @Path("{id}/send-tickets")
    @Transactional
    public SendResultDto sendTickets(@PathParam("id") long id) {
        List<Long> visitorIds = registrant(id).getVisitors().stream().map(Visitor::getId).toList();
        var result = ticketService.sendTickets(visitorIds, "Sending tickets to registrant " + registrant(id).getName());
        return new SendResultDto(result.sent(), result.failed(), result.jobId());
    }

    // ------------------------------------------------------------------ invoices

    /** Invoice data pre-filled from the registrant (for the "send original invoice" form). */
    @GET
    @Path("{id}/invoice")
    @Transactional
    public InvoiceDto invoice(@PathParam("id") long id) {
        return toDto(invoiceService.fromRegistrant(registrant(id)));
    }

    /** Preview of the original invoice PDF with the given data (nothing is stored or sent). */
    @POST
    @Path("{id}/invoice/preview")
    @Produces("application/pdf")
    @Transactional
    public Response previewInvoice(@PathParam("id") long id, InvoiceInput input) {
        Registrant registrant = registrant(id);
        InvoiceData data = merge(invoiceService.fromRegistrant(registrant), input);
        data.setInvoiceType(InvoiceData.ORIGINAL_BG);
        if (registrant.getRealInvoiceNumber() == 0) {
            data.setInvoiceNumber("PREVIEW");
        }
        return Response.ok(invoiceService.renderPdf(data, registrant.isCompany())).type("application/pdf").build();
    }

    /**
     * Marks all visitors as paid, assigns the real invoice number (if not yet assigned) and e-mails the original
     * invoice to the registrant.
     */
    @POST
    @Path("{id}/invoice/send")
    @Transactional
    public Dtos.Message sendInvoice(@PathParam("id") long id, InvoiceInput input) {
        Registrant registrant = registrant(id);
        registrantService.markPaid(registrant);
        if (registrant.getRealInvoiceNumber() == 0) {
            registrantService.generateRealInvoiceNumber(registrant);
        }
        InvoiceData data = merge(invoiceService.fromRegistrant(registrant), input);
        data.setInvoiceType(InvoiceData.ORIGINAL_BG);
        data.setInvoiceNumber(String.valueOf(registrant.getRealInvoiceNumber()));
        try {
            invoiceService.sendOriginal(registrant, data);
            return new Dtos.Message("Invoice " + data.getInvoiceNumber() + " sent to " + registrant.getEmail());
        } catch (RuntimeException e) {
            return new Dtos.Message("Visitors marked as paid (invoice " + data.getInvoiceNumber()
                + "), but the e-mail could not be sent: " + e.getMessage());
        }
    }

    private static InvoiceData merge(InvoiceData data, InvoiceInput input) {
        if (input == null) {
            return data;
        }
        data.setClient(input.client());
        data.setClientAddress(input.clientAddress());
        data.setClientEIK(input.clientEIK());
        data.setClientVAT(input.clientVAT());
        data.setMol(input.mol());
        if (StringUtils.isNotBlank(input.invoiceDate())) {
            data.setInvoiceDate(input.invoiceDate());
        }
        if (!data.getInvoiceDetails().isEmpty()) {
            InvoiceDetail detail = data.getInvoiceDetails().getFirst();
            if (input.singlePriceWithVAT() != null) {
                detail.setSinglePriceWithVAT(input.singlePriceWithVAT());
            }
            if (StringUtils.isNotBlank(input.description())) {
                detail.setDescription(input.description());
            }
        }
        return data;
    }

    private static InvoiceDto toDto(InvoiceData d) {
        InvoiceDetail detail = d.getInvoiceDetails().isEmpty() ? null : d.getInvoiceDetails().getFirst();
        return new InvoiceDto(d.getRegistrantId(), d.getInvoiceNumber(), d.getInvoiceDate(), d.getClient(),
            d.getClientAddress(), d.getClientEIK(), d.getClientVAT(), d.getMol(), d.getPaymentType(),
            detail != null ? detail.getPassQty() : 0, detail != null ? detail.getSinglePriceWithVAT() : BigDecimal.ZERO,
            detail != null ? detail.getDescription() : null, d.getTotalPrice(), d.getTotalPriceVAT(),
            d.getTotalPriceWithVAT());
    }

    private Registrant registrant(long id) {
        return AdminSupport.found(registrantRepository.findByIdOptional(id), "Registrant");
    }

    private void apply(Registrant registrant, RegistrantInput input) {
        if (StringUtils.isBlank(input.name())) {
            throw ValidationFailedException.of("name", "The name is required");
        }
        registrant.setName(input.name().trim());
        registrant.setEmail(StringUtils.trimToEmpty(input.email()));
        registrant.setCompany(input.company());
        registrant.setStudent(input.student());
        registrant.setAddress(input.address());
        registrant.setVatNumber(input.vatNumber());
        registrant.setEik(input.eik());
        registrant.setMol(input.mol());
        registrant.setPaymentType(input.paymentType() != null ? input.paymentType() : PaymentType.BANK_TRANSFER);
        registrant.setBranch(input.branchYear() != null ? branchService.findByYear(input.branchYear()) :
                             branchService.getCurrentBranch());
        if (input.realInvoiceNumber() != null) {
            registrant.setRealInvoiceNumber(input.realInvoiceNumber());
        }
        if (input.proformaInvoiceNumber() != null) {
            registrant.setProformaInvoiceNumber(input.proformaInvoiceNumber());
        }
    }
}
