package com.zoonza.playground.verification.internal.application.required;

public interface EmailSender {
    void send(String to, String subject, String text);
}
