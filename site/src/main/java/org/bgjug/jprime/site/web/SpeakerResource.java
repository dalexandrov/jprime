package org.bgjug.jprime.site.web;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.bgjug.jprime.core.service.SpeakerService;

@Path("/speakers")
@Produces(MediaType.TEXT_HTML)
public class SpeakerResource {

    @Inject
    @Location("speakers.html")
    Template speakers;

    @Inject
    SpeakerService speakerService;

    @GET
    public String speakers() {
        return speakers.data("speakers", speakerService.confirmed()).render();
    }
}
