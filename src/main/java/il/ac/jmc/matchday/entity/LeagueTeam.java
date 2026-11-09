package il.ac.jmc.matchday.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Membership of a team in a league. A team may participate in more than one league.
 */
@Entity
@Table(name = "league_teams",
       uniqueConstraints = @UniqueConstraint(columnNames = {"league_id", "team_id"}))
public class LeagueTeam {

    /** Database identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** League that contains the team.  */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    /** Team that is a member of the league. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** When the membership was created. This is set automatically when the membership is first persisted and is not updatable.  */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * JPA-only constructor.
     */
    protected LeagueTeam() {}

    /**
     * Creates a league-team membership.
     *
     * @param league league that contains the team
     * @param team member team
     */
    public LeagueTeam(League league, Team team) {
        this.league = league;
        this.team = team;
    }

    /**
     * Initializes the creation timestamp before the membership is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Returns the database id.
     *
     * @return membership id
     */
    public Long getId() { return id; }

    /**
     * Returns the league side of the membership.
     *
     * @return league
     */
    public League getLeague() { return league; }

    /**
     * Returns the team side of the membership.
     *
     * @return team
     */
    public Team getTeam() { return team; }

    /**
     * Returns when the membership was created.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() { return createdAt; }
}
