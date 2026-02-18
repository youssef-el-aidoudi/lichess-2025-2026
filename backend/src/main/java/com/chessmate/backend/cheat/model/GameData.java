package com.chessmate.backend.cheat.model;

import java.util.List;

public class GameData {
    private final List<String> uciMoves;

    public GameData(List<String> uciMoves) {
        this.uciMoves = uciMoves;
    }

    public List<String> getUciMoves() {
        return uciMoves;
    }
}
