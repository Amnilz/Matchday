package il.ac.jmc.matchday.dto;

import il.ac.jmc.matchday.entity.MatchStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Public match browsing filters from the matches page.
 *
 * @param query         free-text team name search, may be blank
 * @param leagueIds     league ids to include, empty/null for all
 * @param teamIds       team ids (home or away) to include, empty/null for all
 * @param statuses      match statuses to include, empty/null for all
 * @param fromDate      earliest match date, inclusive, or {@code null}
 * @param toDate        latest match date, inclusive, or {@code null}
 * @param resultFilters result outcomes (home win / draw / away win) to include
 */
public record MatchSearchCriteria(
        String query,
        List<Long> leagueIds,
        List<Long> teamIds,
        List<MatchStatus> statuses,
        LocalDate fromDate,
        LocalDate toDate,
        List<MatchResultFilter> resultFilters
) {
    /**
     * Returns true when at least one filter or search term is set.
     *
     * @return true when this criteria contains user input
     */
    public boolean hasFilters() {
        return (query != null && !query.isBlank())
                || (leagueIds != null && !leagueIds.isEmpty())
                || (teamIds != null && !teamIds.isEmpty())
                || (statuses != null && !statuses.isEmpty())
                || (resultFilters != null && !resultFilters.isEmpty())
                || fromDate != null
                || toDate != null;
    }

    /**
     * Returns true when both dates are set and the range is reversed.
     *
     * @return true when the date range is invalid
     */
    public boolean hasInvalidDateRange() {
        return fromDate != null && toDate != null && fromDate.isAfter(toDate);
    }
}
