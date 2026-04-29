package com.connectcolor.View;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import com.connectcolor.Model.BoardListener;
import com.connectcolor.Util.CellState;
import com.connectcolor.Util.Dir;
import com.connectcolor.Util.GameSettings;

import java.util.ArrayList;



public class BoardPanel extends GridPane {
    private final CellView[][] cells;
    private final int rows;
    private final int cols;
    private final int size;
    private ArrayList<BoardListener> listeners = new ArrayList<>();


    public BoardPanel(GameSettings settings) {
        this(settings.getRows(), settings.getCols(), settings.getCellSize());
    }

    public BoardPanel(int rows, int cols, int size) {
        this.rows = rows;
        this.cols = cols;
        this.size = size;
        cells = new CellView[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int r = row;
                int c = col;


                CellView cell = new CellView(size);
                
                StackPane cellStack = new StackPane();
                cellStack.setMinSize(size, size);
                cellStack.setPrefSize(size, size);
                cellStack.setMaxSize(size, size);
                cellStack.getChildren().addAll(cell.getOuter(), cell.getInner(), cell.getEndpointCircle());

                cellStack.setOnMousePressed(e -> {
                    notifyCellPressed(r,c);
                });

                // start full drag
                cellStack.setOnDragDetected(e -> cellStack.startFullDrag());

                // this fires when mouse enters while dragging
                cellStack.setOnMouseDragEntered(e -> notifyCellHovered(r, c));


                add(cellStack, col, row);
                cells[row][col] = cell;

            }
        }

        setOnMouseReleased(event -> {
            notifyMouseReleased();

        });


    }

    public void setColor(int row, int col, CellState state, boolean isFixed, Dir prev, Dir next) {
            CellView view = cells[row][col];

        if (state == CellState.Empty) {
            view.clear();
            return;
        }

        if (isFixed) {
            view.showEndpoint(state.getColor(), prev, next);
        } else {
            view.showPath(state.getColor());
            view.setPiece(prev, next);
        }
    }
    

    public void addListener(BoardListener listener) {
        listeners.add(listener);
    }

    public void notifyCellPressed(int row, int col) {
        for(BoardListener listener : listeners) {
            listener.onCellPressed(row, col);
        }
    }

    public void notifyCellHovered(int row, int col) {
        for(BoardListener listener : listeners) {
            listener.onCellHovered(row, col);
        }
    }

    public void notifyMouseReleased() {
        for(BoardListener listener : listeners) {
            listener.onMouseReleased();
        }
    }


}
