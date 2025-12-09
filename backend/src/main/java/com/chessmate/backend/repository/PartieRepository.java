package com.chessmate.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chessmate.backend.entiter.Partie;

public interface PartieRepository extends JpaRepository<Partie, Long>{
    Optional<Partie> findByTitle(String title);   // recherche par titre 

    @Query("SELECT p FROM Partie p WHERE p.dateHeureUTC = :dt")
    List<Partie> findAllPartiesByDateHeureUTC(@Param("dt") LocalDateTime dt);  // recherche par date creation
    
    @Query("SELECT p FROM Partie p WHERE p.typeResultat = :res")
    List<Partie> findAllPartiesByTypeResultat(@Param("res") String type_r);  // recherche par type de resultat


}
