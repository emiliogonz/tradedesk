package com.example.tradedesk.exception;

public class LastAdminException extends RuntimeException {
    public LastAdminException() {
        super("Cannot remove the last ADMIN; promote another user to ADMIN first");
    }
}