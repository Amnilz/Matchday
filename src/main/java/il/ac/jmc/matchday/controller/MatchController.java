package il.ac.jmc.matchday.controller;

import il.ac.jmc.matchday.controller.support.MatchSearchModelBuilder;
import il.ac.jmc.matchday.dto.MatchResultFilter;
import il.ac.jmc.matchday.dto.MatchSearchCriteria;
import il.ac.jmc.matchday.dto.CommentForm;
import il.ac.jmc.matchday.dto.CommentThreadItem;
import il.ac.jmc.matchday.entity.Match;
import il.ac.jmc.matchday.entity.MatchStatus;
import il.ac.jmc.matchday.service.CommentService;
import il.ac.jmc.matchday.service.MatchEventService;
import il.ac.jmc.matchday.service.MatchService;
import il.ac.jmc.matchday.service.SessionHistoryService;
import il.ac.jmc.matchday.util.PrincipalUtil;
import il.ac.jmc.matchday.util.ReturnUrlUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * Public pages for browsing matches: filterable list and match detail with comments.
 */
@Controller
@RequestMapping("/matches")
public class MatchController {

    /** Thymeleaf view name for the matches list page. */
    private static final String VIEW_LIST = "matches/list";
    /** Thymeleaf fragment selector for the AJAX results partial. */
    private static final String VIEW_RESULTS_FRAGMENT = VIEW_LIST + " :: results";
    /** Public matches list path. */
    private static final String LIST_PATH = "/matches";

    /** Match lookup service. */
    private final MatchService matchService;
    /** Match-event lookup service. */
    private final MatchEventService matchEventService;
    /** Comment lookup service. */
    private final CommentService commentService;
    /** Session history service. */
    private final SessionHistoryService sessionHistoryService;
    /** Shared match-list model builder. */
    private final MatchSearchModelBuilder matchSearchModelBuilder;

    /**
     * Creates the controller with match, event, comment and list-building services.
     *
     * @param matchService match lookup service
     * @param matchEventService match-event lookup service
     * @param commentService comment lookup service
     * @param sessionHistoryService session history service
     * @param matchSearchModelBuilder shared match-list model builder
     */
    public MatchController(MatchService matchService,
                           MatchEventService matchEventService,
                           CommentService commentService,
                           SessionHistoryService sessionHistoryService,
                           MatchSearchModelBuilder matchSearchModelBuilder) {
        this.matchService = matchService;
        this.matchEventService = matchEventService;
        this.commentService = commentService;
        this.sessionHistoryService = sessionHistoryService;
        this.matchSearchModelBuilder = matchSearchModelBuilder;
    }

    /**
     * Shows the filterable, paginated matches list.
     *
     * @param query optional text query
     * @param leagueIds optional league filters
     * @param teamIds optional team filters
     * @param statuses optional match status filters
     * @param resultFilters optional result filters
     * @param fromDate optional start date
     * @param toDate optional end date
     * @param page zero-based page number
     * @param fragment whether to return only the results fragment
     * @param model MVC model populated for the list
     * @return full list view or results fragment
     */
    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(value = "leagueId", required = false) List<Long> leagueIds,
                       @RequestParam(value = "teamId", required = false) List<Long> teamIds,
                       @RequestParam(value = "status", required = false) List<MatchStatus> statuses,
                       @RequestParam(value = "result", required = false) List<MatchResultFilter> resultFilters,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "false") boolean fragment,
                       HttpServletRequest request,
                       Model model) {
        MatchSearchCriteria criteria = new MatchSearchCriteria(
                query, leagueIds, teamIds, statuses, fromDate, toDate, resultFilters);

        matchSearchModelBuilder.populate(model, criteria, page);
        model.addAttribute("returnUrl", ReturnUrlUtil.currentRequestUrl(request));
        return fragment ? VIEW_RESULTS_FRAGMENT : VIEW_LIST;
    }

    /**
     * Shows one match with score, events when completed and visible comment thread.
     *
     * @param id match id
     * @param principal current user principal, when logged in
     * @param model MVC model populated for the detail page
     * @return match detail view
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(required = false) String returnUrl,
                         Principal principal,
                         Model model) {
        Match match = matchService.getByIdOrThrow(id);
        sessionHistoryService.rememberMatch(match);

        String currentUrl = LIST_PATH + "/" + id;
        String username = PrincipalUtil.nameOrNull(principal);
        model.addAttribute("match", match);
        model.addAttribute("backUrl", ReturnUrlUtil.safeOrDefault(returnUrl, LIST_PATH));
        model.addAttribute("backLabel", ReturnUrlUtil.labelOrDefault(returnUrl, "Matches"));
        model.addAttribute("returnUrl", currentUrl);
        model.addAttribute("events", matchEventService.findByMatchId(id));
        model.addAttribute("showEvents", match.getStatus() == MatchStatus.COMPLETED);
        List<CommentThreadItem> commentThread = commentService.findThreadByMatchId(id);
        model.addAttribute("commentThread", commentThread);
        model.addAttribute("commentCount", commentService.countVisibleComments(commentThread));
        model.addAttribute("currentUsername", username);
        model.addAttribute("userBlockedFromComments",
                username != null && commentService.isBlockedFromCommenting(username));
        if (!model.containsAttribute("commentForm")) {
            model.addAttribute("commentForm", new CommentForm());
        }
        return "matches/detail";
    }
}
