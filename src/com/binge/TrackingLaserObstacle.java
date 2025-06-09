package com.binge;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

public class TrackingLaserObstacle extends Obstacle {

    public static enum LaserState {
        IDLE,       // Or SCANNING
        TRACKING,
        CHARGING,
        FIRING,
        COOLDOWN
    }

    Point2D emitterPosition;
    double currentAngleRadians;
    double rotationSpeedRadiansPerSec;
    double detectionRange;
    double detectionRangeSq; // For efficient distance checking

    double fireBeamLength;
    double chargeDurationSecs;
    double fireDurationSecs;
    double cooldownDurationSecs;

    LaserState currentState;
    double stateTimerSecs;

    Point2D laserFireTargetPoint; // Calculated when firing
    boolean isBeamActive;      // True during FIRING state

    Shape emitterBody;      // Visual for the turret base
    Line laserBeamBody;     // Visual for the laser beam

    // Colors for states (can be customized)
    static final Color IDLE_COLOR = Color.DARKGRAY;
    static final Color TRACKING_COLOR = Color.LIGHTSLATEGRAY; // Or some other indicator
    static final Color CHARGE_COLOR = Color.YELLOW;
    static final Color FIRE_COLOR = Color.ORANGE; // Laser beam color

    public TrackingLaserObstacle(Pane pane, Point2D emitterPos,
                                 double rotationSpeedDegPerSec, double detectionRange,
                                 double beamLength, double chargeSecs, double fireSecs,
                                 double cooldownSecs, double initialAngleDegrees) {

        this.emitterPosition = emitterPos;
        this.pos = emitterPos; // Set Obstacle's base position
        this.currentAngleRadians = Math.toRadians(initialAngleDegrees);
        this.rotationSpeedRadiansPerSec = Math.toRadians(rotationSpeedDegPerSec);
        this.detectionRange = detectionRange;
        this.detectionRangeSq = detectionRange * detectionRange;

        this.fireBeamLength = beamLength;
        this.chargeDurationSecs = chargeSecs;
        this.fireDurationSecs = fireSecs;
        this.cooldownDurationSecs = cooldownSecs;

        this.currentState = LaserState.IDLE;
        this.stateTimerSecs = 0.0;
        this.isBeamActive = false;
        this.laserFireTargetPoint = new Point2D(emitterPos.getX(), emitterPos.getY()); // Initial placeholder

        this.fatal = true; // This type of obstacle is fatal

        // Emitter visual (a simple circle)
        this.emitterBody = new Circle(emitterPos.getX(), emitterPos.getY(), 10, IDLE_COLOR);
        pane.getChildren().add(this.emitterBody);

        // Laser beam visual (initially invisible)
        this.laserBeamBody = new Line(emitterPos.getX(), emitterPos.getY(),
                                      emitterPos.getX(), emitterPos.getY()); // Start and end at emitter initially
        this.laserBeamBody.setStroke(FIRE_COLOR);
        this.laserBeamBody.setStrokeWidth(3.0); // Default thickness for the beam
        this.laserBeamBody.setVisible(false);
        pane.getChildren().add(this.laserBeamBody);

        // Note: this.body (from Obstacle) is not explicitly used by this obstacle type for collision.
        // Collision is based on the laserBeamBody's segment when active.
        // We can assign emitterBody to it if some Obstacle logic expects a non-null body,
        // but it won't be used for its primary collision.
        this.body = this.emitterBody; // Or null, if not strictly needed by base class logic
    }

