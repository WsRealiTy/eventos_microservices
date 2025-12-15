package com.eventos.registration_service.service; // Atenção ao pacote (mude para registration_service no outro arquivo)

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailClient {

    private final RestTemplate restTemplate = new RestTemplate();
    
    // CORREÇÃO 1: URL correta (/emails em vez de /email/send)
    private final String EMAIL_SERVICE_URL = "http://email-service:8090/emails";

    public void enviarEmail(String destinatario, String assunto, String corpo) {
        try {
            Map<String, String> payload = new HashMap<>();
            
            // CORREÇÃO 2: Chaves em português para bater com o Python (EmailSchema)
            payload.put("destinatario", destinatario);
            payload.put("assunto", assunto);
            payload.put("corpo", corpo);

            restTemplate.postForEntity(EMAIL_SERVICE_URL, payload, String.class);
            System.out.println(">>> E-mail enviado com sucesso para: " + destinatario);
        } catch (Exception e) {
            // Imprime o erro no console para você conseguir debugar se falhar de novo
            System.err.println("!!! Erro ao enviar e-mail: " + e.getMessage());
        }
    }
}