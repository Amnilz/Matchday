package il.ac.jmc.matchday.dto;

/**
 * Read-only projection used for the teams list page.
 * Returned by a single JOIN query - avoids N+1 player-count lookups.
 */
public interface TeamSummary {
    /**
     * Returns the team id.
     *
     * @return team id
     */
    Long getId();

    /**
     * Returns the team name.
     *
     * @return team name
     */
    String getName();

    /**
     * Returns the team logo URL, or {@code null} when none is set.
     *
     * @return logo URL, or {@code null}
     */
    String getLogoUrl();

    /**
     * Returns the team description, or {@code null} when none is set.
     *
     * @return team description, or {@code null}
     */
    String getDescription();

    /**
     * Returns the active player count populated by the repository projection.
     *
     * @return active player count
     */
    @SuppressWarnings("unused") // read by Thymeleaf (${team.playerCount}), populated from the COUNT(p) query alias
    long getPlayerCount();
}
