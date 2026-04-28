package com.connectcolor.View;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

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
        inner.setVisible(false);
    }

    public void clear() {
        inner.setVisible(false);
        inner.getElements().clear();
        endpointCircle.setVisible(false);
    }

    private boolean isOpposite(Dir a, Dir b) {
        return (a == Dir.UP && b == Dir.DOWN) ||
           (a == Dir.DOWN && b == Dir.UP) ||
           (a == Dir.LEFT && b == Dir.RIGHT) ||
           (a == Dir.RIGHT && b == Dir.LEFT);
    }

    private double[] pointForDir(Dir d, double S, double m) {
        double cx = S / 2.0;
        double cy = S / 2.0;

        switch (d) {
            case UP:
                return new double[]{cx, m};
            case DOWN:
                return new double[]{cx, S - m};
            case LEFT:
                return new double[]{m, cy};
            case RIGHT:
                return new double[]{S - m, cy};
            default:
                return new double[]{cx, cy};
        }
    }


    public void setPiece(Dir prev, Dir next) {
        inner.getElements().clear();

        double S = this.size;       // store size in CellView field
        double cx = S / 2.0, cy = S / 2.0;
        double m = S * 0.09;

        // End-cap (only one connection)
        if (prev != null && next == null) {
            double[] p = pointForDir(prev, S, m);
            inner.getElements().add(new javafx.scene.shape.MoveTo(cx, cy));
            inner.getElements().add(new javafx.scene.shape.LineTo(p[0], p[1]));
            animateDrawIn();
            return;
        }
        if (prev == null && next != null) {
            double[] p = pointForDir(next, S, m);
            inner.getElements().add(new javafx.scene.shape.MoveTo(cx, cy));
            inner.getElements().add(new javafx.scene.shape.LineTo(p[0], p[1]));
            animateDrawIn();
            return;
        }

        // Two connections: straight or corner
        if (prev != null && next != null) {
            double[] a = pointForDir(prev, S, m);
            double[] b = pointForDir(next, S, m);

            inner.getElements().add(new javafx.scene.shape.MoveTo(a[0], a[1]));
            inner.getElements().add(new javafx.scene.shape.LineTo(cx, cy));
            inner.getElements().add(new javafx.scene.shape.LineTo(b[0], b[1]));

            // Only animate corners if you want:
            if (!isOpposite(prev, next)) {
                animateDrawIn();   // corner animation
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
