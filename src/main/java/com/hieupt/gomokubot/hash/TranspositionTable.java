package com.hieupt.gomokubot.hash;

import com.hieupt.gomokubot.core.Move;

import java.util.Objects;

public final class TranspositionTable {

    public enum Bound {
        EXACT,
        LOWER_BOUND,
        UPPER_BOUND
    }

    private static final int EMPTY_MOVE = -1;

    private final int width;
    private final int height;
    private final int capacity;
    private final int indexMask;

    private final long[] hashes;
    private final int[] depths;
    private final int[] scores;
    private final byte[] bounds;
    private final int[] bestMoves;
    private final boolean[] occupied;

    public static final class ProbeResult {

        private int depth;
        private int score;
        private Bound bound;
        private int bestMoveX;
        private int bestMoveY;
        private boolean hasBestMove;

        public ProbeResult() {
        }

        public int getDepth() {
            return depth;
        }

        public int getScore() {
            return score;
        }

        public Bound getBound() {
            return bound;
        }

        public boolean hasBestMove() {
            return hasBestMove;
        }

        public int getBestMoveX() {
            if (!hasBestMove) {
                throw new IllegalStateException("Entry has no best move");
            }
            return bestMoveX;
        }

        public int getBestMoveY() {
            if (!hasBestMove) {
                throw new IllegalStateException("Entry has no best move");
            }
            return bestMoveY;
        }

        private void set(
                int depth,
                int score,
                Bound bound,
                int bestMove,
                int width
        ) {
            this.depth = depth;
            this.score = score;
            this.bound = bound;

            this.hasBestMove = bestMove != EMPTY_MOVE;

            if (hasBestMove) {
                this.bestMoveX = bestMove % width;
                this.bestMoveY = bestMove / width;
            }
        }
    }

    public TranspositionTable(
            int width,
            int height,
            int requestedCapacity
    ) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Board dimensions must be positive"
            );
        }

        if (requestedCapacity <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be positive"
            );
        }

        this.width = width;
        this.height = height;
        this.capacity = roundUpToPowerOfTwo(requestedCapacity);
        this.indexMask = capacity - 1;

        this.hashes = new long[capacity];
        this.depths = new int[capacity];
        this.scores = new int[capacity];
        this.bounds = new byte[capacity];
        this.bestMoves = new int[capacity];
        this.occupied = new boolean[capacity];

        for (int i = 0; i < capacity; i++) {
            bestMoves[i] = EMPTY_MOVE;
        }
    }

    public boolean probe(long hash, ProbeResult result) {
        Objects.requireNonNull(result, "ProbeResult must not be null");

        int index = indexOf(hash);

        if (!occupied[index] || hashes[index] != hash) {
            return false;
        }

        result.set(
                depths[index],
                scores[index],
                decodeBound(bounds[index]),
                bestMoves[index],
                width
        );

        return true;
    }

    public void store(
            long hash,
            int depth,
            int score,
            Bound bound,
            Move bestMove
    ) {
        if (depth < 0) {
            throw new IllegalArgumentException(
                    "Depth must not be negative"
            );
        }

        Objects.requireNonNull(bound, "Bound must not be null");

        if (bestMove != null) {
            validateMove(bestMove);
        }

        int index = indexOf(hash);

        boolean samePosition =
                occupied[index] && hashes[index] == hash;

        if (occupied[index] && !samePosition
                && depth < depths[index]) {
            return;
        }

        if (samePosition && depth < depths[index]) {
            return;
        }

        hashes[index] = hash;
        depths[index] = depth;
        scores[index] = score;
        bounds[index] = encodeBound(bound);
        bestMoves[index] = encodeMove(bestMove);
        occupied[index] = true;
    }

    public void clear() {
        for (int i = 0; i < capacity; i++) {
            occupied[i] = false;
            bestMoves[i] = EMPTY_MOVE;
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private int indexOf(long hash) {
        long mixed = hash ^ (hash >>> 32);
        mixed *= 0x9E3779B97F4A7C15L;
        mixed ^= mixed >>> 32;

        return (int) mixed & indexMask;
    }

    private int encodeMove(Move move) {
        if (move == null) {
            return EMPTY_MOVE;
        }

        return move.y() * width + move.x();
    }

    private void validateMove(Move move) {
        if (move.x() < 0 || move.x() >= width
                || move.y() < 0 || move.y() >= height) {
            throw new IndexOutOfBoundsException(
                    "Move outside board: " + move
            );
        }
    }

    private static byte encodeBound(Bound bound) {
        return switch (bound) {
            case EXACT -> 0;
            case LOWER_BOUND -> 1;
            case UPPER_BOUND -> 2;
        };
    }

    private static Bound decodeBound(byte bound) {
        return switch (bound) {
            case 0 -> Bound.EXACT;
            case 1 -> Bound.LOWER_BOUND;
            case 2 -> Bound.UPPER_BOUND;
            default -> throw new IllegalStateException(
                    "Invalid stored bound: " + bound
            );
        };
    }

    private static int roundUpToPowerOfTwo(int value) {
        if (value > (1 << 30)) {
            throw new IllegalArgumentException(
                    "Capacity is too large"
            );
        }

        int result = 1;

        while (result < value) {
            result <<= 1;
        }

        return result;
    }
}
