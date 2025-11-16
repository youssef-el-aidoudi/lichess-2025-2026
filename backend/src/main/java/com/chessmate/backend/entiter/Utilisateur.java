package com.chessmate.backend.entiter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "Utilisateurs")
@Data
public class Utilisateur {
    //attributs
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    //identifiant généner automatiquement dans la bd
    private long id;
    private String nom;
    private String prenom;
    private String role;
    
    @Column(nullable=false, unique=true)
    private String email;

    @Column(nullable=false)
    private String hash;
    

    @Column(name = "inscription_a", nullable=false)
    private LocalDateTime inscriptionA;
    @Column(name = "connexion_a")
    private LocalDateTime connexionA;
    @Column(name = "banni_a")
    private LocalDateTime banniA;
    
    @OneToOne
    @JoinColumn(name = "id_joueur", unique = true, nullable = true)
    @JsonManagedReference
    private Joueur joueur;

    @ManyToMany
    @JoinTable(
        name = "utilisateur_role",
        joinColumns = @JoinColumn(name = "id_utilisateur"),
        inverseJoinColumns = @JoinColumn(name = "id_role")
    )
    private Set<Role> roles;

    @ManyToMany
    @JoinTable(
        name = "utilisateur_profil",
        joinColumns = @JoinColumn(name = "id_utilisateur"),
        inverseJoinColumns = @JoinColumn(name = "id_profil")
    )
    private Set<Profil> profils = new HashSet<>();



    //getteurs
    public String getEmail(){ return this.email; }
    public String getNom(){ return this.nom; }
    public String getPrenom(){ return this.prenom; }
    public String getHash() { return this.hash; }
    public String getRole() { return this.role; }

    public LocalDateTime getInscrit_A(){ return this.inscriptionA; }
    public LocalDateTime getConnexion_A(){ return this.connexionA; }
    public LocalDateTime getBanni_A(){ return this.banniA; }

    //setters
    public void setEmail(String email){ this.email = email; }
    public void setNom(String nom){ this.nom = nom; }
    public void setPrenom(String prenom){ this.prenom = prenom; }
    public void setHash(String hash){ this.hash = hash; }
    public void setRole(String role) { this.role = role; }

    public void setInscrit_A(LocalDateTime inscription_a){ this.inscriptionA = inscription_a; }
    public void setConnexion_A(LocalDateTime connexion_a){ this.connexionA = connexion_a; }
    public void setBanni_A(LocalDateTime banni_a){ this.banniA= banni_a; }

    public Joueur getJoueur() {
        return joueur;
    }

    public void setJoueur(Joueur j){ this.joueur=j; }

    public Set<Role> getRoles() {
        return roles;
    }

    public Set<Profil> getProfils() {
        return profils;
    }

}