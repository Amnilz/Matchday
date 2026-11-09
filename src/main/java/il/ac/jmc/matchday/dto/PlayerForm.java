package il.ac.jmc.matchday.dto;

import il.ac.jmc.matchday.entity.Player;
import il.ac.jmc.matchday.entity.Position;
import jakarta.validation.constraints.*;

/**
 * Form DTO for creating and editing a {@link Player}.
 */
public class PlayerForm {

    /**
     * Creates an empty player form for MVC binding.
     */
    public PlayerForm() {}

    /** Maximum player full-name length. */
    public static final int FULL_NAME_MAX    = Player.FULL_NAME_MAX;

    /** Maximum player photo URL length. */
    public static final int PHOTO_URL_MAX    = Player.PHOTO_URL_MAX;

    /** Lowest valid shirt number. */
    public static final int SHIRT_NUMBER_MIN = 1;

    /** Highest valid shirt number. */
    public static final int SHIRT_NUMBER_MAX = 99;

    /** Submitted player full name. */
    @NotBlank(message = "Full name is required")
    @Size(max = FULL_NAME_MAX, message = "Name must be " + FULL_NAME_MAX + " characters or fewer")
    private String fullName;

    /** Id of the selected team. */
    @NotNull(message = "Team is required")
    private Long teamId;

    /** Selected playing position. */
    @NotNull(message = "Position is required")
    private Position position;

    /** Submitted squad shirt number. */
    @NotNull(message = "Shirt number is required")
    @Min(value = SHIRT_NUMBER_MIN, message = "Shirt number must be at least " + SHIRT_NUMBER_MIN)
    @Max(value = SHIRT_NUMBER_MAX, message = "Shirt number must be at most " + SHIRT_NUMBER_MAX)
    private Integer shirtNumber;

    /** Submitted player photo URL. */
    @Size(max = PHOTO_URL_MAX, message = "Photo URL must be " + PHOTO_URL_MAX + " characters or fewer")
    private String photoUrl;

    /** Whether the player is active. */
    private boolean active = true;

    /**
     * Returns the submitted full name.
     *
     * @return player full name
     */
    public String getFullName() { return fullName; }

    /**
     * Sets the submitted full name.
     *
     * @param fullName player full name
     */
    public void setFullName(String fullName) { this.fullName = fullName; }

    /**
     * Returns the selected team id.
     *
     * @return team id
     */
    public Long getTeamId() { return teamId; }

    /**
     * Sets the selected team id.
     *
     * @param teamId team id
     */
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    /**
     * Returns the selected player position.
     *
     * @return player position
     */
    public Position getPosition() { return position; }

    /**
     * Sets the selected player position.
     *
     * @param position player position
     */
    public void setPosition(Position position) { this.position = position; }

    /**
     * Returns the submitted shirt number.
     *
     * @return shirt number
     */
    public Integer getShirtNumber() { return shirtNumber; }

    /**
     * Sets the submitted shirt number.
     *
     * @param shirtNumber shirt number
     */
    public void setShirtNumber(Integer shirtNumber) { this.shirtNumber = shirtNumber; }

    /**
     * Returns the submitted photo URL.
     *
     * @return photo URL
     */
    public String getPhotoUrl() { return photoUrl; }

    /**
     * Sets the submitted photo URL.
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
}
