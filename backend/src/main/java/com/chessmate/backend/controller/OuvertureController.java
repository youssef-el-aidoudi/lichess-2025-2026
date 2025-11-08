package com.chessmate.backend.controller;

import java.util.List;
import java.util.Optional;

import org.apache.catalina.connector.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.Ouverture;
import com.chessmate.backend.repository.OuvertureRepository;

@RestController
@RequestMapping("/ouvertures")
public class OuvertureController {
    private final OuvertureRepository ouverRep;

    public OuvertureController(OuvertureRepository rp)
    {
        this.ouverRep = rp;
    }


    @GetMapping
    public ResponseEntity<?> getAllOuverture()
    {
        return ResponseEntity.ok(ouverRep.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<?> getOuverture(@RequestParam(value="code", required=false) String code)
    {
        if(code!=null)
        {
            return ResponseEntity.ok(ouverRep.findByCode(code));
        }
        return ResponseEntity.badRequest()
        .body("Search doit être suivit d'un paramètre : 'code' -> Récupérer une ouverture grâce a son code.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCadenceById(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' de l'ouverture est obligatoire");
        }
        return ResponseEntity.ok(ouverRep.findById(id));
    }

    
    @GetMapping("/{id}/suiteCoups")
    public ResponseEntity<?> getSuiteCoupByOuverture(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' de l'ouverture est obligatoire");
        }
        Optional<Ouverture> ouverture = ouverRep.findById(id);
        if(ouverture.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune ouverture trouvée avec l'id "+ id);
        }

        return ResponseEntity.ok(ouverture.get().getSuiteCoupsStats());
    }
}
