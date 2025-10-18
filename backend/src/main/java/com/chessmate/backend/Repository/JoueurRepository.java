package com.chessmate.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.Entiter.Joueur;

public interface JoueurRepository extends JpaRepository<Joueur, Long>{
    Joueur findById(long id);
    Optional<Joueur> findByPseudo(String pseudo);
    List<Joueur> findAllPlayer();
}
