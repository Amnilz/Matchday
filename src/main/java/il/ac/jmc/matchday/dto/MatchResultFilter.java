package il.ac.jmc.matchday.dto;

/**
 * Result-side filters for advanced match search.
 */
public enum MatchResultFilter {
    /** Completed matches won by the home team. */
    HOME_WIN("Home win"),
    /** Completed matches that ended in a draw. */
    DRAW("Draw"),
    /** Completed matches won by the away team. */
    AWAY_WIN("Away win");

    /** Human-readable label shown in the UI. */
    private final String displayName;

    /**
     * Creates a filter constant with the given display label.
     *
     * @param displayName human-readable label for the UI
     */
    MatchResultFilter(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable label shown in the UI.
     *
     * @return display label
     */
    public String getDisplayName() {
        return displayName;
    }
}
