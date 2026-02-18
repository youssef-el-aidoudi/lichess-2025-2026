package com.chessmate.backend.cheat.pgn;

import com.chessmate.backend.cheat.model.PGNParseResult;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.pgn.PgnHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class PgnParserService {

    public PGNParseResult parseToUci(String pgn) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("game-", ".pgn");
            Files.writeString(tmp, pgn, StandardCharsets.UTF_8);

            PgnHolder holder = new PgnHolder(tmp.toString());
            holder.loadPgn();

            var games = holder.getGames();
            if (games == null || games.isEmpty()) {
                throw new IllegalArgumentException("PGN invalide (aucune game trouvée)");
            }

            var game = games.get(0);
            var halfMoves = game.getHalfMoves();

            Board board = new Board();
            List<String> uci = new ArrayList<>();

            if (halfMoves != null) {
                for (Move m : halfMoves) {
                    uci.add(m.toString().toLowerCase());
                    board.doMove(m);
                }
            }

            return new PGNParseResult(uci, board.getFen());
        } catch (Exception e) {
            throw new IllegalArgumentException("Erreur parsing PGN: " + e.getMessage(), e);
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            }
        }
    }
}