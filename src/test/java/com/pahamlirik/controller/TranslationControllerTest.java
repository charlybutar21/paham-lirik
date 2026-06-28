package com.pahamlirik.controller;

import com.pahamlirik.dto.TranslationResponse;
import com.pahamlirik.service.TranslationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TranslationController.class)
class TranslationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TranslationService translationService;

    @Test
    void translate_ValidRequest_RedirectsToResult() throws Exception {
        // Arrange
        TranslationResponse mockResponse = TranslationResponse.builder()
                .id(1L)
                .songTitle("Test Song")
                .build();
        when(translationService.translateAndAnalyze(anyString())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/translate")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("lyrics", "Some long foreign lyrics content here"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/translate/result/1"));

        verify(translationService, times(1)).translateAndAnalyze("Some long foreign lyrics content here");
    }

    @Test
    void translate_InvalidRequest_RedirectsToHomeWithFlashMessage() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/translate")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("lyrics", "")) // blank lyrics
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("errorMessage"));

        verifyNoInteractions(translationService);
    }

    @Test
    void result_ValidId_ReturnsResultView() throws Exception {
        // Arrange
        TranslationResponse mockResponse = TranslationResponse.builder()
                .id(1L)
                .songTitle("Test Song")
                .artist("Test Artist")
                .build();
        when(translationService.getTranslationById(1L)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/translate/result/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("result"))
                .andExpect(model().attributeExists("translation"));

        verify(translationService, times(1)).getTranslationById(1L);
    }
}
