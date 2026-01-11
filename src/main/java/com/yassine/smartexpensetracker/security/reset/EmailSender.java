package com.yassine.smartexpensetracker.security.reset;

public interface EmailSender {
    void send(String to, String subject, String htmlBody);
}
