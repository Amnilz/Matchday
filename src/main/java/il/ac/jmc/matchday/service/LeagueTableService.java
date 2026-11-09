package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.dto.LeagueTableRow;
import il.ac.jmc.matchday.dto.TeamStats;
import il.ac.jmc.matchday.entity.Match;
import il.ac.jmc.matchday.entity.MatchStatus;
import il.ac.jmc.matchday.entity.Team;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for standings and points calculations.
 */
@Service
@Transactional(readOnly = true)
public class LeagueTableService {

    /** Service for fetching completed matches. */
    private final MatchService matchService;
    /** Service for fetching teams for the global table. */
    private final TeamService teamService;
    /** Service for fetching league membership. */
    private final LeagueService leagueService;

    /**
     * Creates the table service from the services that provide teams, leagues, and completed matches.
     *
     * @param matchService service used to fetch completed matches
     * @param teamService service used to fetch teams for the global table
     * @param leagueService service used to fetch league membership
     */
    public LeagueTableService(MatchService matchService,
                              TeamService teamService,
                              LeagueService leagueService) {
        this.matchService = matchService;
        this.teamService = teamService;
        this.leagueService = leagueService;
    }

    /**
     * Computes stats for the given team from completed matches.
     *
     * @param teamId team id
     * @return aggregate team stats from completed matches
     */
    public TeamStats getTeamStats(Long teamId) {
        List<Match> completed = matchService.findByTeamIdAndStatus(teamId, MatchStatus.COMPLETED);
        return computeStats(teamId, completed);
    }

    /**
     * Computes the full league table from completed matches.
     * Sorting: points desc, goal difference desc, goals for desc, team name asc.
     *
     * @return league table rows sorted by standings rules
     */
    public List<LeagueTableRow> getTable() {
        return leagueService.findRandomLeague()
                .map(league -> getTableForLeague(league.getId()))
                .orElseGet(this::getGlobalTable);
    }

    /**
     * Computes the table for a single league from that league's completed matches.
     *
     * <p>IMPORTANT: {@code matchService.findByLeagueIdAndStatus} must use JOIN FETCH on homeTeam
     * and awayTeam (see {@code MatchRepository.findByLeagueIdAndStatusOrderByMatchDateAsc}).
     * If that query ever drops the JOIN FETCH, every {@code match.getHomeTeam()} call below
     * will trigger a separate SELECT, producing an N+1 query.
     *
     * @param leagueId league id
     * @return standings rows for the selected league
     */
    public List<LeagueTableRow> getTableForLeague(Long leagueId) {
        return buildTable(
                leagueService.findTeams(leagueId),
                matchService.findByLeagueIdAndStatus(leagueId, MatchStatus.COMPLETED));
    }

    /**
     * Computes the standings table across all teams and all completed matches.
     *
     * @return globally ranked table rows
     */
    private List<LeagueTableRow> getGlobalTable() {
        return buildTable(
                teamService.findAll(),
                matchService.findByStatus(MatchStatus.COMPLETED));
    }

    /**
     * Core standings computation shared by {@link #getTableForLeague} and {@link #getGlobalTable}.
     * Accepts any team list and its corresponding completed matches — callers supply both.
     *
     * @param teams teams to include in the table
     * @param completedMatches completed matches to compute stats from
     * @return sorted standings rows
     */
    private List<LeagueTableRow> buildTable(List<Team> teams, List<Match> completedMatches) {
        Map<Long, MutableStats> statsByTeam = new HashMap<>();
        teams.forEach(team -> statsByTeam.put(team.getId(), new MutableStats()));
        completedMatches.forEach(match -> applyCompletedMatch(statsByTeam, match));
        return teams.stream()
                .map(team -> new LeagueTableRow(team, statsByTeam.get(team.getId()).toTeamStats()))
                .sorted(tableComparator())
                .toList();
    }

    /**
     * Computes aggregate stats for one team from a list of completed matches.
     *
     * @param teamId team id to compute stats for
     * @param completedMatches completed matches to scan
     * @return computed team stats
     */
    private TeamStats computeStats(Long teamId, List<Match> completedMatches) {
        int wins = 0, draws = 0, losses = 0, goalsFor = 0, goalsAgainst = 0;

        for (Match match : completedMatches) {
            if (hasMissingScore(match)) continue;

            boolean isHome = match.getHomeTeam().getId().equals(teamId);
            int scored = isHome ? match.getHomeScore() : match.getAwayScore();
            int conceded = isHome ? match.getAwayScore() : match.getHomeScore();

            goalsFor += scored;
            goalsAgainst += conceded;

            if (scored > conceded) {
                wins++;
            } else if (scored == conceded) {
                draws++;
            } else {
                losses++;
            }
        }

        return new TeamStats(wins + draws + losses, wins, draws, losses, goalsFor, goalsAgainst);
    }

    /**
     * Updates the mutable stat accumulators for both teams based on one completed match result.
     *
     * @param statsByTeam mutable stats map keyed by team id
     * @param match completed match to apply
     */
    private void applyCompletedMatch(Map<Long, MutableStats> statsByTeam, Match match) {
        if (hasMissingScore(match)) return;

        MutableStats home = statsByTeam.get(match.getHomeTeam().getId());
        MutableStats away = statsByTeam.get(match.getAwayTeam().getId());
        if (home == null || away == null) return;

        int homeScore = match.getHomeScore();
        int awayScore = match.getAwayScore();

        home.played++;
        away.played++;
        home.goalsFor += homeScore;
        home.goalsAgainst += awayScore;
        away.goalsFor += awayScore;
        away.goalsAgainst += homeScore;

        if (homeScore > awayScore) {
            home.wins++;
            away.losses++;
        } else if (homeScore < awayScore) {
            away.wins++;
            home.losses++;
        } else {
            home.draws++;
            away.draws++;
        }
    }

    /**
     * Returns true when either score is null, making the match unsuitable for stats.
     *
     * @param match match to check
     * @return true if a score is missing
     */
    private boolean hasMissingScore(Match match) {
        return match.getHomeScore() == null || match.getAwayScore() == null;
    }

    /**
     * Returns the standings comparator: points desc, goal difference desc, goals for desc, name asc.
     *
     * @return table row comparator
     */
    private Comparator<LeagueTableRow> tableComparator() {
        return Comparator
                .comparingInt((LeagueTableRow row) -> row.stats().points()).reversed()
                .thenComparing(Comparator.comparingInt((LeagueTableRow row) -> row.stats().goalDifference()).reversed())
                .thenComparing(Comparator.comparingInt((LeagueTableRow row) -> row.stats().goalsFor()).reversed())
                .thenComparing(row -> row.team().getName(), String.CASE_INSENSITIVE_ORDER);
    }

    /** Mutable accumulator used while scanning completed matches to build standings. */
    private static class MutableStats {
        /** Number of matches played. */
        int played;
        /** Number of wins. */
        int wins;
        /** Number of draws. */
        int draws;
        /** Number of losses. */
        int losses;
        /** Total goals scored. */
        int goalsFor;
        /** Total goals conceded. */
        int goalsAgainst;

        /** Creates a zeroed accumulator. */
        MutableStats() {}

        /**
         * Converts this mutable accumulator to an immutable {@link TeamStats} snapshot.
         *
         * @return immutable team stats
         */
        TeamStats toTeamStats() {
            return new TeamStats(played, wins, draws, losses, goalsFor, goalsAgainst);
        }
    }
}
