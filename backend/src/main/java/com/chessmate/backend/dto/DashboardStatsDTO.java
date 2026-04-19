package com.chessmate.backend.dto;

import java.util.List;

public record DashboardStatsDTO(
    Double winRate,
    List<PartiesResumeDTO> lastParties,
    List<OuvertureStatsDTO> bestOpenings,
    Double accuracy,
    int nbParties,
    List<BestMoveDTO> bestMoves,
    List<PerformanceMensuelleDTO> performanceMensuelle
) {}
