package com.pahamlirik.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AppProperties {
    private String provider;
    private int requestTimeout = 60000; // default 60s
    private Gemini gemini = new Gemini();
    private Openai openai = new Openai();

    @Data
    public static class Gemini {
        private String apiKey;
        private String model = "gemini-2.0-flash";
    }

    @Data
    public static class Openai {
        private String apiKey;
        private String model = "gpt-4o-mini";
    }
}
