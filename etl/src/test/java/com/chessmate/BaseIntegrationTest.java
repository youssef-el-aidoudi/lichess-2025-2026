package com.chessmate;

import org.junit.jupiter.api.*;
import java.sql.*;

/**
 * Classe de base pour les tests d'intégration.
 * Gère la connexion H2 et le schéma de base de données.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    protected Connection connection;

    @BeforeAll
    public void setupDatabase() throws Exception {
        Class.forName("org.h2.Driver");

        connection = DriverManager.getConnection(
                "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
                "sa",
                ""
        );

        connection.setAutoCommit(false);
        createSchema();
        insertReferenceData();
        connection.commit();
    }

    @AfterAll
    public void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @BeforeEach
    public void resetCaches() {
        ChessMateETL.cadences = new java.util.HashMap<>();
        ChessMateETL.ouvertures = new java.util.HashMap<>();
    }

    @AfterEach
    public void cleanupTables() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM Partie");
            stmt.execute("DELETE FROM Joueur");
            stmt.execute("DELETE FROM Tournoi");
            connection.commit();
        }
    }

    private void createSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE Organisateur (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    nom VARCHAR(255)
                )
            """);

            stmt.execute("""
                CREATE TABLE Cadence (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    libelle VARCHAR(50) UNIQUE NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE Ouverture (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(10) NOT NULL,
                    libelle VARCHAR(255) NOT NULL,
                    UNIQUE(code, libelle)
                )
            """);

            stmt.execute("""
                CREATE TABLE Joueur (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    pseudonyme VARCHAR(255) UNIQUE NOT NULL,
                    elo INT,
                    equipe VARCHAR(255),
                    fide_id VARCHAR(50)
                )
            """);

            stmt.execute("""
                CREATE TABLE Tournoi (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    libelle VARCHAR(255) UNIQUE NOT NULL,
                    site VARCHAR(255),
                    broadcast_url TEXT,
                    date_debut DATE,
                    Id_Organisateur INT,
                    id_cadence INT,
                    FOREIGN KEY (Id_Organisateur) REFERENCES Organisateur(id),
                    FOREIGN KEY (id_cadence) REFERENCES Cadence(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE Partie (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    title VARCHAR(255),
                    resultat INT,
                    date_heure_utc TIMESTAMP,
                    elo_blanc INT,
                    elo_noir INT,
                    titre_blanc VARCHAR(10),
                    titre_noir VARCHAR(10),
                    round VARCHAR(50),
                    broadcast_url TEXT,
                    game_url TEXT,
                    variant VARCHAR(50),
                    Id_Joueur_Blanc INT NOT NULL,
                    Id_Joueur_Noir INT NOT NULL,
                    id_ouverture INT,
                    id_cadence INT,
                    id_tournoi INT,
                    pgn TEXT,
                    FOREIGN KEY (Id_Joueur_Blanc) REFERENCES Joueur(id),
                    FOREIGN KEY (Id_Joueur_Noir) REFERENCES Joueur(id),
                    FOREIGN KEY (id_ouverture) REFERENCES Ouverture(id),
                    FOREIGN KEY (id_cadence) REFERENCES Cadence(id),
                    FOREIGN KEY (id_tournoi) REFERENCES Tournoi(id)
                )
            """);
        }
    }

    private void insertReferenceData() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO Organisateur (id, nom) VALUES (1, 'Test Org')");

            stmt.execute("INSERT INTO Cadence (id, libelle) VALUES (1, '180+2')");
            stmt.execute("INSERT INTO Cadence (id, libelle) VALUES (2, '300+0')");
            stmt.execute("INSERT INTO Cadence (id, libelle) VALUES (3, '600+5')");

            stmt.execute("INSERT INTO Ouverture (id, code, libelle) VALUES (1, 'C42', 'Petrov Defense')");
            stmt.execute("INSERT INTO Ouverture (id, code, libelle) VALUES (2, 'E60', 'King''s Indian Defense')");
            stmt.execute("INSERT INTO Ouverture (id, code, libelle) VALUES (3, 'C65', 'Ruy Lopez, Berlin Defense')");
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
}