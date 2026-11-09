package il.ac.jmc.matchday.controller.support;

import il.ac.jmc.matchday.dto.MatchResultFilter;
import il.ac.jmc.matchday.dto.MatchSearchCriteria;
import il.ac.jmc.matchday.entity.MatchStatus;
import il.ac.jmc.matchday.service.LeagueService;
import il.ac.jmc.matchday.service.MatchService;
import il.ac.jmc.matchday.service.TeamService;
import il.ac.jmc.matchday.util.Selections;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * Populates match-list filter attributes shared by public and admin match pages.
 */
@Component
public class MatchSearchModelBuilder {

    /** Match search service. */
    private final MatchService matchService;
    /** Team lookup service. */
    private final TeamService  teamService;
    /** League lookup service. */
    private final LeagueService leagueService;

    /**
     * Creates the builder with services used by match list filters.
     *
     * @param matchService match search service
     * @param teamService team lookup service
     * @param leagueService league lookup service
     */
    public MatchSearchModelBuilder(MatchService matchService,
                                   TeamService teamService,
                                   LeagueService leagueService) {
        this.matchService = matchService;
        this.teamService  = teamService;
        this.leagueService = leagueService;
    }

    /**
     * Adds match list results and filter option attributes to the model.
     *
     * @param model MVC model to populate
     * @param criteria active match search criteria
     * @param page zero-based page number
     */
    public void populate(Model model, MatchSearchCriteria criteria, int page) {
        model.addAttribute("matches", matchService.findByCriteria(criteria, page));
        model.addAttribute("leagues", leagueService.findAll());
        model.addAttribute("teams", teamService.findAll());
        model.addAttribute("statuses", MatchStatus.values());
        model.addAttribute("resultFilters", MatchResultFilter.values());
        model.addAttribute("criteria", criteria);
        model.addAttribute("selectedLeagueIds", Selections.toSet(criteria.leagueIds()));
        model.addAttribute("selectedTeamIds", Selections.toSet(criteria.teamIds()));
        model.addAttribute("selectedStatuses", Selections.toSet(criteria.statuses()));
        model.addAttribute("selectedResults", Selections.toSet(criteria.resultFilters()));
    }
}
