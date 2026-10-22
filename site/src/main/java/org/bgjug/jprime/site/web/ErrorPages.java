package org.bgjug.jprime.site.web;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Friendly HTML error pages. */
@Path("/404")
public class ErrorPages {

    @Inject
    @Location("error.html")
    Template error;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String notFoundPage() {
        return render(404, "Page not found", "The page you are looking for does not exist.");
    }

    String render(int status, String title, String text) {
        try {
            return error.data("status", status).data("title", title).data("text", text).render();
        } catch (RuntimeException renderFailure) {
            // e.g. no current branch configured - the layout needs it
            return "<!DOCTYPE html><html><body><h1>" + title + "</h1><p>" + text + "</p></body></html>";
        }
    }
}
