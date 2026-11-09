package il.ac.jmc.matchday.controller;

import il.ac.jmc.matchday.controller.support.Routes;
import il.ac.jmc.matchday.dto.CommentForm;
import il.ac.jmc.matchday.entity.Comment;
import il.ac.jmc.matchday.exception.CommentNotAllowedException;
import il.ac.jmc.matchday.service.CommentService;
import il.ac.jmc.matchday.util.ReturnUrlUtil;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Controller for authenticated match comment actions.
 */
@Controller
@RequestMapping("/comments")
public class CommentController {

    /** Redirect prefix for match detail pages. */
    private static final String MATCH_REDIRECT_PREFIX = "redirect:/matches/";

    /** Anchor for the comments section on a match page. */
    private static final String COMMENTS_ANCHOR = "#comments";

    /** Anchor prefix for scrolling to a specific comment. */
    private static final String COMMENT_ANCHOR_PREFIX = "#comment-";

    /** Comment business service. */
    private final CommentService commentService;

    /**
     * Creates the controller with the comment service.
     *
     * @param commentService comment business service
     */
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Adds a top-level comment to a match.
     *
     * @param matchId match id
     * @param form submitted comment form
     * @param bindingResult validation result
     * @param principal current user principal
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the match comments section
     */
    @PostMapping("/matches/{matchId}")
    public String addToMatch(@PathVariable Long matchId,
                             @Valid @ModelAttribute("commentForm") CommentForm form,
                             BindingResult bindingResult,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            keepCommentForm(form, bindingResult, redirectAttributes);
            return redirectToMatch(matchId);
        }

        try {
            Comment comment = commentService.addComment(matchId, principal.getName(), form.getContent());
            redirectAttributes.addFlashAttribute("commentSuccess", "Comment added.");
            return redirectToComment(matchId, comment.getId());
        } catch (CommentNotAllowedException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("commentError", ex.getMessage());
            redirectAttributes.addFlashAttribute("commentForm", form);
        }

