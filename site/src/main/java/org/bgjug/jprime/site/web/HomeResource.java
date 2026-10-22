package org.bgjug.jprime.site.web;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Partner;
import org.bgjug.jprime.core.model.PartnerPackage;
import org.bgjug.jprime.core.model.Sponsor;
import org.bgjug.jprime.core.model.SponsorPackage;
import org.bgjug.jprime.core.model.TicketPrice;
import org.bgjug.jprime.core.model.TicketType;
import org.bgjug.jprime.core.repository.PartnerRepository;
import org.bgjug.jprime.core.repository.SponsorRepository;
import org.bgjug.jprime.core.service.BranchService;
import org.bgjug.jprime.core.service.DateFormats;
import org.bgjug.jprime.core.service.SpeakerService;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class HomeResource {

    /** Sponsor packages shown on the home page, in this order. */
    static final List<SponsorPackage> SHOWN_PACKAGES = List.of(SponsorPackage.PLATINUM, SponsorPackage.GOLD,
        SponsorPackage.GOLD_LITE, SponsorPackage.GOLD_OPEN, SponsorPackage.SILVER);

    @Inject
    @Location("index.html")
    Template index;

    @Inject
    @Location("team.html")
    Template team;

    @Inject
    @Location("venue.html")
    Template venue;

    @Inject
    @Location("privacy-policy.html")
    Template privacyPolicy;

    @Inject
    @Location("privacy-policy-bg.html")
    Template privacyPolicyBg;

    @Inject
    BranchService branchService;

    @Inject
    SponsorRepository sponsorRepository;

    @Inject
    PartnerRepository partnerRepository;

    @Inject
    SpeakerService speakerService;

    @TemplateData
    public record SponsorGroup(SponsorPackage sponsorPackage, List<Sponsor> sponsors) {
    }

    @TemplateData
    public record PartnerGroup(String title, List<Partner> partners) {
    }

    @TemplateData
    public record TicketOffer(String name, String price, String note, boolean available) {
    }

    @GET
    public String index() {
        Branch branch = branchService.getCurrentBranch();
        LocalDateTime now = LocalDateTime.now();

        Map<SponsorPackage, List<Sponsor>> byPackage = sponsorRepository.findActive().stream()
            .filter(s -> s.getSponsorPackage() != null)
            .collect(Collectors.groupingBy(Sponsor::getSponsorPackage, () -> new EnumMap<>(SponsorPackage.class),
                Collectors.toList()));
        List<SponsorGroup> sponsorGroups = new ArrayList<>();
        for (SponsorPackage p : SHOWN_PACKAGES) {
            List<Sponsor> list = new ArrayList<>(byPackage.getOrDefault(p, List.of()));
            if (!list.isEmpty()) {
                Collections.shuffle(list);
                sponsorGroups.add(new SponsorGroup(p, list));
            }
        }

        Map<String, PartnerPackage> partnerTitles = new LinkedHashMap<>();
        partnerTitles.put("Official Supporters", PartnerPackage.SUPPORTERS);
        partnerTitles.put("Community and Media Partners", PartnerPackage.MEDIA);
        partnerTitles.put("Event Partners", PartnerPackage.OTHER);
        List<PartnerGroup> partnerGroups = new ArrayList<>();
        partnerTitles.forEach((title, p) -> {
            List<Partner> list = new ArrayList<>(partnerRepository.findActive(p));
            if (!list.isEmpty()) {
                Collections.shuffle(list);
                partnerGroups.add(new PartnerGroup(title, list));
            }
        });

        // ticket availability - same rules as the legacy site
        boolean earlyBirdPeriod = branch.getCfpCloseDate() != null && branch.getCfpCloseDate().isAfter(now)
            && branch.getCfpOpenDate() != null && branch.getCfpOpenDate().minusDays(30).isBefore(now);
        Map<TicketType, TicketPrice> prices = branchService.getTicketPrices(branch);
        String cfpClose = DateFormats.withMonthAndYear(branch.getCfpCloseDate());
        List<TicketOffer> tickets = List.of(
            new TicketOffer("Early Bird Ticket", price(prices, TicketType.EARLY_BIRD), "Until " + cfpClose + "!",
                earlyBirdPeriod),
            new TicketOffer("Regular Ticket", price(prices, TicketType.REGULAR), "After " + cfpClose,
                !earlyBirdPeriod && !branch.isSoldOut()),
            new TicketOffer("Students ticket", price(prices, TicketType.STUDENT), "limited to students only",
                earlyBirdPeriod || !branch.isSoldOut()));

        Optional<Branch> next = branchService.getNextBranch();
        LocalDateTime start = next.map(Branch::getStartDate).orElse(branch.getStartDate());
        Integer year = next.map(Branch::getYear)
            .orElse(branch.getStartDate() != null && branch.getStartDate().isAfter(now) ? branch.getYear() : null);

        return index.data("branch", branch)
            .data("year", year)
            .data("conferenceDates", DateFormats.conferenceDates(start))
            .data("cfpCloseDate", cfpClose)
            .data("cfpClosed", branch.getCfpCloseDate() == null || branch.getCfpCloseDate().isBefore(now))
            .data("ticketSalesOpen", branch.getCfpOpenDate() != null && branch.getCfpOpenDate().isBefore(now))
            .data("featuredSpeakers", speakerService.featured())
            .data("sponsorGroups", sponsorGroups)
            .data("partnerGroups", partnerGroups)
            .data("tickets", tickets)
            .data("sponsorPackages", sponsorPackageOffers(branch))
            .render();
    }

    @TemplateData
    public record SponsorPackageOffer(SponsorPackage sponsorPackage, String price, List<String> benefits,
                                      boolean soldOut) {

        public String subject() {
            return sponsorPackage.name().toLowerCase() + " sponsorship";
        }
    }

    static List<SponsorPackageOffer> sponsorPackageOffers(Branch branch) {
        return List.of(
            new SponsorPackageOffer(SponsorPackage.PLATINUM, "EUR 4250.00",
                List.of("9 free passes!", "15% discount on additional passes!", "Booth in the conference hall!",
                    "Presence on the stage for the raffle", "Logo on the presentation screens",
                    "1 invite for the \"thank you\" dinner"), branch.isPackageSoldOut(SponsorPackage.PLATINUM)),
            new SponsorPackageOffer(SponsorPackage.GOLD, "EUR 3250.00",
                List.of("6 free passes!", "10% discount on additional passes!", "Booth in the conference hall!",
                    "Logo on the presentation screens"), branch.isPackageSoldOut(SponsorPackage.GOLD)),
            new SponsorPackageOffer(SponsorPackage.GOLD_LITE, "EUR 2650.00",
                List.of("6 free passes!", "10% discount on additional passes!", "Booth in the corner area!",
                    "Logo on the presentation screens"), branch.isPackageSoldOut(SponsorPackage.GOLD_LITE)),
            new SponsorPackageOffer(SponsorPackage.GOLD_OPEN, "EUR 2250.00",
                List.of("6 free passes!", "10% discount on additional passes!",
                    "Booth under the tent in the area in front of the venue!", "Logo on the presentation screens"),
                branch.isPackageSoldOut(SponsorPackage.GOLD_OPEN)),
            new SponsorPackageOffer(SponsorPackage.SILVER, "EUR 1000.00",
                List.of("3 free passes!", "10% discount on additional passes!", "Logo on the presentation screens"),
                branch.isPackageSoldOut(SponsorPackage.SILVER)));
    }

    static String price(Map<TicketType, TicketPrice> prices, TicketType type) {
        TicketPrice price = prices.get(type);
        return price == null || price.getPrice() == null ? "-" : String.format("%.2f", price.getPrice());
    }

    @GET
    @Path("team")
    public String team() {
        return team.render();
    }

    @GET
    @Path("venue")
    public String venue() {
        LocalDateTime start = branchService.getCurrentBranch().getStartDate();
        String dates = start == null ? "" : start.getDayOfMonth() + "-" + DateFormats.withMonthAndYear(start.plusDays(1));
        return venue.data("conferenceDates", dates).render();
    }

    // ---- old URLs of the legacy site (bookmarks, links in e-mails) ----

    @GET
    @Path("login")
    public jakarta.ws.rs.core.Response legacyLogin() {
        return redirect("/account/login");
    }

    @GET
    @Path("signup")
    public jakarta.ws.rs.core.Response legacySignup() {
        return redirect("/account/signup");
    }

    @GET
    @Path("resetPassword")
    public jakarta.ws.rs.core.Response legacyResetPassword() {
        return redirect("/account/reset-password");
    }

    @GET
    @Path("createNewPassword")
    public jakarta.ws.rs.core.Response legacyNewPassword(@QueryParam("tokenId") String tokenId) {
        return redirect("/account/new-password?tokenId=" + TemplateExtensions.url(tokenId));
    }

    @GET
    @Path("cfp-thank-you")
    public jakarta.ws.rs.core.Response legacyCfpThankYou() {
        return redirect("/cfp/thank-you");
    }

    private static jakarta.ws.rs.core.Response redirect(String location) {
        return jakarta.ws.rs.core.Response.status(301).location(java.net.URI.create(location)).build();
    }

    /** The Flutter mobile web application. */
    @GET
    @Path("app")
    public jakarta.ws.rs.core.Response app() {
        return jakarta.ws.rs.core.Response.seeOther(java.net.URI.create("/app/index.html")).build();
    }

    @GET
    @Path("privacy-policy")
    public String privacyPolicy(@QueryParam("bg") String bg) {
        return bg != null ? privacyPolicyBg.render() : privacyPolicy.render();
    }
}
