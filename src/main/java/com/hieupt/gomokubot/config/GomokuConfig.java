package com.hieupt.gomokubot.config;

public class GomokuConfig {
    private final int width;
    private final int height;
    private final int winLength;

    public GomokuConfig(int width, int height, int winLength) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid board size");
        }

        if (winLength <= 0) {
            throw new IllegalArgumentException("Invalid win length");
        }

        this.width = width;
        this.height = height;
        this.winLength = winLength;
    }
}
