package com.connectcolor.View;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.ArcTo;

import com.connectcolor.Util.Dir;

import javafx.scene.paint.Color;

public class CellView {

    private static final double PATH_WIDTH_RATIO = 0.28;
    private static final double CORNER_RADIUS_RATIO = 0.23;
    private static final Color EMPTY_BACKGROUND = Color.web("#1e1e1e");
    private static final double FINISHED_BACKGROUND_ALPHA = 0.16;
    private static final double FINISHED_BACKGROUND_WHITE_MIX = 0.68;

    private javafx.scene.shape.Path inner; 
    private Rectangle outer;
    private Circle endpointCircle;
    private double width;
    private double height;
    private Dir prev;
    private Dir next;
    

    public CellView(double size) {
        inner = new javafx.scene.shape.Path();
        inner.getStyleClass().add("cell-path");
        inner.setManaged(false);
        inner.setMouseTransparent(true);
        inner.setVisible(false);
        this.width = size;
        this.height = size;

        outer = new Rectangle(size, size, EMPTY_BACKGROUND);

        
        endpointCircle = new Circle(size * 0.3);
        endpointCircle.setVisible(false);
        
        outer.getStyleClass().add("cell-outer");
        endpointCircle.getStyleClass().add("cell-endpoint");
    }

    public javafx.scene.shape.Path getInner() {
        return this.inner;
    }

    public Rectangle getOuter() {
        return this.outer;
    }

    public Circle getEndpointCircle() {
        return this.endpointCircle;
    }

    public void showPath(Color color) {
        inner.setStroke(color);
        inner.setFill(Color.TRANSPARENT);
        inner.setVisible(true);
        endpointCircle.setVisible(false);
    }

    /** show fixed endpoint */
    public void showEndpoint(Color color) {
        prev = null;
        next = null;
        inner.getElements().clear();
        endpointCircle.setFill(color);
        endpointCircle.setVisible(true);
        inner.setStroke(color);
        inner.setFill(Color.TRANSPARENT);
        inner.setVisible(false);
    }

    /** show fixed endpoint with a path connector under the endpoint circle */
    public void showEndpoint(Color color, Dir prev, Dir next) {
        showEndpoint(color);
        setPiece(prev, next);
    }

    public void clear() {
        prev = null;
        next = null;
        setFinishedBackground(null);
        inner.setVisible(false);
        inner.getElements().clear();
        inner.getStrokeDashArray().clear();
        endpointCircle.setVisible(false);
    }

    public void setFinishedBackground(Color color) {
        if (color == null || color.equals(Color.TRANSPARENT)) {
            outer.setFill(EMPTY_BACKGROUND);
            return;
        }

        Color paleColor = color.interpolate(Color.WHITE, FINISHED_BACKGROUND_WHITE_MIX);
        outer.setFill(new Color(paleColor.getRed(), paleColor.getGreen(), paleColor.getBlue(), FINISHED_BACKGROUND_ALPHA));
    }

    public void resize(double width, double height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        this.width = width;
        this.height = height;

        double minSide = Math.min(width, height);
        outer.setWidth(width);
        outer.setHeight(height);
        outer.setArcWidth(minSide * 0.18);
        outer.setArcHeight(minSide * 0.18);
        endpointCircle.setRadius(minSide * 0.3);
        inner.setStrokeWidth(pathStrokeWidth());
        redrawPiece();
    }

    private boolean isOpposite(Dir a, Dir b) {
        return (a == Dir.UP && b == Dir.DOWN) ||
           (a == Dir.DOWN && b == Dir.UP) ||
           (a == Dir.LEFT && b == Dir.RIGHT) ||
           (a == Dir.RIGHT && b == Dir.LEFT);
    }

    private int dirIndex(Dir dir) {
        switch (dir) {
            case UP:
                return 0;
            case RIGHT:
                return 1;
            case DOWN:
                return 2;
            case LEFT:
                return 3;
            default:
                return -1;
        }
    }

