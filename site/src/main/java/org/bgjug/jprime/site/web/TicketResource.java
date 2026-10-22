package org.bgjug.jprime.site.web;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.PaymentType;
import org.bgjug.jprime.core.model.Registrant;
import org.bgjug.jprime.core.model.TicketPrice;
import org.bgjug.jprime.core.model.TicketType;
import org.bgjug.jprime.core.model.Visitor;
import org.bgjug.jprime.core.model.VisitorStatus;
import org.bgjug.jprime.core.service.BranchService;
import org.bgjug.jprime.core.service.DateFormats;
import org.bgjug.jprime.core.service.InvoiceService;
import org.bgjug.jprime.core.service.RegistrantService;
import org.jboss.logging.Logger;

/** Public ticket registration: stores the registrant + visitors and mails a proforma invoice. */
@Path("/tickets")
@Produces(MediaType.TEXT_HTML)
public class TicketResource {

    private static final Logger LOG = Logger.getLogger(TicketResource.class);

    @Inject
    @Location("tickets.html")
    Template tickets;

    @Inject
    @Location("tickets-sold-out.html")
    Template soldOut;

    @Inject
    @Location("tickets-result.html")
    Template result;

    @Inject
    BranchService branchService;

    @Inject
    RegistrantService registrantService;

    @Inject
    InvoiceService invoiceService;

    @GET
    public String form() {
        return render(new TicketForm(), Map.of());
    }

    @Inject
    CaptchaService captchaService;

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String register(@BeanParam TicketForm form, @CookieParam(CaptchaService.COOKIE) String captchaCookie) {
        Branch branch = branchService.getCurrentBranch();
        if (branch.isSoldOut()) {
            return render(form, Map.of());
        }
        Map<String, String> errors = validate(form);
        if (!captchaService.verify(captchaCookie, form.captcha)) {
            errors.put("captcha", "Invalid captcha text, please enter the sum.");
        }
        if (!errors.isEmpty()) {
            return render(form, errors);
        }

        Registrant registrant = new Registrant();
        registrant.setBranch(branch);
        registrant.setCompany(form.company);
        registrant.setStudent(form.student && !form.company);
        for (TicketForm.VisitorRow row : form.getRows()) {
            if (StringUtils.isNoneBlank(row.name(), row.email())) {
                Visitor visitor = new Visitor(row.name(), row.email(), row.company());
                visitor.setStatus(VisitorStatus.REQUESTING);
                registrant.addVisitor(visitor);
            }
        }
        if (form.company) {
            registrant.setName(form.name);
            registrant.setAddress(form.address);
            registrant.setVatNumber(form.vatNumber);
            registrant.setEik(form.eik);
            registrant.setMol(form.mol);
            registrant.setEmail(form.email);
        } else {
            // a private person - the invoice goes to the first visitor
            Visitor first = registrant.getVisitors().getFirst();
            registrant.setName(first.getName());
            registrant.setEmail(first.getEmail());
        }
        registrant.setCreatedDate(LocalDateTime.now());
        registrant.setPaymentType(PaymentType.BANK_TRANSFER);

        Registrant saved = registrantService.register(registrant);
        boolean ok = true;
        try {
            invoiceService.sendProforma(saved);
        } catch (RuntimeException e) {
            // the registration is stored - the organizers will send the invoice manually
            LOG.error("Could not send the proforma invoice", e);
        }
        return result.data("result", ok).render();
    }

    static Map<String, String> validate(TicketForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        List<TicketForm.VisitorRow> rows = form.getRows();
        boolean anyVisitor = rows.stream().anyMatch(r -> StringUtils.isNoneBlank(r.name(), r.email()));
        if (!anyVisitor) {
            errors.put("visitors", "Please enter the name and e-mail of at least one visitor.");
        }
        for (TicketForm.VisitorRow row : rows) {
            if (StringUtils.isNotBlank(row.email()) && !row.email().contains("@")) {
                errors.put("visitors", "Invalid visitor e-mail: " + row.email());
            }
        }
        if (form.company) {
            if (StringUtils.isBlank(form.name)) {
                errors.put("name", "The company name is required for a VAT invoice.");
            }
            if (StringUtils.isBlank(form.email) || !form.email.contains("@")) {
                errors.put("email", "A valid e-mail is required for the invoice.");
            }
        }
        if (!form.acceptTerms) {
            errors.put("acceptTerms", "Please accept the privacy policy.");
        }
        return errors;
    }

    private String render(TicketForm form, Map<String, String> errors) {
        Branch branch = branchService.getCurrentBranch();
        if (branch.isSoldOut()) {
            int year = branch.getStartDate() != null ? branch.getStartDate().getYear() : branch.getYear();
            return soldOut.data("year", year).data("nextYear", year + 1).render();
        }
        Map<TicketType, TicketPrice> prices = branchService.getTicketPrices(branch);
        return tickets.data("form", form)
            .data("errors", errors)
            .data("earlyBirdPrice", HomeResource.price(prices, TicketType.EARLY_BIRD))
            .data("regularPrice", HomeResource.price(prices, TicketType.REGULAR))
            .data("studentPrice", HomeResource.price(prices, TicketType.STUDENT))
            .data("cfpCloseDate", DateFormats.withMonthAndYear(branch.getCfpCloseDate()))
            .render();
    }
}
