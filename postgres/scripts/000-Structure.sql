CREATE TABLE IF NOT EXISTS Joueur
(
    id
    SERIAL,
    pseudonyme
    VARCHAR
(
    50
) NOT NULL,
    elo SMALLINT,
    nb_victoires INT DEFAULT 0,
    nb_defaites INT DEFAULT 0,
    nb_nulles INT DEFAULT 0,
    equipe VARCHAR
(
    100
),
    fide_id VARCHAR
(
    20
),
    PRIMARY KEY
(
    id
),
    UNIQUE
(
    pseudonyme
)
    );

CREATE TABLE IF NOT EXISTS Utilisateur
(
    id
    SERIAL,
    email
    VARCHAR
(
    150
) NOT NULL,
    hash VARCHAR
(
    255
) NOT NULL,
    nom VARCHAR
(
    50
),
    prenom VARCHAR
(
    50
),
    inscription_a TIMESTAMP NOT NULL,
    connexion_a TIMESTAMP,
    banni_a TIMESTAMP,
    id_joueur INT NOT NULL,
    PRIMARY KEY
(
    id
),
    UNIQUE
(
    id_joueur
),
    UNIQUE
(
    email
),
    FOREIGN KEY
(
    id_joueur
) REFERENCES Joueur
(
    id
)
    );

CREATE TYPE CATEGORIE_CADENCE AS ENUM('Bullet','Blitz','Rapide','Classique');
CREATE TABLE IF NOT EXISTS Cadence
(
    id
    SERIAL,
    libelle
    VARCHAR
(
    50
) NOT NULL,
    temps SMALLINT,
    increment SMALLINT,
    type_partie CATEGORIE_CADENCE NOT NULL,
    PRIMARY KEY
(
    id
),
    UNIQUE
(
    libelle
)
    );

CREATE TABLE IF NOT EXISTS Role
(
    id
    SERIAL,
    libelle
    VARCHAR
(
    50
),
    PRIMARY KEY
(
    id
)
    );

CREATE TABLE IF NOT EXISTS Profil
(
    id
    SERIAL,
    libelle
    VARCHAR
(
    50
),
    PRIMARY KEY
(
    id
)
    );

CREATE
EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS Suite_Coups_Stats
(
    id
    SERIAL,
    pgn
    TEXT
    NOT
    NULL,
    pgn_hash
    CHAR
(
    64
) GENERATED ALWAYS AS
(
    encode(
    digest
(
    pgn,
    'sha256'
), 'hex')) STORED,
    nb_victoires INT DEFAULT 0,
    nb_defaites INT DEFAULT 0,
    nb_nulles INT DEFAULT 0,
    probabilite_coup VARCHAR
(
    50
),
    id_suite_coups_stats_precedente INT,
    PRIMARY KEY
(
    id
),
    FOREIGN KEY
(
    id_suite_coups_stats_precedente
) REFERENCES Suite_Coups_Stats
(
    id
),
    UNIQUE
(
    pgn_hash
)
    );

