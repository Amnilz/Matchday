package il.ac.jmc.matchday.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * JPA entity representing a team saved by a registered user.
 */
@Entity
@Table(
        name = "favorite_teams",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorite_team_user_team",
                columnNames = {"user_id", "team_id"}
        )
)
public class FavoriteTeam {

    /** Database id of the favorite link. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User who saved the team as a favorite. Cannot be null. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Team that was saved as a favorite. Cannot be null. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** Timestamp of when the favorite was created. Set automatically when the favorite is first persisted, and cannot be updated. */
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * JPA-only constructor.
     */
    protected FavoriteTeam() {}

    /**
     * Creates a favorite-team link.
     *
     * @param user owner of the favorite
     * @param team favorited team
     */
    public FavoriteTeam(User user, Team team) {
        this.user = user;
        this.team = team;
    }

    /**
     * Initializes the creation timestamp before the favorite is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Returns the database id.
     *
     * @return favorite id
     */
    public Long getId() { return id; }

    /**
     * Returns the user who owns this favorite.
     *
     * @return favorite owner
     */
    public User getUser() { return user; }

    /**
     * Sets the user who owns this favorite.
     *
     * @param user favorite owner
     */
    public void setUser(User user) { this.user = user; }

    /**
     * Returns the favorited team.
     *
     * @return favorited team
     */
    public Team getTeam() { return team; }

    /**
     * Sets the favorited team.
     *
     * @param team favorited team
     */
    public void setTeam(Team team) { this.team = team; }

    /**
     * Returns when the favorite was created.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() { return createdAt; }
}
