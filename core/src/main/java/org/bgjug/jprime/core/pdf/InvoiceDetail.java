package org.bgjug.jprime.core.pdf;

import static java.math.RoundingMode.HALF_EVEN;

import java.math.BigDecimal;

/** One invoice line. Field names are used by the Jasper templates - do not rename. */
public class InvoiceDetail {

    public static final BigDecimal VAT_RATIO = BigDecimal.valueOf(1.2D);

    private Integer idx;
    private BigDecimal singlePriceWithVAT;
    private Integer passQty;
    private String description;

    public InvoiceDetail(BigDecimal singlePriceWithVAT, Integer passQty, String description) {
        this.singlePriceWithVAT = singlePriceWithVAT;
        this.passQty = passQty;
        this.description = description;
    }

    public Integer getIdx() {
        return idx;
    }

    public void setIdx(Integer idx) {
        this.idx = idx;
    }

    public Integer getPassQty() {
        return passQty;
    }

    public void setPassQty(Integer passQty) {
        this.passQty = passQty;
    }

    public BigDecimal getSinglePriceWithVAT() {
        return singlePriceWithVAT;
    }

    public void setSinglePriceWithVAT(BigDecimal singlePriceWithVAT) {
        this.singlePriceWithVAT = singlePriceWithVAT;
    }

    /** Single price without VAT. */
    public BigDecimal getPrice() {
        return getTotalPrice().divide(BigDecimal.valueOf(passQty), 2, HALF_EVEN);
    }

    /** Total without VAT. */
    public BigDecimal getTotalPrice() {
        return singlePriceWithVAT.multiply(BigDecimal.valueOf(passQty)).divide(VAT_RATIO, 2, HALF_EVEN);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
