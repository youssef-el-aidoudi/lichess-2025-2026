package com.chessmate.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
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
    public Object getPartie(@RequestParam(value ="id", required = false) Long id,
                            @RequestParam(value = "code", required = false) String code,
                            @RequestParam(value="date", required=false) LocalDateTime dt,
                            @RequestParam(value="result", required=false) String type_result)
    {
        if(id!=null)
        {
            return partRepo.findById(id);
        }
        else if(code!=null)
        {
            return partRepo.findByCode(code);
        }
        else if(dt!=null)
        {
            return partRepo.findAllPartiesByDateHeure(dt);
        }
        else if(type_result!=null)
        {
            return partRepo.findAllPartiesByTypeResultat(type_result);
        }
        return partRepo.findAll();
    }

    
}
