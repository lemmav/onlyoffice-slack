package com.onlyoffice.slack.exception;

public class UnableToPerformSlackOperationException extends RuntimeException {
    public UnableToPerformSlackOperationException(String message) {
        super(message);
    }

    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
