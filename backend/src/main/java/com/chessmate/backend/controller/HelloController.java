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
@RequestMapping("/test")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Salut, le backend fonctionne !";
    }

    private final UtilisateurRepository userrep;

    public HelloController(UtilisateurRepository user)
    {
        this.userrep = user;
    }

   /*@GetMapping("/insert")
    public Utilisateur insert()
    {
        Utilisateur u = new Utilisateur("sory", "Dela", "sorydela@gmail.com");
        return userrep.save(u);
    }*/

    @GetMapping("/all")
    public List<Utilisateur> all()
    {
        return userrep.findAll();
    }
    
    @GetMapping("/{id}")
    public Optional<Utilisateur> getUtilisateur(@PathVariable long id)
    {
        return userrep.findById(id);
    }
}