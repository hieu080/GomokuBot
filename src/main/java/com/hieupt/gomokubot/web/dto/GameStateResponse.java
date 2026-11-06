package com.hieupt.gomokubot.web.dto;

import com.hieupt.gomokubot.api.BotLevel;
import com.hieupt.gomokubot.api.GomokuEngine;
import com.hieupt.gomokubot.core.Board;
import com.hieupt.gomokubot.core.GameState;
import com.hieupt.gomokubot.core.GameStatus;
import com.hieupt.gomokubot.core.Move;

public record GameStateResponse(
        int width,
        int height,
        int[][] board,
        byte currentPlayer,
        byte humanPlayer,
        byte aiPlayer,
        String status,
        byte winner,
        Integer lastMoveX,
        Integer lastMoveY,
        int moveCount,
        boolean isHumanTurn,
        boolean isGameOver,
        AiSearchStatsDto lastAiSearch,
        String botLevel,
        String botLevelDisplayName
) {
    public static GameStateResponse from(GomokuEngine engine) {
        GameState state = engine.getGameState();
        Board b = state.getBoard();
        int w = b.getWidth();
        int h = b.getHeight();

        int[][] boardArray = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boardArray[y][x] = b.getCell(x, y);
            }
        }

        Move lastMove = state.getLastMove();
        Integer lastX = lastMove != null ? lastMove.x() : null;
        Integer lastY = lastMove != null ? lastMove.y() : null;

        String statusStr;
        if (state.getGameStatus() == GameStatus.WON) {
            statusStr = state.getWinner() == Board.BLACK ? "BLACK_WIN" : "WHITE_WIN";
        } else if (state.getGameStatus() == GameStatus.DRAW) {
            statusStr = "DRAW";
        } else {
            statusStr = "IN_PROGRESS";
        }

        BotLevel level = engine.getAiPlayer() != null ? engine.getAiPlayer().getBotLevel() : BotLevel.HARD;

        return new GameStateResponse(
                w,
                h,
                boardArray,
                state.getCurrentPlayer(),
                engine.getHumanPlayer(),
                engine.getAiPlayerColor(),
                statusStr,
                state.getWinner(),
                lastX,
                lastY,
                state.getMoveCount(),
                engine.isHumanTurn(),
                state.isGameOver(),
                AiSearchStatsDto.from(engine.getLastSearchResult()),
                level.name(),
                level.getDisplayName()
        );
    }
}
