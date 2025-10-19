package com.chessmate.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.entiter.Cadence;

public interface CadenceRopository  extends JpaRepository<Cadence, Long>{
    /*Optional<Cadence> findById(long ig);
    List<Cadence> findAllCadence();*/
    
}
