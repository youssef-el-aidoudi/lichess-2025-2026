package com.chessmate.backend.cheat.service;

import com.chessmate.backend.cheat.detector.CheatDetector;
import com.chessmate.backend.cheat.engine.EngineOracle;
import com.chessmate.backend.cheat.model.CheatReport;
import com.chessmate.backend.cheat.model.CheatSignal;
import com.chessmate.backend.cheat.model.GameData;
import com.chessmate.backend.cheat.pgn.PgnParserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CheatDetectionService {

    private final List<CheatDetector> detectors;
    private final EngineOracle oracle;
    private final PgnParserService pgnParser;

    public CheatDetectionService(List<CheatDetector> detectors,
                                 EngineOracle oracle,
                                 PgnParserService pgnParser) {
        this.detectors = detectors;
        this.oracle = oracle;
        this.pgnParser = pgnParser;
    }

    public CheatReport analyze(GameData game) {
        List<CheatSignal> signals = new ArrayList<>();
        double total = 0;

        for (CheatDetector d : detectors) {
            CheatSignal s = d.detect(game, oracle);
            signals.add(s);
            total += s.getScore();
        }

        double score = signals.isEmpty() ? 0 : total / signals.size();
        String verdict =
                score >= 80 ? "Très suspect" :
                score >= 60 ? "Suspect" :
                score >= 40 ? "Un peu suspect" :
                "Normal";

        return new CheatReport(score, verdict, signals);
    }

    public CheatReport analyzePgn(String pgn) {
        var parsed = pgnParser.parseToUci(pgn);
        return analyze(new GameData(parsed.getUciMoves()));
    }
}