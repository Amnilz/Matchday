package il.ac.jmc.matchday.dto;

import java.io.Serializable;

/**
 * Small display DTO for recent session links.
 *
 * @param id       id of the linked entity (team or match)
 * @param label    text shown for the link
 * @param url      relative URL the link points to
 * @param subtitle optional secondary line (e.g. match date), may be {@code null}
 */
public record RecentLink(Long id, String label, String url, String subtitle) implements Serializable {}
