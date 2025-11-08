package com.chessmate.backend.entiter;


import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class SuiteCoupsStats {
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    @Column(columnDefinition = "TEXT", nullable=false)
    private String pgn;
    
    @Column(columnDefinition = "INT DEFAULT 0")
    private int nbVictoires = 0;
    @Column(columnDefinition = "INT DEFAULT 0")
    private int nbDefaites = 0 ;
    @Column(columnDefinition = "INT DEFAULT 0")
    private int nbNulles = 0;
    private String probabiliteCoup;

    @ManyToOne
    @JoinColumn(name="id_suite_coups_stats_precedente")
    private SuiteCoupsStats suitePrecedente;

    @OneToMany(mappedBy = "suitePrecedente")
    private List<SuiteCoupsStats> suitesSuivantes;
    
    public SuiteCoupsStats() {}

    public String getPgn() {
        return pgn;
    }

    public int getNbVictoires() {
        return nbVictoires;
    }

    public int getNbDefaites() {
        return nbDefaites;
    }

    public int getNbNulles() {
        return nbNulles;
    }

    public String getProbabiliteCoup() {
        return probabiliteCoup;
    }

    public SuiteCoupsStats getSuiteCoupsStatsPrecedente() {
        return suitePrecedente;
    }

    public List<SuiteCoupsStats> getSuitesSuivantes() {
        return suitesSuivantes;
    }



}
