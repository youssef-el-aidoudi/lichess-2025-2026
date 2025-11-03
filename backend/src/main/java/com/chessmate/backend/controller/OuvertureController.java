package com.chessmate.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
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
    public Object getOuverture(@RequestParam(value="id", required=false) Long id,
                               @RequestParam(value="code", required=false) String code)
    {
        if(id!=null)
        {
            return ouverRep.findById(id);
        }
        else if(code!=null)
        {
            return ouverRep.findByCode(code);
        }
        return ouverRep.findAll();
    }
}
