package il.ac.jmc.matchday.repository;

import il.ac.jmc.matchday.entity.MatchEvent;
import il.ac.jmc.matchday.entity.MatchEventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link MatchEvent} entities.
 */
@Repository
public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

    /**
     * Returns all events for the given match, eagerly fetching player + team to avoid N+1 rendering.
     *
     * @param matchId the ID of the match
     * @return a list of match events ordered by minute
     */
    @Query("""
            SELECT e FROM MatchEvent e
            JOIN FETCH e.player p
            JOIN FETCH p.team
            WHERE e.match.id = :matchId
            ORDER BY e.minute ASC, e.id ASC
            """)
    List<MatchEvent> findByMatchIdOrderByMinuteAsc(@Param("matchId") Long matchId);

    /**
     * Returns all events for the given player, most recent match first. Eagerly fetches match + teams.
     *
     * @param playerId the ID of the player
     * @return a list of match events ordered by match date descending
     */
    @Query("""
            SELECT e FROM MatchEvent e
            JOIN FETCH e.match m
            JOIN FETCH m.homeTeam
            JOIN FETCH m.awayTeam
            WHERE e.player.id = :playerId
            ORDER BY m.matchDate DESC
            """)
    List<MatchEvent> findByPlayerIdOrderByMatchMatchDateDesc(@Param("playerId") Long playerId);

    /**
     * Deletes all events belonging to the given match (used when a match is deleted).
     *
     * @param matchId the ID of the match
     */
    void deleteByMatchId(Long matchId);

    /**
     * Returns true when at least one recorded match event references the player.
     *
     * @param playerId the ID of the player
     * @return true if events exist for the player, false otherwise
     */
    boolean existsByPlayerId(Long playerId);

    /**
     * Returns [playerId, type, count] rows for all players in the given id list.
     * One query replaces N×4 individual count calls.
     *
     * @param playerIds a list of player IDs
     * @return a list of Object arrays containing player ID, event type, and count
     */
    @Query("""
            SELECT e.player.id, e.type, COUNT(e)
            FROM MatchEvent e
            WHERE e.player.id IN :playerIds
            GROUP BY e.player.id, e.type
            """)
    List<Object[]> countByPlayerIdsGroupedByType(@Param("playerIds") List<Long> playerIds);

    /**
     * Returns [playerId, count] rows for the top N players of a given event type,
     * ordered by count descending. Pass a {@code PageRequest.of(0, limit)} as pageable.
     *
     * @param type     the type of the event (e.g. GOAL)
     * @param pageable pagination details
     * @return a list of Object arrays containing player ID and count
     */
    @Query("""
            SELECT e.player.id, COUNT(e)
            FROM MatchEvent e
            WHERE e.type = :type
            GROUP BY e.player.id
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> findTopPlayerIdsByType(@Param("type") MatchEventType type, Pageable pageable);
}
