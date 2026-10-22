package org.bgjug.jprime.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** jProfessionals attendee, imported from an Eventbrite CSV. */
@Entity
@Table(name = "VisitorJPro")
public class VisitorJPro extends AbstractEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "company")
    private String company;

    @Column(name = "isPresent", nullable = false)
    private boolean present;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private VisitorStatus status;

    public VisitorJPro() {
    }

    public VisitorJPro(String name, String email, String company) {
        setName(name);
        this.email = email;
        setCompany(company);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company != null ? company.trim() : null;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public VisitorStatus getStatus() {
        return status;
    }

    public void setStatus(VisitorStatus status) {
        this.status = status;
    }
}
