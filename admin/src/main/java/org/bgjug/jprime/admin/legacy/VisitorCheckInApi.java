package org.bgjug.jprime.admin.legacy;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Speaker;
import org.bgjug.jprime.core.model.SubmissionStatus;
import org.bgjug.jprime.core.model.Visitor;
import org.bgjug.jprime.core.model.VisitorStatus;
import org.bgjug.jprime.core.repository.VisitorRepository;
import org.bgjug.jprime.core.service.BranchService;
import org.bgjug.jprime.core.service.SpeakerService;

import org.bgjug.jprime.admin.legacy.CheckInJson.SpeakerJson;
import org.bgjug.jprime.admin.legacy.CheckInJson.SpeakerSearch;
import org.bgjug.jprime.admin.legacy.CheckInJson.VisitorJson;
import org.bgjug.jprime.admin.legacy.CheckInJson.VisitorSearch;

/**
 * Part of the JSON API of the legacy application used by the registration desk (check-in) mobile application. Paths
 * and JSON field names are unchanged. Authentication: HTTP Basic (or the admin UI cookie), role ADMIN.
 */
@Path("/api/visitor")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class VisitorCheckInApi {

    @Inject
    VisitorRepository visitorRepository;

    @Inject
    BranchService branchService;

    /** All visitors of the given year that have a ticket. */
    @GET
    @Path("{year}")
    @Transactional
    public List<VisitorJson> visitors(@PathParam("year") int year) {
        Branch branch = branchService.findByYear(year);
        return visitorRepository.withTicket(branch).stream().map(VisitorJson::of).toList();
    }

    @GET
    @Path("{branch}/{ticket}")
    @Transactional
    public Response visitorByTicket(@PathParam("branch") String branch, @PathParam("ticket") String ticket) {
        return visitorRepository.findByTicket(ticket)
            .map(v -> Response.ok(VisitorJson.of(v)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("search/{branch}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public List<VisitorJson> searchVisitors(@PathParam("branch") String branch, VisitorSearch search) {
        return visitorRepository.search(branchService.getCurrentBranch(), search.email(), search.firstName(),
            search.lastName(), search.company()).stream().map(VisitorJson::of).toList();
    }
}
