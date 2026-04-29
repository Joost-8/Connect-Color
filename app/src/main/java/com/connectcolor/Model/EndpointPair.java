package com.connectcolor.Model;

public final class EndpointPair {
    private final int id;
    private final int startRow;
    private final int startCol;
    private final int endRow;
    private final int endCol;

    public EndpointPair(int id, int startRow, int startCol, int endRow, int endCol) {
        this.id = id;
        this.startRow = startRow;
        this.startCol = startCol;
        this.endRow = endRow;
        this.endCol = endCol;
    }

    public int getId() {
        return id;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    public int getEndRow() {
        return endRow;
    }

    public int getEndCol() {
        return endCol;
    }
}
