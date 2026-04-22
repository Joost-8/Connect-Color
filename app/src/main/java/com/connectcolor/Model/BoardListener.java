package com.connectcolor.Model;

import com.connectcolor.Util.CellState;

public interface BoardListener {

    public void onCellPressed(int row, int col);

    public void onCellHovered(int row, int col);

    public void onMouseReleased();

    public void onCellUpdate(int row, int col, CellState color);

    
    
}
