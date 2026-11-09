package il.ac.jmc.matchday.entity;

/**
 * Playing position of a football player on the pitch.
 * Covers all standard positions from goalkeeper to striker.
 */
public enum Position {
    /** Goalkeeper. */
    GK,
    /** Left back. */
    LB,
    /** Right back. */
    RB,
    /** Centre back. */
    CB,
    /** Central defensive midfielder. */
    CDM,
    /** Central midfielder. */
    CM,
    /** Central attacking midfielder. */
    CAM,
    /** Left midfielder. */
    LM,
    /** Right midfielder. */
    RM,
    /** Left winger. */
    LW,
    /** Right winger. */
    RW,
    /** Striker. */
    ST,
    /** Centre forward. */
    CF;

    /** Max length of the DB column holding this enum's name. */
    public static final int COLUMN_LENGTH = 20;

    /**
     * Returns the label shown in the UI (the position abbreviation itself).
     *
     * @return display label
     */
    public String getDisplayName() {
        return name();
    }
}
