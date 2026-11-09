package il.ac.jmc.matchday.controller.admin;

import il.ac.jmc.matchday.dto.WikiStatusResponse;
import il.ac.jmc.matchday.entity.MatchStatus;
import il.ac.jmc.matchday.service.CommentService;
import il.ac.jmc.matchday.service.LeagueService;
import il.ac.jmc.matchday.service.MatchService;
import il.ac.jmc.matchday.service.PlayerService;
import il.ac.jmc.matchday.service.TeamService;
import il.ac.jmc.matchday.service.UserService;
import il.ac.jmc.matchday.service.WikipediaLogoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Admin dashboard and lightweight status endpoints.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    /** Service for managing teams and their stats. */
    private final TeamService          teamService;
    /** Service for managing leagues and their standings. */
    private final LeagueService        leagueService;
    /** Service for managing players and match statistics. */
    private final PlayerService        playerService;
    /** Service for managing matches and match results. */
    private final MatchService         matchService;
    /** Service for managing and moderating user comments. */
    private final CommentService       commentService;
    /** Service for managing user accounts and roles. */
    private final UserService          userService;
    /** Service for fetching external logos and player photos from Wikipedia. */
    private final WikipediaLogoService wikipediaLogoService;

    /**
     * Creates the dashboard controller with the services used for counters and status.
     *
     * @param teamService team management service
     * @param leagueService league management service
     * @param playerService player management service
     * @param matchService match management service
     * @param commentService comment moderation service
     * @param userService user management service
     * @param wikipediaLogoService background Wikipedia image fetch service
     */
    public AdminController(TeamService teamService,
                           LeagueService leagueService,
                           PlayerService playerService,
                           MatchService matchService,
                           CommentService commentService,
                           UserService userService,
                           WikipediaLogoService wikipediaLogoService) {
        this.teamService          = teamService;
        this.leagueService        = leagueService;
        this.playerService        = playerService;
        this.matchService         = matchService;
        this.commentService       = commentService;
        this.userService          = userService;
        this.wikipediaLogoService = wikipediaLogoService;
    }

    /**
     * Reports the current state of the Wikipedia image fetch job.
     *
     * @return current fetch status for polling UI
     */
    @GetMapping("/wiki-status")
    @ResponseBody
    public WikiStatusResponse wikiStatus() {
        return new WikiStatusResponse(
                wikipediaLogoService.isRunning(),
                wikipediaLogoService.pollType(),
                wikipediaLogoService.pollMessage()
        );
    }

    /**
     * Shows the admin dashboard with league, user, content and moderation counters.
     *
     * @param model MVC model populated for the dashboard template
     * @return admin dashboard view
     */
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("teamCount",      teamService.count());
        model.addAttribute("leagueCount",    leagueService.count());
        model.addAttribute("playerCount",    playerService.count());
        model.addAttribute("userCount",      userService.count());
        model.addAttribute("reportedCommentCount", commentService.countReported());
        model.addAttribute("scheduledCount", matchService.countByStatus(MatchStatus.SCHEDULED));
        model.addAttribute("completedCount", matchService.countByStatus(MatchStatus.COMPLETED));
        model.addAttribute("missingLogoCount", teamService.countMissingLogos());
        model.addAttribute("missingPhotoCount", playerService.countMissingPhotos());
        return "admin/dashboard";
    }
}
