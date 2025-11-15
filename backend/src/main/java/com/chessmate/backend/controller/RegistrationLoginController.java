package com.chessmate.backend.controller;

import java.util.HashMap;
import java.util.Map;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.configuration.JwtUtils;
import com.chessmate.backend.entiter.Utilisateur;
import com.chessmate.backend.repository.UtilisateurRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class RegistrationLoginController {
    
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Utilisateur utilisateur) {
        if (utilisateurRepository.findByEmail(utilisateur.getEmail()) != null) {
            return ResponseEntity.badRequest().body("Cette adresse email existe déja !");
        }
        utilisateur.setHash(passwordEncoder.encode(utilisateur.getHash()));
        return ResponseEntity.ok(utilisateurRepository.save(utilisateur));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Utilisateur utilisateur) {
        try {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(utilisateur.getEmail(), utilisateur.getHash())    
        );
        if (authentication.isAuthenticated()) {
            Map<String, Object> authData = new HashMap<>();
            authData.put("token", jwtUtils.generateToken(utilisateur.getEmail()));
            authData.put("type", "Bearer");
            return ResponseEntity.ok(authData);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("email ou mot de passe invalide !");
    } catch (AuthenticationException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("email ou mot de passe invalide !");
    }

    }
}
