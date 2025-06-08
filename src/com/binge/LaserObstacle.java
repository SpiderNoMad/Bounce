package com.binge;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape; // Required for the body property

public class LaserObstacle extends Obstacle {

    public static enum LaserOrientation {
        HORIZONTAL,
        VERTICAL
    }

    private final LaserOrientation orientation;
    Point2D startPoint; // Stores the logical start of the laser beam
    Point2D endPoint;   // Stores the logical end of the laser beam

    boolean isOn;
    double cycleDuration = 4.0; // 2s on + 2s off
    double onDuration = 2.0;
    double timer; // Manages the blinking cycle

    Color onColor = Color.RED; // Laser color when active
    // Off color is handled by visibility, so no offColor field needed for stroke
    double thickness = 3.0; // Base thickness

    // Pulsing properties
    boolean isPulsing;
    double minThickness;
    double maxThickness;
    double pulseDuration; // Time for one full pulse cycle (e.g., grow and shrink)
    double pulseTimer;    // Timer for the pulse cycle

    public LaserObstacle(Pane pane, LaserOrientation orientation,
                         double primaryAxisPos, double startSecondaryAxis, double endSecondaryAxis,
                         boolean initiallyOn, double initialTimerOffset, // Existing parameters
                         boolean isPulsing, double minThickness, double maxThickness, double pulseDuration) { // New pulsing parameters

        this.orientation = orientation; // From existing logic
        this.fatal = true;              // From existing logic
        this.color = onColor;           // From existing logic (onColor is a class field)

        this.cycleDuration = 4.0;       // From existing logic (or ensure it's a class field)
        this.onDuration = 2.0;          // From existing logic (or ensure it's a class field)
        this.timer = initialTimerOffset % cycleDuration; // From existing logic
        this.isOn = (this.timer < onDuration);           // From existing logic

        // Coordinate and point calculations (from existing logic)
        double lineStartX, lineStartY, lineEndX, lineEndY;
        if (orientation == LaserOrientation.HORIZONTAL) {
            this.pos = new Point2D(startSecondaryAxis + (endSecondaryAxis - startSecondaryAxis) / 2, primaryAxisPos);
            this.startPoint = new Point2D(startSecondaryAxis, primaryAxisPos);
            this.endPoint = new Point2D(endSecondaryAxis, primaryAxisPos);
            lineStartX = startSecondaryAxis; lineStartY = primaryAxisPos; lineEndX = endSecondaryAxis; lineEndY = primaryAxisPos;
        } else { // VERTICAL
            this.pos = new Point2D(primaryAxisPos, startSecondaryAxis + (endSecondaryAxis - startSecondaryAxis) / 2);
            this.startPoint = new Point2D(primaryAxisPos, startSecondaryAxis);
            this.endPoint = new Point2D(primaryAxisPos, endSecondaryAxis);
            lineStartX = primaryAxisPos; lineStartY = startSecondaryAxis; lineEndX = primaryAxisPos; lineEndY = endSecondaryAxis;
        }

        // Pulsing parameters initialization
        this.isPulsing = isPulsing;
        if (this.isPulsing) {
            this.minThickness = minThickness;
            this.maxThickness = maxThickness;
            // Ensure pulseDuration is positive to avoid division by zero or negative time
            this.pulseDuration = (pulseDuration > 0) ? pulseDuration : 1.0; // Default to 1s if invalid
            this.pulseTimer = 0.0;
        } else {
            this.minThickness = this.thickness; // this.thickness is the base class field (e.g. 3.0)
            this.maxThickness = this.thickness;
            this.pulseDuration = 1.0;
            this.pulseTimer = 0.0;
        }

        // Setup the visual representation (JavaFX Line)
        Line lineBody = new Line(lineStartX, lineStartY, lineEndX, lineEndY);
        // Set initial stroke width: if pulsing and on, it could be minThickness,
        // or just base thickness and let update() fix it in the first frame.
        // Simpler to set to base thickness, update() will adjust if needed.
        lineBody.setStrokeWidth(this.thickness);
        lineBody.setStroke(this.onColor);
        lineBody.setVisible(this.isOn);

        this.body = lineBody;
        pane.getChildren().add(this.body);
    }

