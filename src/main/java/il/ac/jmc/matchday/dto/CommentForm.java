package il.ac.jmc.matchday.dto;

import il.ac.jmc.matchday.entity.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form DTO for adding a comment to a match.
 */
public class CommentForm {

    /** Maximum allowed comment length, shared with the {@link Comment} entity. */
    public static final int CONTENT_MAX = Comment.CONTENT_MAX;

    /**
     * Creates an empty comment form for MVC binding.
     */
    public CommentForm() {}

    /** Submitted comment text. */
    @NotBlank(message = "Comment cannot be empty")
    @Size(max = CONTENT_MAX, message = "Comment must be " + CONTENT_MAX + " characters or fewer")
    private String content;

    /**
     * Returns the submitted comment text.
     *
     * @return comment text
     */
    public String getContent() { return content; }

    /**
     * Sets the submitted comment text.
     *
     * @param content comment text
     */
    public void setContent(String content) { this.content = content; }
}
