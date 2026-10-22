package org.bgjug.jprime.site.web;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bgjug.jprime.core.model.Session;
import org.bgjug.jprime.core.model.Submission;
import org.bgjug.jprime.core.repository.SessionRepository;
import org.bgjug.jprime.core.service.BranchService;

/**
 * JSON API used by the jPrime mobile/PWA application (the Flutter app served under /app). The JSON format is
 * unchanged. CORS is open for these paths (see application.properties).
 */
@Path("/pwa")
public class PwaResource {

    @Inject
    SessionRepository sessionRepository;

    @Inject
    BranchService branchService;

    public record SessionDTO(Long id, String hallName, String title, String lectorName, String coLectorName,
                             String talkDescription, LocalDateTime startTime, LocalDateTime endTime) {

        static SessionDTO of(Session session, String hallName) {
            Submission submission = session.getSubmission();
            if (submission == null) {
                return new SessionDTO(session.getId(), hallName, session.getTitle(), null, null, null,
                    session.getStartTime(), session.getEndTime());
            }
            return new SessionDTO(session.getId(), hallName, submission.getTitle(),
                submission.getSpeaker() != null ? submission.getSpeaker().getName() : null,
                submission.getCoSpeaker() != null ? submission.getCoSpeaker().getName() : null,
                submission.getDescription(), session.getStartTime(), session.getEndTime());
        }
    }

    @GET
    @Path("findSessionsByHall")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SessionDTO> sessionsByHall(@QueryParam("hallName") String hallName) {
        return sessionRepository.findForBranchAndHall(branchService.getCurrentBranch(), hallName)
            .stream()
            .map(s -> SessionDTO.of(s, hallName))
            .toList();
    }

    /** The old PWA page was replaced by the Flutter application (served under /app). */
    @GET
    public Response pwa() {
        return Response.seeOther(URI.create("/app/index.html")).build();
    }
}
