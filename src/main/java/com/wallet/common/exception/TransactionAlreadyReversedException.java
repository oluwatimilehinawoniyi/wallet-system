package com.wallet.common.exception;

public class TransactionAlreadyReversedException extends RuntimeException {

    public TransactionAlreadyReversedException(String message) {
        super(message);
    }
}

