package il.ac.jmc.matchday.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing a football competition.
 */
@Entity
@Table(name = "leagues")
public class League {

    /** Maximum league name length. */
    public static final int NAME_MAX        = 100;

    /** Maximum league description length. */
    public static final int DESCRIPTION_MAX = 1000;

    /** Maximum league logo URL length. */
    public static final int LOGO_URL_MAX    = 2048;

    /** Database identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** League name, unique and non-null. */
    @Column(unique = true, nullable = false, length = NAME_MAX)
    private String name;

    /** League description, optional and potentially long.  */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**  League logo URL, optional and potentially long.  */
    @Column(length = LOGO_URL_MAX)
    private String logoUrl;

    /** Whether the league is active and should be shown in listings. */
    @Column(nullable = false)
    private boolean active = true;

    /** When the league was created, set automatically and never updated.  */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** When the league was last updated, set automatically on create and update.  */
    private LocalDateTime updatedAt;

    /**
     * JPA-only constructor.
     */
    protected League() {}

    /**
     * Creates a league with the given name.
     *
     * @param name league name
     */
    public League(String name) {
        this.name = name;
    }

    /**
     * Initializes audit timestamps before the league is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    /**
     * Refreshes the update timestamp before the league is saved.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the database id.
     *
     * @return league id
     */
    public Long getId() { return id; }

    /**
     * Returns the league name.
     *
     * @return league name
     */
    public String getName() { return name; }

    /**
     * Sets the league name.
     *
     * @param name league name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the league description.
     *
     * @return league description
     */
    public String getDescription() { return description; }

    /**
     * Sets the league description.
     *
     * @param description league description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Returns the league logo URL.
     *
     * @return logo URL
     */
    public String getLogoUrl() { return logoUrl; }

    /**
     * Sets the league logo URL.
     *
     * @param logoUrl logo URL
     */
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    /**
     * Returns whether the league is active.
     *
     * @return true when active
     */
    public boolean isActive() { return active; }

    /**
     * Sets whether the league is active.
     *
     * @param active true when active
     */
    public void setActive(boolean active) { this.active = active; }

    /**
     * Returns when the league was created.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Returns when the league was last updated.
     *
     * @return update timestamp
     */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
