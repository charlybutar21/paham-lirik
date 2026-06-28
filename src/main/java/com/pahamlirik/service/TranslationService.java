package com.pahamlirik.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pahamlirik.config.AppProperties;
import com.pahamlirik.dto.AiAnalysisResult;
import com.pahamlirik.dto.LyricLine;
import com.pahamlirik.dto.TranslationResponse;
import com.pahamlirik.entity.Translation;
import com.pahamlirik.exception.AiServiceException;
import com.pahamlirik.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    private final TranslationRepository translationRepository;
    private final AiService aiService;
    private final GeminiService geminiService;
    private final OpenAiService openAiService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    /**
     * Translates and analyzes the given song lyrics.
     * Uses SHA-256 hash of the lyrics to cache and retrieve matching results from the DB.
     */
    @Transactional
    public TranslationResponse translateAndAnalyze(String lyrics) {
        if (lyrics == null || lyrics.trim().isEmpty()) {
            throw new IllegalArgumentException("Lirik lagu tidak boleh kosong");
        }
        if (lyrics.length() > 10000) {
            throw new IllegalArgumentException("Lirik lagu terlalu panjang, maksimal 10.000 karakter");
        }

        String lyricsHash = computeSha256(lyrics.trim());
        log.info("Processing translation request. Lyrics hash: {}", lyricsHash);

        // Check local DB Cache
        Optional<Translation> cachedTranslation = translationRepository.findByOriginalLyricsHash(lyricsHash);
        if (cachedTranslation.isPresent()) {
            log.info("Cache hit! Returning cached translation with ID: {}", cachedTranslation.get().getId());
            return mapToResponse(cachedTranslation.get());
        }

        log.info("Cache miss! Querying AI service...");
        AiAnalysisResult aiResult;
        String activeProvider = appProperties.getProvider();
        String providerUsed = activeProvider;

        try {
            aiResult = aiService.analyzeLyrics(lyrics.trim());
        } catch (Exception e) {
            log.warn("Primary AI service ({}) failed with: {}. Trying fallback AI service...", activeProvider, e.getMessage());
            try {
                if ("openai".equalsIgnoreCase(activeProvider)) {
                    log.info("Falling back to Gemini...");
                    aiResult = geminiService.analyzeLyrics(lyrics.trim());
                    providerUsed = "gemini";
                } else {
                    log.info("Falling back to OpenAI...");
                    aiResult = openAiService.analyzeLyrics(lyrics.trim());
                    providerUsed = "openai";
                }
            } catch (Exception fallbackException) {
                log.error("Fallback AI service failed as well", fallbackException);
                throw new AiServiceException("Seluruh layanan AI (Gemini & OpenAI) sedang tidak tersedia saat ini. Silakan coba beberapa saat lagi.", fallbackException);
            }
        }

        // Serialize line translations to JSON string for saving in DB
        String translatedTextJson = "";
        try {
            translatedTextJson = objectMapper.writeValueAsString(aiResult.getLineTranslations());
        } catch (Exception e) {
            log.error("Failed to serialize line translations to JSON", e);
            throw new AiServiceException("Failed to serialize translation results for database persistence", e);
        }

        Translation translation = Translation.builder()
                .songTitle(aiResult.getSongTitle())
                .artist(aiResult.getArtist())
                .originalLyrics(lyrics.trim())
                .originalLyricsHash(lyricsHash)
                .translatedText(translatedTextJson)
                .meaningAnalysis(aiResult.getMeaningAnalysis())
                .storySummary(aiResult.getStorySummary())
                .sourceLanguage(aiResult.getSourceLanguage())
                .targetLanguage("id")
                .aiProvider(providerUsed)
                .build();

        Translation savedTranslation = translationRepository.save(translation);
        log.info("Successfully saved translation to database with ID: {} (provider: {})", savedTranslation.getId(), providerUsed);

        return mapToResponse(savedTranslation);
    }

    /**
     * Retrieves the most recent 20 translations.
     */
    @Transactional(readOnly = true)
    public List<TranslationResponse> getRecentTranslations() {
        log.debug("Fetching top 20 recent translations...");
        return translationRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single translation by ID.
     */
    @Transactional(readOnly = true)
    public TranslationResponse getTranslationById(Long id) {
        log.debug("Fetching translation details for ID: {}", id);
        Translation translation = translationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Terjemahan dengan ID " + id + " tidak ditemukan."));
        return mapToResponse(translation);
    }

    /**
     * Maps a Translation entity to a TranslationResponse DTO.
     */
    private TranslationResponse mapToResponse(Translation translation) {
        List<LyricLine> lineTranslations = new ArrayList<>();
        try {
            if (translation.getTranslatedText() != null && !translation.getTranslatedText().isEmpty()) {
                lineTranslations = objectMapper.readValue(
                        translation.getTranslatedText(),
                        new TypeReference<List<LyricLine>>() {}
                );
            }
        } catch (Exception e) {
            log.error("Failed to deserialize line translations from database JSON (ID: {})", translation.getId(), e);
        }

        return TranslationResponse.builder()
                .id(translation.getId())
                .songTitle(translation.getSongTitle())
                .artist(translation.getArtist())
                .originalLyrics(translation.getOriginalLyrics())
                .lineTranslations(lineTranslations)
                .meaningAnalysis(translation.getMeaningAnalysis())
                .storySummary(translation.getStorySummary())
                .sourceLanguage(translation.getSourceLanguage())
                .targetLanguage(translation.getTargetLanguage())
                .aiProvider(translation.getAiProvider())
                .createdAt(translation.getCreatedAt())
                .build();
    }

    /**
     * Helper to compute SHA-256 of text.
     */
    private String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
