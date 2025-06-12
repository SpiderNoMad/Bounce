package com.binge;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;

public class HomingMissileLauncherObstacle extends Obstacle {

    public static enum LauncherState {
        IDLE,
        TRACKING,
        LOCKON, // Or CHARGING
        FIRING, // This state might be very short, just to launch, or manage a volley
        COOLDOWN
    }

    // Launcher Properties
    Point2D emitterPosition;
    double currentAngleRadians;
    double rotationSpeedRadiansPerSec;
    double detectionRangeSq; // Store squared for efficiency

    double lockonDurationSecs;
    double fireIntervalSecs; // Time between shots if firing a volley
    int projectilesPerVolley;
    int projectilesFiredThisVolley;
    double cooldownDurationSecs;

    double spreadAngleRadians; // Total angle of the cone for the spread, stored in radians

    LauncherState currentState;
    double stateTimerSecs;

    Shape emitterBody; // Visual for the launcher base
    Pane obstaclePane; // To add projectiles to the correct pane

    // Projectile Properties (parameters for the HomingLaserProjectile)
    double projectileSpeed;
    double projectileTurnRateDeg; // Store in degrees, convert when spawning
    double projectileLifespanSecs;

    // Colors for states
    static final Color LAUNCHER_IDLE_COLOR = Color.SLATEGRAY;
    static final Color LAUNCHER_TRACKING_COLOR = Color.LIGHTBLUE;
    static final Color LAUNCHER_LOCKON_COLOR = Color.GOLD;
    static final Color LAUNCHER_COOLDOWN_COLOR = Color.LIGHTGRAY;


    public HomingMissileLauncherObstacle(
            Pane pane, Point2D emitterPos,
            double rotationSpeedDeg, double detectionRange,
            double lockonSecs,
            double fireInterval, // Keep for now, though might be unused by basic spread
            int numProjectilesInSpread, // Formerly volleySize
            double spreadAngleDegParam,  // New parameter
            double cooldownSecs,
            double projSpeed, double projTurnRateDeg, double projLifespan,
            double initialAngleDegrees) {

        this.obstaclePane = pane;
        this.emitterPosition = emitterPos;
        this.pos = emitterPos;

        this.currentAngleRadians = Math.toRadians(initialAngleDegrees);
        this.rotationSpeedRadiansPerSec = Math.toRadians(rotationSpeedDeg);
        this.detectionRangeSq = detectionRange * detectionRange;

        this.lockonDurationSecs = lockonSecs;
        this.fireIntervalSecs = fireInterval; // Store it, might be used if volley > 1 spread, or for single shots
        this.projectilesPerVolley = Math.max(1, numProjectilesInSpread); // Use this for spread count
        this.spreadAngleRadians = Math.toRadians(spreadAngleDegParam); // Store new param
        this.projectilesFiredThisVolley = 0; // Reset for volley logic
        this.cooldownDurationSecs = cooldownSecs;

        this.projectileSpeed = projSpeed;
        this.projectileTurnRateDeg = projTurnRateDeg; // Keep in degrees for easy config
        this.projectileLifespanSecs = projLifespan;

        this.currentState = LauncherState.IDLE;
        this.stateTimerSecs = 0.0;

        this.fatal = false; // The launcher itself is not fatal, its projectiles are
        this.color = LAUNCHER_IDLE_COLOR; // Default Obstacle color

        this.emitterBody = new Circle(emitterPos.getX(), emitterPos.getY(), 12, LAUNCHER_IDLE_COLOR); // Slightly larger emitter
        this.obstaclePane.getChildren().add(this.emitterBody);
        this.body = this.emitterBody; // Assign to Obstacle's body
    }

