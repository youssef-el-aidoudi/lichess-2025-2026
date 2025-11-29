package com.chessmate.backend.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.chessmate.backend.configuration.JwtUtils;
import com.chessmate.backend.service.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component // Déclare ce filtre comme un composant Spring, il sera géré automatiquement
public class JwtFilter extends OncePerRequestFilter {
    // Champs finaux pour l'injection de dépendances
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtils jwtUtils;

    // Constructeur explicit pour l'injection
    public JwtFilter(CustomUserDetailsService c, JwtUtils u)
    {
        super();
        this.customUserDetailsService = c;
        this.jwtUtils = u;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Récupère le header Authorization de la requête HTTP
        final String authHeader = request.getHeader("Authorization");

        String username = null; // Nom d'utilisateur extrait du JWT
        String jwt = null;      // Token JWT

        // Vérifie que le header existe et commence par "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Supprime "Bearer " pour récupérer uniquement le token
            jwt = authHeader.substring(7);
            // Extrait le nom d'utilisateur du token
            username = jwtUtils.extractUsername(jwt);
        }

        // Vérifie qu'aucune authentification n'est déjà définie dans le contexte
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Charge les détails de l'utilisateur depuis la base
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

            // Valide le token contre les informations de l'utilisateur
            if (jwtUtils.validateToken(jwt, userDetails)) {
                // Crée un objet d'authentification Spring Security
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // Associe les détails de la requête à l'objet d'authentification
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Place l'authentification dans le contexte de sécurité pour cette requête
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        // Passe la requête au filtre suivant dans la chaîne
        filterChain.doFilter(request, response);

    }

}
