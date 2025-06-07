package com.binge;

import javafx.scene.layout.Pane;

import java.util.ArrayList;

public class Sublevel {
    int num;
    ArrayList<Obstacle> obstacles;
    ArrayList<Collectible> items;
    ArrayList<Displacer> displacers;
    ArrayList<Lock> locks;
    Checkpoint checkpoint;
    Goal goal;
    Pane pane;

    Sublevel(int n) {
        this.num = n;
        this.pane = new Pane();
        this.obstacles = new ArrayList<>();
        this.items = new ArrayList<>();
        this.displacers = new ArrayList<>();
        this.locks = new ArrayList<>();
    }
}
