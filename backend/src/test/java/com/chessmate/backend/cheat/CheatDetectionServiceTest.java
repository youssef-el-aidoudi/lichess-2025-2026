package com.chessmate.backend.cheat;

import com.chessmate.backend.cheat.detector.EngineMatchRateDetector;
import com.chessmate.backend.cheat.engine.EngineMoveAnalysis;
import com.chessmate.backend.cheat.engine.EngineOracle;
import com.chessmate.backend.cheat.model.GameData;
import com.chessmate.backend.cheat.pgn.PgnParserService;
import com.chessmate.backend.cheat.service.CheatDetectionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheatDetectionServiceTest {

    @Test
    void engineMatchRateDetector_works() {

        GameData game = new GameData(List.of("e2e4", "e7e5", "g1f3", "b8c6"));

        EngineOracle oracle = new EngineOracle(fen ->
                new EngineMoveAnalysis("e2e4", List.of("e2e4", "d2d4", "g1f3"))
        );

        // ✅ on passe un vrai parser (même s’il n’est pas utilisé dans analyze(GameData))
        PgnParserService parser = new PgnParserService();

        CheatDetectionService service = new CheatDetectionService(
                List.of(new EngineMatchRateDetector(3)),
                oracle,
                parser
        );

        var report = service.analyze(game);

        assertNotNull(report);
        assertFalse(report.getSignals().isEmpty());

        System.out.println("Score = " + report.getScore());
        System.out.println("Verdict = " + report.getVerdict());
        report.getSignals().forEach(s -> System.out.println(s.getName() + " -> " + s.getDetails()));
    }
}