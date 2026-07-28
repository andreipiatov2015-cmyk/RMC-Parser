package com.rmc.version;

/**
 * Comparator for Version objects.
 * Returns NEWER, OLDER, or EQUAL based on comparison.
 */
public final class VersionComparator {

    private VersionComparator() {
        // Utility class
    }

    /**
     * Compare two Version objects.
     *
     * @param first  The first version to compare
     * @param second The second version to compare
     * @return NEWER if first > second, OLDER if first < second, EQUAL if equal
     */
    public static VersionComparison compare(Version first, Version second) {
        int result = first.compareTo(second);
        if (result > 0) {
            return VersionComparison.NEWER;
        } else if (result < 0) {
            return VersionComparison.OLDER;
        } else {
            return VersionComparison.EQUAL;
        }
    }

    /**
     * Check if the first version is newer than the second.
     *
     * @param first  The first version
     * @param second The second version
     * @return true if first > second
     */
    public static boolean isNewer(Version first, Version second) {
        return compare(first, second) == VersionComparison.NEWER;
    }

    /**
     * Check if the first version is older than the second.
     *
     * @param first  The first version
     * @param second The second version
     * @return true if first < second
     */
    public static boolean isOlder(Version first, Version second) {
        return compare(first, second) == VersionComparison.OLDER;
    }

    /**
     * Check if both versions are equal.
     *
     * @param first  The first version
     * @param second The second version
     * @return true if first == second
     */
    public static boolean isEqual(Version first, Version second) {
        return compare(first, second) == VersionComparison.EQUAL;
    }
}