package com.pahamlirik.config;

import com.pahamlirik.service.AiService;
import com.pahamlirik.service.GeminiService;
import com.pahamlirik.service.OpenAiService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AiConfig {

    @Bean
    public RestTemplate restTemplate(AppProperties appProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds connection timeout
        factory.setReadTimeout(appProperties.getRequestTimeout()); // e.g. 60 seconds from application.properties
        return new RestTemplate(factory);
    }

    @Bean
    @Primary
    public AiService activeAiService(AppProperties appProperties,
                                     GeminiService geminiService,
                                     OpenAiService openAiService) {
        if ("openai".equalsIgnoreCase(appProperties.getProvider())) {
            return openAiService;
        }
        return geminiService;
    }
}
