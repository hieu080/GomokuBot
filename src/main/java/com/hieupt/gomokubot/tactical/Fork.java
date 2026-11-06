package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Move;

import java.util.Objects;

public record Fork(ForkType type, Move move) {
    public Fork {
        Objects.requireNonNull(type, "ForkType must not be null");
        Objects.requireNonNull(move, "Move must not be null");
    }
}

