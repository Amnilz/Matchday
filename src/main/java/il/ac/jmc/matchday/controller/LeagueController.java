package il.ac.jmc.matchday.controller;

import il.ac.jmc.matchday.entity.League;
import il.ac.jmc.matchday.entity.MatchStatus;
import il.ac.jmc.matchday.service.FavoriteTeamService;
import il.ac.jmc.matchday.service.LeagueService;
import il.ac.jmc.matchday.service.LeagueTableService;
import il.ac.jmc.matchday.service.MatchService;
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

/**
 * Public pages for browsing leagues: list, detail and per-league table.
 */
@Controller
@RequestMapping("/leagues")
public class LeagueController {

    /** Number of table rows shown in the league detail preview. */
    private static final int TOP_TABLE_ROWS_LIMIT = 5;
    /** Number of recent results shown in the league detail page. */
    private static final int RECENT_RESULTS_LIMIT = 5;

    /** Thymeleaf view name for the leagues list page. */
    private static final String VIEW_LIST = "leagues/list";
    /** Thymeleaf fragment selector for the AJAX results partial. */
    private static final String VIEW_RESULTS_FRAGMENT = VIEW_LIST + " :: results";
    /** Public leagues list path. */
    private static final String LIST_PATH = "/leagues";

    /** League lookup service. */
    private final LeagueService leagueService;
    /** League table calculation service. */
    private final LeagueTableService leagueTableService;
    /** Match lookup service. */
    private final MatchService matchService;
    /** Favorite-team lookup service. */
    private final FavoriteTeamService favoriteTeamService;

    /**
     * Creates the controller with league browsing collaborators.
     *
     * @param leagueService league lookup service
     * @param leagueTableService league table calculation service
     * @param matchService match lookup service
     * @param favoriteTeamService favorite-team lookup service
     */
    public LeagueController(LeagueService leagueService,
                            LeagueTableService leagueTableService,
                            MatchService matchService,
                            FavoriteTeamService favoriteTeamService) {
        this.leagueService = leagueService;
        this.leagueTableService = leagueTableService;
        this.matchService = matchService;
        this.favoriteTeamService = favoriteTeamService;
    }

    /**
     * Shows the searchable, paginated leagues list.
     *
     * @param name optional league name filter
     * @param page zero-based page number
     * @param fragment whether to return only the results fragment
     * @param model MVC model populated for the list
     * @return full list view or results fragment
     */
    @GetMapping
    public String list(@RequestParam(required = false) String name,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "false") boolean fragment,
                       HttpServletRequest request,
                       Model model) {
        model.addAttribute("leagues", leagueService.findBySearchPage(name, page));
        model.addAttribute("searchQuery", name);
        model.addAttribute("returnUrl", ReturnUrlUtil.currentRequestUrl(request));
        return fragment ? VIEW_RESULTS_FRAGMENT : VIEW_LIST;
    }

    /**
     * Shows one league with table preview, member teams, upcoming matches and recent results.
     *
     * @param id league id
     * @param principal current user principal, when logged in
     * @param request current HTTP request
     * @param model MVC model populated for the detail page
     * @return league detail view
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(required = false) String returnUrl,
                         Principal principal,
                         HttpServletRequest request,
                         Model model) {
        League league = leagueService.getByIdOrThrow(id);
        model.addAttribute("league", league);
        model.addAttribute("backUrl", ReturnUrlUtil.safeOrDefault(returnUrl, LIST_PATH));
        model.addAttribute("teams", leagueService.findTeams(id));
        model.addAttribute("rows", leagueTableService.getTableForLeague(id).stream()
                .limit(TOP_TABLE_ROWS_LIMIT).toList());
        model.addAttribute("upcomingMatches", matchService.findByLeagueIdAndStatus(id, MatchStatus.SCHEDULED));
        model.addAttribute("recentResults", matchService.findRecentCompletedByLeague(id, RECENT_RESULTS_LIMIT));
        model.addAttribute("favoriteTeamIds", favoriteTeamService.findFavoriteTeamIdsOrEmpty(
                PrincipalUtil.nameOrNull(principal)));
        model.addAttribute("returnUrl", ReturnUrlUtil.currentRequestUrl(request));
        return "leagues/detail";
    }

    /**
     * Shows the full standings table for one league.
     *
     * @param id league id
     * @param returnUrl optional return URL for the league detail breadcrumb
     * @param model MVC model populated for the table page
     * @return league table view
     */
    @GetMapping("/{id}/table")
    public String table(@PathVariable Long id,
                        @RequestParam(required = false) String returnUrl,
                        Model model) {
        League league = leagueService.getByIdOrThrow(id);
        model.addAttribute("league", league);
        model.addAttribute("leagues", leagueService.findAll());
        model.addAttribute("selectedLeagueId", id);
        model.addAttribute("returnUrl", ReturnUrlUtil.safeOrDefault(returnUrl, LIST_PATH));
        model.addAttribute("rows", leagueTableService.getTableForLeague(id));
        return "league-table/index";
    }
}
