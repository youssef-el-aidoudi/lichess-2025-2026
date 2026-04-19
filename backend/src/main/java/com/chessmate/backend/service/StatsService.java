package com.chessmate.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.chessmate.backend.dto.PartiesResumeDTO;
import com.chessmate.backend.dto.PerformanceMensuelleDTO;
import com.chessmate.backend.dto.BestMoveDTO;
import com.chessmate.backend.dto.DashboardStatsDTO;
import com.chessmate.backend.dto.OuvertureStatsDTO;
import com.chessmate.backend.repository.JoueurRepository;
import com.chessmate.backend.repository.PartieRepository;
import com.chessmate.backend.entiter.Partie;

@Service
public class StatsService {
    private final PartieRepository partieRepository;
    private final JoueurRepository joueurRepository;

    public StatsService(PartieRepository partieRepository, JoueurRepository jRepository) {
        this.partieRepository = partieRepository;
        this.joueurRepository = jRepository;
    }
    
    public double tauxVictoireJoueur(Long joueurId)
    {
        Double res = partieRepository.getTauxVictoireJoueur(joueurId);
        return res != null ? res : 0.0;
    }

    public int getNbParties(Long joueurId)
    {
        int res = partieRepository.getNbPartiesByJoueur(joueurId);
        return res;
    }

    public List<PartiesResumeDTO> derniereParties(Long joueurId)
    {   
        return partieRepository.getLastParties(joueurId, 5)
            .stream()
            .map(r -> new PartiesResumeDTO(
                (String) r[0],
                (String) r[1],  
                (String) r[2]
            ))
            .toList();
    }

    public List<PerformanceMensuelleDTO> performanceMensuelle(Long joueurId)
    {
        return partieRepository.getPerformanceMensuelle(joueurId)
            .stream()
            .map(r -> new PerformanceMensuelleDTO(
                ((BigDecimal) r[0]).longValue(),
                ((BigDecimal) r[1]).longValue(),
                (String) r[2], 
                (Long) r[3], 
                (Long) r[4], 
                (Long) r[5]
            ))
            .toList();
    }

    public double getPrecisionMoyenne(Long joueurId)
    {
        Double winrate = partieRepository.getTauxVictoireJoueur(joueurId) / 100.0 ;
        int elo = joueurRepository.findById(joueurId).get().getElo();

         // Accuracy basée sur Elo (courbe logistique réaliste)
        double eloFactor = 1 / (1 + Math.exp(-0.003 * (elo - 1500)));
        double baseAccuracy = 65 + eloFactor * 30;

         // Ajustement performance
        double performanceBoost = (winrate - 0.5) * 12;

        double finalAccuracy = baseAccuracy + performanceBoost;

        // Clamp réaliste
        finalAccuracy = Math.max(60, Math.min(finalAccuracy, 98));
        
        return Math.round(finalAccuracy * 100.0) / 100.0;
    }

    private static class MoveStats {
        int total = 0;
        int wins = 0;
    }

    //stats pour les meilleur coups par probabilité
    public List<BestMoveDTO> getBestMovesByPhase(Long joueurId)
    {
        List<Partie> parties = partieRepository.findAllByJoueur(joueurId);

        Map<String, Map<String, MoveStats>> stats = new HashMap<>();

        for(Partie partie : parties)
        {
            if(partie.getPgnPartie() == null) continue;

            boolean isblanc = partie.getJoueurBlanc().getId() == joueurId;

            boolean win = (isblanc && partie.getResultat() == 2) ||
                        (!isblanc && partie.getResultat() == 0);
            
            List<String> moves = extractMoves(partie.getPgnPartie());

            for(int i=0; i < moves.size(); i++)
            {
                if((isblanc && i%2 == 0) ||
                    (!isblanc && i%2 == 1)) {
                        int numeroCoup = (i / 2) + 1;

                        String phase;
                        if(numeroCoup <= 10) phase = "Debut de partie";
                        else if(numeroCoup <=30) phase = "Milieu de partie";
                        else phase = "Finale";

                        stats.putIfAbsent(phase, new HashMap<>());
                        stats.get(phase).putIfAbsent(moves.get(i), new MoveStats());

                        MoveStats ms = stats.get(phase).get(moves.get(i));
                        ms.total++;
                        if(win) ms.wins++;
                    }
                }
            }


        List<BestMoveDTO> result = new ArrayList<>();
        for(String phase : stats.keySet())
        {
            Map<String, MoveStats> phaseMoves = stats.get(phase);
            String bestMove = null;
            MoveStats bestStats = null;

            for(Map.Entry<String, MoveStats> entry : phaseMoves.entrySet())
            {
                if(bestStats == null || entry.getValue().total > bestStats.total)
                {
                    bestMove = entry.getKey();
                    bestStats = entry.getValue();
                }
            }

            if(bestStats != null && bestStats.total >= 5) //filtre anti fake stats
            {
                double winrate = (bestStats.wins * 100) /bestStats.total;
                winrate = Math.round(winrate * 100) / 100;

                result.add(new BestMoveDTO(
                    phase,
                    bestMove,
                    bestStats.total,
                    winrate
                ));
            }
        }
        return result;
    }

    //methode pour extraire les moves
    private List<String> extractMoves(String pgn) {
        // 1. Supprimer commentaires et variations
        String cleaned = pgn
            .replaceAll("\\{[^}]*\\}", "")    // enlever {…}
            .replaceAll("\\([^)]*\\)", "")    // enlever (…)
            .replaceAll("\\d+\\.", "")        // enlever numéros de coups
            .replaceAll("1-0|0-1|1/2-1/2", "")// enlever résultats
            .trim();

        // 2. Séparer tous les tokens par espace
        String[] tokens = cleaned.split("\\s+");

        // 3. Filtrer les coups valides (lettres + chiffres ou roques)
        return Arrays.stream(tokens)
                    .map(t -> t.replaceAll("[!?]+", "")) // enlever annotations ! ou ?
                    .filter(t -> t.matches("[KQNBR]?[a-h]?[1-8]?x?[a-h][1-8](=[QNRB])?|0-0(-0)?")) 
                    .toList();
    }
    //stats sur les ouvertures
    public List<OuvertureStatsDTO> getStatsOuvertures(Long joueurId)
    {
        return partieRepository.getStatsOuvertures(joueurId, 5)
            .stream()
            .map(r -> {

                double winrate = r[2] != null ? ((Double) r[2]) : 0.0;
                double utilisation = r[3] != null ? ((Double) r[3]) : 0.0;

                return new OuvertureStatsDTO(
                    (String) r[0],
                    ((Long) r[1]).intValue(),
                    Math.round(winrate * 100.0) / 100.0,
                    Math.round(utilisation * 100.0) / 100.0                    
                );
            })
            .toList();
    }

    public DashboardStatsDTO getDashboardStats(Long joueurId) {

        Double winRate = tauxVictoireJoueur(joueurId);
        List<PartiesResumeDTO> lastParties = derniereParties(joueurId);
        List<OuvertureStatsDTO> openings = getStatsOuvertures(joueurId);
        Double accuracy = getPrecisionMoyenne(joueurId);
        int nbParties = getNbParties(joueurId);
        List<BestMoveDTO> bestMoves = getBestMovesByPhase(joueurId);
        List<PerformanceMensuelleDTO> perf = performanceMensuelle(joueurId);

        return new DashboardStatsDTO(
            winRate,
            lastParties,
            openings,
            accuracy,
            nbParties,
            bestMoves,
            perf
        );
    }
}
