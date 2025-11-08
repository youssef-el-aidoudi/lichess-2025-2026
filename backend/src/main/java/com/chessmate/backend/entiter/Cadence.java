package com.chessmate.backend.entiter;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
@Entity
public class Cadence {
    //attributs
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    //identifiant généner automatiquement dans la bd
    private long id;

    @Column(nullable=false, unique=true)
    private String libelle;
    private int temps;
    private int increment;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private CategorieCadence typePartie;

    @OneToMany(mappedBy = "cadence")
    private List<Partie> parties;

    @OneToMany(mappedBy = "cadence")
    private List<Tournoi> tournois;

    public Cadence() {}

    //getteurs
    public String getLibelle() {return this.libelle;}
    public int getTempsCadence() {return this.temps;}
    public int getIncrement() {return this.increment;}

    //setteurs
    public void setLibelle(String l) { this.libelle = l; }
    public void setTemps(int t) { this.temps = t; }
    public void setIncrement(int i) { this.increment = i; }

    public CategorieCadence getTypePartie() {
        return typePartie;
    }

    public List<Partie> getParties() {
        return parties;
    }

    public List<Tournoi> getTournois() {
        return tournois;
    }


    public enum CategorieCadence {
        Bullet, Blitz, Rapide, Classique
    }
}
