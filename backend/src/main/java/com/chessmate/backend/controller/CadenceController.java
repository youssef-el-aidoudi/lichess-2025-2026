package com.chessmate.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.Cadence;
import com.chessmate.backend.repository.CadenceRopository;

@RestController
@RequestMapping("/cadences")
public class CadenceController {
    private final CadenceRopository repCadence;

    public CadenceController(CadenceRopository rep)
    {
        this.repCadence = rep;
    }

    @GetMapping
    public Object getCadence()
    {
        return repCadence.findAll();
    }
}
