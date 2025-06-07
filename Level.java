package com.binge;

import java.util.*;

import static com.binge.Main.character;
import static com.binge.Main.pane;

public class Level {
    int levelLength, index;
    ArrayList<Sublevel> sublevels;
    ArrayList<Checkpoint> checkpoints;

    Level(int n) {
        this.levelLength = 0;
        this.index = n;
        this.sublevels = new ArrayList<>();
        this.checkpoints = new ArrayList<>();
    }
}