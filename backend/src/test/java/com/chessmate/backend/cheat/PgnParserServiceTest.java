package com.chessmate.backend.cheat;

import com.chessmate.backend.cheat.pgn.PgnParserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PgnParserServiceTest {

    @Test
    void parsePgn_toUci_shouldWork() {
        String pgn = """
                [Event "Test"]
                [Site "Local"]
                [Date "2026.02.09"]
                [Round "1"]
                [White "A"]
                [Black "B"]
                [Result "*"]

                1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 *
                """;

        PgnParserService parser = new PgnParserService();
        var result = parser.parseToUci(pgn);

        assertNotNull(result);
        assertTrue(result.getUciMoves().size() > 0);

        System.out.println(result.getUciMoves());
        System.out.println("Final FEN: " + result.getFinalFen());
    }
}
