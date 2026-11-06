package com.hieupt.gomokubot.time;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.search.SearchLimits;

import java.util.Objects;

public class TimeManager {

    public static final long DEFAULT_MOVE_TIME_MS = 2000L;
    public static final long MIN_MOVE_TIME_MS = 100L;
    public static final long MAX_MOVE_TIME_MS = 15000L;

    private final long fixedMoveTimeMs;
    private final boolean dynamicMode;
    private long totalRemainingTimeMs;
    private final long incrementMs;

    public TimeManager() {
        this(DEFAULT_MOVE_TIME_MS);
    }

    public TimeManager(long fixedMoveTimeMs) {
        if (fixedMoveTimeMs <= 0) {
            throw new IllegalArgumentException("Move time must be positive");
        }
        this.fixedMoveTimeMs = fixedMoveTimeMs;
        this.dynamicMode = false;
        this.totalRemainingTimeMs = 0;
        this.incrementMs = 0;
    }

    public TimeManager(long totalTimeMs, long incrementMs) {
        if (totalTimeMs <= 0) {
            throw new IllegalArgumentException("Total time must be positive");
        }
        if (incrementMs < 0) {
            throw new IllegalArgumentException("Increment must not be negative");
        }
        this.fixedMoveTimeMs = DEFAULT_MOVE_TIME_MS;
        this.dynamicMode = true;
        this.totalRemainingTimeMs = totalTimeMs;
        this.incrementMs = incrementMs;
    }

    public long allocateTime(Board board) {
        Objects.requireNonNull(board, "Board must not be null");

        if (!dynamicMode) {
            int occupied = board.getOccupiedCount();
            if (occupied == 0) {
                return 50L;
            } else if (occupied <= 2) {
                return Math.min(500L, fixedMoveTimeMs);
            }
            return fixedMoveTimeMs;
        }

        int occupied = board.getOccupiedCount();
        int expectedRemainingMoves = Math.max(10, (board.getWidth() * board.getHeight() - occupied) / 2);

        int movesWeight = Math.min(expectedRemainingMoves, 30);
        long allocated = (totalRemainingTimeMs / movesWeight) + incrementMs;

        if (occupied >= 8 && occupied <= 25) {
            allocated = (long) (allocated * 1.3);
        }

        return Math.max(MIN_MOVE_TIME_MS, Math.min(allocated, MAX_MOVE_TIME_MS));
    }

    public SearchLimits createSearchLimits(Board board, int maxDepth) {
        long timeMs = allocateTime(board);
        return SearchLimits.depthAndTime(maxDepth, timeMs);
    }

    public SearchLimits createSearchLimits(Board board) {
        return createSearchLimits(board, 12);
    }

    public void updateRemainingTime(long timeSpentMs) {
        if (dynamicMode) {
            totalRemainingTimeMs = Math.max(0, totalRemainingTimeMs - timeSpentMs + incrementMs);
        }
    }

    public long getFixedMoveTimeMs() {
        return fixedMoveTimeMs;
    }

    public long getTotalRemainingTimeMs() {
        return totalRemainingTimeMs;
    }

    public long getIncrementMs() {
        return incrementMs;
    }

    public boolean isDynamicMode() {
        return dynamicMode;
    }
}
