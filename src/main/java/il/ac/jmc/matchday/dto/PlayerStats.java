package il.ac.jmc.matchday.dto;

/**
 * Immutable stats snapshot for a single player, computed from match events.
 *
 * @param goals       goals scored
 * @param assists     assists made
 * @param yellowCards yellow cards received
 * @param redCards    red cards received
 */
public record PlayerStats(int goals, int assists, int yellowCards, int redCards) {}
