package com.chessmate.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.entiter.Cadence;
import com.chessmate.backend.entiter.Cadence.CategorieCadence;

public interface CadenceRopository  extends JpaRepository<Cadence, Long>{
    Optional<Cadence> findByLibelle(String lib);
    List<Cadence> findByTemps(int t);
    List<Cadence> findByIncrement(int c);
    List<Cadence> findByTypePartie(CategorieCadence typePartie);
}
