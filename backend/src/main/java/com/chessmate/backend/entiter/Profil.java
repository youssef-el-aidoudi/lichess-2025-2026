package com.chessmate.backend.entiter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Profil {
    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    
    private String libelle;

    public Profil() {}

    public Profil(String libelle)
    {
        this.libelle = libelle;
    }

    public String getLibelle(){ return this.libelle; }
}
