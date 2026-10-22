package org.bgjug.jprime.site.web;

import java.time.LocalDate;
import java.util.List;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Session;
import org.bgjug.jprime.core.repository.SessionRepository;
import org.bgjug.jprime.core.service.BranchService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/agenda")
@Produces(MediaType.TEXT_HTML)
public class AgendaResource {

    @Inject
    @Location("agenda.html")
    Template agenda;

    @Inject
    @Location("talk.html")
    Template talk;

    @Inject
    SessionRepository sessionRepository;

    @Inject
    BranchService branchService;

    /** Hall names as stored in VenueHall.name, in column order. */
    @ConfigProperty(name = "jprime.agenda.halls", defaultValue = "Hall A,Hall B,Workshops")
    List<String> halls;

    /** Column captions (same order as the halls). */
    @ConfigProperty(name = "jprime.agenda.hall-captions", defaultValue = "Hall A,Hall B,Workshops & Deep Dives")
    List<String> hallCaptions;

    @GET
    public String agenda() {
        Branch branch = branchService.getCurrentBranch();
        List<AgendaBuilder.Day> days = List.of();
        if (branch.isAgendaPublished() && branch.getStartDate() != null) {
            int numberOfDays = branch.getDuration() != null ? (int) Math.max(1, branch.getDuration().toDays()) : 2;
            LocalDate first = branch.getStartDate().toLocalDate();
            days = AgendaBuilder.build(sessionRepository.findForBranch(branch), halls, first, numberOfDays);
        }
        return agenda.data("branch", branch).data("days", days).data("hallCaptions", hallCaptions).render();
    }

    @GET
    @Path("{id}")
    public String talk(@PathParam("id") long id) {
        Session session = sessionRepository.findByIdOptional(id)
            .filter(Session::isTalk)
            .orElseThrow(() -> new NotFoundException("Invalid session id " + id));
        return talk.data("talk", session).data("submission", session.getSubmission()).render();
    }
}
