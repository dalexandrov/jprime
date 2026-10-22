package org.bgjug.jprime.core.model;

import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "Submission")
public class Submission extends AbstractEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank
    @Size(max = 10000)
    @Column(name = "description", length = 10000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "level")
    private SessionLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private SessionType type = SessionType.CONFERENCE_SESSION;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "speaker", nullable = false, foreignKey = @ForeignKey(name = "fk_submission_speaker"))
    private Speaker speaker;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "coSpeaker", foreignKey = @ForeignKey(name = "fk_submission_coSpeaker"))
    private Speaker coSpeaker;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch", referencedColumnName = "label", foreignKey = @ForeignKey(name = "fk_submission_branch"))
    private Branch branch;

    @Column(name = "featured")
    private Boolean featured = false;

    public Submission() {
    }

    public Submission(Branch branch) {
        this.branch = branch;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public SessionLevel getLevel() {
        return level;
    }

    public void setLevel(SessionLevel level) {
        this.level = level;
    }

    public SessionType getType() {
        return type;
    }

    public void setType(SessionType type) {
        this.type = type;
    }

    public Speaker getSpeaker() {
        return speaker;
    }

    public void setSpeaker(Speaker speaker) {
        this.speaker = speaker;
    }

    public Speaker getCoSpeaker() {
        return coSpeaker;
    }

    public void setCoSpeaker(Speaker coSpeaker) {
        this.coSpeaker = coSpeaker;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public boolean isFeatured() {
        return Boolean.TRUE.equals(featured);
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    /** Type with the legacy default for old rows that have no type. */
    public SessionType getTypeOrDefault() {
        return type != null ? type : SessionType.CONFERENCE_SESSION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Submission other)) {
            return false;
        }
        return Objects.equals(getId(), other.getId()) && Objects.equals(title, other.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }

    @Override
    public String toString() {
        return "Submission{title='" + title + "', status=" + status + ", level=" + level + ", type=" + type
            + ", branch=" + branch + '}';
    }
}
