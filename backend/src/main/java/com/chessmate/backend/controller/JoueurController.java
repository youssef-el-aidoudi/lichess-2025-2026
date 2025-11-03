package com.chessmate.backend.controller;

import java.util.List;
import java.util.Optional;

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
    public Object getJoueur(@RequestParam(value="id", required = false) Long id,
                            @RequestParam(value="pseudo", required = false) String pseudo) {
        if (id != null) {
            return joueurRep.findById(id);
        } else if (pseudo != null) {
            return joueurRep.findByPseudo(pseudo);
        }
        return joueurRep.findAll();
    }

    
}
