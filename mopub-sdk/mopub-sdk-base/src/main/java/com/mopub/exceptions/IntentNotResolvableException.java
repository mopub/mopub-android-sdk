// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.exceptions;

public class IntentNotResolvableException extends Exception {
    public IntentNotResolvableException(Throwable throwable) {
        super(throwable);
    }

    public IntentNotResolvableException(String message) {
        super(message);
    }
}
