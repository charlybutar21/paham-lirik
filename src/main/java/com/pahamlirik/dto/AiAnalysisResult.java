package com.pahamlirik.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysisResult {

    @JsonProperty("song_title")
    private String songTitle;

    private String artist;

    @JsonProperty("source_language")
    private String sourceLanguage;

    @JsonProperty("line_translations")
    private List<LyricLine> lineTranslations;

    @JsonProperty("meaning_analysis")
    private String meaningAnalysis;

    @JsonProperty("story_summary")
    private String storySummary;
}
