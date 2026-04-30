package com.connectcolor.Model.listeners;

import com.connectcolor.Util.CellState;

public interface BoardListener {

    public void onCellPressed(int row, int col);

    public void onCellHovered(int row, int col);

    public void onMoveRequested(int rowDelta, int colDelta);

    public void onMouseReleased();

    public void onCellUpdate(int row, int col, CellState color);

    
    
}
