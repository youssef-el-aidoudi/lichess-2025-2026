package com.chessmate.backend.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.Profil;
import com.chessmate.backend.repository.ProfilRepository;

@RestController
@RequestMapping("/profils")
public class ProfilController {
    private final ProfilRepository profilRep;

    public ProfilController(ProfilRepository rep)
    {
        this.profilRep = rep;
    }

    @GetMapping
    public ResponseEntity<?> getAllProfils()
    {
        return ResponseEntity.ok(profilRep.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<?> getProfil(@RequestParam(value="libelle", required=false) String libelle)
    {
        if(libelle!=null)
        {
            return ResponseEntity.ok(profilRep.findByLibelle(libelle));
        }
        return ResponseEntity.badRequest()
        .body("Search doit être suivit d'un paramètre : 'libelle' -> Récupérer un profil grâce a son libelle.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProfilById(@PathVariable Long id)
    {
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du profil est obligatoire");
        }

        return ResponseEntity.ok(profilRep.findById(id));
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<?> getUtilisateursByProfil(@PathVariable Long id)
    {
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du profil est obligatoire");
        }

        Optional<Profil> profil = profilRep.findById(id);

        if(profil.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun profil trouvé avec l'id "+ id);
        }

        return ResponseEntity.ok(profil.get().getUtilisateurs());
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<?> getRolesByProfil(@PathVariable Long id)
    {
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du profil est obligatoire");
        }

        Optional<Profil> profil = profilRep.findById(id);

        if(profil.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun profil trouvé avec l'id "+ id);
        }

        return ResponseEntity.ok(profil.get().getRoles());
    }
}
