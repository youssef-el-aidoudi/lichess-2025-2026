package com.chessmate;

import org.junit.jupiter.api.*;
import java.sql.*;

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
                ""
        );

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
}