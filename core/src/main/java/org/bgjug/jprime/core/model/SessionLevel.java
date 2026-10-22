package org.bgjug.jprime.core.model;

public enum SessionLevel {
    BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced");

    private final String label;

    SessionLevel(String label) {
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
