package il.ac.jmc.matchday.controller;

import il.ac.jmc.matchday.entity.League;
import il.ac.jmc.matchday.entity.MatchEventType;
import il.ac.jmc.matchday.service.LeagueService;
import il.ac.jmc.matchday.service.LeagueTableService;
import il.ac.jmc.matchday.service.MatchService;
import il.ac.jmc.matchday.service.PlayerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Controller that handles requests for the application home page.
 */
@Controller
public class HomeController {

    /** Number of upcoming and recent matches shown on the home page. */
    private static final int HOME_MATCH_LIMIT = 5;
    /** Number of teams shown in the top-league table preview. */
    private static final int TOP_TEAMS_LIMIT  = 3;
    /** Number of players shown in each stat ranking list. */
    private static final int TOP_STATS_LIMIT  = 5;

    /** Match lookup service. */
    private final MatchService matchService;
    /** League table calculation service. */
    private final LeagueTableService leagueTableService;
    /** League lookup service. */
    private final LeagueService leagueService;
    /** Player lookup and statistics service. */
    private final PlayerService playerService;

    /**
     * Creates the controller with services needed to assemble the home page.
     *
     * @param matchService match lookup service
     * @param leagueTableService league table calculation service
     * @param leagueService league lookup service
     * @param playerService player lookup and statistics service
     */
    public HomeController(MatchService matchService,
                          LeagueTableService leagueTableService,
                          LeagueService leagueService,
                          PlayerService playerService) {
        this.matchService = matchService;
        this.leagueTableService = leagueTableService;
        this.leagueService = leagueService;
        this.playerService = playerService;
    }

    /**
     * Renders the league home page.
     *
     * @param model MVC model populated for the home page
     * @return home view
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("returnUrl", "/");
        model.addAttribute("upcomingMatches", matchService.findUpcoming(HOME_MATCH_LIMIT));
        model.addAttribute("recentResults",   matchService.findRecentCompleted(HOME_MATCH_LIMIT));
        League topLeague = leagueService.findRandomLeague().orElse(null);
        model.addAttribute("topLeague", topLeague);
        model.addAttribute("topRows", topLeague != null
                ? leagueTableService.getTableForLeague(topLeague.getId()).stream()
                        .limit(TOP_TEAMS_LIMIT).toList()
                : List.of());
        model.addAttribute("topScorers",   playerService.getTopByType(MatchEventType.GOAL,        TOP_STATS_LIMIT));
        model.addAttribute("topAssisters", playerService.getTopByType(MatchEventType.ASSIST,      TOP_STATS_LIMIT));
        model.addAttribute("topYellows",   playerService.getTopByType(MatchEventType.YELLOW_CARD, TOP_STATS_LIMIT));
        model.addAttribute("topReds",      playerService.getTopByType(MatchEventType.RED_CARD,    TOP_STATS_LIMIT));
        return "home/index";
    }
}
