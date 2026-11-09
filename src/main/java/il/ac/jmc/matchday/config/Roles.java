package il.ac.jmc.matchday.config;

/**
 * Constants for the Spring Security authority strings used in this application.
 * Use these instead of inline string literals to avoid typos and duplication.
 */
public final class Roles {

    /** Authority string for administrators. */
    public static final String ADMIN = "ROLE_ADMIN";

    /** Authority string for regular authenticated users. */
    public static final String USER  = "ROLE_USER";

    /** Prevents instantiation of this constants class. */
    private Roles() {}
}
