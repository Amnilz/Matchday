package il.ac.jmc.matchday.dto;

import il.ac.jmc.matchday.entity.Comment;

import java.util.ArrayList;
import java.util.List;

/**
 * Display node for a nested match comment thread.
 */
public class CommentThreadItem {

    /** Comment represented by this thread node. */
    private final Comment comment;

    /** Direct replies to this comment, in display order. */
    private final List<CommentThreadItem> replies = new ArrayList<>();

    /**
     * Wraps a comment as a thread node with an initially empty reply list.
     *
     * @param comment comment represented by this node
     */
    public CommentThreadItem(Comment comment) {
        this.comment = comment;
    }

    /**
     * Returns the comment at this node.
     *
     * @return comment at this node
     */
    public Comment getComment() { return comment; }

    /**
     * Returns the mutable list of direct replies, in display order.
     *
     * @return direct replies
     */
    public List<CommentThreadItem> getReplies() { return replies; }
}
