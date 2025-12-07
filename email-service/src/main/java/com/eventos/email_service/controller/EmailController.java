package com.eventos.email_service.controller;

import com.eventos.email_service.service.EmailService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/emails")
public class EmailController {

    private final EmailService service;

    public EmailController(EmailService service) {
        this.service = service;
    }

    @PostMapping
    public String sendEmail(@RequestParam String to,
                            @RequestParam String subject,
                            @RequestParam String body) {
        service.sendEmail(to, subject, body);
        return "E-mail enviado com sucesso!";
    }
}
