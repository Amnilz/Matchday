package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.exception.CommentNotAllowedException;
import il.ac.jmc.matchday.exception.EntityNotFoundException;
import il.ac.jmc.matchday.dto.CommentThreadItem;
import il.ac.jmc.matchday.entity.Comment;
import il.ac.jmc.matchday.entity.Match;
import il.ac.jmc.matchday.entity.User;
import il.ac.jmc.matchday.repository.CommentRepository;
import il.ac.jmc.matchday.repository.MatchRepository;
import il.ac.jmc.matchday.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for user comments on matches.
 */
@Service
@Transactional
public class CommentService {

    /** Repository for comment persistence and thread queries. */
    private final CommentRepository commentRepository;

    /** Repository used to resolve matches for new comments. */
    private final MatchRepository matchRepository;

    /** User service used for author lookup and block status changes. */
    private final UserService userService;

    /**
     * Creates the service with comment, match and user collaborators.
     *
     * @param commentRepository comment repository
     * @param matchRepository match repository
     * @param userService user lookup and moderation service
     */
    public CommentService(CommentRepository commentRepository,
                          MatchRepository matchRepository,
                          UserService userService) {
        this.commentRepository = commentRepository;
        this.matchRepository = matchRepository;
        this.userService = userService;
    }

    /**
     * Adds a validated comment to a match for the authenticated user.
     *
     * @param matchId match id
     * @param username comment author username
     * @param content submitted comment text
     * @return saved comment
     */
    public Comment addComment(Long matchId, String username, String content) {
        User user = requireUser(username);
        ensureUserCanComment(user);

        String normalized = validateAndNormalizeContent(content);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException(Match.class));
        return commentRepository.save(new Comment(match, user, normalized));
    }

    /**
     * Adds a reply to an existing visible comment and returns the saved reply.
     *
     * @param parentCommentId parent comment id
     * @param username reply author username
     * @param content submitted reply text
     * @return saved reply
     */
    public Comment addReply(Long parentCommentId, String username, String content) {
        User user = requireUser(username);
        ensureUserCanComment(user);
        Comment parent = requireComment(parentCommentId);
        if (!parent.isVisible() || parent.isDeleted()) {
            throw new CommentNotAllowedException("This comment cannot be replied to.");
        }

        Comment reply = new Comment(parent.getMatch(), user, validateAndNormalizeContent(content));
        reply.setParentComment(parent);
        return commentRepository.save(reply);
    }

    /**
     * Updates the content of a comment owned by the authenticated user.
     *
     * @param commentId comment id
     * @param username current username
     * @param content replacement comment text
     * @return match id for redirecting
     */
    public Long updateComment(Long commentId, String username, String content) {
        User user = requireUser(username);
        ensureUserCanComment(user);
        Comment comment = requireComment(commentId);
        ensureOwner(comment, user, "Only the comment author can edit this comment.");
        if (comment.isDeleted()) {
            throw new CommentNotAllowedException("Deleted comments cannot be edited.");
        }

        comment.setContent(validateAndNormalizeContent(content));
        return comment.getMatch().getId();
    }

    /**
     * Soft-deletes a comment owned by the authenticated user.
     * Intentionally allowed for blocked users: blocking prevents posting new content, not retracting their own.
     *
     * @param commentId comment id
     * @param username current username
     * @return match id for redirecting
     */
    public Long deleteComment(Long commentId, String username) {
        User user = requireUser(username);
        Comment comment = requireComment(commentId);
        ensureOwner(comment, user, "Only the comment author can delete this comment.");

        markDeleted(comment, "Comment deleted by user", false);
        return comment.getMatch().getId();
    }

    /**
     * Marks a visible comment as reported by an authenticated user.
     *
     * @param commentId comment id
     * @param username reporting username
     * @return match id for redirecting
     */
    public Long reportComment(Long commentId, String username) {
        User user = requireUser(username);
        ensureUserCanComment(user);
        Comment comment = requireComment(commentId);
        if (comment.isDeleted() || !comment.isVisible()) {
            throw new CommentNotAllowedException("This comment cannot be reported.");
        }
        if (comment.getUser().getId().equals(user.getId())) {
            throw new CommentNotAllowedException("You cannot report your own comment.");
        }

        comment.setReported(true);
        return comment.getMatch().getId();
    }

    /**
     * Returns visible match comments as a nested thread.
     *
     * @param matchId match id
     * @return nested visible comment thread
     */
    @Transactional(readOnly = true)
    public List<CommentThreadItem> findThreadByMatchId(Long matchId) {
        List<Comment> comments = commentRepository.findThreadCandidatesByMatchIdOrderByCreatedAtAsc(matchId);
        Map<Long, CommentThreadItem> byId = new LinkedHashMap<>();
        List<CommentThreadItem> roots = new ArrayList<>();

        for (Comment comment : comments) {
            byId.put(comment.getId(), new CommentThreadItem(comment));
        }

        for (CommentThreadItem item : byId.values()) {
            Comment parent = item.getComment().getParentComment();
            if (parent != null && byId.containsKey(parent.getId())) {
                byId.get(parent.getId()).getReplies().add(item);
            } else {
                roots.add(item);
            }
        }

        roots.removeIf(this::isEmptyDeletedBranch);
        return roots;
    }

    /**
     * Counts visible, non-deleted comments in a prepared thread.
     *
     * @param thread prepared comment thread
     * @return visible comment count
     */
    public int countVisibleComments(List<CommentThreadItem> thread) {
        int count = 0;
        for (CommentThreadItem item : thread) {
            if (!item.getComment().isDeleted()) {
                count++;
            }
            count += countVisibleComments(item.getReplies());
        }
        return count;
    }

    /**
     * Returns comments written by the given user, newest first.
     *
     * @param username username to load comments for
     * @return comments written by the user
     */
    @Transactional(readOnly = true)
    public List<Comment> findByUsername(String username) {
        User user = requireUser(username);
        return commentRepository.findByUserIdWithMatchOrderByCreatedAtDesc(user.getId());
    }

    /**
     * Returns one page of comments for the admin moderation list.
     *
     * @param reportedOnly whether to include only reported comments
     * @param page zero-based page number
     * @return comment page
     */
    @Transactional(readOnly = true)
    public Page<Comment> findAdminCommentsPage(boolean reportedOnly, int page) {
        return Pagination.page(page, pageable -> commentRepository.findAdminCommentsPage(reportedOnly, pageable));
    }

    /**
     * Counts reported comments for the admin dashboard.
     *
     * @return number of reported comments
     */
    @Transactional(readOnly = true)
    public long countReported() {
        return commentRepository.countByReportedTrue();
    }

    /**
     * Hides a non-deleted comment from public match threads.
     *
     * @param commentId comment id
     * @return moderated comment
     */
    public Comment hideComment(Long commentId) {
        Comment comment = requireComment(commentId);
        ensureNotDeleted(comment);
        comment.setVisible(false);
        return comment;
    }

    /**
     * Restores a hidden comment to public match threads.
     *
     * @param commentId comment id
     * @return moderated comment
     */
    public Comment showComment(Long commentId) {
        Comment comment = requireComment(commentId);
        ensureNotDeleted(comment);
        comment.setVisible(true);
        return comment;
    }

    /**
     * Clears the report marker after an admin has reviewed a comment.
     *
     * @param commentId comment id
     * @return moderated comment
     */
    public Comment resolveReport(Long commentId) {
        Comment comment = requireComment(commentId);
        comment.setReported(false);
        return comment;
    }

    /**
     * Soft-deletes a comment after admin review while preserving reply structure.
     *
     * @param commentId comment id
     * @return moderated comment
     */
    public Comment deleteCommentAsAdmin(Long commentId) {
        Comment comment = requireComment(commentId);
        markDeleted(comment, "Comment removed by admin", true);
        return comment;
    }

    /**
     * Blocks the author of a comment from future comment actions. Block rules live in {@link UserService}.
     *
     * @param commentId comment id
     * @return comment whose author was blocked
     */
    public Comment blockCommentAuthor(Long commentId) {
        Comment comment = requireComment(commentId);
        userService.blockUser(comment.getUser().getId(), null);
        return comment;
    }

    /**
     * Allows the author of a comment to use comment actions again.
     *
     * @param commentId comment id
     * @return comment whose author was unblocked
     */
    public Comment unblockCommentAuthor(Long commentId) {
        Comment comment = requireComment(commentId);
        userService.unblockUser(comment.getUser().getId());
        return comment;
    }

    /**
     * Returns true when the user is blocked from comment actions.
     *
     * @param username username to check
     * @return true when blocked from commenting
     */
    @Transactional(readOnly = true)
    public boolean isBlockedFromCommenting(String username) {
        return userService.findByUsername(username)
                .map(User::isBlocked)
                .orElse(false);
    }

    /**
     * Resolves a username to a user or throws a not-found exception.
     *
     * @param username username to resolve
     * @return matching user
     */
    private User requireUser(String username) {
        return userService.getByUsernameOrThrow(username);
    }

    /**
     * Resolves a comment with author and match data loaded.
     *
     * @param commentId comment id
     * @return matching comment
     */
    private Comment requireComment(Long commentId) {
        return commentRepository.findByIdWithUserAndMatch(commentId)
                .orElseThrow(() -> new EntityNotFoundException(Comment.class));
    }

    /**
     * Ensures a user is allowed to create or report comments.
     *
     * @param user user to check
     */
    private void ensureUserCanComment(User user) {
        if (user.isBlocked()) {
            throw new CommentNotAllowedException("Your account is blocked from posting comments.");
        }
    }

    /**
     * Ensures the given user owns the comment.
     *
     * @param comment comment being mutated
     * @param user current user
     * @param message exception message when ownership fails
     */
    private void ensureOwner(Comment comment, User user, String message) {
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new CommentNotAllowedException(message);
        }
    }

    /**
     * Ensures the comment has not already been soft-deleted.
     *
     * @param comment comment to check
     */
    private void ensureNotDeleted(Comment comment) {
        if (comment.isDeleted()) {
            throw new CommentNotAllowedException("Deleted comments cannot be moderated this way.");
        }
    }

    /**
     * Applies the soft-delete state to a comment while preserving thread structure.
     *
     * @param comment comment to soft-delete
     * @param replacementContent content shown in place of the original text
     * @param clearReport whether to clear the reported marker
     */
    private void markDeleted(Comment comment, String replacementContent, boolean clearReport) {
        comment.setDeleted(true);
        comment.setVisible(false);
        comment.setContent(replacementContent);
        if (clearReport) {
            comment.setReported(false);
        }
    }

    /**
     * Trims and validates submitted comment text.
     *
     * @param content submitted comment text
     * @return normalized comment text
     */
    private String validateAndNormalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Comment cannot be empty");
        }
        if (normalized.length() > Comment.CONTENT_MAX) {
            throw new IllegalArgumentException(
                    "Comment must be " + Comment.CONTENT_MAX + " characters or fewer");
        }
        return normalized;
    }

    /**
     * Removes empty deleted reply branches from a thread tree.
     *
     * @param item thread item to inspect
     * @return true when this item is deleted and has no visible replies
     */
    private boolean isEmptyDeletedBranch(CommentThreadItem item) {
        item.getReplies().removeIf(this::isEmptyDeletedBranch);
        return item.getComment().isDeleted() && item.getReplies().isEmpty();
    }
}
