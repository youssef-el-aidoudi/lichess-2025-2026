package com.chessmate.backend.entiter;


import java.time.LocalDateTime;

import org.hibernate.annotations.ManyToAny;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class Partie {
    //attributs
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    //identifiant généner automatiquement dans la bd
    private long id;

    private String title;
    private Short resultat;
    
    @Column(columnDefinition = "TEXT")
    private String pgn;

    private LocalDateTime dateHeureUTC;
    private Short eloBlanc;
    private Short eloNoir;
    private Short scoreBlancDiff;
    private Short scoreNoirDiff;
    private String titreBlanc;
    private String titreNoir;
    private String typeResultat;
    private int round;
    
    @Column(columnDefinition = "TEXT")
    private String broadcastUrl;
   
    @Column(columnDefinition = "TEXT")
    private String gameUrl;
    
    private String variant;

    @ManyToOne
    @JoinColumn(name="id_suite_coups_stats", nullable=false)
    private SuiteCoupsStats suiteCoupsStats;

    @ManyToOne
    @JoinColumn(name="id_ouverture", nullable=false)
    private Ouverture ouverture;

    @ManyToOne
    @JoinColumn(name="id_cadence", nullable=false)
    private Cadence cadence;

    @ManyToOne
    @JoinColumn(name="id_joueur_blanc", nullable=false)
    private Joueur joueurBlanc;

    @ManyToOne
    @JoinColumn(name="id_joueur_noir", nullable=false)
    private Joueur joueurNoir;

    @ManyToOne
    @JoinColumn(name="id_tournoi", nullable=false)
    private Tournoi tournoi;

    public Partie() {}

    //getteurs
    public Short getResultat() { return this.resultat; }
    public String getPgnPartie() { return this.pgn; }
    public Short getScore_Blanc_Diff() { return this.scoreBlancDiff; }
    public Short getScore_Noir_Diff() { return this.scoreNoirDiff; }
    public String getTitre_blanc() { return this.titreBlanc; }
    public String getTitre_noir() { return this.titreNoir; }
    public String getType_Resultat() { return this.typeResultat; }
    public int getRound() { return this.round; }
    public Short getElo_Blanc() { return this.eloBlanc; }
    public Short getElo_Noir() { return this.eloNoir; }
    public LocalDateTime getDateHeurePartie() { return this.dateHeureUTC; }

    public String getTitle() {
        return title;
    }

    public String getBroadcastUrl() {
        return broadcastUrl;
    }

    public String getGameUrl() {
        return gameUrl;
    }

    public String getVariant() {
        return variant;
    }

    public Cadence getCadence() {
        return cadence;
    }

    public Joueur getJoueurBlanc() {
        return joueurBlanc;
    }

    public Joueur getJoueurNoir() {
        return joueurNoir;
    }

    public Tournoi getTournoi() {
        return tournoi;
    }

    public Ouverture getOuverture() {
        return ouverture;
    }

    public SuiteCoupsStats getSuiteCoupsStats() {
        return suiteCoupsStats;
    }


}
