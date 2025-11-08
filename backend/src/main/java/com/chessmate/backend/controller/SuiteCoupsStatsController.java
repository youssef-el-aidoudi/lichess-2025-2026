package com.chessmate.backend.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.SuiteCoupsStats;
import com.chessmate.backend.repository.SuiteCoupsStatsRepository;

@RestController
@RequestMapping("/coups_stats")
public class SuiteCoupsStatsController {

    private final SuiteCoupsStatsRepository coupRepo;

    public SuiteCoupsStatsController(SuiteCoupsStatsRepository rep)
    {
        this.coupRepo = rep;
    }

    @GetMapping
    public ResponseEntity<?> getAllCoupStats()
    {
        return ResponseEntity.ok(coupRepo.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCoupsStatsById(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du coup est obligatoire");
        }
        return ResponseEntity.ok(coupRepo.findById(id));
    }

    @GetMapping("/{id}/coup_precedent")
    public ResponseEntity<?> getSuiteCoupsStatsPrecedente(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du coup est obligatoire");
        }
        
        Optional<SuiteCoupsStats> coups_stats = coupRepo.findById(id);
        if(coups_stats.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune coups trouvée avec l'id "+ id);
        }

        return ResponseEntity.ok(coups_stats.get().getSuiteCoupsStatsPrecedente());

    }

    @GetMapping("/{id}/coups_suivants")
    public ResponseEntity<?> getSuiteCoupsStatsSuivantes(@PathVariable Long id)
    {
        //aucun id renseigner
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du coup est obligatoire");
        }
        
        Optional<SuiteCoupsStats> coups_stats = coupRepo.findById(id);
        if(coups_stats.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune coups trouvée avec l'id "+ id);
        }

        return ResponseEntity.ok(coups_stats.get().getSuitesSuivantes());

    }

}