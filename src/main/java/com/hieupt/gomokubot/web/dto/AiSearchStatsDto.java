package com.hieupt.gomokubot.web.dto;

import com.hieupt.gomokubot.search.SearchResult;

public record AiSearchStatsDto(
        Integer bestX,
        Integer bestY,
        int score,
        int depth,
        long nodesSearched,
        long elapsedTimeMillis,
        boolean completed
) {
    public static AiSearchStatsDto from(SearchResult result) {
        if (result == null) {
            return null;
        }
        Integer x = result.hasBestMove() ? result.bestMove().x() : null;
        Integer y = result.hasBestMove() ? result.bestMove().y() : null;
        return new AiSearchStatsDto(
                x,
                y,
                result.score(),
                result.completedDepth(),
                result.nodesSearched(),
                result.elapsedTimeMillis(),
                result.completed()
        );
    }
}

