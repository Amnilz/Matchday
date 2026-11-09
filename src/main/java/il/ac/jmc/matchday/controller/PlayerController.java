package il.ac.jmc.matchday.controller;

import il.ac.jmc.matchday.controller.support.PlayerListModelBuilder;
import il.ac.jmc.matchday.entity.Match;
import il.ac.jmc.matchday.entity.MatchEvent;
import il.ac.jmc.matchday.entity.MatchStatus;
import il.ac.jmc.matchday.entity.Player;
import il.ac.jmc.matchday.entity.Position;
import il.ac.jmc.matchday.exception.EntityNotFoundException;
import il.ac.jmc.matchday.service.MatchEventService;
import il.ac.jmc.matchday.service.MatchService;
import il.ac.jmc.matchday.service.PlayerService;
import il.ac.jmc.matchday.service.LeagueTableService;
import il.ac.jmc.matchday.util.ReturnUrlUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Public pages for browsing players: filterable/sortable list and player detail.
 */
@Controller
@RequestMapping("/players")
public class PlayerController {

    /** Thymeleaf view name for the players list page. */
    private static final String VIEW_LIST = "players/list";
    /** Thymeleaf fragment selector for the AJAX results partial. */
    private static final String VIEW_RESULTS_FRAGMENT = VIEW_LIST + " :: results";
    /** Public players list path. */
    private static final String LIST_PATH = "/players";

    /** Player lookup service. */
    private final PlayerService playerService;
    /** Match lookup service. */
    private final MatchService matchService;
    /** Match-event lookup service. */
    private final MatchEventService matchEventService;
    /** League table calculation service. */
    private final LeagueTableService leagueTableService;
    /** Shared player-list model builder. */
    private final PlayerListModelBuilder playerListModelBuilder;

    /**
     * Creates the controller with player, match, event and list-building services.
     *
     * @param playerService player lookup service
     * @param matchService match lookup service
     * @param matchEventService match-event lookup service
     * @param leagueTableService league table calculation service
     * @param playerListModelBuilder shared player-list model builder
     */
    public PlayerController(PlayerService playerService,
                            MatchService matchService,
                            MatchEventService matchEventService,
                            LeagueTableService leagueTableService,
                            PlayerListModelBuilder playerListModelBuilder) {
        this.playerService = playerService;
        this.matchService = matchService;
        this.matchEventService = matchEventService;
        this.leagueTableService = leagueTableService;
        this.playerListModelBuilder = playerListModelBuilder;
    }

    /**
     * Shows the filterable, stat-sortable players list.
     *
     * @param name optional player name filter
     * @param leagueIds optional league filters
     * @param teamIds optional team filters
     * @param positions optional position filters
     * @param sortBy optional statistic sort key
     * @param dir sort direction
     * @param page zero-based page number
     * @param fragment whether to return only the results fragment
     * @param model MVC model populated for the list
     * @return full list view or results fragment
     */
    @GetMapping
    public String list(@RequestParam(required = false) String name,
                       @RequestParam(value = "leagueId", required = false) List<Long> leagueIds,
                       @RequestParam(value = "teamId",   required = false) List<Long>     teamIds,
                       @RequestParam(value = "position", required = false) List<Position>  positions,
                       @RequestParam(required = false) String sortBy,
                       @RequestParam(defaultValue = "desc") String dir,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "false") boolean fragment,
                       HttpServletRequest request,
                       Model model) {
        List<Long> effectiveTeamIds = playerListModelBuilder.populate(model, name, leagueIds, teamIds, positions);

        model.addAttribute("players", playerService.listFiltered(name, effectiveTeamIds, positions, sortBy, dir, page));
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("dir", dir);
        model.addAttribute("returnUrl", ReturnUrlUtil.currentRequestUrl(request));
        return fragment ? VIEW_RESULTS_FRAGMENT : VIEW_LIST;
    }

    /**
     * Shows one active player with stats, team matches and match events.
     *
     * @param id player id
     * @param model MVC model populated for the detail page
     * @return player detail view
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(required = false) String returnUrl,
                         Model model) {
        Player player = playerService.findById(id)
                .filter(Player::isActive)
                .orElseThrow(() -> new EntityNotFoundException(Player.class));

        String currentUrl = LIST_PATH + "/" + id;
        Long teamId = player.getTeam().getId();
        List<Match> completedMatches = matchService.findByTeamIdAndStatus(teamId, MatchStatus.COMPLETED);
        List<Match> upcomingMatches  = matchService.findByTeamIdAndStatus(teamId, MatchStatus.SCHEDULED);
        List<MatchEvent> events = matchEventService.findByPlayerId(id);
        Map<Long, List<MatchEvent>> eventsByMatch = matchEventService.groupByMatchId(events);

        model.addAttribute("player", player);
        model.addAttribute("backUrl", ReturnUrlUtil.safeOrDefault(returnUrl, LIST_PATH));
        model.addAttribute("backLabel", ReturnUrlUtil.labelOrDefault(returnUrl, "Players"));
        model.addAttribute("returnUrl", currentUrl);
        model.addAttribute("completedMatches", completedMatches);
        model.addAttribute("upcomingMatches", upcomingMatches);
        model.addAttribute("teamStats", leagueTableService.getTeamStats(teamId));
        model.addAttribute("stats", matchEventService.computeStats(events));
        model.addAttribute("events", events);
        model.addAttribute("eventsByMatch", eventsByMatch);
        return "players/detail";
    }
}