    @Override
    public void update(double deltaTime) {
        stateTimerSecs += deltaTime;

        // Ensure Main.character and its position are accessible
        if (Main.character == null || Main.character.pos == null) {
            // Maybe revert to IDLE if character is not available
            if (currentState != LaserState.IDLE) {
                 currentState = LaserState.IDLE;
                 stateTimerSecs = 0.0;
                 this.emitterBody.setFill(IDLE_COLOR);
                 this.laserBeamBody.setVisible(false);
                 this.isBeamActive = false;
            }
            // Potentially add a small random rotation in IDLE if desired
            return;
        }
        Point2D playerPos = Main.character.pos;

        switch (currentState) {
            case IDLE:
                // Scan for player
                if (emitterPosition.distanceSquared(playerPos) < detectionRangeSq) {
                    currentState = LaserState.TRACKING;
                    stateTimerSecs = 0.0;
                    this.emitterBody.setFill(TRACKING_COLOR);
                }
                // Optional: Add slight random sweep rotation here if desired
                // Example: currentAngleRadians += (Math.random() - 0.5) * 0.01;
                break;

            case TRACKING:
                // Calculate target angle to player
                double dx = playerPos.getX() - emitterPosition.getX();
                double dy = playerPos.getY() - emitterPosition.getY();
                double targetAngleRadians = Math.atan2(dy, dx);

                // Normalize angles for shortest path rotation
                double angleDiff = targetAngleRadians - currentAngleRadians;
                while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
                while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

                // Rotate towards target angle, clamped by rotationSpeed
                double maxRotation = rotationSpeedRadiansPerSec * deltaTime;
                if (Math.abs(angleDiff) < maxRotation) {
                    currentAngleRadians = targetAngleRadians; // Snap if close enough
                } else {
                    currentAngleRadians += Math.signum(angleDiff) * maxRotation;
                }
                // Normalize currentAngleRadians to [0, 2*PI) or (-PI, PI]) if preferred (optional)
                currentAngleRadians = (currentAngleRadians + 2 * Math.PI) % (2 * Math.PI);


                // Check if aimed (e.g., small angle difference or time based)
                // Using a time threshold for now for simplicity
                double AIM_THRESHOLD_SECONDS = 0.5; // Time to track before charging
                if (stateTimerSecs > AIM_THRESHOLD_SECONDS || Math.abs(angleDiff) < Math.toRadians(5.0) /*5 degrees tolerance*/) {
                    currentState = LaserState.CHARGING;
                    stateTimerSecs = 0.0;
                    this.emitterBody.setFill(CHARGE_COLOR);
                }

                if (emitterPosition.distanceSquared(playerPos) > detectionRangeSq) { // Player out of range
                    currentState = LaserState.IDLE;
                    stateTimerSecs = 0.0;
                    this.emitterBody.setFill(IDLE_COLOR);
                }
                break;

            case CHARGING:
                // Optional: Visual effect for charging (e.g., emitterBody.setRadius(...))
                if (stateTimerSecs >= chargeDurationSecs) {
                    currentState = LaserState.FIRING;
                    stateTimerSecs = 0.0;
                    isBeamActive = true; // Mark beam as active

                    // Initial beam setup will happen in the first frame of FIRING state.
                    // Just ensure visibility and color are set here.
                    laserBeamBody.setVisible(true);
                    this.emitterBody.setFill(FIRE_COLOR);
                }
                break;

            case FIRING:
                // Aiming logic (similar to TRACKING state)
                if (Main.character != null && Main.character.pos != null) {
                    Point2D playerPosFiring = Main.character.pos;
                    double dxFiring = playerPosFiring.getX() - emitterPosition.getX();
                    double dyFiring = playerPosFiring.getY() - emitterPosition.getY();
                    double targetAngleRadiansFiring = Math.atan2(dyFiring, dxFiring);

                    double angleDiffFiring = targetAngleRadiansFiring - currentAngleRadians;
                    while (angleDiffFiring > Math.PI) angleDiffFiring -= 2 * Math.PI;
                    while (angleDiffFiring < -Math.PI) angleDiffFiring += 2 * Math.PI;

                    double maxRotationFiring = rotationSpeedRadiansPerSec * deltaTime;
                    if (Math.abs(angleDiffFiring) < maxRotationFiring) {
                        currentAngleRadians = targetAngleRadiansFiring;
                    } else {
                        currentAngleRadians += Math.signum(angleDiffFiring) * maxRotationFiring;
                    }
                    currentAngleRadians = (currentAngleRadians + 2 * Math.PI) % (2 * Math.PI);
                }
                // If character is null, laser continues firing at last known angle.

                // Update laser beam path based on current (possibly new) angle
                this.laserFireTargetPoint = new Point2D(
                    this.emitterPosition.getX() + this.fireBeamLength * Math.cos(this.currentAngleRadians),
                    this.emitterPosition.getY() + this.fireBeamLength * Math.sin(this.currentAngleRadians)
                );

                this.laserBeamBody.setStartX(this.emitterPosition.getX());
                this.laserBeamBody.setStartY(this.emitterPosition.getY());
                this.laserBeamBody.setEndX(this.laserFireTargetPoint.getX());
                this.laserBeamBody.setEndY(this.laserFireTargetPoint.getY());
                // ensure laserBeamBody is visible (already set when entering FIRING, but good for safety)
                if (!this.laserBeamBody.isVisible()) {
                    this.laserBeamBody.setVisible(true);
                }


                // Check duration
                if (stateTimerSecs >= fireDurationSecs) {
                    currentState = LaserState.COOLDOWN;
                    stateTimerSecs = 0.0;
                    isBeamActive = false;
                    laserBeamBody.setVisible(false);
                    this.emitterBody.setFill(IDLE_COLOR);
                }
                break;

            case COOLDOWN:
                if (stateTimerSecs >= cooldownDurationSecs) {
                    currentState = LaserState.IDLE; // Or TRACKING if player still in range
                    stateTimerSecs = 0.0;
                    // Emitter color already set to IDLE_COLOR or COOLDOWN_COLOR
                }
                break;
        }

        // Update emitter visual rotation (if applicable, e.g., if emitterBody is an ImageView or a custom shape)
        // For a simple Circle, rotation isn't visible. If it were a an ImageView or a Path:
        // emitterBody.setRotate(Math.toDegrees(currentAngleRadians));
        // This would require emitterBody to be created with its "front" pointing along the 0-angle axis.
    }

