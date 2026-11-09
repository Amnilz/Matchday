package il.ac.jmc.matchday.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A snapshot of one user search, stored in session for quick re-running.
 *
 * @param label       Human-readable summary shown in the UI (e.g. "League: Arsenal, Completed").
 * @param queryString Raw URL query string used to re-run this search (e.g. {@code q=Arsenal&status=COMPLETED}).
 * @param performedAt When the search was executed.
 */
public record RecentSearch(
        String label,
        String queryString,
        LocalDateTime performedAt
) implements Serializable {}
