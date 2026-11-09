package il.ac.jmc.matchday.controller;

import il.ac.jmc.matchday.controller.support.Routes;
import il.ac.jmc.matchday.entity.User;
import il.ac.jmc.matchday.exception.EntityNotFoundException;
import il.ac.jmc.matchday.service.CommentService;
import il.ac.jmc.matchday.service.FavoriteTeamService;
import il.ac.jmc.matchday.service.RecentSearchService;
import il.ac.jmc.matchday.service.SessionHistoryService;
import il.ac.jmc.matchday.service.UserService;
import il.ac.jmc.matchday.util.PrincipalUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

/**
 * The logged-in user's profile page: favorites, recent activity and own comments.
 */
@Controller
public class ProfileController {

    /** User lookup service. */
    private final UserService userService;
    /** Favorite-team lookup service. */
    private final FavoriteTeamService favoriteTeamService;
    /** Session-backed recent-search service. */
    private final RecentSearchService recentSearchService;
    /** Comment lookup service. */
    private final CommentService commentService;
    /** Session history service. */
    private final SessionHistoryService sessionHistoryService;

    /**
     * Creates the controller with services used to assemble the profile page.
     *
     * @param userService user lookup service
     * @param favoriteTeamService favorite-team lookup service
     * @param recentSearchService recent-search session service
     * @param commentService comment lookup service
     * @param sessionHistoryService session history service
     */
    public ProfileController(UserService userService,
                             FavoriteTeamService favoriteTeamService,
                             RecentSearchService recentSearchService,
                             CommentService commentService,
                             SessionHistoryService sessionHistoryService) {
        this.userService = userService;
        this.favoriteTeamService = favoriteTeamService;
        this.recentSearchService = recentSearchService;
        this.commentService = commentService;
        this.sessionHistoryService = sessionHistoryService;
    }

    /**
     * Shows the current user's profile; requires authentication.
     *
     * @param principal current user principal
     * @param model MVC model populated for the profile page
     * @return profile view
     */
    @GetMapping(Routes.PROFILE)
    public String profile(Principal principal, Model model) {
        User user = userService.findByUsername(PrincipalUtil.requireName(principal))
                .orElseThrow(() -> new EntityNotFoundException(User.class));

        model.addAttribute("user", user);
        model.addAttribute("favoriteTeams", favoriteTeamService.findFavoriteTeams(user.getUsername()));
        model.addAttribute("favoriteTeamIds", favoriteTeamService.findFavoriteTeamIds(user.getUsername()));
        model.addAttribute("recentTeams", sessionHistoryService.getRecentTeams());
        model.addAttribute("recentMatches", sessionHistoryService.getRecentMatches());
        model.addAttribute("recentSearches", recentSearchService.findRecentSearches());
        model.addAttribute("userComments", commentService.findByUsername(user.getUsername()));
        model.addAttribute("currentUsername", user.getUsername());
        model.addAttribute("userBlockedFromComments", user.isBlocked());
        model.addAttribute("returnUrl", Routes.PROFILE);
        return "profile/index";
    }
}
