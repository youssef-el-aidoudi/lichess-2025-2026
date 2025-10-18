package com.chessmate.backend.Entiter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Joueur {
    //attributs
    @Id @GeneratedValue(strategy= GenerationType.AUTO)
    //identifiant généner automatiquement dans la bd
    private long id;

    private String pseudo;
    private int elo;
    private int nb_victoires;
    private int nb_defaites;
    private int nb_nulles;

    //constructeurs par défaut
    public Joueur() {}

    public Joueur(String pseudo, int elo)
    {
        this.pseudo = pseudo;
        this.elo = elo;
    }

    //getteurs
    public String getPseudo() {return this.pseudo; }
    public int getElo(){ return this.elo; }
    public long getNbVictoires() {return this.nb_victoires; }
    public long getNbDefaites() {return this.nb_defaites; }
    public long getNbNulles() {return this.nb_nulles; }

    //setteurs
    public void setPseudo(String pseudo) { this.pseudo = pseudo;}
    public void setElo(int elo) { this.elo = elo; }
    public void setNbVictoires(int nbvictoires) { this.nb_victoires = nbvictoires;}
    public void setNbDefaites(int nbdefaites) { this.nb_defaites = nbdefaites;}
    public void setNbNulles(int nbnulles) { this.nb_nulles = nbnulles;}

}
