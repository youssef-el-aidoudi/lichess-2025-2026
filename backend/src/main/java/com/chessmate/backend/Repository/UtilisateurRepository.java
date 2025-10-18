package com.chessmate.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.Entiter.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long>{
    Optional<Utilisateur> findById(long id);
    Optional<Utilisateur> findByMail(String email);
    List<Utilisateur> findAllUtilisateurs();
}
