package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.dto.RecentLink;
import il.ac.jmc.matchday.entity.Match;
import il.ac.jmc.matchday.entity.Team;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serial;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Session-scoped storage for per-browser recent user activity.
 */
@Service
@SessionScope
public class SessionHistoryService implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Maximum number of recent links retained per category per session. */
    private static final int MAX_ITEMS = 5;
    /** Path prefix for team detail pages. */
    private static final String TEAMS_PATH = "/teams/";
    /** Path prefix for match detail pages. */
    private static final String MATCHES_PATH = "/matches/";
    /** Date format used for match date labels in recent-history entries. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Recently viewed team links for this session, newest first. */
    private final ArrayList<RecentLink> recentTeams = new ArrayList<>();

    /** Recently viewed match links for this session, newest first. */
    private final ArrayList<RecentLink> recentMatches = new ArrayList<>();

    /**
     * Creates an empty session history service for one browser session.
     */
    public SessionHistoryService() {}

    /**
     * Remembers a team detail page visit for this session.
     *
     * @param team visited team
     */
    public void rememberTeam(Team team) {
        remember(recentTeams, new RecentLink(
                team.getId(),
                team.getName(),
                TEAMS_PATH + team.getId(),
                null
        ));
    }

    /**
     * Remembers a match detail page visit for this session.
     *
     * @param match visited match
     */
    public void rememberMatch(Match match) {
        String label = match.getHomeTeam().getName() + " vs " + match.getAwayTeam().getName();
        String subtitle = match.getMatchDate() != null ? DATE_FORMAT.format(match.getMatchDate()) : null;
        remember(recentMatches, new RecentLink(
                match.getId(),
                label,
                MATCHES_PATH + match.getId(),
                subtitle
        ));
    }

    /**
     * Returns an immutable snapshot of recently viewed teams, newest first.
     *
     * @return recently viewed team links
     */
    public List<RecentLink> getRecentTeams() {
        return List.copyOf(recentTeams);
    }

    /**
     * Returns an immutable snapshot of recently viewed matches, newest first.
     *
     * @return recently viewed match links
     */
    public List<RecentLink> getRecentMatches() {
        return List.copyOf(recentMatches);
    }

    /**
     * Adds the link to the list, deduplicating by URL and capping the list at {@link #MAX_ITEMS}.
     *
     * @param items mutable recent-link list to update
     * @param link new link to prepend
     */
    private void remember(List<RecentLink> items, RecentLink link) {
        items.removeIf(existing -> existing.url().equals(link.url()));
        items.addFirst(link);
        if (items.size() > MAX_ITEMS) {
            items.subList(MAX_ITEMS, items.size()).clear();
        }
    }
}
