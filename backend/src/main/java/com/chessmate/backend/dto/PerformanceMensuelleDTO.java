package com.chessmate.backend.dto;

public record PerformanceMensuelleDTO(
    Long annee,
    Long mois_numero,
    String mois,
    Long victoires,
    Long defaites,
    Long nulles
) {}
