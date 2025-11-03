package com.chessmate.backend.entiter;


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
    private LocalDateTime dateHeure;
    private int eloBlanc;
    private int eloNoir;
    private int score_blanc_diff;
    private int score_noir_diff;
    private String titreBlanc;
    private String titreNoir;
    private String typeResultat;
    private int round;
    private String code;

    public Partie() {}

    public Partie(String result, String pgn, LocalDateTime dt, int elo_b, int elo_n, 
                int sc_b_diff, int sc_n_diff, String titre_n, String titre_b, String type_result, 
                int round, String code)
    {
        this.resultat = result;
        this.pgn = pgn;
        this.dateHeure = dt;
        this.eloBlanc = elo_b;
        this.eloNoir = elo_n;
        this.score_blanc_diff = sc_b_diff;
        this.score_noir_diff = sc_n_diff;
        this.titreBlanc = titre_b;
        this.titreNoir = titre_n;
        this.typeResultat = type_result;
        this.round = round;
        this.code = code;
    }

    //getteurs
    public String getResultat() { return this.resultat; }
    public String getPgnPartie() { return this.pgn; }
    public int getScore_Blanc_Diff() { return this.score_blanc_diff; }
    public int getScore_Noir_Diff() { return this.score_noir_diff; }
    public String getTitre_blanc() { return this.titreBlanc; }
    public String getTitre_noir() { return this.titreNoir; }
    public String getType_Resultat() { return this.typeResultat; }
    public String getCodePartie() { return this.code; }
    public int getRound() { return this.round; }
    public int getElo_Blanc() { return this.eloBlanc; }
    public int getElo_Noir() { return this.eloNoir; }
    public LocalDateTime getDateHeurePartie() { return this.dateHeure; }


}
