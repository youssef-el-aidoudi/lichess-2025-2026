package com.chessmate.backend.configuration;

import org.springframework.context.annotation.Bean;

import com.chessmate.backend.filter.JwtFilter;
import com.chessmate.backend.service.CustomUserDetailsService;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration // Déclare cette classe comme configuration Spring
@EnableWebSecurity // Active la sécurité Spring Security
public class SecurityConfig {

    // Service personnalisé pour charger les utilisateurs depuis la base
    private final CustomUserDetailsService customUserDetailsService;
    
    // Classe utilitaire pour gérer les JWT
    private final JwtUtils jwtUtils;

    // Constructeur manuel pour injecter les dépendances
    public SecurityConfig(CustomUserDetailsService s, JwtUtils u) {
        this.customUserDetailsService = s;
        this.jwtUtils = u;
    }

    // Bean pour encoder les mots de passe avec BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Bean pour configurer le AuthenticationManager de Spring Security
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception {
        // Récupère le builder de l'AuthenticationManager
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        // Configure le UserDetailsService et le PasswordEncoder
        authenticationManagerBuilder.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder);
        return authenticationManagerBuilder.build();
    }

    // Bean pour configurer la chaîne de filtres de sécurité HTTP
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Désactive CSRF (protection contre les attaques cross-site, ici inutile pour une API REST)
                .csrf(AbstractHttpConfigurer::disable)
                // Configure les autorisations pour les requêtes HTTP
                .authorizeHttpRequests(auth -> 
                        auth.requestMatchers(
                            "/api/auth/**", // Autorise toutes les routes d'authentification
                            
                            // Autorise les routes Swagger pour la documentation
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui/index.html")
                            .permitAll() // Ces routes sont accessibles sans authentification
                            .anyRequest().authenticated() // Toutes les autres routes nécessitent une authentification
                )
                // Ajoute le filtre JWT avant le filtre standard de Spring Security
                .addFilterBefore(new JwtFilter(customUserDetailsService, jwtUtils), UsernamePasswordAuthenticationFilter.class)
                .build(); // Construit et retourne la chaîne de filtres
    }

}
