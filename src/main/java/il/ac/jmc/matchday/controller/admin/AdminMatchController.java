package il.ac.jmc.matchday.controller.admin;

import il.ac.jmc.matchday.controller.support.MatchSearchModelBuilder;
import il.ac.jmc.matchday.dto.MatchForm;
import il.ac.jmc.matchday.dto.MatchResultFilter;
import il.ac.jmc.matchday.dto.MatchSearchCriteria;
import il.ac.jmc.matchday.entity.Match;
import il.ac.jmc.matchday.entity.MatchEventType;
import il.ac.jmc.matchday.entity.MatchStatus;
import il.ac.jmc.matchday.entity.Team;
import il.ac.jmc.matchday.service.LeagueService;
import il.ac.jmc.matchday.service.MatchService;
import il.ac.jmc.matchday.service.PlayerService;
import il.ac.jmc.matchday.service.TeamService;
import il.ac.jmc.matchday.util.ReturnUrlUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin CRUD controller for matches, including detail/result form handling.
 */
@Controller
@RequestMapping("/admin/matches")
public class AdminMatchController {

    /** Thymeleaf view for the match list page. */
    private static final String VIEW_LIST = "admin/matches/list";

    /** Thymeleaf view for the create/edit match form. */
    private static final String VIEW_FORM = "admin/matches/form";

    /** Thymeleaf fragment returned for AJAX list refreshes. */
    private static final String VIEW_RESULTS_FRAGMENT = VIEW_LIST + " :: results";

    /** Plain path for the match list, used as a safe return fallback. */
    private static final String LIST_PATH = "/admin/matches";

    /** Shared model attribute for the edited entity id. */
    private static final String MODEL_EDIT_ID = AdminModelAttributes.EDIT_ID;

    /** Model attribute that selects the visible form tab. */
    private static final String MODEL_ACTIVE_TAB = "activeTab";

    /** Model attribute that controls whether the events tab is enabled. */
    private static final String MODEL_EVENTS_TAB_ENABLED = "eventsTabEnabled";

    /** Tab id for the match-details form section. */
    private static final String DETAILS_TAB = "details";

    /** Tab id for the match-events form section. */
    private static final String EVENTS_TAB = "events";

    /** Match business service. */
    private final MatchService            matchService;

    /** Team lookup service. */
    private final TeamService             teamService;

    /** Player lookup service. */
    private final PlayerService           playerService;

    /** League lookup service. */
    private final LeagueService           leagueService;

    /** Shared model builder for match list filters and results. */
    private final MatchSearchModelBuilder matchSearchModelBuilder;

    /**
     * Creates the controller with services needed for match forms and filters.
     *
     * @param matchService match business service
     * @param teamService team lookup service
     * @param playerService player lookup service
     * @param leagueService league lookup service
     * @param matchSearchModelBuilder shared match-list model builder
     */
    public AdminMatchController(MatchService matchService,
                                TeamService teamService,
                                PlayerService playerService,
                                LeagueService leagueService,
                                MatchSearchModelBuilder matchSearchModelBuilder) {
        this.matchService             = matchService;
        this.teamService              = teamService;
        this.playerService            = playerService;
        this.leagueService            = leagueService;
        this.matchSearchModelBuilder  = matchSearchModelBuilder;
    }

    // ── List ─────────────────────────────────────────────────────────────────

