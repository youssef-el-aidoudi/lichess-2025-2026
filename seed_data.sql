-- Seed Data for ChessMate
-- Passwords are 'password' hashed with BCrypt
-- Hash: $2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOnu

-- 1. Insert Joueurs
INSERT INTO joueur (elo, nb_defaites, nb_nulles, nb_victoires, pseudonyme) 
VALUES (1200, 0, 0, 0, 'user1') RETURNING id;
-- Assume id 1

INSERT INTO joueur (elo, nb_defaites, nb_nulles, nb_victoires, pseudonyme) 
VALUES (1500, 2, 1, 10, 'grandmaster') RETURNING id;
-- Assume id 2

INSERT INTO joueur (elo, nb_defaites, nb_nulles, nb_victoires, pseudonyme) 
VALUES (800, 10, 0, 1, 'beginner') RETURNING id;
-- Assume id 3

-- 2. Insert Utilisateurs
-- Link user1 to joueur 1
INSERT INTO utilisateur (email, hash, id_joueur, inscription_a, nom, prenom, role) 
VALUES ('user1@gmail.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOnu', 1, NOW(), 'User', 'One', 'ROLE_USER');

-- Link grandmaster to joueur 2
INSERT INTO utilisateur (email, hash, id_joueur, inscription_a, nom, prenom, role) 
VALUES ('gm@gmail.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOnu', 2, NOW(), 'Grand', 'Master', 'ROLE_USER');

-- Link beginner to joueur 3
INSERT INTO utilisateur (email, hash, id_joueur, inscription_a, nom, prenom, role) 
VALUES ('beginner@gmail.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOnu', 3, NOW(), 'Begin', 'Ner', 'ROLE_USER');

-- 3. Insert Friendships
-- user1 and grandmaster are friends (ACCEPTED)
INSERT INTO friendship (id_joueur_from, id_joueur_to, status, created_at) 
VALUES (1, 2, 'ACCEPTED', NOW());

-- user1 has a pending request from beginner
INSERT INTO friendship (id_joueur_from, id_joueur_to, status, created_at) 
VALUES (3, 1, 'PENDING', NOW());
