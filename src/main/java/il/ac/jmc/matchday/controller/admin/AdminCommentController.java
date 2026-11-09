package il.ac.jmc.matchday.controller.admin;

import il.ac.jmc.matchday.config.Roles;
import il.ac.jmc.matchday.entity.Comment;
import il.ac.jmc.matchday.exception.CommentNotAllowedException;
import il.ac.jmc.matchday.service.CommentService;
import il.ac.jmc.matchday.util.ErrorMessages;
import il.ac.jmc.matchday.util.Pagination;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.function.Function;

/**
 * Admin moderation screens and actions for reported or hidden comments.
 */
@Controller
@RequestMapping("/admin/comments")
public class AdminCommentController {

    /** Prefix for redirect URLs to the moderation list. */
    private static final String REDIRECT_LIST_PREFIX = "redirect:/admin/comments?reportedOnly=";
    /** Prefix for anchor tags to scroll back to the modified comment. */
    private static final String COMMENT_ANCHOR_PREFIX = "#comment-";
    /** Default error message if an action fails without a specific message. */
    private static final String ACTION_FAILED_MESSAGE = "Comment action could not be completed.";

    /** Service handling comment moderation. */
    private final CommentService commentService;

    /**
     * Creates the controller with the comment moderation service.
     *
     * @param commentService service that owns comment moderation rules
     */
    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Shows the paginated moderation list.
     *
     * @param reportedOnly whether to show only reported comments
     * @param page zero-based page number
     * @param model MVC model populated for the template
     * @return admin comment moderation view
     */
    @GetMapping
    public String list(@RequestParam(defaultValue = "false") boolean reportedOnly,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("comments", commentService.findAdminCommentsPage(reportedOnly, page));
        model.addAttribute("reportedOnly", reportedOnly);
        model.addAttribute("adminRole", Roles.ADMIN);
        return "admin/comments/list";
    }

    /**
     * Hides a comment from public pages.
     *
     * @param id comment id
     * @param reportedOnly whether the admin is currently filtering reported comments
     * @param page current page number
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the moderation list
     */
    @PostMapping("/{id}/hide")
    public String hide(@PathVariable Long id,
                       @RequestParam(defaultValue = "true") boolean reportedOnly,
                       @RequestParam(defaultValue = "0") int page,
                       RedirectAttributes redirectAttributes) {
        return applyModerationAction(
                id,
                reportedOnly,
                page,
                redirectAttributes,
                commentService::hideComment,
                comment -> "Comment by " + comment.getUser().getUsername() + " hidden."
        );
    }

    /**
     * Restores a hidden comment to public visibility.
     *
     * @param id comment id
     * @param reportedOnly whether the admin is currently filtering reported comments
     * @param page current page number
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the moderation list
     */
    @PostMapping("/{id}/show")
    public String show(@PathVariable Long id,
                       @RequestParam(defaultValue = "true") boolean reportedOnly,
                       @RequestParam(defaultValue = "0") int page,
                       RedirectAttributes redirectAttributes) {
        return applyModerationAction(
                id,
                reportedOnly,
                page,
                redirectAttributes,
                commentService::showComment,
                comment -> "Comment by " + comment.getUser().getUsername() + " restored."
        );
    }

    /**
     * Clears the reported flag from a comment.
     *
     * @param id comment id
     * @param reportedOnly whether the admin is currently filtering reported comments
     * @param page current page number
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the moderation list
     */
    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable Long id,
                          @RequestParam(defaultValue = "true") boolean reportedOnly,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirectAttributes) {
        return applyModerationAction(
                id,
                reportedOnly,
                page,
                redirectAttributes,
                commentService::resolveReport,
                comment -> "Report for " + comment.getUser().getUsername() + " cleared."
        );
    }

    /**
     * Deletes a comment as an admin action.
     *
     * @param id comment id
     * @param reportedOnly whether the admin is currently filtering reported comments
     * @param page current page number
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the moderation list
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(defaultValue = "true") boolean reportedOnly,
                         @RequestParam(defaultValue = "0") int page,
                         RedirectAttributes redirectAttributes) {
        return applyModerationAction(
                id,
                reportedOnly,
                page,
                redirectAttributes,
                commentService::deleteCommentAsAdmin,
                comment -> "Comment by " + comment.getUser().getUsername() + " removed."
        );
    }

    /**
     * Blocks the author of a comment.
     *
     * @param id comment id
     * @param reportedOnly whether the admin is currently filtering reported comments
     * @param page current page number
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the moderation list
     */
    @PostMapping("/{id}/block-author")
    public String blockAuthor(@PathVariable Long id,
                              @RequestParam(defaultValue = "true") boolean reportedOnly,
                              @RequestParam(defaultValue = "0") int page,
                              RedirectAttributes redirectAttributes) {
        return applyModerationAction(
                id,
                reportedOnly,
                page,
                redirectAttributes,
                commentService::blockCommentAuthor,
                comment -> comment.getUser().getUsername() + " blocked."
        );
    }

    /**
     * Unblocks the author of a comment.
     *
     * @param id comment id
     * @param reportedOnly whether the admin is currently filtering reported comments
     * @param page current page number
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the moderation list
     */
    @PostMapping("/{id}/unblock-author")
    public String unblockAuthor(@PathVariable Long id,
                                @RequestParam(defaultValue = "true") boolean reportedOnly,
                                @RequestParam(defaultValue = "0") int page,
                                RedirectAttributes redirectAttributes) {
        return applyModerationAction(
                id,
                reportedOnly,
                page,
                redirectAttributes,
                commentService::unblockCommentAuthor,
                comment -> comment.getUser().getUsername() + " unblocked."
        );
    }

    /**
     * Helper to apply a moderation action and abstract away try-catch blocks and redirects.
     *
     * @param id                 the comment ID
     * @param reportedOnly       whether the list is filtered to reported comments
     * @param page               the pagination page
     * @param redirectAttributes flash attributes to show success/error
     * @param action             the moderation action function to execute
     * @param successMessage     the function to generate a success message after executing
     * @return the redirect URL
     */
    private String applyModerationAction(Long id,
                                          boolean reportedOnly,
                                          int page,
                                          RedirectAttributes redirectAttributes,
                                          Function<Long, Comment> action,
                                          Function<Comment, String> successMessage) {
        try {
            Comment comment = action.apply(id);
            redirectAttributes.addFlashAttribute("success", successMessage.apply(comment));
        } catch (CommentNotAllowedException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error",
                    ErrorMessages.messageOrDefault(ex, ACTION_FAILED_MESSAGE));
        }
        return redirectToList(reportedOnly, page, id);
    }

    /**
     * Builds the redirect URL to return to the admin comment list.
     *
     * @param reportedOnly whether the view is filtering reported comments
     * @param page         the current page
     * @param commentId    the comment ID for adding an anchor link
     * @return the formatted redirect string
     */
    private String redirectToList(boolean reportedOnly, int page, Long commentId) {
        return REDIRECT_LIST_PREFIX + reportedOnly
                + "&page=" + Pagination.normalizePage(page)
                + COMMENT_ANCHOR_PREFIX + commentId;
    }
}
