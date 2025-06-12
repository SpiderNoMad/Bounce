package com.binge;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Polygon;

public class SpiralMissileLauncherObstacle extends Obstacle {

    public static enum SpiralLauncherState {
        IDLE,
        AIMING_TARGET_POSITION, // Optional: Aim towards player first
        FIRING_SPIRAL,
        COOLDOWN
    }

    // Launcher Properties
    Point2D emitterPosition;
    double currentAngleRadians; // Used for initial aim and during spiral
    double initialAimRotationSpeedRadPerSec; // Speed to aim at player before starting spiral
    double spiralRotationSpeedRadPerSec;   // Rotation speed during the spiral itself
    double detectionRangeSq;

    double aimDurationSecs;        // How long to aim/lock before spiral starts
    double spiralFireDurationSecs; // Total duration of the spiral firing sequence
    double fireIntervalSecs;       // Time between projectiles during spiral
    double timeSinceLastShot;      // Timer for individual shots
    double cooldownDurationSecs;

    SpiralLauncherState currentState;
    double stateTimerSecs;         // General timer for current state duration

    Shape emitterBody;
    Pane obstaclePane; // Pane to add projectiles to

    // Projectile Properties (parameters for the HomingLaserProjectile)
    double projectileSpeed;
    double projectileTurnRateDeg;
    double projectileLifespanSecs;

    // Colors for states (can be new or reuse others)
    static final Color SPIRAL_IDLE_COLOR = Color.DARKOLIVEGREEN;
    static final Color SPIRAL_AIMING_COLOR = Color.OLIVEDRAB;
    static final Color SPIRAL_FIRING_COLOR = Color.GREENYELLOW; // Emitter color when firing spiral
    static final Color SPIRAL_COOLDOWN_COLOR = Color.LIGHTGREEN;


    public SpiralMissileLauncherObstacle(
            Pane pane, Point2D emitterPos,
            double initialAimRotSpeedDeg, double spiralRotSpeedDeg,
            double detectionRange, double aimTimeSecs,
            double spiralFireDurSecs, double fireInterval,
            double cooldownSecs,
            double projSpeed, double projTurnRateDeg, double projLifespan,
            double initialAngleDegrees) {

        this.obstaclePane = pane;
        this.emitterPosition = emitterPos;
        this.pos = emitterPos; // Obstacle base position

        this.currentAngleRadians = Math.toRadians(initialAngleDegrees);
        this.initialAimRotationSpeedRadPerSec = Math.toRadians(initialAimRotSpeedDeg);
        this.spiralRotationSpeedRadPerSec = Math.toRadians(spiralRotSpeedDeg);
        this.detectionRangeSq = detectionRange * detectionRange;

        this.aimDurationSecs = aimTimeSecs;
        this.spiralFireDurationSecs = spiralFireDurSecs;
        this.fireIntervalSecs = fireInterval;
        this.cooldownDurationSecs = cooldownSecs;

        this.projectileSpeed = projSpeed;
        this.projectileTurnRateDeg = projTurnRateDeg;
        this.projectileLifespanSecs = projLifespan;

        this.currentState = SpiralLauncherState.IDLE;
        this.stateTimerSecs = 0.0;
        this.timeSinceLastShot = 0.0; // Initialize shot timer

        this.fatal = false; // Launcher itself is not fatal
        this.color = SPIRAL_IDLE_COLOR; // Default Obstacle color

        // Emitter visual (a triangle pointing right by default)
        double size = 12.0; // "Radius" or characteristic size of the triangle
        Polygon triangleEmitter = new Polygon();
        triangleEmitter.getPoints().addAll(new Double[]{
            size, 0.0,                                  // Point 1 (Tip pointing right)
            -size / 2.0, size * 0.8660254,              // Point 2 (Top-left, for equilateral)
            -size / 2.0, -size * 0.8660254             // Point 3 (Bottom-left, for equilateral)
        });
        // 0.8660254 is approx Math.sin(60 degrees) or Math.sqrt(3)/2

        triangleEmitter.setFill(SPIRAL_IDLE_COLOR); // Set initial color

        // Set the layout position of the polygon (its defined points are relative to 0,0)
        triangleEmitter.setLayoutX(emitterPos.getX());
        triangleEmitter.setLayoutY(emitterPos.getY());

        this.emitterBody = triangleEmitter;
        this.obstaclePane.getChildren().add(this.emitterBody);
        this.body = this.emitterBody; // Assign to Obstacle's body
    }

