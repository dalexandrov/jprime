package org.bgjug.jprime.core.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TicketPrice")
public class TicketPrice extends AbstractEntity {

    @Column(name = "price", precision = 38, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticketType", length = 32)
    private TicketType ticketType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch", referencedColumnName = "label", foreignKey = @ForeignKey(name = "fk_ticket_price_branch"))
    private Branch branch;

    @Column(name = "validFrom")
    private LocalDateTime validFrom;

    @Column(name = "validUntil")
    private LocalDateTime validUntil;

    public TicketPrice() {
    }

    public TicketPrice(BigDecimal price, TicketType ticketType, Branch branch) {
        this.price = price;
        this.ticketType = ticketType;
        this.branch = branch;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public void setTicketType(TicketType ticketType) {
        this.ticketType = ticketType;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }
}
