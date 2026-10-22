package org.bgjug.jprime.core.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Speaker;
import org.bgjug.jprime.core.model.Submission;
import org.bgjug.jprime.core.model.SubmissionStatus;
import org.bgjug.jprime.core.model.User;
import org.bgjug.jprime.core.repository.SpeakerRepository;
import org.bgjug.jprime.core.repository.SubmissionRepository;
import org.bgjug.jprime.core.repository.UserRepository;
import org.jboss.logging.Logger;

/**
 * Call for papers: creating / updating submissions and their speakers, plus the related e-mails.
 */
@ApplicationScoped
public class CfpService {

    private static final Logger LOG = Logger.getLogger(CfpService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Inject
    SubmissionRepository submissionRepository;

    @Inject
    SpeakerRepository speakerRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ThumbnailService thumbnailService;

    @Inject
    BranchService branchService;

    @Inject
    MailService mailService;

    @Inject
    MailTemplates mailTemplates;

    /**
     * Validates the input and stores a new submission for the given branch.
     *
     * @throws ValidationFailedException with field names like {@code title}, {@code speaker.email}
     */
    @Transactional
    public Submission submit(SubmissionInput input, Branch branch) {
        validateSubmission(input);
        Submission submission = new Submission(branchService.reference(branch));
        apply(submission, input);
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submissionRepository.persist(submission);
        LOG.infof("[submitTalk] %s", submission);
        return submission;
    }

    /**
     * Admin edit of an existing submission (speakers are re-resolved by e-mail like for new submissions).
     */
    @Transactional
    public Submission update(long id, SubmissionInput input, Branch branch, Boolean featured) {
        validateSubmission(input);
        Submission submission = submissionRepository.findByIdOptional(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown submission " + id));
        apply(submission, input);
        if (branch != null) {
            submission.setBranch(branchService.reference(branch));
        }
        if (featured != null) {
            submission.setFeatured(featured);
        }
        return submission;
    }

    private void apply(Submission submission, SubmissionInput input) {
        submission.setTitle(input.title().trim());
        submission.setDescription(input.description());
        submission.setLevel(input.level());
        submission.setType(input.type());
        submission.setSpeaker(resolveSpeaker(input.speaker()));
        submission.setCoSpeaker(input.hasCoSpeaker() ? resolveSpeaker(input.coSpeaker()) : null);
    }

    private void validateSubmission(SubmissionInput input) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (StringUtils.isBlank(input.title())) {
            errors.put("title", "Title is required!");
        } else if (input.title().length() > 255) {
            errors.put("title", "Title is too long (max 255 characters)");
        }
        if (StringUtils.isBlank(input.description())) {
            errors.put("description", "Abstract is required!");
        } else if (input.description().length() > 10000) {
            errors.put("description", "Abstract is too long (max 10000 characters)");
        }
        validateSpeaker(input.speaker(), "speaker", errors);
        if (input.hasCoSpeaker()) {
            validateSpeaker(input.coSpeaker(), "coSpeaker", errors);
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void validateSpeaker(SpeakerInput speaker, String role, Map<String, String> errors) {
        if (speaker == null || StringUtils.isBlank(speaker.email()) || !EMAIL.matcher(speaker.email().trim()).matches()) {
            errors.put(role + ".email", "Invalid Email!");
            return;
        }
        Optional<Speaker> existing = speakerRepository.findByEmail(speaker.email().trim());
        if (existing.isPresent()) {
            return; // only the provided fields will be updated
        }
        Optional<User> user = userRepository.findByEmail(speaker.email().trim());
        if (user.isPresent() && (!StringUtils.equalsIgnoreCase(user.get().getFirstName(), speaker.firstName())
            || !StringUtils.equalsIgnoreCase(user.get().getLastName(), speaker.lastName()))) {
            errors.put(role + ".email", "Invalid user information! Can't store the data.");
            return;
        }
        if (StringUtils.isBlank(speaker.firstName())) {
            errors.put(role + ".firstName", "First name is required!");
        } else if (StringUtils.isBlank(speaker.lastName())) {
            errors.put(role + ".lastName", "Last name is required!");
        } else if (StringUtils.isBlank(speaker.bio())) {
            errors.put(role + ".bio", "Bio is required!");
        }
    }

    /**
     * Finds the speaker by e-mail and updates the provided fields, converts an existing plain user into a speaker,
     * or creates a new speaker.
     */
    private Speaker resolveSpeaker(SpeakerInput input) {
        String email = input.email().trim();
        Optional<Speaker> existing = speakerRepository.findByEmail(email);
        if (existing.isEmpty()) {
            Optional<User> user = userRepository.findByEmail(email);
            if (user.isPresent()) {
                userRepository.convertToSpeaker(user.get().getId());
                userRepository.getEntityManager().detach(user.get());
                existing = speakerRepository.findByEmail(email);
            }
        }
        Speaker speaker = existing.orElseGet(() -> {
            Speaker s = new Speaker();
            s.setEmail(email);
            speakerRepository.persist(s);
            return s;
        });
        copyNonBlank(input, speaker);
        return speaker;
    }

    private void copyNonBlank(SpeakerInput input, Speaker speaker) {
        if (StringUtils.isNotBlank(input.firstName())) {
            speaker.setFirstName(input.firstName().trim());
        }
        if (StringUtils.isNotBlank(input.lastName())) {
            speaker.setLastName(input.lastName().trim());
        }
        if (StringUtils.isNotBlank(input.headline())) {
            speaker.setHeadline(input.headline().trim());
        }
        if (StringUtils.isNotBlank(input.bio())) {
            speaker.setBio(input.bio());
        }
        if (StringUtils.isNotBlank(input.twitter())) {
            String twitter = input.twitter().trim();
            speaker.setTwitter(twitter.startsWith("@") ? twitter.substring(1) : twitter);
        }
        if (StringUtils.isNotBlank(input.bsky())) {
            speaker.setBsky(input.bsky().trim());
        }
        if (input.hasPicture()) {
            speaker.setPicture(thumbnailService.speakerPicture(input.picture()));
        }
    }

    // ------------------------------------------------------------------ status changes

    @Transactional
    public Submission changeStatus(long id, SubmissionStatus status) {
        Submission submission = submissionRepository.findByIdOptional(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown submission " + id));
        submission.setStatus(status);
        return submission;
    }

    /** Accepts and e-mails the speakers. Returns false when the e-mail could not be sent. */
    @Transactional
    public boolean accept(long id) {
        Submission submission = changeStatus(id, SubmissionStatus.ACCEPTED);
        return sendStatusMail(submission, MailTemplates.SUBMISSION_ACCEPTED);
    }

    @Transactional
    public boolean reject(long id) {
        Submission submission = changeStatus(id, SubmissionStatus.REJECTED);
        return sendStatusMail(submission, MailTemplates.SUBMISSION_REJECTED);
    }

    /** Only SUBMITTED submissions can be deleted. */
    @Transactional
    public void delete(long id) {
        Submission submission = submissionRepository.findByIdOptional(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown submission " + id));
        if (submission.getStatus() != SubmissionStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED submissions can be deleted!");
        }
        submissionRepository.delete(submission);
    }

    private boolean sendStatusMail(Submission submission, String template) {
        String subject = "Your jPrime talk proposal status";
        int year = submission.getBranch() != null ? submission.getBranch().getYear() :
                   branchService.getCurrentBranch().getYear();
        String dueDate = submission.getBranch() != null && submission.getBranch().getCfpCloseDate() != null ?
                         DateFormats.withMonthAndYear(submission.getBranch().getCfpCloseDate().plusMonths(1)) : "";
        boolean ok = mailService.sendQuietly(submission.getSpeaker().getEmail(), subject,
            mailTemplates.render(template, Map.of("firstName", nn(submission.getSpeaker().getFirstName()), "title",
                submission.getTitle(), "year", year, "dueDate", dueDate)));
        if (submission.getCoSpeaker() != null) {
            ok &= mailService.sendQuietly(submission.getCoSpeaker().getEmail(), subject,
                mailTemplates.render(template, Map.of("firstName", nn(submission.getCoSpeaker().getFirstName()),
                    "title", submission.getTitle(), "year", year, "dueDate", dueDate)));
        }
        return ok;
    }

    /** "Thank you" to the speakers and "new proposal" to the organizers. */
    public boolean sendNotificationEmails(Submission submission) {
        String thanks = mailTemplates.render(MailTemplates.SUBMISSION_RECEIVED, Map.of());
        boolean ok = mailService.sendQuietly(submission.getSpeaker().getEmail(), "jPrime talk proposal", thanks);
        if (submission.getCoSpeaker() != null) {
            ok &= mailService.sendQuietly(submission.getCoSpeaker().getEmail(), "jPrime talk proposal", thanks);
        }
        ok &= mailService.sendQuietly(mailService.conferenceAddress(), "New talk proposal",
            mailTemplates.render(MailTemplates.SUBMISSION_NEW, newSubmissionData(submission)));
        return ok;
    }

    private static Map<String, Object> newSubmissionData(Submission s) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("title", s.getTitle());
        data.put("description", nn(s.getDescription()));
        data.put("level", s.getLevel() != null ? s.getLevel().getLabel() : "");
        data.put("type", s.getTypeOrDefault().getLabel());
        data.put("speakerName", s.getSpeaker().getName());
        data.put("speakerBio", nn(s.getSpeaker().getBio()));
        data.put("coSpeakerName", s.getCoSpeaker() != null ? s.getCoSpeaker().getName() : "");
        data.put("coSpeakerBio", s.getCoSpeaker() != null ? nn(s.getCoSpeaker().getBio()) : "");
        return data;
    }

    private static String nn(String value) {
        return value == null ? "" : value;
    }

    @Transactional
    public boolean sendNotificationEmails(long submissionId) {
        return sendNotificationEmails(submissionRepository.findById(submissionId));
    }
}
