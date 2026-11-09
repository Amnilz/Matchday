package il.ac.jmc.matchday.controller;

import il.ac.jmc.matchday.controller.support.Routes;
import il.ac.jmc.matchday.entity.Team;
import il.ac.jmc.matchday.service.FavoriteTeamService;
import il.ac.jmc.matchday.util.PrincipalUtil;
import il.ac.jmc.matchday.util.ReturnUrlUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Adds and removes the logged-in user's favorite teams, redirecting back to the source page.
 */
@Controller
@RequestMapping("/favorites/teams")
public class FavoriteTeamController {

    /** Service for managing favorite teams. */
    private final FavoriteTeamService favoriteTeamService;

    /**
     * Creates the controller with the favorite-team service.
     *
     * @param favoriteTeamService favorite-team business service
     */
    public FavoriteTeamController(FavoriteTeamService favoriteTeamService) {
        this.favoriteTeamService = favoriteTeamService;
    }

    /**
     * Saves the team as a favorite and flashes a confirmation.
     *
     * @param teamId team id
     * @param returnUrl optional safe return URL
     * @param principal current user principal
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect to the source page or team detail page
     */
    @PostMapping("/{teamId}")
    public String add(@PathVariable Long teamId,
                      @RequestParam(required = false) String returnUrl,
                       Principal principal,
                       RedirectAttributes redirectAttributes) {
        Team team = favoriteTeamService.addFavorite(PrincipalUtil.requireName(principal), teamId);
        redirectAttributes.addFlashAttribute("successMessage",
                team.getName() + " was added to your favorites.");
        return Routes.REDIRECT_PREFIX + safeReturnUrl(returnUrl, teamId);
    }

    /**
     * Removes the team from favorites and flashes a confirmation.
     *
     * @param teamId team id
     * @param returnUrl optional safe return URL
     * @param principal current user principal
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect to the source page or team detail page
     */
    @PostMapping("/{teamId}/remove")
    public String remove(@PathVariable Long teamId,
                         @RequestParam(required = false) String returnUrl,
                          Principal principal,
                          RedirectAttributes redirectAttributes) {
        Team team = favoriteTeamService.removeFavorite(PrincipalUtil.requireName(principal), teamId);
        redirectAttributes.addFlashAttribute("successMessage",
                team.getName() + " was removed from your favorites.");
        return Routes.REDIRECT_PREFIX + safeReturnUrl(returnUrl, teamId);
    }

    /**
     * Returns the return URL when it is whitelisted, or falls back to the team detail page.
     *
     * @param returnUrl candidate return URL from the request
     * @param teamId team id used for the fallback URL
     * @return safe redirect target
     */
    private String safeReturnUrl(String returnUrl, Long teamId) {
        return ReturnUrlUtil.isSafe(returnUrl, Routes.TEAMS, Routes.SEARCH, Routes.PROFILE)
                ? returnUrl
                : Routes.TEAMS + "/" + teamId;
    }
}