    @Override
    boolean checkCollision(Character c, double dispX, double dispY, double deltaTime) {
        if (!isBeamActive) { // Only check collision if the beam is currently firing
            return false;
        }

        Point2D charCenter = c.pos; // Using current character position
        double charRadius = c.radius;
        double charRadiusSq = charRadius * charRadius;

        // Laser segment endpoints are emitterPosition and laserFireTargetPoint
        Point2D p1 = this.emitterPosition;
        Point2D p2 = this.laserFireTargetPoint; // This is updated when Firing state begins

        // Vector representing the laser segment
        Point2D lineVec = p2.subtract(p1);
        // Vector from line start (p1) to character center
        Point2D startToChar = charCenter.subtract(p1);

        double lineLengthSq = lineVec.magnitudeSquared();

        if (lineLengthSq == 0) { // Laser beam has zero length (should not happen if fireBeamLength > 0)
            // Collision is essentially point (p1) vs circle (character)
            if (charCenter.distanceSquared(p1) < charRadiusSq) {
                handleCollision(c);
                return true;
            }
            return false;
        }

        // Project startToChar onto lineVec to find the parameter t
        double t = startToChar.dot(lineVec) / lineLengthSq;

        // Clamp t to the range [0, 1] to find the closest point on the segment
        double tClamped = Math.max(0, Math.min(1, t));

        // Calculate the closest point on the segment to the character's center
        // Point2D closestPointOnSegment = p1.add(lineVec.scale(tClamped));
        // Corrected usage for Point2D:
        Point2D scaledLineVec = lineVec.scale(tClamped);
        Point2D closestPointOnSegment = new Point2D(p1.getX() + scaledLineVec.getX(), p1.getY() + scaledLineVec.getY());


        // Check if the distance from character center to closest point is less than radius
        if (charCenter.distanceSquared(closestPointOnSegment) < charRadiusSq) {
            handleCollision(c); // Call our specific handler
            return true;
        }

        return false;
    }

    // handleCollision can be simple if it just revives player
    public void handleCollision(Character c) {
        if (this.fatal) { // isBeamActive check will be in checkCollision
            c.revive();
        }
    }
}
