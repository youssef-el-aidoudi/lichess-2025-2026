package com.chessmate.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.entiter.Ouverture;

public interface OuvertureRepository extends JpaRepository<Ouverture, Long>{
    Optional<Ouverture> findByCode(String code);
    
}
