package com.chessmate.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.Ouverture;
import com.chessmate.backend.entiter.Utilisateur;
import com.chessmate.backend.repository.UtilisateurRepository;

@RestController
@RequestMapping("/users")
public class UtilisateurController {

    private final UtilisateurRepository repUser;

    public UtilisateurController(UtilisateurRepository rep)
    {
        this.repUser = rep;
    }
    
    @GetMapping
    public ResponseEntity<?> getAllUtilisateur()
    {
        return ResponseEntity.ok(repUser.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<?> getUtilisateur(@RequestParam(value="email", required=false) String email,
                                            @RequestParam(value="dateInscription", required=false) LocalDateTime dtIns,
                                            @RequestParam(value="dateConnexion", required=false) LocalDateTime dtCon,
                                            @RequestParam(value="dateBanni", required=false) LocalDateTime dtBan)
    {
        if(email != null)
        {
            return ResponseEntity.ok(repUser.findByEmail(email));
        }
        else if(dtIns != null)
        {
            return ResponseEntity.ok(repUser.findByInscriptionA(dtIns));
        }
        else if(dtCon != null)
        {
            return ResponseEntity.ok(repUser.findByConnexionA(dtCon));
        }
        else if(dtBan != null)
        {
            return ResponseEntity.ok(repUser.findByBanniA(dtBan));
        }
        
        return ResponseEntity.badRequest()
        .body("Search doit être suivit d'un paramètre : 'email' -> Récupérer un utilisateur grâce a son mail.\n" +
          "'dateInscription' -> Récupérer les utilisateurs inscrit a une date de début donnée.\n" +
          "'dateConnexion' -> Récupérer les utilisateurs qui se sont connecter a une date de début donnée.\n" +
          "'datBanni' -> Récupérer les utilisateurs qui on été banni a une date de début donnée.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUtilisateurById(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' de l'utilisateur est obligatoire");
        }

        return ResponseEntity.ok(repUser.findById(id));
    }

    //recuperer les informations du joueur associé a l'utilisateur
    @GetMapping("/{id}/monjoueur")
    public ResponseEntity<?> getJoueurByUser(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' de l'utilisateur est obligatoire");
        }

        Optional<Utilisateur> user = repUser.findById(id);
        if(user.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun utilisateur trouvée avec l'id "+ id);
        }

        return ResponseEntity.ok(user.get().getJoueur());
    }

    @GetMapping("/{id}/profils")
    public ResponseEntity<?> getProfilByUser(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' de l'utilisateur est obligatoire");
        }

        Optional<Utilisateur> user = repUser.findById(id);
        if(user.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun utilisateur trouvée avec l'id "+ id);
        }

        return ResponseEntity.ok(user.get().getProfils());
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<?> getRoleByUser(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' de l'utilisateur est obligatoire");
        }

        Optional<Utilisateur> user = repUser.findById(id);
        if(user.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun utilisateur trouvée avec l'id "+ id);
        }

        return ResponseEntity.ok(user.get().getRoles());
    }
}
