package org.bgjug.jprime.admin.api;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.bgjug.jprime.core.service.PresenceService;
import org.bgjug.jprime.core.service.PresenceService.RaffleEntry;

/** Raffle candidates: present visitors (registered via /qr/tuk on the site). */
@Path("/api/admin/raffle")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class RaffleAdminResource {

    @Inject
    PresenceService presenceService;

    @GET
    @Path("jprime")
    @Transactional
    public List<RaffleEntry> jprime() {
        return presenceService.raffleCandidates();
    }

    @GET
    @Path("jpro")
    public List<RaffleEntry> jpro() {
        return presenceService.jProRaffleCandidates();
    }
}
