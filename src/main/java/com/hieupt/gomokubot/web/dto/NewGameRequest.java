package com.hieupt.gomokubot.web.dto;

import com.hieupt.gomokubot.api.BotLevel;

public record NewGameRequest(
        Integer width,
        Integer height,
        Byte humanPlayer,
        Long moveTimeMs,
        String level
) {
    public NewGameRequest(Integer width, Integer height, Byte humanPlayer, Long moveTimeMs) {
        this(width, height, humanPlayer, moveTimeMs, "HARD");
    }

    public int resolvedWidth() {
        return width != null && width >= 5 && width <= 30 ? width : 15;
    }

    public int resolvedHeight() {
        return height != null && height >= 5 && height <= 30 ? height : 15;
    }

    public byte resolvedHumanPlayer() {
        return humanPlayer != null && (humanPlayer == 1 || humanPlayer == 2) ? humanPlayer : 2;
    }

    public BotLevel resolvedBotLevel() {
        return BotLevel.fromString(level);
    }

    public long resolvedMoveTimeMs() {
        if (moveTimeMs != null && moveTimeMs >= 50 && moveTimeMs <= 30000) {
            return moveTimeMs;
        }
        return resolvedBotLevel().getDefaultTimeMs();
    }
}
