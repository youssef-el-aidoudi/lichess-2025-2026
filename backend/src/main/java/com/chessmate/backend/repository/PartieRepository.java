package com.chessmate.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chessmate.backend.entiter.Partie;

public interface PartieRepository extends JpaRepository<Partie, Long>{
    Optional<Partie> findByTitle(String title);   // recherche par titre 

    @Query("SELECT p FROM Partie p WHERE p.dateHeureUTC = :dt")
    List<Partie> findAllPartiesByDateHeureUTC(@Param("dt") LocalDateTime dt);  // recherche par date creation
    
    @Query("SELECT p FROM Partie p WHERE p.typeResultat = :res")
    List<Partie> findAllPartiesByTypeResultat(@Param("res") String type_r);  // recherche par type de resultat

    @Query("SELECT COUNT(p) FROM Partie p WHERE p.joueurBlanc.id = :joueurId OR p.joueurNoir.id = :joueurId")
    Integer getNbPartiesByJoueur(@Param("joueurId") Long joueurId);

    @Query(value = """
    SELECT ROUND(
        (
            COUNT(*) FILTER (
                WHERE (p.id_joueur_blanc = :joueurId AND p.resultat = 2)
                   OR (p.id_joueur_noir = :joueurId AND p.resultat = 0)
            )::numeric
            / NULLIF(COUNT(*), 0)
        ) * 100
    )
    FROM partie p
    WHERE :joueurId IN (p.id_joueur_blanc, p.id_joueur_noir)
""", nativeQuery = true)
    Double getTauxVictoireJoueur(@Param("joueurId") Long joueurId);

    @Query(value = """
        SELECT 
            EXTRACT(YEAR FROM p.date_heure_utc) AS annee,
            EXTRACT(MONTH FROM p.date_heure_utc) AS mois_numero,
            TO_CHAR(p.date_heure_utc, 'FMMonth') AS mois,
            
            SUM(CASE 
                    WHEN (p.id_joueur_blanc = :joueurId AND p.resultat = 2) 
                    OR (p.id_joueur_noir = :joueurId AND p.resultat = 0) 
                    THEN 1 ELSE 0 
                END) AS nb_victoires,
                
            SUM(CASE 
                    WHEN (p.id_joueur_blanc = :joueurId AND p.resultat = 0) 
                    OR (p.id_joueur_noir = :joueurId AND p.resultat = 2) 
                    THEN 1 ELSE 0 
                END) AS nb_defaites,
                
            SUM(CASE 
                    WHEN p.resultat = 1 
                    AND :joueurId IN (p.id_joueur_blanc, p.id_joueur_noir) 
                    THEN 1 ELSE 0 
                END) AS nb_nulles

        FROM partie p
        WHERE :joueurId IN (p.id_joueur_blanc, p.id_joueur_noir)

        GROUP BY 
            annee,
            mois_numero,
            mois

        ORDER BY 
            annee,
            mois_numero
        """, nativeQuery = true)
        List<Object[]> getPerformanceMensuelle(@Param("joueurId") Long joueurId);
    
    @Query(value = """
        SELECT TO_CHAR(p.date_heure_utc, 'YYYY-MM-DD') AS dates,
           CASE 
               WHEN p.id_joueur_blanc = :joueurId THEN jNoir.pseudonyme
               ELSE jBlanc.pseudonyme
           END AS adversaire,
           CASE 
               WHEN p.id_joueur_blanc = :joueurId AND p.resultat = 2 THEN 'Victoire'
               WHEN p.id_joueur_noir = :joueurId AND p.resultat = 0 THEN 'Victoire'
               WHEN p.id_joueur_blanc = :joueurId AND p.resultat = 0 THEN 'Défaite'
               WHEN p.id_joueur_noir = :joueurId AND p.resultat = 2 THEN 'Défaite'
               ELSE 'Nulle'
           END AS resultat
        FROM partie p
        JOIN joueur jBlanc ON p.id_joueur_blanc = jBlanc.id
        JOIN joueur jNoir ON p.id_joueur_noir = jNoir.id
        WHERE :joueurId IN (p.id_joueur_blanc, p.id_joueur_noir)
        ORDER BY dates DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getLastParties(@Param("joueurId") Long joueurId, @Param("limit") int limit);

    @Query("""
        SELECT p FROM Partie p
        WHERE p.joueurBlanc.id = :joueurId
        OR p.joueurNoir.id = :joueurId
        """)
    List<Partie> findAllByJoueur(@Param("joueurId") Long joueurId);
    
    @Query("""
        SELECT o.libelle,
            COUNT(p),
            SUM(
                CASE 
                    WHEN (p.joueurBlanc.id = :joueurId AND p.resultat = 2)
                        OR (p.joueurNoir.id = :joueurId AND p.resultat = 0)
                    THEN 1 ELSE 0
                END
            ) * 100.0 / COUNT(p),
            COUNT(p) * 100.0 /
            (
                SELECT COUNT(p2)
                FROM Partie p2
                WHERE p2.joueurBlanc.id = :joueurId
                    OR p2.joueurNoir.id = :joueurId
            )
        FROM Partie p
        JOIN p.ouverture o
        WHERE p.joueurBlanc.id = :joueurId
        OR p.joueurNoir.id = :joueurId
        GROUP BY o.libelle
        HAVING COUNT(p) >= (
            SELECT CASE 
                    WHEN COUNT(p2) < 20 THEN 1          
                    ELSE COUNT(p2) * 0.01           
                END
            FROM Partie p2
            WHERE p2.joueurBlanc.id = :joueurId
            OR p2.joueurNoir.id = :joueurId
        )
        ORDER BY COUNT(p) DESC
        LIMIT :limit
        """)
    List<Object[]> getStatsOuvertures(@Param("joueurId") Long joueurId, @Param("limit") int limit);
}