    /**
     * Shows the searchable, paginated admin match list.
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

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Shows the create-match form.
     *
     * @param model MVC model populated for the form
     * @return match form view
     */
    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) String returnUrl, Model model) {
        MatchForm form = new MatchForm();
        leagueService.findPrimaryLeague().ifPresent(league -> form.setLeagueId(league.getId()));
        model.addAttribute("form",   form);
        model.addAttribute(MODEL_EDIT_ID, null);
        model.addAttribute(MODEL_ACTIVE_TAB, DETAILS_TAB);
        model.addAttribute(MODEL_EVENTS_TAB_ENABLED, false);
        model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
        populateFormModel(model, null);
        return VIEW_FORM;
    }

    /**
     * Validates and creates a match.
     *
     * @param form submitted match form
     * @param result validation result
     * @param model MVC model reused when validation fails
     * @param redirectAttributes flash attributes for success messages
     * @return form view on validation errors, otherwise redirect to the match list
     */
    @PostMapping
    public String create(@Valid @ModelAttribute("form") MatchForm form,
                         BindingResult result,
                         @RequestParam(required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        matchService.validateAdminDetailsForm(form, result);
        if (result.hasErrors()) {
            model.addAttribute(MODEL_EDIT_ID, null);
            model.addAttribute(MODEL_ACTIVE_TAB, tabForErrors(result));
            model.addAttribute(MODEL_EVENTS_TAB_ENABLED, form.getStatus() == MatchStatus.COMPLETED);
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
            populateFormModel(model, null);
            return VIEW_FORM;
        }
        Match saved = matchService.createFromForm(form);
        redirectAttributes.addFlashAttribute("success",
                saved.getHomeTeam().getName() + " vs " + saved.getAwayTeam().getName() + " created.");
        return redirectBack(returnUrl);
    }

    // ── Edit ─────────────────────────────────────────────────────────────────

    /**
     * Shows the edit form for a match.
     *
     * @param id match id
     * @param model MVC model populated for the form
     * @return match form view
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @RequestParam(required = false) String returnUrl,
                           Model model) {
        Match match = matchService.getByIdOrThrow(id);
        MatchForm form = matchService.buildForm(match);
        model.addAttribute("form",   form);
        model.addAttribute(MODEL_EDIT_ID, id);
        model.addAttribute(MODEL_ACTIVE_TAB, DETAILS_TAB);
        model.addAttribute(MODEL_EVENTS_TAB_ENABLED, form.getStatus() == MatchStatus.COMPLETED);
        model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
        populateFormModel(model, match);
        return VIEW_FORM;
    }

    /**
     * Validates and updates a match.
     *
     * @param id match id
     * @param form submitted match form
     * @param result validation result
     * @param model MVC model reused when validation fails
     * @param redirectAttributes flash attributes for success messages
     * @return form view on validation errors, otherwise redirect to the match list
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") MatchForm form,
                         BindingResult result,
                         @RequestParam(required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Match match = matchService.getByIdOrThrow(id);
        matchService.validateAdminDetailsForm(form, result);
        if (result.hasErrors()) {
            model.addAttribute(MODEL_EDIT_ID, id);
            model.addAttribute(MODEL_ACTIVE_TAB, tabForErrors(result));
            model.addAttribute(MODEL_EVENTS_TAB_ENABLED, form.getStatus() == MatchStatus.COMPLETED);
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
            populateFormModel(model, match);
            return VIEW_FORM;
        }
        Match updated = matchService.updateFromForm(match, form);
        redirectAttributes.addFlashAttribute("success",
                updated.getHomeTeam().getName() + " vs " + updated.getAwayTeam().getName() + " updated.");
        return redirectBack(returnUrl);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes a match.
     *
     * @param id match id
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the match list
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String returnUrl,
                         RedirectAttributes redirectAttributes) {
        Match match = matchService.getByIdOrThrow(id);
        String label = match.getHomeTeam().getName() + " vs " + match.getAwayTeam().getName();
        matchService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Match \"" + label + "\" deleted.");
        return redirectBack(returnUrl);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Adds select-list data and optional match context to the form model.
     *
     * @param model MVC model to populate
     * @param match current match, or {@code null} on create
     */
    private void populateFormModel(Model model, Match match) {
        List<Team> teams = teamService.findAll();
        model.addAttribute("leagues",     leagueService.findAll());
        model.addAttribute("teams",       teams);
        model.addAttribute("statuses",    MatchStatus.values());
        model.addAttribute("eventTypes",  MatchEventType.values());
        model.addAttribute("allPlayers",  playerService.findAll());
        model.addAttribute("teamLeagueIds", leagueService.findLeagueIdsCsvByTeam(teams));
        if (match != null) {
            model.addAttribute("match", match);
        }
    }

    /**
     * Chooses the form tab that should be shown after validation errors.
     *
     * @param result validation result
     * @return events tab when event fields failed, otherwise details tab
     */
    private String tabForErrors(BindingResult result) {
        return result.getFieldErrors().stream()
                .anyMatch(error -> error.getField().startsWith("events["))
                ? EVENTS_TAB
                : DETAILS_TAB;
    }

    /**
     * Returns a safe same-site URL for leaving the form, falling back to the list.
     *
     * @param returnUrl submitted return URL
     * @return safe return URL
     */
    private String safeReturnUrl(String returnUrl) {
        return ReturnUrlUtil.safeOrDefault(returnUrl, LIST_PATH);
    }

    /**
     * Builds a redirect back to the submitted list URL.
     *
     * @param returnUrl submitted return URL
     * @return redirect view name
     */
    private String redirectBack(String returnUrl) {
        return "redirect:" + safeReturnUrl(returnUrl);
    }

}
