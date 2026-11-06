package com.hieupt.gomokubot.hash;

import com.hieupt.gomokubot.core.Board;

import java.util.Objects;
import java.util.SplittableRandom;

public class ZobristHash {
    private static final long DEFAULT_SEED = 20261105L;

    private final int width;
    private final int height;

    private final long[][][] pieceKeys;
    private final long sideToMoveKey;

    public ZobristHash(int width, int height) {
        this(width, height, DEFAULT_SEED);
    }

    public ZobristHash(int width, int height, long seed) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Board dimensions must be positive"
            );
        }

        this.width = width;
        this.height = height;

        this.pieceKeys = new long[height][width][3];

        SplittableRandom random = new SplittableRandom(seed);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pieceKeys[y][x][Board.BLACK] = random.nextLong();
                pieceKeys[y][x][Board.WHITE] = random.nextLong();
            }
        }

        this.sideToMoveKey = random.nextLong();
    }

    public long computeHash(Board board, byte currentPlayer) {
        Objects.requireNonNull(board, "Board must not be null");
        validatePlayer(currentPlayer);
        validateDimensions(board);

        long hash = 0L;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                byte piece = board.getCell(x, y);

                if (piece == Board.BLACK || piece == Board.WHITE) {
                    hash ^= pieceKeys[y][x][piece];
                }
            }
        }

        if (currentPlayer == Board.WHITE) {
            hash ^= sideToMoveKey;
        }

        return hash;
    }

    public long updateAfterMove(
            long currentHash,
            int x,
            int y,
            byte player
    ) {
        validatePosition(x, y);
        validatePlayer(player);

        long hash = currentHash ^ pieceKeys[y][x][player];

        // The turn switches after every move.
        return hash ^ sideToMoveKey;
    }

    public long updateAfterUndo(
            long currentHash,
            int x,
            int y,
            byte player
    ) {
        // XOR is reversible, so undo uses the same operation.
        return updateAfterMove(currentHash, x, y, player);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void validatePosition(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException(
                    "Position outside board: (" + x + ", " + y + ")"
            );
        }
    }

    private void validateDimensions(Board board) {
        if (board.getWidth() != width || board.getHeight() != height) {
            throw new IllegalArgumentException(
                    "Board dimensions do not match Zobrist configuration"
            );
        }
    }

    private void validatePlayer(byte player) {
        if (player != Board.BLACK && player != Board.WHITE) {
            throw new IllegalArgumentException(
                    "Invalid player: " + player
            );
        }
    }
}
