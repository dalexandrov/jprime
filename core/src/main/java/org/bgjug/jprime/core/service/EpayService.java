package org.bgjug.jprime.core.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.EpayResponse;
import org.bgjug.jprime.core.model.JPrimeException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * ePay.bg payload signing / verification (ported 1:1 from the legacy {@code EpayUtil}). The legacy site no longer
 * offered ePay payments in the UI (only bank transfer), so this is kept for when online payments are re-enabled.
 * See https://www.epay.bg/v3main/img/front/tech_wire.pdf
 */
@ApplicationScoped
public class EpayService {

    public static final String DEMO_KEY = "KQUND3A923RX7PEW3WI9CAEK4YGWBEVIO2ASJELWUUM24R0SNGFFW02CA7GC0BMY";
    public static final String DEMO_KIN = "D990264495";
    public static final String DEMO_URL = "https://demo.epay.bg/";

    @ConfigProperty(name = "jprime.epay.key")
    Optional<String> key;

    @ConfigProperty(name = "jprime.epay.kin", defaultValue = "8323189568")
    String kin;

    @ConfigProperty(name = "jprime.epay.url", defaultValue = "https://www.epay.bg/v3main/paylogin")
    String url;

    @ConfigProperty(name = "jprime.epay.real", defaultValue = "false")
    boolean real;

    /** Form fields to post to ePay. */
    public record EpayPayload(String encoded, String checksum, String epayUrl) {
    }

    public EpayPayload encrypt(int numberOfTickets, long invoiceNumber, int amount, String expiry) {
        String description = numberOfTickets == 1 ? "One jPrime.io ticket" : numberOfTickets + " jPrime.io tickets";
        String message = "MIN=" + (real ? kin : DEMO_KIN) + "\r\n"
            + "INVOICE=" + invoiceNumber + "\r\n"
            + "AMOUNT=" + amount + "\r\n"
            + "CURRENCY=EUR\r\n"
            + "EXP_TIME=" + expiry + "\r\n"
            + "DESCR=" + description + "\r\n"
            + "ENCODING=utf-8";
        String encoded = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
        return new EpayPayload(encoded, checksum(encoded, real), real ? url : DEMO_URL);
    }

    /** Decodes the back channel notification. */
    public EpayResponse decrypt(String encoded, String checksum) {
        EpayResponse response = new EpayResponse();
        response.setIsValid(checksum != null && checksum.equalsIgnoreCase(checksum(encoded, real)));
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        Map<String, String> parsed = parse(decoded);
        long invoice = Long.parseLong(parsed.get("INVOICE"));
        response.setBorikaAuthorizationCode(parsed.getOrDefault("BCODE", "").trim());
        response.setTransactionNumberIfPaidByCard(parsed.get("STAN"));
        response.setStatus(EpayResponse.Status.valueOf(parsed.get("STATUS")));
        if (parsed.get("PAY_TIME") != null) {
            response.setDate(LocalDateTime.parse(parsed.get("PAY_TIME"), DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        }
        response.setEpayAnswer("INVOICE=" + invoice + ":STATUS=" + (response.isValid() ? "OK" : "ERR"));
        return response;
    }

    static Map<String, String> parse(String decoded) {
        Map<String, String> parsed = new HashMap<>();
        for (String part : decoded.trim().split(":")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                parsed.put(kv[0], kv[1]);
            }
        }
        return parsed;
    }

    String checksum(String encoded, boolean useRealKey) {
        String secret = useRealKey ? key.orElseThrow(() -> new JPrimeException("jprime.epay.key is not set")) : DEMO_KEY;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return HexFormat.of().formatHex(mac.doFinal(encoded.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new JPrimeException(e);
        }
    }
}
