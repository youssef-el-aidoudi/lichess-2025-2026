package com.chessmate.backend.entiter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Cadence {
    //attributs
    @Id @GeneratedValue(strategy= GenerationType.AUTO)
    //identifiant généner automatiquement dans la bd
    private long id;

    private String libelle;
    private int temps;
    private int increment;

    public Cadence() {}

    public Cadence(String libelle, int time, int incr)
    {
        this.libelle = libelle;
        this.temps = time;
        this.increment = incr;
    }

    //getteurs
    public String getLibelle() {return this.libelle;}
    public int getTempsCadence() {return this.temps;}
    public int getIncrement() {return this.increment;}

    //setteurs
    public void setLibelle(String l) { this.libelle = l; }
    public void setTemps(int t) { this.temps = t; }
    public void setIncrement(int i) { this.increment = i; }


    
}
