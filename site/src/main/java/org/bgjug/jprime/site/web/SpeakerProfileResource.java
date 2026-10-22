package org.bgjug.jprime.site.web;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.bgjug.jprime.core.model.Speaker;
import org.bgjug.jprime.core.service.SpeakerService;

/** Public speaker profile (only confirmed or featured speakers of the current branch). */
@Path("/speaker")
@Produces(MediaType.TEXT_HTML)
public class SpeakerProfileResource {

    @Inject
    @Location("speaker.html")
    Template speaker;

    @Inject
    SpeakerService speakerService;

    @GET
    @Path("{id}")
    public String speaker(@PathParam("id") long id) {
        Speaker found = speakerService.publicProfile(id).orElseThrow(() -> new NotFoundException("Unknown speaker"));
        return speaker.data("speaker", found).render();
    }
}
