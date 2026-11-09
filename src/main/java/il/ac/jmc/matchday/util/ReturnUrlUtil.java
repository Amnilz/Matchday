package il.ac.jmc.matchday.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Enumeration;
import java.util.List;

/**
 * Utility for validating relative return-URL values before redirecting.
 */
public final class ReturnUrlUtil {

    /** Root path for the home page. */
    private static final String HOME_PATH = "/";
    /** Public search path. */
    private static final String SEARCH_PATH = "/search";
    /** Public players path. */
    private static final String PLAYERS_PATH = "/players";
    /** Public teams path. */
    private static final String TEAMS_PATH = "/teams";
    /** Public matches path. */
    private static final String MATCHES_PATH = "/matches";
    /** Public leagues path. */
    private static final String LEAGUES_PATH = "/leagues";
    /** Profile path. */
    private static final String PROFILE_PATH = "/profile";
    /** AJAX fragment flag that should not be preserved in return URLs. */
    private static final String FRAGMENT_PARAM = "fragment";
    /** Standard pagination parameter. */
    private static final String PAGE_PARAM = "page";
    /** Player-stat sort field parameter. */
    private static final String SORT_BY_PARAM = "sortBy";
    /** Player-stat sort direction parameter. */
    private static final String DIRECTION_PARAM = "dir";
    /** Labels for exact return paths. */
    private static final List<RouteLabel> EXACT_LABELS = List.of(
            new RouteLabel(HOME_PATH, "Home"),
            new RouteLabel(SEARCH_PATH, "Search"),
            new RouteLabel(PROFILE_PATH, "Profile"),
            new RouteLabel(PLAYERS_PATH, "Players"),
            new RouteLabel(TEAMS_PATH, "Teams"),
            new RouteLabel(MATCHES_PATH, "Matches"),
            new RouteLabel(LEAGUES_PATH, "Leagues")
    );
    /** Labels for detail-page return path prefixes. */
    private static final List<RouteLabel> DETAIL_LABELS = List.of(
            new RouteLabel(PLAYERS_PATH + "/", "Player"),
            new RouteLabel(TEAMS_PATH + "/", "Team"),
            new RouteLabel(MATCHES_PATH + "/", "Match"),
            new RouteLabel(LEAGUES_PATH + "/", "League")
    );

    /** Prevents instantiation of this utility class. */
    private ReturnUrlUtil() {}

    /**
     * Returns {@code true} when {@code url} is a safe same-site relative URL:
     * non-null, starts with {@code /}, does not start with {@code //},
     * and contains no CR or LF characters.
     *
     * @param url URL to validate
     * @return true when the URL is safe for same-site redirects
     */
    public static boolean isSafe(String url) {
        return url != null
                && url.startsWith("/")
                && !url.startsWith("//")
                && !url.contains("\r")
                && !url.contains("\n");
    }

    /**
     * Returns {@code true} when {@code url} is {@linkplain #isSafe(String) safe}
     * and starts with one of the given path prefixes.
     *
     * @param url URL to validate
     * @param allowedPrefixes allowed path prefixes
     * @return true when the URL is safe and starts with an allowed prefix
     */
    public static boolean isSafe(String url, String... allowedPrefixes) {
        if (!isSafe(url)) return false;
        for (String prefix : allowedPrefixes) {
            if (url.startsWith(prefix)) return true;
        }
        return false;
    }

    /**
     * Returns a safe same-site URL, or a fallback when the submitted value is unsafe.
     *
     * @param url candidate return URL
     * @param fallback fallback path to use when the candidate is unsafe
     * @return safe return URL
     */
    public static String safeOrDefault(String url, String fallback) {
        return isSafe(url) ? url : fallback;
    }

    /**
     * Returns a short UI label that describes where a safe return URL leads.
     *
     * @param url candidate return URL
     * @param fallback fallback label when the URL is unsafe or unknown
     * @return label for the return link
     */
    public static String labelOrDefault(String url, String fallback) {
        if (!isSafe(url)) return fallback;

        String path = pathOnly(url);
        for (RouteLabel label : EXACT_LABELS) {
            if (label.path().equals(path)) return label.label();
        }
        for (RouteLabel label : DETAIL_LABELS) {
            if (path.startsWith(label.path())) return label.label();
        }
        return fallback;
    }

    /**
     * Builds a same-site return URL for the current request, excluding fragment-only AJAX parameters.
     *
     * @param request current HTTP request
     * @return relative return URL
     */
    public static String currentRequestUrl(HttpServletRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(request.getRequestURI());
        boolean hasSortBy = hasText(request.getParameter(SORT_BY_PARAM));

        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (shouldSkipParam(name, request, hasSortBy)) continue;

            for (String value : request.getParameterValues(name)) {
                if (hasText(value)) {
                    builder.queryParam(name, value);
                }
            }
        }

        return builder.build().toUriString();
    }

    /**
     * Returns true when a request parameter should not be copied into a return URL.
     *
     * @param name parameter name
     * @param request current request
     * @param hasSortBy whether an active sort field is present
     * @return true when the parameter is empty, internal, or default noise
     */
    private static boolean shouldSkipParam(String name, HttpServletRequest request, boolean hasSortBy) {
        if (FRAGMENT_PARAM.equals(name)) return true;
        if (PAGE_PARAM.equals(name) && "0".equals(request.getParameter(name))) return true;
        if (DIRECTION_PARAM.equals(name) && !hasSortBy) return true;
        return allBlank(request.getParameterValues(name));
    }

    /**
     * Returns true when every value is blank.
     *
     * @param values parameter values
     * @return true when no non-blank value exists
     */
    private static boolean allBlank(String[] values) {
        if (values == null) return true;
        for (String value : values) {
            if (hasText(value)) return false;
        }
        return true;
    }

    /**
     * Returns true when a value contains non-whitespace text.
     *
     * @param value candidate value
     * @return true when non-blank
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Strips query string and fragment parts from a relative URL.
     *
     * @param url relative URL
     * @return path portion only
     */
    private static String pathOnly(String url) {
        int queryIndex = url.indexOf('?');
        int fragmentIndex = url.indexOf('#');
        int end = url.length();
        if (queryIndex >= 0) end = queryIndex;
        if (fragmentIndex >= 0 && fragmentIndex < end) end = fragmentIndex;
        return url.substring(0, end);
    }

    /**
     * Label for a route path or prefix.
     *
     * @param path route path or prefix
     * @param label user-facing return-link label
     */
    private record RouteLabel(String path, String label) {}
}
