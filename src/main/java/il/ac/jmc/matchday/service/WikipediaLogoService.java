package il.ac.jmc.matchday.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import il.ac.jmc.matchday.entity.League;
import il.ac.jmc.matchday.entity.Player;
import il.ac.jmc.matchday.entity.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Background service that bulk-fetches image URLs from Wikipedia for teams and
 * players that currently have none.
 */
@Service
public class WikipediaLogoService {

    /** Logger for this service. */
    private static final Logger log = LoggerFactory.getLogger(WikipediaLogoService.class);

    /** Maximum number of retry attempts on HTTP 429 (rate-limit) responses. */
    private static final int MAX_RETRIES = 3;
    /** Milliseconds to sleep between Wikipedia API calls to respect rate limits. */
    private static final long SLEEP_MS = 2_500;
    /** Milliseconds to wait before retrying after a rate-limit response. */
    private static final long RETRY_WAIT_MS = 5_000;

    /** Wikipedia MediaWiki Action API endpoint. */
    private static final String ACTION_API = "https://en.wikipedia.org/w/api.php";
    /** Wikipedia REST summary API endpoint template (requires a {@code title} path variable). */
    private static final String SUMMARY_API = "https://en.wikipedia.org/api/rest_v1/page/summary/{title}";

    /** HTTP client used for all Wikipedia requests. */
    private final RestClient restClient;
    /** JSON mapper used to parse Wikipedia API responses. */
    private final ObjectMapper objectMapper;
    /** Team service used to read and save teams. */
    private final TeamService teamService;
    /** League service used to read and save leagues. */
    private final LeagueService leagueService;
    /** Player service used to read and save players. */
    private final PlayerService playerService;
    /** Flag indicating whether a background fetch is currently in progress. */
    private final AtomicBoolean running = new AtomicBoolean(false);
    /** The completion message from the most recent fetch job (polled and cleared on read). */
    private final AtomicReference<String> lastMessage = new AtomicReference<>("");
    /** The entity type ({@code "teams"}, {@code "leagues"}, or {@code "players"}) processed by the last fetch. */
    private final AtomicReference<String> lastType = new AtomicReference<>("");

    /**
     * Creates the Wikipedia image service.
     *
     * @param teamService service used to read and save teams
     * @param leagueService service used to read and save leagues
     * @param playerService service used to read and save players
     */
    public WikipediaLogoService(TeamService teamService,
                                LeagueService leagueService,
                                PlayerService playerService,
                                @Value("${app.wikipedia.user-agent}") String userAgent) {
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Api-User-Agent", userAgent)
                .build();
        this.objectMapper = new ObjectMapper();
        this.teamService = teamService;
        this.leagueService = leagueService;
        this.playerService = playerService;
    }

    /**
     * Returns true while a background fetch is in progress.
     *
     * @return true when any Wikipedia fetch job is running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Returns the last completion message once, then clears it (poll-and-consume).
     *
     * @return last completion message, or blank when none is pending
     */
    public String pollMessage() {
        return lastMessage.getAndSet("");
    }

    /**
     * Returns which entity type the last fetch handled: "teams", "leagues" or "players".
     *
     * @return last fetched entity type
     */
    public String pollType() {
        return lastType.get();
    }

    /**
     * Asynchronously fills in missing team logos from Wikipedia; no-op when a fetch is already running.
     */
    @Async
    public void fetchMissingTeamLogos() {
        if (!running.compareAndSet(false, true)) return;
        fetchMissing(
                teamService::findAll,
                Team::getLogoUrl,
                Team::getName,
                Team::setLogoUrl,
                teamService::save,
                "football club",
                "teams"
        );
    }

    /**
     * Asynchronously fills in missing league logos from Wikipedia; no-op when a fetch is already running.
     */
    @Async
    public void fetchMissingLeagueLogos() {
        if (!running.compareAndSet(false, true)) return;
        fetchMissing(
                leagueService::findAll,
                League::getLogoUrl,
                League::getName,
                League::setLogoUrl,
                leagueService::save,
                "association football league",
                "leagues"
        );
    }

    /**
     * Asynchronously fills in missing player photos from Wikipedia; no-op when a fetch is already running.
     */
    @Async
    public void fetchMissingPlayerPhotos() {
        if (!running.compareAndSet(false, true)) return;
        fetchMissing(
                playerService::findAll,
                Player::getPhotoUrl,
                Player::getFullName,
                Player::setPhotoUrl,
                playerService::save,
                "footballer",
                "players"
        );
    }

