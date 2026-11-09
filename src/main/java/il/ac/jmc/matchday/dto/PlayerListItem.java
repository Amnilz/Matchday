package il.ac.jmc.matchday.dto;

import il.ac.jmc.matchday.entity.Player;

/**
 * Bundles a player with their computed stats for display on the players list page.
 *
 * @param player the player entity
 * @param stats  the player's aggregated match-event stats
 */
public record PlayerListItem(Player player, PlayerStats stats) {}
