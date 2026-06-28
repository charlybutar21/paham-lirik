package com.pahamlirik.controller;

import com.pahamlirik.dto.TranslationResponse;
import com.pahamlirik.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final TranslationService translationService;

    @GetMapping("/")
    public String home(Model model) {
        log.info("Request received for homepage GET /");
        
        List<TranslationResponse> recentTranslations = translationService.getRecentTranslations();
        model.addAttribute("recentTranslations", recentTranslations);
        
        return "index";
    }
}
