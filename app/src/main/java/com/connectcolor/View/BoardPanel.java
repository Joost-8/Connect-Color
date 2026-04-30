package com.connectcolor.View;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import com.connectcolor.Model.listeners.BoardListener;
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

        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        for (int col = 0; col < cols; col++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / cols);
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setFillWidth(true);
            getColumnConstraints().add(constraints);
        }

        for (int row = 0; row < rows; row++) {
            RowConstraints constraints = new RowConstraints();
            constraints.setPercentHeight(100.0 / rows);
            constraints.setVgrow(Priority.ALWAYS);
            constraints.setFillHeight(true);
            getRowConstraints().add(constraints);
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int r = row;
                int c = col;


                CellView cell = new CellView(size);
                
                StackPane cellStack = new StackPane();
                cellStack.setMinSize(0, 0);
                cellStack.setPrefSize(size, size);
                cellStack.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                cellStack.getChildren().addAll(cell.getOuter(), cell.getInner(), cell.getEndpointCircle());
                cellStack.widthProperty().addListener((obs, oldWidth, newWidth) -> cell.resize(cellStack.getWidth(), cellStack.getHeight()));
                cellStack.heightProperty().addListener((obs, oldHeight, newHeight) -> cell.resize(cellStack.getWidth(), cellStack.getHeight()));
                GridPane.setHgrow(cellStack, Priority.ALWAYS);
                GridPane.setVgrow(cellStack, Priority.ALWAYS);

                cellStack.setOnMousePressed(e -> {
                    notifyCellPressed(r,c);
                });

                // start full drag
                cellStack.setOnDragDetected(e -> cellStack.startFullDrag());

                // this fires when mouse enters while dragging
                cellStack.setOnMouseDragEntered(e -> notifyCellHovered(r, c));
                cellStack.setOnMouseEntered(e -> notifyCellHovered(r, c));


                add(cellStack, col, row);
                cells[row][col] = cell;

            }
        }

        setOnMouseReleased(event -> {
            notifyMouseReleased();

        });


    }

    public void setColor(int row, int col, CellState state, boolean isFixed, Dir prev, Dir next, CellState finishedPathColor) {
        CellView view = cells[row][col];

        if (state == CellState.Empty) {
            view.clear();
            return;
        }

        view.setFinishedBackground(finishedPathColor != CellState.Empty ? finishedPathColor.getColor() : null);

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
