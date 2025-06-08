package com.binge;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

public class SpinningLaserObstacle extends Obstacle {

    Point2D pivotPoint;
    double length;
    double currentAngleRadians; // Current angle in radians
    double rotationSpeedRadians; // Radians per second

    // Calculated current endpoints of the laser, updated each frame
    Point2D currentStartPoint;
    Point2D currentEndPoint;

    // For blinking logic
    boolean isOn;
    double cycleDuration = 4.0; // 2s on + 2s off
    double onDuration = 2.0;
    double timer; // Manages the blinking cycle

    // Visual properties
    Color onColor = Color.CYAN; // Different color for spinning lasers for now
    double thickness = 3.0;

    // Pulsing properties
    boolean isPulsing;
    double minThickness;
    double maxThickness;
    double pulseDuration; // Time for one full pulse cycle
    double pulseTimer;    // Timer for the pulse cycle

    public SpinningLaserObstacle(Pane pane, Point2D pivot, double length,
                                 double initialAngleDegrees, double rotationSpeedDegrees,
                                 double initialTimerOffset,
                                 boolean isPulsing, double minThickness, double maxThickness, double pulseDuration) { // New pulsing parameters

        this.pivotPoint = pivot;
        this.length = length;
        this.currentAngleRadians = Math.toRadians(initialAngleDegrees);
        this.rotationSpeedRadians = Math.toRadians(rotationSpeedDegrees);

        this.fatal = true;
        this.color = this.onColor; // Use class field onColor

        // Blinking timer setup (from existing logic)
        this.cycleDuration = 4.0; // Assuming these are class constants or initialized here
        this.onDuration = 2.0;
        this.timer = initialTimerOffset % this.cycleDuration;
        this.isOn = (this.timer < this.onDuration);

        // Pulsing parameters initialization
        this.isPulsing = isPulsing;
        if (this.isPulsing) {
            this.minThickness = minThickness;
            this.maxThickness = maxThickness;
            this.pulseDuration = (pulseDuration > 0) ? pulseDuration : 1.0;
            this.pulseTimer = 0.0;
        } else {
            this.minThickness = this.thickness; // Use class field thickness
            this.maxThickness = this.thickness; // Use class field thickness
            this.pulseDuration = 1.0;
            this.pulseTimer = 0.0;
        }

        // Calculate initial endpoints (from existing logic)
        double halfLength = length / 2.0;
        double cosAngleInit = Math.cos(this.currentAngleRadians);
        double sinAngleInit = Math.sin(this.currentAngleRadians);
        this.currentStartPoint = new Point2D(
            this.pivotPoint.getX() + halfLength * cosAngleInit,
            this.pivotPoint.getY() + halfLength * sinAngleInit
        );
        this.currentEndPoint = new Point2D(
            this.pivotPoint.getX() - halfLength * cosAngleInit,
            this.pivotPoint.getY() - halfLength * sinAngleInit
        );
        this.pos = this.pivotPoint;

        // Setup visual body (from existing logic)
        Line lineBody = new Line(
            this.currentStartPoint.getX(), this.currentStartPoint.getY(),
            this.currentEndPoint.getX(), this.currentEndPoint.getY()
        );
        lineBody.setStrokeWidth(this.thickness); // Use class field thickness for initial/base
        lineBody.setStroke(this.onColor);    // Use class field onColor
        lineBody.setVisible(this.isOn);

        this.body = lineBody;
        pane.getChildren().add(this.body);
    }

    @Override
    public void update(double deltaTime) {
        // 1. Blinking Logic (from existing update)
        timer = (timer + deltaTime) % cycleDuration;
        boolean newIsOn = (timer < onDuration);
        if (newIsOn != isOn) {
            isOn = newIsOn;
            if (this.body != null) {
                this.body.setVisible(isOn);
            }
        }

        // 2. Rotation Logic (from existing update)
        currentAngleRadians += rotationSpeedRadians * deltaTime;
        double halfLength = length / 2.0;
        double cosAngle = Math.cos(currentAngleRadians);
        double sinAngle = Math.sin(currentAngleRadians);
        this.currentStartPoint = new Point2D(
            this.pivotPoint.getX() + halfLength * cosAngle,
            this.pivotPoint.getY() + halfLength * sinAngle
        );
        this.currentEndPoint = new Point2D(
            this.pivotPoint.getX() - halfLength * cosAngle,
            this.pivotPoint.getY() - halfLength * sinAngle
        );
        if (this.body instanceof Line) {
            Line lineBody = (Line) this.body;
            lineBody.setStartX(this.currentStartPoint.getX());
            lineBody.setStartY(this.currentStartPoint.getY());
            lineBody.setEndX(this.currentEndPoint.getX());
            lineBody.setEndY(this.currentEndPoint.getY());

            // 3. Pulsing logic (NEW - applied to the lineBody obtained above)
            if (this.isOn && this.isPulsing) {
                pulseTimer = (pulseTimer + deltaTime) % pulseDuration;
                double pulseProgressRatio = pulseTimer / pulseDuration;
                double wave = 0.5 * (1 - Math.cos(pulseProgressRatio * 2 * Math.PI));
                double currentVisualThickness = this.minThickness + (this.maxThickness - this.minThickness) * wave;
                lineBody.setStrokeWidth(currentVisualThickness);
            } else {
                lineBody.setStrokeWidth(this.thickness); // Set to base thickness
            }
        }
    }

    @Override
    boolean checkCollision(Character c, double dispX, double dispY, double deltaTime) {
        if (!isOn) {
            return false; // No collision if the laser is off
        }

        Point2D charCenter = c.pos; // Using current character position
        double charRadius = c.radius;
        double charRadiusSq = charRadius * charRadius;

        // Laser segment endpoints are already updated by the 'update' method
        Point2D p1 = this.currentStartPoint;
        Point2D p2 = this.currentEndPoint;

        // Vector representing the laser segment
        Point2D lineVec = p2.subtract(p1);
        // Vector from line start (p1) to character center
        Point2D startToChar = charCenter.subtract(p1);

        double lineLengthSq = lineVec.magnitudeSquared();

        if (lineLengthSq == 0) { // Laser has zero length (p1 and p2 are the same)
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
            Point2D scaledLineVec = lineVec.scale(tClamped); // tClamped was defined in the previous line
            Point2D closestPointOnSegment = new Point2D(p1.getX() + scaledLineVec.getX(), p1.getY() + scaledLineVec.getY());

        // Check if the distance from character center to closest point is less than radius
        if (charCenter.distanceSquared(closestPointOnSegment) < charRadiusSq) {
            handleCollision(c); // Call our specific handler
            return true;
        }

        return false;
    }

    // Placeholder for handleCollision method (specific to this class if needed, or use a generic one)
    // For now, let's assume a simple one for fatal lasers.
    public void handleCollision(Character c) {
        if (this.fatal && this.isOn) {
            c.revive();
        }
    }
}
