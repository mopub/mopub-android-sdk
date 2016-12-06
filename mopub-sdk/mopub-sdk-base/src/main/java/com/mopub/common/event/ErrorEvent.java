package com.mopub.common.event;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Immutable data class with error event data.
 */
public class ErrorEvent extends BaseEvent {
    @Nullable private final String mErrorExceptionClassName;
    @Nullable private final String mErrorMessage;
    @Nullable private final String mErrorStackTrace;
    @Nullable private final String mErrorFileName;
    @Nullable private final String mErrorClassName;
    @Nullable private final String mErrorMethodName;
    @Nullable private final Integer mErrorLineNumber;

    private ErrorEvent(@NonNull Builder builder) {
        super(builder);
        mErrorExceptionClassName = builder.mErrorExceptionClassName;
        mErrorMessage = builder.mErrorMessage;
        mErrorStackTrace = builder.mErrorStackTrace;
        mErrorFileName = builder.mErrorFileName;
        mErrorClassName = builder.mErrorClassName;
        mErrorMethodName = builder.mErrorMethodName;
        mErrorLineNumber = builder.mErrorLineNumber;
    }

    @Nullable
    public String getErrorExceptionClassName() {
        return mErrorExceptionClassName;
    }

    @Nullable
    public String getErrorMessage() {
        return mErrorMessage;
    }

    @Nullable
    public String getErrorStackTrace() {
        return mErrorStackTrace;
    }

    @Nullable
    public String getErrorFileName() {
        return mErrorFileName;
    }

    @Nullable
    public String getErrorClassName() {
        return mErrorClassName;
    }

    @Nullable
    public String getErrorMethodName() {
        return mErrorMethodName;
    }

    @Nullable
    public Integer getErrorLineNumber() {
        return mErrorLineNumber;
    }

    @Override
    public String toString() {
        final String string = super.toString();
        return string +
                "ErrorEvent\n" +
                "ErrorExceptionClassName: " + getErrorExceptionClassName() + "\n" +
                "ErrorMessage: " + getErrorMessage() + "\n" +
                "ErrorStackTrace: " + getErrorStackTrace() + "\n" +
                "ErrorFileName: " + getErrorFileName() + "\n" +
                "ErrorClassName: " + getErrorClassName() + "\n" +
                "ErrorMethodName: " + getErrorMethodName() + "\n" +
                "ErrorLineNumber: " + getErrorLineNumber() + "\n";
    }

    public static class Builder extends BaseEvent.Builder {
        @Nullable private String mErrorExceptionClassName;
        @Nullable private String mErrorMessage;
        @Nullable private String mErrorStackTrace;
        @Nullable private String mErrorFileName;
        @Nullable private String mErrorClassName;
        @Nullable private String mErrorMethodName;
        @Nullable private Integer mErrorLineNumber;

        public Builder(@NonNull Name name, @NonNull Category category, double samplingRate) {
            super(ScribeCategory.EXCHANGE_CLIENT_ERROR, name, category, samplingRate);
        }

        @NonNull
        public Builder withErrorExceptionClassName(@Nullable String errorExceptionClassName) {
            mErrorExceptionClassName = errorExceptionClassName;
            return this;
        }

        @NonNull
        public Builder withErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        @NonNull
        public Builder withErrorStackTrace(@Nullable String errorStackTrace) {
            mErrorStackTrace = errorStackTrace;
            return this;
        }

        @NonNull
        public Builder withErrorFileName(@Nullable String errorFileName) {
            mErrorFileName = errorFileName;
            return this;
        }

        @NonNull
        public Builder withErrorClassName(@Nullable String errorClassName) {
            mErrorClassName = errorClassName;
            return this;
        }

        @NonNull
        public Builder withErrorMethodName(@Nullable String errorMethodName) {
            mErrorMethodName = errorMethodName;
            return this;
        }

        @NonNull
        public Builder withErrorLineNumber(@Nullable Integer errorLineNumber) {
            mErrorLineNumber = errorLineNumber;
            return this;
        }

        @NonNull
        public Builder withException(@Nullable Exception exception) {
            mErrorExceptionClassName = exception.getClass().getName();
            mErrorMessage = exception.getMessage();

            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));
            mErrorStackTrace = stringWriter.toString();

            if (exception.getStackTrace().length > 0) {
                mErrorFileName = exception.getStackTrace()[0].getFileName();
                mErrorClassName = exception.getStackTrace()[0].getClassName();
                mErrorMethodName = exception.getStackTrace()[0].getMethodName();
                mErrorLineNumber = exception.getStackTrace()[0].getLineNumber();
            }
            return this;
        }

        @NonNull
        @Override
        public ErrorEvent build() {
            return new ErrorEvent(this);
        }
    }
}
