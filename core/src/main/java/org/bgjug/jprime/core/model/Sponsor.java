package org.bgjug.jprime.core.model;

import java.util.Objects;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;

@Entity
@DiscriminatorValue("Sponsor")
public class Sponsor extends User {

    @Enumerated(EnumType.STRING)
    @Column(name = "sponsorPackage")
    private SponsorPackage sponsorPackage;

    @Column(name = "companyName")
    private String companyName;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "logo", columnDefinition = "LONGBLOB")
    private byte[] logo;

    @Column(name = "companyWebsite")
    private String companyWebsite;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private Boolean active;

    public Sponsor() {
    }

    public Sponsor(SponsorPackage sponsorPackage, String companyName, String companyWebsite, String email) {
        this.sponsorPackage = sponsorPackage;
        this.companyName = companyName;
        this.companyWebsite = companyWebsite;
        this.active = true;
        setEmail(email);
    }

    public SponsorPackage getSponsorPackage() {
        return sponsorPackage;
    }

    public void setSponsorPackage(SponsorPackage sponsorPackage) {
        this.sponsorPackage = sponsorPackage;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public byte[] getLogo() {
        return logo;
    }

    public void setLogo(byte[] logo) {
        this.logo = logo;
    }

    public String getCompanyWebsite() {
        return companyWebsite;
    }

    public void setCompanyWebsite(String companyWebsite) {
        this.companyWebsite = companyWebsite;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Sponsor sponsor && Objects.equals(companyName, sponsor.companyName)
            && Objects.equals(getId(), sponsor.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(companyName);
    }
}