    /**
     * Generic helper that iterates over all entities of type {@code T}, skips those that
     * already have a URL, and attempts to fill in the missing URL via Wikipedia.
     *
     * @param <T> entity type
     * @param entitySupplier supplies all entities of the type
     * @param getUrl extracts the current URL from an entity (may be {@code null} or blank)
     * @param getName extracts the display name used for the Wikipedia search query
     * @param setUrl applies the resolved URL back to the entity
     * @param save persists the updated entity
     * @param keywords additional search keywords (e.g. {@code "football club"}) appended to the name query
     * @param type human-readable entity type label used in log messages (e.g. {@code "teams"})
     */
    private <T> void fetchMissing(Supplier<List<T>> entitySupplier,
                                  Function<T, String> getUrl,
                                  Function<T, String> getName,
                                  BiConsumer<T, String> setUrl,
                                  Consumer<T> save,
                                  String keywords,
                                  String type) {
        int updated = 0;
        int skipped = 0;
        int notFound = 0;

        try {
            for (T entity : entitySupplier.get()) {
                String current = getUrl.apply(entity);
                if (current != null && !current.isBlank()) {
                    skipped++;
                    continue;
                }

                Optional<String> imageUrl = fetchImageUrl(getName.apply(entity), keywords);
                if (imageUrl.isPresent()) {
                    setUrl.accept(entity, imageUrl.get());
                    save.accept(entity);
                    updated++;
                } else {
                    notFound++;
                }
            }
        } finally {
            running.set(false);
            lastType.set(type);
            lastMessage.set("Wikipedia fetch complete - "
                    + updated + " updated, "
                    + skipped + " already had one, "
                    + notFound + " not found.");
            log.info("Wikipedia {} fetch complete: {} updated, {} skipped, {} not found.",
                    type, updated, skipped, notFound);
        }
    }

