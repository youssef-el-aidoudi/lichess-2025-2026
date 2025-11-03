package com.chessmate.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.Ouverture;
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
    
    @GetMapping
    public Object getUtilisateur(@RequestParam(value="id", required=false) Long id)
    {
        if(id!=null)
        {
            return repUser.findById(id);
        }
        return repUser.findAll();
    }
}
