package org.bgjug.jprime.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bgjug.jprime.core.model.Article;
import org.bgjug.jprime.core.model.BackgroundJob;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Partner;
import org.bgjug.jprime.core.model.PartnerPackage;
import org.bgjug.jprime.core.model.PaymentType;
import org.bgjug.jprime.core.model.Registrant;
import org.bgjug.jprime.core.model.Session;
import org.bgjug.jprime.core.model.SessionLevel;
import org.bgjug.jprime.core.model.SessionType;
import org.bgjug.jprime.core.model.Speaker;
import org.bgjug.jprime.core.model.Sponsor;
import org.bgjug.jprime.core.model.SponsorPackage;
import org.bgjug.jprime.core.model.Submission;
import org.bgjug.jprime.core.model.SubmissionStatus;
import org.bgjug.jprime.core.model.Tag;
import org.bgjug.jprime.core.model.TicketType;
import org.bgjug.jprime.core.model.User;
import org.bgjug.jprime.core.model.VenueHall;
import org.bgjug.jprime.core.model.Visitor;
import org.bgjug.jprime.core.model.VisitorStatus;
import org.bgjug.jprime.core.service.DateFormats;
import org.bgjug.jprime.core.service.PageResult;

/**
 * JSON representations used by the admin API. Entities are never serialized directly.
 */
public final class Dtos {

    private Dtos() {
    }

    public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

