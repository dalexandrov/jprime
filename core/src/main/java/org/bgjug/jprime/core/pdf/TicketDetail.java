package org.bgjug.jprime.core.pdf;

/** One ticket. Property names are used by the Jasper template. */
public class TicketDetail {

    private String number;
    private String holder;
    private String type;

    public TicketDetail(String number, String holder, String type) {
        this.number = number;
        this.holder = holder;
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public String getHolder() {
        return holder;
    }

    public String getType() {
        return type;
    }
}
