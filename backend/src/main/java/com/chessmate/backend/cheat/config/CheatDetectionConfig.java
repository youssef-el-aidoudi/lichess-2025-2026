package com.chessmate.backend.cheat.config;

import com.chessmate.backend.cheat.detector.CheatDetector;
import com.chessmate.backend.cheat.detector.EngineMatchRateDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CheatDetectionConfig {

    @Bean
    public EngineMatchRateDetector engineMatchRateDetector() {
        return new EngineMatchRateDetector(3);
    }

    @Bean
    public List<CheatDetector> cheatDetectors(EngineMatchRateDetector d1) {
        return List.of(d1);
    }
}