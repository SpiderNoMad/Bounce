package com.binge;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.*;

public class Main extends Application {
    public static Canvas canvas;
    public static Pane pane;
    public static Scene scene;

    // Screen properties
    public static final int WINDOW_HEIGHT = 800;
    public static final int WINDOW_WIDTH = 1200;

    // Physics constants
    public static final double GRAVITY = 980;          // pixels per second squared
    private static final double MOVE_ACCELERATION = 600; // horizontal acceleration, pixels per second squared
    private static final double MAX_MOVE_SPEED = 1000;    // maximum horizontal speed
    private static final double NATURAL_SPEED_LIM = 500;
    public static final double FRICTION = 0.6; 
    
    // containers
    public static Level currentLevel = new Level(0);
    public static Sublevel currentSublevel = new Sublevel(0);

    // For fixed timestep physics
    public static Timeline timeline;
    private static final double FIXED_PHYSICS_DT = 1.0 / 60.0; // Physics update rate (e.g., 60Hz)
    public static GraphicsContext mainCanvasGc; // To allow drawLine from updateGamePhysics

    // Main character
    public static Character character = new Character(150, 50, 20, Color.rgb(255,241,204));

    @Override
    public void start(Stage stage) {
        canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        mainCanvasGc = canvas.getGraphicsContext2D(); // Store the GraphicsContext

        pane = new Pane(canvas);

        PageLoader.loadMainPage();

//        scene = new Scene(pane, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setScene(scene);
        stage.setTitle("Ball");
        stage.show();

        handleKeyEvent();

        final double FIXED_PHYSICS_DT = 1.0 / 60.0; // 60 FPS physics
        final Duration frameDuration = Duration.seconds(FIXED_PHYSICS_DT);

        timeline = new Timeline(new KeyFrame(frameDuration, e -> {
            if (character.inGame) {
                mainCanvasGc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                updateGamePhysics(character, pane, canvas);
            }
        }));

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

    }