        public static <T> Page<T> of(PageResult<T> result) {
            return new Page<>(result.content(), result.page(), result.size(), result.totalElements(),
                result.totalPages());
        }
    }

    public record Message(String message) {
    }

    // ------------------------------------------------------------------ branches

    public record BranchDto(int year, String label, LocalDateTime startDate, long durationHours,
                            String durationText, LocalDateTime cfpOpenDate, LocalDateTime cfpCloseDate,
                            boolean soldOut, boolean agendaPublished, boolean currentBranch,
                            Set<SponsorPackage> soldOutPackages, Map<TicketType, BigDecimal> ticketPrices) {

        public static BranchDto of(Branch b, Map<TicketType, BigDecimal> prices) {
            return new BranchDto(b.getYear(), b.getLabel(), b.getStartDate(),
                b.getDuration() != null ? b.getDuration().toHours() : 0, DateFormats.formatDuration(b.getDuration()),
                b.getCfpOpenDate(), b.getCfpCloseDate(), b.isSoldOut(), b.isAgendaPublished(), b.isCurrentBranch(),
                b.soldOutPackageSet(), prices);
        }
    }

    public record BranchInput(LocalDateTime startDate, long durationHours, LocalDateTime cfpOpenDate,
                              LocalDateTime cfpCloseDate, boolean soldOut, boolean agendaPublished,
                              Set<SponsorPackage> soldOutPackages, Map<TicketType, BigDecimal> ticketPrices) {
    }

    // ------------------------------------------------------------------ tags & articles

    public record TagDto(Long id, String name) {

        public static TagDto of(Tag t) {
            return new TagDto(t.getId(), t.getName());
        }
    }

    public record ArticleDto(Long id, String title, String description, boolean published, String text,
                             List<TagDto> tags, LocalDateTime createdDate, String createdBy) {

        public static ArticleDto of(Article a) {
            return new ArticleDto(a.getId(), a.getTitle(), a.getDescription(), a.isPublished(), a.getText(),
                a.getTags().stream().map(TagDto::of).sorted(Comparator.comparing(TagDto::name)).toList(),
                a.getCreatedDate(), a.getCreatedBy());
        }

        /** Without the (potentially long) text - for lists. */
        public static ArticleDto summary(Article a) {
            return new ArticleDto(a.getId(), a.getTitle(), a.getDescription(), a.isPublished(), null,
                a.getTags().stream().map(TagDto::of).toList(), a.getCreatedDate(), a.getCreatedBy());
        }
    }

    public record ArticleInput(String title, String description, boolean published, String text, List<Long> tagIds) {
    }

    // ------------------------------------------------------------------ sponsors & partners

    public record SponsorDto(Long id, String companyName, String companyWebsite, String email, String description,
                             SponsorPackage sponsorPackage, boolean active) {

        public static SponsorDto of(Sponsor s) {
            return new SponsorDto(s.getId(), s.getCompanyName(), s.getCompanyWebsite(), s.getEmail(),
                s.getDescription(), s.getSponsorPackage(), s.isActive());
        }
    }

    public record PartnerDto(Long id, String companyName, String companyWebsite, String description,
                             PartnerPackage partnerPackage, boolean active) {

        public static PartnerDto of(Partner p) {
            return new PartnerDto(p.getId(), p.getCompanyName(), p.getCompanyWebsite(), p.getDescription(),
                p.getPartnerPackage(), p.isActive());
        }
    }

    // ------------------------------------------------------------------ speakers & submissions

    public record SubmissionSummary(Long id, String title, Integer branchYear, SubmissionStatus status,
                                    boolean featured) {

        public static SubmissionSummary of(Submission s) {
            return new SubmissionSummary(s.getId(), s.getTitle(), s.getBranch() != null ? s.getBranch().getYear() : null,
                s.getStatus(), s.isFeatured());
        }
    }

    public record SpeakerDto(Long id, String firstName, String lastName, String email, String phone, String headline,
                             String twitter, String bsky, String bio, String speakerType, int numberOfSubmissions,
                             boolean featured, SubmissionStatus status, Integer lastBranchYear,
                             List<SubmissionSummary> submissions) {

        public static SpeakerDto of(Speaker s) {
            return new SpeakerDto(s.getId(), s.getFirstName(), s.getLastName(), s.getEmail(), s.getPhone(),
                s.getHeadline(), s.getTwitter(), s.getBsky(), s.getBio(), s.getSpeakerType(),
                s.getNumberOfSubmissions(), s.isFeatured(), s.getStatus(),
                s.getLastBranch() != null ? s.getLastBranch().getYear() : null,
                s.getAllSubmissions().stream().map(SubmissionSummary::of).toList());
        }
    }

    public record PersonRef(Long id, String firstName, String lastName, String email) {

        public static PersonRef of(Speaker s) {
            return s == null ? null : new PersonRef(s.getId(), s.getFirstName(), s.getLastName(), s.getEmail());
        }
    }

    public record SubmissionDto(Long id, String title, String description, SessionLevel level, SessionType type,
                                SubmissionStatus status, boolean featured, Integer branchYear, PersonRef speaker,
                                PersonRef coSpeaker, LocalDateTime createdDate) {

        public static SubmissionDto of(Submission s) {
            return new SubmissionDto(s.getId(), s.getTitle(), s.getDescription(), s.getLevel(), s.getTypeOrDefault(),
                s.getStatus(), s.isFeatured(), s.getBranch() != null ? s.getBranch().getYear() : null,
                PersonRef.of(s.getSpeaker()), PersonRef.of(s.getCoSpeaker()), s.getCreatedDate());
        }
    }

    public record SpeakerFields(String firstName, String lastName, String email, String headline, String twitter,
                                String bsky, String bio) {
    }

    public record SubmissionInput(String title, String description, SessionLevel level, SessionType type,
                                  Integer branchYear, Boolean featured, SpeakerFields speaker,
                                  SpeakerFields coSpeaker) {
    }

    // ------------------------------------------------------------------ halls & sessions

    public record HallDto(Long id, String name, String description) {

        public static HallDto of(VenueHall h) {
            return new HallDto(h.getId(), h.getName(), h.getDescription());
        }
    }

    public record SessionDto(Long id, String title, LocalDateTime startTime, LocalDateTime endTime, Long submissionId,
                             String submissionTitle, String speakers, Long hallId, String hallName) {

        public static SessionDto of(Session s) {
            Submission sub = s.getSubmission();
            String speakers = null;
            if (sub != null && sub.getSpeaker() != null) {
                speakers = sub.getSpeaker().getName() + (sub.getCoSpeaker() != null ? " & " + sub.getCoSpeaker().getName() :
                                                         "");
            }
            return new SessionDto(s.getId(), s.getRawTitle(), s.getStartTime(), s.getEndTime(),
                sub != null ? sub.getId() : null, sub != null ? sub.getTitle() : null, speakers,
                s.getHall() != null ? s.getHall().getId() : null, s.getHall() != null ? s.getHall().getName() : null);
        }
    }

    public record SessionInput(String title, LocalDateTime startTime, LocalDateTime endTime, Long submissionId,
                               Long hallId) {
    }

    // ------------------------------------------------------------------ registrants & visitors

    public record VisitorDto(Long id, String name, String email, String company, VisitorStatus status, String ticket,
                             boolean present, boolean registered, Long registrantId, String registrantName,
                             long registrantInvoiceNumber, LocalDateTime createdDate) {

        public static VisitorDto of(Visitor v) {
            Registrant r = v.getRegistrant();
            return new VisitorDto(v.getId(), v.getName(), v.getEmail(), v.getCompany(), v.getStatus(), v.getTicket(),
                v.isPresent(), v.isRegistered(), r != null ? r.getId() : null, r != null ? r.getName() : null,
                r != null ? r.getRealInvoiceNumber() : 0, v.getCreatedDate());
        }
    }

    public record VisitorInput(String name, String email, String company, VisitorStatus status, Long registrantId) {
    }

    public record RegistrantDto(Long id, String name, String email, boolean company, boolean student, String address,
                                String vatNumber, String eik, String mol, PaymentType paymentType,
                                long epayInvoiceNumber, long realInvoiceNumber, long proformaInvoiceNumber,
                                Integer branchYear, int visitorCount, List<VisitorDto> visitors,
                                LocalDateTime createdDate) {

        public static RegistrantDto of(Registrant r, boolean withVisitors) {
            return new RegistrantDto(r.getId(), r.getName(), r.getEmail(), r.isCompany(), r.isStudent(),
                r.getAddress(), r.getRawVatNumber(), r.getEik(), r.getMol(), r.getPaymentType(),
                r.getEpayInvoiceNumber(), r.getRealInvoiceNumber(), r.getProformaInvoiceNumber(),
                r.getBranch() != null ? r.getBranch().getYear() : null, r.getVisitors().size(),
                withVisitors ? r.getVisitors().stream().map(VisitorDto::of).toList() : null, r.getCreatedDate());
        }
    }

    public record RegistrantInput(String name, String email, boolean company, boolean student, String address,
                                  String vatNumber, String eik, String mol, PaymentType paymentType,
                                  Integer branchYear, Long realInvoiceNumber, Long proformaInvoiceNumber) {
    }

    public record VisitorStats(long requesting, long payed, long sponsored, long total) {
    }

    public record VisitorList(List<VisitorDto> visitors, VisitorStats stats) {
    }

    /** Editable invoice fields (the admin can fix them before the original invoice is sent). */
    public record InvoiceInput(String client, String clientAddress, String clientEIK, String clientVAT, String mol,
                               String invoiceDate, BigDecimal singlePriceWithVAT, String description) {
    }

    public record InvoiceDto(Long registrantId, String invoiceNumber, String invoiceDate, String client,
                             String clientAddress, String clientEIK, String clientVAT, String mol, String paymentType,
                             int passQty, BigDecimal singlePriceWithVAT, String description, BigDecimal totalPrice,
                             BigDecimal totalPriceVAT, BigDecimal totalPriceWithVAT) {
    }

    // ------------------------------------------------------------------ users, jobs, misc

    public record UserDto(Long id, String firstName, String lastName, String email, String phone, String type) {

        public static UserDto of(User u) {
            String type = u instanceof Speaker ? "Speaker" : u instanceof Sponsor ? "Sponsor" : "User";
            return new UserDto(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getPhone(), type);
        }
    }

    public record UserInput(String firstName, String lastName, String email, String phone, String password) {
    }

    public record JobDto(String jobId, String description, String status, String log, LocalDateTime completed) {

        public static JobDto of(BackgroundJob j) {
            return new JobDto(j.getJobId(), j.getDescription(), j.getStatus(), j.getLog(), j.getCompleted());
        }
    }

    public record SendResultDto(int sent, int failed, String jobId) {
    }

    public record MailInput(String subject, String content) {
    }

    public record Option(String value, String label) {
    }
}
