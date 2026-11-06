package com.hieupt.gomokubot.search;

public record SearchLimits(
        int maxDepth,
        long maxNodes,
        long timeLimitMillis
) {

    public SearchLimits {
        if (maxDepth < 0) {
            throw new IllegalArgumentException(
                    "Maximum depth must not be negative"
            );
        }

        if (maxNodes < 0) {
            throw new IllegalArgumentException(
                    "Maximum nodes must not be negative"
            );
        }

        if (timeLimitMillis < 0) {
            throw new IllegalArgumentException(
                    "Time limit must not be negative"
            );
        }

        if (maxDepth == 0
                && maxNodes == 0
                && timeLimitMillis == 0) {
            throw new IllegalArgumentException(
                    "At least one search limit must be specified"
            );
        }
    }

    public static SearchLimits depthLimit(int maxDepth) {
        return new SearchLimits(maxDepth, 0, 0);
    }

    public static SearchLimits nodeLimit(long maxNodes) {
        return new SearchLimits(0, maxNodes, 0);
    }

    public static SearchLimits timeLimit(long timeLimitMillis) {
        return new SearchLimits(0, 0, timeLimitMillis);
    }

    public static SearchLimits depthAndTime(
            int maxDepth,
            long timeLimitMillis
    ) {
        return new SearchLimits(maxDepth, 0, timeLimitMillis);
    }

    public static SearchLimits unlimited() {
        return new SearchLimits(Integer.MAX_VALUE, 0, 0);
    }

    public boolean hasDepthLimit() {
        return maxDepth > 0;
    }

    public boolean hasNodeLimit() {
        return maxNodes > 0;
    }

    public boolean hasTimeLimit() {
        return timeLimitMillis > 0;
    }
}