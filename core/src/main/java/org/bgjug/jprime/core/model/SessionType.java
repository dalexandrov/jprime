package org.bgjug.jprime.core.model;

public enum SessionType {
    CONFERENCE_SESSION("Conference session"), DEEP_DIVE("Deep Dive"), WORKSHOP("Workshop");

    private final String label;

    SessionType(String label) {
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
