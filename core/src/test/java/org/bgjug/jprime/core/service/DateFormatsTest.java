package org.bgjug.jprime.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class DateFormatsTest {

    @Test
    void ordinals() {
        assertEquals("1st", DateFormats.ordinal(1));
        assertEquals("2nd", DateFormats.ordinal(2));
        assertEquals("3rd", DateFormats.ordinal(3));
        assertEquals("4th", DateFormats.ordinal(4));
        assertEquals("11th", DateFormats.ordinal(11));
        assertEquals("12th", DateFormats.ordinal(12));
        assertEquals("13th", DateFormats.ordinal(13));
        assertEquals("21st", DateFormats.ordinal(21));
        assertEquals("22nd", DateFormats.ordinal(22));
        assertEquals("31st", DateFormats.ordinal(31));
    }

    @Test
    void conferenceDates() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 3, 9, 0);
        assertEquals("3rd of June", DateFormats.withMonth(start));
        assertEquals("3rd of June 2026", DateFormats.withMonthAndYear(start));
        assertEquals("3rd and 4th of June 2026", DateFormats.conferenceDates(start));
        assertEquals("", DateFormats.conferenceDates(null));
    }

    @Test
    void durations() {
        assertEquals("2 days", DateFormats.formatDuration(Duration.ofDays(2)));
        assertEquals("04:00:00", DateFormats.formatDuration(Duration.ofHours(4)));
        assertEquals("Not Set", DateFormats.formatDuration(null));
    }
}
