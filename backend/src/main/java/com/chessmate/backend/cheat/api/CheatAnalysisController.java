package com.chessmate.backend.cheat.api;

import com.chessmate.backend.cheat.entity.CheatAnalysisEntity;
import com.chessmate.backend.cheat.model.CheatReport;
import com.chessmate.backend.cheat.service.CheatAnalysisStorageService;
import com.chessmate.backend.cheat.service.CheatDetectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cheat")
@CrossOrigin(origins = "*")
public class CheatAnalysisController {

    private final CheatDetectionService cheatDetectionService;
    private final CheatAnalysisStorageService storageService;

    public CheatAnalysisController(CheatDetectionService cheatDetectionService,
                                   CheatAnalysisStorageService storageService) {
        this.cheatDetectionService = cheatDetectionService;
        this.storageService = storageService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody CheatAnalysisRequest request) {
        try {
            if (request == null || request.getPgn() == null || request.getPgn().isBlank()) {
                return ResponseEntity.badRequest().body("Le PGN est obligatoire.");
            }

            CheatReport report = cheatDetectionService.analyzePgn(request.getPgn(), request.getEloWhite(), request.getEloBlack());
            return ResponseEntity.ok(report);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("PGN invalide : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur pendant l'analyse : " + e.getMessage());
        }
    }

    @PostMapping("/analyze-and-save")
    public ResponseEntity<?> analyzeAndSave(@RequestBody CheatAnalysisRequest request) {
        try {
            if (request == null || request.getPgn() == null || request.getPgn().isBlank()) {
                return ResponseEntity.badRequest().body("Le PGN est obligatoire.");
            }

            CheatReport report = cheatDetectionService.analyzePgn(request.getPgn(), request.getEloWhite(), request.getEloBlack());
            CheatAnalysisEntity saved = storageService.saveAnalysis(request.getPgn(), report);

            return ResponseEntity.ok(saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("PGN invalide : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur pendant l'analyse : " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<CheatAnalysisEntity>> history() {
        return ResponseEntity.ok(storageService.findAll());
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<?> historyById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(storageService.findById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Analyse introuvable : " + e.getMessage());
        }
    }
}