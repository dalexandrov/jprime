package org.bgjug.jprime.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;

/** A conference attendee (one ticket). */
@Entity
@Table(name = "Visitor")
public class Visitor extends AbstractEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "registrant", nullable = false, foreignKey = @ForeignKey(name = "FK_registrant_visitor"))
    private Registrant registrant;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "company")
    private String company;

    /** Ticket number (UUID) - set when the ticket is e-mailed. */
    @Column(name = "ticket")
    private String ticket;

    /** Registered for the raffle ("I am here"). */
    @Column(name = "isPresent", nullable = false)
    private boolean present;

    /** Checked-in at the registration desk (mobile app). */
    @Column(name = "isRegistered", nullable = false)
    private boolean registered;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private VisitorStatus status;

    public Visitor() {
    }

    public Visitor(String name, String email, String company) {
        setName(name);
        this.email = email;
        setCompany(company);
    }

    public Registrant getRegistrant() {
        return registrant;
    }

    public void setRegistrant(Registrant registrant) {
        this.registrant = registrant;
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
        this.email = email != null ? email.trim() : null;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company != null ? company.trim() : null;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public VisitorStatus getStatus() {
        return status;
    }

    public void setStatus(VisitorStatus status) {
        this.status = status;
    }

    /** The visitor's e-mail, falling back to the registrant's one. */
    public String anyEmail() {
        if (StringUtils.isNotBlank(email)) {
            return email;
        }
        return registrant != null ? registrant.getEmail() : null;
    }

    @Override
    public String toString() {
        return "Visitor{name='" + name + "', email='" + email + "', company='" + company + "', present=" + present
            + ", ticket='" + ticket + "', registered=" + registered + '}';
    }
}
