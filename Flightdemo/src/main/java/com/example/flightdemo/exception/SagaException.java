package com.example.flightdemo.exception;

import lombok.Getter;

@Getter
public class SagaException extends RuntimeException {
    private final String failedStep;

    public SagaException(String failedStep, String message) {
        super(message);
        this.failedStep = failedStep;
    }
}
