package com.hieupt.gomokubot.api;

public enum BotLevel {
    EASY("Dễ (Tập chơi)", 2, 300L, false, false, false),
    MEDIUM("Trung bình", 4, 800L, true, false, true),
    HARD("Khó (Thi đấu)", 8, 2000L, true, true, true),
    MASTER("Đại kiện tướng", 12, 4000L, true, true, true);

    private final String displayName;
    private final int maxDepth;
    private final long defaultTimeMs;
    private final boolean vcfEnabled;
    private final boolean vctEnabled;
    private final boolean useOpeningBook;

    BotLevel(
            String displayName,
            int maxDepth,
            long defaultTimeMs,
            boolean vcfEnabled,
            boolean vctEnabled,
            boolean useOpeningBook
    ) {
        this.displayName = displayName;
        this.maxDepth = maxDepth;
        this.defaultTimeMs = defaultTimeMs;
        this.vcfEnabled = vcfEnabled;
        this.vctEnabled = vctEnabled;
        this.useOpeningBook = useOpeningBook;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public long getDefaultTimeMs() {
        return defaultTimeMs;
    }

    public boolean isVcfEnabled() {
        return vcfEnabled;
    }

    public boolean isVctEnabled() {
        return vctEnabled;
    }

    public boolean isUseOpeningBook() {
        return useOpeningBook;
    }

    public static BotLevel fromString(String name) {
        if (name == null || name.isBlank()) {
            return HARD;
        }
        try {
            return BotLevel.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return HARD;
        }
    }
}

