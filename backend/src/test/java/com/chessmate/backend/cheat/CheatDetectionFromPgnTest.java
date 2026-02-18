package com.chessmate.backend.cheat;

import com.chessmate.backend.cheat.engine.EngineMoveAnalysis;
import com.chessmate.backend.cheat.engine.EngineOracle;
import com.chessmate.backend.cheat.model.CheatReport;
import com.chessmate.backend.cheat.pgn.PgnParserService;
import com.chessmate.backend.cheat.service.CheatDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class CheatDetectionFromPgnSpringTest {

    @MockitoBean
    private EngineOracle engineOracle; // mock Stockfish

    @Autowired
    private CheatDetectionService cheatDetectionService;

    // BONUS debug: pour vérifier que le parsing renvoie des coups
    @Autowired
    private PgnParserService pgnParserService;

    @Test
    void analyzePgn_shouldWork_withSpring_andMockOracle() {

        when(engineOracle.analyzeFen(anyString()))
                .thenReturn(new EngineMoveAnalysis(
                        "e2e4",
                        List.of("e2e4", "d2d4", "g1f3")
                ));

        String pgn = """
[Event "Test"]

1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 *
""";

        var parsed = pgnParserService.parseToUci(pgn);
        System.out.println("UCI parsed = " + parsed.getUciMoves());
        System.out.println("FEN parsed = " + parsed.getFinalFen());

        CheatReport report = cheatDetectionService.analyzePgn(pgn);

        assertNotNull(report);
        assertNotNull(report.getSignals());
        assertFalse(report.getSignals().isEmpty());

        System.out.println("Score = " + report.getScore());
        System.out.println("Verdict = " + report.getVerdict());
        report.getSignals().forEach(s -> System.out.println(s.getName() + " -> " + s.getDetails()));
    }
}