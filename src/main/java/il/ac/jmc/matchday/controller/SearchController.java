package il.ac.jmc.matchday.controller;

import il.ac.jmc.matchday.controller.support.Routes;
import il.ac.jmc.matchday.dto.MatchResultFilter;
import il.ac.jmc.matchday.dto.MatchSearchCriteria;
import il.ac.jmc.matchday.entity.MatchStatus;
import il.ac.jmc.matchday.entity.Position;
import il.ac.jmc.matchday.dto.AdvancedSearchResults;
import il.ac.jmc.matchday.service.FavoriteTeamService;
import il.ac.jmc.matchday.service.LeagueService;
import il.ac.jmc.matchday.service.RecentSearchService;
import il.ac.jmc.matchday.service.SearchService;
import il.ac.jmc.matchday.service.TeamService;
import il.ac.jmc.matchday.util.PrincipalUtil;
import il.ac.jmc.matchday.util.ReturnUrlUtil;
import il.ac.jmc.matchday.util.Selections;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * Public advanced search controller for matches, players and teams.
 */
@Controller
@RequestMapping(Routes.SEARCH)
public class SearchController {

    /** Thymeleaf view name for the search index page. */
    private static final String VIEW_INDEX = "search/index";
    /** Thymeleaf fragment selector for the AJAX results partial. */
    private static final String VIEW_RESULTS_FRAGMENT = VIEW_INDEX + " :: results";

    /** Advanced search service. */
    private final SearchService searchService;
    /** Session-backed recent-search service. */
    private final RecentSearchService recentSearchService;
    /** Team lookup service. */
    private final TeamService teamService;
    /** Favorite-team lookup service. */
    private final FavoriteTeamService favoriteTeamService;
    /** League lookup service. */
    private final LeagueService leagueService;

    /**
     * Creates the controller with search, lookup, favorites and recent-search services.
     *
     * @param searchService advanced search service
     * @param recentSearchService session-backed recent-search service
     * @param teamService team lookup service
     * @param favoriteTeamService favorite-team lookup service
     * @param leagueService league lookup service
     */
    public SearchController(SearchService searchService,
                            RecentSearchService recentSearchService,
                            TeamService teamService,
                            FavoriteTeamService favoriteTeamService,
                            LeagueService leagueService) {
        this.searchService = searchService;
        this.recentSearchService = recentSearchService;
        this.teamService = teamService;
        this.favoriteTeamService = favoriteTeamService;
        this.leagueService = leagueService;
    }

    /**
     * Shows the advanced search page or results fragment.
     *
     * @param query optional text query
     * @param leagueIds optional league filters
     * @param teamIds optional team filters
     * @param statuses optional match status filters
     * @param resultFilters optional match result filters
     * @param positions optional player position filters
     * @param skipRemember whether to skip saving this request as a recent search
     * @param fragment whether to return only the results fragment
     * @param fromDate optional match start date
     * @param toDate optional match end date
     * @param request current HTTP request
     * @param principal current user principal, when logged in
     * @param model MVC model populated for the search page
     * @return full search view or results fragment
     */
    @GetMapping
    public String search(@RequestParam(name = "q", required = false) String query,
                         @RequestParam(value = "leagueId", required = false) List<Long> leagueIds,
                         @RequestParam(value = "teamId", required = false) List<Long> teamIds,
                         @RequestParam(value = "status", required = false) List<MatchStatus> statuses,
                         @RequestParam(value = "result", required = false) List<MatchResultFilter> resultFilters,
                         @RequestParam(value = "position", required = false) List<Position> positions,
                         @RequestParam(defaultValue = "false") boolean skipRemember,
                         @RequestParam(defaultValue = "false") boolean fragment,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                         HttpServletRequest request,
                         Principal principal,
                         Model model) {
        MatchSearchCriteria matchCriteria = new MatchSearchCriteria(
                query, leagueIds, teamIds, statuses, fromDate, toDate, resultFilters
        );

        String queryString = recentSearchService.cleanQueryString(request);
        AdvancedSearchResults results = searchService.searchAll(query, matchCriteria, positions, queryString);

        boolean shouldRemember = !fragment
                && !skipRemember
                && results.hasSearch()
                && searchService.shouldRemember(query, matchCriteria, positions);
        if (shouldRemember) {
            recentSearchService.rememberSearch(searchService.buildRecentSearch(
                    query, matchCriteria, positions, queryString));
        }

        model.addAttribute("query", query);
        model.addAttribute("leagues", leagueService.findAll());
        model.addAttribute("teams", teamService.findAll());
        model.addAttribute("statuses", MatchStatus.values());
        model.addAttribute("resultFilters", MatchResultFilter.values());
        model.addAttribute("positions", Position.values());
        model.addAttribute("criteria", matchCriteria);
        model.addAttribute("selectedLeagueIds", Selections.toSet(leagueIds));
        model.addAttribute("selectedTeamIds", Selections.toSet(teamIds));
        model.addAttribute("selectedStatuses", Selections.toSet(statuses));
        model.addAttribute("selectedResults", Selections.toSet(resultFilters));
        model.addAttribute("selectedPositions", Selections.toSet(positions));
        model.addAttribute("favoriteTeamIds", favoriteTeamService.findFavoriteTeamIdsOrEmpty(
                PrincipalUtil.nameOrNull(principal)));
        model.addAttribute("matches", results.matches());
        model.addAttribute("players", results.players());
        model.addAttribute("teamResults", results.teams());
        model.addAttribute("hasSearch", results.hasSearch());
        model.addAttribute("recentSearches", recentSearchService.findRecentSearches());
        model.addAttribute("returnUrl", recentSearchService.buildReturnUrl(queryString));
        return fragment ? VIEW_RESULTS_FRAGMENT : VIEW_INDEX;
    }

    /**
     * Removes one saved recent search from the session.
     *
     * @param queryString query string identifying the recent search
     * @param returnUrl optional return URL
     * @return redirect back to the safe return URL
     */
    @PostMapping("/recent/delete")
    public String deleteRecentSearch(@RequestParam String queryString,
                                     @RequestParam(defaultValue = Routes.SEARCH) String returnUrl) {
        recentSearchService.forgetSearch(queryString);
        return Routes.REDIRECT_PREFIX + recentSearchService.appendSkipRemember(safeReturnUrl(returnUrl));
    }

    /**
     * Clears all saved recent searches from the session.
     *
     * @param returnUrl optional return URL
     * @return redirect back to the safe return URL
     */
    @PostMapping("/recent/clear")
    public String clearRecentSearches(@RequestParam(defaultValue = Routes.SEARCH) String returnUrl) {
        recentSearchService.clearSearches();
        return Routes.REDIRECT_PREFIX + recentSearchService.appendSkipRemember(safeReturnUrl(returnUrl));
    }

    /**
     * Returns the given URL when it is safe, or falls back to the search page.
     *
     * @param returnUrl candidate return URL
     * @return safe redirect target
     */
    private String safeReturnUrl(String returnUrl) {
        return ReturnUrlUtil.isSafe(returnUrl) ? returnUrl : Routes.SEARCH;
    }
}
