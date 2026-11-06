package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Move;

import java.util.List;
import java.util.Objects;

public record VCTResult(
        boolean found,
        Move winningMove,
        List<Move> winningSequence,
        int depth,
        long nodesSearched,
        long elapsedTimeMillis
) {
    public VCTResult {
        winningSequence = winningSequence == null
                ? List.of()
                : List.copyOf(winningSequence);
    }

    public static VCTResult success(
            List<Move> sequence,
            long nodesSearched,
            long elapsedTimeMillis
    ) {
        Objects.requireNonNull(sequence, "Sequence must not be null");
        if (sequence.isEmpty()) {
            throw new IllegalArgumentException("Winning sequence must not be empty");
        }
        Move winningMove = sequence.get(0);
        return new VCTResult(
                true,
                winningMove,
                sequence,
                sequence.size(),
                nodesSearched,
                elapsedTimeMillis
        );
    }

    public static VCTResult failure(
            long nodesSearched,
            long elapsedTimeMillis
    ) {
        return new VCTResult(
                false,
                null,
                List.of(),
                0,
                nodesSearched,
                elapsedTimeMillis
        );
    }
}

