package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.dto.AdvancedSearchResults;
import il.ac.jmc.matchday.dto.MatchSearchCriteria;
import il.ac.jmc.matchday.dto.MatchResultFilter;
import il.ac.jmc.matchday.dto.RecentSearch;
import il.ac.jmc.matchday.dto.TeamSummary;
import il.ac.jmc.matchday.entity.Match;
import il.ac.jmc.matchday.entity.MatchStatus;
import il.ac.jmc.matchday.entity.Player;
import il.ac.jmc.matchday.entity.Position;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Facade for advanced search across matches, players, and teams.
 * All filtering logic lives in the underlying services and repositories.
 */
@Service
@Transactional(readOnly = true)
public class SearchService {

    /** Date format used when describing a recent-search date range filter. */
    private static final DateTimeFormatter DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Max rows shown per result section; the full list lives on the dedicated list pages. */
    private static final int SECTION_LIMIT = 10;

    /** Match search service. */
    private final MatchService matchService;
    /** Player search service. */
    private final PlayerService playerService;
    /** Team search service. */
    private final TeamService teamService;
    /** League filter helper service. */
    private final LeagueService leagueService;

    /**
     * Creates the service with the section-specific search services.
     *
     * @param matchService match search service
     * @param playerService player search service
     * @param teamService team search service
     * @param leagueService league filter helper service
     */
    public SearchService(MatchService matchService,
                         PlayerService playerService,
                         TeamService teamService,
                         LeagueService leagueService) {
        this.matchService  = matchService;
        this.playerService = playerService;
        this.teamService   = teamService;
        this.leagueService = leagueService;
    }

    /**
     * Returns the first matches matching the given criteria (date range, team, status, text query).
     *
     * @param criteria active match search criteria
     * @return first page of matching matches
     */
    private Page<Match> searchMatches(MatchSearchCriteria criteria) {
        return matchService.findTopByCriteria(criteria, SECTION_LIMIT);
    }

    /**
     * Returns the first active players matching any combination of name, teams, and positions.
     *
     * @param name optional name filter
     * @param leagueIds optional league id filter
     * @param teamIds optional team id filter
     * @param positions optional position filter
     * @return first page of matching players
     */
    private Page<Player> searchPlayers(String name,
                                       List<Long> leagueIds,
                                       List<Long> teamIds,
                                       List<Position> positions) {
        List<Long> effectiveTeamIds = leagueService.effectiveTeamIdsForLeagueFilter(leagueIds, teamIds);
        return playerService.findActiveByFilters(name, effectiveTeamIds, positions, SECTION_LIMIT);
    }

    /**
     * Returns the first teams matching the query text and optional league filter.
     * Reuses the same repository query as the teams list page so counts stay consistent.
     *
     * @param query optional name filter
     * @param leagueIds optional league id filter
     * @return first page of matching team summaries
     */
    private Page<TeamSummary> searchTeams(String query, List<Long> leagueIds) {
        return teamService.findTopSummaries(query, leagueIds, SECTION_LIMIT);
    }

    /**
     * Runs the combined league search and keeps the decision about which
     * sections should query data inside the search service.
     *
     * @param query optional free-text query
     * @param matchCriteria match search criteria
     * @param positions optional player position filters
     * @param queryString cleaned request query string
     * @return combined search results
     */
    public AdvancedSearchResults searchAll(String query,
                                           MatchSearchCriteria matchCriteria,
                                           List<Position> positions,
                                           String queryString) {
        boolean hasSearch = queryString != null && !queryString.isBlank();
        boolean matchSearch = hasSearch && matchCriteria.hasFilters();
        boolean playerSearch = hasSearch && (hasText(query)
                || hasValues(matchCriteria.leagueIds())
                || hasValues(matchCriteria.teamIds())
                || hasValues(positions));
        boolean teamSearch = hasSearch && (hasText(query) || hasValues(matchCriteria.leagueIds()));

        return new AdvancedSearchResults(
                matchSearch ? searchMatches(matchCriteria) : Page.empty(),
                playerSearch ? searchPlayers(query, matchCriteria.leagueIds(), matchCriteria.teamIds(), positions) : Page.empty(),
                teamSearch ? searchTeams(query, matchCriteria.leagueIds()) : Page.empty(),
                hasSearch
        );
    }

