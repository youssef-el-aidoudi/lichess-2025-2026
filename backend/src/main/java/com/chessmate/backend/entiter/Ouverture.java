package com.chessmate.backend.entiter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Ouverture {
    //attributs
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    //identifiant généner automatiquement dans la bd
    private long id;

    @Column(nullable=false)
    private String code;

    @Column(nullable=false)
    private String libelle;

    @ManyToOne
    @JoinColumn(name="id_suite_coups_stats")
    private SuiteCoupsStats suiteCoupsStats;
    

    public Ouverture() {}

    //getteurs
    public String getCodeOuverture(){ return this.code; }
    public String getLibelleOuverture() { return this.libelle; }

    public SuiteCoupsStats getSuiteCoupsStats() {
        return suiteCoupsStats;
    }
}
