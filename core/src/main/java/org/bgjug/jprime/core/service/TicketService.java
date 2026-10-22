package org.bgjug.jprime.core.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Visitor;
import org.bgjug.jprime.core.model.VisitorStatus;
import org.bgjug.jprime.core.pdf.InvoiceLanguage;
import org.bgjug.jprime.core.pdf.TicketData;
import org.bgjug.jprime.core.pdf.TicketDetail;
import org.bgjug.jprime.core.pdf.TicketExporter;
import org.bgjug.jprime.core.repository.VisitorRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Generates ticket PDFs + QR codes and e-mails them to the visitors.
 */
@ApplicationScoped
public class TicketService {

    private static final Logger LOG = Logger.getLogger(TicketService.class);
    /** More tickets than this are sent by a background job. */
    private static final int SYNC_LIMIT = 3;

    @Inject
    TicketExporter ticketExporter;

    @Inject
    MailService mailService;

    @Inject
    MailTemplates mailTemplates;

    @Inject
    BranchService branchService;

    @Inject
    VisitorRepository visitorRepository;

    @Inject
    BackgroundJobService jobService;

    @ConfigProperty(name = "jprime.site-url", defaultValue = "https://jprime.io")
    String siteUrl;

    public record SendResult(int sent, int failed, String jobId) {
    }

    /** Sends tickets to all visitors of the current branch that paid / are sponsored and have no ticket yet. */
    public SendResult sendPendingTickets() {
        List<Long> ids = visitorRepository.pendingTickets(branchService.getCurrentBranch())
            .stream()
            .map(Visitor::getId)
            .toList();
        return sendTickets(ids, "Sending emails with tickets.");
    }

    /** Sends tickets to the given visitors - synchronously for few visitors, as a background job otherwise. */
    public SendResult sendTickets(List<Long> visitorIds, String jobDescription) {
        if (visitorIds.size() > SYNC_LIMIT) {
            String jobId = jobService.createJob(jobDescription);
            jobService.runJob(jobId, visitorIds, this::sendTicket, id -> "visitor " + id);
            return new SendResult(0, 0, jobId);
        }
        int sent = 0;
        int failed = 0;
        for (Long id : visitorIds) {
            if (QuarkusTransaction.requiringNew().call(() -> sendTicket(id))) {
                sent++;
            } else {
                failed++;
            }
        }
        return new SendResult(sent, failed, null);
    }

    /**
     * Generates (if needed) the ticket number, renders the PDF and mails it. Must run inside a transaction.
     *
     * @return true when the mail was sent
     */
    public boolean sendTicket(Long visitorId) {
        Visitor visitor = visitorRepository.findById(visitorId);
        if (visitor == null || visitor.getStatus() == VisitorStatus.REQUESTING) {
            return false;
        }
        String email = visitor.anyEmail();
        if (StringUtils.isBlank(email)) {
            return false;
        }
        Branch branch = visitor.getRegistrant().getBranch() != null ? visitor.getRegistrant().getBranch() :
                        branchService.getCurrentBranch();
        TicketData data = new TicketData();
        data.setEvent("JPrime " + branch.getYear());
        String previousTicket = visitor.getTicket();
        String ticketNumber = StringUtils.isEmpty(previousTicket) ? UUID.randomUUID().toString() :
                              visitor.getTicket();
        data.addDetail(new TicketDetail(ticketNumber, visitor.getName(), "Visitor"));
        byte[] pdf = ticketExporter.exportTicket(data, InvoiceLanguage.EN);
        byte[] qr = ticketExporter.generateTicketQrCode(data);
        visitor.setTicket(ticketNumber);
        String body = mailTemplates.render(MailTemplates.TICKET,
            Map.of("year", branch.getYear(), "visitorName", StringUtils.defaultString(visitor.getName()), "ticket",
                ticketNumber, "siteUrl", siteUrl));
        try {
            mailService.send(email, "JPrime " + branch.getYear() + " Conference ticket !", body,
                Attachment.pdf(pdf, "ticket_" + visitor.getId() + ".pdf"), Attachment.inlinePng(qr, "ticket_qr.png"));
            return true;
        } catch (RuntimeException e) {
            LOG.errorf("Unable to send ticket %s to %s: %s", ticketNumber, email, e.getMessage());
            visitor.setTicket(previousTicket);
            return false;
        }
    }
}
