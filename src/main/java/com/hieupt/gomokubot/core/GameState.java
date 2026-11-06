package com.hieupt.gomokubot.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameState {
    private final Board board;

    private byte currentPlayer;

    private GameStatus gameStatus;

    private byte winner;

    private final List<MoveRecord> moveHistory;

    private record MoveRecord(Move move, byte player) {
    }

    public GameState(int width, int height, byte startingPlayer) {
        this(new Board(width, height), startingPlayer);
    }

    public GameState(Board board, byte startingPlayer) {
        this.board = Objects.requireNonNull(board, "Board must not be null");
        validatePlayer(startingPlayer);

        this.currentPlayer = startingPlayer;
        this.gameStatus = GameStatus.IN_PROGRESS;
        this.winner = Board.EMPTY;
        this.moveHistory = new ArrayList<>();
    }

    public Board getBoard() {
        return board;
    }

    public byte getCurrentPlayer() {
        return currentPlayer;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public byte getWinner() {
        return winner;
    }

    public int getMoveCount() {
        return moveHistory.size();
    }

    public Move getLastMove() {
        if (moveHistory.isEmpty()) {
            return null;
        }

        return moveHistory.get(moveHistory.size() - 1).move();
    }

    public List<Move> getMoveHistory() {
        return moveHistory.stream()
                .map(MoveRecord::move)
                .toList();
    }

    public boolean isGameOver() {
        return gameStatus != GameStatus.IN_PROGRESS;
    }

    public void makeMove(Move move) {
        Objects.requireNonNull(move, "Move must not be null");

        if (isGameOver()) {
            throw new IllegalStateException("Game has already ended");
        }

        board.placeStone(
                move.x(),
                move.y(),
                currentPlayer
        );

        moveHistory.add(new MoveRecord(move, currentPlayer));

        switchPlayer();
    }

    public Move undoMove() {
        if (moveHistory.isEmpty()) {
            throw new IllegalStateException("No move to undo");
        }

        MoveRecord lastRecord =
                moveHistory.remove(moveHistory.size() - 1);

        Move lastMove = lastRecord.move();

        board.removeStone(lastMove.x(), lastMove.y());

        currentPlayer = lastRecord.player();

        gameStatus = GameStatus.IN_PROGRESS;
        winner = Board.EMPTY;

        return lastMove;
    }

    public void declareWinner(byte winningPlayer) {
        validatePlayer(winningPlayer);

        if (isGameOver()) {
            throw new IllegalStateException("Game has already ended");
        }

        if (moveHistory.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot declare a winner without any moves"
            );
        }

        byte lastPlayer = moveHistory.get(
                moveHistory.size() - 1
        ).player();

        if (lastPlayer != winningPlayer) {
            throw new IllegalStateException(
                    "Winner must be the player who made the last move"
            );
        }

        this.winner = winningPlayer;
        this.gameStatus = GameStatus.WON;
    }

    public void declareDraw() {
        if (isGameOver()) {
            throw new IllegalStateException("Game has already ended");
        }

        if (!board.isFull()) {
            throw new IllegalStateException(
                    "Cannot declare a draw while empty cells remain"
            );
        }

        this.winner = Board.EMPTY;
        this.gameStatus = GameStatus.DRAW;
    }

    public GameState copy() {
        GameState copy = new GameState(
                board.copy(),
                currentPlayer
        );

        copy.gameStatus = this.gameStatus;
        copy.winner = this.winner;
        copy.moveHistory.addAll(this.moveHistory);

        return copy;
    }

    private void switchPlayer() {
        currentPlayer = (currentPlayer == Board.BLACK)
                ? Board.WHITE
                : Board.BLACK;
    }

    private void validatePlayer(byte player) {
        if (player != Board.BLACK && player != Board.WHITE) {
            throw new IllegalArgumentException(
                    "Invalid player value: " + player
            );
        }
    }
}
