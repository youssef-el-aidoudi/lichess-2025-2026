package com.chessmate.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.entiter.Joueur;

public interface JoueurRepository extends JpaRepository<Joueur, Long>{
    /*Joueur findById(long id);
    Optional<Joueur> findByPseudo(String pseudo);
    List<Joueur> findAllPlayer();*/
}
