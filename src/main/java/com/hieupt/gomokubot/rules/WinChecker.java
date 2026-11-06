package com.hieupt.gomokubot.rules;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.Move;

import java.util.Objects;

public class WinChecker {
    private static final int[][] DIRECTIONS = {
            {1, 0},   // Horizontal
            {0, 1},   // Vertical
            {1, 1},   // Diagonal: \
            {1, -1}   // Diagonal: /
    };

    private final int winLength;

    public WinChecker(int winLength) {
        if (winLength <= 0) {
            throw new IllegalArgumentException(
                    "Win length must be positive"
            );
        }

        this.winLength = winLength;
    }

    public boolean isWinningMove(Board board, Move move) {
        Objects.requireNonNull(board, "Board must not be null");
        Objects.requireNonNull(move, "Move must not be null");

        int x = move.x();
        int y = move.y();

        if (!board.isInside(x, y)) {
            throw new IndexOutOfBoundsException(
                    "Move is outside the board: (" + x + ", " + y + ")"
            );
        }

        byte player = board.getCell(x, y);

        if (player != Board.BLACK && player != Board.WHITE) {
            throw new IllegalStateException(
                    "No stone exists at the specified move"
            );
        }

        for (int[] direction : DIRECTIONS) {
            int count = 1;

            count += countConsecutive(
                    board,
                    x,
                    y,
                    direction[0],
                    direction[1],
                    player
            );

            count += countConsecutive(
                    board,
                    x,
                    y,
                    -direction[0],
                    -direction[1],
                    player
            );

            if (count >= winLength) {
                return true;
            }
        }

        return false;
    }

    private int countConsecutive(
            Board board,
            int x,
            int y,
            int dx,
            int dy,
            byte player
    ) {
        int count = 0;

        int currentX = x + dx;
        int currentY = y + dy;

        while (board.isInside(currentX, currentY)
                && board.getCell(currentX, currentY) == player) {

            count++;

            currentX += dx;
            currentY += dy;
        }

        return count;
    }

    public int getWinLength() {
        return winLength;
    }
}
