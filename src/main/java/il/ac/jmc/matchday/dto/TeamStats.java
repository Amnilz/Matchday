package il.ac.jmc.matchday.dto;

/**
 * Immutable stats snapshot for a single team, computed from completed matches.
 * Points rule: win = 3, draw = 1, loss = 0.
 *
 * @param played       completed matches played
 * @param wins         matches won
 * @param draws        matches drawn
 * @param losses       matches lost
 * @param goalsFor     goals scored
 * @param goalsAgainst goals conceded
 */
public record TeamStats(int played, int wins, int draws, int losses, int goalsFor, int goalsAgainst) {

    /** Points awarded for a win. */
    public static final int WIN_POINTS = 3;

    /** Points awarded for a draw. */
    public static final int DRAW_POINTS = 1;

    /**
     * Returns goals scored minus goals conceded.
     *
     * @return goal difference
     */
    public int goalDifference() { return goalsFor - goalsAgainst; }

    /**
     * Returns total points under the win=3 / draw=1 / loss=0 rule.
     *
     * @return total points
     */
    public int points() { return wins * WIN_POINTS + draws * DRAW_POINTS; }
}
