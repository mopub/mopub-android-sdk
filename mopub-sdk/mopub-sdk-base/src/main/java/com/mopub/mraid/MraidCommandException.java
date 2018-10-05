// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mraid;

class MraidCommandException extends Exception {
    MraidCommandException() {
        super();
    }

    MraidCommandException(String detailMessage) {
        super(detailMessage);
    }

    MraidCommandException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    MraidCommandException(Throwable throwable) {
        super(throwable);
    }
}
