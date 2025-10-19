package com.chessmate.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.entiter.Ouverture;

public interface OuvertureRepository extends JpaRepository<Ouverture, Long>{
    /*Optional<Ouverture> findById(long id);
    List<Ouverture> findAllCadence();*/
}
