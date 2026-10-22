package org.bgjug.jprime.core.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class InvoiceDataTest {

    @Test
    void vatIsCalculatedLikeBefore() {
        InvoiceData data = new InvoiceData();
        data.addInvoiceDetail(new InvoiceDetail(BigDecimal.valueOf(120), 3, "jPrime 2026 билет за конференция"));

        InvoiceDetail detail = data.getInvoiceDetails().getFirst();
        assertEquals(1, detail.getIdx());
        assertEquals(new BigDecimal("100.00"), detail.getPrice());
        assertEquals(new BigDecimal("300.00"), detail.getTotalPrice());
        assertEquals(new BigDecimal("300.00"), data.getTotalPrice());
        assertEquals(0, new BigDecimal("360").compareTo(data.getTotalPriceWithVAT()));
        assertEquals(0, new BigDecimal("60").compareTo(data.getTotalPriceVAT()));
    }
}
