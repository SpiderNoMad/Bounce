package com.binge;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import static com.binge.Main.*;
import static com.binge.Main.canvas;

public class Character {
    Point2D pos, v;
    int radius, jumpCount = 0;
    Circle body;
    boolean movingLeft = false, movingRight = false, movingUp = false, specialTransport = false;
    int coins = 0;
    int levelNum, sublevelNum;
    Checkpoint lastCheckpoint;
    boolean inGame = false;

    Character(double posX, double posY, int radius, Color color) {
        this.pos = new Point2D(posX, posY);
        this.v = new Point2D(0, 0);
        this.radius = radius;
        this.body = new Circle(posX, posY, radius);
        this.body.setFill(color);
        this.body.setStroke(Color.BLACK);
    }


    void revive() {
        if (lastCheckpoint==null) {
            this.inGame = false;
            this.terminate();
        } else {
            character.sublevelNum = this.lastCheckpoint.substageNum;
            Main.currentSublevel = Main.currentLevel.sublevels.get(character.sublevelNum - 1);
            pane = Main.currentSublevel.pane;
            pane.getChildren().remove(Main.canvas);
            pane.getChildren().add(Main.canvas);
            character.pos.setX(character.lastCheckpoint.pos.getX());
            character.pos.setY(character.lastCheckpoint.pos.getY());
            character.v.setX(0);
            character.v.setY(0);
            if (!pane.getChildren().contains(character.body)) pane.getChildren().add(character.body);
            Main.scene.setRoot(pane);
        }
    }

    void terminate() {
        mainCanvasGc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        character.inGame = false;
        character.v.setX(0);
        character.v.setY(0);
        PageLoader.loadDeathPage();
    }
}