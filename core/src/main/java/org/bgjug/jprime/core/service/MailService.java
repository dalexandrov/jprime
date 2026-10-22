package org.bgjug.jprime.core.service;

import java.util.Optional;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.core.model.JPrimeException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Sends HTML e-mails. When {@code jprime.mail.test-address} is set, every message goes to that address instead of
 * the real recipient (useful on staging).
 */
@ApplicationScoped
public class MailService {

    private static final Logger LOG = Logger.getLogger(MailService.class);

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "jprime.mail.test-address")
    Optional<String> testAddress;

    @ConfigProperty(name = "jprime.mail.conference-address", defaultValue = "conference@jprime.io")
    String conferenceAddress;

    public String conferenceAddress() {
        return conferenceAddress;
    }

    /**
     * @throws JPrimeException when the mail cannot be sent
     */
    public void send(String to, String subject, String html, Attachment... attachments) {
        if (StringUtils.isBlank(to)) {
            throw new JPrimeException("No recipient for mail '" + subject + "'");
        }
        String recipient = testAddress.filter(StringUtils::isNotBlank).orElse(to);
        Mail mail = Mail.withHtml(recipient, subject, html);
        for (Attachment attachment : attachments) {
            if (attachment.inline()) {
                mail.addInlineAttachment(attachment.name(), attachment.data(), attachment.contentType(),
                    "<" + attachment.name() + ">");
            } else {
                mail.addAttachment(attachment.name(), attachment.data(), attachment.contentType());
            }
        }
        try {
            mailer.send(mail);
        } catch (RuntimeException e) {
            throw new JPrimeException("Could not send mail '" + subject + "' to " + to, e);
        }
    }

    /** Same as {@link #send} but only logs failures. */
    public boolean sendQuietly(String to, String subject, String html, Attachment... attachments) {
        try {
            send(to, subject, html, attachments);
            return true;
        } catch (RuntimeException e) {
            LOG.errorf(e, "Mail '%s' to %s failed", subject, to);
            return false;
        }
    }
}
