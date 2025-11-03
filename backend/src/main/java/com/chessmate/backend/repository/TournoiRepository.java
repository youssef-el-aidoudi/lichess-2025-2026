package com.chessmate.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.chessmate.backend.entiter.Tournoi;

public interface TournoiRepository extends JpaRepository<Tournoi, Long>{
    /*Optional<Tournoi> findById(long id);*/
    Optional<Tournoi> findByCode(String code);
    Optional<Tournoi> findByLibelle(String libelle);
    
    @Query("SELECT t FROM tournoi WHERE t.creeA = :date")
    List<Tournoi> findAllTournoisByCreeA(@Param("date") LocalDateTime dt);
}
