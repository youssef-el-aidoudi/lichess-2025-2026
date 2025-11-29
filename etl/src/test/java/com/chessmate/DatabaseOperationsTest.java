
package com.chessmate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests des opérations base de données")
class DatabaseOperationsTest extends BaseIntegrationTest {

    @Test
    @DisplayName("copyJoueurs insère correctement les joueurs")
    void copyJoueursInsertsPlayers() throws SQLException, IOException {
        Map<String, JoueurDTO> joueurs = new HashMap<>();
        joueurs.put("Magnus", new JoueurDTO("Magnus", 2850, "Norway", "12345"));
        joueurs.put("Hikaru", new JoueurDTO("Hikaru", 2800, "USA", "67890"));

        Map<String, Integer> ids = copyJoueursH2(joueurs);

        assertThat(ids).hasSize(2);
        assertThat(ids).containsKey("Magnus");
        assertThat(ids).containsKey("Hikaru");

        connection.commit();
    }

    @Test
    @DisplayName("copyJoueurs gère les doublons (idempotence)")
    void copyJoueursIdempotency() throws SQLException, IOException {
        Map<String, JoueurDTO> joueurs = new HashMap<>();
        joueurs.put("Magnus", new JoueurDTO("Magnus", 2850, "Norway", "12345"));

        // first insertion
        Map<String, Integer> ids1 = copyJoueursH2(joueurs);
        connection.commit();

        // second insertion
        Map<String, Integer> ids2 = copyJoueursH2(joueurs);
        connection.commit();

        assertThat(ids1.get("Magnus")).isEqualTo(ids2.get("Magnus"));
    }

    @Test
    @DisplayName("copyTournois insère correctement les tournois")
    void copyTournoisInsertsTournaments() throws SQLException, IOException {
        Map<String, TournoiDTO> tournois = new HashMap<>();
        tournois.put("Tata Steel",
                new TournoiDTO("Tata Steel", "Wijk aan Zee", "url", java.sql.Date.valueOf("2024-01-01")));

        Map<String, Integer> ids = copyTournoisH2(tournois);
        connection.commit();

        assertThat(ids).hasSize(1);
        assertThat(ids).containsKey("Tata Steel");
    }

    @Test
    @DisplayName("copyParties insère correctement les parties")
    void copyPartiesInsertsGames() throws SQLException, IOException {
        // inserer joueurs et tournois
        Map<String, JoueurDTO> joueurs = new HashMap<>();
        joueurs.put("WhitePlayer", new JoueurDTO("WhitePlayer", 2000, null, null));
        joueurs.put("BlackPlayer", new JoueurDTO("BlackPlayer", 2000, null, null));
        Map<String, Integer> joueurIds = copyJoueursH2(joueurs);

        Map<String, TournoiDTO> tournois = new HashMap<>();
        tournois.put("Test Event", new TournoiDTO("Test Event", "Site", "url", null));
        Map<String, Integer> tournoiIds = copyTournoisH2(tournois);

        connection.commit();

        // creer parties
        List<Game> games = new ArrayList<>();
        Game g = createBasicGame("WhitePlayer", "BlackPlayer", "1-0");
        g.broadcastName = "Test Event";
        games.add(g);

        // copier les parties
        copyPartiesH2(games, joueurIds, tournoiIds);
        connection.commit();

        // verifier l'insertion
        try (var stmt = connection.createStatement();
                var rs = stmt.executeQuery("SELECT count(*) FROM Partie")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }
}
