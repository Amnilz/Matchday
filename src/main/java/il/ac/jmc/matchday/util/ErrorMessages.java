package il.ac.jmc.matchday.util;

/**
 * Utility for turning exceptions into user-readable flash messages.
 */
public final class ErrorMessages {

    /** Prevents instantiation of this utility class. */
    private ErrorMessages() {}

    /**
     * Returns the exception message, or the fallback when the message is missing or blank.
     *
     * @param ex exception to read
     * @param fallback fallback message
     * @return user-readable message
     */
    public static String messageOrDefault(Exception ex, String fallback) {
        return (ex.getMessage() == null || ex.getMessage().isBlank()) ? fallback : ex.getMessage();
    }
}
