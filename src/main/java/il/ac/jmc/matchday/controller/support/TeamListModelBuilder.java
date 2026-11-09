package il.ac.jmc.matchday.controller.support;

import il.ac.jmc.matchday.service.LeagueService;
import il.ac.jmc.matchday.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.List;

/**
 * Populates the team-list attributes shared by the public and admin
 * team list pages.
 */
@Component
public class TeamListModelBuilder {

    /** Team lookup service. */
    private final TeamService teamService;
    /** League lookup service. */
    private final LeagueService leagueService;

    /**
     * Creates the builder with team and league lookup services.
     *
     * @param teamService team list service
     * @param leagueService league lookup service
     */
    public TeamListModelBuilder(TeamService teamService, LeagueService leagueService) {
        this.teamService = teamService;
        this.leagueService = leagueService;
    }

    /**
     * Adds team list results and filter option attributes to the model.
     *
     * @param model MVC model to populate
     * @param name optional team name filter
     * @param leagueIds optional selected league ids
     * @param page zero-based page number
     */
    public void populate(Model model, String name, List<Long> leagueIds, int page) {
        List<Long> selectedLeagueIds = leagueIds != null ? leagueIds : List.of();
        model.addAttribute("teams", teamService.findSummariesPage(name, selectedLeagueIds, page));
        model.addAttribute("searchQuery", name);
        model.addAttribute("leagues", leagueService.findAll());
        model.addAttribute("selectedLeagueIds", selectedLeagueIds);
    }
}
