package com.chessmate.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.Entiter.Ouverture;

public interface OuvertureRepository extends JpaRepository<Ouverture, Long>{
    Optional<Ouverture> findById(long id);
    List<Ouverture> findAllCadence();
}
