package org.bgjug.jprime.core.pdf;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Everything printed on an invoice. Property names match the Jasper report parameters. */
public class InvoiceData {

    public static final String ORIGINAL_BG = "Оригинал";
    public static final String PROFORMA_BG = "Проформа";

    private String invoiceNumber;
    private String invoiceDate;
    private String invoiceType;
    private String client;
    private String clientAddress;
    private String clientEIK;
    private String clientVAT;
    private String mol;
    private String paymentType;
    private Long registrantId;
    private final List<InvoiceDetail> invoiceDetails = new ArrayList<>();

    public BigDecimal getTotalPrice() {
        return invoiceDetails.stream().map(InvoiceDetail::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPriceWithVAT() {
        return invoiceDetails.stream()
            .map(d -> d.getSinglePriceWithVAT().multiply(BigDecimal.valueOf(d.getPassQty())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPriceVAT() {
        return getTotalPriceWithVAT().subtract(getTotalPrice());
    }

    public void addInvoiceDetail(InvoiceDetail detail) {
        invoiceDetails.add(detail);
        detail.setIdx(invoiceDetails.size());
    }

    public List<InvoiceDetail> getInvoiceDetails() {
        return invoiceDetails;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public String getClientEIK() {
        return clientEIK;
    }

    public void setClientEIK(String clientEIK) {
        this.clientEIK = clientEIK;
    }

    public String getClientVAT() {
        return clientVAT;
    }

    public void setClientVAT(String clientVAT) {
        this.clientVAT = clientVAT;
    }

    public String getMol() {
        return mol;
    }

    public void setMol(String mol) {
        this.mol = mol;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public Long getRegistrantId() {
        return registrantId;
    }

    public void setRegistrantId(Long registrantId) {
        this.registrantId = registrantId;
    }
}
