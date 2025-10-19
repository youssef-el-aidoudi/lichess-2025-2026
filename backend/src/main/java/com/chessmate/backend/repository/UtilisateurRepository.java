package com.chessmate.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.entiter.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long>{
    /*Optional<Utilisateur> findById(long id);
    Optional<Utilisateur> findByEmail(String email);
    List<Utilisateur> findAllUtilisateurs();*/
}
