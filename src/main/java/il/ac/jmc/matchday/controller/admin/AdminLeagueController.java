package il.ac.jmc.matchday.controller.admin;

import il.ac.jmc.matchday.dto.LeagueForm;
import il.ac.jmc.matchday.entity.League;
import il.ac.jmc.matchday.service.LeagueService;
import il.ac.jmc.matchday.service.WikipediaLogoService;
import il.ac.jmc.matchday.util.ReturnUrlUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Admin CRUD controller for leagues and league-team membership.
 */
@Controller
@RequestMapping("/admin/leagues")
public class AdminLeagueController {

    /** Thymeleaf view for the league list page. */
    private static final String VIEW_LIST = "admin/leagues/list";

    /** Thymeleaf view for the create/edit league form. */
    private static final String VIEW_FORM = "admin/leagues/form";

    /** Thymeleaf fragment returned for AJAX list refreshes. */
    private static final String VIEW_RESULTS_FRAGMENT = VIEW_LIST + " :: results";

    /** Redirect target for the league list. */
    private static final String REDIRECT_LIST = "redirect:/admin/leagues";

    /** Plain path for the league list, used as a safe return fallback. */
    private static final String LIST_PATH = "/admin/leagues";

    /** Redirect prefix for a league edit page. */
    private static final String REDIRECT_EDIT_PREFIX = "redirect:/admin/leagues/";

    /** Anchor for the membership section inside the league form. */
    private static final String TEAMS_ANCHOR = "#league-teams";

    /** Shared model attribute for the edited entity id. */
    private static final String MODEL_EDIT_ID = AdminModelAttributes.EDIT_ID;

    /** League business service. */
    private final LeagueService leagueService;

    /** Background Wikipedia logo fetch service. */
    private final WikipediaLogoService wikipediaLogoService;

    /**
     * Creates the controller with league management and image-fetch services.
     *
     * @param leagueService league business service
     * @param wikipediaLogoService background Wikipedia logo fetch service
     */
    public AdminLeagueController(LeagueService leagueService,
                                 WikipediaLogoService wikipediaLogoService) {
        this.leagueService = leagueService;
        this.wikipediaLogoService = wikipediaLogoService;
    }

