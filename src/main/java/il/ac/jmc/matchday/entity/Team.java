package il.ac.jmc.matchday.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing a football team in the league.
 */
@Entity
@Table(name = "teams")
public class Team {

    /** Maximum team name length. */
    public static final int NAME_MAX        = 100;

    /** Maximum team description length. */
    public static final int DESCRIPTION_MAX = 1000;

    /** Maximum team logo URL length. */
    public static final int LOGO_URL_MAX    = 2048;

    /** Database identifier. Generated automatically by the database when a new team is inserted. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Team name. Must be unique and non-null, with a maximum length defined by {@link #NAME_MAX}. */
    @Column(unique = true, nullable = false, length = NAME_MAX)
    private String name;

    /** Team description. Optional, with a maximum length defined by {@link #DESCRIPTION_MAX}.  */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Team logo URL. Optional, with a maximum length defined by {@link #LOGO_URL_MAX}.  */
    @Column(length = LOGO_URL_MAX)
    private String logoUrl;

    /** Timestamp of when the team was created. Set automatically when the team is first persisted and never updated. */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp of when the team was last updated. Set automatically when the team is first persisted and updated whenever the team is saved.  */
    private LocalDateTime updatedAt;

    /**
     * JPA-only constructor.
     */
    protected Team() {}

    /**
     * Creates a team with the given name.
     *
     * @param name team name
     */
    public Team(String name) {
        this.name = name;
    }

    /**
     * Initializes audit timestamps before the team is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    /**
     * Refreshes the update timestamp before the team is saved.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the database id.
     *
     * @return team id
     */
    public Long getId() { return id; }

    /**
     * Returns the team name.
     *
     * @return team name
     */
    public String getName() { return name; }

    /**
     * Sets the team name.
     *
     * @param name team name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the team description.
     *
     * @return team description
     */
    public String getDescription() { return description; }

    /**
     * Sets the team description.
     *
     * @param description team description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Returns the team logo URL.
     *
     * @return logo URL
     */
    public String getLogoUrl() { return logoUrl; }

    /**
     * Sets the team logo URL.
     *
     * @param logoUrl logo URL
     */
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    /**
     * Returns when the team was created.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Returns when the team was last updated.
     *
     * @return update timestamp
     */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
