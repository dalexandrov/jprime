package org.bgjug.jprime.admin.security;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * When the React UI is bundled into this application (profile {@code bundle-ui}), deep links such as
 * {@code /speakers/12} must return {@code index.html} so the client side router can handle them.
 */
@ApplicationScoped
public class SpaRouting {

    void spaFallback(@Observes Router router) {
        router.route().last().handler(ctx -> {
            String path = ctx.normalizedPath();
            boolean isApi = path.startsWith("/api/") || path.startsWith("/q/") || path.equals("/j_security_check");
            boolean isFile = path.substring(path.lastIndexOf('/') + 1).contains(".");
            if ("GET".equals(ctx.request().method().name()) && !isApi && !isFile && !ctx.response().ended()) {
                ctx.reroute("/index.html");
            } else {
                ctx.next();
            }
        });
    }
}
