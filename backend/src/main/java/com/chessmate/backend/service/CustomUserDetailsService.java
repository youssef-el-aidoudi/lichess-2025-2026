package com.chessmate.backend.service;

import java.util.Collections;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.chessmate.backend.entiter.Utilisateur;
import com.chessmate.backend.repository.UtilisateurRepository;

import lombok.RequiredArgsConstructor;

@Service
//@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UtilisateurRepository utilisateurRepository;

    public CustomUserDetailsService(UtilisateurRepository rep)
    {
        this.utilisateurRepository = rep;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<Utilisateur> user = utilisateurRepository.findByEmail(email);

        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + email);
        }
        
        /*return new org.springframework.security.core.userdetails.User(
        user.get().getEmail(), 
        user.get().getHash(), 
        Collections.singletonList(new SimpleGrantedAuthority(user.get().getRole()))); 
        */   
        
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.get().getEmail())
            .password(user.get().getHash()) 
            .authorities(Collections.singletonList(new SimpleGrantedAuthority(user.get().getRole())))
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();
        } 
    
}
