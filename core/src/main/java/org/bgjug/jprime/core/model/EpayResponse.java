package org.bgjug.jprime.core.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Data received from ePay.bg on the back channel. Embedded in the {@code Registrant} table.
 */
@Embeddable
public class EpayResponse {

    public enum Status {
        PAID, DENIED, EXPIRED
    }

    @Column(name = "isValid")
    private Boolean isValid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "date")
    private LocalDateTime date;

    /** BCODE */
    @Column(name = "borikaAuthorizationCode")
    private String borikaAuthorizationCode;

    /** STAN */
    @Column(name = "transactionNumberIfPaidByCard")
    private String transactionNumberIfPaidByCard;

    /** INVOICE=123456:STATUS=OK|ERR */
    @Column(name = "epayAnswer")
    private String epayAnswer;

    public Boolean getIsValid() {
        return isValid;
    }

    public boolean isValid() {
        return Boolean.TRUE.equals(isValid);
    }

    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getBorikaAuthorizationCode() {
        return borikaAuthorizationCode;
    }

    public void setBorikaAuthorizationCode(String borikaAuthorizationCode) {
        this.borikaAuthorizationCode = borikaAuthorizationCode;
    }

    public String getTransactionNumberIfPaidByCard() {
        return transactionNumberIfPaidByCard;
    }

    public void setTransactionNumberIfPaidByCard(String transactionNumberIfPaidByCard) {
        this.transactionNumberIfPaidByCard = transactionNumberIfPaidByCard;
    }

    public String getEpayAnswer() {
        return epayAnswer;
    }

    public void setEpayAnswer(String epayAnswer) {
        this.epayAnswer = epayAnswer;
    }
}
