package il.ac.jmc.matchday.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * Utility for reading the authenticated username from a {@link Principal}.
 */
public final class PrincipalUtil {

    /** Prevents instantiation of this utility class. */
    private PrincipalUtil() {}

    /**
     * Returns the principal's name, or {@code null} when not authenticated.
     *
     * @param principal current principal, or {@code null}
     * @return username or {@code null}
     */
    public static String nameOrNull(Principal principal) {
        return principal != null ? principal.getName() : null;
    }

    /**
     * Returns the principal's name, or throws 401 Unauthorized when not authenticated.
     *
     * @param principal current principal
     * @return authenticated username
     */
    public static String requireName(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return principal.getName();
    }
}
