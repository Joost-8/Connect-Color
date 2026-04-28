package com.connectcolor.Util;

public final class GameSettings {
    private final int rows;
    private final int cols;
    private final int pairs;
    private final int cellSize;
    
    private GameSettings(int rows, int cols, int pairs, int cellSize) {
        this.rows = rows;
        this.cols = cols;
        this.pairs = pairs;
        this.cellSize = cellSize;
    }

    public static GameSettings easy() {
        return new GameSettings(6, 6, 5, 90);
    }

    public static GameSettings medium() {
        return new GameSettings(8, 8, 7, 80);
    }

    public static GameSettings hard() {
        return new GameSettings(10, 10, 9, 75);
    }

    public static GameSettings expert() {
        return new GameSettings(12, 12, 12, 70);
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
