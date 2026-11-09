package il.ac.jmc.matchday.controller.admin;

import il.ac.jmc.matchday.controller.support.TeamListModelBuilder;
import il.ac.jmc.matchday.dto.TeamForm;
import il.ac.jmc.matchday.entity.League;
import il.ac.jmc.matchday.entity.Team;
import il.ac.jmc.matchday.exception.EntityNotFoundException;
import il.ac.jmc.matchday.service.LeagueService;
import il.ac.jmc.matchday.service.TeamService;
import il.ac.jmc.matchday.service.WikipediaLogoService;
import il.ac.jmc.matchday.util.ReturnUrlUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin CRUD controller for teams and team logo maintenance.
 */
@Controller
@RequestMapping("/admin/teams")
public class AdminTeamController {

    /** Thymeleaf view for the team list page. */
    private static final String VIEW_LIST = "admin/teams/list";

    /** Thymeleaf view for the create/edit team form. */
    private static final String VIEW_FORM = "admin/teams/form";

    /** Thymeleaf fragment returned for AJAX list refreshes. */
    private static final String VIEW_RESULTS_FRAGMENT = VIEW_LIST + " :: results";

    /** Redirect target for the team list. */
    private static final String REDIRECT_LIST = "redirect:/admin/teams";

    /** Plain path for the team list, used as a safe return fallback. */
    private static final String LIST_PATH = "/admin/teams";

    /** Redirect prefix for returning to a league edit page. */
    private static final String REDIRECT_LEAGUE_EDIT_PREFIX = "redirect:/admin/leagues/";

    /** Anchor for the league membership section. */
    private static final String LEAGUE_TEAMS_ANCHOR = "#league-teams";

    /** Shared model attribute for the edited entity id. */
    private static final String MODEL_EDIT_ID = AdminModelAttributes.EDIT_ID;

    /** Team business service. */
    private final TeamService          teamService;

    /** League lookup and membership service. */
    private final LeagueService        leagueService;

    /** Background Wikipedia logo fetch service. */
    private final WikipediaLogoService wikipediaLogoService;

    /** Shared model builder for team list filters. */
    private final TeamListModelBuilder teamListModelBuilder;

    /**
     * Creates the controller with team, league and image-fetch collaborators.
     *
     * @param teamService team business service
     * @param leagueService league lookup service
     * @param wikipediaLogoService background Wikipedia logo fetch service
     * @param teamListModelBuilder shared team-list model builder
     */
    public AdminTeamController(TeamService teamService,
                               LeagueService leagueService,
                               WikipediaLogoService wikipediaLogoService,
                               TeamListModelBuilder teamListModelBuilder) {
        this.teamService          = teamService;
        this.leagueService        = leagueService;
        this.wikipediaLogoService = wikipediaLogoService;
        this.teamListModelBuilder = teamListModelBuilder;
    }

