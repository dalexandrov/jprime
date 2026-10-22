package org.bgjug.jprime.admin.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.bgjug.jprime.admin.dto.Dtos.Option;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.PartnerPackage;
import org.bgjug.jprime.core.model.PaymentType;
import org.bgjug.jprime.core.model.SessionLevel;
import org.bgjug.jprime.core.model.SessionType;
import org.bgjug.jprime.core.model.SponsorPackage;
import org.bgjug.jprime.core.model.SubmissionStatus;
import org.bgjug.jprime.core.model.VisitorStatus;
import org.bgjug.jprime.core.model.VisitorType;
import org.bgjug.jprime.core.service.BranchService;

/** Values for the drop downs of the UI. */
@Path("/api/admin/lookups")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class LookupResource {

    @Inject
    BranchService branchService;

    public record Lookups(int currentYear, List<Integer> years, Map<String, List<Option>> enums) {
    }

    @GET
    public Lookups lookups() {
        List<Integer> years = branchService.allBranches().stream().map(Branch::getYear).sorted((a, b) -> b - a).toList();
        return new Lookups(branchService.getCurrentBranch().getYear(), years, Map.of(
            "sessionLevel", Arrays.stream(SessionLevel.values()).map(v -> new Option(v.name(), v.getLabel())).toList(),
            "sessionType", Arrays.stream(SessionType.values()).map(v -> new Option(v.name(), v.getLabel())).toList(),
            "submissionStatus", Arrays.stream(SubmissionStatus.values()).map(v -> new Option(v.name(), v.name())).toList(),
            "visitorStatus", Arrays.stream(VisitorStatus.values()).map(v -> new Option(v.name(), v.getLabel())).toList(),
            "visitorType", Arrays.stream(VisitorType.values()).map(v -> new Option(v.name(), v.name())).toList(),
            "sponsorPackage", Arrays.stream(SponsorPackage.values()).map(v -> new Option(v.name(), v.getPackageName())).toList(),
            "partnerPackage", Arrays.stream(PartnerPackage.values()).map(v -> new Option(v.name(), v.name())).toList(),
            "paymentType", Arrays.stream(PaymentType.values()).map(v -> new Option(v.name(), v.getValue())).toList()));
    }
}
