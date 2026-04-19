package com.chessmate.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.dto.BestMoveDTO;
import com.chessmate.backend.dto.DashboardStatsDTO;
import com.chessmate.backend.dto.OuvertureStatsDTO;
import com.chessmate.backend.dto.PartiesResumeDTO;
import com.chessmate.backend.dto.PerformanceMensuelleDTO;
import com.chessmate.backend.service.StatsService;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/joueur/{id}/taux-victoire")
    public double tauxVictoire(@PathVariable Long id) {
        return statsService.tauxVictoireJoueur(id);
    }

    @GetMapping("/joueur/{id}/perf-mensuelle")
    public List<PerformanceMensuelleDTO> performanceMensuelle(@PathVariable Long id)
    {
        return statsService.performanceMensuelle(id);
    }

    @GetMapping("/joueur/{id}/last-parties")
    public List<PartiesResumeDTO> derniereParties(@PathVariable Long id)
    {
        return statsService.derniereParties(id);
    }

    @GetMapping("/joueur/{id}/nb-parties")
    public int getNbPartiesJoueur(@PathVariable Long id)
    {
        return statsService.getNbParties(id);
    }
    
    @GetMapping("/joueur/{id}/accuracyAverage")
    public double getAccuracyAverage(@PathVariable Long id)
    {
        return statsService.getPrecisionMoyenne(id);
    }

    @GetMapping("/joueur/{id}/bestMoves")
    public List<BestMoveDTO> bestMoves(@PathVariable Long id)
    {
        return statsService.getBestMovesByPhase(id);
    }

    @GetMapping("/joueur/{id}/bestOuvertures")
    public List<OuvertureStatsDTO> ouvertureStats(@PathVariable Long id)
    {
        return statsService.getStatsOuvertures(id);
    }

    @GetMapping("/joueur/{id}/allStats")
    public DashboardStatsDTO getAllStats(@PathVariable Long id) {
        return statsService.getDashboardStats(id);
    }   
}
