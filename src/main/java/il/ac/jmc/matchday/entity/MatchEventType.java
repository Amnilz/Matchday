package il.ac.jmc.matchday.entity;

/**
 * Type of event that can occur during a match.
 */
public enum MatchEventType {
    /** A goal scored by a player. */
    GOAL("Goal"),
    /** An assist credited to a player. */
    ASSIST("Assist"),
    /** A yellow card shown to a player. */
    YELLOW_CARD("Yellow card"),
    /** A red card shown to a player. */
    RED_CARD("Red card");

    /** Max length of the DB column holding this enum's name. */
    public static final int COLUMN_LENGTH = 20;

    /** Human-readable label shown in the UI. */
    private final String displayName;

    /** Creates a match event type with the given display name.
     *
     * @param  displayName human-readable label shown in the UI
     */
    MatchEventType(String displayName) {
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
