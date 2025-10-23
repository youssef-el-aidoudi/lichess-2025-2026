package com.chessmate;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.sql.Date;

public class ChessMateETL {

    //classe pour garder les infos d'une partie
    static class Game {
        String white, black, result, event, site;
        String date, round, variant, eco, opening;
        Integer whiteElo, blackElo;
        String whiteTitle, blackTitle, whiteTeam, blackTeam, whiteFideId, blackFideId;
        String utcDate, utcTime;
        String broadcastName, broadcastUrl, gameUrl;
        String timeControl;
        StringBuilder moves = new StringBuilder(); //stock les coups
    }

    public static void main(String[] args) throws Exception {
        //connexion à la base, mots de passe à changer lol
        Connection conn = DriverManager.getConnection(
            "jdbc:postgresql://localhost:5432/chessmate", "chessmate", "CHANGEME");
        
        conn.setAutoCommit(false); // on commit nous même, pas de commit auto

        //prepare statement pour inserer un joueur, si pseudo existe deja -> update
        PreparedStatement psJoueur = conn.prepareStatement(
            "INSERT INTO Joueur (pseudonyme, elo, equipe, fide_id) VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (pseudonyme) DO UPDATE SET " +
            "elo = EXCLUDED.elo, equipe = EXCLUDED.equipe, fide_id = EXCLUDED.fide_id " +
            "RETURNING id");

        //prepare statement pour inserer tournoi, si existe deja -> skip
        PreparedStatement psTournoi = conn.prepareStatement(
            "INSERT INTO Tournoi (libelle, site, broadcast_url, date_debut, Id_Organisateur, id_cadence) " +
            "VALUES (?, ?, ?, ?, 1, 1) " +
            "ON CONFLICT (libelle) DO NOTHING RETURNING id");
        
        PreparedStatement psCheckTournoi = conn.prepareStatement(
            "SELECT id FROM Tournoi WHERE libelle = ?");

        //prepare statement pour inserer ouverture, si existe deja -> update
        PreparedStatement psOuverture = conn.prepareStatement(
            "INSERT INTO Ouverture (code, libelle, id_position) VALUES (?, ?, ?) " +
            "ON CONFLICT (code) DO UPDATE SET libelle = EXCLUDED.libelle " +
            "RETURNING id");
        
        //prepare cadence (genre rapid/blitz etc)
        PreparedStatement psCadence = conn.prepareStatement(
            "INSERT INTO Cadence (libelle, temps, increment) VALUES (?, NULL, NULL) " +
            "ON CONFLICT (libelle) DO NOTHING RETURNING id");
        
        PreparedStatement psCheckCadence = conn.prepareStatement(
            "SELECT id FROM Cadence WHERE libelle = ?");
        
        //prepare statement pour inserer la partie
        PreparedStatement psPartie = conn.prepareStatement(
            "INSERT INTO Partie (title, resultat, pgn, date_heure_utc, elo_blanc, elo_noir, " +
            "titre_blanc, titre_noir, round, broadcast_url, game_url, variant, " +
            "Id_Joueur_Blanc, Id_Joueur_Noir, id_ouverture, id_cadence, id_tournoi) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        //lecture du fichier pgn
        System.out.println("lecture du fichier...");
        String filePath = "../data/lichess_db_broadcast_2025-09.pgn";
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        System.out.println("ok : fichier trouvé !");
        
        String line;
        Game current = new Game();
        int batchCount = 0;
        int total = 0;
        int skipped = 0;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("[")) {
                parseMetadata(line, current); //parse les metadatas genre white, black, date etc
            } else if (!line.trim().isEmpty() && !line.startsWith("[")) {
                current.moves.append(line).append("\n"); // stock les coups
            } else if (line.trim().isEmpty() && current.white != null) {
                //si la partie a du sens
                if (filter(current)) { //ffiltre a choisir
                    try {
                        //insert les joueurs et recup id
                        int idBlanc = insererJoueur(psJoueur, current.white, current.whiteElo, 
                                                     current.whiteTeam, current.whiteFideId);
                        int idNoir = insererJoueur(psJoueur, current.black, current.blackElo, 
                                                    current.blackTeam, current.blackFideId);
                        
                        Integer idTournoi = null;
                        if (current.broadcastName != null) {
                            idTournoi = insererTournoi(psTournoi, psCheckTournoi, current);
                        }
                        
                        Integer idOuverture = null;
                        if (current.eco != null) {
                            Integer idPosition = getIdPosition(conn, current.moves.toString());
                            idOuverture = insererOuverture(psOuverture, current.eco, current.opening, idPosition);
                        }
                        
                        Integer idCadence = null;
                        if (current.timeControl != null) {
                            idCadence = insererCadence(psCadence, psCheckCadence, current.timeControl);
                        }
                        
                        insererPartie(psPartie, current, idBlanc, idNoir, idOuverture, idCadence, idTournoi);
                        
                        total++;
                        batchCount++;
                        
                        if (batchCount >= 100) { //commit tous les 100 parties
                            conn.commit();
                            batchCount = 0;
                            System.out.println("parties insérées : " + total + " (ignorées : " + skipped + ")");
                        }
                    } catch (Exception e) {
                        System.err.println("erreur pour : " + current.white + " vs " + current.black);
                        e.printStackTrace();
                        conn.rollback(); //rollback si pb
                    }
                } else {
                    skipped++;
                }
                current = new Game(); //reset la game
            }
        }
        
