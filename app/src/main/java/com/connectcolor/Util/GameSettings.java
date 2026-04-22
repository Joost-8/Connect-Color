package com.connectcolor.Util;

public class GameSettings {
    private int rows, cols, pairs;
    
    private GameSettings(int rows, int cols, int pairs) {
        this.rows = rows;
        this.cols = cols;
        this.pairs = pairs;
    }

    public static GameSettings easy() {
        return new GameSettings(6, 6, 5);
    }

    public static GameSettings medium() {
        return new GameSettings(8, 8, 7);
    }

    public static GameSettings hard() {
        return new GameSettings(10, 10, 9);
    }

    public static GameSettings expert() {
        return new GameSettings(12, 12, 12);
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

}
