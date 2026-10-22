package org.bgjug.jprime.site.web;

import java.util.ArrayList;
import java.util.List;

import org.jboss.resteasy.reactive.RestForm;

/** Ticket registration form. Visitors are repeated fields. */
public class TicketForm {

    @RestForm
    public List<String> visitorName = new ArrayList<>();
    @RestForm
    public List<String> visitorEmail = new ArrayList<>();
    @RestForm
    public List<String> visitorCompany = new ArrayList<>();

    @RestForm
    public boolean student;
    @RestForm
    public boolean company;

    @RestForm
    public String name;
    @RestForm
    public String address;
    @RestForm
    public String vatNumber;
    @RestForm
    public String eik;
    @RestForm
    public String mol;
    @RestForm
    public String email;

    @RestForm
    public String captcha;
    @RestForm
    public boolean acceptTerms;

    @io.quarkus.qute.TemplateData
    public record VisitorRow(String name, String email, String company) {
    }

    /** At least one (possibly empty) row so the form always shows one visitor. */
    public List<VisitorRow> getRows() {
        List<VisitorRow> rows = new ArrayList<>();
        int count = Math.max(1, visitorName.size());
        for (int i = 0; i < count; i++) {
            rows.add(new VisitorRow(get(visitorName, i), get(visitorEmail, i), get(visitorCompany, i)));
        }
        return rows;
    }

    private static String get(List<String> values, int i) {
        return i < values.size() && values.get(i) != null ? values.get(i) : "";
    }
}
