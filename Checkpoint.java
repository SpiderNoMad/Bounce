package com.binge;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import static com.binge.Main.*;

public class Checkpoint extends RectangleObstacle {
    int width = 30, substageNum;
    boolean activate = false;

    Checkpoint(Pane pane, double posX, double posY) {
        super(pane, posX, posY, 30, 30, 0, Color.GRAY.deriveColor(0, 1, 1, 0.5), false);
    }

    @Override
    void handleCollision(Character c, Point2D normal, double penetration, double deltaTime) {
        if (this.activate) return;
        c.lastCheckpoint = this;
        this.activate = true;
        this.body.setFill(Color.GREEN.deriveColor(0, 1, 1, 0.3));
    }

    @Override
    public String toString() {
        return "Checkpoint: " + "{" + "x=" + pos.x + ", y=" + pos.y + ", activate=" + this.activate + '}';
    }
}

class Goal extends RectangleObstacle {
    Goal(Pane pane, double posX) {
        super(pane, posX, WINDOW_HEIGHT/2, 30, WINDOW_HEIGHT, 0, Color.BLUE.deriveColor(0, 1, 1, 0.5), false);
    }

    @Override
    void handleCollision(Character c, Point2D normal, double penetration, double deltaTime) {
        PageLoader.loadFinishPage();
    }
}