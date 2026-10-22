package org.bgjug.jprime.site.web;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.core.service.PresenceService;

/** "I am here!" - attendees register their presence for the raffle (QR code on the screens points here). */
@Path("/qr")
@Produces(MediaType.TEXT_HTML)
public class QrResource {

    private static final String NOT_FOUND = "User not found ... please write your name exactly as on the badge.";

    @Inject
    @Location("qr/qr.html")
    Template qr;

    @Inject
    @Location("qr/presence.html")
    Template presence;

    @Inject
    @Location("qr/presence-jpro.html")
    Template presenceJPro;

    @Inject
    @Location("qr/success.html")
    Template success;

    @Inject
    PresenceService presenceService;

    @GET
    public String qr() {
        return qr.render();
    }

    @GET
    @Path("tuk")
    public String presenceForm() {
        return presence.data("message", null).render();
    }

    @POST
    @Path("tuk")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.MULTIPART_FORM_DATA})
    public String markPresent(@FormParam("id") String id, @FormParam("name") String name,
        @FormParam("company") String company) {
        Long visitorId = null;
        if (StringUtils.isNotBlank(id)) {
            try {
                visitorId = Long.valueOf(id.trim());
            } catch (NumberFormatException e) {
                return presence.data("message", "The ID must be a number.").render();
            }
        }
        if (presenceService.markPresent(visitorId, name, company)) {
            return success.render();
        }
        return presence.data("message", NOT_FOUND).render();
    }

    @GET
    @Path("tukjpro")
    public String presenceJProForm() {
        return presenceJPro.data("message", null).render();
    }

    @POST
    @Path("tukjpro")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.MULTIPART_FORM_DATA})
    public String markJProPresent(@FormParam("name") String name, @FormParam("email") String email) {
        if (presenceService.markJProPresent(name, email)) {
            return success.render();
        }
        return presenceJPro.data("message", NOT_FOUND).render();
    }
}
