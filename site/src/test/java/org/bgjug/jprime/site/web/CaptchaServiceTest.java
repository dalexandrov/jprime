package org.bgjug.jprime.site.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class CaptchaServiceTest {

    private final CaptchaService service = new CaptchaService(Optional.of("test-secret"));

    @Test
    void verifiesOnlyTheRightAnswer() {
        CaptchaService.Captcha captcha = service.generate(150, 30);
        assertEquals((byte) 0x89, captcha.png()[0]);
        int correct = -1;
        for (int answer = 11; answer <= 108; answer++) {
            if (service.verify(captcha.cookieValue(), Integer.toString(answer))) {
                assertEquals(-1, correct, "only one answer must be accepted");
                correct = answer;
            }
        }
        assertTrue(correct > 0);
        assertFalse(service.verify(captcha.cookieValue(), ""));
        assertFalse(service.verify(null, Integer.toString(correct)));
        assertFalse(service.verify("tampered." + captcha.cookieValue(), Integer.toString(correct)));
    }

    @Test
    void anotherSecretDoesNotAccept() {
        CaptchaService.Captcha captcha = service.generate(150, 30);
        CaptchaService other = new CaptchaService(Optional.of("other-secret"));
        for (int answer = 11; answer <= 108; answer++) {
            assertFalse(other.verify(captcha.cookieValue(), Integer.toString(answer)));
        }
    }
}
