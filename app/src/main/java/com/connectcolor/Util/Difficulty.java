package com.connectcolor.Util;

public enum Difficulty {
    EASY("Easy", "easy", 6, 6, 5, 90),
    MEDIUM("Medium", "medium", 8, 8, 7, 80),
    HARD("Hard", "hard", 10, 10, 9, 75),
    EXPERT("Expert", "expert", 12, 12, 12, 70);

    private final String displayName;
    private final String key;
    private final int rows;
    private final int cols;
    private final int pairs;
    private final int cellSize;

    Difficulty(String displayName, String key, int rows, int cols, int pairs, int cellSize) {
        this.displayName = displayName;
        this.key = key;
        this.rows = rows;
        this.cols = cols;
        this.pairs = pairs;
        this.cellSize = cellSize;
    }

    public String getKey() {
        return key;
    }

    public GameSettings createSettings() {
        return new GameSettings(rows, cols, pairs, cellSize);
    }

    public static Difficulty fromKey(String key) {
        if (key != null) {
            for (Difficulty difficulty : values()) {
                if (difficulty.key.equalsIgnoreCase(key)) {
                    return difficulty;
                }
            }
        }
        return MEDIUM;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
