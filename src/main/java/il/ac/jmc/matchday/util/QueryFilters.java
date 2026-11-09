package il.ac.jmc.matchday.util;

import java.util.List;

/**
 * Shared helpers for optional repository IN (...) filters.
 * JPQL forbids binding an empty list, so empty filters bind a sentinel id
 * (matched by no row) together with a "filter empty" flag.
 */
public final class QueryFilters {

    /** Sentinel id that matches no real row; bound when a filter list is empty. */
    public static final Long NO_MATCH_ID = -1L;

    /** Prevents instantiation of this utility class. */
    private QueryFilters() {}

    /**
     * Returns the ids, or a single-sentinel list when the filter is empty.
     *
     * @param ids requested ids
     * @return ids suitable for binding to an {@code IN} query
     */
    public static List<Long> idsOrSentinel(List<Long> ids) {
        return (ids == null || ids.isEmpty()) ? List.of(NO_MATCH_ID) : ids;
    }
}
