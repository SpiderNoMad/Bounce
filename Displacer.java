package com.binge;

import javafx.animation.PauseTransition;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import static com.binge.Main.GRAVITY;

public interface Displacer {
    abstract public boolean checkCollision(Character c);
    abstract void handleCollision(Character c);
}

class GrapplePoint extends CircleObstacle implements Displacer {
    double speed;
    boolean cooldown;
    int radius;

    GrapplePoint(Pane pane, double posX, double posY, int radius) {
        super(pane, posX, posY, radius, Color.GREEN.deriveColor(0, 1, 1, 0.5));
        this.cooldown = false;
        this.speed = 1000;
        this.radius = radius;
    }

    @Override
    public boolean checkCollision(Character c) {
        double diffX = this.pos.getX() - c.pos.getX();
        double diffY = this.pos.getY() - c.pos.getY();
        return (diffX*diffX + diffY*diffY < this.radius * this.radius);
    }

    @Override
    public void handleCollision(Character c) {
        double diffX = this.pos.getX() - c.pos.getX();
        double diffY = this.pos.getY() - c.pos.getY();
        Point2D diff = new Point2D(diffX, diffY);
        c.v = diff.normalize().scale(this.speed);

        this.cooldown = true;
        PauseTransition cooldown = new PauseTransition(Duration.seconds(1));
        cooldown.setOnFinished(e -> {
            this.cooldown = false;
        });
        cooldown.play();
    }
}