    private void updateGamePhysics(Character character, Pane pane, Canvas canvas) {

        // 1. Apply forces (Gravity, Input)
        character.v.add(0, GRAVITY * Main.FIXED_PHYSICS_DT);

        if (character.movingLeft && character.v.getX() > -NATURAL_SPEED_LIM) {
            character.v.add(-MOVE_ACCELERATION * Main.FIXED_PHYSICS_DT, 0);
        }
        if (character.movingRight && character.v.getX() < NATURAL_SPEED_LIM) {
            character.v.add(MOVE_ACCELERATION * Main.FIXED_PHYSICS_DT, 0);
        }
        if (character.movingUp) { // Jump is an impulse
            character.v.setY(-450); // Adjusted jump velocity, tune as needed
            character.movingUp = false;
        }

        // 2. Clamp velocity (max speed limits)
        character.v.setX(Math.max(-MAX_MOVE_SPEED, Math.min(character.v.getX(), MAX_MOVE_SPEED)));
        character.v.setY(Math.max(-MAX_MOVE_SPEED, Math.min(character.v.getY(), MAX_MOVE_SPEED))); // MAX_MOVE_SPEED for Y might be very high

        // 3. Collision Detection and Resolution with Obstacles
        boolean characterCollidedWithObstacle = false;
        for (Obstacle obs : currentSublevel.obstacles) {
            double displacementX = character.v.getX() * Main.FIXED_PHYSICS_DT;
            double displacementY = character.v.getY() * Main.FIXED_PHYSICS_DT;
            if (obs.checkCollision(character, displacementX, displacementY, Main.FIXED_PHYSICS_DT)) {
                characterCollidedWithObstacle = true;
            }
        }

        // 4. Collectibles
        Iterator<Collectible> itemIterator = currentSublevel.items.iterator();
        while (itemIterator.hasNext()) {
            Collectible item = itemIterator.next();
            if (item.checkCollision(character)) {
                item.handleCollision(character);
                if (item.hitbox != null && item.hitbox.body != null) {
                    pane.getChildren().remove(item.hitbox.body);
                }
                itemIterator.remove();
            }
        }


        // 5. Displacers (e.g., GrapplePoint)
        for (Displacer d : currentSublevel.displacers) {
            if (d.checkCollision(character)) {
                if (d instanceof GrapplePoint gp) {
                    if (!gp.cooldown) {
                        // Draw line directly using mainCanvasGc
                        drawLine(this.mainCanvasGc, character.pos.x, character.pos.y, gp.pos.x, gp.pos.y);
                    }
                    if (character.specialTransport && !gp.cooldown) {
                        d.handleCollision(character); // GrapplePoint might need 'dt' if its effect is over time
                    }
                }
            }
        }

        for (Checkpoint c : currentLevel.checkpoints) {
            if (c != null && currentSublevel.num == c.substageNum) {
                double displacementX = character.v.getX() * Main.FIXED_PHYSICS_DT;
                double displacementY = character.v.getY() * Main.FIXED_PHYSICS_DT;
                c.checkCollision(character, displacementX, displacementY, Main.FIXED_PHYSICS_DT);
            }
        }

        for (Lock l : currentSublevel.locks) {
            l.key.checkCollision(character);
            l.checkCollision(character, 0, 0, Main.FIXED_PHYSICS_DT);
        }

        if (currentSublevel.goal != null) {
            currentSublevel.goal.checkCollision(character, 0, 0, Main.FIXED_PHYSICS_DT);
        }

        // 6. Update position IF NO OBSTACLE COLLISION handled position
        // If an obstacle collision occurred, its handleCollision should have set the correct position.
        // If characterCollidedWithObstacle is true, we assume position and velocity are handled.
        // If false, apply standard Euler integration for position.
        if (!characterCollidedWithObstacle) {
            character.pos.add(character.v.getX() * Main.FIXED_PHYSICS_DT, character.v.getY() * Main.FIXED_PHYSICS_DT);
        }
        // Always sync visual body to logical position after all physics and collision responses.
        // Note: Obstacle.java handleCollision should update character.pos, this ensures body reflects it.
        character.body.setCenterX(character.pos.getX());
        character.body.setCenterY(character.pos.getY());


        // 7. Boundary Collisions (Pane edges)
        double restitutionBoundary = 0.4; // How much to bounce off pane boundaries
        // Ground
        if (character.body.getCenterY() + character.body.getRadius() > pane.getHeight()) {
            character.body.setCenterY(pane.getHeight() - character.body.getRadius());
            character.pos.setY(character.body.getCenterY());
            if (character.v.getY() > 0) character.v.setY(-character.v.getY() * restitutionBoundary);
            character.jumpCount = 0; // Reset jump count on ground
        }
        // Ceiling
        if (character.body.getCenterY() - character.body.getRadius() < 0) {
            character.body.setCenterY(character.body.getRadius());
            character.pos.setY(character.body.getCenterY());
            if (character.v.getY() < 0) character.v.setY(-character.v.getY() * restitutionBoundary);
        }
        // Left Wall
        if (character.body.getCenterX() - character.body.getRadius() < 0) {
            int lastSubstage = character.sublevelNum - 1;
            String lastStagePath = "src/com/binge/Stages/stage1/" + lastSubstage + ".in";
            File lastStageFile = new File(lastStagePath);

//            if (lastStageFile.exists() && character.currentSubstage!=1) {
            if (character.sublevelNum - 1 >= 1) {
                character.sublevelNum -= 1;
                currentSublevel = currentLevel.sublevels.get(character.sublevelNum -1);
                scene.setRoot(currentSublevel.pane);
                if (!currentSublevel.pane.getChildren().contains(canvas)) currentSublevel.pane.getChildren().add(canvas);
                Point2D pos = character.pos;
                pos.add(WINDOW_WIDTH - 2.5*character.radius, 0);
                character.pos = pos;
                character.body.setCenterX(character.pos.getX());
                character.body.setCenterY(character.pos.getY());
                if (!currentSublevel.pane.getChildren().contains(character.body)) currentSublevel.pane.getChildren().add(character.body);
            } else {
                character.body.setCenterX(character.body.getRadius());
                character.pos.setX(character.body.getCenterX());
                if (character.v.getX() < 0) character.v.setX(-character.v.getX() * restitutionBoundary);
            }
        }
        // Right Wall
        if (character.body.getCenterX() + character.body.getRadius() > pane.getWidth()) {
            int nextSubstage = character.sublevelNum + 1;
            String nextStagePath = "src/com/binge/Stages/stage1/" + nextSubstage + ".in";
            File nextStageFile = new File(nextStagePath);

//            if (nextStageFile.exists()) {
            if (character.sublevelNum + 1 <= currentLevel.levelLength) {
                character.sublevelNum += 1;
                currentSublevel = currentLevel.sublevels.get(character.sublevelNum -1);
                scene.setRoot(currentSublevel.pane);
                if (!currentSublevel.pane.getChildren().contains(canvas)) currentSublevel.pane.getChildren().add(canvas);
                Point2D pos = character.pos;
                pos.add(-WINDOW_WIDTH + 2.5*character.radius, 0);
                character.pos = pos;
                character.body.setCenterX(character.pos.getX());
                character.body.setCenterY(character.pos.getY());
                if (!currentSublevel.pane.getChildren().contains(character.body)) currentSublevel.pane.getChildren().add(character.body);
            } else {
                character.body.setCenterX(pane.getWidth() - character.body.getRadius());
                character.pos.setX(character.body.getCenterX());
                if (character.v.getX() > 0) {
                    character.v.setX(-character.v.getX() * restitutionBoundary);
                }
            }
        }


        // Global friction/drag - apply this carefully.
        // This could be air resistance or a general damping.
        // If applied after specific collision responses, it will affect them.
        // For smoother sliding on surfaces, the surface's own friction should dominate.
        // Let's apply a very light air drag if NOT in collision with an obstacle that handled friction.
        if (!characterCollidedWithObstacle) {
            double airDragCoefficient = 0.01; // Very light drag
            character.v.setX(character.v.getX() * (1.0 - airDragCoefficient * Main.FIXED_PHYSICS_DT)); // Scale by dt for consistency
            character.v.setY(character.v.getY() * (1.0 - airDragCoefficient * Main.FIXED_PHYSICS_DT));
        }
        // The old `character.v = character.v.scale(FRICTION);` when `collide` was true is removed
        // to let obstacle-specific friction take precedence.

        // System.out.println(character.v.getX() + " " + character.v.getY() + " | Jump: " + character.jumpCount);
    }

