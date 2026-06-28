package com.pahamlirik.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pahamlirik.config.AppProperties;
import com.pahamlirik.dto.AiAnalysisResult;
import com.pahamlirik.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService implements AiService {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

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
        String apiKey = appProperties.getGemini().getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AiServiceException("Gemini API Key is not configured. Please check your environment variables.");
        }

        String model = appProperties.getGemini().getModel();
        String url = String.format(GEMINI_API_URL, model, apiKey);

        String prompt = String.format(PROMPT_TEMPLATE, lyrics);

        // Prepare request body according to Gemini schema
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> contentNode = new HashMap<>();
        contentNode.put("parts", List.of(part));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("maxOutputTokens", 4096);
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(contentNode));
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int maxRetries = 2; // Initial run + 1 retry
        int attempt = 0;
        String responseBody = null;

        while (attempt < maxRetries) {
            try {
                log.info("Sending request to Gemini API, model: {}, attempt: {}", model, attempt + 1);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                responseBody = response.getBody();
                break; // Break loop on success
            } catch (HttpStatusCodeException e) {
                attempt++;
                log.error("Gemini API error (Status: {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
                if (attempt < maxRetries && e.getStatusCode().value() == 429) {
                    log.warn("Rate limited (429). Retrying after 2 seconds delay...");
                    sleep(2000);
                } else {
                    throw new AiServiceException("Error from Gemini API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
                }
            } catch (Exception e) {
                attempt++;
                log.error("Network or connection error while calling Gemini API: {}", e.getMessage());
                if (attempt < maxRetries) {
                    log.warn("Retrying call after 2 seconds delay due to network error...");
                    sleep(2000);
                } else {
                    throw new AiServiceException("Failed to reach Gemini API: " + e.getMessage(), e);
                }
            }
        }

        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new AiServiceException("Received empty response from Gemini API");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode candidates = rootNode.path("candidates");
            if (candidates.isMissingNode() || candidates.size() == 0) {
                throw new AiServiceException("No completion candidates returned by Gemini API. Full response: " + responseBody);
            }

            JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
            if (textNode.isMissingNode()) {
                throw new AiServiceException("Failed to find response text part in Gemini candidate structure. Full response: " + responseBody);
            }

            String aiResponseText = textNode.asText();
            String cleanedJson = cleanJsonResponse(aiResponseText);
            log.debug("Cleaned Gemini JSON response: {}", cleanedJson);

            return objectMapper.readValue(cleanedJson, AiAnalysisResult.class);
        } catch (Exception e) {
            log.error("Failed to parse Gemini API response: {}", responseBody, e);
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
