package com.yassine.expensetracker.security.reset;

public interface EmailSender {
    void send(String to, String subject, String htmlBody);
}
