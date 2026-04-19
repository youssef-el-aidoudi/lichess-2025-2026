package com.chessmate.backend.service;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Component
public class StockfishWrapper {

    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private static final long COMMAND_TIMEOUT_MS = 5000; // 5 second timeout

    // Get Stockfish path - tries multiple locations
    private String getStockfishPath() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isLinux = os.contains("linux");

        // Try 1: Check if stockfish is in system PATH (works on both Linux and Windows)
        try {
            String[] cmd = isLinux ? new String[] { "sh", "-c", "which stockfish" }
                    : new String[] { "cmd", "/c", "where stockfish" };
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            if (line != null && !line.isEmpty()) {
                System.out.println(">>> Found Stockfish in PATH: " + line);
                return line.trim();
            }
        } catch (Exception e) {
            System.err.println("Could not find stockfish in PATH: " + e.getMessage());
        }

        // Try 2: /app directory (Docker container Linux)
        if (isLinux) {
            String dockerPath = "/usr/games/stockfish";
            if (Files.exists(Paths.get(dockerPath))) {
                System.out.println(">>> Found Stockfish at: " + dockerPath);
                return dockerPath;
            }

            String dockerPath2 = "/usr/bin/stockfish";
            if (Files.exists(Paths.get(dockerPath2))) {
                System.out.println(">>> Found Stockfish at: " + dockerPath2);
                return dockerPath2;
            }
        }

        // Try 3: Windows local exe
        String currentDir = System.getProperty("user.dir");
        String devPath = currentDir + File.separator + "src" + File.separator + "main" + File.separator +
                "java" + File.separator + "com" + File.separator + "chessmate" + File.separator +
                "backend" + File.separator + "service" + File.separator + "stockfish-windows-x86-64-avx2.exe";

        if (Files.exists(Paths.get(devPath)) && !isLinux) {
            System.out.println(">>> Found Stockfish at: " + devPath);
            return devPath;
        }

