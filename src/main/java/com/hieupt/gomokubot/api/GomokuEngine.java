package com.hieupt.gomokubot.api;

import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.GameEngine;
import com.hieupt.gomokubot.core.GameState;
import com.hieupt.gomokubot.core.GameStatus;
import com.hieupt.gomokubot.core.Move;
import com.hieupt.gomokubot.search.SearchResult;

import java.util.Objects;

public class GomokuEngine {

    public static final int DEFAULT_SIZE = 15;
    public static final int WIN_LENGTH = 5;

    private final GameEngine gameEngine;
    private final AiPlayer aiPlayer;
    private GameState gameState;
    private byte humanPlayer;
    private byte aiPlayerColor;
    private SearchResult lastSearchResult;

    public GomokuEngine() {
        this(DEFAULT_SIZE, DEFAULT_SIZE, Board.BLACK);
    }

    public GomokuEngine(int width, int height, byte humanPlayer) {
        this(width, height, humanPlayer, BotLevel.HARD);
    }

    public GomokuEngine(int width, int height, byte humanPlayer, BotLevel level) {
        this(
                new GameEngine(WIN_LENGTH),
                new AiPlayer(width, height, level),
                width,
                height,
                humanPlayer
        );
    }

    public GomokuEngine(
            GameEngine gameEngine,
            AiPlayer aiPlayer,
            int width,
            int height,
            byte humanPlayer
    ) {
        this.gameEngine = Objects.requireNonNull(gameEngine, "GameEngine must not be null");
        this.aiPlayer = Objects.requireNonNull(aiPlayer, "AiPlayer must not be null");
        startNewGame(width, height, humanPlayer);
    }

    public synchronized void startNewGame(int width, int height, byte humanPlayer) {
        if (humanPlayer != Board.BLACK && humanPlayer != Board.WHITE) {
            throw new IllegalArgumentException("Invalid human player: " + humanPlayer);
        }
        this.humanPlayer = humanPlayer;
        this.aiPlayerColor = humanPlayer == Board.BLACK ? Board.WHITE : Board.BLACK;
        this.gameState = new GameState(width, height, Board.BLACK);
        this.lastSearchResult = null;
        this.aiPlayer.getSearchEngine().clearTranspositionTable();

        // If human chose WHITE, AI plays first as BLACK
        if (this.humanPlayer == Board.WHITE) {
            playAiMove();
        }
    }

    public synchronized GameStatus playHumanMove(int x, int y) {
        if (gameState.isGameOver()) {
            throw new IllegalStateException("Game is already over");
        }
        if (gameState.getCurrentPlayer() != humanPlayer) {
            throw new IllegalStateException("Not human's turn");
        }

        Move move = new Move(x, y);
        GameStatus status = gameEngine.playMove(gameState, move);

        // If game is still in progress, AI makes the next move
        if (status == GameStatus.IN_PROGRESS) {
            playAiMove();
        }

        return gameState.getGameStatus();
    }

    public synchronized SearchResult playAiMove() {
        if (gameState.isGameOver()) {
            return null;
        }

        byte currentPlayer = gameState.getCurrentPlayer();
        SearchResult result = aiPlayer.findBestMove(gameState.getBoard(), currentPlayer);
        this.lastSearchResult = result;

        if (result.hasBestMove()) {
            gameEngine.playMove(gameState, result.bestMove());
        }

        return result;
    }

    public synchronized boolean undo() {
        if (gameState.getMoveCount() == 0) {
            return false;
        }

        // If it was human's turn, undo both the AI move and human move
        if (gameState.getCurrentPlayer() == humanPlayer) {
            if (gameState.getMoveCount() >= 2) {
                gameState.undoMove();
                gameState.undoMove();
                return true;
            } else if (gameState.getMoveCount() == 1) {
                gameState.undoMove();
                return true;
            }
        } else {
            gameState.undoMove();
            return true;
        }
        return false;
    }

    public synchronized SearchResult switchSide() {
        if (gameState.isGameOver()) {
            return null;
        }

        byte temp = this.humanPlayer;
        this.humanPlayer = this.aiPlayerColor;
        this.aiPlayerColor = temp;

        // Nếu sau khi đổi bên, lượt hiện tại thuộc về AI, AI sẽ tự động đi
        if (gameState.getCurrentPlayer() == this.aiPlayerColor) {
            return playAiMove();
        }

        return null;
    }

    public GameState getGameState() {
        return gameState;
    }

    public byte getHumanPlayer() {
        return humanPlayer;
    }

    public byte getAiPlayerColor() {
        return aiPlayerColor;
    }

    public SearchResult getLastSearchResult() {
        return lastSearchResult;
    }

    public AiPlayer getAiPlayer() {
        return aiPlayer;
    }

    public boolean isHumanTurn() {
        return !gameState.isGameOver() && gameState.getCurrentPlayer() == humanPlayer;
    }

    public boolean isAiTurn() {
        return !gameState.isGameOver() && gameState.getCurrentPlayer() == aiPlayerColor;
    }
}
