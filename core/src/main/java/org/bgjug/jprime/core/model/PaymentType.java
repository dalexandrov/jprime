package org.bgjug.jprime.core.model;

/**
 * Stored as ORDINAL in {@code Registrant.paymentType} (INT column) - never reorder the constants!
 */
public enum PaymentType {
    EPAY_ACCOUNT("ePay account", "epay.bg"),
    EPAY_CREDIT_CARD("Bank card", "Банкова карта"),
    BANK_TRANSFER("Direct bank transfer", "Банков превод");

    private final String value;
    private final String bulgarianValue;

    PaymentType(String value, String bulgarianValue) {
        this.value = value;
        this.bulgarianValue = bulgarianValue;
    }

    public String getValue() {
        return value;
    }

    public String getBulgarianValue() {
        return bulgarianValue;
    }

    @Override
    public String toString() {
        return value;
    }
}
