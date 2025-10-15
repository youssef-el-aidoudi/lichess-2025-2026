CREATE TABLE Joueur
(
    id           COUNTER,
    pseudonyme   VARCHAR(50),
    elo          SMALLINT,
    nb_victoires INT,
    nb_defaites  INT,
    nb_nulles    INT,
    PRIMARY KEY (id)
);

CREATE TABLE Utilisateur
(
    id            COUNTER,
    email         VARCHAR(150) NOT NULL,
    hash          VARCHAR(255) NOT NULL,
    nom           VARCHAR(50),
    prenom        VARCHAR(50),
    inscription_a DATETIME     NOT NULL,
    connexion_a   DATETIME,
    banni_a       DATETIME,
    id_joueur     INT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (id_joueur),
    UNIQUE (email),
    FOREIGN KEY (id_joueur) REFERENCES Joueur (id)
);

CREATE TABLE Cadence
(
    id        COUNTER,
    libelle   VARCHAR(50),
    temps     SMALLINT,
    increment SMALLINT,
    PRIMARY KEY (id)
);

CREATE TABLE Role
(
    id      COUNTER,
    libelle VARCHAR(50),
    PRIMARY KEY (id)
);

CREATE TABLE Profil
(
    id      COUNTER,
    libelle VARCHAR(50),
    PRIMARY KEY (id)
);

CREATE TABLE Position_Stats
(
    id                     COUNTER,
    pgn                    TEXT NOT NULL,
    nb_victoires           INT,
    nb_defaites            INT,
    nb_nulles              INT,
    probabilite_coup       VARCHAR(50),
    id_position_precedente INT  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (id_position_precedente),
    FOREIGN KEY (id_position_precedente) REFERENCES Position_Stats (id)
);

CREATE TABLE Tournoi
(
    id              COUNTER,
    libelle         VARCHAR(50),
    code            VARCHAR(10),
    cree_a          DATETIME,
    modifie_a       DATETIME,
    Id_Organisateur INT NOT NULL,
    id_cadence      INT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (Id_Organisateur) REFERENCES Joueur (id),
    FOREIGN KEY (id_cadence) REFERENCES Cadence (id)
);

CREATE TABLE Ouverture
(
    id          COUNTER,
    code        VARCHAR(50)  NOT NULL,
    libelle     VARCHAR(150) NOT NULL,
    id_position INT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (code),
    FOREIGN KEY (id_position) REFERENCES Position_Stats (id)
);

CREATE TABLE Partie
(
    id               COUNTER,
    resultat         SMALLINT,
    pgn              TEXT,
    date_heure_utc   DATETIME,
    elo_blanc        SMALLINT,
    elo_noir         SMALLINT,
    score_blanc_diff SMALLINT,
    score_noir_diff  SMALLINT,
    titre_blanc      VARCHAR(5),
    titre_noir       VARCHAR(5),
    type_resultat    VARCHAR(50),
    round            SMALLINT,
    code             VARCHAR(10),
    id_ouverture     INT,
    id_cadence       INT,
    Id_Joueur_Blanc  INT,
    Id_Joueur_Noir   INT,
    id_tournoi       INT,
    PRIMARY KEY (id),
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
    inscription DATETIME,
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
