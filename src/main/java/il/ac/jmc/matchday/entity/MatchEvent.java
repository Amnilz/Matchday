package il.ac.jmc.matchday.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records a single event (goal, assist, card) by a player in a match.
 */
@Entity
@Table(name = "match_events")
public class MatchEvent {
    /** Database identifier. Generated automatically by the database when a new event is inserted.  */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Match where the event occurred. This is a many-to-one relationship because a match can have multiple events, but each event belongs to exactly one match. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    /** Player credited with the event. This is a many-to-one relationship because a player can have multiple events in a match (e.g., multiple goals), but each event is credited to exactly one player.  */
    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /**  Type of the event (goal, assist, card). Stored as a string in the database for readability and stability.  */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = MatchEventType.COLUMN_LENGTH)
    private MatchEventType type;

    /** Minute of the match when the event occurred. Null if unknown. */
    private Integer minute;

    /** Timestamp of when the event was created. Set automatically when the event is first persisted and never updated. */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** JPA-only constructor.  */
    protected MatchEvent() {}

    /**
     * Creates a match event.
     *
     * @param match match where the event occurred
     * @param player player credited with the event
     * @param type event type
     * @param minute match minute, or {@code null} when unknown
     */
    public MatchEvent(Match match, Player player, MatchEventType type, Integer minute) {
        this.match = match;
        this.player = player;
        this.type = type;
        this.minute = minute;
    }

    /**
     * Initializes the creation timestamp before the event is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Returns the database id.
     *
     * @return event id
     */
    public Long getId() { return id; }

    /**
     * Returns the match where the event occurred.
     *
     * @return match
     */
    public Match getMatch() { return match; }

    /**
     * Sets the match where the event occurred.
     *
     * @param match match
     */
    public void setMatch(Match match) { this.match = match; }

    /**
     * Returns the player credited with the event.
     *
     * @return player
     */
    public Player getPlayer() { return player; }

    /**
     * Sets the player credited with the event.
     *
     * @param player player
     */
    public void setPlayer(Player player) { this.player = player; }

    /**
     * Returns the event type.
     *
     * @return event type
     */
    public MatchEventType getType() { return type; }

    /**
     * Sets the event type.
     *
     * @param type event type
     */
    public void setType(MatchEventType type) { this.type = type; }

    /**
     * Returns the match minute.
     *
     * @return match minute, or {@code null} when unknown
     */
    public Integer getMinute() { return minute; }

    /**
     * Sets the match minute.
     *
     * @param minute match minute
     */
    public void setMinute(Integer minute) { this.minute = minute; }

    /**
     * Returns when the event was created.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() { return createdAt; }
}
