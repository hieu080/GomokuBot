package com.hieupt.gomokubot.search;

import com.hieupt.gomokubot.core.Move;

public record SearchResult(
        Move bestMove,
        int score,
        int completedDepth,
        long nodesSearched,
        long elapsedTimeMillis,
        boolean completed
) {

    public SearchResult {
        if (completedDepth < 0) {
            throw new IllegalArgumentException(
                    "Completed depth must not be negative"
            );
        }

        if (nodesSearched < 0) {
            throw new IllegalArgumentException(
                    "Nodes searched must not be negative"
            );
        }

        if (elapsedTimeMillis < 0) {
            throw new IllegalArgumentException(
                    "Elapsed time must not be negative"
            );
        }
    }

    public boolean hasBestMove() {
        return bestMove != null;
    }

    public double getNodesPerSecond() {
        if (elapsedTimeMillis == 0) {
            return 0.0;
        }

        return nodesSearched * 1000.0 / elapsedTimeMillis;
    }
}