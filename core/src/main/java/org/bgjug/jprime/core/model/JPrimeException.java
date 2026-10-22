package org.bgjug.jprime.core.model;

public class JPrimeException extends RuntimeException {

    public JPrimeException(String message) {
        super(message);
    }

    public JPrimeException(Throwable cause) {
        super(cause);
    }

    public JPrimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
