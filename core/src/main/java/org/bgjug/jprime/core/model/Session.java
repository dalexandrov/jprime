package org.bgjug.jprime.core.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * An agenda slot. Either references an accepted {@link Submission} (a talk) or has only a title (Break, TBA...).
 */
@Entity
@Table(name = "Session")
public class Session extends AbstractEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "submission", foreignKey = @ForeignKey(name = "fk_session_submission"))
    private Submission submission;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hall", foreignKey = @ForeignKey(name = "fk_session_hall"))
    private VenueHall hall;

    public Session() {
    }

    public Session(Submission submission, LocalDateTime startTime, LocalDateTime endTime, VenueHall hall) {
        this.submission = submission;
        this.startTime = startTime;
        this.endTime = endTime;
        this.hall = hall;
    }

    /** The submission title for talks, the stored title for breaks. */
    public String getTitle() {
        return submission != null ? submission.getTitle() : title;
    }

    /** The raw value of the title column. */
    public String getRawTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public VenueHall getHall() {
        return hall;
    }

    public void setHall(VenueHall hall) {
        this.hall = hall;
    }

    public boolean isTalk() {
        return submission != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Session session)) {
            return false;
        }
        return Objects.equals(getTitle(), session.getTitle()) && Objects.equals(startTime, session.startTime)
            && Objects.equals(endTime, session.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), startTime, endTime);
    }

    @Override
    public String toString() {
        return "Session{title='" + getTitle() + "', startTime=" + startTime + ", endTime=" + endTime + '}';
    }
}
