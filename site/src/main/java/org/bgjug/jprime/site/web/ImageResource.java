package org.bgjug.jprime.site.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;
import org.bgjug.jprime.core.model.Partner;
import org.bgjug.jprime.core.model.Speaker;
import org.bgjug.jprime.core.model.Sponsor;
import org.bgjug.jprime.core.repository.PartnerRepository;
import org.bgjug.jprime.core.repository.SpeakerRepository;
import org.bgjug.jprime.core.repository.SponsorRepository;

/** Pictures and logos stored in the database. */
@Path("/image")
public class ImageResource {

    @Inject
    SponsorRepository sponsorRepository;

    @Inject
    PartnerRepository partnerRepository;

    @Inject
    SpeakerRepository speakerRepository;

    @GET
    @Path("sponsor/{id}")
    public Response sponsor(@PathParam("id") long id) {
        return image(sponsorRepository.findByIdOptional(id).map(Sponsor::getLogo).orElse(null));
    }

    @GET
    @Path("partner/{id}")
    public Response partner(@PathParam("id") long id) {
        return image(partnerRepository.findByIdOptional(id).map(Partner::getLogo).orElse(null));
    }

    @GET
    @Path("speaker/{id}")
    public Response speaker(@PathParam("id") long id) {
        return image(speakerRepository.findByIdOptional(id).map(Speaker::getPicture).orElse(null));
    }

    static Response image(byte[] data) {
        if (data == null || data.length == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        CacheControl cache = new CacheControl();
        cache.setMaxAge(3600);
        return Response.ok(data).type(contentType(data)).cacheControl(cache).build();
    }

    /** Sniffs PNG / GIF / SVG, defaults to JPEG. */
    static String contentType(byte[] data) {
        if (data.length > 3 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
            return "image/png";
        }
        if (data.length > 2 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F') {
            return "image/gif";
        }
        if (data.length > 4 && (data[0] == '<' || (data[0] & 0xFF) == 0xEF)) {
            return "image/svg+xml";
        }
        return "image/jpeg";
    }
}
