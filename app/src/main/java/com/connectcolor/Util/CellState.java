package com.connectcolor.Util;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;


public enum CellState {
    
    Empty(Color.TRANSPARENT, false),
    White(Color.WHITE, true),
    Green(Color.GREEN, true),
    Blue(Color.BLUE, true),
    Red(Color.RED, true),
    Purple(Color.PURPLE, true),
    Orange(Color.ORANGE, true),
    Pink(Color.PINK, true),
    Yellow(Color.YELLOW, true),
    Cyan(Color.CYAN, true),
    Brown(Color.BROWN, true),
    LightGreen(Color.LIGHTGREEN, true),
    MidnightBlue(Color.MIDNIGHTBLUE, true);
    

    

    private final Color color;
    private final boolean isColor;

    CellState(Color color, boolean isColor) {
        this.color = color;
        this.isColor = isColor;
    }


    public Color getColor() {
        return this.color;
    }

    public boolean isColor() {
        return this.isColor;
    }

    public static List<CellState> getColorStates() {
        List<CellState> colors = new ArrayList<>();
        for (CellState state : CellState.values()) {
            if (state.isColor()) {
                colors.add(state);
            }
        }
        return colors;
    }



}
