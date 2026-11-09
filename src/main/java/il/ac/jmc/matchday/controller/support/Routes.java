package il.ac.jmc.matchday.controller.support;

/**
 * Route paths shared by multiple controllers (redirect targets, return-URL prefixes).
 * Single-controller paths stay local to their controller.
 */
public final class Routes {

    /** Spring MVC view-name prefix that triggers a client redirect. */
    public static final String REDIRECT_PREFIX = "redirect:";

    /** Profile page path. */
    public static final String PROFILE = "/profile";

    /** Public teams list path. */
    public static final String TEAMS   = "/teams";

    /** Advanced search page path. */
    public static final String SEARCH  = "/search";

    /** Prevents instantiation of this constants class. */
    private Routes() {}
}
