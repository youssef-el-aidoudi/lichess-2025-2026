package com.chessmate.backend.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.chessmate.backend.entiter.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long>{
    Optional<Utilisateur> findByEmail(String email);

    @Query("SELECT u FROM utilisateur u WHERE u.inscription_a = :date")
    List<Utilisateur> findByInscriptionA(@Param("date") LocalDateTime dt);

    @Query("SELECT u FROM utilisateur u WHERE u.connexion_a = :date")
    List<Utilisateur> findByConnexionA(@Param("date") LocalDateTime dt);

    @Query("SELECT u FROM utilisateur u WHERE u.banni_a = :date")
    List<Utilisateur> findByBanniA(@Param("date") LocalDateTime dt);
}
