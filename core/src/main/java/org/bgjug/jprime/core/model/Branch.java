package org.bgjug.jprime.core.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One conference edition ("YEAR_2026"). The primary key is the {@code label} column which is always derived from
 * the year.
 * <p>
 * Note: the legacy entity used property access, which is why the audit columns of this table are camelCase
 * ({@code createdBy}, {@code createdDate}, ...) unlike all other tables.
 */
@Entity
@Table(name = "Branch")
@EntityListeners(AuditListener.class)
public class Branch implements Auditable, Serializable {

    private static final String BRANCH_PREFIX = "YEAR_";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @Column(name = "label", length = 16)
    private String label;

    @Column(name = "branch_year", nullable = false)
    private int year;

    @Column(name = "startDate")
    private LocalDateTime startDate;

    /** Stored as NUMERIC nanoseconds - the Hibernate 6+ default for {@link Duration}. */
    @Column(name = "duration")
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Duration duration;

    @Column(name = "cfpOpenDate")
    private LocalDateTime cfpOpenDate;

    @Column(name = "cfpCloseDate")
    private LocalDateTime cfpCloseDate;

    /** JSON array of {@link SponsorPackage} names. */
    @Column(name = "soldOutPackages", length = 1024)
    private String soldOutPackages;

    @Column(name = "soldOut", nullable = false)
    private boolean soldOut;

    @Column(name = "currentBranch", nullable = false)
    private boolean currentBranch;

    @Column(name = "agendaPublished", nullable = false)
    private boolean agendaPublished;

    @Column(name = "createdBy")
    private String createdBy;

    @Column(name = "createdDate")
    private LocalDateTime createdDate;

    @Column(name = "lastModifiedBy")
    private String lastModifiedBy;

    @Column(name = "lastModifiedDate")
    private LocalDateTime lastModifiedDate;

    public Branch() {
    }

    public Branch(int year) {
        setYear(year);
    }

    public static String labelOf(int year) {
        return BRANCH_PREFIX + year;
    }

    public String getLabel() {
        return label;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
        this.label = labelOf(year);
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public LocalDateTime getCfpOpenDate() {
        return cfpOpenDate;
    }

    public void setCfpOpenDate(LocalDateTime cfpOpenDate) {
        this.cfpOpenDate = cfpOpenDate;
    }

    public LocalDateTime getCfpCloseDate() {
        return cfpCloseDate;
    }

    public void setCfpCloseDate(LocalDateTime cfpCloseDate) {
        this.cfpCloseDate = cfpCloseDate;
    }

    public String getSoldOutPackages() {
        return soldOutPackages;
    }

    public void setSoldOutPackages(String soldOutPackages) {
        this.soldOutPackages = soldOutPackages;
    }

    public Set<SponsorPackage> soldOutPackageSet() {
        if (StringUtils.isBlank(soldOutPackages)) {
            return Collections.emptySet();
        }
        try {
            Set<SponsorPackage> parsed = OBJECT_MAPPER.readValue(soldOutPackages, new TypeReference<>() {
            });
            return parsed.isEmpty() ? EnumSet.noneOf(SponsorPackage.class) : EnumSet.copyOf(parsed);
        } catch (JsonProcessingException e) {
            throw new JPrimeException(e);
        }
    }

    public void soldOutPackageSet(Set<SponsorPackage> packages) {
        try {
            this.soldOutPackages = packages != null ? OBJECT_MAPPER.writeValueAsString(packages) : "";
        } catch (JsonProcessingException e) {
            throw new JPrimeException(e);
        }
    }

    public boolean isPackageSoldOut(SponsorPackage sponsorPackage) {
        return soldOutPackageSet().contains(sponsorPackage);
    }

    public boolean isSoldOut() {
        return soldOut;
    }

    public void setSoldOut(boolean soldOut) {
        this.soldOut = soldOut;
    }

    public boolean isCurrentBranch() {
        return currentBranch;
    }

    public void setCurrentBranch(boolean currentBranch) {
        this.currentBranch = currentBranch;
    }

    public boolean isAgendaPublished() {
        return agendaPublished;
    }

    public void setAgendaPublished(boolean agendaPublished) {
        this.agendaPublished = agendaPublished;
    }

    /** CFP is open right now. */
    @Transient
    public boolean isCfpOpen() {
        LocalDateTime now = LocalDateTime.now();
        return cfpOpenDate != null && cfpCloseDate != null && cfpOpenDate.isBefore(now) && cfpCloseDate.isAfter(now);
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    @Override
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    @Override
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    @Override
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Branch that && year == that.year;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(year);
    }

    @Override
    public String toString() {
        return Integer.toString(year);
    }
}
