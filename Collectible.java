package com.binge;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public abstract class Collectible {
    Point2D pos;
    int radius;
    CircleObstacle hitbox;

    Collectible(double posX, double posY, int radius) {
        this.pos = new Point2D(posX, posY);
        this.radius = radius;
    }

    public boolean checkCollision(Character c) {
        double diffX = this.pos.getX() - c.pos.getX();
        double diffY = this.pos.getY() - c.pos.getY();
        return (diffX*diffX + diffY*diffY < (this.radius + c.radius) * (this.radius + c.radius));
    }

    abstract void handleCollision(Character c);
}

class Coin extends Collectible {
    int value;

    Coin(Pane pane, double posX, double posY, int radius, int value) {
        super(posX, posY, radius);
        this.value = value;
        hitbox = new CircleObstacle(pane, posX, posY, radius, Color.YELLOW);
    }

    @Override
    public void handleCollision(Character c) {
        c.coins += this.value;
        hitbox.color = Color.TRANSPARENT;
        hitbox.body.setFill(hitbox.color);
        hitbox.body.setStroke(Color.TRANSPARENT);
    }
}

class SizeShifter extends Collectible {
    int duration, increment;

    SizeShifter(Pane pane, double posX, double posY, int radius, int increment) {
        super(posX, posY, radius);
        this.duration = 15;
        this.increment = increment;
        if (this.increment < 0) hitbox = new CircleObstacle(pane, posX, posY, radius, Color.RED);
        else hitbox = new CircleObstacle(pane, posX, posY, radius, Color.BLUE);
    }

    @Override
    public void handleCollision(Character c) {
        hitbox.color = Color.TRANSPARENT;
        hitbox.body.setFill(hitbox.color);
        hitbox.body.setStroke(Color.TRANSPARENT);

        if (c.radius + increment <= 0) return;
        c.radius += this.increment;
        c.body.setRadius(c.radius);
    }
}