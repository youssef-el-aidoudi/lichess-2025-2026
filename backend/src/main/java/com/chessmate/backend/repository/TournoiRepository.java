package com.chessmate.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.entiter.Tournoi;

public interface TournoiRepository extends JpaRepository<Tournoi, Long>{
    /*Optional<Tournoi> findById(long id);
    Optional<Tournoi> findByCode(String code);
    List<Tournoi> findAllTournois();
    List<Tournoi> findAllTournoisByDate(LocalDateTime dt);*/
}
