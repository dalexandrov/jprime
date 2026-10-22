package org.bgjug.jprime.site.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.bgjug.jprime.core.model.Session;
import org.bgjug.jprime.core.model.Submission;
import org.bgjug.jprime.core.model.VenueHall;
import org.junit.jupiter.api.Test;

class AgendaBuilderTest {

    private static final VenueHall HALL_A = new VenueHall("Hall A", null);
    private static final VenueHall HALL_B = new VenueHall("Hall B", null);
    private static final LocalDate DAY1 = LocalDate.of(2026, 6, 3);

    @Test
    void buildsRowsPerSlotAndColumnsPerHall() {
        Session opening = slot("Opening", null, 9, 0, 9, 30, DAY1);
        Session talkA = talk("Talk A", HALL_A, 9, 30, 10, 20);
        Session talkB = talk("Talk B", HALL_B, 9, 30, 10, 20);
        Session talkA2 = talk("Talk A2", HALL_A, 10, 30, 11, 20);
        Session lastYearBreak = slot("Break", null, 10, 0, 10, 30, DAY1.minusYears(1));
        Session day2 = talk("Day 2", HALL_B, 9, 30, 10, 20);
        day2.setStartTime(day2.getStartTime().plusDays(1));
        day2.setEndTime(day2.getEndTime().plusDays(1));

        List<AgendaBuilder.Day> days = AgendaBuilder.build(
            List.of(talkA2, talkB, opening, talkA, lastYearBreak, day2), List.of("Hall A", "Hall B", "Workshops"), DAY1, 2);

        assertEquals(2, days.size());
        List<AgendaBuilder.Row> rows = days.getFirst().rows();
        assertEquals(3, rows.size());
        assertTrue(rows.get(0).isCommon());
        assertEquals("Opening", rows.get(0).common().getTitle());
        assertEquals(List.of(talkA, talkB), rows.get(1).cells().subList(0, 2));
        assertNull(rows.get(1).cells().get(2));
        assertEquals(talkA2, rows.get(2).cells().get(0));
        assertNull(rows.get(2).cells().get(1));
        assertEquals(day2, days.get(1).rows().getFirst().cells().get(1));
    }

    private static Session talk(String title, VenueHall hall, int h1, int m1, int h2, int m2) {
        Submission submission = new Submission();
        submission.setTitle(title);
        return new Session(submission, DAY1.atTime(h1, m1), DAY1.atTime(h2, m2), hall);
    }

    private static Session slot(String title, VenueHall hall, int h1, int m1, int h2, int m2, LocalDate day) {
        Session session = new Session(null, day.atTime(h1, m1), day.atTime(h2, m2), hall);
        session.setTitle(title);
        return session;
    }

    @SuppressWarnings("unused")
    private static LocalDateTime at(int h, int m) {
        return DAY1.atTime(h, m);
    }
}
