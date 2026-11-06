package com.hieupt.gomokubot.tactical;

import com.hieupt.gomokubot.core.Move;

import java.util.List;
import java.util.Objects;

public record Threat(
        ThreatType type,
        Move attackMove,
        List<Move> defenseMoves
) {
    public Threat {
        Objects.requireNonNull(type, "ThreatType must not be null");
        Objects.requireNonNull(attackMove, "Attack move must not be null");
        defenseMoves = defenseMoves == null ? List.of() : List.copyOf(defenseMoves);
    }

    public boolean isWinning() {
        return type == ThreatType.FIVE;
    }

    public boolean isOpenFour() {
        return type == ThreatType.OPEN_FOUR;
    }

    public boolean isOpenThree() {
        return type == ThreatType.OPEN_THREE;
    }

    public boolean isFour() {
        return type == ThreatType.FOUR || type == ThreatType.OPEN_FOUR;
    }

    public boolean isThree() {
        return type == ThreatType.OPEN_THREE;
    }

    public boolean hasDefense() {
        return !defenseMoves.isEmpty();
    }
}

