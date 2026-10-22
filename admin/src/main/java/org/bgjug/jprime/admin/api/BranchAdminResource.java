package org.bgjug.jprime.admin.api;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.bgjug.jprime.admin.dto.Dtos.BranchDto;
import org.bgjug.jprime.admin.dto.Dtos.BranchInput;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.TicketPrice;
import org.bgjug.jprime.core.model.TicketType;
import org.bgjug.jprime.core.service.BranchService;

/** Conference editions ("branches") and their ticket prices. */
@Path("/api/admin/branches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class BranchAdminResource {

    @Inject
    BranchService branchService;

    @GET
    public List<BranchDto> list() {
        return branchService.allBranches().stream()
            .sorted((a, b) -> b.getYear() - a.getYear())
            .map(b -> BranchDto.of(b, prices(b)))
            .toList();
    }

    @GET
    @Path("{year}")
    public BranchDto get(@PathParam("year") int year) {
        Branch branch = branchService.findByYear(year);
        return BranchDto.of(branch, prices(branch));
    }

    /** Creates or updates the branch of the given year. */
    @PUT
    @Path("{year}")
    public BranchDto save(@PathParam("year") int year, BranchInput input) {
        Branch data = new Branch(year);
        data.setStartDate(input.startDate());
        data.setDuration(Duration.ofHours(input.durationHours()));
        data.setCfpOpenDate(input.cfpOpenDate());
        // the CFP closes at the end of the selected day (as before)
        data.setCfpCloseDate(input.cfpCloseDate() != null ?
                             input.cfpCloseDate().toLocalDate().atStartOfDay().plusDays(1).minusSeconds(1) : null);
        data.setSoldOut(input.soldOut());
        data.setAgendaPublished(input.agendaPublished());
        data.soldOutPackageSet(input.soldOutPackages() != null ? input.soldOutPackages() : EnumSet.noneOf(
            org.bgjug.jprime.core.model.SponsorPackage.class));
        Map<TicketType, BigDecimal> prices = new EnumMap<>(TicketType.class);
        for (TicketType type : TicketType.values()) {
            BigDecimal price = input.ticketPrices() != null ? input.ticketPrices().get(type) : null;
            prices.put(type, price != null ? price : BigDecimal.ZERO);
        }
        Branch saved = branchService.saveBranch(data, prices);
        return BranchDto.of(saved, prices);
    }

    @POST
    @Path("{year}/current")
    public BranchDto makeCurrent(@PathParam("year") int year) {
        branchService.setAsCurrent(year);
        return get(year);
    }

    @DELETE
    @Path("{year}")
    public void delete(@PathParam("year") int year) {
        branchService.deleteBranch(year);
    }

    private Map<TicketType, BigDecimal> prices(Branch branch) {
        Map<TicketType, BigDecimal> result = new EnumMap<>(TicketType.class);
        Map<TicketType, TicketPrice> prices = branchService.getTicketPrices(branch);
        prices.forEach((type, price) -> result.put(type, price.getPrice()));
        return result;
    }
}
