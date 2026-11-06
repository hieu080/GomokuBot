package com.hieupt.gomokubot.tactical;

public enum ForkType {
    DOUBLE_FOUR(100_000, "Bốn đôi (4-4)"),
    FOUR_THREE(50_000, "Bốn - Ba mở (4-3)"),
    DOUBLE_THREE(25_000, "Ba - Ba mở (3-3)");

    private final int bonusScore;
    private final String description;

    ForkType(int bonusScore, String description) {
        this.bonusScore = bonusScore;
        this.description = description;
    }

    public int getBonusScore() {
        return bonusScore;
    }

    public String getDescription() {
        return description;
    }
}

