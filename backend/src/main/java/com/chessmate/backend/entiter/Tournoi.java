package com.chessmate.backend.entiter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Tournoi {
    //attributs
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    //identifiant généner automatiquement dans la bd
    private long id;

    @Column(unique=true)
    private String libelle;

    private String code;
    
    @Column(columnDefinition = "TEXT")
    private String site;
    
    @Column(columnDefinition = "TEXT")
    private String broadcastUrl;
    
    private LocalDate dateDebut;
    private LocalDateTime creeA;
    private LocalDateTime modifierA;

    @ManyToOne
    @JoinColumn(name="id_organisateur", nullable=false)
    private Joueur organisateur;

    @ManyToOne
    @JoinColumn(name="id_cadence", nullable=false)
    private Cadence cadence;

    @ManyToMany
    @JoinTable(
        name = "participer",
        joinColumns = @JoinColumn(name = "id_tournoi"),
        inverseJoinColumns = @JoinColumn(name = "id_joueur")
    )
    private Set<Joueur> participants = new HashSet<>();

    @OneToMany(mappedBy = "tournoi")
    private List<Partie> parties;

    public Tournoi() {}
    
    //getteurs
    public String getLibelle() {return this.libelle;}
    public String getCode(){ return this.code; }
    public LocalDateTime getCree_A() {return this.creeA;}
    public LocalDateTime getModifier_A() {return this.modifierA;}

    //setteurs
    public void setLibelle(String l) { this.libelle = l; }
    public void setCode(String c) { this.code = c; }
    public void setModifier_A(LocalDateTime dt){ this.modifierA = dt;}

    public String getBroadcastUrl() {
        return broadcastUrl;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public String getSite() {
        return site;
    }

    public Set<Joueur> getParticipants() {
        return participants;
    }

    public Joueur getOrganisateur() {
        return organisateur;
    }

    public Cadence getCadence() {
        return cadence;
    }

    public List<Partie> getParties() {
        return parties;
    }
}
