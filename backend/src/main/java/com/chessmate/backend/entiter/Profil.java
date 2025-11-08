package com.chessmate.backend.entiter;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

@Entity
public class Profil {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    
    private String libelle;

    @ManyToMany(mappedBy = "profils")
    private Set<Utilisateur> utilisateurs;

    @ManyToMany(mappedBy = "profils")
    private Set<Role> roles;

    public Profil() {}

    public Profil(String libelle)
    {
        this.libelle = libelle;
    }

    public String getLibelle(){ return this.libelle; }

    public Set<Utilisateur> getUtilisateurs() {
        return utilisateurs;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
