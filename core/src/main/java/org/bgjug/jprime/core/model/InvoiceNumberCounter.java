package org.bgjug.jprime.core.model;

/**
 * The three single-row tables holding the invoice number counters. Accessed with native SQL and a row lock
 * (see {@code NumberGeneratorService}) so that the site and the admin app never hand out the same number.
 */
public enum InvoiceNumberCounter {
    EPAY("EpayInvoiceNumberGenerator", 8100000001L),
    REAL("RealInvoiceNumberGenerator", 8000000001L),
    PROFORMA("ProformaInvoiceNumberGenerator", 7000000001L);

    private final String table;
    private final long initialValue;

    InvoiceNumberCounter(String table, long initialValue) {
        this.table = table;
        this.initialValue = initialValue;
    }

    public String table() {
        return table;
    }

    public long initialValue() {
        return initialValue;
    }
}
