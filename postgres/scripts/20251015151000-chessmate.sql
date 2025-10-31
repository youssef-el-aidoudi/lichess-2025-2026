CREATE TABLE Joueur
(
    id           SERIAL,
    pseudonyme   VARCHAR(50) NOT NULL,
    elo          SMALLINT,
    nb_victoires INT DEFAULT 0,
    nb_defaites  INT DEFAULT 0,
    nb_nulles    INT DEFAULT 0,
    equipe       VARCHAR(100),
    fide_id      VARCHAR(20),
    PRIMARY KEY (id),
    UNIQUE (pseudonyme)
);

CREATE TABLE Utilisateur
(
    id            SERIAL,
    email         VARCHAR(150) NOT NULL,
    hash          VARCHAR(255) NOT NULL,
    nom           VARCHAR(50),
    prenom        VARCHAR(50),
    inscription_a TIMESTAMP     NOT NULL,
    connexion_a   TIMESTAMP,
    banni_a       TIMESTAMP,
    id_joueur     INT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (id_joueur),
    UNIQUE (email),
    FOREIGN KEY (id_joueur) REFERENCES Joueur (id)
);

CREATE TYPE CATEGORIE_CADENCE AS ENUM('Bullet','Blitz','Rapide','Classique');
CREATE TABLE Cadence
(
    id        SERIAL,
    libelle   VARCHAR(50) NOT NULL,
    temps     SMALLINT,
    increment SMALLINT,
    type_partie CATEGORIE_CADENCE NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (libelle)
);

CREATE TABLE Role
(
    id      SERIAL,
    libelle VARCHAR(50),
    PRIMARY KEY (id)
);

CREATE TABLE Profil
(
    id      SERIAL,
    libelle VARCHAR(50),
    PRIMARY KEY (id)
);

CREATE TABLE Suite_Coups_Stats
(
    id                     SERIAL,
    pgn                    TEXT NOT NULL,
    nb_victoires           INT DEFAULT 0,
    nb_defaites            INT DEFAULT 0,
    nb_nulles              INT DEFAULT 0,
    probabilite_coup       VARCHAR(50),
    id_suite_coups_stats_precedente INT,
    PRIMARY KEY (id),
    FOREIGN KEY (id_suite_coups_stats_precedente) REFERENCES Suite_Coups_Stats (id)
);

CREATE TABLE Tournoi
(
    id              SERIAL,
    libelle         VARCHAR(255),
    code            VARCHAR(10),
    site            TEXT,
    broadcast_url   TEXT,
    date_debut      DATE,
    cree_a          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modifie_a       TIMESTAMP,
    Id_Organisateur INT NOT NULL,
    id_cadence      INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (libelle),
    FOREIGN KEY (Id_Organisateur) REFERENCES Joueur (id),
    FOREIGN KEY (id_cadence) REFERENCES Cadence (id)
);

CREATE TABLE Ouverture
(
    id          SERIAL,
    code        VARCHAR(50)  NOT NULL,
    libelle     VARCHAR(150) NOT NULL,
    id_position INT,
    PRIMARY KEY (id),
    UNIQUE (code),
    FOREIGN KEY (id_position) REFERENCES Suite_Coups_Stats (id)
);

CREATE TABLE Partie
(
    title            VARCHAR(255),
    id               SERIAL,
    resultat         SMALLINT,
    date_heure_utc   TIMESTAMP,
    elo_blanc        SMALLINT,
    elo_noir         SMALLINT,
    score_blanc_diff SMALLINT,
    score_noir_diff  SMALLINT,
    titre_blanc      VARCHAR(5),
    titre_noir       VARCHAR(5),
    type_resultat    VARCHAR(50),
    round            VARCHAR(20),
    broadcast_url    TEXT,
    game_url         TEXT,
    variant          VARCHAR(50),
    id_suite_coups_stats INT NOT NULL,
    id_ouverture     INT,
    id_cadence       INT,
    Id_Joueur_Blanc  INT NOT NULL,
    Id_Joueur_Noir   INT NOT NULL,
    id_tournoi       INT,
    pgn              TEXT,

    PRIMARY KEY (id),
    FOREIGN KEY (id_suite_coups_stats) REFERENCES Suite_Coups_Stats (id),
    FOREIGN KEY (id_ouverture) REFERENCES Ouverture (id),
    FOREIGN KEY (id_cadence) REFERENCES Cadence (id),
    FOREIGN KEY (Id_Joueur_Blanc) REFERENCES Joueur (id),
    FOREIGN KEY (Id_Joueur_Noir) REFERENCES Joueur (id),
    FOREIGN KEY (id_tournoi) REFERENCES Tournoi (id)
);

CREATE TABLE participer
(
    id_joueur   INT,
    id_tournoi  INT,
    inscription TIMESTAMP,
    PRIMARY KEY (id_joueur, id_tournoi),
    FOREIGN KEY (id_joueur) REFERENCES Joueur (id),
    FOREIGN KEY (id_tournoi) REFERENCES Tournoi (id)
);

CREATE TABLE Utilisateur_Role
(
    id_utilisateur INT,
    id_role        INT,
    PRIMARY KEY (id_utilisateur, id_role),
    FOREIGN KEY (id_utilisateur) REFERENCES Utilisateur (id),
    FOREIGN KEY (id_role) REFERENCES Role (id)
);

CREATE TABLE Utilisateur_Profil
(
    id_utilisateur INT,
    id_profil      INT,
    PRIMARY KEY (id_utilisateur, id_profil),
    FOREIGN KEY (id_utilisateur) REFERENCES Utilisateur (id),
    FOREIGN KEY (id_profil) REFERENCES Profil (id)
);

CREATE TABLE Profil_Role
(
    id_role   INT,
    id_profil INT,
    PRIMARY KEY (id_role, id_profil),
    FOREIGN KEY (id_role) REFERENCES Role (id),
    FOREIGN KEY (id_profil) REFERENCES Profil (id)
);

-- Données initiales nécessaires
INSERT INTO Joueur (pseudonyme, elo) VALUES ('System', 2000);
INSERT INTO Cadence (libelle, temps, increment, type_partie) VALUES
('1+0',   60,   0,  'Bullet'),
('1+1',   60,   1,  'Bullet'),
('2+1',  120,   1,  'Bullet'),
('3+0',  180,   0,  'Blitz'),
('3+2',  180,   2,  'Blitz'),
('3+5',  180,   5,  'Blitz'),
('5+0',  300,   0,  'Blitz'),
('5+3',  300,   3,  'Blitz'),
('5+5',  300,   5,  'Blitz'),
('10+0', 600,   0,  'Rapide'),
('10+5', 600,   5,  'Rapide'),
('10+10',600,  10,  'Rapide'),
('15+10',900,  10,  'Rapide'),
('15+15',900,  15,  'Rapide'),
('25+10',1500, 10,  'Rapide'),
('30+0',1800,   0,  'Classique'),
('30+30',1800, 30,  'Classique'),
('45+45',2700, 45,  'Classique'),
('60+0',3600,   0,  'Classique'),
('90+0',5400,   0,  'Classique'),
('90+30',5400, 30,  'Classique'),
('120+0',7200,  0,  'Classique'),
('120+30',7200, 30, 'Classique');