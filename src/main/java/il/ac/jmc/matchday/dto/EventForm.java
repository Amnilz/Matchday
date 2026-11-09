package il.ac.jmc.matchday.dto;

import il.ac.jmc.matchday.entity.MatchEventType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Form DTO for adding a single match event (goal, assist, card) to a match.
 */
public class EventForm {

    /** Earliest valid match event minute. */
    public static final int MINUTE_MIN = 1;

    /** Latest valid match event minute, including stoppage-time allowance. */
    public static final int MINUTE_MAX = 120;

    /**
     * Creates an empty event form for MVC binding.
     */
    public EventForm() {}

    /** Id of the player involved in the event. */
    @NotNull(message = "Player is required")
    private Long playerId;

    /** Type of the match event. */
    @NotNull(message = "Event type is required")
    private MatchEventType type;

    /** Minute at which the event occurred. */
    @NotNull(message = "Minute is required")
    @Min(value = MINUTE_MIN, message = "Minute must be at least " + MINUTE_MIN)
    @Max(value = MINUTE_MAX, message = "Minute must be at most " + MINUTE_MAX)
    private Integer minute;

    /**
     * Returns the player selected for the event.
     *
     * @return selected player id
     */
    public Long getPlayerId() { return playerId; }

    /**
     * Sets the player selected for the event.
     *
     * @param playerId selected player id
     */
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

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
     * Returns the event minute.
     *
     * @return event minute
     */
    public Integer getMinute() { return minute; }

    /**
     * Sets the event minute.
     *
     * @param minute event minute
     */
    public void setMinute(Integer minute) { this.minute = minute; }
}
