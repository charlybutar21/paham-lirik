package com.pahamlirik.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationResponse {

    private Long id;
    private String songTitle;
    private String artist;
    private String originalLyrics;
    private List<LyricLine> lineTranslations;
    private String meaningAnalysis;
    private String storySummary;
    private String sourceLanguage;
    private String targetLanguage;
    private String aiProvider;
    private LocalDateTime createdAt;

    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        return createdAt.format(formatter);
    }
}
