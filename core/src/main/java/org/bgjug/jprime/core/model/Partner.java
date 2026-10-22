package org.bgjug.jprime.core.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "Partner")
public class Partner extends AbstractEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "partnerPackage")
    private PartnerPackage partnerPackage;

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

    /** Legacy rows without a package are media partners. */
    public PartnerPackage getPartnerPackage() {
        return partnerPackage != null ? partnerPackage : PartnerPackage.MEDIA;
    }

    public void setPartnerPackage(PartnerPackage partnerPackage) {
        this.partnerPackage = partnerPackage;
    }
}
