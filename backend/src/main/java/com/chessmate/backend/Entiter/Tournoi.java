package com.chessmate.backend.Entiter;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Tournoi {
    //attributs
    @Id @GeneratedValue(strategy= GenerationType.AUTO)
    //identifiant généner automatiquement dans la bd
    private long id;

    private String libelle;
    private String code;
    private LocalDateTime cree_a;
    private LocalDateTime modifier_a;

    public Tournoi() {}

    public Tournoi(String libelle, String code, LocalDateTime cree)
    {
        this.libelle = libelle;
        this.code = code;
        this.cree_a = cree;
    }

    //getteurs
    public String getLibelle() {return this.libelle;}
    public String getCode(){ return this.code; }
    public LocalDateTime getCree_A() {return this.cree_a;}
    public LocalDateTime getModifier_A() {return this.modifier_a;}

    //setteurs
    public void setLibelle(String l) { this.libelle = l; }
    public void setCode(String c) { this.code = c; }
    public void setModifier_A(LocalDateTime dt){ this.modifier_a = dt;}
}
