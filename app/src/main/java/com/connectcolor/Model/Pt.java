package com.connectcolor.Model;

public final class Pt {
    public final int x;
    public final int y;

    public Pt(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pt)) return false;
        Pt pt = (Pt) o;
        return x == pt.x && y == pt.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "Pt(" + x + "," + y + ")";
    }
}