CREATE TABLE Position_Stats
(
    hash               BIGINT PRIMARY KEY,
    fen                TEXT        NOT NULL, -- utile en cas de collision
    nb_total           BIGINT      NOT NULL,
    nb_victoires_blanc BIGINT      NOT NULL,
    nb_victoires_noir  BIGINT      NOT NULL,
    nb_nulles          BIGINT      NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS Tournoi
(
    id
    SERIAL,
    libelle
    VARCHAR
(
    255
),
    code VARCHAR
(
    10
),
    site TEXT,
    broadcast_url TEXT,
    date_debut DATE,
    cree_a TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modifie_a TIMESTAMP,
    Id_Organisateur INT NOT NULL,
    id_cadence INT NOT NULL,
    PRIMARY KEY
(
    id
),
    UNIQUE
(
    libelle
),
    FOREIGN KEY
(
    Id_Organisateur
) REFERENCES Joueur
(
    id
),
    FOREIGN KEY
(
    id_cadence
) REFERENCES Cadence
(
    id
)
    );

CREATE TABLE IF NOT EXISTS Ouverture
(
    id
    SERIAL,
    code
    VARCHAR
(
    50
) NOT NULL,
    libelle VARCHAR
(
    150
) NOT NULL,
    id_suite_coups_stats INT,
    PRIMARY KEY
(
    id
),
    FOREIGN KEY
(
    id_suite_coups_stats
) REFERENCES Suite_Coups_Stats
(
    id
)
    );

CREATE TABLE IF NOT EXISTS Partie
(
    title
    VARCHAR
(
    255
),
    id SERIAL,
    resultat SMALLINT,
    pgn TEXT,
    date_heure_utc TIMESTAMP,
    elo_blanc SMALLINT,
    elo_noir SMALLINT,
    score_blanc_diff SMALLINT,
    score_noir_diff SMALLINT,
    titre_blanc VARCHAR
(
    5
),
    titre_noir VARCHAR
(
    5
),
    type_resultat VARCHAR
(
    50
),
    round VARCHAR
(
    20
),
    broadcast_url TEXT,
    game_url TEXT,
    variant VARCHAR
(
    50
),
    id_suite_coups_stats INT,
    hash_position_stats INT,
    id_ouverture INT,
    id_cadence INT,
    Id_Joueur_Blanc INT NOT NULL,
    Id_Joueur_Noir INT NOT NULL,
    id_tournoi INT,
    PRIMARY KEY
(
    id
),
    FOREIGN KEY
(
    id_suite_coups_stats
) REFERENCES Suite_Coups_Stats
(
    id
),
    FOREIGN KEY
(
    hash_position_stats
) REFERENCES Position_Stats
(
    hash
),
    FOREIGN KEY
(
    id_ouverture
) REFERENCES Ouverture
(
    id
),
    FOREIGN KEY
(
    id_cadence
) REFERENCES Cadence
(
    id
),
    FOREIGN KEY
(
    Id_Joueur_Blanc
) REFERENCES Joueur
(
    id
),
    FOREIGN KEY
(
    Id_Joueur_Noir
) REFERENCES Joueur
(
    id
),
    FOREIGN KEY
(
    id_tournoi
) REFERENCES Tournoi
(
    id
)
    );

CREATE TABLE IF NOT EXISTS participer
(
    id_joueur
    INT,
    id_tournoi
    INT,
    inscription
    TIMESTAMP,
    PRIMARY
    KEY
(
    id_joueur,
    id_tournoi
),
    FOREIGN KEY
(
    id_joueur
) REFERENCES Joueur
(
    id
),
    FOREIGN KEY
(
    id_tournoi
) REFERENCES Tournoi
(
    id
)
    );

CREATE TABLE IF NOT EXISTS Utilisateur_Role
(
    id_utilisateur
    INT,
    id_role
    INT,
    PRIMARY
    KEY
(
    id_utilisateur,
    id_role
),
    FOREIGN KEY
(
    id_utilisateur
) REFERENCES Utilisateur
(
    id
),
    FOREIGN KEY
(
    id_role
) REFERENCES Role
(
    id
)
    );

CREATE TABLE IF NOT EXISTS Utilisateur_Profil
(
    id_utilisateur
    INT,
    id_profil
    INT,
    PRIMARY
    KEY
(
    id_utilisateur,
    id_profil
),
    FOREIGN KEY
(
    id_utilisateur
) REFERENCES Utilisateur
(
    id
),
    FOREIGN KEY
(
    id_profil
) REFERENCES Profil
(
    id
)
    );

CREATE TABLE IF NOT EXISTS Profil_Role
(
    id_role
    INT,
    id_profil
    INT,
    PRIMARY
    KEY
(
    id_role,
    id_profil
),
    FOREIGN KEY
(
    id_role
) REFERENCES Role
(
    id
),
    FOREIGN KEY
(
    id_profil
) REFERENCES Profil
(
    id
)
    );

CREATE TABLE Tache
(
    tache_id        SERIAL PRIMARY KEY,

    debut_partie_id INTEGER NOT NULL,
    fin_partie_id   INTEGER NOT NULL,

    statut          VARCHAR(20) DEFAULT 'pending'
        CHECK (status IN ('pending', 'running', 'completed', 'failed')),

    worker_rank     INTEGER,
    debut_a         TIMESTAMP,
    fin_at          TIMESTAMP,

    tentative       INTEGER     DEFAULT 0,
    message_erreur  TEXT,
    cree_a          TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tache_statut_tentative
    ON tasks (statut, tentative_compteur) WHERE statut IN ('pending', 'failed');

CREATE INDEX idx_tache_worker
    ON tasks (worker_rank, statut);