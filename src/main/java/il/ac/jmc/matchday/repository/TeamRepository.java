package il.ac.jmc.matchday.repository;

import il.ac.jmc.matchday.dto.TeamSummary;
import il.ac.jmc.matchday.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link il.ac.jmc.matchday.entity.Team} entities.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * Returns {@code true} if a team with the given name is already stored (case-insensitive).
     *
     * @param name the team name to check
     * @return true if a team exists with the given name, false otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Returns {@code true} if another team (different id) already has the given name (case-insensitive).
     *
     * @param name the team name to check
     * @param id   the ID of the team to exclude from check
     * @return true if another team exists with the given name, false otherwise
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Returns all teams sorted alphabetically.
     *
     * @return a list of all teams sorted by name
     */
    List<Team> findAllByOrderByNameAsc();

    /**
     * Filters teams by optional name/description text and optional league membership,
     * returning one page of summaries with player count, sorted alphabetically.
     * The countQuery uses COUNT(DISTINCT t.id) to account for the GROUP BY in the main query.
     *
     * @param name              the text search query for team name or description
     * @param nameFilterEmpty   true to ignore the name filter
     * @param leagueIds         a list of league IDs to filter by
     * @param leagueFilterEmpty true to ignore the league filter
     * @param pageable          pagination details
     * @return a page of team summaries
     */
    @Query(value = """
            SELECT t.id AS id, t.name AS name, t.logoUrl AS logoUrl,
                   t.description AS description, COUNT(p) AS playerCount
            FROM Team t LEFT JOIN Player p ON p.team = t
            WHERE (:nameFilterEmpty = TRUE
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
                   OR LOWER(t.description) LIKE LOWER(CONCAT('%', :name, '%')))
            AND (:leagueFilterEmpty = TRUE OR EXISTS (
                SELECT 1 FROM LeagueTeam lt WHERE lt.team = t AND lt.league.id IN :leagueIds
            ))
            GROUP BY t.id, t.name, t.logoUrl, t.description
            ORDER BY t.name ASC
            """,
           countQuery = """
            SELECT COUNT(DISTINCT t.id)
            FROM Team t
            WHERE (:nameFilterEmpty = TRUE
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
                   OR LOWER(t.description) LIKE LOWER(CONCAT('%', :name, '%')))
            AND (:leagueFilterEmpty = TRUE OR EXISTS (
                SELECT 1 FROM LeagueTeam lt WHERE lt.team = t AND lt.league.id IN :leagueIds
            ))
            """)
    Page<TeamSummary> findSummariesPageable(@Param("name")              String name,
                                             @Param("nameFilterEmpty")   boolean nameFilterEmpty,
                                             @Param("leagueIds")         List<Long> leagueIds,
                                             @Param("leagueFilterEmpty") boolean leagueFilterEmpty,
                                             Pageable pageable);

    /**
     * Counts teams that do not yet have a logo URL.
     *
     * @return the number of teams missing logo URLs
     */
    @Query("""
            SELECT COUNT(t)
            FROM Team t
            WHERE t.logoUrl IS NULL OR TRIM(t.logoUrl) = ''
            """)
    long countMissingLogoUrls();
}
