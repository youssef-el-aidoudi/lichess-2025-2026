package com.chessmate.backend.Entiter;


import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Partie {
    //attributs
    @Id @GeneratedValue(strategy= GenerationType.AUTO)
    //identifiant généner automatiquement dans la bd
    private long id;

    private String resultat;
    private String pgn;
    private LocalDateTime date_heure;
    private int elo_blanc;
    private int elo_noir;
    private int score_blanc_diff;
    private int score_noir_diff;
    private String titre_blanc;
    private String titre_noir;
    private String type_resultat;
    private int round;
    private String code;

    public Partie() {}

    public Partie(String result, String pgn, LocalDateTime dt, int elo_b, int elo_n, 
                int sc_b_diff, int sc_n_diff, String titre_n, String titre_b, String type_result, 
                int round, String code)
    {
        this.resultat = result;
        this.pgn = pgn;
        this.date_heure = dt;
        this.elo_blanc = elo_b;
        this.elo_noir = elo_n;
        this.score_blanc_diff = sc_b_diff;
        this.score_noir_diff = sc_n_diff;
        this.titre_blanc = titre_b;
        this.titre_noir = titre_n;
        this.type_resultat = type_result;
        this.round = round;
        this.code = code;
    }

    //getteurs
    public String getResultat() { return this.resultat; }
    public String getPgnPartie() { return this.pgn; }
    public int getScore_Blanc_Diff() { return this.score_blanc_diff; }
    public int getScore_Noir_Diff() { return this.score_noir_diff; }
    public String getTitre_blanc() { return this.titre_blanc; }
    public String getTitre_noir() { return this.titre_noir; }
    public String getType_Resultat() { return this.type_resultat; }
    public String getCodePartie() { return this.code; }
    public int getRound() { return this.round; }
    public int getElo_Blanc() { return this.elo_blanc; }
    public int getElo_Noir() { return this.elo_noir; }
    public LocalDateTime getDateHeurePartie() { return this.date_heure; }


}
