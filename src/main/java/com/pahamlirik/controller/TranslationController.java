package com.pahamlirik.controller;

import com.pahamlirik.dto.TranslationRequest;
import com.pahamlirik.dto.TranslationResponse;
import com.pahamlirik.service.TranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/translate")
@RequiredArgsConstructor
@Slf4j
public class TranslationController {

    private final TranslationService translationService;

    @PostMapping
    public String translate(@Valid TranslationRequest request,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {
        log.info("Request received to translate lyrics");

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldError("lyrics") != null 
                    ? bindingResult.getFieldError("lyrics").getDefaultMessage() 
                    : "Input tidak valid";
            log.warn("Validation failed for translation request: {}", errorMsg);
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/";
        }

        try {
            TranslationResponse response = translationService.translateAndAnalyze(request.getLyrics());
            return "redirect:/translate/result/" + response.getId();
        } catch (Exception e) {
            log.error("Failed to translate and analyze lyrics", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/result/{id}")
    public String result(@PathVariable Long id, Model model) {
        log.info("Request received for translation result ID: {}", id);
        
        try {
            TranslationResponse translation = translationService.getTranslationById(id);
            model.addAttribute("translation", translation);
            return "result";
        } catch (IllegalArgumentException e) {
            log.warn("Translation result not found for ID: {}", id);
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }
}