        return redirectToMatch(matchId);
    }

    /**
     * Edits an existing comment owned by the current user.
     *
     * @param commentId comment id
     * @param matchId fallback match id for redirecting
     * @param returnUrl optional safe return URL
     * @param form submitted comment form
     * @param bindingResult validation result
     * @param principal current user principal
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect after the edit action
     */
    @PostMapping("/{commentId}/edit")
    public String edit(@PathVariable Long commentId,
                       @RequestParam Long matchId,
                       @RequestParam(required = false) String returnUrl,
                       @Valid @ModelAttribute("commentForm") CommentForm form,
                       BindingResult bindingResult,
                       Principal principal,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            keepCommentForm(form, bindingResult, redirectAttributes);
            return redirectAfterAction(matchId, returnUrl);
        }

        Long redirectMatchId = applyCommentAction(
                () -> commentService.updateComment(commentId, principal.getName(), form.getContent()),
                matchId,
                "Comment updated.",
                redirectAttributes
        );
        return redirectAfterAction(redirectMatchId, returnUrl);
    }

    /**
     * Adds a reply to an existing comment thread.
     *
     * @param commentId parent comment id
     * @param matchId fallback match id for redirecting
     * @param form submitted comment form
     * @param bindingResult validation result
     * @param principal current user principal
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the new reply or match comments section
     */
    @PostMapping("/{commentId}/replies")
    public String reply(@PathVariable Long commentId,
                        @RequestParam Long matchId,
                        @Valid @ModelAttribute("commentForm") CommentForm form,
                        BindingResult bindingResult,
                        Principal principal,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            keepCommentForm(form, bindingResult, redirectAttributes);
            return redirectToMatch(matchId);
        }

        try {
            Comment reply = commentService.addReply(commentId, principal.getName(), form.getContent());
            redirectAttributes.addFlashAttribute("commentSuccess", "Reply added.");
            return redirectToComment(reply.getMatch().getId(), reply.getId());
        } catch (CommentNotAllowedException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("commentError", ex.getMessage());
            return redirectToMatch(matchId);
        }
    }


    /**
     * Deletes an existing comment owned by the current user.
     *
     * @param commentId comment id
     * @param matchId fallback match id for redirecting
     * @param returnUrl optional safe return URL
     * @param principal current user principal
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect after the delete action
     */
    @PostMapping("/{commentId}/delete")
    public String delete(@PathVariable Long commentId,
                         @RequestParam Long matchId,
                         @RequestParam(required = false) String returnUrl,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {
        Long redirectMatchId = applyCommentAction(
                () -> commentService.deleteComment(commentId, principal.getName()),
                matchId,
                "Comment deleted.",
                redirectAttributes
        );
        return redirectAfterAction(redirectMatchId, returnUrl);
    }

    /**
     * Reports a visible comment for admin review.
     *
     * @param commentId comment id
     * @param matchId fallback match id for redirecting
     * @param principal current user principal
     * @param redirectAttributes flash attributes for the redirect
     * @return redirect back to the match comments section
     */
    @PostMapping("/{commentId}/report")
    public String report(@PathVariable Long commentId,
                         @RequestParam Long matchId,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {
        Long redirectMatchId = applyCommentAction(
                () -> commentService.reportComment(commentId, principal.getName()),
                matchId,
                "Comment reported.",
                redirectAttributes
        );
        return redirectToMatch(redirectMatchId);
    }

    /**
     * Keeps the rejected comment form and validation errors across a redirect.
     *
     * @param form submitted comment form
     * @param bindingResult validation result
     * @param redirectAttributes flash attributes for the redirect
     */
    private void keepCommentForm(CommentForm form,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("commentForm", form);
        redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "commentForm", bindingResult);
    }

    /**
     * Builds a redirect to a match's comments section.
     *
     * @param matchId match id
     * @return match redirect URL with comments anchor
     */
    private String redirectToMatch(Long matchId) {
        return MATCH_REDIRECT_PREFIX + matchId + COMMENTS_ANCHOR;
    }

    /**
     * Builds a redirect to a specific comment on a match page.
     *
     * @param matchId match id
     * @param commentId comment id
     * @return match redirect URL with comment anchor
     */
    private String redirectToComment(Long matchId, Long commentId) {
        return MATCH_REDIRECT_PREFIX + matchId + COMMENT_ANCHOR_PREFIX + commentId;
    }

    /**
     * Chooses a safe post-action redirect, preserving profile return URLs when allowed.
     *
     * @param matchId fallback match id
     * @param returnUrl optional submitted return URL
     * @return safe redirect URL
     */
    private String redirectAfterAction(Long matchId, String returnUrl) {
        if (ReturnUrlUtil.isSafe(returnUrl, Routes.PROFILE)) {
            return Routes.REDIRECT_PREFIX + returnUrl;
        }
        return redirectToMatch(matchId);
    }

    /**
     * Runs a comment action and converts expected failures into flash messages.
     *
     * @param action action that returns the match id to redirect to
     * @param fallbackMatchId match id used when the action fails
     * @param successMessage flash message used on success
     * @param redirectAttributes flash attributes for the redirect
     * @return match id to redirect to
     */
    private Long applyCommentAction(CommentAction action,
                                    Long fallbackMatchId,
                                    String successMessage,
                                    RedirectAttributes redirectAttributes) {
        try {
            Long matchId = action.apply();
            redirectAttributes.addFlashAttribute("commentSuccess", successMessage);
            return matchId;
        } catch (CommentNotAllowedException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("commentError", ex.getMessage());
            return fallbackMatchId;
        }
    }

    /**
     * Comment action that returns the affected match id.
     */
    @FunctionalInterface
    private interface CommentAction {
        /**
         * Applies the comment mutation.
         *
         * @return affected match id
         */
        Long apply();
    }
}