    /**
     * Shows the searchable, paginated leagues admin list.
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
        Page<League> leagues = leagueService.findBySearchPage(name, page);
        List<Long> leagueIds = leagues.getContent().stream().map(League::getId).toList();
        Map<Long, Long> teamCounts = leagueService.findTeamCounts(leagueIds);
        model.addAttribute("leagues", leagues);
        model.addAttribute("teamCounts", teamCounts);
        model.addAttribute("searchQuery", name);
        model.addAttribute("missingLogoCount", leagueService.countMissingLogos());
        model.addAttribute("returnUrl", ReturnUrlUtil.currentRequestUrl(request));
        return fragment ? VIEW_RESULTS_FRAGMENT : VIEW_LIST;
    }

    /**
     * Shows the create-league form.
     *
     * @param model MVC model populated with an empty form
     * @return league form view
     */
    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) String returnUrl, Model model) {
        model.addAttribute("form", new LeagueForm());
        model.addAttribute(MODEL_EDIT_ID, null);
        model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
        return VIEW_FORM;
    }

    /**
     * Validates and creates a league.
     *
     * @param form submitted league form
     * @param result validation result
     * @param model MVC model reused when validation fails
     * @param redirectAttributes flash attributes for success messages
     * @return form view on validation errors, otherwise redirect to the edit page
     */
    @PostMapping
    public String create(@Valid @ModelAttribute("form") LeagueForm form,
                         BindingResult result,
                         @RequestParam(required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        leagueService.validateForm(form, result, null);
        if (result.hasErrors()) {
            model.addAttribute(MODEL_EDIT_ID, null);
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
            return VIEW_FORM;
        }
        League league = leagueService.createFromForm(form);
        redirectAttributes.addFlashAttribute("success", "League \"" + league.getName() + "\" created.");
        return redirectToEdit(league.getId());
    }

    /**
     * Shows the edit form and membership controls for one league.
     *
     * @param id league id
     * @param model MVC model populated for the form
     * @return league form view
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @RequestParam(required = false) String returnUrl,
                           Model model) {
        League league = leagueService.getByIdOrThrow(id);
        model.addAttribute("form", leagueService.buildForm(league));
        model.addAttribute(MODEL_EDIT_ID, id);
        model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
        populateMembershipModel(model, id);
        return VIEW_FORM;
    }

    /**
     * Validates and updates a league.
     *
     * @param id league id
     * @param form submitted league form
     * @param result validation result
     * @param model MVC model reused when validation fails
     * @param redirectAttributes flash attributes for success messages
     * @return form view on validation errors, otherwise redirect to the league list
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") LeagueForm form,
                         BindingResult result,
                         @RequestParam(required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        League league = leagueService.getByIdOrThrow(id);
        leagueService.validateForm(form, result, id);
        if (result.hasErrors()) {
            model.addAttribute(MODEL_EDIT_ID, id);
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
            populateMembershipModel(model, id);
            return VIEW_FORM;
        }
        leagueService.updateFromForm(league, form);
        redirectAttributes.addFlashAttribute("success", "League \"" + league.getName() + "\" updated.");
        return redirectBack(returnUrl);
    }

    /**
     * Adds a team to a league.
     *
     * @param id league id
     * @param teamId optional team id selected in the form
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the league membership section
     */
    @PostMapping("/{id}/teams")
    public String addTeam(@PathVariable Long id,
                          @RequestParam(required = false) Long teamId,
                          RedirectAttributes redirectAttributes) {
        League league = leagueService.getByIdOrThrow(id);
        if (teamId == null) {
            redirectAttributes.addFlashAttribute("error", "Choose a team to add.");
            return redirectToTeams(id);
        }
        leagueService.addTeam(id, teamId);
        redirectAttributes.addFlashAttribute("success", "Team added to \"" + league.getName() + "\".");
        return redirectToTeams(id);
    }

    /**
     * Removes a team from a league when business rules allow it.
     *
     * @param leagueId league id
     * @param teamId team id
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the league membership section
     */
    @PostMapping("/{leagueId}/teams/{teamId}/remove")
    public String removeTeam(@PathVariable Long leagueId,
                             @PathVariable Long teamId,
                             RedirectAttributes redirectAttributes) {
        League league = leagueService.getByIdOrThrow(leagueId);
        try {
            leagueService.removeTeam(leagueId, teamId);
            redirectAttributes.addFlashAttribute("success", "Team removed from \"" + league.getName() + "\".");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToTeams(leagueId);
    }

    /**
     * Deletes a league when it has no blocking relationships.
     *
     * @param id league id
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the league list
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String returnUrl,
                         RedirectAttributes redirectAttributes) {
        League league = leagueService.getByIdOrThrow(id);
        try {
            leagueService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "League \"" + league.getName() + "\" deleted.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectBack(returnUrl);
    }

    /**
     * Starts the background logo fetch for leagues missing logo URLs.
     *
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the league list
     */
    @PostMapping("/fetch-logos")
    public String fetchLogos(RedirectAttributes redirectAttributes) {
        return WikipediaFetchTrigger.start(wikipediaLogoService,
                wikipediaLogoService::fetchMissingLeagueLogos, redirectAttributes, REDIRECT_LIST);
    }

    /**
     * Adds current and available team membership lists to the form model.
     *
     * @param model MVC model to populate
     * @param leagueId league id
     */
    private void populateMembershipModel(Model model, Long leagueId) {
        model.addAttribute("leagueTeams", leagueService.findTeams(leagueId));
        model.addAttribute("availableTeams", leagueService.findAvailableTeams(leagueId));
    }

    /**
     * Builds the redirect URL for a league edit page.
     *
     * @param leagueId league id
     * @return edit redirect URL
     */
    private String redirectToEdit(Long leagueId) {
        return REDIRECT_EDIT_PREFIX + leagueId + "/edit";
    }

    /**
     * Builds the redirect URL for the membership section of a league edit page.
     *
     * @param leagueId league id
     * @return edit redirect URL with membership anchor
     */
    private String redirectToTeams(Long leagueId) {
        return redirectToEdit(leagueId) + TEAMS_ANCHOR;
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
