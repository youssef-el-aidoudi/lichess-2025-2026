package com.chessmate;

import java.io.*;
import java.sql.*;

public class ChessMateETL {

    /**
     * Point d’entrée principal du programme ETL ChessMate.
     *
     * Connection à la BD postgreSQL
     * Lit un fichier PGN, extrait les métadonnées des parties,
     * et insère les informations correspondantes (joueurs, tournois, parties, positions)
     * dans une base de données PostgreSQL.
     *
     * @param args arguments de ligne de commande (chemin du fichier PGN, etc.)
     */
    public static void main(String[] args) throws Exception {
        //connexion à la base, mots de passe à changer lol
        Connection connection = DriverManager.getConnection(
            "jdbc:postgresql://localhost:5432/chessmate", "chessmate", "AdleneTest");

        connection.setAutoCommit(false); // on commit nous même, pas de commit auto

        /**
         * PreparedStatement pour insérer ou mettre à jour un joueur.
         * Insère un joueur avec pseudonyme, elo, équipe et fide_id.
         * Si le pseudonyme existe déjà, met à jour elo, équipe et fide_id.
         * Retourne l'id du joueur inséré ou existant.
         */
        PreparedStatement psJoueur = connection.prepareStatement(
            "INSERT INTO Joueur (pseudonyme, elo, equipe, fide_id) VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (pseudonyme) DO UPDATE SET " +
            "elo = EXCLUDED.elo, equipe = EXCLUDED.equipe, fide_id = EXCLUDED.fide_id " +
            "RETURNING id");

        /**
         * PreparedStatement pour insérer un tournoi si celui-ci n'existe pas.
         * Insère libellé, site, broadcast_url, date_debut, Id_Organisateur et id_cadence.
         * Si le libellé existe déjà, ne fait rien (ON CONFLICT DO NOTHING).
         * Retourne l'id du tournoi inséré.
         */
        PreparedStatement psTournoi = connection.prepareStatement(
            "INSERT INTO Tournoi (libelle, site, broadcast_url, date_debut, Id_Organisateur, id_cadence) " +
            "VALUES (?, ?, ?, ?, 1, 1) " +
            "ON CONFLICT (libelle) DO NOTHING RETURNING id");


        /**
         * PreparedStatement pour vérifier l'existence d'un tournoi via son libellé.
         * Sélectionne l'id du tournoi correspondant au libellé donné.
         */
        PreparedStatement psCheckTournoi = connection.prepareStatement(
            "SELECT id FROM Tournoi WHERE libelle = ?");

        /**
         * PreparedStatement pour insérer ou mettre à jour une ouverture d'échecs.
         * Insère code, libellé et id_position.
         * Si le code existe déjà, met à jour uniquement le libellé.
         * Retourne l'id de l'ouverture insérée ou existante.
         */
        PreparedStatement psOuverture = connection.prepareStatement(
            "INSERT INTO Ouverture (code, libelle, id_position) VALUES (?, ?, ?) " +
            "ON CONFLICT (code) DO UPDATE SET libelle = EXCLUDED.libelle " +
            "RETURNING id");


        /**
         * PreparedStatement pour insérer une partie d'échecs.
         * Insère title, résultat, date et heure, elo des joueurs, titres, round,
         * URLs, variante, id de suite de coups, id des joueurs, id de l'ouverture,
         * id de cadence, id du tournoi et PGN.
         */
        PreparedStatement psPartie = connection.prepareStatement(
            "INSERT INTO Partie (title, resultat, date_heure_utc, elo_blanc, elo_noir, " +
            "titre_blanc, titre_noir, round, broadcast_url, game_url, variant, id_suite_coups_stats, " +
            "Id_Joueur_Blanc, Id_Joueur_Noir, id_ouverture, id_cadence, id_tournoi, pgn) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        //lecture du fichier pgn
        BufferedReader reader = null;
        String filePath = "../data/lichess_db_broadcast_2025-09.pgn";
        try {
            System.out.println("Lecture du fichier...");
            reader = new BufferedReader(new FileReader(filePath));
            System.out.println("OK : fichier trouvé !");
        } catch ( IOException  e) {
            System.err.println("Erreur lors de l'accès ou de la lecture du fichier : " + filePath);
            e.printStackTrace();
            return; // arrête le programme si une erreur IO survient
        }

        //déclaration variables utiles pour la boucle principale
        String ligne;
        Game gameActuelle = new Game(); //initialisation de la game
;
        int cptGamesTmp = 0;
        int totalGames = 0;
        int skippedGames = 0;

        /**
         * BOUCLE PRINCIPALE
         * Lit un fichier PGN ligne par ligne, parse les informations des parties et insère les données dans la base.
         *
         * Pour chaque ligne du fichier :
         * - Si la ligne commence par '[', elle contient des métadonnées (nom des joueurs, date, résultat, etc.)
         *   et est parsée via la méthode parseMetadata.
         * - Sinon, si la ligne n'est pas vide et ne commence pas par '[', elle contient les coups de la partie
         *   et est ajoutée à l'objet Game courant.
         * - Si la ligne est vide et qu'une partie est en cours (white non null), cela signifie la fin de la partie.
         *   Dans ce cas :
         *   - On récupère l'ID de la cadence correspondante.
         *   - On applique un filtre pour décider si la partie doit être insérée.
         *   - Les joueurs sont insérés ou récupérés dans la base et leurs ID sont récupérés.
         *   - Le tournoi est inséré si nécessaire et l'ID est récupéré.
         *   - La suite des coups est récupérée ou créée dans la base.
         *   - L'ouverture est insérée si disponible et l'ID est récupéré.
         *   - La partie est enfin insérée dans la table Partie.
         *   - Un commit est fait tous les 100 parties pour optimiser les performances.
         * - Les parties qui ne passent pas le filtre sont ignorées.
         *
         * À la fin de la lecture :
         * - Un commit final est effectué pour les parties restantes.
         * - Le reader est fermé.
         * - La connexion à la base est fermée.
         *
         * Les variables de suivi :
         * - totalGames : nombre total de parties insérées.
         * - skippedGames : nombre de parties ignorées.
         * - cptGamesTmp : compteur temporaire pour commits par lot.
         *
         * Gestion des erreurs :
         * - En cas d'exception lors de l'insertion d'une partie, un rollback est effectué pour cette transaction
         *   et un message d'erreur est affiché.
         */
        while ((ligne = reader.readLine()) != null) {
            if (ligne.startsWith("[")) { //chaque ligne commence par un "[" et se termine par un "]"
                parseMetadata(ligne, gameActuelle); //parse les metadatas genre white, black, date etc

            //sinon si la ligne commence pas par "[", ça sera la liste des coups joués
            } else if (!ligne.trim().isEmpty() && !ligne.startsWith("[")) {
                gameActuelle.moves.append(ligne).append("\n"); // stock les coups

            //vérification si partie terminée->next partie (ligne vide, mais joueur white toujours stocké par ex)
            } else if (ligne.trim().isEmpty() && gameActuelle.white != null) {

                //recherche & recup idCadence
                Integer idCadence = getIdCadence(connection, gameActuelle.timeControl);
                //si la partie convient nos filtres
                if (filter(gameActuelle) && idCadence != null) { //ffiltre a choisir
                    try {
                        //insert les joueurs et recup id
                        int idBlanc = insererJoueur(psJoueur, gameActuelle.white, gameActuelle.whiteElo,
                                                     gameActuelle.whiteTeam, gameActuelle.whiteFideId);
                        int idNoir = insererJoueur(psJoueur, gameActuelle.black, gameActuelle.blackElo,
                                                    gameActuelle.blackTeam, gameActuelle.blackFideId);

                        //insertion tournoi && recup idTournoi
                        Integer idTournoi = null;
                        if (gameActuelle.broadcastName != null) {
                            idTournoi = insererTournoi(psTournoi, psCheckTournoi, gameActuelle);
                        }

                        // Récupérer l'ID de Suite_Coups_Stats AVANT l'ouverture
                        Integer idSuiteCoups = getIdSuiteCoups(connection, gameActuelle.moves.toString());

                        //insertion ouverture && recup idOuverture
                        Integer idOuverture = null;
                        if (gameActuelle.eco != null) {
                            idOuverture = insererOuverture(psOuverture, gameActuelle.eco, gameActuelle.opening, idSuiteCoups);
                        }


                        //insertion de la partie dans la bd
                        insererPartie(psPartie, gameActuelle, idBlanc, idNoir, idOuverture, idCadence, idTournoi, idSuiteCoups);

                        totalGames++; //nombre total de parties insérées
                        cptGamesTmp++; //nombre temporaire de parties inserées (jusqu'a 100)

                        if (cptGamesTmp >= 100) { //commit tous les 100 parties
                            connection.commit();
                            cptGamesTmp = 0;
                            System.out.println("parties insérées : " + totalGames + " (ignorées : " + skippedGames + ")");
                        }
                    } catch (Exception e) {
                        System.err.println("erreur pour : " + gameActuelle.white + " vs " + gameActuelle.black);
                        e.printStackTrace();
                        connection.rollback(); //rollback si pb
                    }
                } else {
                    skippedGames++;
                }
                gameActuelle=new Game();
            }
        }

        if (cptGamesTmp > 0) { //commit final
            connection.commit();
        }

        reader.close();
        System.out.println("terminé ! total : " + totalGames + " parties insérées, " + skippedGames + " ignorées");
        connection.close();
    }




    /**
     * Analyse une ligne de métadonnées d'un fichier PGN et met à jour les champs correspondants de l'objet {@link Game}.
     *
     * Exemple de ligne PGN : [White "MagnusCarlsen"]
     *
     * Étapes :
     *  - Extrait la clé ("White") et la valeur ("MagnusCarlsen")
     *  - Met à jour le bon attribut de l'objet Game selon la clé
     *
     * @param line Ligne extraite du fichier PGN (doit commencer par un crochet ouvrant '[')
     * @param game Instance de Game dans laquelle les informations doivent être enregistrées
     */
    static void parseMetadata(String line, Game game) {
        //trouve la position de la fin de le clé, dès qu'on trouve un espace
        int finCle = line.indexOf(" ");
        //trouve la position de début et fin de "valeur" entre guillemets
        int valStart = line.indexOf("\"") + 1;
        int valEnd = line.lastIndexOf("\"");

        //ignorer les cas non complets
        if (finCle == -1 || valStart == -1 || valEnd == -1) return;

        String key = line.substring(1, finCle);  //recuperation cle
        String val = line.substring(valStart, valEnd);      //recuperation valeur

        switch (key) { //switch case pour remplir les infos de la game
            //infos sur la game
            case "Event": game.event = val; break;
            case "Date": game.date = val; break;
            case "Round": game.round = val; break;
            case "White": game.white = val; break;
            case "Black": game.black = val; break;
            case "Result": game.result = val; break;
            case "Variant": game.variant = val; break;
            case "ECO": game.eco = val; break;
            case "Opening": game.opening = val; break;
            case "TimeControl": game.timeControl = val; break;

            //infos sur joueur blanc
            case "WhiteElo": game.whiteElo = parseInt(val); break;
            case "WhiteTitle": game.whiteTitle = val; break;
            case "WhiteTeam": game.whiteTeam = val; break;
            case "WhiteFideId": game.whiteFideId = val; break;

            //infos sur joueur noir
            case "BlackElo": game.blackElo = parseInt(val); break;
            case "BlackTitle": game.blackTitle = val; break;
            case "BlackTeam": game.blackTeam = val; break;
            case "BlackFideId": game.blackFideId = val; break;

            //liens et datage
            case "UTCDate": game.utcDate = val; break;
            case "UTCTime": game.utcTime = val; break;
            case "BroadcastName": game.broadcastName = val; break;
            case "BroadcastURL": game.broadcastUrl = val; break;
            case "GameURL": game.gameUrl = val; break;
        }
    }


    /**
     * Filtre les parties non valides selon certains critères de cohérence.
     *
     * Une partie est considérée valide si :
     *  - Les deux joueurs ont un ELO défini et supérieur ou égal à 800
     *  - Le résultat est renseigné et différent de "*"
     *
     * @param g L'objet {@link Game} représentant une partie
     * @return true si la partie passe le filtre, false sinon
     */
    static boolean filter(Game g) {

        return g.whiteElo != null && g.blackElo != null
            && g.whiteElo >= 800 && g.blackElo >= 800
            && g.result != null && !g.result.equals("*");
    }




    /**
     * Convertit une chaîne en entier tout en gérant les exceptions.
     * Si la conversion échoue, retourne {@code null} au lieu de lever une erreur.
     *
     * @param s La chaîne à convertir en entier.
     * @return Un objet {@link Integer} correspondant à la valeur entière de {@code s}, ou {@code null} en cas d’erreur.
     */
    static Integer parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }





