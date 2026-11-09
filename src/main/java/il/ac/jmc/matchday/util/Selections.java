package il.ac.jmc.matchday.util;

import java.util.List;
import java.util.Set;

/**
 * Utility for converting nullable multi-select request parameters into sets
 * used by templates to mark selected filter options.
 */
public final class Selections {

    /** Prevents instantiation of this utility class. */
    private Selections() {}

    /**
     * Returns the values as an immutable set, or an empty set when the list is {@code null}.
     *
     * @param values nullable list of selected values
     * @param <T> selected value type
     * @return immutable set of selected values
     */
    public static <T> Set<T> toSet(List<T> values) {
        return values != null ? Set.copyOf(values) : Set.of();
    }
}
