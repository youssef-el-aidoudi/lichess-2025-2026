package com.chessmate.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.chessmate.backend.entiter.Joueur;

public interface JoueurRepository extends JpaRepository<Joueur, Long>{
    Optional<Joueur> findByPseudonyme(String pseudo);
    
    @Query("SELECT j FROM joueur WHERE j.equipe = :equipe")
    List<Joueur> findByEquipe(@Param("equipe")String equipe);

    @Query("SELECT j FROM joueur WHERE j.elo = :elo")
    List<Joueur> findByElo(@Param("elo")int elo);

}