    @Override
    public void update(double deltaTime) {
        stateTimerSecs += deltaTime;

        // Ensure Main.character and its position are accessible for TRACKING state
        Point2D playerPos = null;
        if (Main.character != null && Main.character.pos != null) {
            playerPos = Main.character.pos;
        }

        switch (currentState) {
            case IDLE:
                this.emitterBody.setFill(LAUNCHER_IDLE_COLOR);
                if (playerPos != null && emitterPosition.distanceSquared(playerPos) < detectionRangeSq) {
                    currentState = LauncherState.TRACKING;
                    stateTimerSecs = 0.0;
                }
                // Optional: Add sweeping scan rotation here if desired
                break;

            case TRACKING:
                this.emitterBody.setFill(LAUNCHER_TRACKING_COLOR);
                if (playerPos == null || emitterPosition.distanceSquared(playerPos) > detectionRangeSq) {
                    currentState = LauncherState.IDLE; // Player lost or out of range
                    stateTimerSecs = 0.0;
                    break;
                }

                // Aim at player
                double dx = playerPos.getX() - emitterPosition.getX();
                double dy = playerPos.getY() - emitterPosition.getY();
                double targetAngleRadians = Math.atan2(dy, dx);

                double angleDiff = targetAngleRadians - currentAngleRadians;
                while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
                while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

                double maxRotation = rotationSpeedRadiansPerSec * deltaTime;
                if (Math.abs(angleDiff) < maxRotation) {
                    currentAngleRadians = targetAngleRadians;
                } else {
                    currentAngleRadians += Math.signum(angleDiff) * maxRotation;
                }
                currentAngleRadians = (currentAngleRadians + 2 * Math.PI) % (2 * Math.PI);

                // If aimed (e.g., small angle difference or after a short time tracking)
                // For simplicity, let's use a small angle difference.
                if (Math.abs(angleDiff) < Math.toRadians(5.0)) { // 5 degree tolerance
                    currentState = LauncherState.LOCKON;
                    stateTimerSecs = 0.0;
                }
                break;

            case LOCKON:
                this.emitterBody.setFill(LAUNCHER_LOCKON_COLOR);
                if (playerPos == null || emitterPosition.distanceSquared(playerPos) > detectionRangeSq) {
                    currentState = LauncherState.IDLE; // Player lost during lockon
                    stateTimerSecs = 0.0;
                    break;
                }
                if (stateTimerSecs >= lockonDurationSecs) {
                    currentState = LauncherState.FIRING;
                    stateTimerSecs = 0.0;
                    projectilesFiredThisVolley = 0;
                }
                break;

            case FIRING:
                // This state now fires the entire spread at once, then transitions.
                // The 'fireIntervalSecs' and 'projectilesFiredThisVolley' would be used if firing
                // multiple individual projectiles sequentially within this FIRING state.
                // For a simultaneous spread, we fire all and then cooldown.

                if (projectilesFiredThisVolley == 0) { // Ensure this block runs only once per FIRING state entry
                    double centerAngle = this.currentAngleRadians; // Aimed direction
                    int numToFire = this.projectilesPerVolley;

                    if (numToFire == 1) {
                        // Fire a single projectile straight ahead
                        Point2D initialDir = new Point2D(Math.cos(centerAngle), Math.sin(centerAngle));
                        HomingLaserProjectile projectile = new HomingLaserProjectile(
                                this.obstaclePane,
                                new Point2D(this.emitterPosition.getX(), this.emitterPosition.getY()),
                                initialDir,
                                this.projectileSpeed,
                                this.projectileTurnRateDeg,
                                this.projectileLifespanSecs,
                                Main.character
                        );
                        Main.activeProjectiles.add(projectile);
                    } else {
                        double angleStep = this.spreadAngleRadians / (numToFire - 1);
                        double startAngle = centerAngle - this.spreadAngleRadians / 2.0;

                        for (int i = 0; i < numToFire; i++) {
                            double fireAngle = startAngle + (i * angleStep);
                            Point2D initialDir = new Point2D(Math.cos(fireAngle), Math.sin(fireAngle));
                            HomingLaserProjectile projectile = new HomingLaserProjectile(
                                    this.obstaclePane,
                                    new Point2D(this.emitterPosition.getX(), this.emitterPosition.getY()), // New Point2D for safety
                                    initialDir,
                                    this.projectileSpeed,
                                    this.projectileTurnRateDeg,
                                    this.projectileLifespanSecs,
                                    Main.character
                            );
                            Main.activeProjectiles.add(projectile);
                        }
                    }
                    projectilesFiredThisVolley = numToFire; // Mark volley as complete
                }

                // After firing the spread (which happens effectively instantly in one update frame), transition to COOLDOWN.
                // The stateTimerSecs for FIRING doesn't really apply here if it's an instant spread.
                // If FIRING state had a duration (e.g. for a sustained beam or continuous fire),
                // then stateTimerSecs would be checked.
                currentState = LauncherState.COOLDOWN;
                stateTimerSecs = 0.0; // Reset timer for COOLDOWN state
                // No need to change emitter color here, COOLDOWN state will handle it.
                break;

            case COOLDOWN:
                this.emitterBody.setFill(LAUNCHER_COOLDOWN_COLOR);
                if (stateTimerSecs >= cooldownDurationSecs) {
                    currentState = LauncherState.IDLE;
                    stateTimerSecs = 0.0;
                }
                break;
        }
        // Update emitter body rotation if it's a shape that shows rotation
        // e.g., if (!(emitterBody instanceof Circle)) {
        //    emitterBody.setRotate(Math.toDegrees(currentAngleRadians));
        // }
    }

    @Override
    boolean checkCollision(Character c, double dispX, double dispY, double deltaTime) {
        // Launcher itself might not be collidable, or simple circle collision with emitterBody
        // For now, let's say it's not directly harmful by touch.
        // Collision with this.emitterBody (this.body) can be implemented if needed.
        // Example:
        // if (this.body instanceof Circle && c != null && c.pos != null) {
        //    Circle eb = (Circle) this.body;
        //    double distSq = this.emitterPosition.distanceSquared(c.pos);
        //    double radiiSumSq = (eb.getRadius() + c.radius) * (eb.getRadius() + c.radius);
        //    return distSq < radiiSumSq;
        // }
        return false;
    }

    // If checkCollision can be true, this would handle it.
    // For now, if checkCollision is always false, this isn't strictly needed
    // but good practice to override if extending Obstacle.
    // void handleCollision(Character c, Point2D normal, double penetration, double deltaTime) {
    //    // If launcher is a physical blocker, apply physics. Otherwise, nothing.
    // }
    // Or if using the simpler Obstacle handleCollision signature (if we had one)
    // public void handleCollision(Character c) { /* ... */ }

    // Note: The `handleCollision` specific to this class might not be needed if the launcher itself isn't harmful.
    // The projectiles will have their own collision handling.
}