    /**
     * Searches Wikipedia for the entity with the given name and keywords, then retrieves the
     * thumbnail URL from the article summary.  Follows Wikipedia's spelling suggestion when
     * one is returned.  League lookups use a dedicated multi-query strategy.
     *
     * @param name display name of the entity to look up
     * @param keywords additional search terms (e.g. {@code "football club"})
     * @return thumbnail URL if one was found, or empty
     */
    private Optional<String> fetchImageUrl(String name, String keywords) {
        try {
            if (isLeagueLookup(keywords)) {
                Optional<String> exactImageUrl = fetchSummaryThumbnail(name);
                if (exactImageUrl.isPresent()) return exactImageUrl;

                Optional<String> leagueTitle = findLeagueArticleTitle(name, keywords);
                return leagueTitle.isPresent()
                        ? fetchSummaryThumbnail(leagueTitle.get())
                        : Optional.empty();
            }

            Optional<String> title = findArticleTitle(name, keywords);
            return title.isPresent() ? fetchSummaryThumbnail(title.get()) : Optional.empty();
        } catch (Exception e) {
            log.warn("Wikipedia fetch failed for '{}': {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns {@code true} when the keywords indicate a league-type entity lookup.
     *
     * @param keywords search keywords to inspect
     * @return {@code true} if keywords contain "league"
     */
    private boolean isLeagueLookup(String keywords) {
        return keywords != null && keywords.toLowerCase().contains("league");
    }

    /**
     * Tries multiple search queries to find the Wikipedia article title for a league entity.
     *
     * @param name league name to search for
     * @param keywords additional keywords appended to the first query attempt
     * @return Wikipedia article title if found, or empty
     * @throws Exception if the Wikipedia search request fails
     */
    private Optional<String> findLeagueArticleTitle(String name, String keywords) throws Exception {
        List<String> queries = List.of(name + " " + keywords, name);
        for (String query : queries) {
            Optional<String> title = firstPreferredLeagueTitle(search(query, 5), name);
            if (title.isPresent()) return title;
        }
        return Optional.empty();
    }

    /**
     * Searches Wikipedia for a non-league entity and returns the article title of the best match,
     * following any spelling suggestion returned by the search API.
     *
     * @param name entity name to search for
     * @param keywords additional search keywords (may be blank)
     * @return Wikipedia article title if found, or empty
     * @throws Exception if the Wikipedia search request fails
     */
    private Optional<String> findArticleTitle(String name, String keywords) throws Exception {
        String query = keywords == null || keywords.isBlank() ? name : name + " " + keywords;
        JsonNode search = search(query, 1);
        String suggestion = search.path("query").path("searchinfo").path("suggestion").asText("");

        if (!suggestion.isBlank()) {
            Optional<String> correctedTitle = firstSearchTitle(search(suggestion, 1));
            if (correctedTitle.isPresent()) return correctedTitle;
        }

        return firstSearchTitle(search);
    }

    /**
     * Scans a Wikipedia search result for the first hit whose title matches or ends with
     * the normalized entity name ג€” a heuristic for selecting the canonical league article.
     *
     * @param searchJson parsed Wikipedia search response
     * @param name original entity name used to build the normalized comparison value
     * @return matched article title, or empty if none of the hits match
     */
    private Optional<String> firstPreferredLeagueTitle(JsonNode searchJson, String name) {
        JsonNode hits = searchJson.path("query").path("search");
        if (!hits.isArray() || hits.isEmpty()) return Optional.empty();

        String normalizedName = normalizeTitle(name);
        for (JsonNode hit : hits) {
            String title = hit.path("title").asText("");
            String normalizedTitle = normalizeTitle(title);
            if (normalizedTitle.equals(normalizedName)
                    || normalizedTitle.endsWith(" " + normalizedName)) {
                return Optional.of(title);
            }
        }
        return Optional.empty();
    }

    /**
     * Normalises a Wikipedia article title for fuzzy comparison by lower-casing,
     * removing parenthetical disambiguation suffixes, and collapsing non-alphanumeric runs to spaces.
     *
     * @param title raw Wikipedia title to normalise
     * @return normalised title
     */
    private String normalizeTitle(String title) {
        return title.toLowerCase()
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    /**
     * Calls the Wikipedia search API and returns the parsed JSON response.
     * Sleeps for {@link #SLEEP_MS} milliseconds after the call to respect rate limits.
     *
     * @param query search query string
     * @param limit maximum number of results to request
     * @return parsed JSON response node
     * @throws Exception if the HTTP call fails or JSON cannot be parsed
     */
    private JsonNode search(String query, int limit) throws Exception {
        String json = getWithRetry(() -> restClient.get()
                .uri(ACTION_API, b -> b
                        .queryParam("action", "query")
                        .queryParam("list", "search")
                        .queryParam("srsearch", query)
                        .queryParam("format", "json")
                        .queryParam("srlimit", limit)
                        .queryParam("srprop", "")
                        .build())
                .retrieve()
                .body(String.class));

        sleep();
        return json != null ? objectMapper.readTree(json) : objectMapper.createObjectNode();
    }

    /**
     * Extracts the title of the first search hit from a Wikipedia search response.
     *
     * @param searchJson parsed Wikipedia search response
     * @return title of the first hit, or empty if there are no hits
     */
    private static Optional<String> firstSearchTitle(JsonNode searchJson) {
        JsonNode hits = searchJson.path("query").path("search");
        if (!hits.isArray() || hits.isEmpty()) return Optional.empty();

        String title = hits.get(0).path("title").asText("");
        return title.isBlank() ? Optional.empty() : Optional.of(title);
    }

    /**
     * Fetches the thumbnail image URL from the Wikipedia REST summary API for the given article title.
     * Sleeps for {@link #SLEEP_MS} milliseconds after the call to respect rate limits.
     *
     * @param title Wikipedia article title
     * @return thumbnail source URL, or empty if the article has no thumbnail
     * @throws Exception if the HTTP call fails or JSON cannot be parsed
     */
    private Optional<String> fetchSummaryThumbnail(String title) throws Exception {
        String json = getWithRetry(() -> restClient.get()
                .uri(SUMMARY_API, title)
                .header("Accept", "application/json")
                .retrieve()
                .body(String.class));

        sleep();
        if (json == null) return Optional.empty();

        String source = objectMapper.readTree(json).path("thumbnail").path("source").asText("");
        return source.isBlank() ? Optional.empty() : Optional.of(source);
    }

    /**
     * Executes the given HTTP request supplier, retrying up to {@link #MAX_RETRIES} times
     * when Wikipedia responds with HTTP 429 (Too Many Requests).
     * Returns {@code null} on 403/404 or after exhausting retries.
     *
     * @param request HTTP request to execute
     * @return response body, or {@code null} if the request ultimately failed
     */
    private String getWithRetry(Supplier<String> request) {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                return request.get();
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    attempts++;
                    log.warn("Wikipedia rate-limited - waiting {} s before retry {}/{}.",
                            RETRY_WAIT_MS / 1000, attempts, MAX_RETRIES);
                    waitBeforeRetry();
                } else if (e.getStatusCode() == HttpStatus.FORBIDDEN
                        || e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    return null;
                } else {
                    log.warn("Wikipedia GET failed ({}): {}", e.getStatusCode(), e.getMessage());
                    return null;
                }
            } catch (Exception e) {
                log.warn("Wikipedia GET error: {}", e.getMessage());
                return null;
            }
        }
        log.warn("Wikipedia GET gave up after {} attempts.", MAX_RETRIES);
        return null;
    }

    /**
     * Sleeps for {@link #RETRY_WAIT_MS} milliseconds before retrying a rate-limited request.
     * Restores the interrupt flag if interrupted.
     */
    private void waitBeforeRetry() {
        try {
            Thread.sleep(RETRY_WAIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sleeps for {@link #SLEEP_MS} milliseconds to pace Wikipedia API calls.
     * Restores the interrupt flag if interrupted.
     */
    private void sleep() {
        try {
            Thread.sleep(SLEEP_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
