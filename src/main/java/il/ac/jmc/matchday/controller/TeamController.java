package il.ac.jmc.matchday.controller;

import il.ac.jmc.matchday.controller.support.Routes;
import il.ac.jmc.matchday.controller.support.TeamListModelBuilder;
import il.ac.jmc.matchday.entity.MatchStatus;
import il.ac.jmc.matchday.entity.Team;
import il.ac.jmc.matchday.service.FavoriteTeamService;
import il.ac.jmc.matchday.service.LeagueService;
import il.ac.jmc.matchday.service.LeagueTableService;
import il.ac.jmc.matchday.service.MatchService;
import il.ac.jmc.matchday.service.PlayerService;
import il.ac.jmc.matchday.service.SessionHistoryService;
import il.ac.jmc.matchday.service.TeamService;
import il.ac.jmc.matchday.util.PrincipalUtil;
import il.ac.jmc.matchday.util.ReturnUrlUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

/**
 * Public pages for browsing teams: searchable list and team detail with squad and results.
 */
@Controller
@RequestMapping(Routes.TEAMS)
public class TeamController {

    /** Thymeleaf view name for the teams list page. */
    private static final String VIEW_LIST = "teams/list";
    /** Thymeleaf fragment selector for the AJAX results partial. */
    private static final String VIEW_RESULTS_FRAGMENT = VIEW_LIST + " :: results";

    /** Team lookup service. */
    private final TeamService teamService;
    /** Player lookup service. */
    private final PlayerService playerService;
    /** Match lookup service. */
    private final MatchService matchService;
    /** League lookup service. */
    private final LeagueService leagueService;
    /** League table calculation service. */
    private final LeagueTableService leagueTableService;
    /** Favorite-team lookup service. */
    private final FavoriteTeamService favoriteTeamService;
    /** Session history service. */
    private final SessionHistoryService sessionHistoryService;
    /** Shared team-list model builder. */
    private final TeamListModelBuilder teamListModelBuilder;

    /**
     * Creates the controller with services needed for team browsing pages.
     *
     * @param teamService team lookup service
     * @param playerService player lookup service
     * @param matchService match lookup service
     * @param leagueService league lookup service
     * @param leagueTableService league table calculation service
     * @param favoriteTeamService favorite-team lookup service
     * @param sessionHistoryService session history service
     * @param teamListModelBuilder shared team-list model builder
     */
    public TeamController(TeamService teamService,
                          PlayerService playerService,
                          MatchService matchService,
                          LeagueService leagueService,
                          LeagueTableService leagueTableService,
                          FavoriteTeamService favoriteTeamService,
                          SessionHistoryService sessionHistoryService,
                          TeamListModelBuilder teamListModelBuilder) {
        this.teamService = teamService;
        this.playerService = playerService;
        this.matchService = matchService;
        this.leagueService = leagueService;
        this.leagueTableService = leagueTableService;
        this.favoriteTeamService = favoriteTeamService;
        this.sessionHistoryService = sessionHistoryService;
        this.teamListModelBuilder = teamListModelBuilder;
    }

    /**
     * Shows the searchable, paginated teams list.
     *
     * @param name optional team name filter
     * @param leagueIds optional league filters
     * @param page zero-based page number
     * @param fragment whether to return only the results fragment
     * @param principal current user principal, when logged in
     * @param request current HTTP request
     * @param model MVC model populated for the list
     * @return full list view or results fragment
     */
    @GetMapping
    public String list(@RequestParam(required = false) String name,
                       @RequestParam(value = "leagueId", required = false) List<Long> leagueIds,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "false") boolean fragment,
                       Principal principal,
                       HttpServletRequest request,
                       Model model) {
        teamListModelBuilder.populate(model, name, leagueIds, page);
        model.addAttribute("favoriteTeamIds", favoriteTeamService.findFavoriteTeamIdsOrEmpty(
                PrincipalUtil.nameOrNull(principal)));
        model.addAttribute("returnUrl", ReturnUrlUtil.currentRequestUrl(request));
        return fragment ? VIEW_RESULTS_FRAGMENT : VIEW_LIST;
    }

    /**
     * Shows one team with squad, leagues, stats, upcoming matches and results.
     *
     * @param id team id
     * @param principal current user principal, when logged in
     * @param model MVC model populated for the detail page
     * @return team detail view
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(required = false) String returnUrl,
                         Principal principal,
                         Model model) {
        Team team = teamService.getByIdOrThrow(id);
        sessionHistoryService.rememberTeam(team);

        String currentUrl = Routes.TEAMS + "/" + id;
        model.addAttribute("team", team);
        model.addAttribute("backUrl", ReturnUrlUtil.safeOrDefault(returnUrl, Routes.TEAMS));
        model.addAttribute("backLabel", ReturnUrlUtil.labelOrDefault(returnUrl, "Teams"));
        model.addAttribute("leagues", leagueService.findLeaguesForTeam(id));
        model.addAttribute("players", playerService.findActiveByTeamId(id));
        model.addAttribute("completedMatches", matchService.findByTeamIdAndStatus(id, MatchStatus.COMPLETED));
        model.addAttribute("upcomingMatches", matchService.findByTeamIdAndStatus(id, MatchStatus.SCHEDULED));
        model.addAttribute("stats", leagueTableService.getTeamStats(id));
        model.addAttribute("favorite", principal != null && favoriteTeamService.isFavorite(principal.getName(), id));
        model.addAttribute("returnUrl", currentUrl);
        return "teams/detail";
    }

}
