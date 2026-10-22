package org.bgjug.jprime.core.pdf;

import java.util.ArrayList;
import java.util.List;

public class TicketData {

    private final List<TicketDetail> details = new ArrayList<>();
    private String organizer = "JPrime Events";
    private String event;
    /** Optional - falls back to the report default. */
    private String location;
    /** Optional - falls back to the report default. */
    private String dateTime;

    public List<TicketDetail> getDetails() {
        return details;
    }

    public void addDetail(TicketDetail detail) {
        details.add(detail);
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
