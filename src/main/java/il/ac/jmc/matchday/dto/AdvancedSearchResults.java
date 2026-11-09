package il.ac.jmc.matchday.dto;

import il.ac.jmc.matchday.entity.Match;
import il.ac.jmc.matchday.entity.Player;
import org.springframework.data.domain.Page;

/**
 * Combined result set for the league-wide advanced search page.
 * Each section is capped to the first page of results; {@code getTotalElements()}
 * carries the full count so the page can link to the dedicated list pages.
 *
 * @param matches   first page of matches hitting the criteria
 * @param players   first page of players hitting the criteria
 * @param teams     first page of team summaries hitting the criteria
 * @param hasSearch whether the user actually submitted any search input
 */
public record AdvancedSearchResults(
        Page<Match> matches,
        Page<Player> players,
        Page<TeamSummary> teams,
        boolean hasSearch
) {
}
