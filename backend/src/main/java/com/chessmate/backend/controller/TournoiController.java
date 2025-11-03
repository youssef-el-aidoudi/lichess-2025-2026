package com.chessmate.backend.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public Object getTournoi(@RequestParam(value="id", required=false) Long id,
                            @RequestParam(value="code", required=false) String code,
                            @RequestParam(value="libelle", required=false) String libelle,
                            @RequestParam(value="date", required=false) LocalDateTime dt)
    {
        if(id!=null)
        {
            return tourRep.findById(id);
        }
        else if(code!=null)
        {
            return tourRep.findByCode(code);
        }
        else if(libelle!=null)
        {
            return tourRep.findByLibelle(libelle);
        }
        else if(dt!=null)
        {
            return tourRep.findAllTournoisByCreeA(dt);
        }

        return tourRep.findAll();
    }

   

}
