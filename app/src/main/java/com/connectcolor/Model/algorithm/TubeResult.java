package com.connectcolor.Model.algorithm;

public class TubeResult {
    Grid tube; 
    UnionFind uf;

    public TubeResult(Grid tube, UnionFind uf) {
        this.tube = tube;
        this.uf = uf;

    }

    public Grid getTube() {
        return this.tube;
    }

    public UnionFind getUF() {
        return uf;
    }
}
