package org.bgjug.jprime.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.bgjug.jprime.core.model.EpayResponse;
import org.junit.jupiter.api.Test;

class EpayServiceTest {

    private EpayService demoService() {
        EpayService service = new EpayService();
        service.key = Optional.empty();
        service.kin = "8323189568";
        service.url = "https://www.epay.bg/v3main/paylogin";
        service.real = false;
        return service;
    }

    @Test
    void roundTripWithTheDemoKey() {
        EpayService service = demoService();
        String message = "INVOICE=8100000001:STATUS=PAID:PAY_TIME=20260301120000:STAN=123456:BCODE=ABC123 ";
        String encoded = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
        EpayResponse response = service.decrypt(encoded, service.checksum(encoded, false));
        assertTrue(response.isValid());
        assertEquals(EpayResponse.Status.PAID, response.getStatus());
        assertEquals("ABC123", response.getBorikaAuthorizationCode());
        assertEquals("123456", response.getTransactionNumberIfPaidByCard());
        assertEquals("INVOICE=8100000001:STATUS=OK", response.getEpayAnswer());
    }

    @Test
    void payloadIsSigned() {
        EpayService.EpayPayload payload = demoService().encrypt(2, 8100000002L, 200, "01.08.2026");
        String decoded = new String(Base64.getDecoder().decode(payload.encoded()), StandardCharsets.UTF_8);
        assertTrue(decoded.contains("MIN=" + EpayService.DEMO_KIN));
        assertTrue(decoded.contains("DESCR=2 jPrime.io tickets"));
        assertEquals(EpayService.DEMO_URL, payload.epayUrl());
        assertEquals(40, payload.checksum().length());
    }
}
