package il.ac.jmc.matchday.controller.support;

import il.ac.jmc.matchday.entity.Position;
import il.ac.jmc.matchday.service.LeagueService;
import il.ac.jmc.matchday.service.TeamService;
import il.ac.jmc.matchday.util.Selections;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.List;

/**
 * Populates the player-list filter attributes shared by the public and admin
 * player list pages.
 */
@Component
public class PlayerListModelBuilder {

    /** League lookup service. */
    private final LeagueService leagueService;
    /** Team lookup service. */
    private final TeamService teamService;

    /**
     * Creates the builder with league and team lookup services.
     *
     * @param leagueService league lookup service
     * @param teamService team lookup service
     */
    public PlayerListModelBuilder(LeagueService leagueService, TeamService teamService) {
        this.leagueService = leagueService;
        this.teamService = teamService;
    }

    /**
     * Adds the shared filter attributes and returns the effective team-id filter
     * (selected teams intersected with the selected leagues' teams).
     *
     * @param model MVC model to populate
     * @param name optional player name filter
     * @param leagueIds optional selected league ids
     * @param teamIds optional selected team ids
     * @param positions optional selected positions
     * @return effective team ids after applying league filters
     */
    public List<Long> populate(Model model, String name,
                               List<Long> leagueIds, List<Long> teamIds, List<Position> positions) {
        model.addAttribute("leagues", leagueService.findAll());
        model.addAttribute("teams", teamService.findAll());
        model.addAttribute("positions", Position.values());
        model.addAttribute("selectedLeagueIds", Selections.toSet(leagueIds));
        model.addAttribute("selectedTeamIds", Selections.toSet(teamIds));
        model.addAttribute("selectedPositions", Selections.toSet(positions));
        model.addAttribute("searchQuery", name);
        return leagueService.effectiveTeamIdsForLeagueFilter(leagueIds, teamIds);
    }
}
