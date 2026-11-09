package il.ac.jmc.matchday.dto;

import il.ac.jmc.matchday.entity.League;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form DTO for creating and editing a league.
 * Max-length constants delegate to {@link League} so there is a single source of truth.
 */
public class LeagueForm {

    /**
     * Creates an empty league form for MVC binding.
     */
    public LeagueForm() {}

    /** Maximum league name length. */
    public static final int NAME_MAX        = League.NAME_MAX;

    /** Maximum league description length. */
    public static final int DESCRIPTION_MAX = League.DESCRIPTION_MAX;

    /** Maximum league logo URL length. */
    public static final int LOGO_URL_MAX    = League.LOGO_URL_MAX;

    /** Submitted league name. */
    @NotBlank(message = "League name is required")
    @Size(max = NAME_MAX, message = "Name must be " + NAME_MAX + " characters or fewer")
    private String name;

    /** Submitted league description. */
    @Size(max = DESCRIPTION_MAX, message = "Description must be " + DESCRIPTION_MAX + " characters or fewer")
    private String description;

    /** Submitted league logo URL. */
    @Size(max = LOGO_URL_MAX, message = "Logo URL must be " + LOGO_URL_MAX + " characters or fewer")
    private String logoUrl;

    /** Whether the league should be visible and usable. */
    private boolean active = true;

    /**
     * Returns the submitted league name.
     *
     * @return league name
     */
    public String getName() { return name; }

    /**
     * Sets the submitted league name.
     *
     * @param name league name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the submitted league description.
     *
     * @return league description
     */
    public String getDescription() { return description; }

    /**
     * Sets the submitted league description.
     *
     * @param description league description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Returns the submitted league logo URL.
     *
     * @return logo URL
     */
    public String getLogoUrl() { return logoUrl; }

    /**
     * Sets the submitted league logo URL.
     *
     * @param logoUrl logo URL
     */
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    /**
     * Returns whether the league should be visible/usable.
     *
     * @return true when active
     */
    public boolean isActive() { return active; }

    /**
     * Sets whether the league should be visible/usable.
     *
     * @param active true when active
     */
    public void setActive(boolean active) { this.active = active; }
}
