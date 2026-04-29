package com.connectcolor.View;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.ArcTo;

import com.connectcolor.Util.Dir;

import javafx.scene.paint.Color;

public class CellView {

    private javafx.scene.shape.Path inner; 
    private Rectangle outer;
    private Circle endpointCircle;
    private double size;
    

    public CellView(double size) {
        inner = new javafx.scene.shape.Path();
        inner.getStyleClass().add("cell-path");
        inner.setManaged(false);
        inner.setMouseTransparent(true);
        inner.setVisible(false);
        this.size = size;

        outer = new Rectangle(size, size, Color.BLACK);

        
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
        inner.setVisible(false);
        inner.getElements().clear();
        inner.getStrokeDashArray().clear();
        endpointCircle.setVisible(false);
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

    private double[] pointForDir(Dir d, double S) {
        double cx = S / 2.0;
        double cy = S / 2.0;

        switch (d) {
            case UP:
                return new double[]{cx, 0};
            case DOWN:
                return new double[]{cx, S};
            case LEFT:
                return new double[]{0, cy};
            case RIGHT:
                return new double[]{S, cy};
            default:
                return new double[]{cx, cy};
        }
    }


    public void setPiece(Dir prev, Dir next) {
        inner.getElements().clear();
        inner.getStrokeDashArray().clear();

        if (prev == null && next == null) {
            inner.setVisible(false);
            return;
        }

        inner.setVisible(true);

        double S = this.size;
        double cx = S / 2.0, cy = S / 2.0;

        // End-cap (only one connection)
        if (prev != null && next == null) {
            double[] p = pointForDir(prev, S);
            inner.getElements().add(new MoveTo(cx, cy));
            inner.getElements().add(new LineTo(p[0], p[1]));
            return;
        }
        if (prev == null && next != null) {
            double[] p = pointForDir(next, S);
            inner.getElements().add(new MoveTo(cx, cy));
            inner.getElements().add(new LineTo(p[0], p[1]));
            return;
        }

        // Two connections: straight or corner
        if (prev != null && next != null) {
            double[] a = pointForDir(prev, S);
            double[] b = pointForDir(next, S);

            inner.getElements().add(new MoveTo(a[0], a[1]));
            if (isOpposite(prev, next)) {
                inner.getElements().add(new LineTo(b[0], b[1]));
            } else {
                inner.getElements().add(new ArcTo(S / 2.0, S / 2.0, 0, b[0], b[1], false, !isClockwiseQuarter(prev, next)));
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
