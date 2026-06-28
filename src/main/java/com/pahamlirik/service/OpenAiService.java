package com.pahamlirik.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pahamlirik.config.AppProperties;
import com.pahamlirik.dto.AiAnalysisResult;
import com.pahamlirik.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService implements AiService {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private static final String PROMPT_TEMPLATE = 
        "Kamu adalah seorang penerjemah dan analis lirik lagu profesional.\n\n" +
        "Diberikan lirik lagu berikut:\n" +
        "---\n" +
        "%s\n" +
        "---\n\n" +
        "Tugas kamu:\n" +
        "1. **Deteksi bahasa** lirik tersebut.\n" +
        "2. **Identifikasi** judul lagu dan artis (jika bisa dikenali dari liriknya). Jika tidak bisa, tulis \"Tidak Diketahui\".\n" +
        "3. **Terjemahkan setiap baris** lirik ke Bahasa Indonesia. Pertahankan urutan baris. Baris kosong tetap jadi baris kosong.\n" +
        "4. **Analisis makna** lagu secara keseluruhan (2-4 paragraf).\n" +
        "5. **Rangkum cerita** lagu dalam bentuk narasi singkat (1-2 paragraf).\n\n" +
        "Berikan respons dalam format JSON berikut (TANPA markdown code block, langsung JSON):\n" +
        "{\n" +
        "  \"song_title\": \"...\",\n" +
        "  \"artist\": \"...\",\n" +
        "  \"source_language\": \"...\",\n" +
        "  \"line_translations\": [\n" +
        "    {\"original\": \"baris asli 1\", \"translated\": \"terjemahan 1\"},\n" +
        "    {\"original\": \"baris asli 2\", \"translated\": \"terjemahan 2\"}\n" +
        "  ],\n" +
        "  \"meaning_analysis\": \"...\",\n" +
        "  \"story_summary\": \"...\"\n" +
        "}";

    @Override
    public AiAnalysisResult analyzeLyrics(String lyrics) throws AiServiceException {
        String apiKey = appProperties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AiServiceException("OpenAI API Key is not configured. Please check your environment variables.");
        }

        String model = appProperties.getOpenai().getModel();
        String prompt = String.format(PROMPT_TEMPLATE, lyrics);

        // Prepare request body
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Kamu adalah penerjemah lirik lagu profesional.");

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        Map<String, Object> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(systemMessage, userMessage));
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 4096);
        requestBody.put("response_format", responseFormat);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int maxRetries = 2; // Initial run + 1 retry
        int attempt = 0;
        String responseBody = null;

        while (attempt < maxRetries) {
            try {
                log.info("Sending request to OpenAI API, model: {}, attempt: {}", model, attempt + 1);
                ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_API_URL, entity, String.class);
                responseBody = response.getBody();
                break;
            } catch (HttpStatusCodeException e) {
                attempt++;
                log.error("OpenAI API error (Status: {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
                if (attempt < maxRetries && e.getStatusCode().value() == 429) {
                    log.warn("Rate limited (429). Retrying after 2 seconds delay...");
                    sleep(2000);
                } else {
                    throw new AiServiceException("Error from OpenAI API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
                }
            } catch (Exception e) {
                attempt++;
                log.error("Network or connection error while calling OpenAI API: {}", e.getMessage());
                if (attempt < maxRetries) {
                    log.warn("Retrying call after 2 seconds delay due to network error...");
                    sleep(2000);
                } else {
                    throw new AiServiceException("Failed to reach OpenAI API: " + e.getMessage(), e);
                }
            }
        }

        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new AiServiceException("Received empty response from OpenAI API");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choices = rootNode.path("choices");
            if (choices.isMissingNode() || choices.size() == 0) {
                throw new AiServiceException("No completion choices returned by OpenAI API. Full response: " + responseBody);
            }

            JsonNode contentNode = choices.get(0).path("message").path("content");
            if (contentNode.isMissingNode()) {
                throw new AiServiceException("Failed to find content message in OpenAI choice structure. Full response: " + responseBody);
            }

            String aiResponseText = contentNode.asText();
            String cleanedJson = cleanJsonResponse(aiResponseText);
            log.debug("Cleaned OpenAI JSON response: {}", cleanedJson);

            return objectMapper.readValue(cleanedJson, AiAnalysisResult.class);
        } catch (Exception e) {
            log.error("Failed to parse OpenAI API response: {}", responseBody, e);
            throw new AiServiceException("Failed to parse AI response into structured translation results: " + e.getMessage(), e);
        }
    }

    private String cleanJsonResponse(String responseText) {
        if (responseText == null) {
            return "";
        }
        String cleaned = responseText.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("Thread interrupted during retry delay", e);
        }
    }
}
