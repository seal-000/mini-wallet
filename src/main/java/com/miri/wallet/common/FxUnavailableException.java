package com.miri.wallet.common;

public class FxUnavailableException extends RuntimeException {
    public FxUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