    private boolean isClockwiseQuarter(Dir from, Dir to) {
        return (dirIndex(to) - dirIndex(from) + 4) % 4 == 1;
    }

    private double[] pointForDir(Dir d) {
        double cx = width / 2.0;
        double cy = height / 2.0;

        switch (d) {
            case UP:
                return new double[]{cx, 0};
            case DOWN:
                return new double[]{cx, height};
            case LEFT:
                return new double[]{0, cy};
            case RIGHT:
                return new double[]{width, cy};
            default:
                return new double[]{cx, cy};
        }
    }

    private double pathStrokeWidth() {
        return Math.max(6, Math.min(width, height) * PATH_WIDTH_RATIO);
    }

    private double cornerRadius() {
        return Math.min(width, height) * CORNER_RADIUS_RATIO;
    }

    private double[] vectorForDir(Dir dir) {
        switch (dir) {
            case UP:
                return new double[]{0, -1};
            case RIGHT:
                return new double[]{1, 0};
            case DOWN:
                return new double[]{0, 1};
            case LEFT:
                return new double[]{-1, 0};
            default:
                return new double[]{0, 0};
        }
    }

    private void addCorner(Dir from, Dir to) {
        double radius = cornerRadius();
        double cx = width / 2.0;
        double cy = height / 2.0;
        double[] fromEdge = pointForDir(from);
        double[] toEdge = pointForDir(to);
        double[] fromVector = vectorForDir(from);
        double[] toVector = vectorForDir(to);
        double[] fromTangent = new double[]{cx + fromVector[0] * radius, cy + fromVector[1] * radius};
        double[] toTangent = new double[]{cx + toVector[0] * radius, cy + toVector[1] * radius};

        inner.getElements().add(new MoveTo(fromEdge[0], fromEdge[1]));
        inner.getElements().add(new LineTo(fromTangent[0], fromTangent[1]));
        inner.getElements().add(new ArcTo(radius, radius, 0, toTangent[0], toTangent[1], false, !isClockwiseQuarter(from, to)));
        inner.getElements().add(new LineTo(toEdge[0], toEdge[1]));
    }


    public void setPiece(Dir prev, Dir next) {
        this.prev = prev;
        this.next = next;
        redrawPiece();
    }

    private void redrawPiece() {
        inner.getElements().clear();
        inner.getStrokeDashArray().clear();

        if (prev == null && next == null) {
            inner.setVisible(false);
            return;
        }

        inner.setVisible(true);

        double cx = width / 2.0;
        double cy = height / 2.0;

        // End-cap (only one connection)
        if (prev != null && next == null) {
            double[] p = pointForDir(prev);
            inner.getElements().add(new MoveTo(cx, cy));
            inner.getElements().add(new LineTo(p[0], p[1]));
            return;
        }
        if (prev == null && next != null) {
            double[] p = pointForDir(next);
            inner.getElements().add(new MoveTo(cx, cy));
            inner.getElements().add(new LineTo(p[0], p[1]));
            return;
        }

        // Two connections: straight or corner
        if (prev != null && next != null) {
            double[] a = pointForDir(prev);
            double[] b = pointForDir(next);

            inner.getElements().add(new MoveTo(a[0], a[1]));
            if (isOpposite(prev, next)) {
                inner.getElements().add(new LineTo(b[0], b[1]));
            } else {
                inner.getElements().clear();
                addCorner(prev, next);
            }
        }
    }

    private void animateDrawIn() {
        // dash trick to "draw" the stroke quickly
        inner.getStrokeDashArray().setAll(999.0, 999.0);
        inner.setStrokeDashOffset(999.0);

        javafx.animation.Timeline t = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
            new javafx.animation.KeyValue(inner.strokeDashOffsetProperty(), 999.0)
            ),
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(90),
            new javafx.animation.KeyValue(inner.strokeDashOffsetProperty(), 0.0)
            )
        );
        t.setOnFinished(e -> inner.getStrokeDashArray().clear());
        t.play();
    }



}
