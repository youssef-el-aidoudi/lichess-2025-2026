package com.chessmate.backend.service;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class StockfishService {

    private final StockfishWrapper stockfish;

    public StockfishService(StockfishWrapper stockfish) {
        this.stockfish = stockfish;
    }

    public String getBestMove(String moves, String mode) {
        return stockfish.getBestMove(moves, mode);
    }

    public Map<String, Object> evaluatePosition(String moves, int depth) {
        return stockfish.getEvaluation(moves, depth);
    }

    public void setDepth(int depth) {
        stockfish.setDepth(depth);
    }

    public void setMovetime(int movetime) {
        stockfish.setMovetime(movetime);
    }
}
