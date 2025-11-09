package com.chessmate.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.Utilisateur;
import com.chessmate.backend.repository.UtilisateurRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RegistrationLoginController {
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Utilisateur utilisateur) {
        if (utilisateurRepository.findByUsername(utilisateur.getNom()) != null) {
            return ResponseEntity.badRequest().body("Ce nom existe déja !");
        }
        utilisateur.setHash(passwordEncoder.encode(utilisateur.getHash()));
        return ResponseEntity.ok(utilisateurRepository.save(utilisateur));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Utilisateur utilisateur) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(utilisateur.getNom(), utilisateur.getHash()));
            return ResponseEntity.ok("Connexion Réussie");
        } catch(Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("nom ou mot de passe invalide !");
        }
    }
}
