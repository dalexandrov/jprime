package org.bgjug.jprime.site.web;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.core.model.JPrimeException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Simple arithmetic captcha ("12 + 7 =") like the legacy one, but stateless: the expected answer is kept in a signed
 * cookie (HMAC) instead of the HTTP session, so the site needs no server side session.
 */
@ApplicationScoped
public class CaptchaService {

    public static final String COOKIE = "jprime_captcha";
    private static final long VALIDITY_SECONDS = 30 * 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] secret;

    public CaptchaService(@ConfigProperty(name = "jprime.captcha.secret") Optional<String> configuredSecret) {
        if (configuredSecret.filter(StringUtils::isNotBlank).isPresent()) {
            secret = configuredSecret.get().getBytes(StandardCharsets.UTF_8);
        } else {
            secret = new byte[32];
            RANDOM.nextBytes(secret);
        }
    }

    public record Captcha(byte[] png, String cookieValue) {
    }

    public Captcha generate(int width, int height) {
        int first = 10 + RANDOM.nextInt(90);
        int second = 1 + RANDOM.nextInt(9);
        String text = first + "+" + second + "=";
        byte[] image = render(text, width, height);
        long expires = Instant.now().getEpochSecond() + VALIDITY_SECONDS;
        String answer = Integer.toString(first + second);
        return new Captcha(image, expires + "." + sign(answer, expires));
    }

    /** True when the user's answer matches the one signed into the cookie and the cookie is not expired. */
    public boolean verify(String cookieValue, String answer) {
        if (StringUtils.isAnyBlank(cookieValue, answer)) {
            return false;
        }
        int dot = cookieValue.indexOf('.');
        if (dot < 0) {
            return false;
        }
        long expires;
        try {
            expires = Long.parseLong(cookieValue.substring(0, dot));
        } catch (NumberFormatException e) {
            return false;
        }
        if (expires < Instant.now().getEpochSecond()) {
            return false;
        }
        String expected = sign(answer.trim(), expires);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
            cookieValue.substring(dot + 1).getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String answer, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((answer + "|" + expires).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new JPrimeException(e);
        }
    }

    private static byte[] render(String text, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(240, 244, 247));
        g.fillRect(0, 0, width, height);
        Color circleColor = new Color(195, 208, 247);
        int circles = 6 + RANDOM.nextInt(4);
        for (int i = 0; i < circles; i++) {
            g.setColor(i % 2 == 0 ? circleColor : new Color(195, 123 + RANDOM.nextInt(122), 208));
            int w = (int) (RANDOM.nextDouble() * width / 2.0 * RANDOM.nextDouble() * 2.0);
            int h = (int) (RANDOM.nextDouble() * height / 2.0 * RANDOM.nextDouble() * 2.0);
            g.drawOval((int) (RANDOM.nextDouble() * width - w), (int) (RANDOM.nextDouble() * height - h), w, h);
        }
        Color textColor = new Color(131, 153, 184);
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 24);
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        int charDim = Math.max(metrics.getMaxAdvance(), metrics.getHeight());
        float margin = 20f;
        float spacePerChar = (width - 2 * margin) / (text.length() - 1f);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            BufferedImage charImage = new BufferedImage(charDim, charDim, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = charImage.createGraphics();
            cg.translate(charDim / 2, charDim / 2);
            if (Character.isDigit(c)) {
                cg.transform(AffineTransform.getRotateInstance((RANDOM.nextDouble() - 0.5) * 0.7));
            }
            cg.translate(-charDim / 2, -charDim / 2);
            cg.setColor(textColor);
            cg.setFont(font);
            int x = (int) (0.5 * charDim - 0.5 * metrics.charWidth(c));
            cg.drawString(String.valueOf(c), x, (charDim - metrics.getAscent()) / 2 + metrics.getAscent());
            cg.dispose();
            g.drawImage(charImage, (int) (margin + spacePerChar * i - charDim / 2f), (height - charDim) / 2, null);
        }
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new JPrimeException(e);
        }
    }
}
