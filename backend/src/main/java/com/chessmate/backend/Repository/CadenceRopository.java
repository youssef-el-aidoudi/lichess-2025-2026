package com.chessmate.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.Entiter.Cadence;

public interface CadenceRopository  extends JpaRepository<Cadence, Long>{
    Optional<Cadence> findById(long ig);
    List<Cadence> findAllCadence();
    
}
