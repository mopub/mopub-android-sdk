package com.mopub.exceptions;

public class IntentNotResolvableException extends Exception {
    public IntentNotResolvableException(Throwable throwable) {
        super(throwable);
    }

    public IntentNotResolvableException(String message) {
        super(message);
    }
}