        throw new FileNotFoundException("Stockfish executable not found. OS: " + os
                + ". Tried PATH, /usr/games/stockfish, /usr/bin/stockfish, and dev paths");
    }

    private int depth = 15;
    private int movetime = 1000;
    private boolean isAvailable = false;

    @PostConstruct
    public void start() {
        try {
            String stockfishPath = getStockfishPath();
            System.out.println(">>> Tentative de démarrage de Stockfish depuis: " + stockfishPath);

            ProcessBuilder builder = new ProcessBuilder(stockfishPath);
            builder.redirectErrorStream(true);

            process = builder.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Initialize UCI
            sendCommand("uci");
            waitFor("uciok", 3000);

            // Verify engine is ready
            sendCommand("isready");
            waitFor("readyok", 3000);

            isAvailable = true;
            System.out.println(">>> Stockfish est prêt et en cours d'exécution!");
        } catch (Exception e) {
            System.err.println("ERREUR: Impossible de démarrer Stockfish. " + e.getMessage());
            e.printStackTrace();
            isAvailable = false;
            // Don't throw - let the application continue without Stockfish
        }
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setMovetime(int movetime) {
        this.movetime = movetime;
    }

    private void clearBufferAndStop() throws IOException {
        sendCommand("stop");
        sendCommand("isready");
        
        long startTime = System.currentTimeMillis();
        String line;
        // Check ready() to avoid blocking indefinitely if process died
        while (System.currentTimeMillis() - startTime < 1000) {
            if (reader.ready()) {
                line = reader.readLine();
                if (line == null || line.equals("readyok")) {
                    break;
                }
            } else {
                try { Thread.sleep(10); } catch (Exception e) {}
            }
        }
    }

    public synchronized String getBestMove(String moves, String mode) {
        if (!isAvailable || process == null || !process.isAlive()) {
            System.err.println("Stockfish is not available. Returning default move.");
            return "e2e4";
        }

        try {
            clearBufferAndStop();

            // Handle empty moves - starting position
            String positionCmd = moves == null || moves.isEmpty()
                    ? "position startpos"
                    : "position startpos moves " + moves;

            sendCommand(positionCmd);

            String goCommand;
            if ("depth".equalsIgnoreCase(mode)) {
                goCommand = "go depth " + this.depth;
            } else if ("movetime".equalsIgnoreCase(mode)) {
                goCommand = "go movetime " + this.movetime;
            } else {
                goCommand = "go movetime 1000";
            }

            sendCommand(goCommand);

            // Wait for bestmove with timeout
            long startTime = System.currentTimeMillis();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    String[] parts = line.split(" ");
                    if (parts.length > 1) {
                        return parts[1];
                    }
                    break;
                }

                if (System.currentTimeMillis() - startTime > COMMAND_TIMEOUT_MS) {
                    System.err.println("Timeout waiting for Stockfish bestmove");
                    return "e2e4";
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur Stockfish (getBestMove): " + e.getMessage());
            e.printStackTrace();
        }

        return "e2e4";
    }

    public synchronized Map<String, Object> getEvaluation(String moves, int depth) {
        Map<String, Object> result = new HashMap<>();
        result.put("score", 0);
        result.put("isMate", false);
        result.put("bestMove", "e2e4");

        if (!isAvailable || process == null || !process.isAlive()) {
            System.err.println("Stockfish n'est pas disponible pour l'évaluation.");
            return result;
        }

        try {
            clearBufferAndStop();

            String positionCmd = moves == null || moves.isEmpty()
                    ? "position startpos"
                    : "position startpos moves " + moves;

            sendCommand(positionCmd);
            sendCommand("go depth " + depth);

            int lastScore = 0;
            boolean lastIsMate = false;
            String bestMove = "e2e4";

            long startTime = System.currentTimeMillis();
            String line;
            while ((line = reader.readLine()) != null) {
                // Parse info lines for score
                if (line.contains("score cp ")) {
                    try {
                        int idx = line.indexOf("score cp ") + 9;
                        String scoreStr = line.substring(idx).split(" ")[0];
                        lastScore = Integer.parseInt(scoreStr);
                        lastIsMate = false;
                    } catch (Exception e) {
                        // Ignore parse errors
                    }
                } else if (line.contains("score mate ")) {
                    try {
                        int idx = line.indexOf("score mate ") + 11;
                        String mateStr = line.substring(idx).split(" ")[0];
                        lastScore = Integer.parseInt(mateStr);
                        lastIsMate = true;
                    } catch (Exception e) {
                        // Ignore parse errors
                    }
                }

                if (line.startsWith("bestmove")) {
                    String[] parts = line.split(" ");
                    if (parts.length > 1) {
                        bestMove = parts[1];
                    }
                    break;
                }

                if (System.currentTimeMillis() - startTime > COMMAND_TIMEOUT_MS) {
                    System.err.println("Timeout attendant l'évaluation de Stockfish");
                    break;
                }
            }

            result.put("score", lastScore);
            result.put("isMate", lastIsMate);
            result.put("bestMove", bestMove);
        } catch (Exception e) {
            System.err.println("Erreur Stockfish (getEvaluation): " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private void sendCommand(String command) throws IOException {
        if (writer != null) {
            writer.write(command + "\n");
            writer.flush();
        }
    }

    private void waitFor(String expected, long timeoutMs) throws IOException {
        long startTime = System.currentTimeMillis();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(expected)) {
                break;
            }
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                System.err.println("Timeout attendant: " + expected);
                break;
            }
        }
    }

    @PreDestroy
    public void stop() {
        try {
            if (writer != null) {
                sendCommand("quit");
            }
            if (process != null) {
                process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                if (process.isAlive()) {
                    process.destroy();
                }
            }
        } catch (Exception e) {
            System.err.println("Error stopping Stockfish: " + e.getMessage());
        }
    }
}
