package il.ac.jmc.matchday.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing a league match between two teams.
 * Scores are populated when the match status transitions to {@link MatchStatus#COMPLETED}.
 */
@Entity
@Table(name = "matches")
public class Match {

    /** Database identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Home team. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    /**  Away team.  */
    @ManyToOne(optional = false)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    /**
     * League this match belongs to.
     * The column stays nullable at the DB level so {@code ddl-auto=update} does not reject
     * legacy rows created before the league feature existed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "league_id")
    private League league;

    /** Scheduled date and time of the match. */
    @Column(nullable = false)
    private LocalDateTime matchDate;

    /** Home team score, or {@code null} before completion. */
    private Integer homeScore;

    /** Away team score, or {@code null} before completion. */
    private Integer awayScore;

    /** Match status, defaulting to {@link MatchStatus#SCHEDULED}.  */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = MatchStatus.COLUMN_LENGTH)
    private MatchStatus status = MatchStatus.SCHEDULED;

    /** Timestamp when the match was created, set automatically on persist and never updated. */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the match was last updated, set automatically on persist and update. */
    private LocalDateTime updatedAt;

    /**
     * JPA-only constructor.
     */
    protected Match() {}

    /**
     * Creates a scheduled match between two teams.
     *
     * @param homeTeam home team
     * @param awayTeam away team
     * @param matchDate scheduled date and time
     */
    public Match(Team homeTeam, Team awayTeam, LocalDateTime matchDate) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.matchDate = matchDate;
    }

    /**
     * Initializes audit timestamps before the match is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    /**
     * Refreshes the update timestamp before the match is saved.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the database id.
     *
     * @return match id
     */
    public Long getId() { return id; }

    /**
     * Returns the home team.
     *
     * @return home team
     */
    public Team getHomeTeam() { return homeTeam; }

    /**
     * Sets the home team.
     *
     * @param homeTeam home team
     */
    public void setHomeTeam(Team homeTeam) { this.homeTeam = homeTeam; }

    /**
     * Returns the away team.
     *
     * @return away team
     */
    public Team getAwayTeam() { return awayTeam; }

    /**
     * Sets the away team.
     *
     * @param awayTeam away team
     */
    public void setAwayTeam(Team awayTeam) { this.awayTeam = awayTeam; }

    /**
     * Returns the league this match belongs to.
     *
     * @return league
     */
    public League getLeague() { return league; }

    /**
     * Sets the league this match belongs to.
     *
     * @param league league
     */
    public void setLeague(League league) { this.league = league; }

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
     * Returns the home score.
     *
     * @return home score, or {@code null} before completion
     */
    public Integer getHomeScore() { return homeScore; }

    /**
     * Sets the home score.
     *
     * @param homeScore home score
     */
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    /**
     * Returns the away score.
     *
     * @return away score, or {@code null} before completion
     */
    public Integer getAwayScore() { return awayScore; }

    /**
     * Sets the away score.
     *
     * @param awayScore away score
     */
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    /**
     * Returns the match status.
     *
     * @return match status
     */
    public MatchStatus getStatus() { return status; }

    /**
     * Sets the match status.
     *
     * @param status match status
     */
    public void setStatus(MatchStatus status) { this.status = status; }

    /**
     * Returns when the match was created.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Returns when the match was last updated.
     *
     * @return update timestamp
     */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /**
     * Returns "W", "D", or "L" from the perspective of the given team,
     * or {@code null} if scores are not yet recorded.
     *
     * @param teamId team id whose perspective should be used
     * @return {@code W}, {@code D}, {@code L}, or {@code null} when scores are missing
     */
    public String getOutcomeForTeam(Long teamId) {
        if (homeScore == null || awayScore == null) return null;
        boolean isHome = homeTeam.getId().equals(teamId);
        int scored   = isHome ? homeScore : awayScore;
        int conceded = isHome ? awayScore : homeScore;
        if (scored > conceded) return "W";
        if (scored < conceded) return "L";
        return "D";
    }
}
