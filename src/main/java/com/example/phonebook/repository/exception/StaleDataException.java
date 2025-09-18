package com.example.phonebook.repository.exception;

public class StaleDataException extends RuntimeException {
    public StaleDataException(String message) {
        super(message);
    }
}
