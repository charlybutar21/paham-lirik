package com.pahamlirik.repository;

import com.pahamlirik.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
    
    // Get the most recent 20 translations for the public history
    List<Translation> findTop20ByOrderByCreatedAtDesc();
    
    // Find cached translation using the SHA-256 hash of original lyrics
    Optional<Translation> findByOriginalLyricsHash(String originalLyricsHash);
}