        if (batchCount > 0) { //commit final
            conn.commit();
        }
        
        reader.close();
        System.out.println("terminé ! total : " + total + " parties insérées, " + skipped + " ignorées");
        conn.close();
    }

    //parse la metadata ligne type [Key "Value"]
    static void parseMetadata(String line, Game g) {
        int keyEnd = line.indexOf(" ");
        int valStart = line.indexOf("\"") + 1;
        int valEnd = line.lastIndexOf("\"");
        if (keyEnd == -1 || valStart == -1 || valEnd == -1) return;
        
        String key = line.substring(1, keyEnd);
        String val = line.substring(valStart, valEnd);
        
        switch (key) { //switch case pour remplir les infos de la game
            case "Event": g.event = val; break;
            case "Date": g.date = val; break;
            case "Round": g.round = val; break;
            case "White": g.white = val; break;
            case "Black": g.black = val; break;
            case "Result": g.result = val; break;
            case "Variant": g.variant = val; break;
            case "ECO": g.eco = val; break;
            case "Opening": g.opening = val; break;
            case "TimeControl": g.timeControl = val; break;
            
            case "WhiteElo": g.whiteElo = parseInt(val); break;
            case "WhiteTitle": g.whiteTitle = val; break;
            case "WhiteTeam": g.whiteTeam = val; break;
            case "WhiteFideId": g.whiteFideId = val; break;
            
            case "BlackElo": g.blackElo = parseInt(val); break;
            case "BlackTitle": g.blackTitle = val; break;
            case "BlackTeam": g.blackTeam = val; break;
            case "BlackFideId": g.blackFideId = val; break;
            
            case "UTCDate": g.utcDate = val; break;
            case "UTCTime": g.utcTime = val; break;
            case "BroadcastName": g.broadcastName = val; break;
            case "BroadcastURL": g.broadcastUrl = val; break;
            case "GameURL": g.gameUrl = val; break;
        }
    }
    
    //filtre les parties trop faibles ou avec resultat inconnu //à definir
    static boolean filter(Game g) {
        return g.whiteElo != null && g.blackElo != null
            && g.whiteElo >= 2000 && g.blackElo >= 2000
            && g.result != null && !g.result.equals("*");
    }
    
    static Integer parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }
    
    // insert un joueur et recup id
    static int insererJoueur(PreparedStatement ps, String nom, Integer elo, 
                             String equipe, String fideId) throws SQLException {
        ps.setString(1, nom);
        ps.setObject(2, elo);
        ps.setString(3, equipe);
        ps.setString(4, fideId);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt("id");
    }
    
    // insert tournoi si inexistant
    static Integer insererTournoi(PreparedStatement psInsert, PreparedStatement psCheck, 
                                  Game g) throws SQLException {
        String libelle = truncate(g.broadcastName, 255);
        
        psCheck.setString(1, libelle);
        ResultSet rs = psCheck.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
        
        psInsert.setString(1, libelle);
        psInsert.setString(2, g.site);
        psInsert.setString(3, g.broadcastUrl);
        psInsert.setDate(4, parseDate(g.utcDate));
        
        ResultSet rsInsert = psInsert.executeQuery();
        if (rsInsert.next()) {
            return rsInsert.getInt("id");
        }
        return null;
    }
    
    //insert ouverture
    static Integer insererOuverture(PreparedStatement ps, String eco, String opening, 
                                    Integer idPosition) throws SQLException {
        ps.setString(1, truncate(eco, 50));
        ps.setString(2, opening != null ? truncate(opening, 150) : "Unknown");
        ps.setObject(3, idPosition);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt("id");
    }

    static Integer getIdPosition(Connection conn, String pgn) throws SQLException {
        String pgnHash = pgn.substring(0, Math.min(pgn.length(), 1000));
        
        PreparedStatement ps = conn.prepareStatement(
            "SELECT id FROM Position_Stats WHERE pgn = ?"
        );
        ps.setString(1, pgnHash);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("id");
        
        ps = conn.prepareStatement(
            "INSERT INTO Position_Stats (pgn, id_position_precedente) VALUES (?, NULL) RETURNING id"
        );
        ps.setString(1, pgnHash);
        rs = ps.executeQuery();
        rs.next();
        return rs.getInt("id");
    }
    
    static Integer insererCadence(PreparedStatement psInsert, PreparedStatement psCheck,
                                    String timeControl) throws SQLException {
            if (timeControl == null) return null;
            
            String libelle = truncate(timeControl, 50);
            
            //essayer d'abord de récupérer
            psCheck.setString(1, libelle);
            ResultSet rs = psCheck.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                rs.close();
                return id;
            }
            rs.close();
            
            //sinon insérer
            psInsert.setString(1, libelle);
            ResultSet rsInsert = psInsert.executeQuery();
            if (rsInsert.next()) {
                int id = rsInsert.getInt("id");
                rsInsert.close();
                return id;
            }
            rsInsert.close();
            
            //si ça échoue, retenter un SELECT (race condition possible)
            psCheck.setString(1, libelle);
            rs = psCheck.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                rs.close();
                return id;
            }
            rs.close();
            
            return null;
        }
    
    static void insererPartie(PreparedStatement ps, Game g, int idBlanc, int idNoir,
                              Integer idOuverture, Integer idCadence, Integer idTournoi) throws SQLException {
        //pour définir vainqueur
        int resultat = g.result.equals("1-0") ? 2 : g.result.equals("0-1") ? 0 : 1;
        
        int idx = 1;
        ps.setString(idx++, truncate(g.event, 255));
        ps.setInt(idx++, resultat);
        ps.setString(idx++, cleanPgn(g.moves.toString()));
        ps.setTimestamp(idx++, parseTimestamp(g.utcDate, g.utcTime));
        ps.setObject(idx++, g.whiteElo);
        ps.setObject(idx++, g.blackElo);
        ps.setString(idx++, g.whiteTitle);
        ps.setString(idx++, g.blackTitle);
        ps.setString(idx++, g.round);
        ps.setString(idx++, g.broadcastUrl);
        ps.setString(idx++, g.gameUrl);
        ps.setString(idx++, g.variant);
        ps.setInt(idx++, idBlanc);
        ps.setInt(idx++, idNoir);
        ps.setObject(idx++, idOuverture);
        ps.setObject(idx++, idCadence);
        ps.setObject(idx++, idTournoi);
        
        ps.executeUpdate();
    }
    
    static Date parseDate(String s) {
        if (s == null) return null;
        try {
            return Date.valueOf(s.replace('.', '-'));
        } catch (Exception e) {
            return null;
        }
    }
    
    static Timestamp parseTimestamp(String date, String time) {
        if (date == null) return null;
        try {
            String datetime = date.replace('.', '-') + " " + (time != null ? time : "00:00:00");
            return Timestamp.valueOf(datetime);
        } catch (Exception e) {
            return null;
        }
    }
    
    static String truncate(String s, int maxLength) {
        if (s == null) return null;
        return s.length() > maxLength ? s.substring(0, maxLength) : s;
    }


    // remplace tous les espaces multiples et retours ligne par un seul espace
    static String cleanPgn(String raw) {
    if (raw == null) return null;
    return raw.replaceAll("\\s+", " ").trim();
}
}
