package com.chessmate.backend.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.Role;
import com.chessmate.backend.repository.RoleRepository;

@RestController
@RequestMapping("/roles")
public class RoleController {
    private final RoleRepository roleRep;

    public RoleController(RoleRepository rep)
    {
        this.roleRep = rep;
    }

    @GetMapping
    public ResponseEntity<?> getAllRoles()
    {
        return ResponseEntity.ok(roleRep.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<?> getRole(@RequestParam(value="libelle", required=false) String libelle)
    {
        if(libelle!=null)
        {
            return ResponseEntity.ok(roleRep.findByLibelle(libelle));
        }
        return ResponseEntity.badRequest()
        .body("Search doit être suivit d'un paramètre : 'libelle' -> Récupérer un role grâce a son libelle.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoleById(@PathVariable Long id)
    {
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du role est obligatoire");
        }

        return ResponseEntity.ok(roleRep.findById(id));
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<?> getUtilisateursByRole(@PathVariable Long id)
    {
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du role est obligatoire");
        }

        Optional<Role> role = roleRep.findById(id);

        if(role.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun rôle trouvé avec l'id "+ id);
        }

        return ResponseEntity.ok(role.get().getUtilisateurs());
    }

    @GetMapping("/{id}/profils")
    public ResponseEntity<?> getProfilsByRole(@PathVariable Long id)
    {
        if(id == null)
        {
            return ResponseEntity.badRequest().body("Le paramètre 'id' du role est obligatoire");
        }

        Optional<Role> role = roleRep.findById(id);

        if(role.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun rôle trouvé avec l'id "+ id);
        }

        return ResponseEntity.ok(role.get().getProfils());
    }
}
