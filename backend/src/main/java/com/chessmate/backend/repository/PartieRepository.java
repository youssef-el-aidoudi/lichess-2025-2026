package com.chessmate.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.chessmate.backend.entiter.Partie;

public interface PartieRepository extends JpaRepository<Partie, Long>{
    /*Optional<Partie> findById(long id);*/
    Optional<Partie> findByCode(String code);

    @Query("SELECT p FROM partie p WHERE p.dateHeure = :dt")
    List<Partie> findAllPartiesByDateHeure(@Param("dt") LocalDateTime dt);
    
    @Query("SELECT p FROM partie p WHERE p.typeResultat = :res")
    List<Partie> findAllPartiesByTypeResultat(@Param("res") String type_r);
}
