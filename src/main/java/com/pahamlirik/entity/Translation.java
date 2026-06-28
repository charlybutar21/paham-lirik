package com.pahamlirik.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "translations", indexes = {
    @Index(name = "idx_created_at", columnList = "createdAt DESC"),
    @Index(name = "idx_song_title", columnList = "songTitle"),
    @Index(name = "idx_lyrics_hash", columnList = "originalLyricsHash")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "song_title")
    private String songTitle;

    private String artist;

    @Column(name = "original_lyrics", columnDefinition = "TEXT", nullable = false)
    private String originalLyrics;

    @Column(name = "original_lyrics_hash", length = 64)
    private String originalLyricsHash;

    @Column(name = "translated_text", columnDefinition = "TEXT", nullable = false)
    private String translatedText; // Stores JSON array of line translations

    @Column(name = "meaning_analysis", columnDefinition = "TEXT")
    private String meaningAnalysis;

    @Column(name = "story_summary", columnDefinition = "TEXT")
    private String storySummary;

    @Column(name = "source_language")
    private String sourceLanguage;

    @Column(name = "target_language")
    private String targetLanguage;

    @Column(name = "ai_provider")
    private String aiProvider;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
