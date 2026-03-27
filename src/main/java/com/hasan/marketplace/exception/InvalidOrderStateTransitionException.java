package com.hasan.marketplace.exception;

public class InvalidOrderStateTransitionException extends RuntimeException {

    public InvalidOrderStateTransitionException(String message) {
        super(message);
    }
}
