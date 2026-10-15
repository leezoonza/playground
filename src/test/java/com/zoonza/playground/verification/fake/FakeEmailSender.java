package com.zoonza.playground.verification.fake;

import com.zoonza.playground.verification.internal.application.required.EmailSender;

import java.util.ArrayList;
import java.util.List;

public class FakeEmailSender implements EmailSender {
    private final List<SentMail> sentMails = new ArrayList<>();
    private RuntimeException failure;

    @Override
    public void send(String to, String subject, String text) {
        if (failure != null) {
            throw failure;
        }

        sentMails.add(new SentMail(to, subject, text));
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public List<SentMail> sentMails() {
        return List.copyOf(sentMails);
    }

    public record SentMail(String to, String subject, String text) {
    }
}
