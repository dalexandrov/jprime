package org.bgjug.jprime.site.web;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Global exception mappers + a last-resort route that sends unknown HTML URLs to the 404 page. */
@ApplicationScoped
public class ErrorMappers {

    private static final Logger LOG = Logger.getLogger(ErrorMappers.class);

    @Inject
    ErrorPages errorPages;

    @ServerExceptionMapper
    public Response notFound(NotFoundException e) {
        return Response.status(404).type(MediaType.TEXT_HTML_TYPE)
            .entity(errorPages.render(404, "Page not found", "The page you are looking for does not exist.")).build();
    }

    @ServerExceptionMapper
    public Response unexpected(RuntimeException e) {
        if (e instanceof jakarta.ws.rs.WebApplicationException wae) {
            return wae.getResponse();
        }
        LOG.error("Unexpected error", e);
        return Response.status(500).type(MediaType.TEXT_HTML_TYPE)
            .entity(errorPages.render(500, "Something went wrong", "Please try again later or contact conference@jprime.io."))
            .build();
    }

    void lastResort(@Observes Router router) {
        router.route().last().handler(ctx -> {
            String accept = ctx.request().getHeader("Accept");
            if ("GET".equals(ctx.request().method().name()) && accept != null && accept.contains("text/html")
                && !ctx.response().ended()) {
                ctx.redirect("/404");
            } else {
                ctx.next();
            }
        });
    }
}
