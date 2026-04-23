package com.chessmate.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.entiter.PartieJouee;

public interface PartieJoueeRepository extends JpaRepository<PartieJouee, Long> {
}