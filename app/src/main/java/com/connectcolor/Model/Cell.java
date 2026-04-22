package com.connectcolor.Model;

import com.connectcolor.Util.CellState;

public class Cell {
    private int row, col;
    private CellState solutionState;
    private CellState playerState;
    private boolean fixed;
    private Cell parent;

    public Cell(int row, int col, Boolean fixed) {
        this.row = row;
        this.col = col;
        this.solutionState = CellState.Empty;
        this.playerState = CellState.Empty;
        this.fixed = fixed;
        this.parent = null;


    }

    public void setPlayerState(CellState playerState) {
        if (!fixed) {
            this.playerState = playerState;
        }
    }

    public void setSolutionState(CellState solutionState) {
        this.solutionState = solutionState;
    }


    public CellState getPlayerState() {
        return playerState;
    }

    public boolean isFixed() {
        return fixed;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
    
    public CellState getSolutionState() {
        return this.solutionState;
    }

    public boolean isCorrect() {
        return playerState == solutionState;
    }

    public Cell getParent() {
        return this.parent;
    }

    public void setParent(Cell parent) {
        this.parent = parent;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public void forcePlayerState(CellState playerState) {
        this.playerState = playerState;
    }

    
}
