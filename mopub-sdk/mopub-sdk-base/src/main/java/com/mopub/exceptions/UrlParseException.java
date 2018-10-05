// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.exceptions;

public class UrlParseException extends Exception {
    public UrlParseException(final String detailMessage) {
        super(detailMessage);
    }

    public UrlParseException(final Throwable throwable) {
        super(throwable);
    }
}
