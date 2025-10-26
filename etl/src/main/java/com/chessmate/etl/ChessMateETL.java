package com.chessmate.etl;

import io.minio.*;
import java.io.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class ChessMateETL {

    static class Game {
        String white, black, result, date, eco, timeControl;
        Integer whiteElo, blackElo;
    }

    public static void main(String[] args) throws Exception {
        //connexion à MinIO
        MinioClient minioClient = MinioClient.builder()
            .endpoint("http://localhost:9000")
            .credentials("minioadmin", "minioadmin")
            .build();

        InputStream in = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket("lichess-raw")
                .object("lichess_2025_09.pgn")
                .build()
        );

        //connexion PostgreSQL
        Connection conn = DriverManager.getConnection(
            "jdbc:postgresql://localhost:5432/lichess", "postgres", "password");
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO game (white, black, result, date, eco, timecontrol, whiteelo, blackelo) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

        //parser ligne par ligne
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        Game current = new Game();
        int batchCount = 0;
        int total = 0;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("[")) {
                parseMetadata(line, current);
            } else if (line.trim().isEmpty() && current.white != null) {
                //fin de partie
                if (filter(current)) {
                    insertBatch(ps, current);
                    batchCount++;
                    total++;
                }
                if (batchCount >= 1000) {
                    ps.executeBatch();
                    batchCount = 0;
                    System.out.println("Inserted " + total + " games...");
                }
                current = new Game();
            }
        }
        ps.executeBatch();
        System.out.println("✅ Done. Total games inserted: " + total);
    }

    // ------------- Fonctions utilitaires -------------

    static void parseMetadata(String line, Game g) {
        int keyEnd = line.indexOf(" ");
        int valStart = line.indexOf("\"") + 1;
        int valEnd = line.lastIndexOf("\"");
        if (keyEnd == -1 || valStart == -1 || valEnd == -1) return;
        String key = line.substring(1, keyEnd);
        String val = line.substring(valStart, valEnd);

        switch (key) {
            case "White": g.white = val; break;
            case "Black": g.black = val; break;
            case "Result": g.result = val; break;
            case "Date": g.date = val; break;
            case "ECO": g.eco = val; break;
            case "TimeControl": g.timeControl = val; break;
            case "WhiteElo": g.whiteElo = parseInt(val); break;
            case "BlackElo": g.blackElo = parseInt(val); break;
        }
    }

    static boolean filter(Game g) {
        return g.whiteElo != null && g.blackElo != null
            && g.whiteElo >= 2000 && g.blackElo >= 2000
            && g.result != null;
    }

    static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    static void insertBatch(PreparedStatement ps, Game g) throws SQLException {
        ps.setString(1, g.white);
        ps.setString(2, g.black);
        ps.setString(3, g.result);
        ps.setDate(4, g.date != null ? Date.valueOf(g.date.replace('.', '-')) : null);
        ps.setString(5, g.eco);
        ps.setString(6, g.timeControl);
        ps.setObject(7, g.whiteElo);
        ps.setObject(8, g.blackElo);
        ps.addBatch();
    }
}