    /**
     * Insère un joueur dans la base de données ou met à jour ses informations s'il existe déjà.
     * Utilise la clause SQL "ON CONFLICT" pour éviter les doublons sur le pseudonyme.
     *
     * @param ps       PreparedStatement configuré pour insérer ou mettre à jour un joueur
     * @param nom      pseudonyme du joueur
     * @param elo      classement Elo du joueur (nullable)
     * @param equipe   nom de l’équipe du joueur (peut être null)
     * @param fideId   identifiant FIDE du joueur (peut être null)
     * @return         l'identifiant unique (id) du joueur inséré ou mis à jour
     * @throws SQLException si une erreur SQL survient lors de l’exécution
     */
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





    /**
     * Insère un tournoi dans la base de données s'il n'existe pas déjà.
     * Vérifie l’existence du tournoi à partir de son libellé avant insertion.
     *
     * @param psInsert  PreparedStatement d’insertion du tournoi
     * @param psCheck   PreparedStatement de vérification de l’existence du tournoi
     * @param game         objet Game contenant les métadonnées du tournoi
     * @return          l’identifiant du tournoi inséré ou déjà existant, sinon null
     * @throws SQLException si une erreur SQL survient lors de l’insertion ou de la vérification
     */
    static Integer insererTournoi(PreparedStatement psInsert, PreparedStatement psCheck,
                                  Game game) throws SQLException {
        String libelle = truncate(game.broadcastName, 255); //clean les names hyper longs


        //vérification de l'existence du tournoi
        psCheck.setString(1, libelle);
        ResultSet rs = psCheck.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }

