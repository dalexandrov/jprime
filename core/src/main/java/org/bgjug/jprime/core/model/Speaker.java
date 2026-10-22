package org.bgjug.jprime.core.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import org.apache.commons.lang3.StringUtils;

@Entity
@DiscriminatorValue("Speaker")
public class Speaker extends User {

    @Column(name = "bio", length = 3000, columnDefinition = "TEXT")
    private String bio;

    @Column(name = "headline")
    private String headline;

    @Column(name = "twitter")
    private String twitter;

    @Column(name = "bsky")
    private String bsky;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "picture", columnDefinition = "LONGBLOB")
    private byte[] picture;

    @OneToMany(mappedBy = "speaker", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private Set<Submission> submissions = new HashSet<>();

    @OneToMany(mappedBy = "coSpeaker", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private Set<Submission> coSpeakerSubmissions = new HashSet<>();

    // ---- per-branch flags computed by updateFlags(Branch) (not persisted) ----
    @Transient
    private SubmissionStatus status;
    @Transient
    private boolean featured;
    @Transient
    private Branch lastBranch;
    @Transient
    private String speakerType;
    @Transient
    private int numberOfSubmissions;

    public Speaker() {
    }

    public Speaker(String firstName, String lastName, String email, String headline, String twitter) {
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
        this.headline = headline;
        this.twitter = twitter;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getTwitter() {
        return twitter;
    }

    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    public String getBsky() {
        return bsky;
    }

    public void setBsky(String bsky) {
        this.bsky = bsky;
    }

    public byte[] getPicture() {
        return picture;
    }

    public void setPicture(byte[] picture) {
        this.picture = picture;
    }

    public Set<Submission> getSubmissions() {
        return submissions;
    }

    public Set<Submission> getCoSpeakerSubmissions() {
        return coSpeakerSubmissions;
    }

    /** All submissions (as speaker or co-speaker), oldest branch first. */
    public List<Submission> getAllSubmissions() {
        return Stream.concat(submissions.stream(), coSpeakerSubmissions.stream())
            .sorted(Comparator.comparing(s -> s.getBranch() != null ? s.getBranch().getYear() : 0))
            .toList();
    }

    public List<Submission> branchSubmissions(Branch branch) {
        return Stream.concat(submissions.stream(), coSpeakerSubmissions.stream())
            .filter(s -> Objects.equals(s.getBranch(), branch))
            .toList();
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public Branch getLastBranch() {
        return lastBranch;
    }

    public String getSpeakerType() {
        return speakerType;
    }

    public int getNumberOfSubmissions() {
        return numberOfSubmissions;
    }

    /** Computes the transient flags (status, featured, type...) for the given branch. Same logic as before. */
    public Speaker updateFlags(Branch branch) {
        Collection<Submission> current = branchSubmissions(branch);
        this.featured = current.stream().anyMatch(s -> Boolean.TRUE.equals(s.getFeatured()));
        this.status = current.stream()
            .map(Submission::getStatus)
            .filter(Objects::nonNull)
            .max(Comparator.comparing(SubmissionStatus::ordinal))
            .orElse(SubmissionStatus.ABORTED);
        this.lastBranch = Stream.concat(submissions.stream(), coSpeakerSubmissions.stream())
            .map(Submission::getBranch)
            .filter(Objects::nonNull)
            .max(Comparator.comparing(Branch::getYear))
            .orElse(null);
        this.speakerType = String.join("/", current.stream()
            .map(s -> s.getSpeaker() != null && Objects.equals(s.getSpeaker().getId(), getId()) ? "Speaker" :
                      "Co-Speaker")
            .distinct()
            .sorted(Comparator.reverseOrder())
            .toList());
        this.numberOfSubmissions = current.size();
        return this;
    }

    /** Normalises the X/Twitter handle to just the user name, like the legacy site did. */
    public String getTwitterHandle() {
        if (StringUtils.isBlank(twitter)) {
            return null;
        }
        String handle = twitter.trim();
        for (String prefix : List.of("@", "https://", "http://", "www.", "twitter.com/", "x.com/")) {
            if (handle.startsWith(prefix)) {
                handle = handle.substring(prefix.length());
            }
        }
        return handle.contains("/") || handle.isBlank() ? null : handle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Speaker speaker) || !super.equals(o)) {
            return false;
        }
        return Objects.equals(twitter, speaker.twitter) && Objects.equals(bsky, speaker.bsky);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(twitter, bsky);
    }
}
