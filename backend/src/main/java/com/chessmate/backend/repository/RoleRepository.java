package com.chessmate.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessmate.backend.entiter.Role;

public interface RoleRepository extends JpaRepository<Role, Long>{

}
