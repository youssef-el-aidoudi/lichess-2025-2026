package com.chessmate.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.entiter.Partie;

public interface PartieRepository extends JpaRepository<Partie, Long>{
    /*Optional<Partie> findById(long id);
    Optional<Partie> findByCode(String code);
    List<Partie> findAllParties();
    List<Partie> findAllPartiesByDates(LocalDateTime dt);
    List<Partie> findAllPartiesByResultType(String type_r);*/
}
