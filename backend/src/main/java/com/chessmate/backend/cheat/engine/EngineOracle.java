package com.chessmate.backend.cheat.engine;

import java.util.function.Function;

public class EngineOracle {

    private final Function<String, EngineMoveAnalysis> analyzeFenFn;

    public EngineOracle(Function<String, EngineMoveAnalysis> analyzeFenFn) {
        this.analyzeFenFn = analyzeFenFn;
    }

    public EngineMoveAnalysis analyzeFen(String fen) {
        return analyzeFenFn.apply(fen);
    }
}
