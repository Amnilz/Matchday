package il.ac.jmc.matchday.controller.admin;

import il.ac.jmc.matchday.controller.support.PlayerListModelBuilder;
import il.ac.jmc.matchday.dto.PlayerForm;
import il.ac.jmc.matchday.entity.Player;
import il.ac.jmc.matchday.entity.Position;
import il.ac.jmc.matchday.entity.Team;
import il.ac.jmc.matchday.service.PlayerService;
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
 * Admin CRUD controller for players and player photo maintenance.
 */
@Controller
@RequestMapping("/admin/players")
public class AdminPlayerController {

    /** Thymeleaf view for the player list page. */
    private static final String VIEW_LIST = "admin/players/list";

    /** Thymeleaf view for the create/edit player form. */
    private static final String VIEW_FORM = "admin/players/form";

    /** Thymeleaf fragment returned for AJAX list refreshes. */
    private static final String VIEW_RESULTS_FRAGMENT = VIEW_LIST + " :: results";

    /** Redirect target for the player list. */
    private static final String REDIRECT_LIST = "redirect:/admin/players";

    /** Plain path for the player list, used as a safe return fallback. */
    private static final String LIST_PATH = "/admin/players";

    /** Shared model attribute for the edited entity id. */
    private static final String MODEL_EDIT_ID = AdminModelAttributes.EDIT_ID;

    /** Player business service. */
    private final PlayerService          playerService;

    /** Team lookup service. */
    private final TeamService            teamService;

    /** Background Wikipedia photo fetch service. */
    private final WikipediaLogoService   wikipediaLogoService;

    /** Shared model builder for player list filters. */
    private final PlayerListModelBuilder playerListModelBuilder;

    /**
     * Creates the controller with player, team and image-fetch collaborators.
     *
     * @param playerService player business service
     * @param teamService team lookup service
     * @param wikipediaLogoService background Wikipedia photo fetch service
     * @param playerListModelBuilder shared player-list model builder
     */
    public AdminPlayerController(PlayerService playerService,
                                 TeamService teamService,
                                 WikipediaLogoService wikipediaLogoService,
                                 PlayerListModelBuilder playerListModelBuilder) {
        this.playerService          = playerService;
        this.teamService            = teamService;
        this.wikipediaLogoService   = wikipediaLogoService;
        this.playerListModelBuilder = playerListModelBuilder;
    }

    /**
     * Shows the filterable, paginated admin players list.
     *
     * @param name optional player name filter
     * @param leagueIds optional league filters
     * @param teamIds optional team filters
     * @param positions optional position filters
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
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "false") boolean fragment,
                       HttpServletRequest request,
                       Model model) {
        List<Long> effectiveTeamIds = playerListModelBuilder.populate(model, name, leagueIds, teamIds, positions);

        model.addAttribute("players", playerService.listAllFiltered(name, effectiveTeamIds, positions, page));
        model.addAttribute("missingPhotoCount", playerService.countMissingPhotos());
        model.addAttribute("returnUrl", ReturnUrlUtil.currentRequestUrl(request));
        return fragment ? VIEW_RESULTS_FRAGMENT : VIEW_LIST;
    }

    /**
     * Shows the create-player form, optionally preselecting a team.
     *
     * @param teamId optional team id to preselect
     * @param model MVC model populated for the form
     * @return player form view
     */
    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long teamId,
                          @RequestParam(required = false) String returnUrl,
                          Model model) {
        PlayerForm form = new PlayerForm();
        if (teamId != null && teamService.findById(teamId).isPresent()) {
            form.setTeamId(teamId);
        }
        model.addAttribute("form", form);
        model.addAttribute(MODEL_EDIT_ID, null);
        model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
        populateFormModel(model);
        return VIEW_FORM;
    }

    /**
     * Validates and creates a player.
     *
     * @param form submitted player form
     * @param result validation result
     * @param model MVC model reused when validation fails
     * @param redirectAttributes flash attributes for success messages
     * @return form view on validation errors, otherwise redirect to the player list
     */
    @PostMapping
    public String create(@Valid @ModelAttribute("form") PlayerForm form,
                         BindingResult result,
                         @RequestParam(required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Team team = resolveTeam(form.getTeamId(), result);
        playerService.validateShirtNumber(form, team, null, result);
        if (result.hasErrors()) {
            model.addAttribute(MODEL_EDIT_ID, null);
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
            populateFormModel(model);
            return VIEW_FORM;
        }
        Player player = playerService.createFromForm(form, team);
        redirectAttributes.addFlashAttribute("success",
                "Player \"" + player.getFullName() + "\" created.");
        return redirectBack(returnUrl);
    }

    /**
     * Shows the edit-player form.
     *
     * @param id player id
     * @param model MVC model populated for the form
     * @return player form view
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @RequestParam(required = false) String returnUrl,
                           Model model) {
        Player player = playerService.getByIdOrThrow(id);
        model.addAttribute("form", playerService.buildForm(player));
        model.addAttribute(MODEL_EDIT_ID, id);
        model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
        populateFormModel(model);
        return VIEW_FORM;
    }

    /**
     * Validates and updates a player.
     *
     * @param id player id
     * @param form submitted player form
     * @param result validation result
     * @param model MVC model reused when validation fails
     * @param redirectAttributes flash attributes for success messages
     * @return form view on validation errors, otherwise redirect to the player list
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") PlayerForm form,
                         BindingResult result,
                         @RequestParam(required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Player player = playerService.getByIdOrThrow(id);
        Team team = resolveTeam(form.getTeamId(), result);
        playerService.validateShirtNumber(form, team, player, result);
        if (result.hasErrors()) {
            model.addAttribute(MODEL_EDIT_ID, id);
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
            populateFormModel(model);
            return VIEW_FORM;
        }
        playerService.updateFromForm(player, form, team);
        redirectAttributes.addFlashAttribute("success",
                "Player \"" + player.getFullName() + "\" updated.");
        return redirectBack(returnUrl);
    }

    /**
     * Deletes a player when service rules allow it.
     *
     * @param id player id
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the player list
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String returnUrl,
                         RedirectAttributes redirectAttributes) {
        Player player = playerService.getByIdOrThrow(id);
        String name = player.getFullName();
        try {
            playerService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Player \"" + name + "\" deleted.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectBack(returnUrl);
    }

    /**
     * Starts the background photo fetch for players missing photo URLs.
     *
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the player list
     */
    @PostMapping("/fetch-photos")
    public String fetchPhotos(RedirectAttributes redirectAttributes) {
        return WikipediaFetchTrigger.start(wikipediaLogoService,
                wikipediaLogoService::fetchMissingPlayerPhotos, redirectAttributes, REDIRECT_LIST);
    }

    // helpers

    /**
     * Resolves the selected team and records a form error when it no longer exists.
     *
     * @param teamId selected team id
     * @param result validation result that receives lookup errors
     * @return selected team, or {@code null} when absent
     */
    private Team resolveTeam(Long teamId, BindingResult result) {
        if (teamId == null) return null;
        return teamService.findById(teamId).orElseGet(() -> {
            result.rejectValue("teamId", "invalid", "Selected team does not exist");
            return null;
        });
    }

    /**
     * Adds team and position choices to the player form model.
     *
     * @param model MVC model to populate
     */
    private void populateFormModel(Model model) {
        model.addAttribute("teams", teamService.findAll());
        model.addAttribute("positions", Position.values());
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