    @Override
    public void update(double deltaTime) {
        stateTimerSecs += deltaTime; // General timer for state duration
        timeSinceLastShot += deltaTime; // Timer for firing interval

        Point2D playerPos = null;
        if (Main.character != null && Main.character.pos != null) {
            playerPos = Main.character.pos;
        }

        switch (currentState) {
            case IDLE:
                this.emitterBody.setFill(SPIRAL_IDLE_COLOR);
                if (playerPos != null && emitterPosition.distanceSquared(playerPos) < detectionRangeSq) {
                    currentState = SpiralLauncherState.AIMING_TARGET_POSITION;
                    stateTimerSecs = 0.0;
                    timeSinceLastShot = 0.0; // Reset shot timer as well
                }
                break;

            case AIMING_TARGET_POSITION:
                this.emitterBody.setFill(SPIRAL_AIMING_COLOR);
                if (playerPos == null || emitterPosition.distanceSquared(playerPos) > detectionRangeSq) {
                    currentState = SpiralLauncherState.IDLE; // Player lost or out of range
                    stateTimerSecs = 0.0;
                    break;
                }

                // Aim at player using initialAimRotationSpeedRadPerSec
                double dx = playerPos.getX() - emitterPosition.getX();
                double dy = playerPos.getY() - emitterPosition.getY();
                double targetAngleRadians = Math.atan2(dy, dx);

                double angleDiff = targetAngleRadians - currentAngleRadians;
                while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
                while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

                double maxRotation = initialAimRotationSpeedRadPerSec * deltaTime;
                if (Math.abs(angleDiff) < maxRotation) {
                    currentAngleRadians = targetAngleRadians;
                } else {
                    currentAngleRadians += Math.signum(angleDiff) * maxRotation;
                }
                currentAngleRadians = (currentAngleRadians + 2 * Math.PI) % (2 * Math.PI);

                // Transition to FIRING_SPIRAL after aimDurationSecs or if sufficiently aimed
                if (stateTimerSecs >= aimDurationSecs || Math.abs(angleDiff) < Math.toRadians(5.0)) {
                    currentState = SpiralLauncherState.FIRING_SPIRAL;
                    stateTimerSecs = 0.0;       // Reset general state timer for spiral duration
                    timeSinceLastShot = fireIntervalSecs; // Fire first shot immediately in FIRING_SPIRAL
                }
                break;

            case FIRING_SPIRAL:
                this.emitterBody.setFill(SPIRAL_FIRING_COLOR);

                // Continuous rotation during spiral
                currentAngleRadians += spiralRotationSpeedRadPerSec * deltaTime;
                currentAngleRadians = (currentAngleRadians + 2 * Math.PI) % (2 * Math.PI); // Normalize angle

                if (timeSinceLastShot >= fireIntervalSecs) {
                    Point2D initialDir = new Point2D(Math.cos(currentAngleRadians), Math.sin(currentAngleRadians));

                    HomingLaserProjectile projectile = new HomingLaserProjectile(
                            this.obstaclePane,
                            new Point2D(this.emitterPosition.getX(), this.emitterPosition.getY()),
                            initialDir,
                            this.projectileSpeed,
                            this.projectileTurnRateDeg,
                            this.projectileLifespanSecs,
                            Main.character // Projectiles are homing
                    );
                    Main.activeProjectiles.add(projectile);
                    timeSinceLastShot = 0.0; // Reset timer for the next shot
                }

                if (stateTimerSecs >= spiralFireDurationSecs) {
                    currentState = SpiralLauncherState.COOLDOWN;
                    stateTimerSecs = 0.0;
                }
                break;

            case COOLDOWN:
                this.emitterBody.setFill(SPIRAL_COOLDOWN_COLOR);
                if (stateTimerSecs >= cooldownDurationSecs) {
                    currentState = SpiralLauncherState.IDLE;
                    stateTimerSecs = 0.0;
                }
                break;
        }
        // Visual update for emitter rotation if it's not a Circle
        // if (!(emitterBody instanceof Circle)) { // Original check, but Polygon also has setRotate
        //     emitterBody.setRotate(Math.toDegrees(currentAngleRadians));
        // }
        // Update emitter body rotation
        if (this.emitterBody != null) {
            this.emitterBody.setRotate(Math.toDegrees(this.currentAngleRadians));
        }
    }

    @Override
    boolean checkCollision(Character c, double dispX, double dispY, double deltaTime) {
        // Launcher body is not the primary threat
        return false;
    }

    // No specific handleCollision needed if checkCollision is always false
}
