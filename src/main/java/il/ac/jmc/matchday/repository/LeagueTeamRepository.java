package il.ac.jmc.matchday.repository;

import il.ac.jmc.matchday.entity.LeagueTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Spring Data JPA repository for team memberships in leagues.
 */
@Repository
public interface LeagueTeamRepository extends JpaRepository<LeagueTeam, Long> {

    /**
     * Returns true when the team is already a member of the league.
     *
     * @param leagueId league id
     * @param teamId team id
     * @return true when the membership exists
     */
    boolean existsByLeagueIdAndTeamId(Long leagueId, Long teamId);

    /**
     * Returns a league's memberships with the team fetched, sorted by team name.
     *
     * @param leagueId league id
     * @return memberships in the league
     */
    @Query("""
            SELECT lt FROM LeagueTeam lt
            JOIN FETCH lt.team t
            WHERE lt.league.id = :leagueId
            ORDER BY t.name ASC
            """)
    List<LeagueTeam> findByLeagueIdWithTeamOrderByTeamName(@Param("leagueId") Long leagueId);

    /**
     * Returns a team's memberships with the league fetched, sorted by league name.
     *
     * @param teamId team id
     * @return memberships for the team
     */
    @Query("""
            SELECT lt FROM LeagueTeam lt
            JOIN FETCH lt.league l
            WHERE lt.team.id = :teamId
            ORDER BY l.name ASC
            """)
    List<LeagueTeam> findByTeamIdWithLeagueOrderByLeagueName(@Param("teamId") Long teamId);

    /**
     * Returns the ids of all teams that belong to the league.
     *
     * @param leagueId league id
     * @return team ids in the league
     */
    @Query("""
            SELECT lt.team.id FROM LeagueTeam lt
            WHERE lt.league.id = :leagueId
            """)
    Set<Long> findTeamIdsByLeagueId(@Param("leagueId") Long leagueId);

    /**
     * Returns (teamId, leagueId) pairs for the listed teams, used to filter team dropdowns by league.
     *
     * @param teamIds team ids to inspect
     * @return rows containing team id and league id
     */
    @Query("""
            SELECT lt.team.id, lt.league.id FROM LeagueTeam lt
            WHERE lt.team.id IN :teamIds
            ORDER BY lt.team.id ASC, lt.league.id ASC
            """)
    List<Object[]> findLeagueIdsByTeamIds(@Param("teamIds") List<Long> teamIds);

    /**
     * Returns (leagueId, teamCount) pairs for all listed leagues in one query.
     * Leagues with no teams are absent from the result; callers should default missing entries to 0.
     *
     * @param leagueIds league ids to count
     * @return rows containing league id and team count
     */
    @Query("SELECT lt.league.id, COUNT(lt) FROM LeagueTeam lt WHERE lt.league.id IN :leagueIds GROUP BY lt.league.id")
    List<Object[]> countTeamsByLeagueIds(@Param("leagueIds") List<Long> leagueIds);

    /**
     * Removes one team's membership in one league; absent rows are a no-op.
     *
     * @param leagueId league id
     * @param teamId team id
     */
    @Modifying
    void deleteByLeagueIdAndTeamId(Long leagueId, Long teamId);

    /**
     * Removes all memberships of a league that is being deleted.
     *
     * @param leagueId league id
     */
    @Modifying
    void deleteByLeagueId(Long leagueId);

    /**
     * Removes all memberships of a team that is being deleted.
     *
     * @param teamId team id
     */
    @Modifying
    void deleteByTeamId(Long teamId);
}
