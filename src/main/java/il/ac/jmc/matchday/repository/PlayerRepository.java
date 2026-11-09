package il.ac.jmc.matchday.repository;

import il.ac.jmc.matchday.entity.Player;
import il.ac.jmc.matchday.entity.Position;
import il.ac.jmc.matchday.entity.Team;
import il.ac.jmc.matchday.entity.MatchEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link il.ac.jmc.matchday.entity.Player} entities.
 */
@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    /**
     * Returns {@code true} if a player with the given shirt number already exists in the given team.
     *
     * @param team        the team to check
     * @param shirtNumber the shirt number to check
     * @return true if the shirt number exists in the team, false otherwise
     */
    boolean existsByTeamAndShirtNumber(Team team, Integer shirtNumber);

    /**
     * Returns {@code true} if the team identified by {@code teamId} has at least one player.
     *
     * @param teamId the ID of the team
     * @return true if the team has players, false otherwise
     */
    boolean existsByTeamId(Long teamId);

    /**
     * Returns active players in the given team, ordered by shirt number. Used by public-facing pages.
     *
     * @param teamId the ID of the team
     * @return a list of active players ordered by shirt number
     */
    List<Player> findByTeamIdAndActiveTrueOrderByShirtNumberAsc(Long teamId);

    /**
     * Counts players that do not yet have a photo URL.
     *
     * @return the number of players missing photo URLs
     */
    @Query("""
            SELECT COUNT(p)
            FROM Player p
            WHERE p.photoUrl IS NULL OR TRIM(p.photoUrl) = ''
            """)
    long countMissingPhotoUrls();

    /**
     * Fetches all players whose IDs are in the given list, eagerly loading their team.
     *
     * @param ids a list of player IDs
     * @return a list of players with dynamically loaded teams
     */
    @Query("SELECT p FROM Player p JOIN FETCH p.team WHERE p.id IN :ids")
    List<Player> findAllByIdWithTeam(@Param("ids") List<Long> ids);

    /**
     * Filters active players by name, team, and position, returning one page. Used by the advanced search page.
     * The countQuery uses a plain JOIN because Spring Data cannot derive counts from JOIN FETCH queries.
     *
     * @param name                the text search query for player name
     * @param teamIds             a list of team IDs to filter by
     * @param teamFilterEmpty     true to ignore the team filter
     * @param positions           a list of positions to filter by
     * @param positionFilterEmpty true to ignore the position filter
     * @param pageable            pagination details
     * @return a page of active matched players
     */
    @Query(value = """
            SELECT p FROM Player p
            JOIN FETCH p.team team
            WHERE p.active = true
            AND   (:name IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%')))
            AND   (:teamFilterEmpty = TRUE OR team.id IN (:teamIds))
            AND   (:positionFilterEmpty = TRUE OR p.position IN (:positions))
            ORDER BY team.name ASC, p.shirtNumber ASC, p.id ASC
            """,
           countQuery = """
            SELECT COUNT(p) FROM Player p
            JOIN p.team team
            WHERE p.active = true
            AND   (:name IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%')))
            AND   (:teamFilterEmpty = TRUE OR team.id IN (:teamIds))
            AND   (:positionFilterEmpty = TRUE OR p.position IN (:positions))
            """)
    Page<Player> findActiveByFilters(@Param("name") String name,
                                     @Param("teamIds") List<Long> teamIds,
                                     @Param("teamFilterEmpty") boolean teamFilterEmpty,
                                     @Param("positions") List<Position> positions,
                                     @Param("positionFilterEmpty") boolean positionFilterEmpty,
                                     Pageable pageable);

    /**
     * Filters active players and returns a page of IDs ordered by the selected display sort.
     * The service fetches those IDs with team data and batch-loads stats for the page.
     *
     * @param name                the text search query for player name
     * @param teamIds             a list of team IDs to filter by
     * @param teamFilterEmpty     true to ignore the team filter
     * @param positions           a list of positions to filter by
     * @param positionFilterEmpty true to ignore the position filter
     * @param sortBy              the field to sort by (e.g., 'goals', 'assists')
     * @param direction           the sort direction ('asc' or 'desc')
     * @param goalType            the MatchEventType representing a goal
     * @param assistType          the MatchEventType representing an assist
     * @param yellowType          the MatchEventType representing a yellow card
     * @param redType             the MatchEventType representing a red card
     * @param pageable            pagination and sorting details
     * @return a page of player IDs
     */
    @Query(value = """
            SELECT p.id
            FROM Player p
            JOIN p.team team
            LEFT JOIN MatchEvent e ON e.player = p
            WHERE p.active = true
            AND   (:name IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%')))
            AND   (:teamFilterEmpty = TRUE OR team.id IN (:teamIds))
            AND   (:positionFilterEmpty = TRUE OR p.position IN (:positions))
            GROUP BY p.id, team.name, p.shirtNumber
            ORDER BY
                CASE WHEN :sortBy = 'goals' AND :direction = 'asc'
                     THEN SUM(CASE WHEN e.type = :goalType THEN 1 ELSE 0 END) ELSE 0 END ASC,
                CASE WHEN :sortBy = 'goals' AND :direction = 'desc'
                     THEN SUM(CASE WHEN e.type = :goalType THEN 1 ELSE 0 END) ELSE 0 END DESC,
                CASE WHEN :sortBy = 'assists' AND :direction = 'asc'
                     THEN SUM(CASE WHEN e.type = :assistType THEN 1 ELSE 0 END) ELSE 0 END ASC,
                CASE WHEN :sortBy = 'assists' AND :direction = 'desc'
                     THEN SUM(CASE WHEN e.type = :assistType THEN 1 ELSE 0 END) ELSE 0 END DESC,
                CASE WHEN :sortBy = 'yellows' AND :direction = 'asc'
                     THEN SUM(CASE WHEN e.type = :yellowType THEN 1 ELSE 0 END) ELSE 0 END ASC,
                CASE WHEN :sortBy = 'yellows' AND :direction = 'desc'
                     THEN SUM(CASE WHEN e.type = :yellowType THEN 1 ELSE 0 END) ELSE 0 END DESC,
                CASE WHEN :sortBy = 'reds' AND :direction = 'asc'
                     THEN SUM(CASE WHEN e.type = :redType THEN 1 ELSE 0 END) ELSE 0 END ASC,
                CASE WHEN :sortBy = 'reds' AND :direction = 'desc'
                     THEN SUM(CASE WHEN e.type = :redType THEN 1 ELSE 0 END) ELSE 0 END DESC,
                team.name ASC,
                p.shirtNumber ASC,
                p.id ASC
            """,
           countQuery = """
            SELECT COUNT(p) FROM Player p
            JOIN p.team team
            WHERE p.active = true
            AND   (:name IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%')))
            AND   (:teamFilterEmpty = TRUE OR team.id IN (:teamIds))
            AND   (:positionFilterEmpty = TRUE OR p.position IN (:positions))
            """)
    Page<Long> findActiveIdsByFiltersPageable(@Param("name") String name,
                                              @Param("teamIds") List<Long> teamIds,
                                              @Param("teamFilterEmpty") boolean teamFilterEmpty,
                                              @Param("positions") List<Position> positions,
                                              @Param("positionFilterEmpty") boolean positionFilterEmpty,
                                              @Param("sortBy") String sortBy,
                                              @Param("direction") String direction,
                                              @Param("goalType") MatchEventType goalType,
                                              @Param("assistType") MatchEventType assistType,
                                              @Param("yellowType") MatchEventType yellowType,
                                              @Param("redType") MatchEventType redType,
                                              Pageable pageable);

    /**
     * Filters all players (including inactive) by name, team, and position, returning one page.
     * Used by the admin list. The countQuery omits JOIN FETCH for Spring Data count compatibility.
     *
     * @param name                the text search query for player name
     * @param teamIds             a list of team IDs to filter by
     * @param teamFilterEmpty     true to ignore the team filter
     * @param positions           a list of positions to filter by
     * @param positionFilterEmpty true to ignore the position filter
     * @param pageable            pagination details
     * @return a page of matched players
     */
    @Query(value = """
            SELECT p FROM Player p
            JOIN FETCH p.team team
            WHERE (:name      IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%')))
            AND   (:teamFilterEmpty = TRUE OR team.id IN (:teamIds))
            AND   (:positionFilterEmpty = TRUE OR p.position IN (:positions))
            ORDER BY team.name ASC, p.shirtNumber ASC, p.id ASC
            """,
           countQuery = """
            SELECT COUNT(p) FROM Player p
            JOIN p.team team
            WHERE (:name      IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%')))
            AND   (:teamFilterEmpty = TRUE OR team.id IN (:teamIds))
            AND   (:positionFilterEmpty = TRUE OR p.position IN (:positions))
            """)
    Page<Player> findByFiltersPageable(@Param("name")               String name,
                                        @Param("teamIds")            List<Long> teamIds,
                                        @Param("teamFilterEmpty")    boolean teamFilterEmpty,
                                        @Param("positions")          List<Position> positions,
                                        @Param("positionFilterEmpty") boolean positionFilterEmpty,
                                        Pageable pageable);
}
