package com.eventos.email_service.service;

import com.eventos.email_service.model.EmailLog;
import com.eventos.email_service.repository.EmailLogRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository repository;

    public EmailService(JavaMailSender mailSender, EmailLogRepository repository) {
        this.mailSender = mailSender;
        this.repository = repository;
    }

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);

        // Registrar envio
        EmailLog log = new EmailLog();
        log.setToEmail(to);
        log.setSubject(subject);
        log.setBody(body);
        log.setSentAt(LocalDateTime.now());
        repository.save(log);
    }
}
