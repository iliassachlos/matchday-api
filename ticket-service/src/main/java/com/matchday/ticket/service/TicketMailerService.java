package com.matchday.ticket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class TicketMailerService {

    private final JavaMailSender mailSender;
    private final String from;

    public TicketMailerService(JavaMailSender mailSender, @Value("${matchday.mail.from}") String from) {
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

    /**
     * Send the ticket email: an HTML body with the QR code shown inside it.
     *
     * @param to      - the ticket holder's email address
     * @param subject - subject of the email
     * @param html    - body of the email; refers to the QR code as
     *                <img src="cid:qr">
     * @param qrPng   - the QR code image
     */
    public void sendTicket(String to, String subject, String html, byte[] qrPng) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.addInline("qr", new ByteArrayResource(qrPng), "image/png");
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not build the ticket email", e);
        }

        mailSender.send(message);
    }

}
