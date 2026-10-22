package org.bgjug.jprime.core.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ResetPasswordToken", uniqueConstraints = @UniqueConstraint(name = "UNQ_TOKEN", columnNames = "tokenId"))
public class ResetPasswordToken extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", foreignKey = @ForeignKey(name = "fk_token_user"))
    private User owner;

    /** SHA-512 hex of the token that was e-mailed. */
    @Column(name = "tokenId", nullable = false)
    private String tokenId;

    @Column(name = "used", nullable = false)
    private boolean used;

    public ResetPasswordToken() {
    }

    public ResetPasswordToken(User owner, String tokenIdHash) {
        this.owner = owner;
        this.tokenId = tokenIdHash;
        setCreatedDate(LocalDateTime.now());
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
