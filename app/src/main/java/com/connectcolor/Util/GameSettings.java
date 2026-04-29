package com.connectcolor.Util;

public final class GameSettings {
    private final int rows;
    private final int cols;
    private final int pairs;
    private final int cellSize;
    
    public GameSettings(int rows, int cols, int pairs, int cellSize) {
        this.rows = rows;
        this.cols = cols;
        this.pairs = pairs;
        this.cellSize = cellSize;
    }

    public static GameSettings easy() {
        return Difficulty.EASY.createSettings();
    }

    public static GameSettings medium() {
        return Difficulty.MEDIUM.createSettings();
    }

    public static GameSettings hard() {
        return Difficulty.HARD.createSettings();
    }

    public static GameSettings expert() {
        return Difficulty.EXPERT.createSettings();
    }

    public static GameSettings forDifficulty(Difficulty difficulty) {
        return difficulty.createSettings();
    }

    public int getRows() {
        return this.rows;
    }

    public int getCols() {
        return this.cols;
    }

    public int getPairs() {
        return this.pairs;
    }

    public int getCellSize() {
        return this.cellSize;
    }

    public int getSceneWidth() {
        return this.cols * this.cellSize;
    }

    public int getSceneHeight() {
        return this.rows * this.cellSize;
    }

}
