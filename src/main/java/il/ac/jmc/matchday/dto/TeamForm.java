package il.ac.jmc.matchday.dto;

import il.ac.jmc.matchday.entity.Team;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form DTO for creating and editing a {@link Team}.
 */
public class TeamForm {

    /**
     * Creates an empty team form for MVC binding.
     */
    public TeamForm() {}

    /** Maximum team name length. */
    public static final int NAME_MAX        = Team.NAME_MAX;

    /** Maximum team description length. */
    public static final int DESCRIPTION_MAX = Team.DESCRIPTION_MAX;

    /** Maximum team logo URL length. */
    public static final int LOGO_URL_MAX    = Team.LOGO_URL_MAX;

    /** Submitted team name. */
    @NotBlank(message = "Team name is required")
    @Size(max = NAME_MAX, message = "Name must be " + NAME_MAX + " characters or fewer")
    private String name;

    /** Submitted team description. */
    @Size(max = DESCRIPTION_MAX, message = "Description must be " + DESCRIPTION_MAX + " characters or fewer")
    private String description;

    /** Submitted team logo URL. */
    @Size(max = LOGO_URL_MAX, message = "Logo URL must be " + LOGO_URL_MAX + " characters or fewer")
    private String logoUrl;

    /** League id selected when creating from a league page. */
    private Long leagueId;

    /** Whether save should redirect back to the league page. */
    private boolean returnToLeague;

    /**
     * Returns the submitted team name.
     *
     * @return team name
     */
    public String getName() { return name; }

    /**
     * Sets the submitted team name.
     *
     * @param name team name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the submitted team description.
     *
     * @return team description
     */
    public String getDescription() { return description; }

    /**
     * Sets the submitted team description.
     *
     * @param description team description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Returns the submitted team logo URL.
     *
     * @return logo URL
     */
    public String getLogoUrl() { return logoUrl; }

    /**
     * Sets the submitted team logo URL.
     *
     * @param logoUrl logo URL
     */
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    /**
     * Returns the league id selected when creating from a league page.
     *
     * @return selected league id
     */
    public Long getLeagueId() { return leagueId; }

    /**
     * Sets the league id selected when creating from a league page.
     *
     * @param leagueId selected league id
     */
    public void setLeagueId(Long leagueId) { this.leagueId = leagueId; }

    /**
     * Returns whether save should redirect back to the league page.
     *
     * @return true to return to the league page
     */
    public boolean isReturnToLeague() { return returnToLeague; }

    /**
     * Sets whether save should redirect back to the league page.
     *
     * @param returnToLeague true to return to the league page
     */
    public void setReturnToLeague(boolean returnToLeague) { this.returnToLeague = returnToLeague; }
}
