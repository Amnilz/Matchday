package il.ac.jmc.matchday.dto;

import il.ac.jmc.matchday.entity.Team;

/**
 * One computed row in the public league table.
 *
 * @param team  the team this row describes
 * @param stats the team's aggregated results and points
 */
public record LeagueTableRow(Team team, TeamStats stats) {}
