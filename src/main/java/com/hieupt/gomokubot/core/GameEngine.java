package com.hieupt.gomokubot.core;

import com.hieupt.gomokubot.rules.MoveValidator;
import com.hieupt.gomokubot.rules.WinChecker;

import java.util.Objects;

public class GameEngine {
    private final MoveValidator moveValidator;
    private final WinChecker winChecker;

    public GameEngine(int winLength) {
        this(new MoveValidator(), new WinChecker(winLength));
    }

    public GameEngine(
            MoveValidator moveValidator,
            WinChecker winChecker
    ) {
        this.moveValidator = Objects.requireNonNull(
                moveValidator,
                "MoveValidator must not be null"
        );

        this.winChecker = Objects.requireNonNull(
                winChecker,
                "WinChecker must not be null"
        );
    }

    public GameStatus playMove(GameState gameState, Move move) {
        Objects.requireNonNull(
                gameState,
                "GameState must not be null"
        );

        // 1. Validate the move before changing the board.
        moveValidator.validateMove(gameState, move);

        // 2. Capture the player before GameState switches turns.
        byte currentPlayer = gameState.getCurrentPlayer();

        // 3. Apply the move.
        gameState.makeMove(move);

        // 4. Check winning condition.
        if (winChecker.isWinningMove(gameState.getBoard(), move)) {
            gameState.declareWinner(currentPlayer);
            return gameState.getGameStatus();
        }

        // 5. Check draw condition.
        if (gameState.getBoard().isFull()) {
            gameState.declareDraw();
        }

        return gameState.getGameStatus();
    }

    public MoveValidator getMoveValidator() {
        return moveValidator;
    }

    public WinChecker getWinChecker() {
        return winChecker;
    }
}
