package org.bgjug.jprime.core.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.PaymentType;
import org.bgjug.jprime.core.model.Registrant;
import org.bgjug.jprime.core.model.TicketPrice;
import org.bgjug.jprime.core.model.TicketType;
import org.bgjug.jprime.core.pdf.InvoiceData;
import org.bgjug.jprime.core.pdf.InvoiceDetail;
import org.bgjug.jprime.core.pdf.InvoiceExporter;
import org.bgjug.jprime.core.pdf.InvoiceLanguage;
import org.jboss.logging.Logger;

/**
 * Builds invoice data out of a registrant, renders the PDF and mails it.
 */
@ApplicationScoped
public class InvoiceService {

    private static final Logger LOG = Logger.getLogger(InvoiceService.class);
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    @Inject
    BranchService branchService;

    @Inject
    InvoiceExporter invoiceExporter;

    @Inject
    MailService mailService;

    public InvoiceData fromRegistrant(Registrant registrant) {
        InvoiceData result = new InvoiceData();
        result.setClient(registrant.getName());
        result.setClientAddress(registrant.getAddress());
        result.setClientEIK(registrant.getEik());
        result.setClientVAT(registrant.getVatNumber() != null ? registrant.getVatNumber() : "");
        result.setMol(registrant.getMol());
        result.setRegistrantId(registrant.getId());
        result.setPaymentType(
            registrant.getPaymentType() != null ? registrant.getPaymentType().getBulgarianValue() : "");
        result.setInvoiceNumber(Long.toString(registrant.getRealInvoiceNumber()));
        result.setInvoiceDate(LocalDate.now().format(DATE_FORMAT));

        Branch branch = registrant.getBranch() != null ? registrant.getBranch() : branchService.getCurrentBranch();
        String description = "jPrime " + branch.getYear() + " билет за конференция";
        int tickets = registrant.getVisitors().size();
        Map<TicketType, TicketPrice> prices = branchService.getTicketPrices(branch);
        BigDecimal price;
        if (registrant.isStudent()) {
            price = priceOf(prices, TicketType.STUDENT);
        } else {
            price = priceOf(prices, TicketType.REGULAR);
            LocalDateTime registered = registrant.getCreatedDate() != null ? registrant.getCreatedDate() :
                                       branch.getCfpOpenDate();
            // early bird: registered before the CFP closed and the invoice is issued within 3 days
            if (registered != null && branch.getCfpCloseDate() != null && registered.isBefore(branch.getCfpCloseDate())
                && Duration.between(registered, LocalDateTime.now()).abs().compareTo(Duration.ofDays(3)) <= 0
                && prices.containsKey(TicketType.EARLY_BIRD)) {
                price = priceOf(prices, TicketType.EARLY_BIRD);
            }
        }
        result.addInvoiceDetail(new InvoiceDetail(price, tickets, description));
        return result;
    }

    private static BigDecimal priceOf(Map<TicketType, TicketPrice> prices, TicketType type) {
        TicketPrice price = prices.get(type);
        return price != null && price.getPrice() != null ? price.getPrice() : BigDecimal.ZERO;
    }

    /** Proforma for bank transfers, original otherwise - as rendered right after registration. */
    public InvoiceData initialInvoice(Registrant registrant) {
        InvoiceData data = fromRegistrant(registrant);
        if (registrant.getPaymentType() == PaymentType.BANK_TRANSFER) {
            data.setInvoiceType(InvoiceData.PROFORMA_BG);
            data.setInvoiceNumber(String.valueOf(registrant.getProformaInvoiceNumber()));
        } else {
            data.setInvoiceType(InvoiceData.ORIGINAL_BG);
            data.setInvoiceNumber(String.valueOf(registrant.getRealInvoiceNumber()));
        }
        return data;
    }

    public byte[] renderPdf(InvoiceData data, boolean company) {
        return invoiceExporter.exportInvoice(data, company, InvoiceLanguage.BG, branchService.getCurrentBranch().getYear());
    }

    public static String pdfFileName(Registrant registrant, BigDecimal totalWithVat) {
        String date = LocalDate.now().format(FILE_DATE_FORMAT);
        int tickets = registrant.getVisitors().size();
        if (registrant.getRealInvoiceNumber() != 0) {
            return date + ", " + registrant.getRealInvoiceNumber() + ", " + tickets + "tickets, " + totalWithVat + ".pdf";
        }
        return "P " + date + ", " + registrant.getProformaInvoiceNumber() + ", " + tickets + "tickets, " + totalWithVat
            + ".pdf";
    }

    /** Mails the proforma invoice after a public ticket registration (to the registrant and to the organizers). */
    public void sendProforma(Registrant registrant) {
        InvoiceData data = initialInvoice(registrant);
        byte[] pdf = renderPdf(data, registrant.isCompany());
        String fileName = pdfFileName(registrant, data.getTotalPriceWithVAT());
        mailService.send(registrant.getEmail(), "jPrime.io invoice", """
            Thank you for registering at jPrime! Your proforma invoice is attached as part of this mail.<br/>
            Once we confirm your payment, we'll send you the original invoice.<br/>
            The attendees that you registered will receive the tickets a few days before the event on their emails.""",
            Attachment.pdf(pdf, fileName));
        mailService.sendQuietly(mailService.conferenceAddress(), "jPrime.io invoice",
            "We got some registrations: " + registrant.getVisitors(), Attachment.pdf(pdf, fileName));
    }

    /** Mails the original invoice (admin action, after the payment is received). */
    public void sendOriginal(Registrant registrant, InvoiceData data) {
        byte[] pdf = renderPdf(data, registrant.isCompany());
        String fileName = pdfFileName(registrant, data.getTotalPriceWithVAT());
        mailService.send(registrant.getEmail(), "jPrime.io original invoice", """
            Please find attached the invoice for the conference passes that you purchased.
            The attendees that you registered will receive the tickets a few days before the event on their emails.""",
            Attachment.pdf(pdf, fileName));
        mailService.sendQuietly(mailService.conferenceAddress(), "jPrime.io invoice",
            "The attached invoice was sent to " + registrant.getEmail(), Attachment.pdf(pdf, fileName));
        LOG.infof("Original invoice %s sent to %s", data.getInvoiceNumber(), registrant.getEmail());
    }
}
