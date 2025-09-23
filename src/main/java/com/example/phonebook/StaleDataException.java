package com.example.phonebook;

public class StaleDataException extends RuntimeException {
    public StaleDataException(String message) {
        super(message);
    }
}
