package org.bgjug.jprime.core.model;

public enum VisitorStatus {
    REQUESTING("Requesting"), PAYED("Payed"), SPONSORED("Sponsored");

    private final String label;

    VisitorStatus(String label) {
        this.label = label;
    }

    /** The enum constant name (what is stored in the database and posted by forms). */
    public String getKey() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
