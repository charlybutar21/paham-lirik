package com.pahamlirik.service;

import com.pahamlirik.dto.AiAnalysisResult;
import com.pahamlirik.exception.AiServiceException;

public interface AiService {
    
    /**
     * Translates and analyzes the given song lyrics.
     *
     * @param lyrics The raw song lyrics text.
     * @return Structured analysis results.
     * @throws AiServiceException if the AI provider call fails or response parsing fails.
     */
    AiAnalysisResult analyzeLyrics(String lyrics) throws AiServiceException;
}
