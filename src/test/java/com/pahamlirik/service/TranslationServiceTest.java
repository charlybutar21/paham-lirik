package com.pahamlirik.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pahamlirik.config.AppProperties;
import com.pahamlirik.dto.AiAnalysisResult;
import com.pahamlirik.dto.LyricLine;
import com.pahamlirik.dto.TranslationResponse;
import com.pahamlirik.entity.Translation;
import com.pahamlirik.repository.TranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private TranslationRepository translationRepository;

    @Mock
    private AiService aiService;

    @Mock
    private GeminiService geminiService;

    @Mock
    private OpenAiService openAiService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private ObjectMapper objectMapper;

    private TranslationService translationService;

    private String rawLyrics;
    private String lyricsHash;

    @BeforeEach
    void setUp() {
        translationService = new TranslationService(
                translationRepository,
                aiService,
                geminiService,
                openAiService,
                appProperties,
                objectMapper
        );
        rawLyrics = "When you try your best but you don't succeed";
        // SHA-256 hash of rawLyrics
        lyricsHash = "59218671de8cc2b5df01c905f87b8d80c3b01859bf5df7e4479e000c011e3b62";
    }

    @Test
    void translateAndAnalyze_CacheHit_ReturnsCachedResponse() throws Exception {
        // Arrange
        Translation mockTranslation = Translation.builder()
                .id(1L)
                .songTitle("Fix You")
                .artist("Coldplay")
                .originalLyrics(rawLyrics)
                .originalLyricsHash(lyricsHash)
                .translatedText("[{\"original\":\"When you try your best\",\"translated\":\"Saat kamu mencoba yang terbaik\"}]")
                .meaningAnalysis("Meaning of fixing you")
                .storySummary("Summary of fixing you")
                .sourceLanguage("en")
                .targetLanguage("id")
                .aiProvider("gemini")
                .createdAt(LocalDateTime.now())
                .build();

        List<LyricLine> lyricLines = List.of(new LyricLine("When you try your best", "Saat kamu mencoba yang terbaik"));

        when(translationRepository.findByOriginalLyricsHash(anyString())).thenReturn(Optional.of(mockTranslation));
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(lyricLines);

        // Act
        TranslationResponse response = translationService.translateAndAnalyze(rawLyrics);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Fix You", response.getSongTitle());
        assertEquals("Coldplay", response.getArtist());
        assertEquals(1, response.getLineTranslations().size());
        assertEquals("Saat kamu mencoba yang terbaik", response.getLineTranslations().get(0).getTranslated());

        verify(translationRepository, times(1)).findByOriginalLyricsHash(anyString());
        verifyNoInteractions(aiService);
        verify(translationRepository, never()).save(any(Translation.class));
    }

    @Test
    void translateAndAnalyze_CacheMiss_CallsAiAndSavesToDb() throws Exception {
        // Arrange
        List<LyricLine> lyricLines = List.of(new LyricLine("When you try your best", "Saat kamu mencoba yang terbaik"));
        AiAnalysisResult aiResult = AiAnalysisResult.builder()
                .songTitle("Fix You")
                .artist("Coldplay")
                .sourceLanguage("en")
                .lineTranslations(lyricLines)
                .meaningAnalysis("Meaning of fixing you")
                .storySummary("Summary of fixing you")
                .build();

        Translation savedTranslation = Translation.builder()
                .id(10L)
                .songTitle("Fix You")
                .artist("Coldplay")
                .originalLyrics(rawLyrics)
                .originalLyricsHash(lyricsHash)
                .translatedText("[{\"original\":\"When you try your best\",\"translated\":\"Saat kamu mencoba yang terbaik\"}]")
                .meaningAnalysis("Meaning of fixing you")
                .storySummary("Summary of fixing you")
                .sourceLanguage("en")
                .targetLanguage("id")
                .aiProvider("gemini")
                .createdAt(LocalDateTime.now())
                .build();

        when(translationRepository.findByOriginalLyricsHash(anyString())).thenReturn(Optional.empty());
        when(aiService.analyzeLyrics(anyString())).thenReturn(aiResult);
        when(appProperties.getProvider()).thenReturn("gemini");
        when(objectMapper.writeValueAsString(any())).thenReturn("serialized-json");
        when(translationRepository.save(any(Translation.class))).thenReturn(savedTranslation);
        when(objectMapper.readValue(eq(savedTranslation.getTranslatedText()), any(TypeReference.class))).thenReturn(lyricLines);

        // Act
        TranslationResponse response = translationService.translateAndAnalyze(rawLyrics);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Fix You", response.getSongTitle());
        assertEquals("gemini", response.getAiProvider());
        
        verify(translationRepository, times(1)).findByOriginalLyricsHash(anyString());
        verify(aiService, times(1)).analyzeLyrics(rawLyrics);
        verify(translationRepository, times(1)).save(any(Translation.class));
    }

    @Test
    void getTranslationById_ValidId_ReturnsResponse() throws Exception {
        // Arrange
        Translation mockTranslation = Translation.builder()
                .id(5L)
                .songTitle("Yellow")
                .artist("Coldplay")
                .translatedText("[]")
                .createdAt(LocalDateTime.now())
                .build();

        when(translationRepository.findById(5L)).thenReturn(Optional.of(mockTranslation));
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of());

        // Act
        TranslationResponse response = translationService.getTranslationById(5L);

        // Assert
        assertNotNull(response);
        assertEquals("Yellow", response.getSongTitle());
        verify(translationRepository, times(1)).findById(5L);
    }

    @Test
    void getTranslationById_InvalidId_ThrowsException() {
        // Arrange
        when(translationRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            translationService.getTranslationById(99L);
        });

        assertEquals("Terjemahan dengan ID 99 tidak ditemukan.", exception.getMessage());
        verify(translationRepository, times(1)).findById(99L);
    }

    @Test
    void translateAndAnalyze_AiServiceFails_FallsBackToAlternative() throws Exception {
        // Arrange
        List<LyricLine> lyricLines = List.of(new LyricLine("When you try your best", "Saat kamu mencoba yang terbaik"));
        AiAnalysisResult aiResult = AiAnalysisResult.builder()
                .songTitle("Fix You")
                .artist("Coldplay")
                .sourceLanguage("en")
                .lineTranslations(lyricLines)
                .meaningAnalysis("Meaning of fixing you")
                .storySummary("Summary of fixing you")
                .build();

        Translation savedTranslation = Translation.builder()
                .id(12L)
                .songTitle("Fix You")
                .artist("Coldplay")
                .originalLyrics(rawLyrics)
                .originalLyricsHash(lyricsHash)
                .translatedText("[{\"original\":\"When you try your best\",\"translated\":\"Saat kamu mencoba yang terbaik\"}]")
                .meaningAnalysis("Meaning of fixing you")
                .storySummary("Summary of fixing you")
                .sourceLanguage("en")
                .targetLanguage("id")
                .aiProvider("openai")
                .createdAt(LocalDateTime.now())
                .build();

        when(translationRepository.findByOriginalLyricsHash(anyString())).thenReturn(Optional.empty());
        when(appProperties.getProvider()).thenReturn("gemini");
        when(aiService.analyzeLyrics(anyString())).thenThrow(new RuntimeException("Gemini API 503 Service Unavailable"));
        when(openAiService.analyzeLyrics(anyString())).thenReturn(aiResult);
        when(objectMapper.writeValueAsString(any())).thenReturn("serialized-json");
        when(translationRepository.save(any(Translation.class))).thenReturn(savedTranslation);
        when(objectMapper.readValue(eq(savedTranslation.getTranslatedText()), any(TypeReference.class))).thenReturn(lyricLines);

        // Act
        TranslationResponse response = translationService.translateAndAnalyze(rawLyrics);

        // Assert
        assertNotNull(response);
        assertEquals(12L, response.getId());
        assertEquals("Fix You", response.getSongTitle());
        
        verify(translationRepository, times(1)).findByOriginalLyricsHash(anyString());
        verify(aiService, times(1)).analyzeLyrics(rawLyrics);
        verify(openAiService, times(1)).analyzeLyrics(rawLyrics);
        verify(geminiService, never()).analyzeLyrics(anyString());
        verify(translationRepository, times(1)).save(any(Translation.class));
    }
}
