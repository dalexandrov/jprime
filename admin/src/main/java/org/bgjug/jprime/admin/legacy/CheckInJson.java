package org.bgjug.jprime.admin.legacy;

import java.util.List;

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

/** JSON shapes of the legacy check-in API (unchanged field names). */
public final class CheckInJson {

    private CheckInJson() {
    }

    /** Same JSON shape as the legacy Visitor entity serialization. */
    public record VisitorJson(Long id, String name, String email, String company, String ticket, boolean present,
                              boolean registered, VisitorStatus status, String createdBy, String lastModifiedBy) {

        public static VisitorJson of(Visitor v) {
            return new VisitorJson(v.getId(), v.getName(), v.getEmail(), v.getCompany(), v.getTicket(), v.isPresent(),
                v.isRegistered(), v.getStatus(), v.getCreatedBy(), v.getLastModifiedBy());
        }
    }

    public record BranchJson(String label, int year) {
    }

    /** Same JSON shape as the legacy Speaker entity serialization (bio, picture... were ignored there too). */
    public record SpeakerJson(Long id, String firstName, String lastName, String email, String name, String twitter,
                              String bsky, SubmissionStatus status, boolean featured, BranchJson branch,
                              String speakerType, int numberOfSubmissions, String createdBy, String lastModifiedBy) {

        public static SpeakerJson of(Speaker s) {
            return new SpeakerJson(s.getId(), s.getFirstName(), s.getLastName(), s.getEmail(), s.getName(),
                s.getTwitter(), s.getBsky(), s.getStatus(), s.isFeatured(),
                s.getLastBranch() != null ? new BranchJson(s.getLastBranch().getLabel(), s.getLastBranch().getYear()) :
                null, s.getSpeakerType(), s.getNumberOfSubmissions(), s.getCreatedBy(), s.getLastModifiedBy());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VisitorSearch(String email, String firstName, String lastName, String company) {
    }

    public record SpeakerSearch(String firstName, String lastName, String email) {
    }

}
