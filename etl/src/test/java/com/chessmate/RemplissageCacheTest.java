package com.chessmate;

import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour remplissageCache()
 */
public class RemplissageCacheTest extends BaseIntegrationTest {

    @Test
    @DisplayName("remplissageCache() charge correctement les cadences")
    public void testRemplissageCache_Cadences() throws SQLException {
        ChessMateETL.remplissageCache(connection);

        assertEquals(3, ChessMateETL.cadences.size(), "Devrait charger 3 cadences");
        assertEquals(1, ChessMateETL.cadences.get("180+2"));
        assertEquals(2, ChessMateETL.cadences.get("300+0"));
        assertEquals(3, ChessMateETL.cadences.get("600+5"));
    }

    @Test
    @DisplayName("remplissageCache() charge correctement les ouvertures")
    public void testRemplissageCache_Ouvertures() throws SQLException {
        ChessMateETL.remplissageCache(connection);

        assertEquals(3, ChessMateETL.ouvertures.size(), "Devrait charger 3 ouvertures");
        assertEquals(1, ChessMateETL.ouvertures.get("C42|Petrov Defense"));
        assertEquals(2, ChessMateETL.ouvertures.get("E60|King's Indian Defense"));
        assertEquals(3, ChessMateETL.ouvertures.get("C65|Ruy Lopez, Berlin Defense"));
    }

    @Test
    @DisplayName("remplissageCache() fonctionne avec des tables vides")
    public void testRemplissageCache_TablesVides() throws SQLException {
        // Vider les tables de référence
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM Ouverture");
            stmt.execute("DELETE FROM Cadence");
            connection.commit();
        }

        ChessMateETL.remplissageCache(connection);

        assertEquals(0, ChessMateETL.cadences.size());
        assertEquals(0, ChessMateETL.ouvertures.size());
    }
}