package org.eso.ias.command;

/**
 * Th eexception generated when marshalling or unmarshalling commands
 * and replies to and from strings.
 */
public class StringSerializerException extends Exception {
    public StringSerializerException() {
    }

    public StringSerializerException(String message) {
        super(message);
    }

    public StringSerializerException(String message, Throwable cause) {
        super(message, cause);
    }

    public StringSerializerException(Throwable cause) {
        super(cause);
    }

    public StringSerializerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
