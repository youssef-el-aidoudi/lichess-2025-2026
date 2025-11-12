package com.chessmate.backend.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.Cadence;
import com.chessmate.backend.repository.CadenceRopository;

@RestController
@RequestMapping("/cadences")
public class CadenceController {
    private final CadenceRopository repCadence;

    public CadenceController(CadenceRopository rep)
    {
        this.repCadence = rep;
    }

    @GetMapping
    public ResponseEntity<?> getAllCadence()
    {
        return ResponseEntity.ok(repCadence.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<?> getCadence(@RequestParam(value="temps", required=false) Integer temps,
                            @RequestParam(value="libelle", required=false) String libelle,
                            @RequestParam(value="increment", required=false) Integer increment,
                            @RequestParam(value="typePartie", required=false) String typePartie)
    {
        if(temps != null)
        {
            return ResponseEntity.ok(repCadence.findByTemps(temps));
        }
        else if(libelle != null)
        {
            return ResponseEntity.ok(repCadence.findByLibelle(libelle));
        }
        else if(increment != null)
        {
            return ResponseEntity.ok(repCadence.findByIncrement(increment));
        }
        else if(typePartie != null)
        {
            Cadence.CategorieCadence cat = Cadence.CategorieCadence.valueOf(typePartie);
            return ResponseEntity.ok(repCadence.findByTypePartie(cat));
        }
        
        return ResponseEntity.badRequest()
        .body("Search doit être suivit d'un paramètre : 'temps' -> Récupérer les cadences ayant le même temps.\n" +
          "'increment' -> Récupérer les cadences ayant le même increment.\n" +
          "'typePartie' -> Récupérer les cadences par type de partie.\n" +
          "'libelle' -> Récupérer une cadence par son libelle.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCadenceById(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' de la cadence est obligatoire");
        }
        return ResponseEntity.ok(repCadence.findById(id));
    }

    //parties associés a une cadence
    @GetMapping("/{id}/parties")
    public ResponseEntity<?> getPartiesByCadence(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' de la cadence est obligatoire");
        }
        
        Optional<Cadence> cadence = repCadence.findById(id);

        if(cadence.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune cadence trouvée avec l'id "+ id);
        }

        return ResponseEntity.ok(cadence.get().getParties());
    }

    //tounois associés a une cadence
    @GetMapping("/{id}/tournois")
    public ResponseEntity<?> getTournoiByCadence(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' de la cadence est obligatoire");
        }
        
        Optional<Cadence> cadence = repCadence.findById(id);

        if(cadence.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune cadence trouvée avec l'id "+ id);
        }

        return ResponseEntity.ok(cadence.get().getTournois());
    }

}
