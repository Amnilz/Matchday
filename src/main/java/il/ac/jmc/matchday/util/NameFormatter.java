package il.ac.jmc.matchday.util;

/**
 * Shared display-name formatting for user-entered entity names.
 */
public final class NameFormatter {

    /** Prevents instantiation of this utility class. */
    private NameFormatter() {}

    /**
     * Capitalises the first letter of each word; leaves the remaining letters unchanged.
     *
     * @param name name to format
     * @return title-cased name, or {@code null} when input is {@code null}
     */
    public static String toTitleCase(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return trimmed;

        String[] words = trimmed.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word.substring(1));
        }
        return sb.toString();
    }
}
