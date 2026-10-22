package org.bgjug.jprime.core.service;

/**
 * Mail attachment. Inline attachments are referenced from the HTML body as {@code cid:<name>}.
 */
public record Attachment(byte[] data, String name, String contentType, boolean inline) {

    public static Attachment pdf(byte[] data, String name) {
        return new Attachment(data, name, "application/pdf", false);
    }

    public static Attachment inlinePng(byte[] data, String name) {
        return new Attachment(data, name, "image/png", true);
    }
}
