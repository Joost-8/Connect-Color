package com.connectcolor.Controllers;

import java.io.IOException;

import com.connectcolor.Model.BoardListener;
import com.connectcolor.Util.CellState;
import com.connectcolor.Util.Dir;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.paint.Color;
import com.connectcolor.View.BoardPanel;
import com.connectcolor.Model.Board;
import java.util.ArrayList;
import com.connectcolor.Model.Cell;

public class PrimaryController implements BoardListener{

    private CellState activeColor = null;
    private Board board;
    private BoardPanel boardPanel;
    private Runnable onPuzzleSolved;
    private boolean dragging = false;
    private int headRow = -1, headCol = -1;    
    private int lastRow = -1, lastCol = -1;  

    public PrimaryController(Board board, BoardPanel boardPanel) {
        this(board, boardPanel, null);
    }

    public PrimaryController(Board board, BoardPanel boardPanel, Runnable onPuzzleSolved) {
        this.board = board;
        this.boardPanel = boardPanel;
        this.onPuzzleSolved = onPuzzleSolved;
        
    }

    @FXML
    private void switchToSecondary() throws IOException {
        //App.setRoot("secondary");
    }


    @Override
    public void onCellPressed(int row, int col) {
        Cell pressed = board.getCell(row, col);
        
        // CASE 1: click fixed endpoint -> start/reset from that endpoint
        if (pressed.isFixed()) {
            board.startPathFromFixed(row, col);

            activeColor = pressed.getSolutionState();
            dragging = true;

            headRow = row; headCol = col;
            lastRow = row; lastCol = col;
            return;
        }

        // CASE 2: click non-fixed path cell -> trim there and continue from it
        CellState colorHere = pressed.getPlayerState();
        if (colorHere != CellState.Empty && board.resumePathFromCell(row, col, colorHere)) {
            activeColor = colorHere;
            dragging = true;

            headRow = row; headCol = col;
            lastRow = row; lastCol = col;
            return;
        }

        // Otherwise: cancel
        activeColor = null;
        dragging = false;
        headRow = headCol = lastRow = lastCol = -1;
    }

    @Override
    public void onCellHovered(int row, int col) {
        if (!dragging || activeColor == null) return;

        // must move 1 step from current head
        if (!board.areOrthogonalNeighbors(headRow, headCol, row, col)) return;

        boolean changed = board.tryDragStep(activeColor, headRow, headCol, row, col);
        if (!changed) {
            board.redrawPathTail(activeColor);
            return;
        }

        if (board.isSolved()) {
            onMouseReleased();
            if (onPuzzleSolved != null) {
                onPuzzleSolved.run();
            }
            return;
        }

        if (!board.isPathUnfinished(activeColor)) {
            onMouseReleased();
            return;
        }

        // refresh head after the board updated the path (extend or undo)
        int[] head = board.getHead(activeColor);
        if (head != null) {
            headRow = head[0];
            headCol = head[1];
        }
    }

    @Override
    public void onMouseReleased() {
        activeColor = null;
        dragging = false;
        headRow = headCol = lastRow = lastCol = -1;
    }

    @Override
    public void onCellUpdate(int row, int col, CellState cellState) {
        Cell cell = board.getCell(row, col);
        
        Dir[] dirs = board.getPrevNextDirs(row, col); // returns {prevDir, nextDir}

        boardPanel.setColor(row, col, cellState, cell.isFixed(), dirs[0], dirs[1]);
 

    }



}
