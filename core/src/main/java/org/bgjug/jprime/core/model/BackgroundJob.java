package org.bgjug.jprime.core.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Long running admin operation (e.g. sending tickets) and its progress log. */
@Entity
@Table(name = "BackgroundJob")
public class BackgroundJob {

    @Id
    @Column(name = "jobId", length = 40)
    private String jobId;

    @Column(name = "description", length = 128, nullable = false)
    private String description;

    @Column(name = "status", length = 128)
    private String status;

    @Column(name = "log", columnDefinition = "LONGTEXT")
    private String log;

    @Column(name = "completed")
    private LocalDateTime completed;

    public BackgroundJob() {
    }

    public BackgroundJob(String jobId, String description) {
        this.jobId = jobId;
        this.description = description;
    }

    public void appendToJobLog(String message) {
        log = log != null ? log + System.lineSeparator() + message : message;
    }

    public String getJobId() {
        return jobId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLog() {
        return log;
    }

    public LocalDateTime getCompleted() {
        return completed;
    }

    public void setCompleted(LocalDateTime completed) {
        this.completed = completed;
    }
}