    private void handleKeyEvent() {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.A || event.getCode() == KeyCode.LEFT) character.movingLeft = true;
            if (event.getCode() == KeyCode.D || event.getCode() == KeyCode.RIGHT) character.movingRight = true;
            if (event.getCode() == KeyCode.W || event.getCode() == KeyCode.UP) {
                if (character.jumpCount < 2) { // Allow double jump if on ground or in air once
                    character.jumpCount++;
                    character.movingUp = true; // This will be an impulse
                }
            }
            if (event.getCode() == KeyCode.P) {
                if (timeline.getStatus() == Animation.Status.PAUSED) {
                    timeline.play();
                } else {
                    timeline.pause();
                }
            }
            if (event.getCode() == KeyCode.SPACE) character.specialTransport = true;
        });

        scene.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.A || event.getCode() == KeyCode.LEFT) character.movingLeft = false;
            if (event.getCode() == KeyCode.D || event.getCode() == KeyCode.RIGHT) character.movingRight = false;
            // movingUp is an impulse, so no key release needed to stop it in the same way as continuous movement
            // If W was for continuous upward thrust, then character.movingUp = false; would be needed.
            if (event.getCode() == KeyCode.SPACE) character.specialTransport = false;
        });
    }

    private void drawLine(GraphicsContext gc, double x0, double y0, double x1, double y1) {
        gc.save();
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        gc.strokeLine(x0, y0, x1, y1);
    }

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}