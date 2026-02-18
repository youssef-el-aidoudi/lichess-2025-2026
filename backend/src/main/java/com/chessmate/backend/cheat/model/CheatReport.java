package com.chessmate.backend.cheat.model;

import java.util.List;

public class CheatReport {
    private final double score; // 0..100
    private final String verdict;
    private final List<CheatSignal> signals;

    public CheatReport(double score, String verdict, List<CheatSignal> signals) {
        this.score = score;
        this.verdict = verdict;
        this.signals = signals;
    }

    public double getScore() { return score; }
    public String getVerdict() { return verdict; }
    public List<CheatSignal> getSignals() { return signals; }
}
