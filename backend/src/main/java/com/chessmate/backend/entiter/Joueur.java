package com.chessmate.backend.entiter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToMany;

@Entity
public class Joueur {
    //attributs
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    //identifiant généner automatiquement dans la bd
    private long id;

    @Column(nullable=false, unique=true, length = 50)
    private String pseudonyme;
    private int elo;
    private int nbVictoires;
    private int nbDefaites;
    private int nbNulles;
    private String equipe;
    private String fideId;

    @OneToOne(mappedBy = "joueur")
    @JsonBackReference
    private Utilisateur utilisateur;

    @OneToMany(mappedBy = "organisateur")
    private List<Tournoi> tournoisOrganises;

    @ManyToMany(mappedBy = "participants")
    private List<Tournoi> tournoisParticipes;

    //constructeurs par défaut
    public Joueur() {}

    //getteurs
    public String getPseudo() {return this.pseudonyme; }
    public int getElo(){ return this.elo; }
    public long getNbVictoires() {return this.nbVictoires; }
    public long getNbDefaites() {return this.nbDefaites; }
    public long getNbNulles() {return this.nbNulles; }
    public String getEquipe() {return this.equipe;}
    public String getFide_id() {return this.fideId; }
 
    //setteurs
    public void setPseudo(String pseudo) { this.pseudonyme = pseudo;}
    public void setElo(int elo) { this.elo = elo; }
    public void setNbVictoires(int nbvictoires) { this.nbVictoires = nbvictoires;}
    public void setNbDefaites(int nbdefaites) { this.nbDefaites = nbdefaites;}
    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public List<Tournoi> getTournoisOrganises() {
        return tournoisOrganises;
    }

    public List<Tournoi> getTournoisParticipes() {
        return tournoisParticipes;
    }

    public void setNbNulles(int nbnulles) { this.nbNulles = nbnulles;}

}
