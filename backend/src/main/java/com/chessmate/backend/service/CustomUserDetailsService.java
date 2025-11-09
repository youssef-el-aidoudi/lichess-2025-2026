package com.chessmate.backend.service;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.chessmate.backend.entiter.Utilisateur;
import com.chessmate.backend.repository.UtilisateurRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService {
    private final UtilisateurRepository utilisateurRepository;

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur utilisateur = (Utilisateur) utilisateurRepository.findByUsername(username);

        if (utilisateur == null) {
            throw new UsernameNotFoundException("cet utilisateur n'a pas été trouvé: " + username);
        }
        return null;
    }
    
}
