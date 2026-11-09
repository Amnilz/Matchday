package il.ac.jmc.matchday.controller;

import il.ac.jmc.matchday.entity.League;
import il.ac.jmc.matchday.service.LeagueService;
import il.ac.jmc.matchday.service.LeagueTableService;
import il.ac.jmc.matchday.util.ReturnUrlUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Public controller for the league table page.
 */
@Controller
public class LeagueTableController {

    /** Public leagues list path used as a safe return fallback. */
    private static final String LEAGUES_PATH = "/leagues";

    /** Service that calculates standings from completed matches. */
    private final LeagueTableService leagueTableService;
    /** League lookup service. */
    private final LeagueService leagueService;

    /**
     * Creates the controller with the services needed to choose and calculate a table.
     *
     * @param leagueTableService service that calculates standings from completed matches
     * @param leagueService league lookup service
     */
    public LeagueTableController(LeagueTableService leagueTableService,
                                 LeagueService leagueService) {
        this.leagueTableService = leagueTableService;
        this.leagueService = leagueService;
    }

    /**
     * Shows the standings table for a selected league, or the primary league when none is selected.
     *
     * @param leagueId optional league id to display
     * @param returnUrl optional return URL for links back to league pages
     * @param model MVC model populated for the table page
     * @return league table view
     */
    @GetMapping("/league-table")
    public String table(@RequestParam(required = false) Long leagueId,
                        @RequestParam(required = false) String returnUrl,
                        Model model) {
        League league = leagueId != null
                ? leagueService.getByIdOrThrow(leagueId)
                : leagueService.findPrimaryLeague().orElse(null);

        model.addAttribute("league", league);
        model.addAttribute("leagues", leagueService.findAll());
        model.addAttribute("selectedLeagueId", league != null ? league.getId() : null);
        model.addAttribute("returnUrl", ReturnUrlUtil.safeOrDefault(returnUrl, LEAGUES_PATH));
        model.addAttribute("rows", league != null
                ? leagueTableService.getTableForLeague(league.getId())
                : leagueTableService.getTable());
        return "league-table/index";
    }
}
