package org.bgjug.jprime.site.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import io.quarkus.qute.TemplateExtension;

/** Helpers usable in all templates, e.g. {@code {tag.name.url}} or {@code {session.startTime.fmt('HH:mm')}}. */
@TemplateExtension
public class TemplateExtensions {

    static String url(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String fmt(LocalDateTime value, String pattern) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
    }

    static String fmt(LocalDate value, String pattern) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
    }

    /** Text with line breaks preserved (escaped first). */
    static io.quarkus.qute.RawString paragraphs(String value) {
        if (value == null) {
            return new io.quarkus.qute.RawString("");
        }
        String escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        return new io.quarkus.qute.RawString(escaped.replace("\r\n", "\n").replace("\n", "<br>"));
    }
}
