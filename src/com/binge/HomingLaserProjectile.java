package com.binge;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;

public class HomingLaserProjectile {

    Point2D position;
    Point2D velocity; // Current velocity vector (direction and speed combined)
    double speed;     // Magnitude of velocity
    double turnRateRadiansPerSec; // Max rate at which it can change its velocity direction

    double lifespanSecs; // How long it exists before disappearing
    Character target;    // Reference to Main.character

    Shape body;          // Visual representation (e.g., small Circle)
    Pane projectilePane; // Pane this projectile belongs to, for removal

    boolean isActive;    // To mark for removal from game loop and pane

    static final double DEFAULT_RADIUS = 5.0; // Default radius for the projectile body

    public HomingLaserProjectile(Pane pane, Point2D startPos, Point2D initialVelocityDir,
                                 double speed, double turnRateDeg, double lifespan,
                                 Character target) {
        this.projectilePane = pane;
        this.position = new Point2D(startPos.getX(), startPos.getY()); // Ensure new Point2D if startPos is mutable
        this.speed = speed;

        Point2D normalizedInitialDir = initialVelocityDir.normalize();
        this.velocity = normalizedInitialDir.scale(this.speed);

        this.turnRateRadiansPerSec = Math.toRadians(turnRateDeg);
        this.lifespanSecs = lifespan;
        this.target = target; // Could be null, handle in update

        this.body = new Circle(this.position.getX(), this.position.getY(), DEFAULT_RADIUS, Color.MAGENTA);
        // Ensure this new body is added to the pane passed in,
        // which should be the main game pane where the character and other elements reside.
        // This pane reference is also stored for later removal.
        this.projectilePane.getChildren().add(this.body);

        this.isActive = true;
    }

    public void update(double deltaTime) {
        if (!isActive) {
            return;
        }

        // Lifespan check
        lifespanSecs -= deltaTime;
        if (lifespanSecs <= 0) {
            setActive(false); // Mark for removal
            // Visual removal will be handled by main game loop based on isActive, or here if preferred
            // removeFromPane(); // Could also be called here directly
            return;
        }

        // Homing/Steering logic
        if (target != null && target.pos != null) {
            Point2D dirToTarget = target.pos.subtract(this.position); // Vector from projectile to target

            // Calculate target angle (angle of dirToTarget)
            double targetAngle = Math.atan2(dirToTarget.getY(), dirToTarget.getX());

            // Calculate current movement angle
            double currentAngle = Math.atan2(this.velocity.getY(), this.velocity.getX());

            // Calculate shortest angle difference
            double angleDiff = targetAngle - currentAngle;
            while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
            while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

            // Clamp rotation to turnRateRadiansPerSec
            double maxTurnThisFrame = turnRateRadiansPerSec * deltaTime;
            double actualTurn = Math.max(-maxTurnThisFrame, Math.min(maxTurnThisFrame, angleDiff));

            double newAngle = currentAngle + actualTurn;

            // Update velocity vector based on new angle and original speed
            this.velocity = new Point2D(Math.cos(newAngle) * this.speed,
                                        Math.sin(newAngle) * this.speed);
        }
        // If no target, or target.pos is null, it continues in its current velocity direction.

        // Update position based on velocity
        Point2D displacement = this.velocity.scale(deltaTime); // scale still returns a new Point2D
        this.position = this.position.add(displacement); // Now uses the new add method

        // Update visual body's screen position
        if (this.body instanceof Circle) {
            ((Circle)this.body).setCenterX(this.position.getX());
            ((Circle)this.body).setCenterY(this.position.getY());
        }
        // Else if using another shape, update its layoutX/Y or specific properties
    }

    public boolean checkCollisionWithPlayer(Character player) {
        if (!isActive) {
            return false; // Cannot collide if not active
        }
        if (player == null || player.pos == null || this.body == null) {
            return false; // Cannot collide if essential objects are null
        }

        // Assuming projectile body is a Circle, get its radius.
        // If body could be other shapes, this needs to be more generic or radius stored separately.
        double projectileRadius = 0;
        if (this.body instanceof Circle) {
            projectileRadius = ((Circle) this.body).getRadius();
        } else {
            // Fallback or error if body is not a Circle and radius isn't otherwise known
            // For now, let's assume it's always a Circle as per constructor
            // or use a fixed hitbox size if body is abstract.
            // Using DEFAULT_RADIUS as defined in the class is safer if body could change.
            projectileRadius = DEFAULT_RADIUS;
        }

        double playerRadius = player.radius;

        // Calculate squared distance between centers
        double distSq = this.position.distanceSquared(player.pos);

        // Calculate sum of radii squared
        double sumRadii = projectileRadius + playerRadius;
        double sumRadiiSq = sumRadii * sumRadii;

        if (distSq < sumRadiiSq) {
            // Collision detected
            this.setActive(false); // Deactivate projectile on hit
            // this.removeFromPane(); // Optionally remove from pane immediately
                                 // Or let main loop handle removal based on isActive.
            return true;
        }

        return false;
    }

    // Call this to remove the projectile's visual from the scene
    public void removeFromPane() {
        if (this.projectilePane != null && this.body != null) {
            this.projectilePane.getChildren().remove(this.body);
        }
    }

    // Getter for isActive, useful for the main game loop to know when to remove
    public boolean isActive() {
        return isActive;
    }

    // Setter for isActive, e.g., when lifespan ends or collision occurs
    public void setActive(boolean active) {
        this.isActive = active;
    }

    // Getter for the body, if needed externally (e.g. for adding to different layers)
    public Shape getBody() {
        return body;
    }
}
