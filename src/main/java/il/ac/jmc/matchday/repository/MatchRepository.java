package il.ac.jmc.matchday.repository;

import il.ac.jmc.matchday.entity.Match;
import il.ac.jmc.matchday.entity.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link il.ac.jmc.matchday.entity.Match} entities.
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    /**
     * Returns a single match with both teams fetched in one query.
     *
     * @param id the ID of the match
     * @return an Optional containing the match if found
     */
    @Query("""
            SELECT m FROM Match m
            JOIN FETCH m.homeTeam
            JOIN FETCH m.awayTeam
            LEFT JOIN FETCH m.league
            WHERE m.id = :id
            """)
    Optional<Match> findByIdWithTeams(@Param("id") Long id);

    /**
     * Returns all matches with the given status, sorted by match date ascending.
     *
     * @param status the status of the matches
     * @return a list of matches
     */
    @Query("""
            SELECT m FROM Match m
            JOIN FETCH m.homeTeam
            JOIN FETCH m.awayTeam
            LEFT JOIN FETCH m.league
            WHERE m.status = :status
            ORDER BY m.matchDate ASC
            """)
    List<Match> findByStatusOrderByMatchDateAsc(@Param("status") MatchStatus status);

    /**
     * Returns the first {@code pageable.pageSize} matches with the given status, ascending by date.
     * Pass {@code PageRequest.of(0, limit)} to push the LIMIT to the DB.
     *
     * @param status   the status of the matches
     * @param pageable pagination details indicating limit
     * @return a list of matches
     */
    @Query("""
            SELECT m FROM Match m
            JOIN FETCH m.homeTeam
            JOIN FETCH m.awayTeam
            LEFT JOIN FETCH m.league
            WHERE m.status = :status
            ORDER BY m.matchDate ASC
            """)
    List<Match> findTopByStatusOrderByMatchDateAsc(@Param("status") MatchStatus status, Pageable pageable);

    /**
     * Returns the first {@code pageable.pageSize} matches with the given status, descending by date.
     * Pass {@code PageRequest.of(0, limit)} to push the LIMIT to the DB.
     *
     * @param status   the status of the matches
     * @param pageable pagination details indicating limit
     * @return a list of matches
     */
    @Query("""
            SELECT m FROM Match m
            JOIN FETCH m.homeTeam
            JOIN FETCH m.awayTeam
            LEFT JOIN FETCH m.league
            WHERE m.status = :status
            ORDER BY m.matchDate DESC
            """)
    List<Match> findTopByStatusOrderByMatchDateDesc(@Param("status") MatchStatus status, Pageable pageable);

    /**
     * Returns matches in the given league with the given status, sorted by match date ascending.
     *
     * @param leagueId the ID of the league
     * @param status   the status of the matches
     * @return a list of matches
     */
    @Query("""
            SELECT m FROM Match m
            JOIN FETCH m.homeTeam
            JOIN FETCH m.awayTeam
            LEFT JOIN FETCH m.league
            WHERE m.league.id = :leagueId
            AND m.status = :status
            ORDER BY m.matchDate ASC
            """)
    List<Match> findByLeagueIdAndStatusOrderByMatchDateAsc(@Param("leagueId") Long leagueId,
                                                           @Param("status") MatchStatus status);

    /**
     * Returns the {@code pageable.pageSize} most recent matches in the given league with the given status.
     * Pass {@code PageRequest.of(0, limit)} to push the LIMIT to the DB.
     *
     * @param leagueId the ID of the league
     * @param status   the status of the matches
     * @param pageable pagination details indicating limit
     * @return a list of matches
     */
    @Query("""
            SELECT m FROM Match m
            JOIN FETCH m.homeTeam
            JOIN FETCH m.awayTeam
            LEFT JOIN FETCH m.league
            WHERE m.league.id = :leagueId
            AND m.status = :status
            ORDER BY m.matchDate DESC
            """)
    List<Match> findTopByLeagueIdAndStatusOrderByMatchDateDesc(@Param("leagueId") Long leagueId,
                                                               @Param("status") MatchStatus status,
                                                               Pageable pageable);

    /**
     * Returns all matches involving the given team with the given status.
     *
     * @param teamId the ID of the team
     * @param status the status of the matches
     * @return a list of matches
     */
    @Query("""
            SELECT m FROM Match m
            JOIN FETCH m.homeTeam
            JOIN FETCH m.awayTeam
            LEFT JOIN FETCH m.league
            WHERE (m.homeTeam.id = :teamId OR m.awayTeam.id = :teamId)
            AND m.status = :status
            ORDER BY m.matchDate DESC
            """)
    List<Match> findByTeamIdAndStatus(@Param("teamId") Long teamId, @Param("status") MatchStatus status);

    /**
     * Returns the number of matches with the given status.
     *
     * @param status the status of the matches
     * @return the number of matches
     */
    long countByStatus(MatchStatus status);

    /**
     * Returns {@code true} if any match involves the given team (home or away).
     *
     * @param homeTeamId the ID of the home team check
     * @param awayTeamId the ID of the away team check (usually the same as homeTeamId)
     * @return true if a match exists with the given team, false otherwise
     */
    boolean existsByHomeTeamIdOrAwayTeamId(Long homeTeamId, Long awayTeamId);

    /**
     * Returns {@code true} if any match belongs to the given league.
     *
     * @param leagueId the ID of the league
     * @return true if a match exists in the league, false otherwise
     */
    boolean existsByLeagueId(Long leagueId);

    /**
     * Returns {@code true} if a match exists that belongs to the given league and involves the given team.
     *
     * @param leagueId the ID of the league
     * @param teamId   the ID of the team
     * @return true if a match exists, false otherwise
     */
    @Query("""
            SELECT COUNT(m) > 0 FROM Match m
            WHERE m.league.id = :leagueId
            AND (m.homeTeam.id = :teamId OR m.awayTeam.id = :teamId)
            """)
    boolean existsByLeagueIdAndTeamId(@Param("leagueId") Long leagueId,
                                      @Param("teamId") Long teamId);

    /**
     * Filters matches by team, league, status, date range, text query, and result,
     * returning one page of results ordered by match date, newest first.
     * The countQuery omits JOIN FETCH because Spring Data requires a plain JOIN for count queries
     * when the main query uses JOIN FETCH.
     *
     * @param query             the text search query
     * @param leagueIds         a list of league IDs to filter by
     * @param leagueFilterEmpty true to ignore the league filter
     * @param teamIds           a list of team IDs to filter by
     * @param teamFilterEmpty   true to ignore the team filter
     * @param statuses          a list of match statuses to filter by
     * @param statusFilterEmpty true to ignore the status filter
     * @param resultFilterEmpty true to ignore the result filter
     * @param homeWin           true to include home wins
     * @param draw              true to include draws
     * @param awayWin           true to include away wins
     * @param fromDateTime      the start of the date range
     * @param toDateTime        the end of the date range
     * @param pageable          pagination and sorting details
     * @return a page of matches
     */
    @Query(value = """
            SELECT m FROM Match m
            JOIN FETCH m.homeTeam home
            JOIN FETCH m.awayTeam away
            LEFT JOIN FETCH m.league league
            WHERE (:query IS NULL
                   OR LOWER(home.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(away.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(league.name) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (:leagueFilterEmpty = TRUE OR league.id IN (:leagueIds))
            AND (:teamFilterEmpty = TRUE OR home.id IN (:teamIds) OR away.id IN (:teamIds))
            AND (:statusFilterEmpty = TRUE OR m.status IN (:statuses))
            AND (:resultFilterEmpty = TRUE
                 OR (:homeWin = TRUE AND m.homeScore > m.awayScore)
                 OR (:draw = TRUE AND m.homeScore = m.awayScore)
                 OR (:awayWin = TRUE AND m.homeScore < m.awayScore))
            AND (:fromDateTime IS NULL OR m.matchDate >= :fromDateTime)
            AND (:toDateTime IS NULL OR m.matchDate < :toDateTime)
            """,
           countQuery = """
            SELECT COUNT(m) FROM Match m
            JOIN m.homeTeam home
            JOIN m.awayTeam away
            LEFT JOIN m.league league
            WHERE (:query IS NULL
                   OR LOWER(home.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(away.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(league.name) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (:leagueFilterEmpty = TRUE OR league.id IN (:leagueIds))
            AND (:teamFilterEmpty = TRUE OR home.id IN (:teamIds) OR away.id IN (:teamIds))
            AND (:statusFilterEmpty = TRUE OR m.status IN (:statuses))
            AND (:resultFilterEmpty = TRUE
                 OR (:homeWin = TRUE AND m.homeScore > m.awayScore)
                 OR (:draw = TRUE AND m.homeScore = m.awayScore)
                 OR (:awayWin = TRUE AND m.homeScore < m.awayScore))
            AND (:fromDateTime IS NULL OR m.matchDate >= :fromDateTime)
            AND (:toDateTime IS NULL OR m.matchDate < :toDateTime)
            """)
    Page<Match> findByFiltersPageable(@Param("query") String query,
                                       @Param("leagueIds") List<Long> leagueIds,
                                       @Param("leagueFilterEmpty") boolean leagueFilterEmpty,
                                       @Param("teamIds") List<Long> teamIds,
                                       @Param("teamFilterEmpty") boolean teamFilterEmpty,
                                       @Param("statuses") List<MatchStatus> statuses,
                                       @Param("statusFilterEmpty") boolean statusFilterEmpty,
                                       @Param("resultFilterEmpty") boolean resultFilterEmpty,
                                       @Param("homeWin") boolean homeWin,
                                       @Param("draw") boolean draw,
                                       @Param("awayWin") boolean awayWin,
                                       @Param("fromDateTime") LocalDateTime fromDateTime,
                                       @Param("toDateTime") LocalDateTime toDateTime,
                                       Pageable pageable);
}
