package com.chessmate.backend.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.Partie;
import com.chessmate.backend.entiter.Tournoi;
import com.chessmate.backend.repository.TournoiRepository;

@RestController
@RequestMapping("tournoi")
public class TournoiController {
    private final TournoiRepository tourRep;

    public TournoiController(TournoiRepository rp)
    {
        this.tourRep = rp;
    }

    @GetMapping
    public ResponseEntity<?> getAllTournoi()
    {
        return ResponseEntity.ok(tourRep.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<?> getTournoi(
                            @RequestParam(value="code", required=false) String code,
                            @RequestParam(value="libelle", required=false) String libelle,
                            @RequestParam(value="dateDebut", required=false) LocalDateTime dtDebut,
                            @RequestParam(value="dateCreation", required=false) LocalDateTime dtCreeA,
                            @RequestParam(value="dateModif", required=false) LocalDateTime dtModifA)
    {
        if(code!=null)
        {
            return ResponseEntity.ok(tourRep.findByCode(code));
        }
        else if(libelle!=null)
        {
            return ResponseEntity.ok(tourRep.findByLibelle(libelle));
        }
        else if(dtDebut!=null)
        {
            return ResponseEntity.ok(tourRep.findAllTournoisByDateDebut(dtDebut));
        }
        else if(dtCreeA!=null)
        {
            return ResponseEntity.ok(tourRep.findAllTournoisByCreeA(dtCreeA));
        }
        else if(dtModifA!=null)
        {
            return ResponseEntity.ok(tourRep.findAllTournoisByModifierA(dtModifA));
        }

        return ResponseEntity.badRequest()
        .body("Search doit être suivit d'un paramètre : 'code' -> Récupérer un tournoi grâce a son code.\n" +
          "'dateDebut' -> Récupérer les tournois qui ont commencer a une date de début donnée.\n" +
          "'dateCreation' -> Récupérer les tournois créer a une date de début donnée.\n" +
          "'dateModif' -> Récupérer les tournois modifier a une date de début donnée.\n" +
          "'libelle' -> Récupérer un tournoi par son libelle.");

    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getTournoiById(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du tournoi est obligatoire");
        }
        return ResponseEntity.ok(tourRep.findById(id));
    }

    @GetMapping("/{id}/organisateur")
    public ResponseEntity<?> getOrganisateurTournoi(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du tournoi est obligatoire");
        }

        //on recupere le tournoi
        Optional<Tournoi> tournoi = tourRep.findById(id);
        if(tournoi.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun tournoi trouvée avec l'id "+ id);
        }

        //on recupere l'organisateur du tournoi
        return ResponseEntity.ok(tournoi.get().getOrganisateur());
    }

    @GetMapping("/{id}/cadence")
    public ResponseEntity<?> getCadenceTournoi(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du tournoi est obligatoire");
        }

        //on recupere le tournoi
        Optional<Tournoi> tournoi = tourRep.findById(id);
        if(tournoi.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun tournoi trouvée avec l'id "+ id);
        }

        //on recupere l'organisateur du tournoi
        return ResponseEntity.ok(tournoi.get().getCadence());
    }

    @GetMapping("/{id}/parties")
    public ResponseEntity<?> getPartiesTournoi(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du tournoi est obligatoire");
        }

        //on recupere le tournoi
        Optional<Tournoi> tournoi = tourRep.findById(id);
        if(tournoi.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun tournoi trouvée avec l'id "+ id);
        }

        //on recupere l'organisateur du tournoi
        return ResponseEntity.ok(tournoi.get().getParties());
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<?> getParticipantTournoi(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du tournoi est obligatoire");
        }

        //on recupere le tournoi
        Optional<Tournoi> tournoi = tourRep.findById(id);
        if(tournoi.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun tournoi trouvée avec l'id "+ id);
        }

        //on recupere l'organisateur du tournoi
        return ResponseEntity.ok(tournoi.get().getParticipants());
    }

}
