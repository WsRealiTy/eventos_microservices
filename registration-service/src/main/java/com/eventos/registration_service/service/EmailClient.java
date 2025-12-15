package com.eventos.registration_service.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String EMAIL_SERVICE_URL = "http://email-service:8090/emails";

    public void enviarEmail(String destinatario, String assunto, String corpo) {
        try {
            // 1. Pega o token da requisição atual que chegou no Registration Service
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String token = null;
            if (attributes != null) {
                token = attributes.getRequest().getHeader("Authorization");
            }

            if (token == null) {
                System.err.println("!!! Erro: Token não encontrado na requisição atual.");
                return;
            }

            // 2. Prepara os headers com o Token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 3. Monta o corpo
            Map<String, String> payload = new HashMap<>();
            payload.put("destinatario", destinatario);
            payload.put("assunto", assunto);
            payload.put("corpo", corpo);

            // 4. Envia com headers (HttpEntity)
            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(EMAIL_SERVICE_URL, request, String.class);
            System.out.println(">>> E-mail enviado com sucesso para: " + destinatario);

        } catch (Exception e) {
            System.err.println("!!! Erro ao enviar e-mail: " + e.getMessage());
        }
    }
}