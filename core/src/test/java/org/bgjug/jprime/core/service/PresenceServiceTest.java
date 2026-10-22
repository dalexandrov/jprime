package org.bgjug.jprime.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PresenceServiceTest {

    @Test
    void masksTheDomainOfTheEmail() {
        assertEquals("john@*******.com", PresenceService.maskEmail("john@example.com"));
        assertEquals("a@*.bg", PresenceService.maskEmail("a@b.bg"));
        assertEquals("not-an-email", PresenceService.maskEmail("not-an-email"));
        assertEquals("", PresenceService.maskEmail(null));
    }
}
