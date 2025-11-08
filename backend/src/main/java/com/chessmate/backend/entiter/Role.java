package com.chessmate.backend.entiter;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

@Entity
public class Role {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    private String libelle;

    @ManyToMany(mappedBy = "roles")
    private Set<Utilisateur> utilisateurs;

    @ManyToMany
    @JoinTable(
        name = "profil_role",
        joinColumns = @JoinColumn(name = "id_role"),
        inverseJoinColumns = @JoinColumn(name = "id_profil")
    )
    private Set<Profil> profils;

    public Role() {}

    public String getLibelle() { return this.libelle; }

    public Set<Utilisateur> getUtilisateurs() {
        return utilisateurs;
    }

    public Set<Profil> getProfils() {
        return profils;
    }
    
}
