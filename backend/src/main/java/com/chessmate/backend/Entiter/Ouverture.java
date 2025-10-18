package com.chessmate.backend.Entiter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Ouverture {
    //attributs
    @Id @GeneratedValue(strategy= GenerationType.AUTO)
    //identifiant généner automatiquement dans la bd
    private long id;

    private String code;
    private String libelle;

    public Ouverture() {}

    public Ouverture(String code, String libelle)
    {
        this.code = code;
        this.libelle = libelle;
    }

    //getteurs
    public String getCodeOuverture(){ return this.code; }
    public String getLibelleOuverture() { return this.libelle; }
}
