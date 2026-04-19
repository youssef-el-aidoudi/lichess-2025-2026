package com.chessmate.backend.controller;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chessmate.backend.entiter.Joueur;
import com.chessmate.backend.entiter.OnlinePartie;
import com.chessmate.backend.repository.JoueurRepository;
import com.chessmate.backend.repository.OnlinePartieRepository;

@RestController
@RequestMapping("/api/online-parties")
public class OnlinePartieController {

    private final OnlinePartieRepository partieRepo;
    private final JoueurRepository joueurRepo;

    public OnlinePartieController(OnlinePartieRepository pr, JoueurRepository jr) {
        this.partieRepo = pr;
        this.joueurRepo = jr;
    }

    /** POST /api/online-parties/save - Called by ai_engine to save game results */
    @PostMapping("/save")
    public ResponseEntity<?> saveGameResult(@RequestBody Map<String, Object> body) {
        try {
            String gameId = (String) body.get("gameId");
            String whitePseudo = (String) body.get("white");
            String blackPseudo = (String) body.get("black");
            int resultat = ((Number) body.get("resultat")).intValue();
            String resultType = (String) body.get("resultType");
            String pgn = (String) body.get("pgn");
            String timeControl = (String) body.get("timeControl");
            int totalMoves = ((Number) body.get("totalMoves")).intValue();

            Optional<Joueur> whiteOpt = joueurRepo.findByPseudonyme(whitePseudo);
            Optional<Joueur> blackOpt = joueurRepo.findByPseudonyme(blackPseudo);

            if (whiteOpt.isEmpty() || blackOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Joueur(s) non trouvé(s)");
            }

            OnlinePartie partie = new OnlinePartie();
            partie.setGameId(gameId);
            partie.setJoueurBlanc(whiteOpt.get());
            partie.setJoueurNoir(blackOpt.get());
            partie.setResultat((short) resultat);
            partie.setResultType(resultType);
            partie.setPgn(pgn);
            partie.setTimeControl(timeControl);
            partie.setTotalMoves(totalMoves);
            partie.setPlayedAt(LocalDateTime.now());

            partieRepo.save(partie);
            return ResponseEntity.ok(Map.of("message", "Partie sauvegardée", "id", partie.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur: " + e.getMessage());
        }
    }

    /** GET /api/online-parties/player/{pseudo} - Get game history for a player */
    @GetMapping("/player/{pseudo}")
    public ResponseEntity<?> getPlayerGames(@PathVariable String pseudo) {
        Optional<Joueur> joueurOpt = joueurRepo.findByPseudonyme(pseudo);
        if (joueurOpt.isEmpty()) return ResponseEntity.notFound().build();

        List<OnlinePartie> parties = partieRepo.findByJoueur(joueurOpt.get());
        List<Map<String, Object>> result = parties.stream().map(p -> {
            Map<String, Object> data = new HashMap<>();
            data.put("gameId", p.getGameId());
            data.put("white", p.getJoueurBlanc().getPseudo());
            data.put("black", p.getJoueurNoir().getPseudo());
            data.put("resultat", p.getResultat());
            data.put("resultType", p.getResultType());
            data.put("timeControl", p.getTimeControl());
            data.put("totalMoves", p.getTotalMoves());
            data.put("playedAt", p.getPlayedAt().toString());
            return data;
        }).toList();

        return ResponseEntity.ok(result);
    }
}
