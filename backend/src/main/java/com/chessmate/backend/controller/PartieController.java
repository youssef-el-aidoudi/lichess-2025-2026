package com.chessmate.backend.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.catalina.connector.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.Partie;
import com.chessmate.backend.repository.PartieRepository;

@RestController
@RequestMapping("/parties")
public class PartieController {
    private final PartieRepository partRepo;

    public PartieController(PartieRepository p)
    {
        this.partRepo = p;
    }

    @GetMapping
    public ResponseEntity<?> getPartie()
    {
        return ResponseEntity.ok(partRepo.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<?> getPartieSearch(@RequestParam(value = "title", required = false) String title,
                            @RequestParam(value="date", required=false) LocalDateTime dt,
                            @RequestParam(value="result", required=false) String type_result)
    {
        if(title!=null)
        {
            return ResponseEntity.ok(partRepo.findByTitle(title));
        }
        else if(dt!=null)
        {
            return ResponseEntity.ok(partRepo.findAllPartiesByDateHeureUTC(dt));
        }
        else if(type_result!=null)
        {
            return ResponseEntity.ok(partRepo.findAllPartiesByTypeResultat(type_result));
        }
        return ResponseEntity.badRequest()
        .body("Search doit être suivit d'un paramètre : 'title' -> Récupérer une partie grâce a son titre.\n" +
          "'date' -> Récupérer les parties a une date données.\n" +
          "'result' -> Récupérer les parties par type de résultat.");

    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPartieById(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' de la partie est obligatoire");
        }
        return ResponseEntity.ok(partRepo.findById(id));
    }

    @GetMapping("/{id}/joueurs")
    public ResponseEntity<?> getJoueursPartie(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id'de la partie est obligatoire");
        }

        //on recupere la partie
        Optional<Partie> partie = partRepo.findById(id);
        if(partie.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune partie trouvée avec l'id "+ id);
        }

        //on recupere les joueurs de la parties
        Map<String, Object> result = new HashMap<>();
        result.put("joueurBlanc", partie.get().getJoueurBlanc());
        result.put("joueurNoir", partie.get().getJoueurNoir());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/ouverture")
    public ResponseEntity<?> getOuverturePartie(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id'de la partie est obligatoire");
        }

        //on recupere la partie
        Optional<Partie> partie = partRepo.findById(id);
        if(partie.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune partie trouvée avec l'id "+ id);
        }

        //on recupere l'ouverture de la partie
        return ResponseEntity.ok(partie.get().getOuverture());
    }

    @GetMapping("/{id}/suiteCoups")
    public ResponseEntity<?> getSuiteCoupsPartie(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id'de la partie est obligatoire");
        }

        //on recupere la partie
        Optional<Partie> partie = partRepo.findById(id);
        if(partie.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune partie trouvée avec l'id "+ id);
        }

        return ResponseEntity.ok(partie.get().getSuiteCoupsStats());
    }

    @GetMapping("/{id}/tournoi")
    public ResponseEntity<?> getTournoiPartie(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id'de la partie est obligatoire");
        }

        //on recupere la partie
        Optional<Partie> partie = partRepo.findById(id);
        if(partie.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune partie trouvée avec l'id "+ id);
        }

        //on recupere le tournoi de la partie
        return ResponseEntity.ok(partie.get().getTournoi());
    }
    
}
