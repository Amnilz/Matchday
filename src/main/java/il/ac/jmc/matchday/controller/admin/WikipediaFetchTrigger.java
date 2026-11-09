package il.ac.jmc.matchday.controller.admin;

import il.ac.jmc.matchday.service.WikipediaLogoService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Shared guard for the admin "fetch from Wikipedia" actions: starts the given
 * fetch unless one is already running, in which case it flashes an error.
 */
final class WikipediaFetchTrigger {

    /** Error message flashed when a fetch is requested while one is already in progress. */
    private static final String ALREADY_RUNNING_MESSAGE = "A Wikipedia fetch is already in progress.";

    /** Prevents instantiation of this utility class. */
    private WikipediaFetchTrigger() {}

    /**
     * Starts the given Wikipedia fetch unless one is already in progress,
     * in which case an error flash attribute is added and the fetch is skipped.
     *
     * @param service the Wikipedia logo service used to check running state
     * @param fetch the fetch action to run when no fetch is active
     * @param redirectAttributes flash scope for error messages
     * @param redirect the redirect view name to return
     * @return the redirect view name
     */
    static String start(WikipediaLogoService service,
                        Runnable fetch,
                        RedirectAttributes redirectAttributes,
                        String redirect) {
        if (service.isRunning()) {
            redirectAttributes.addFlashAttribute("error", ALREADY_RUNNING_MESSAGE);
        } else {
            fetch.run();
        }
        return redirect;
    }
}
