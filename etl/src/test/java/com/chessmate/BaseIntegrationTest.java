package com.chessmate;

import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.Map;

/**
 * Classe de base pour les tests d'intégration.
 * Gère la connexion H2, le schéma et les données de référence.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    protected Connection connection;

    @BeforeAll
    public void setupDatabase() throws Exception {
        // Charger le driver H2
        Class.forName("org.h2.Driver");

        // Connexion H2 en mémoire avec mode PostgreSQL
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
                "sa",
                "");

        connection.setAutoCommit(false);

        // Créer le schéma une seule fois
        createSchema();
        connection.commit();
    }

    @AfterAll
    public void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @BeforeEach
    public void prepareTestData() throws SQLException {
        // Reset caches
        ChessMateETL.cadences.clear();
        ChessMateETL.ouvertures.clear();

        // Nettoyer les tables
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM Partie");
            stmt.execute("DELETE FROM Joueur");
            stmt.execute("DELETE FROM Tournoi");
            stmt.execute("DELETE FROM Ouverture");
            stmt.execute("DELETE FROM Cadence");
            stmt.execute("DELETE FROM Organisateur");
            connection.commit();
        }

        // Ré-insertion des données de référence
        insertReferenceData();
        connection.commit();
    }

    private void createSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            stmt.execute("""
                        CREATE TABLE Organisateur (
                            id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            nom VARCHAR(255)
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE Cadence (
                            id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            libelle VARCHAR(50) UNIQUE NOT NULL
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE Ouverture (
                            id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            code VARCHAR(10) NOT NULL,
                            libelle VARCHAR(255) NOT NULL,
                            UNIQUE(code, libelle)
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE Joueur (
                            id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            pseudonyme VARCHAR(255) UNIQUE NOT NULL,
                            elo INT,
                            equipe VARCHAR(255),
                            fide_id VARCHAR(50)
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE Tournoi (
                            id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            libelle VARCHAR(255) UNIQUE NOT NULL,
                            site VARCHAR(255),
                            broadcast_url TEXT,
                            date_debut DATE,
                            id_organisateur INT,
                            id_cadence INT,
                            FOREIGN KEY (id_organisateur) REFERENCES Organisateur(id),
                            FOREIGN KEY (id_cadence) REFERENCES Cadence(id)
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE Partie (
                             id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             title VARCHAR(255),
                             resultat SMALLINT,
                             pgn TEXT,
                             date_heure_utc TIMESTAMP,
                             elo_blanc SMALLINT,
                             elo_noir SMALLINT,
                             score_blanc_diff SMALLINT,
                             score_noir_diff SMALLINT,
                             titre_blanc VARCHAR(5),
                             titre_noir VARCHAR(5),
                             type_resultat VARCHAR(50),
                             round VARCHAR(20),
                             broadcast_url TEXT,
                             game_url TEXT,
                             variant VARCHAR(50),
                             id_ouverture INT,
                             id_cadence INT,
                             id_joueur_blanc INT NOT NULL,
                             id_joueur_noir INT NOT NULL,
                             id_tournoi INT,
                             FOREIGN KEY (id_ouverture) REFERENCES Ouverture(id),
                             FOREIGN KEY (id_cadence) REFERENCES Cadence(id),
                             FOREIGN KEY (id_joueur_blanc) REFERENCES Joueur(id),
                             FOREIGN KEY (id_joueur_noir) REFERENCES Joueur(id),
                             FOREIGN KEY (id_tournoi) REFERENCES Tournoi(id)
                         )
                    """);
        }
    }

    private void insertReferenceData() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Organisateur
            stmt.execute("INSERT INTO Organisateur (nom) VALUES ('Test Org')");

            // Cadences
            stmt.execute("INSERT INTO Cadence (libelle) VALUES ('180+2')");
            stmt.execute("INSERT INTO Cadence (libelle) VALUES ('300+0')");
            stmt.execute("INSERT INTO Cadence (libelle) VALUES ('600+5')");

            // Ouvertures
            stmt.execute("INSERT INTO Ouverture (code, libelle) VALUES ('C42', 'Petrov Defense')");
            stmt.execute("INSERT INTO Ouverture (code, libelle) VALUES ('E60', 'King''s Indian Defense')");
            stmt.execute("INSERT INTO Ouverture (code, libelle) VALUES ('C65', 'Ruy Lopez, Berlin Defense')");
        }
    }

    protected Game createBasicGame(String white, String black, String result) {
        Game g = new Game();
        g.white = white;
        g.black = black;
        g.result = result;
        g.event = "Test Event";
        g.whiteElo = 2000;
        g.blackElo = 2000;
        g.utcDate = "2024.01.15";
        g.utcTime = "14:30:00";
        g.eco = "C42";
        g.opening = "Petrov Defense";
        g.timeControl = "180+2";
        g.moves = new StringBuilder("1. e4 e5");
        return g;
    }

    /**
     * Version compatible H2 de ChessMateETL.copyJoueurs.
     * Utilise des instructions SQL INSERT avec MERGE au lieu de PostgreSQL COPY.
     */
    protected Map<String, Integer> copyJoueursH2(Map<String, JoueurDTO> joueurs) throws SQLException {
        if (joueurs.isEmpty())
            return new java.util.HashMap<>();

        // Insère chaque joueur en utilisant MERGE (version H2 de INSERT ... ON
        // CONFLICT)
        String mergeSql = "MERGE INTO Joueur (pseudonyme, elo, equipe, fide_id) " +
                "KEY(pseudonyme) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(mergeSql)) {
            for (JoueurDTO j : joueurs.values()) {
                pstmt.setString(1, j.pseudonyme);
                if (j.elo != null) {
                    pstmt.setInt(2, j.elo);
                } else {
                    pstmt.setNull(2, java.sql.Types.INTEGER);
                }
                pstmt.setString(3, j.equipe);
                pstmt.setString(4, j.fideId);
                pstmt.executeUpdate();
            }
        }

        // Récupération des IDs
        Map<String, Integer> ids = new java.util.HashMap<>();
        StringBuilder pseudos = new StringBuilder();
        for (String pseudo : joueurs.keySet()) {
            if (pseudos.length() > 0)
                pseudos.append(",");
            pseudos.append("'").append(pseudo.replace("'", "''")).append("'");
        }

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT pseudonyme, id FROM Joueur WHERE pseudonyme IN (" + pseudos + ")")) {
            while (rs.next()) {
                ids.put(rs.getString("pseudonyme"), rs.getInt("id"));
            }
        }

        return ids;
    }

    /**
     * Version compatible H2 de ChessMateETL.copyTournois.
     * Utilise des instructions SQL INSERT avec MERGE au lieu de PostgreSQL COPY.
     */
    protected Map<String, Integer> copyTournoisH2(Map<String, TournoiDTO> tournois) throws SQLException {
        if (tournois.isEmpty())
            return new java.util.HashMap<>();

        // Récupération des IDs réels depuis la base de données
        int orgId = 1;
        int cadenceId = 1;
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id FROM Organisateur LIMIT 1")) {
            if (rs.next()) {
                orgId = rs.getInt("id");
            }
        }
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id FROM Cadence LIMIT 1")) {
            if (rs.next()) {
                cadenceId = rs.getInt("id");
            }
        }

        String mergeSql = "MERGE INTO Tournoi (libelle, site, broadcast_url, date_debut, id_organisateur, id_cadence) "
                +
                "KEY(libelle) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(mergeSql)) {
            for (TournoiDTO t : tournois.values()) {
                pstmt.setString(1, t.libelle);
                pstmt.setString(2, t.site);
                pstmt.setString(3, t.broadcastUrl);
                if (t.dateDebut != null) {
                    pstmt.setDate(4, new java.sql.Date(t.dateDebut.getTime()));
                } else {
                    pstmt.setNull(4, java.sql.Types.DATE);
                }
                pstmt.setInt(5, orgId);
                pstmt.setInt(6, cadenceId);
                pstmt.executeUpdate();
            }
        }

        // Récupération des IDs
        Map<String, Integer> ids = new java.util.HashMap<>();
        StringBuilder libelles = new StringBuilder();
        for (String lib : tournois.keySet()) {
            if (libelles.length() > 0)
                libelles.append(",");
            libelles.append("'").append(lib.replace("'", "''")).append("'");
        }

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT libelle, id FROM Tournoi WHERE libelle IN (" + libelles + ")")) {
            while (rs.next()) {
                ids.put(rs.getString("libelle"), rs.getInt("id"));
            }
        }

        return ids;
    }

    /**
     * Version compatible H2 de ChessMateETL.copyParties.
     * Utilise des instructions SQL INSERT standard au lieu de PostgreSQL COPY.
     */
    protected void copyPartiesH2(java.util.List<Game> parties,
            Map<String, Integer> joueurIds,
            Map<String, Integer> tournoiIds) throws SQLException {
        if (parties.isEmpty())
            return;

        String insertSql = "INSERT INTO Partie (title, resultat, date_heure_utc, elo_blanc, elo_noir, " +
                "titre_blanc, titre_noir, round, broadcast_url, game_url, variant, " +
                "id_joueur_blanc, id_joueur_noir, id_ouverture, id_cadence, id_tournoi, pgn) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            for (Game g : parties) {
                Integer idBlanc = joueurIds.get(g.white);
                Integer idNoir = joueurIds.get(g.black);

                if (idBlanc == null || idNoir == null)
                    continue;

                Integer idTournoi = g.broadcastName != null ? tournoiIds.get(g.broadcastName) : null;
                Integer idOuverture = ChessMateETL.getIdOuverture(g.eco, g.opening);
                Integer idCadence = ChessMateETL.getIdCadence(g.timeControl);

                int resultat = g.result.equals("1-0") ? 2 : g.result.equals("0-1") ? 0 : 1;

                int paramIdx = 1;
                pstmt.setString(paramIdx++, truncate(g.event, 255));
                pstmt.setInt(paramIdx++, resultat);

                java.sql.Timestamp ts = parseTimestamp(g.utcDate, g.utcTime);
                if (ts != null) {
                    pstmt.setTimestamp(paramIdx++, ts);
                } else {
                    pstmt.setNull(paramIdx++, java.sql.Types.TIMESTAMP);
                }

                if (g.whiteElo != null) {
                    pstmt.setInt(paramIdx++, g.whiteElo);
                } else {
                    pstmt.setNull(paramIdx++, java.sql.Types.SMALLINT);
                }

                if (g.blackElo != null) {
                    pstmt.setInt(paramIdx++, g.blackElo);
                } else {
                    pstmt.setNull(paramIdx++, java.sql.Types.SMALLINT);
                }

                pstmt.setString(paramIdx++, g.whiteTitle);
                pstmt.setString(paramIdx++, g.blackTitle);
                pstmt.setString(paramIdx++, g.round);
                pstmt.setString(paramIdx++, g.broadcastUrl);
                pstmt.setString(paramIdx++, g.gameUrl);
                pstmt.setString(paramIdx++, g.variant);
                pstmt.setInt(paramIdx++, idBlanc);
                pstmt.setInt(paramIdx++, idNoir);

                if (idOuverture != null) {
                    pstmt.setInt(paramIdx++, idOuverture);
                } else {
                    pstmt.setNull(paramIdx++, java.sql.Types.INTEGER);
                }

                if (idCadence != null) {
                    pstmt.setInt(paramIdx++, idCadence);
                } else {
                    pstmt.setNull(paramIdx++, java.sql.Types.INTEGER);
                }

                if (idTournoi != null) {
                    pstmt.setInt(paramIdx++, idTournoi);
                } else {
                    pstmt.setNull(paramIdx++, java.sql.Types.INTEGER);
                }

                pstmt.setString(paramIdx++, g.moves != null ? g.moves.toString() : null);

                pstmt.executeUpdate();
            }
        }
    }

    private String truncate(String s, int maxLength) {
        if (s == null)
            return null;
        return s.length() > maxLength ? s.substring(0, maxLength) : s;
    }

    private java.sql.Timestamp parseTimestamp(String date, String time) {
        if (date == null)
            return null;
        try {
            String datetime = date.replace('.', '-') + " " + (time != null ? time : "00:00:00");
            return java.sql.Timestamp.valueOf(datetime);
        } catch (Exception e) {
            return null;
        }
    }
}