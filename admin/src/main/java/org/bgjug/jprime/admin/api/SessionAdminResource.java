package org.bgjug.jprime.admin.api;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.admin.dto.Dtos.SessionDto;
import org.bgjug.jprime.admin.dto.Dtos.SessionInput;
import org.bgjug.jprime.admin.dto.Dtos.SubmissionDto;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Session;
import org.bgjug.jprime.core.model.SubmissionStatus;
import org.bgjug.jprime.core.repository.SessionRepository;
import org.bgjug.jprime.core.repository.SubmissionRepository;
import org.bgjug.jprime.core.repository.VenueHallRepository;
import org.bgjug.jprime.core.service.BranchService;
import org.bgjug.jprime.core.service.ValidationFailedException;

/**
 * Agenda slots of the current branch. A slot is either a confirmed talk in a hall, or a titled slot (e.g. "Break",
 * "Lunch") without hall that spans all halls, or "TBA" in a hall.
 */
@Path("/api/admin/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class SessionAdminResource {

    @Inject
    SessionRepository sessionRepository;

    @Inject
    SubmissionRepository submissionRepository;

    @Inject
    VenueHallRepository hallRepository;

    @Inject
    BranchService branchService;

    @GET
    public List<SessionDto> list() {
        Branch branch = branchService.getCurrentBranch();
        return sessionRepository.findForBranch(branch).stream()
            .filter(s -> s.getSubmission() != null || s.getStartTime() == null
                || s.getStartTime().getYear() == branch.getYear())
            .map(SessionDto::of)
            .toList();
    }

    /** Confirmed submissions of the current branch that are not yet scheduled. */
    @GET
    @Path("unscheduled-submissions")
    public List<SubmissionDto> unscheduled() {
        Branch branch = branchService.getCurrentBranch();
        Set<Long> scheduled = sessionRepository.findForBranch(branch).stream()
            .map(Session::getSubmission)
            .filter(Objects::nonNull)
            .map(s -> s.getId())
            .collect(Collectors.toSet());
        return submissionRepository.findByBranchAndStatus(branch, SubmissionStatus.CONFIRMED).stream()
            .filter(s -> !scheduled.contains(s.getId()))
            .map(SubmissionDto::of)
            .toList();
    }

    @GET
    @Path("{id}")
    public SessionDto get(@PathParam("id") long id) {
        return SessionDto.of(AdminSupport.found(sessionRepository.findByIdOptional(id), "Session"));
    }

    @POST
    @Transactional
    public SessionDto create(SessionInput input) {
        Session session = new Session();
        apply(session, input);
        sessionRepository.persist(session);
        return SessionDto.of(session);
    }

    @PUT
    @Path("{id}")
    @Transactional
    public SessionDto update(@PathParam("id") long id, SessionInput input) {
        Session session = AdminSupport.found(sessionRepository.findByIdOptional(id), "Session");
        apply(session, input);
        return SessionDto.of(session);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") long id) {
        sessionRepository.deleteById(id);
    }

    private void apply(Session session, SessionInput input) {
        if (input.startTime() == null || input.endTime() == null || !input.endTime().isAfter(input.startTime())) {
            throw ValidationFailedException.of("endTime", "Start and end time are required, end after start");
        }
        session.setStartTime(input.startTime());
        session.setEndTime(input.endTime());
        if (input.submissionId() != null) {
            if (input.hallId() == null) {
                throw ValidationFailedException.of("hallId", "A talk needs a hall");
            }
            session.setSubmission(AdminSupport.found(submissionRepository.findByIdOptional(input.submissionId()),
                "Submission"));
            session.setHall(AdminSupport.found(hallRepository.findByIdOptional(input.hallId()), "Hall"));
            session.setTitle(null);
        } else {
            if (StringUtils.isBlank(input.title())) {
                throw ValidationFailedException.of("title", "Select a talk or enter a title (e.g. Break)");
            }
            session.setSubmission(null);
            session.setTitle(input.title().trim());
            // titled slots span all halls, except "TBA" which is placed in a hall
            session.setHall(input.hallId() != null && "TBA".equalsIgnoreCase(input.title().trim()) ?
                            AdminSupport.found(hallRepository.findByIdOptional(input.hallId()), "Hall") : null);
        }
    }
}
