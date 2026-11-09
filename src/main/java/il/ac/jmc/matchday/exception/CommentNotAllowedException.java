package il.ac.jmc.matchday.exception;

import java.io.Serial;

/**
 * Raised when an authenticated user is not allowed to perform a comment action.
 */
public class CommentNotAllowedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new CommentNotAllowedException with the specified message.
     * @param message the detail message explaining why the comment action is not allowed
     */
    public CommentNotAllowedException(String message) {
        super(message);
    }
}
