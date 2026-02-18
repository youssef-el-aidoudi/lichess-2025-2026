package com.chessmate.backend.service;

import com.chessmate.backend.cheat.engine.EngineMoveAnalysis;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Profile("!test") // ✅ ne démarre pas Stockfish en tests
@Component
public class StockfishWrapper {

    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;

    private static final long COMMAND_TIMEOUT_MS = 5000;

    private int depth = 15;
    private int movetime = 1000;
    private boolean isAvailable = false;

    // -------------------- PATH RESOLUTION --------------------

    private String getStockfishPath() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        boolean isUnix = !isWindows; // linux + mac

        // 1) PATH
        try {
            String[] cmd = isUnix
                    ? new String[]{"sh", "-c", "which stockfish"}
                    : new String[]{"cmd", "/c", "where stockfish"};

            Process p = new ProcessBuilder(cmd).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = br.readLine();
                if (line != null && !line.isBlank()) {
                    System.out.println(">>> Found Stockfish in PATH: " + line.trim());
                    return line.trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Could not find stockfish in PATH: " + e.getMessage());
        }

        // 2) Docker / Linux common paths
        if (isUnix) {
            String[] candidates = {"/usr/games/stockfish", "/usr/bin/stockfish", "/bin/stockfish"};
            for (String c : candidates) {
                if (Files.exists(Paths.get(c))) {
                    System.out.println(">>> Found Stockfish at: " + c);
                    return c;
                }
            }
        }

        // 3) Local dev Windows exe (only if Windows)
        if (isWindows) {
            String currentDir = System.getProperty("user.dir");
            String devPath = currentDir + File.separator + "src" + File.separator + "main" + File.separator +
                    "java" + File.separator + "com" + File.separator + "chessmate" + File.separator +
                    "backend" + File.separator + "service" + File.separator + "stockfish-windows-x86-64-avx2.exe";

            if (Files.exists(Paths.get(devPath))) {
                System.out.println(">>> Found Stockfish at: " + devPath);
                return devPath;
            }
        }

        throw new FileNotFoundException(
                "Stockfish executable not found. OS=" + os +
                        ". Install it (Mac: brew install stockfish, Linux: apt install stockfish) or put it in PATH."
        );
    }

    // -------------------- LIFECYCLE --------------------

    @PostConstruct
    public void start() {
        try {
            String stockfishPath = getStockfishPath();
            System.out.println(">>> Starting Stockfish: " + stockfishPath);

            ProcessBuilder builder = new ProcessBuilder(stockfishPath);
            builder.redirectErrorStream(true);

            process = builder.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            // UCI init
            sendCommand("uci");
            waitFor("uciok", 3000);

            sendCommand("isready");
            waitFor("readyok", 3000);

            isAvailable = true;
            System.out.println(">>> Stockfish ready!");
        } catch (Exception e) {
            System.err.println("ERREUR: Stockfish not available: " + e.getMessage());
            e.printStackTrace();
            isAvailable = false; // app continues without engine
        }
    }

    @PreDestroy
    public void stop() {
        try {
            if (writer != null) {
                sendCommand("quit");
            }
            if (process != null) {
                process.waitFor(2, TimeUnit.SECONDS);
                if (process.isAlive()) process.destroy();
            }
        } catch (Exception e) {
            System.err.println("Error stopping Stockfish: " + e.getMessage());
        }
    }

    // -------------------- PUBLIC API --------------------

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setMovetime(int movetime) {
        this.movetime = movetime;
    }

    /** Bestmove depuis startpos + moves UCI ("e2e4 e7e5 ...") */
    public synchronized String getBestMove(String moves, String mode) {
        if (!isAvailable || process == null || !process.isAlive()) {
            System.err.println("Stockfish is not available. Returning default move.");
            return "e2e4";
        }

        try {
            String positionCmd = (moves == null || moves.isBlank())
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

            long startTime = System.currentTimeMillis();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    String[] parts = line.split("\\s+");
                    return (parts.length > 1) ? parts[1] : "e2e4";
                }
                if (System.currentTimeMillis() - startTime > COMMAND_TIMEOUT_MS) {
                    System.err.println("Timeout waiting for Stockfish bestmove");
                    return "e2e4";
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur Stockfish (getBestMove): " + e.getMessage());
        }
        return "e2e4";
    }

    /**
     * ✅ Analyse une FEN et retourne:
     * - bestMove
     * - topMoves (via MultiPV, topN)
     */
    public synchronized EngineMoveAnalysis analyzeFen(String fen, int depth, int topN) {
        if (!isAvailable || process == null || !process.isAlive()) {
            return new EngineMoveAnalysis("e2e4", List.of("e2e4"));
        }

        try {
            int n = Math.max(1, topN);

            // MultiPV => plusieurs variantes => top moves
            sendCommand("setoption name MultiPV value " + n);
            sendCommand("isready");
            waitFor("readyok", 1500);

            sendCommand("position fen " + fen);
            sendCommand("go depth " + Math.max(1, depth));

            Set<String> topMoves = new LinkedHashSet<>();
            String best = null;

            long startTime = System.currentTimeMillis();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("info") && line.contains(" pv ")) {
                    int idx = line.indexOf(" pv ");
                    if (idx >= 0) {
                        String pv = line.substring(idx + 4).trim();
                        String[] pvParts = pv.split("\\s+");
                        if (pvParts.length > 0) {
                            String firstMove = pvParts[0].trim();
                            if (!firstMove.isEmpty()) topMoves.add(firstMove);
                        }
                    }
                }

                if (line.startsWith("bestmove")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 1) best = parts[1];
                    break;
                }

                if (System.currentTimeMillis() - startTime > COMMAND_TIMEOUT_MS) {
                    break;
                }
            }

            if (best == null) best = topMoves.stream().findFirst().orElse("e2e4");
            List<String> list = new ArrayList<>(topMoves);
            if (list.isEmpty()) list = List.of(best);

            return new EngineMoveAnalysis(best, list);

        } catch (Exception e) {
            return new EngineMoveAnalysis("e2e4", List.of("e2e4"));
        }
    }

    // -------------------- INTERNAL --------------------

    private void sendCommand(String command) throws IOException {
        if (writer == null) return;
        writer.write(command);
        writer.write("\n");
        writer.flush();
    }

    private void waitFor(String expected, long timeoutMs) throws IOException {
        long startTime = System.currentTimeMillis();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(expected)) return;
            if (System.currentTimeMillis() - startTime > timeoutMs) return;
        }
    }
}