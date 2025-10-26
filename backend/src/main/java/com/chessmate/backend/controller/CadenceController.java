package com.chessmate.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chessmate.backend.entiter.Cadence;
import com.chessmate.backend.repository.CadenceRopository;

@RestController
@RequestMapping("/cadence")
public class CadenceController {
    private final CadenceRopository repCadence;

    public CadenceController(CadenceRopository rep)
    {
        this.repCadence = rep;
    }

    @GetMapping("/all")
    public List<Cadence> all()
    {
        return repCadence.findAll();
    }
}
