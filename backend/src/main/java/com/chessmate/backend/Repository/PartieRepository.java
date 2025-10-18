package com.chessmate.backend.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.Entiter.Partie;

public interface PartieRepository extends JpaRepository<Partie, Long>{
    Optional<Partie> findById(long id);
    Optional<Partie> findByCode(String code);
    List<Partie> findAllParties();
    List<Partie> findAllPartiesByDates(LocalDateTime dt);
    List<Partie> findAllPartiesByResultType(String type_r);
}
