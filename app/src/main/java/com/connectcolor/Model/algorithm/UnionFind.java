package com.connectcolor.Model.algorithm;

public class UnionFind {
    int[] parent;
    int[] rank;
        
    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];

        for (int i = 0; i < n; i++) {
            parent[i] = i;
            rank[i] = 0;
        }

    }   
    
    
    public int find(int a) {
        if (parent[a] == a) {
            return a;
        } else {
            parent[a] = find(parent[a]);
            return parent[a];
        }
    }
    
    public void union(int a, int b) {
        int ra = find(a);
        int rb = find(b);

        if (ra == rb) {
            return;
        }

        if (rank[ra] < rank[rb]) {
            parent[ra] = rb;
        } else if (rank[rb] < rank[ra]) {
            parent[rb] = ra;
        } else {
            parent[ra] = rb;
            rank[rb]++;
        }
    }
    
    




}
