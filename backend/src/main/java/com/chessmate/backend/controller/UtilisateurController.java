package com.chessmate.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    
    @GetMapping("/all")
    public List<Utilisateur> all()
    {
        return repUser.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Utilisateur> getUtilisateur(@PathVariable long id)
    {
        return repUser.findById(id);
    }

}
