package com.chessmate.backend.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.Joueur;
import com.chessmate.backend.repository.JoueurRepository;

@RestController
@RequestMapping("/joueurs")
public class JoueurController {

    private final JoueurRepository joueurRep;

    public JoueurController(JoueurRepository jp)
    {
        this.joueurRep = jp;
    }

    @GetMapping
    public ResponseEntity<?> getAllJoueur(){
        return ResponseEntity.ok(joueurRep.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJoueurById(@PathVariable Long id)
    {
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du joueur est obligatoire");
        }

        return ResponseEntity.ok(joueurRep.findById(id));
    }

    @GetMapping("/search")
     public ResponseEntity<?> getJoueur(@RequestParam(value="pseudo", required=false) String pseudo,
                                        @RequestParam(value="equipe", required=false) String equipe,
                                        @RequestParam(value="elo", required=false) Integer elo)
    {
        if(pseudo!=null)
        {
            return ResponseEntity.ok(joueurRep.findByPseudonyme(pseudo));
        }
        else if(equipe!=null)
        {
            return ResponseEntity.ok(joueurRep.findByEquipe(equipe));
        }
        else if(elo!=null)
        {
            return ResponseEntity.ok(joueurRep.findByElo(elo));
        }

        return ResponseEntity.badRequest()
        .body("Search doit être suivit d'un paramètre : 'email' -> Récupérer un joueur grâce a son pseudo.\n" +
          "'equipe' -> Récupérer les joueurs d'une equipe donnée.\n" +
          "'elo' -> Récupérer les joueurs qui ayant le même elo.");
    }

    @GetMapping("/{id}/account")
    public ResponseEntity<?> getAccountJoueur(@PathVariable Long id)
    {
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du joueur est obligatoire");
        }

        Optional<Joueur> joueur = joueurRep.findById(id);

        if(joueur.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun joueur trouvé avec l'id "+ id);
        }

        return ResponseEntity.ok(joueur.get().getUtilisateur());
    }

    @GetMapping("/{id}/mestournois")
    public ResponseEntity<?> getTournoiOrganiserJoueur(@PathVariable Long id)
    {
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du joueur est obligatoire");
        }

        Optional<Joueur> joueur = joueurRep.findById(id);

        if(joueur.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun joueur trouvé avec l'id "+ id);
        }

        return ResponseEntity.ok(joueur.get().getTournoisOrganises());
    }
    

    @GetMapping("/{id}/tounois")
    public ResponseEntity<?> getTournoiParticiperJoueur(@PathVariable Long id)
    {
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du joueur est obligatoire");
        }

        Optional<Joueur> joueur = joueurRep.findById(id);

        if(joueur.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun joueur trouvé avec l'id "+ id);
        }

        return ResponseEntity.ok(joueur.get().getTournoisParticipes());
    }
}
