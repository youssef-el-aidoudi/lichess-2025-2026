package com.chessmate.backend.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.chessmate.backend.dto.MlGameDto;
import com.chessmate.backend.entiter.Partie;
import com.chessmate.backend.entiter.PartieJouee;
import com.chessmate.backend.repository.PartieRepository;
import com.chessmate.backend.repository.PartieJoueeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MlProfileService {

    private final PartieRepository partieRepository;
    private final PartieJoueeRepository partieJoueeRepository;
    private final ObjectMapper objectMapper;

    // Paths for Docker container
    private final String PYTHON_SCRIPT_PATH = "/app/ml_chess_profile.py";
    private final String PYTHON_EXEC = "python3";
    private final String BASELINE_CSV_PATH = "/app/games_final_clean.csv";

    public MlProfileService(PartieRepository partieRepository, PartieJoueeRepository partieJoueeRepository, ObjectMapper objectMapper) {
        this.partieRepository = partieRepository;
        this.partieJoueeRepository = partieJoueeRepository;
        this.objectMapper = objectMapper;
    }

    public JsonNode getPersonalizedProfile(String username) throws Exception {
        // 1. Fetch user games using both Partie (Lichess dataset) and PartieJouee (local app games)
        List<Partie> userParties = partieRepository.findByJoueurBlancPseudonymeIgnoreCaseOrJoueurNoirPseudonymeIgnoreCase(username, username);
        List<PartieJouee> userPartiesJouees = partieJoueeRepository.findByJoueurBlancIgnoreCaseOrJoueurNoirIgnoreCase(username, username);
        
        if ((userParties == null || userParties.isEmpty()) && (userPartiesJouees == null || userPartiesJouees.isEmpty())) {
            throw new RuntimeException("Aucune partie trouvée pour l'utilisateur: " + username);
        }

        // 2. Map entities to ML expectations
        List<MlGameDto> mappedGames = new ArrayList<>();
        if (userParties != null) {
            mappedGames.addAll(userParties.stream().map(this::mapToMlGameDto).collect(Collectors.toList()));
        }
        if (userPartiesJouees != null) {
            mappedGames.addAll(userPartiesJouees.stream().map(this::mapPartieJoueeToMlGameDto).collect(Collectors.toList()));
        }

        // 3. Serialize to temporary JSON file to avoid ProcessBuilder buffer limits
        File tempInputFile = File.createTempFile("ml_input_" + username, ".json");
        objectMapper.writeValue(tempInputFile, mappedGames);

        // 4. Call Python Process
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    PYTHON_EXEC,
                    PYTHON_SCRIPT_PATH,
                    "--target-user", username,
                    "--user-games-file", tempInputFile.getAbsolutePath(),
                    "--baseline-default", BASELINE_CSV_PATH
            );
            
            // INDISPENSABLE sur Windows : forcer Python à écrire en UTF-8 sur la sortie standard
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(false);
            
            Process process = pb.start();
            
            // Read stdout in UTF-8 to prevent encoding corruption (Intermdiaire)
            String outputJson = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String errorOutput = new String(process.getErrorStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Erreur du processus ML (code " + exitCode + "): " + errorOutput);
            }
            
            // 5. Return JSON Node to the controller
            return objectMapper.readTree(outputJson);

        } finally {
            try {
                Files.deleteIfExists(tempInputFile.toPath());
            } catch (IOException e) {
                // Ignore cleanup error
            }
        }
    }

    private MlGameDto mapToMlGameDto(Partie p) {
        MlGameDto dto = new MlGameDto();
        dto.setWhiteId(p.getJoueurBlanc() != null ? p.getJoueurBlanc().getPseudo() : "unknown");
        dto.setBlackId(p.getJoueurNoir() != null ? p.getJoueurNoir().getPseudo() : "unknown");
        
        String winner = "draw";
        if (p.getResultat() != null) {
            if (p.getResultat() == 1) {
                winner = "white";
            } else if (p.getResultat() == -1) {
                winner = "black";
            }
        }
        dto.setWinner(winner);

        String typeResult = p.getType_Resultat();
        if ("abandon".equalsIgnoreCase(typeResult)) {
           dto.setVictoryStatus("resign");
        } else if ("timeout".equalsIgnoreCase(typeResult) || "outoftime".equalsIgnoreCase(typeResult)) {
           dto.setVictoryStatus("outoftime");
        } else {
           dto.setVictoryStatus("mate"); 
        }

        dto.setTurns(40);
        dto.setRated(true); 
        dto.setWhiteRating(p.getElo_Blanc() != null ? p.getElo_Blanc().intValue() : 1500);
        dto.setBlackRating(p.getElo_Noir() != null ? p.getElo_Noir().intValue() : 1500);
        
        String ov = p.getOuverture() != null ? p.getOuverture().getLibelleOuverture() : "Unknown";
        dto.setOpeningName(ov.equalsIgnoreCase("Partie locale") ? "Unknown" : ov);
        
        return dto;
    }

    private MlGameDto mapPartieJoueeToMlGameDto(PartieJouee p) {
        MlGameDto dto = new MlGameDto();
        dto.setWhiteId(p.getJoueurBlanc());
        dto.setBlackId(p.getJoueurNoir());
        
        String winner = "draw";
        String vainqueur = p.getVainqueur();
        String result = p.getResultat();
        
        boolean isWhiteWinner = "1-0".equals(result) || "blanc".equalsIgnoreCase(vainqueur) || "white".equalsIgnoreCase(vainqueur)
            || (vainqueur != null && vainqueur.equalsIgnoreCase(p.getJoueurBlanc()));
            
        boolean isBlackWinner = "0-1".equals(result) || "noir".equalsIgnoreCase(vainqueur) || "black".equalsIgnoreCase(vainqueur)
            || (vainqueur != null && vainqueur.equalsIgnoreCase(p.getJoueurNoir()));
            
        if (isWhiteWinner) {
            winner = "white";
        } else if (isBlackWinner) {
            winner = "black";
        }
        dto.setWinner(winner);

        dto.setVictoryStatus("mate"); 
        dto.setTurns(p.getNombreCoups() != null ? p.getNombreCoups() : 40);
        dto.setRated(true); 
        dto.setWhiteRating(1500); 
        dto.setBlackRating(1500); 
        
        String ov = p.getOuverture();
        dto.setOpeningName(ov != null && !ov.equalsIgnoreCase("Partie locale") ? ov : "Unknown");
        
        return dto;
    }
}