    /**
     * Returns true when the request contains a meaningful search filter worth saving in session.
     *
     * @param query optional free-text query
     * @param matchCriteria match search criteria
     * @param positions optional player position filters
     * @return true when the search should be remembered
     */
    public boolean shouldRemember(String query,
                                  MatchSearchCriteria matchCriteria,
                                  List<Position> positions) {
        return matchCriteria.hasFilters()
                || (positions != null && !positions.isEmpty())
                || hasText(query);
    }

    /**
     * Builds a session-safe recent-search snapshot for quick re-running from search/profile pages.
     *
     * @param query optional free-text query
     * @param matchCriteria match search criteria
     * @param positions optional player position filters
     * @param queryString cleaned request query string
     * @return recent-search snapshot
     */
    public RecentSearch buildRecentSearch(String query,
                                          MatchSearchCriteria matchCriteria,
                                          List<Position> positions,
                                          String queryString) {
        return new RecentSearch(
                describe(query, matchCriteria, positions),
                queryString,
                LocalDateTime.now()
        );
    }

    /**
     * Builds a short human-readable label summarising the active search filters.
     *
     * @param query optional free-text query
     * @param matchCriteria active match criteria
     * @param positions optional position filters
     * @return summary label for the recent-search entry
     */
    private String describe(String query,
                            MatchSearchCriteria matchCriteria,
                            List<Position> positions) {
        List<String> parts = new ArrayList<>();
        addTextPart(parts, query);

        addCountPart(parts, matchCriteria.leagueIds(), "league");
        addCountPart(parts, matchCriteria.teamIds(), "team");
        addDisplayParts(parts, matchCriteria.statuses());
        addDisplayParts(parts, matchCriteria.resultFilters());
        addDisplayParts(parts, positions);
        if (matchCriteria.fromDate() != null || matchCriteria.toDate() != null) {
            String from = matchCriteria.fromDate() != null
                    ? matchCriteria.fromDate().format(DATE_LABEL_FORMAT)
                    : "...";
            String to = matchCriteria.toDate() != null
                    ? matchCriteria.toDate().format(DATE_LABEL_FORMAT)
                    : "...";
            parts.add(from + " to " + to);
        }

        String suffix = parts.isEmpty() ? "All" : String.join(", ", parts);
        return "League: " + suffix;
    }

    /**
     * Returns true when the value is non-null and non-blank.
     *
     * @param value string to check
     * @return true when the value carries text
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Returns true when the list is non-null and non-empty.
     *
     * @param values list to check
     * @return true when the list contains values
     */
    private boolean hasValues(List<?> values) {
        return values != null && !values.isEmpty();
    }

    /**
     * Appends the trimmed text to the description parts when non-blank.
     *
     * @param parts accumulator list
     * @param value text value to append
     */
    private void addTextPart(List<String> parts, String value) {
        if (hasText(value)) parts.add(value.trim());
    }

    /**
     * Appends a count summary ("1 league" or "3 leagues") to the description parts.
     *
     * @param parts accumulator list
     * @param values selected values
     * @param singular singular noun for the count label
     */
    private void addCountPart(List<String> parts, List<?> values, String singular) {
        if (values == null || values.isEmpty()) return;
        parts.add(values.size() == 1 ? "1 " + singular : values.size() + " " + singular + "s");
    }

    /**
     * Appends the display name of each value to the description parts.
     *
     * @param parts accumulator list
     * @param values values to display
     */
    private void addDisplayParts(List<String> parts, List<?> values) {
        if (values == null) return;
        values.stream()
                .map(this::displayName)
                .forEach(parts::add);
    }

    /**
     * Returns the human-readable label for a filter value.
     *
     * @param value filter value
     * @return display label
     */
    private String displayName(Object value) {
        if (value instanceof MatchStatus status) return status.getDisplayName();
        if (value instanceof MatchResultFilter result) return result.getDisplayName();
        if (value instanceof Position position) return position.getDisplayName();
        return String.valueOf(value);
    }
}