    @Override
    public void update(double deltaTime) {
        // Blinking logic
        timer = (timer + deltaTime) % cycleDuration;
        boolean newIsOn = (timer < onDuration);

        if (newIsOn != isOn) {
            isOn = newIsOn;
            if (this.body != null) {
                this.body.setVisible(isOn);
            }
        }

        // Pulsing logic
        if (this.body instanceof Line) { // Ensure body is a Line
            Line lineBody = (Line) this.body;
            if (this.isOn && this.isPulsing) {
                pulseTimer = (pulseTimer + deltaTime) % pulseDuration;

                // Calculate sinusoidal pulse progress (0 to 1 and back to 0)
                double pulseProgressRatio = pulseTimer / pulseDuration;
                double wave = 0.5 * (1 - Math.cos(pulseProgressRatio * 2 * Math.PI)); // Value from 0 to 1

                double currentVisualThickness = this.minThickness + (this.maxThickness - this.minThickness) * wave;
                lineBody.setStrokeWidth(currentVisualThickness);
            } else {
                // Set to base thickness if not pulsing or not on
                lineBody.setStrokeWidth(this.thickness);
            }
        }
    }

    @Override
    boolean checkCollision(Character c, double dispX, double dispY, double deltaTime) {
        if (!isOn) {
            return false; // No collision if the laser is off
        }

        // Predicted character position (center of the circle)
        // For lasers, which are instantaneous hazards, using the current character position
        // might be sufficient, as they don't typically "push" the character.
        // If precise swept collision is needed, it's more complex. Let's start with current position.
        Point2D charPos = c.pos; // Current character center

        // Laser properties depend on orientation
        boolean overlapPrimaryAxis, overlapSecondaryAxis;

        if (this.orientation == LaserOrientation.HORIZONTAL) {
            double laserY = this.startPoint.getY(); // y-coordinate of the horizontal laser
            double laserStartX = this.startPoint.getX();
            double laserEndX = this.endPoint.getX();

            overlapPrimaryAxis = Math.abs(charPos.getY() - laserY) < c.radius;
            overlapSecondaryAxis = (charPos.getX() + c.radius > laserStartX) &&
                                   (charPos.getX() - c.radius < laserEndX);
        } else { // VERTICAL
            double laserX = this.startPoint.getX(); // x-coordinate of the vertical laser
            double laserStartY = this.startPoint.getY();
            double laserEndY = this.endPoint.getY();

            overlapPrimaryAxis = Math.abs(charPos.getX() - laserX) < c.radius;
            overlapSecondaryAxis = (charPos.getY() + c.radius > laserStartY) &&
                                   (charPos.getY() - c.radius < laserEndY);
        }

        if (overlapPrimaryAxis && overlapSecondaryAxis) {
            // Collision detected.
            // For lasers, the "normal" and "penetration" are less about physics response
            // and more about just detecting the hit. We can pass dummy values or null
            // if handleCollision for lasers doesn't use them.
            // The `handleCollision(Character c)` signature was a placeholder.
            // Let's assume we need to call the inherited fatal logic if it exists,
            // or directly call c.revive().
            // The Obstacle class doesn't have a generic handleCollision.
            // Our LaserObstacle has `handleCollision(Character c)`.
            handleCollision(c); // Call our specific handler
            return true;
        }

        return false;
    }

    // Placeholder for handleCollision method - will be detailed in a future step
    // It might need to match a specific signature if Obstacle's collision handling evolves.
    // For now, let's assume a simple one.
    public void handleCollision(Character c) {
        if (this.fatal && this.isOn) {
            c.revive();
        }
    }
}
