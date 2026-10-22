package org.bgjug.jprime.core.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Renders the real Jasper templates (compiled at build time by the jasperreports-plugin). */
class PdfRenderingTest {

    @Test
    void rendersBulgarianCompanyInvoice() {
        InvoiceData data = new InvoiceData();
        data.setInvoiceNumber("7000000001");
        data.setInvoiceDate("01.03.2026");
        data.setInvoiceType(InvoiceData.PROFORMA_BG);
        data.setClient("Много дълго име на клиент");
        data.setClientAddress("София");
        data.setClientEIK("123456789");
        data.setClientVAT("BG123456789");
        data.setMol("Иван Иванов");
        data.setPaymentType("Банков превод");
        data.addInvoiceDetail(new InvoiceDetail(BigDecimal.valueOf(170), 2, "jPrime 2026 билет за конференция"));
        byte[] pdf = new InvoiceExporter().exportInvoice(data, true, InvoiceLanguage.BG, 2026);
        assertPdf(pdf);
    }

    @Test
    void rendersTicketAndQrCode() {
        TicketExporter exporter = new TicketExporter();
        TicketData data = new TicketData();
        data.setEvent("JPrime 2026");
        data.addDetail(new TicketDetail(UUID.randomUUID().toString(), "Duke Java", "Visitor"));
        assertPdf(exporter.exportTicket(data, InvoiceLanguage.EN));
        byte[] png = exporter.generateTicketQrCode(data);
        assertEquals((byte) 0x89, png[0]);
        assertEquals('P', png[1]);
    }

    private static void assertPdf(byte[] pdf) {
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}
