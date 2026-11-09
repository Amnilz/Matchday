package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.dto.RecentSearch;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serial;
import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Session-scoped storage for one browser's recent searches.
 */
@Service
@SessionScope
public class RecentSearchService implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Path prefix of the search page, used to detect and skip remember redirects. */
    private static final String SEARCH_PATH = "/search";
    /** Query parameter appended to suppress the remember-search hook on redirects. */
    private static final String SKIP_REMEMBER_PARAM = "skipRemember=true";
    /** Maximum number of recent searches retained per session. */
    private static final int MAX_RECENT_SEARCHES = 5;

    /** Saved searches for this session, newest first. */
    private final ArrayList<RecentSearch> recentSearches = new ArrayList<>();

    /**
     * Creates empty recent-search storage for one browser session.
     */
    public RecentSearchService() {}

    /**
     * Returns this session's saved searches, newest first; empty when none saved.
     *
     * @return saved recent searches
     */
    public List<RecentSearch> findRecentSearches() {
        return List.copyOf(recentSearches);
    }

    /**
     * Saves a search at the top of this session's list, deduplicated and capped to the last 5.
     *
     * @param search search snapshot to remember
     */
    public void rememberSearch(RecentSearch search) {
        recentSearches.removeIf(existing -> existing.queryString().equals(search.queryString()));
        recentSearches.addFirst(search);

        if (recentSearches.size() > MAX_RECENT_SEARCHES) {
            recentSearches.subList(MAX_RECENT_SEARCHES, recentSearches.size()).clear();
        }
    }

    /**
     * Removes the saved search matching the given query string, if present.
     *
     * @param queryString query string identifying the search
     */
    public void forgetSearch(String queryString) {
        if (queryString == null || queryString.isBlank()) return;
        recentSearches.removeIf(existing -> existing.queryString().equals(queryString));
    }

    /**
     * Removes all saved searches from this session.
     */
    public void clearSearches() {
        recentSearches.clear();
    }

    /**
     * Returns the query string from the request after stripping internal
     * parameters and entries with blank values.
     *
     * @param request current HTTP request
     * @return cleaned query string
     */
    public String cleanQueryString(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) return "";
        return Arrays.stream(queryString.split("&"))
                .filter(part -> !part.startsWith("fragment="))
                .filter(part -> !part.startsWith("skipRemember="))
                .filter(part -> !part.startsWith("type="))
                .filter(this::hasMeaningfulValue)
                .collect(Collectors.joining("&"));
    }

    /**
     * Builds the search return URL from a cleaned query string.
     *
     * @param queryString cleaned query string
     * @return search return URL
     */
    public String buildReturnUrl(String queryString) {
        return queryString.isBlank() ? SEARCH_PATH : SEARCH_PATH + "?" + queryString;
    }

    /**
     * Appends {@code skipRemember=true} to a search redirect URL so the
     * target page does not re-save the search in the session.
     *
     * @param returnUrl search return URL
     * @return return URL with skip marker appended when applicable
     */
    public String appendSkipRemember(String returnUrl) {
        if (returnUrl.equals(SEARCH_PATH)) return SEARCH_PATH + "?" + SKIP_REMEMBER_PARAM;
        if (returnUrl.startsWith(SEARCH_PATH + "?")) return returnUrl + "&" + SKIP_REMEMBER_PARAM;
        return returnUrl;
    }

    /**
     * Returns true when a query-string part carries a non-blank decoded value.
     *
     * @param queryPart raw key=value fragment from a query string
     * @return true when the value is non-blank
     */
    private boolean hasMeaningfulValue(String queryPart) {
        int separator = queryPart.indexOf('=');
        if (separator < 0) return true;
        String rawValue = queryPart.substring(separator + 1).replace("+", " ");
        try {
            return !URLDecoder.decode(rawValue, StandardCharsets.UTF_8).isBlank();
        } catch (IllegalArgumentException ignored) {
            return true;
        }
    }
}
