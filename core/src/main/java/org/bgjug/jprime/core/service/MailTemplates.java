package org.bgjug.jprime.core.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.Variant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Renders the e-mail bodies stored in {@code src/main/resources/mail/*.html} (Qute syntax, HTML escaped).
 */
@ApplicationScoped
public class MailTemplates {

    public static final String SUBMISSION_ACCEPTED = "submission-accepted";
    public static final String SUBMISSION_REJECTED = "submission-rejected";
    public static final String SUBMISSION_RECEIVED = "submission-received";
    public static final String SUBMISSION_NEW = "submission-new";
    public static final String RESET_PASSWORD = "reset-password";
    public static final String WELCOME = "welcome";
    public static final String TICKET = "ticket";

    @Inject
    Engine engine;

    private final Map<String, Template> cache = new ConcurrentHashMap<>();

    public String render(String name, Map<String, Object> data) {
        Template template = cache.computeIfAbsent(name, this::load);
        var instance = template.instance();
        data.forEach(instance::data);
        return instance.render();
    }

    private Template load(String name) {
        String path = "mail/" + name + ".html";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Mail template not found: " + path);
            }
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return engine.parse(content, Variant.forContentType(Variant.TEXT_HTML), "mail-" + name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
