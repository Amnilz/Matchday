package il.ac.jmc.matchday.entity;

/**
 * Lifecycle status of a league match.
 */
public enum MatchStatus {
    /** Match has been created but not yet played. */
    SCHEDULED("Scheduled"),
    /** Match has been played and the result is recorded. */
    COMPLETED("Completed"),
    /** Match was canceled and will not be played. */
    CANCELLED("Cancelled");

    /** Max length of the DB column holding this enum's name. */
    public static final int COLUMN_LENGTH = 20;

    /** Human-readable label shown in the UI. */
    private final String displayName;

    /** Constructor for enum constants.
     *
     * @param displayName human-readable label for this status
     */
    MatchStatus(String displayName) {
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