    /**
     * Shows the searchable, paginated admin teams list.
     *
     * @param name optional team name filter
     * @param leagueIds optional league filters
     * @param page zero-based page number
     * @param fragment whether to return only the results fragment
     * @param model MVC model populated for the list
     * @return full list view or results fragment
     */
    @GetMapping
    public String list(@RequestParam(required = false) String name,
                       @RequestParam(value = "leagueId", required = false) List<Long> leagueIds,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "false") boolean fragment,
                       HttpServletRequest request,
                       Model model) {
        teamListModelBuilder.populate(model, name, leagueIds, page);
        model.addAttribute("missingLogoCount", teamService.countMissingLogos());
        model.addAttribute("returnUrl", ReturnUrlUtil.currentRequestUrl(request));
        return fragment ? VIEW_RESULTS_FRAGMENT : VIEW_LIST;
    }

    /**
     * Shows the create-team form, optionally scoped from a league edit page.
     *
     * @param leagueId optional league id to attach after creation
     * @param model MVC model populated for the form
     * @return team form view
     */
    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long leagueId,
                          @RequestParam(required = false) String returnUrl,
                          Model model) {
        TeamForm form = new TeamForm();
        if (leagueId != null) {
            if (leagueService.findById(leagueId).isEmpty()) {
                throw new EntityNotFoundException(League.class);
            }
            form.setLeagueId(leagueId);
            form.setReturnToLeague(true);
        }
        model.addAttribute("form", form);
        model.addAttribute(MODEL_EDIT_ID, null);
        model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
        populateFormModel(model);
        return VIEW_FORM;
    }

    /**
     * Validates and creates a team.
     *
     * @param form submitted team form
     * @param result validation result
     * @param model MVC model reused when validation fails
     * @param redirectAttributes flash attributes for success messages
     * @return form view on validation errors, otherwise a redirect
     */
    @PostMapping
    public String create(@Valid @ModelAttribute("form") TeamForm form,
                         BindingResult result,
                         @RequestParam(required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        teamService.validateForm(form, result, null);
        if (result.hasErrors()) {
            model.addAttribute(MODEL_EDIT_ID, null);
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
            populateFormModel(model);
            return VIEW_FORM;
        }
        Team team = teamService.createFromForm(form);
        redirectAttributes.addFlashAttribute("success", "Team \"" + team.getName() + "\" created.");
        if (form.isReturnToLeague() && form.getLeagueId() != null) {
            return redirectToLeagueTeams(form.getLeagueId());
        }
        return redirectBack(returnUrl);
    }

    /**
     * Shows the edit-team form.
     *
     * @param id team id
     * @param model MVC model populated for the form
     * @return team form view
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @RequestParam(required = false) String returnUrl,
                           Model model) {
        Team team = teamService.getByIdOrThrow(id);
        model.addAttribute("form", teamService.buildForm(team));
        model.addAttribute(MODEL_EDIT_ID, id);
        model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
        populateFormModel(model);
        return VIEW_FORM;
    }

    /**
     * Validates and updates a team.
     *
     * @param id team id
     * @param form submitted team form
     * @param result validation result
     * @param model MVC model reused when validation fails
     * @param redirectAttributes flash attributes for success messages
     * @return form view on validation errors, otherwise redirect to the team list
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") TeamForm form,
                         BindingResult result,
                         @RequestParam(required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Team team = teamService.getByIdOrThrow(id);
        teamService.validateForm(form, result, id);
        if (result.hasErrors()) {
            model.addAttribute(MODEL_EDIT_ID, id);
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
            populateFormModel(model);
            return VIEW_FORM;
        }
        teamService.updateFromForm(team, form);
        redirectAttributes.addFlashAttribute("success", "Team \"" + team.getName() + "\" updated.");
        return redirectBack(returnUrl);
    }

    /**
     * Deletes a team when service rules allow it.
     *
     * @param id team id
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the team list
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String returnUrl,
                         RedirectAttributes redirectAttributes) {
        Team team = teamService.getByIdOrThrow(id);
        try {
            teamService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Team \"" + team.getName() + "\" deleted.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectBack(returnUrl);
    }

    /**
     * Starts the background logo fetch for teams missing logo URLs.
     *
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the team list
     */
    @PostMapping("/fetch-logos")
    public String fetchLogos(RedirectAttributes redirectAttributes) {
        return WikipediaFetchTrigger.start(wikipediaLogoService,
                wikipediaLogoService::fetchMissingTeamLogos, redirectAttributes, REDIRECT_LIST);
    }

    /**
     * Adds league choices to the team form model.
     *
     * @param model MVC model to populate
     */
    private void populateFormModel(Model model) {
        model.addAttribute("leagues", leagueService.findAll());
    }

    /**
     * Builds the redirect URL back to a league's membership section.
     *
     * @param leagueId league id
     * @return league edit redirect URL with membership anchor
     */
    private String redirectToLeagueTeams(Long leagueId) {
        return REDIRECT_LEAGUE_EDIT_PREFIX + leagueId + "/edit" + LEAGUE_TEAMS_ANCHOR;
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
