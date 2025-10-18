package com.chessmate.backend.Entiter;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Utilisateur {
    //attributs
    @Id @GeneratedValue(strategy= GenerationType.AUTO)
    //identifiant généner automatiquement dans la bd
    private long id;
    
    private String email;
    private String hash;
    private String nom;
    private String prenom;
    private LocalDateTime inscription_a;
    private LocalDateTime connexion_a;
    private LocalDateTime banni_a;
    
    //constructeur par défaut
    public Utilisateur() {

    }

    //construteur 
    public Utilisateur(String nom, String prenom, String email)
    {
        this.email = email;
        this.nom = nom;
        this.prenom = prenom;
        //this.inscription_a = LocalDateTime.now();
    }

    //getteurs
    public String getEmail(){ return this.email; }
    public String getNom(){ return this.nom; }
    public String getPrenom(){ return this.prenom; }
    public String getHash() { return this.hash; }

    public LocalDateTime getInscrit_A(){ return this.inscription_a; }
    public LocalDateTime getConnexion_A(){ return this.connexion_a; }
    public LocalDateTime getBanni_A(){ return this.banni_a; }

    //setters
    public void setEmail(String email){ this.email = email; }
    public void setNom(String nom){ this.nom = nom; }
    public void setPrenom(String prenom){ this.prenom = prenom; }

    public void setInscrit_A(LocalDateTime inscription_a){ this.inscription_a = inscription_a; }
    public void setConnexion_A(LocalDateTime connexion_a){ this.connexion_a = connexion_a; }
    public void setBanni_A(LocalDateTime banni_a){ this.banni_a = banni_a; }

}