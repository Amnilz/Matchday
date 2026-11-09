package il.ac.jmc.matchday.dto;

import il.ac.jmc.matchday.entity.MatchStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Form DTO for creating and editing a {@link il.ac.jmc.matchday.entity.Match}.
 * Scores are optional for SCHEDULED/CANCELLED; required when status is COMPLETED.
 */
public class MatchForm {

    /**
     * Creates an empty match form for MVC binding.
     */
    public MatchForm() {}

    /** Id of the selected home team. */
    @NotNull(message = "Home team is required")
    private Long homeTeamId;

    /** Id of the selected away team. */
    @NotNull(message = "Away team is required")
    private Long awayTeamId;

    /** Id of the selected league. */
    @NotNull(message = "League is required")
    private Long leagueId;

    /** Scheduled date and time of the match. */
    @NotNull(message = "Match date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime matchDate;

    /** Match status; defaults to {@link MatchStatus#SCHEDULED}. */
    private MatchStatus status = MatchStatus.SCHEDULED;

    /** Home team score; optional for non-COMPLETED matches. */
    @Min(value = 0, message = "Score cannot be negative")
    private Integer homeScore;

    /** Away team score; optional for non-COMPLETED matches. */
    @Min(value = 0, message = "Score cannot be negative")
    private Integer awayScore;

    /** Match events to record when completing a match. */
    private List<EventForm> events = new ArrayList<>();

    /**
     * Returns the selected home team id.
     *
     * @return home team id
     */
    public Long getHomeTeamId() { return homeTeamId; }

    /**
     * Sets the selected home team id.
     *
     * @param homeTeamId home team id
     */
    public void setHomeTeamId(Long homeTeamId) { this.homeTeamId = homeTeamId; }

    /**
     * Returns the selected away team id.
     *
     * @return away team id
     */
    public Long getAwayTeamId() { return awayTeamId; }

    /**
     * Sets the selected away team id.
     *
     * @param awayTeamId away team id
     */
    public void setAwayTeamId(Long awayTeamId) { this.awayTeamId = awayTeamId; }

    /**
     * Returns the selected league id.
     *
     * @return league id
     */
    public Long getLeagueId() { return leagueId; }

    /**
     * Sets the selected league id.
     *
     * @param leagueId league id
     */
    public void setLeagueId(Long leagueId) { this.leagueId = leagueId; }

    /**
     * Returns the scheduled match date and time.
     *
     * @return match date and time
     */
    public LocalDateTime getMatchDate() { return matchDate; }

    /**
     * Sets the scheduled match date and time.
     *
     * @param matchDate match date and time
     */
    public void setMatchDate(LocalDateTime matchDate) { this.matchDate = matchDate; }

    /**
     * Returns the selected match status.
     *
     * @return match status
     */
    public MatchStatus getStatus() { return status; }

    /**
     * Sets the selected match status.
     *
     * @param status match status
     */
    public void setStatus(MatchStatus status) { this.status = status; }

    /**
     * Returns the submitted home score.
     *
     * @return home score
     */
    public Integer getHomeScore() { return homeScore; }

    /**
     * Sets the submitted home score.
     *
     * @param homeScore home score
     */
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    /**
     * Returns the submitted away score.
     *
     * @return away score
     */
    public Integer getAwayScore() { return awayScore; }

    /**
     * Sets the submitted away score.
     *
     * @param awayScore away score
     */
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    /**
     * Returns the submitted match events.
     *
     * @return mutable event form list
     */
    public List<EventForm> getEvents() { return events; }

    /**
     * Sets the submitted match events, replacing null with an empty list for form safety.
     *
     * @param events event form list
     */
    public void setEvents(List<EventForm> events) {
        this.events = events != null ? events : new ArrayList<>();
    }
}
