package org.bgjug.jprime.core.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** Human readable dates like "3rd of June 2026" (as on the legacy site). */
public final class DateFormats {

    private DateFormats() {
    }

    public static String ordinal(int day) {
        if (day >= 11 && day <= 13) {
            return day + "th";
        }
        return day + switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    /** "3rd" */
    public static String day(LocalDateTime dateTime) {
        return dateTime == null ? "" : ordinal(dateTime.getDayOfMonth());
    }

    /** "3rd of June" */
    public static String withMonth(LocalDateTime dateTime) {
        return dateTime == null ? "" :
               day(dateTime) + " of " + dateTime.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    /** "3rd of June 2026" */
    public static String withMonthAndYear(LocalDateTime dateTime) {
        return dateTime == null ? "" : withMonth(dateTime) + " " + dateTime.getYear();
    }

    /** "3rd and 4th of June 2026" for a two day conference starting at the given date. */
    public static String conferenceDates(LocalDateTime start) {
        return start == null ? "" : day(start) + " and " + withMonthAndYear(start.plusDays(1));
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        return dateTime == null ? "" : dateTime.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
    }

    public static String formatDuration(Duration duration) {
        if (duration == null) {
            return "Not Set";
        }
        long days = duration.toDays();
        if (days > 0) {
            return days + " days";
        }
        if (duration.toHours() > 0) {
            return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutes() % 60,
                duration.getSeconds() % 60);
        }
        if (duration.toMinutes() > 0) {
            return String.format("%02d:%02d", duration.toMinutes(), duration.getSeconds() % 60);
        }
        return duration.getSeconds() + " seconds";
    }
}
