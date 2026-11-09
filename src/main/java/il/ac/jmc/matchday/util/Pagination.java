package il.ac.jmc.matchday.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

/**
 * Shared pagination defaults and request normalization for list pages.
 */
public final class Pagination {

    /** Rows per page on every paginated list. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Prevents instantiation of this utility class. */
    private Pagination() {}

    /**
     * Returns an unsorted page request for the given page number.
     *
     * @param page requested page number
     * @return page request with default size
     */
    private static PageRequest request(int page) {
        return PageRequest.of(normalizePage(page), DEFAULT_PAGE_SIZE);
    }

    /**
     * Returns a sorted page request for the given page number.
     *
     * @param page requested page number
     * @param sort sort order
     * @return sorted page request with default size
     */
    private static PageRequest request(int page, Sort sort) {
        return PageRequest.of(normalizePage(page), DEFAULT_PAGE_SIZE, sort);
    }

    /**
     * Returns a request for the first page holding at most {@code limit} rows (minimum 1).
     *
     * @param limit requested row limit
     * @return first-page request
     */
    public static PageRequest first(int limit) {
        return PageRequest.of(0, Math.max(1, limit));
    }

    /**
     * Returns a sorted request for the first page holding at most {@code limit} rows (minimum 1).
     *
     * @param limit requested row limit
     * @param sort sort order
     * @return sorted first-page request
     */
    public static PageRequest first(int limit, Sort sort) {
        return PageRequest.of(0, Math.max(1, limit), sort);
    }

    /**
     * Returns an empty page carrying the given page number and sort, for invalid-filter shortcuts.
     *
     * @param page requested page number
     * @param sort sort order
     * @param <T> page element type
     * @return empty page with normalized paging metadata
     */
    public static <T> Page<T> empty(int page, Sort sort) {
        return Page.empty(request(page, sort));
    }

    /**
     * Fetches the requested page unsorted; see {@link #page(int, Sort, Function)}.
     *
     * @param page requested page number
     * @param fetcher callback that fetches a page
     * @param <T> page element type
     * @return fetched page, clamped to the last page when needed
     */
    public static <T> Page<T> page(int page, Function<Pageable, Page<T>> fetcher) {
        return page(page, Sort.unsorted(), fetcher);
    }

    /**
     * Fetches the requested page; when the page number is past the last page
     * (e.g. after deletions), re-fetches the last existing page instead.
     *
     * @param page requested page number
     * @param sort sort order
     * @param fetcher callback that fetches a page
     * @param <T> page element type
     * @return fetched page, clamped to the last page when needed
     */
    public static <T> Page<T> page(int page, Sort sort, Function<Pageable, Page<T>> fetcher) {
        Page<T> result = fetcher.apply(request(page, sort));
        if (result.getTotalPages() > 0 && result.getNumber() >= result.getTotalPages()) {
            return fetcher.apply(request(result.getTotalPages() - 1, sort));
        }
        return result;
    }

    /**
     * Clamps a request page number to zero or above.
     *
     * @param page requested page number
     * @return normalized page number
     */
    public static int normalizePage(int page) {
        return Math.max(0, page);
    }
}
