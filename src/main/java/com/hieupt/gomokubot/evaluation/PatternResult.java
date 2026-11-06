package com.hieupt.gomokubot.evaluation;

import java.util.Objects;

public record PatternResult(
        Pattern pattern,
        Direction direction,
        int startX,
        int startY,
        int endX,
        int endY,
        int stoneCount
) {

    public PatternResult {
        Objects.requireNonNull(pattern, "Pattern must not be null");
        Objects.requireNonNull(direction, "Direction must not be null");

        if (stoneCount <= 0) {
            throw new IllegalArgumentException(
                    "Stone count must be positive"
            );
        }
    }

    public enum Direction {

        HORIZONTAL(1, 0),

        VERTICAL(0, 1),

        DIAGONAL_DOWN(1, 1),

        DIAGONAL_UP(1, -1);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public int getDx() {
            return dx;
        }

        public int getDy() {
            return dy;
        }
    }
}
