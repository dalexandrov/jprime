package org.bgjug.jprime.site.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.bgjug.jprime.core.model.Session;

/**
 * Turns the flat list of sessions into a per-day table: one row per time slot, one column per hall. Sessions
 * without a hall (breaks, lunch...) span all columns.
 */
public final class AgendaBuilder {

    private AgendaBuilder() {
    }

    @io.quarkus.qute.TemplateData
    public record Row(LocalDateTime start, LocalDateTime end, Session common, List<Session> cells) {

        public boolean isCommon() {
            return common != null;
        }
    }

    @io.quarkus.qute.TemplateData
    public record Day(LocalDate date, List<Row> rows) {
    }

    public static List<Day> build(List<Session> sessions, List<String> halls, LocalDate firstDay, int days) {
        List<Day> result = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            LocalDate date = firstDay.plusDays(d);
            Map<LocalDateTime, List<Session>> bySlot = new TreeMap<>();
            sessions.stream()
                .filter(s -> s.getStartTime() != null && s.getStartTime().toLocalDate().equals(date))
                .sorted(Comparator.comparing(Session::getStartTime))
                .forEach(s -> bySlot.computeIfAbsent(s.getStartTime(), k -> new ArrayList<>()).add(s));
            List<Row> rows = new ArrayList<>();
            bySlot.forEach((start, slotSessions) -> {
                // breaks first, then one row with a cell per hall
                slotSessions.stream().filter(s -> s.getHall() == null).forEach(s ->
                    rows.add(new Row(start, s.getEndTime(), s, List.of())));
                List<Session> inHalls = slotSessions.stream().filter(s -> s.getHall() != null).toList();
                if (!inHalls.isEmpty()) {
                    Map<String, Session> byHall = new LinkedHashMap<>();
                    inHalls.forEach(s -> byHall.putIfAbsent(s.getHall().getName(), s));
                    List<Session> cells = new ArrayList<>();
                    halls.forEach(h -> cells.add(byHall.get(h)));
                    LocalDateTime end = inHalls.stream().map(Session::getEndTime).filter(Objects::nonNull)
                        .min(Comparator.naturalOrder()).orElse(null);
                    rows.add(new Row(start, end, null, cells));
                }
            });
            if (!rows.isEmpty()) {
                result.add(new Day(date, rows));
            }
        }
        return result;
    }
}
