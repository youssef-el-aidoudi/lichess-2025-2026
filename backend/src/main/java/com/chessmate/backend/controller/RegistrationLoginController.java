package com.chessmate.backend.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import com.chessmate.backend.entiter.Joueur;
import com.chessmate.backend.repository.JoueurRepository;
import com.chessmate.backend.repository.UtilisateurRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
//@RequiredArgsConstructor
@Slf4j
public class RegistrationLoginController {
    
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final JoueurRepository joueurRepository;

    public RegistrationLoginController(UtilisateurRepository r, PasswordEncoder p, JwtUtils j, AuthenticationManager a, JoueurRepository jt)
    {
        this.utilisateurRepository = r;
        this.jwtUtils = j;
        this.passwordEncoder = p;
        this.authenticationManager = a;
        this.joueurRepository = jt;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Utilisateur utilisateur) {
        if (!utilisateurRepository.findByEmail(utilisateur.getEmail()).isEmpty()) {
            return ResponseEntity.badRequest().body("Ce email existe déja !");
        }
        
        utilisateur.setHash(passwordEncoder.encode(utilisateur.getHash()));
        utilisateur.setInscrit_A(LocalDateTime.now());

        if(utilisateur.getJoueur()!=null && utilisateur.getJoueur().getPseudo()!=null)
        {
            Optional<Joueur> monjoueur = joueurRepository.findByPseudonyme(utilisateur.getJoueur().getPseudo());
            if(monjoueur.isPresent())
            {
                utilisateur.setJoueur(monjoueur.get());
            }
            else
            {
                return ResponseEntity.badRequest().body("Joueur avec ce pseudo -> "+ utilisateur.getJoueur().getPseudo() +" introuvable");
            }
        }

        return ResponseEntity.ok(utilisateurRepository.save(utilisateur));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Utilisateur utilisateur) {
        try {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(utilisateur.getEmail(), utilisateur.getHash())    
        );
        if (authentication.isAuthenticated()) {
            //maj connexion du joueur
            Optional<Utilisateur> userFromOpt = utilisateurRepository.findByEmail(utilisateur.getEmail());
            if(userFromOpt.isPresent())
            {
                Utilisateur userFromDb = userFromOpt.get();
                // Mettre à jour la dernière connexion
                userFromDb.setConnexion_A(LocalDateTime.now());
                utilisateurRepository.save(userFromDb);
            }

            //generer le token et le retourner
            Map<String, Object> authData = new HashMap<>();
            authData.put("token", jwtUtils.generateToken(utilisateur.getEmail()));
            authData.put("type", "Bearer");
            return ResponseEntity.ok(authData);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
    } catch (AuthenticationException e) {
        //log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
    }

    }
}
