package com.dota2coach.api.config;

import com.dota2coach.analysis.metric.XRayCalculator;
import com.dota2coach.analysis.report.XRayMarkdownRenderer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans do módulo de análise, que não depende de Spring. */
@Configuration
@EnableConfigurationProperties(RaioXProperties.class)
public class AnalysisConfig {

    @Bean
    public XRayCalculator xRayCalculator() {
        return new XRayCalculator();
    }

    @Bean
    public XRayMarkdownRenderer xRayMarkdownRenderer(RaioXProperties properties) {
        return new XRayMarkdownRenderer(properties.zone());
    }
}
