package com.hieupt.gomokubot.rules;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.GameState;
import com.hieupt.gomokubot.core.GameStatus;
import com.hieupt.gomokubot.core.Move;

import java.util.Objects;

public class MoveValidator {
    public boolean isValidMove(GameState gameState, Move move) {
        Objects.requireNonNull(gameState, "GameState must not be null");

        if (move == null) {
            return false;
        }

        if (gameState.getGameStatus() != GameStatus.IN_PROGRESS) {
            return false;
        }

        Board board = gameState.getBoard();

        int x = move.x();
        int y = move.y();

        if (!board.isInside(x, y)) {
            return false;
        }

        return board.isEmpty(x, y);
    }

    public void validateMove(GameState gameState, Move move) {
        Objects.requireNonNull(gameState, "GameState must not be null");

        if (gameState.getGameStatus() != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Cannot make a move because the game is over"
            );
        }

        if (move == null) {
            throw new IllegalArgumentException(
                    "Move must not be null"
            );
        }

        Board board = gameState.getBoard();

        int x = move.x();
        int y = move.y();

        if (!board.isInside(x, y)) {
            throw new IllegalArgumentException(
                    "Move is outside the board: (" + x + ", " + y + ")"
            );
        }

        if (!board.isEmpty(x, y)) {
            throw new IllegalArgumentException(
                    "Cell is already occupied: (" + x + ", " + y + ")"
            );
        }
    }

    public boolean isValidPosition(Board board, Move move) {
        Objects.requireNonNull(board, "Board must not be null");

        if (move == null) {
            return false;
        }

        int x = move.x();
        int y = move.y();

        return board.isInside(x, y) && board.isEmpty(x, y);
    }

}