        //insertion sinon
        psInsert.setString(1, libelle);
        psInsert.setString(2, game.site);
        psInsert.setString(3, game.broadcastUrl);
        psInsert.setDate(4, parseDate(game.utcDate));

        ResultSet rsInsert = psInsert.executeQuery();
        if (rsInsert.next()) {
            return rsInsert.getInt("id");
        }
        return null;
    }




    /**
     * Insère une ouverture dans la base de données ou met à jour son libellé si le code existe déjà.
     * Utilise un PreparedStatement pour exécuter l'opération et récupère l'ID généré.
     *
     * @param ps        Le {@link PreparedStatement} pré-configuré pour l'insertion de l'ouverture.
     * @param eco       Le code ECO de l'ouverture (ex : "C20"), sera tronqué à 50 caractères si nécessaire.
     * @param opening   Le nom de l'ouverture (ex : "King's Pawn Game"), sera tronqué à 150 caractères si nécessaire.
     *                  Si null, la valeur "Unknown" sera utilisée.
     * @param idSuiteCoups L'ID de la position correspondante dans la table {@code Suite_Coups_Stats}.
     * @return L'identifiant ({@link Integer}) de l'ouverture insérée ou mise à jour.
     * @throws SQLException Si une erreur SQL survient lors de l'exécution du statement.
     */
    static Integer insererOuverture(PreparedStatement ps, String eco, String opening,
                                    Integer idSuiteCoups) throws SQLException {
        ps.setString(1, truncate(eco, 3));
        ps.setString(2, opening != null ? truncate(opening, 150) : "Unknown");
        ps.setObject(3, idSuiteCoups);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt("id");
    }





    /**
     * Recherche ou insère une séquence de coups (suite de coups PGN) dans la table Suite_Coups_Stats.
     *
     * Cette méthode s'assure qu'une séquence donnée de coups n'est enregistrée qu'une seule fois :
     *  - Si la séquence existe déjà (même PGN), on récupère son identifiant.
     *  - Sinon, on insère une nouvelle entrée et on renvoie le nouvel ID.
     *
     * @param connection Connexion active à la base de données PostgreSQL
     * @param pgn Chaîne PGN complète de la partie, contenant les coups
     * @return L'identifiant de la séquence dans la table Suite_Coups_Stats
     * @throws SQLException En cas d'erreur SQL (connexion ou requête)
     */
    static Integer getIdSuiteCoups(Connection connection, String pgn) throws SQLException {
        String pgnNettoye = nettoyerPgnCoups(pgn); //PGN ou y a que les coups propres (pour SuiteCoupsStats)
        //verification si la position existe déja
        PreparedStatement ps = connection.prepareStatement(
            "SELECT id FROM Suite_Coups_Stats WHERE pgn = ?"
        );
        ps.setString(1, pgnNettoye);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("id");


        //sinon insertion
        ps = connection.prepareStatement(
            "INSERT INTO Suite_Coups_Stats (pgn, id_suite_coups_stats_precedente) VALUES (?, NULL) RETURNING id"
        );
        ps.setString(1, pgnNettoye);
        rs = ps.executeQuery();
        rs.next();
        return rs.getInt("id");
    }

    /**
     * Récupère l'identifiant d'une cadence (temps + incrément) à partir du champ TimeControl d'une partie PGN.
     *
     * Cette méthode tente de faire correspondre la valeur TimeControl (ex: "90+30") à une cadence
     * existante dans la base (table Cadence). Si aucune correspondance n'est trouvée, la méthode renvoie null.
     *
     * Exemple :
     *   "90+30"  →  recherche Cadence.libelle = "90+30"
     *
     * @param conn Connexion active à la base de données PostgreSQL
     * @param timeControl Valeur brute du champ TimeControl extraite du PGN
     * @return L'identifiant de la cadence correspondante, ou null si aucune trouvée
     * @throws SQLException En cas d'erreur SQL
     */
    static Integer getIdCadence(Connection conn, String timeControl) throws SQLException {
        if (timeControl == null) return null;

        //parse le format "temps+increment" (ex: "90+30", "10+0")
        String cleanTimeControl = normalizeCadence(timeControl);


        //requete pour trouver la cadence dans la BD
        PreparedStatement ps = conn.prepareStatement(
            "SELECT id FROM Cadence WHERE libelle = ?"
        );
        ps.setString(1, cleanTimeControl);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) { //si on trouve la cadence, return id
            return rs.getInt("id");
        } else {
            return null;
        }

    }



    /**
     * Nettoie un texte PGN brut pour ne conserver que la séquence de coups pure.
     *
     * Cette méthode retire tout ce qui n’est pas directement lié aux coups :
     *  - Les commentaires entre accolades { ... }
     *  - Les annotations (!, ?, !!, ?!, etc.)
     *  - Les notations de coups noirs ("...")
     *  - Les résultats de partie (1-0, 0-1, 1/2-1/2, *)
     *  - Les espaces superflus
     *
     * Exemple :
     *   Entrée : "1. e4 {Coup classique} e5 2. Nf3 Nc6 1-0"
     *   Sortie : "1. e4 e5 2. Nf3 Nc6"
     *
     * @param raw Chaîne PGN brute extraite du fichier
     * @return Le PGN nettoyé, contenant uniquement les coups, ou null si l’entrée est null
     */
    static String nettoyerPgnCoups(String raw) {
    if (raw == null) return null;

    String nettoye = raw;

    //supprimer les annotations de temps ou commentaires { ... }
    nettoye = nettoye.replaceAll("\\{[^}]*\\}", "");

    //supprimer les évaluations de coups (?!, !!, ?, !, etc.)
    nettoye = nettoye.replaceAll("[!?]+", "");

    //supprimer les notations de coups noirs avec "..."
    nettoye = nettoye.replaceAll("\\d+\\.\\.\\.", "");

    //supprimer le résultat final (1-0, 0-1, 1/2-1/2, *)
    nettoye = nettoye.replaceAll("(1-0|0-1|1/2-1/2|\\*)", "");

    //normaliser les espaces
    nettoye = nettoye.replaceAll("\\s+", " ").trim();

    return nettoye;
}




    /**
     * Insère une partie dans la base de données via un {@link PreparedStatement} déjà préparé.
     *
     * Cette méthode mappe les champs de l'objet {@link Game} aux colonnes de la table Partie :
     *  - Convertit le résultat textuel ("1-0", "0-1", "1/2-1/2") en entier
     *  - Convertit les dates/horaires PGN en {@link Timestamp}
     *  - Nettoie et tronque les chaînes si nécessaire
     *
     * @param ps Requête SQL préparée d’insertion dans la table Partie
     * @param game Objet contenant toutes les informations d’une partie PGN
     * @param idBlanc Identifiant du joueur blanc
     * @param idNoir Identifiant du joueur noir
     * @param idOuverture Identifiant de l’ouverture (ou null si inconnue)
     * @param idCadence Identifiant de la cadence (ou null si inconnue)
     * @param idTournoi Identifiant du tournoi (ou null si inconnu)
     * @param idSuiteCoups Identifiant de la séquence de coups
     * @throws SQLException En cas d’erreur lors de l’insertion SQL
     */
    static void insererPartie(PreparedStatement ps, Game game, int idBlanc, int idNoir,
                              Integer idOuverture, Integer idCadence, Integer idTournoi,
                              Integer idSuiteCoups) throws SQLException {
        int resultat = game.result.equals("1-0") ? 2 : game.result.equals("0-1") ? 0 : 1;

        int idx = 1;
        ps.setString(idx++, truncate(game.event, 255)); //titre
        ps.setInt(idx++, resultat);
        ps.setTimestamp(idx++, parseTimestamp(game.utcDate, game.utcTime));
        ps.setObject(idx++, game.whiteElo);
        ps.setObject(idx++, game.blackElo);
        ps.setString(idx++, game.whiteTitle);
        ps.setString(idx++, game.blackTitle);
        ps.setString(idx++, game.round);
        ps.setString(idx++, game.broadcastUrl);
        ps.setString(idx++, game.gameUrl);
        ps.setString(idx++, game.variant);
        ps.setObject(idx++, idSuiteCoups);
        ps.setInt(idx++, idBlanc);
        ps.setInt(idx++, idNoir);
        ps.setObject(idx++, idOuverture);
        ps.setObject(idx++, idCadence);
        ps.setObject(idx++, idTournoi);
        ps.setString(idx++, cleanPgn(game.moves.toString())); // PGN complet pour Partie

        ps.executeUpdate();
    }








    /**
     * Convertit une date au format PGN (ex: "2024.09.18") en objet SQL Date.
     *
     * Si le format est invalide ou absent, la méthode renvoie null.
     *
     * @param dateStr  chaîne de date PGN
     * @return         java.sql.Date correspondante, ou null en cas d’erreur de parsing
     */
    static Date parseDate(String s) {
        if (s == null) return null;
        try {
            return Date.valueOf(s.replace('.', '-'));
        } catch (Exception e) {
            return null;
        }
    }





    /**
     * Convertit une date et une heure sous forme de chaînes en un objet {@link Timestamp}.
     * Si l'heure est nulle, elle est remplacée par "00:00:00".
     * Le format de date attendu est "YYYY.MM.DD" ou "YYYY-MM-DD".
     *
     * @param date La date au format chaîne (ex. "2025.10.30").
     * @param time L'heure au format chaîne (ex. "14:30:00"), ou null pour minuit.
     * @return Un {@link Timestamp} représentant la date et l'heure combinées, ou null si la conversion échoue.
     */
    static Timestamp parseTimestamp(String date, String time) {
        if (date == null) return null;
        try {
            String datetime = date.replace('.', '-') + " " + (time != null ? time : "00:00:00");
            return Timestamp.valueOf(datetime);
        } catch (Exception e) {
            return null;
        }
    }




    /**
     * Tronque une chaîne de caractères à une longueur maximale spécifiée.
     *
     * Utile pour éviter les erreurs SQL sur des champs trop longs
     * (par exemple, les noms de tournois ou les URLs très longues).
     *
     * @param s     chaîne d’entrée
     * @param max   longueur maximale autorisée
     * @return      chaîne tronquée à la longueur max, ou null si l’entrée était null
     */
    static String truncate(String s, int maxLength) {
        if (s == null) return null;
        return s.length() > maxLength ? s.substring(0, maxLength) : s;
    }




    /**
     * Nettoie une chaîne PGN (Portable Game Notation) en remplaçant tous les espaces
     * multiples et retours à la ligne par un seul espace, et en supprimant les espaces
     * en début et fin de chaîne.
     *
     * @param rawPGN La chaîne PGN brute à nettoyer.
     * @return La chaîne PGN nettoyée, ou null si l'entrée est nulle.
     */
    static String cleanPgn(String rawPGN) {
    if (rawPGN == null) return null;
    return rawPGN.replaceAll("\\s+", " ").trim();
}



    /**
     * Normalise une chaîne représentant la cadence d'une partie (TimeControl)
     * en supprimant les caractères non numériques et en gardant uniquement
     * les chiffres et le signe '+' entre le temps principal et l'incrément.
     *
     * Exemple d'utilisation :
     *  - "90minutes + 30 seconds"  -> "90+30"
     *  - "3 min + 2s"              -> "3+2"
     *  - "10+0"                    -> "10+0"
     *
     * Cette méthode est utilisée avant la recherche d'une cadence
     * dans la base de données, afin de rendre les valeurs cohérentes
     * avec les formats stockés dans la table Cadence.
     *
     * @param raw  Chaîne brute issue du PGN (champ TimeControl)
     * @return     Chaîne normalisée au format "temps+incrément",
     *             ou null si l'entrée est nulle.
     */
    static String normalizeCadence(String rawCadence) {
        if (rawCadence == null) return null;
        rawCadence = rawCadence.toLowerCase().replaceAll("[^0-9+]", "");
        return rawCadence; // ex: "90minutes + 30 seconds" → "90+30"
    }
}
