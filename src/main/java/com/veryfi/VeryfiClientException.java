package com.veryfi;

public class VeryfiClientException extends Exception {

    public VeryfiClientException(String message) {
        super(message);
    }

    VeryfiClientException(String message, Throwable error) {
        super(message, error);
    }
}
