package il.ac.jmc.matchday.controller.admin;

import il.ac.jmc.matchday.config.Roles;
import il.ac.jmc.matchday.entity.User;
import il.ac.jmc.matchday.service.UserService;
import il.ac.jmc.matchday.util.ErrorMessages;
import il.ac.jmc.matchday.util.Pagination;
import il.ac.jmc.matchday.util.PrincipalUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Admin user management controller for listing, blocking and unblocking users.
 */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    /** Redirect prefix for returning to the paginated users list. */
    private static final String REDIRECT_LIST_PREFIX = "redirect:/admin/users?page=";

    /** Anchor prefix for scrolling back to the acted-on user row. */
    private static final String USER_ANCHOR_PREFIX = "#user-";

    /** Fallback flash message when a user action fails without a specific message. */
    private static final String ACTION_FAILED_MESSAGE = "User action could not be completed.";

    /** User business service. */
    private final UserService userService;

    /**
     * Creates the controller with the user management service.
     *
     * @param userService user business service
     */
    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Shows the paginated user administration page.
     *
     * @param page zero-based page number
     * @param model MVC model populated for the list
     * @param principal current admin principal
     * @return admin users list view
     */
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model, Principal principal) {
        model.addAttribute("users", userService.findAdminPage(page));
        model.addAttribute("currentUsername", PrincipalUtil.nameOrNull(principal));
        model.addAttribute("adminRole", Roles.ADMIN);
        return "admin/users/list";
    }

    /**
     * Blocks a user unless the service rejects the action.
     *
     * @param id user id
     * @param page current page number
     * @param principal current admin principal
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the user list
     */
    @PostMapping("/{id}/block")
    public String block(@PathVariable Long id,
                        @RequestParam(defaultValue = "0") int page,
                        Principal principal,
                        RedirectAttributes redirectAttributes) {
        try {
            User user = userService.blockUser(id, PrincipalUtil.nameOrNull(principal));
            redirectAttributes.addFlashAttribute("success", user.getUsername() + " blocked.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error",
                    ErrorMessages.messageOrDefault(ex, ACTION_FAILED_MESSAGE));
        }
        return redirectToList(page, id);
    }

    /**
     * Unblocks a user.
     *
     * @param id user id
     * @param page current page number
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the user list
     */
    @PostMapping("/{id}/unblock")
    public String unblock(@PathVariable Long id,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirectAttributes) {
        try {
            User user = userService.unblockUser(id);
            redirectAttributes.addFlashAttribute("success", user.getUsername() + " unblocked.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error",
                    ErrorMessages.messageOrDefault(ex, ACTION_FAILED_MESSAGE));
        }
        return redirectToList(page, id);
    }

    /**
     * Builds the redirect URL back to the same users page and row.
     *
     * @param page requested page number
     * @param userId acted-on user id
     * @return users-list redirect URL with row anchor
     */
    private String redirectToList(int page, Long userId) {
        return REDIRECT_LIST_PREFIX + Pagination.normalizePage(page) + USER_ANCHOR_PREFIX + userId;
    }
}
