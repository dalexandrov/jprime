package org.bgjug.jprime.core.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Business validation errors keyed by form field name. */
public class ValidationFailedException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationFailedException(Map<String, String> errors) {
        super("Validation failed: " + errors);
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public static ValidationFailedException of(String field, String message) {
        return new ValidationFailedException(Map.of(field, message));
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
