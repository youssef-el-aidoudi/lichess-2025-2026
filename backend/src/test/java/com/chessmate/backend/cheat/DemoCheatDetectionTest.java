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

class DemoCheatDetectionTest {

    @Test
    void demo_cheat_detection_from_pgn() {

        // 1️⃣ PGN d'entrée
      String pgn = """
[Event "Demo"]
[Site "Local"]
[Date "2026.02.09"]
[Round "1"]
[White "Player"]
[Black "Player"]
[Result "*"]

1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 *
""";

        // 2️⃣ Parsing PGN -> UCI
        PgnParserService parser = new PgnParserService();
        var parsed = parser.parseToUci(pgn);

        GameData game = new GameData(parsed.getUciMoves());

        System.out.println("UCI = " + parsed.getUciMoves());
        System.out.println("FEN = " + parsed.getFinalFen());

        // 3️⃣ Oracle FAKE (pas Stockfish réel)
        EngineOracle oracle = new EngineOracle(fen ->
            new EngineMoveAnalysis(
                "e2e4",
                List.of("e2e4", "d2d4", "g1f3")
            )
        );

        // 4️⃣ Service de détection
        CheatDetectionService service = new CheatDetectionService(
            List.of(new EngineMatchRateDetector(3)),
            oracle,
            parser
        );

        var report = service.analyze(game);

        // 5️⃣ Résultat
        assertNotNull(report);
        assertFalse(report.getSignals().isEmpty());

        System.out.println("====== CHEAT REPORT ======");
        System.out.println("Score   : " + report.getScore());
        System.out.println("Verdict : " + report.getVerdict());
        report.getSignals()
              .forEach(s -> System.out.println(
                  s.getName() + " -> " + s.getDetails()
              ));
    }
}