package com.matchday.ticket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class TicketMailerService {

    private final JavaMailSender mailSender;
    private final String from;

    public TicketMailerService(JavaMailSender mailSender, @Value ("${matchday.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /**
     * Sends an email with the given parameters.
     * 
     * @param to      - recipient email address
     * @param subject - subject of the email
     * @param text    - body of the email
     */
    public void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }
}
