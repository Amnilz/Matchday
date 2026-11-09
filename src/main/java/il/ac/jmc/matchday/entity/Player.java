package il.ac.jmc.matchday.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing a player belonging to a team.
 * Shirt number is unique within each team.
 */
@Entity
@Table(name = "players",
       uniqueConstraints = @UniqueConstraint(columnNames = {"shirt_number", "team_id"}))
public class Player {

    /** Maximum player full-name length. */
    public static final int FULL_NAME_MAX = 100;

    /** Maximum player photo URL length. */
    public static final int PHOTO_URL_MAX = 2048;

    /**  Database identifier. Generated automatically by the database when a new player is inserted. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Player's full name. Cannot be null and has a maximum length defined by {@link #FULL_NAME_MAX}.   */
    @Column(nullable = false, length = FULL_NAME_MAX)
    private String fullName;

    /**  Player's position on the field. Stored as a string in the database for readability and stability. Cannot be null.   */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = Position.COLUMN_LENGTH)
    private Position position;

    /** Player's shirt number. Must be unique within the player's team. Nullable to allow players without assigned numbers. */
    @Column(name = "shirt_number")
    private Integer shirtNumber;

    /** Player's photo URL. Optional and has a maximum length defined by {@link #PHOTO_URL_MAX}.  */
    @Column(length = PHOTO_URL_MAX)
    private String photoUrl;

    /** Whether the player is currently active. Defaults to true. Cannot be null. */
    @Column(nullable = false)
    private boolean active = true;

    /** Player's team. This is a many-to-one relationship because a team can have multiple players, but each player belongs to exactly one team. The association is mandatory, so the team cannot be null.  */
    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** Timestamp when the player was created. Set automatically when the player is first persisted and never updated. */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the player was last updated. Set automatically whenever the player is updated.  */
    private LocalDateTime updatedAt;

    /**
     * JPA-only constructor.
     */
    protected Player() {}

    /**
     * Creates a player for a team.
     *
     * @param fullName player's full name
     * @param position player's position
     * @param team player's team
     */
    public Player(String fullName, Position position, Team team) {
        this.fullName = fullName;
        this.position = position;
        this.team = team;
    }

    /**
     * Initializes audit timestamps before the player is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    /**
     * Refreshes the update timestamp before the player is saved.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the database id.
     *
     * @return player id
     */
    public Long getId() { return id; }

    /**
     * Returns the player's full name.
     *
     * @return full name
     */
    public String getFullName() { return fullName; }

    /**
     * Sets the player's full name.
     *
     * @param fullName full name
     */
    public void setFullName(String fullName) { this.fullName = fullName; }

    /**
     * Returns the player's position.
     *
     * @return position
     */
    public Position getPosition() { return position; }

    /**
     * Sets the player's position.
     *
     * @param position position
     */
    public void setPosition(Position position) { this.position = position; }

    /**
     * Returns the player's shirt number.
     *
     * @return shirt number
     */
    public Integer getShirtNumber() { return shirtNumber; }

    /**
     * Sets the player's shirt number.
     *
     * @param shirtNumber shirt number
     */
    public void setShirtNumber(Integer shirtNumber) { this.shirtNumber = shirtNumber; }

    /**
     * Returns the player's photo URL.
     *
     * @return photo URL
     */
    public String getPhotoUrl() { return photoUrl; }

    /**
     * Sets the player's photo URL.
     *
     * @param photoUrl photo URL
     */
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    /**
     * Returns whether the player is active.
     *
     * @return true when active
     */
    public boolean isActive() { return active; }

    /**
     * Sets whether the player is active.
     *
     * @param active true when active
     */
    public void setActive(boolean active) { this.active = active; }

    /**
     * Returns the player's team.
     *
     * @return team
     */
    public Team getTeam() { return team; }

    /**
     * Sets the player's team.
     *
     * @param team team
     */
    public void setTeam(Team team) { this.team = team; }

    /**
     * Returns when the player was created.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Returns when the player was last updated.
     *
     * @return update timestamp
     */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
