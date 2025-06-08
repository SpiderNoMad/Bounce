package com.binge;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate; // Import Rotate

import java.util.*;


public abstract class Obstacle {
    Point2D pos; // For CircleObstacle, this is center. For RectangleObstacle, this will be center.
    Shape body;
    Color color;
    boolean fatal;
    double epsilon = 1e-5; // Small value to prevent sticking

    abstract boolean checkCollision(Character c, double dispX, double dispY, double deltaTime);
    // Original handleCollision is specific to how checkCollision determines the interaction.
    // For more complex shapes like rotated rectangles, it's often better for checkCollision
    // to determine the normal and penetration, then call a more generic handler.
    // abstract void handleCollision(Character c, double deltaTime); // This might be removed or changed

    public abstract void update(double deltaTime);
}

class CircleObstacle extends Obstacle {
    private int radius;
    // Circle body is already a field in its JavaFX shape representation

    CircleObstacle(Pane pane, double posX, double posY, int radius, Color color) {
        this(pane, posX, posY, radius, color, false);
    }

    CircleObstacle(Pane pane, double posX, double posY, int radius, Color color, boolean fatal) {
        this.pos = new Point2D(posX, posY); // Center of the circle
        this.radius = radius;
        this.body = new Circle(posX, posY, radius);
        this.fatal = fatal;
        this.color = (fatal ? Color.RED : color);
        this.body.setFill(this.color);
        this.body.setStroke(Color.BLACK);
        pane.getChildren().add(this.body);
    }

    @Override
    boolean checkCollision(Character c, double dispX, double dispY, double deltaTime) {
        // Predicted character center
        Point2D predictedCharPos = new Point2D(c.pos.getX() + dispX, c.pos.getY() + dispY);

        double distSq = predictedCharPos.getDistance(this.pos) * predictedCharPos.getDistance(this.pos);
        double combinedRadius = c.radius + this.radius;

        if (distSq < combinedRadius * combinedRadius) {
            // Collision detected
            Point2D collisionNormal = predictedCharPos.subtract(this.pos).normalize();
            double penetration = combinedRadius - Math.sqrt(distSq);
            handleCollision(c, collisionNormal, penetration, deltaTime);
            return true;
        }
        return false;
    }

    // New handleCollision signature for CircleObstacle
    void handleCollision(Character c, Point2D normal, double penetration, double deltaTime) {
        if (this.fatal) c.revive();

        c.jumpCount = 0;

        // Position correction
        c.pos.add(normal.getX() * (penetration + epsilon), normal.getY() * (penetration + epsilon));
        c.body.setCenterX(c.pos.getX());
        c.body.setCenterY(c.pos.getY());

        // Velocity reflection (simple bounce for circles)
        double vDotN = c.v.dot(normal);
        if (vDotN < 0) { // Moving into the obstacle
            double restitution = Main.FRICTION; // Use global friction as restitution
            c.v.add(-normal.getX() * (1 + restitution) * vDotN, -normal.getY() * (1 + restitution) * vDotN);
        }
    }

    @Override
    public void update(double deltaTime) {
        // No active update logic needed for this obstacle type
    }
}

class RectangleObstacle extends Obstacle {
    private final double width, height;
    private final double angle; // Angle in radians

    // Constructor updated for center position and angle
    RectangleObstacle(Pane pane, double centerX, double centerY, double width, double height, double angleDegrees, Color color, boolean fatal) {
        this.pos = new Point2D(centerX, centerY); // Store center position
        this.width = width;
        this.height = height;
        this.angle = Math.toRadians(angleDegrees);
        this.fatal = fatal;

        // Create a rectangle shape, position it so its center is at (0,0) for rotation, then translate
        Rectangle rectShape = new Rectangle(-width / 2, -height / 2, width, height);
        rectShape.setFill(this.color);
        rectShape.setStroke(Color.BLACK);

        // Apply rotation around the center of the rectangle
        Rotate rotate = new Rotate(angleDegrees, 0, 0); // Rotate around its local center (0,0)

        this.body = rectShape;
        // Translate to the final position AFTER setting up rotation relative to its own center
        this.body.setLayoutX(centerX);
        this.body.setLayoutY(centerY);
        this.body.getTransforms().addAll(rotate);

        this.color = (fatal ? Color.RED : color);
        this.body.setFill(this.color);

        pane.getChildren().add(this.body);
    }


    @Override
    boolean checkCollision(Character c, double dispX, double dispY, double deltaTime) {
        Point2D charPredPosWorld = new Point2D(c.pos.getX() + dispX, c.pos.getY() + dispY);

        // Transform character's center to rectangle's local coordinate system
        // 1. Translate charPredPosWorld so rectangle's center (this.pos) is the origin
        Point2D charRelToRectCenter = charPredPosWorld.subtract(this.pos);

        // 2. Rotate this relative position by -this.angle
        Point2D charLocalPos = charRelToRectCenter.rotate(-this.angle);

        // Now, perform collision check with an AABB centered at (0,0) with rect's width/height
        double halfWidth = this.width / 2.0;
        double halfHeight = this.height / 2.0;

        // Find the closest point on the AABB (in local coords) to charLocalPos
        double clampedX = Math.max(-halfWidth, Math.min(charLocalPos.getX(), halfWidth));
        double clampedY = Math.max(-halfHeight, Math.min(charLocalPos.getY(), halfHeight));

        Point2D closestPointLocal = new Point2D(clampedX, clampedY);
        double distSq = charLocalPos.subtract(closestPointLocal).magnitude(); // Recalculate distance correctly
        distSq *= distSq;


        if (distSq < c.radius * c.radius) {
            // Collision detected
            double actualDistance = Math.sqrt(distSq);
            double penetration = c.radius - actualDistance;

            // Calculate collision normal (from rectangle towards circle)
            // Normal in local coordinates (from closestPointLocal to charLocalPos)
            Point2D normalLocal = charLocalPos.subtract(closestPointLocal).normalize();
            if (actualDistance < epsilon) { // Character center is very close to/on closest point (e.g. inside)
                // If charLocalPos is inside the AABB, clampedX/Y is charLocalPos.x/y.
                // We need a robust way to find the normal.
                // A common approach is to find the shallowest penetration axis.
                // For simplicity here, if deep inside, push out along vector from rect center to char center (local).
                // However, charLocalPos.subtract(closestPointLocal) should still give a direction.
                // If distance is near zero because char center is ON the clamped point, it means
                // char center might be inside.
                // A better normal if inside: vector from charLocalPos to one of the AABB edges.
                // Smallest overlap determines the normal.
                // dx_pen = c.radius - (halfWidth - Math.abs(charLocalPos.getX())) -> this is for AABB vs AABB
                // For Circle vs AABB, if charLocalPos is inside AABB, then normal is from charLocalPos to nearest edge.
                // Let's assume charLocalPos.subtract(closestPointLocal).normalize() is mostly fine for now
                // It points from the surface of the rectangle towards the center of the circle.
                // If charLocalPos == closestPointLocal (center of circle is inside rectangle)
                // the normal should be based on which "face" is closest to escape.
                // This case is tricky. For now, if distSq is very small and positive, normalLocal is valid.
                // If distSq is zero (char center on closest point), and that point is an edge, normal is outward from edge.
                // If char center is *inside* the rectangle, distSq calculation would be 0 using this method
                // because closestPointLocal would be charLocalPos. This needs refinement.

                // Refined check for character center inside the rectangle (local coordinates)
                if (Math.abs(charLocalPos.getX()) < halfWidth && Math.abs(charLocalPos.getY()) < halfHeight) {
                    // Character center is inside the rectangle.
                    // Find overlap with each side to determine shallowest penetration axis.
                    double dx = halfWidth - Math.abs(charLocalPos.getX());
                    double dy = halfHeight - Math.abs(charLocalPos.getY());

                    if (dx < dy) {
                        normalLocal = new Point2D(charLocalPos.getX() > 0 ? 1 : -1, 0);
                        penetration = c.radius + dx; // penetration is radius + how much it's inside the halfwidth
                    } else {
                        normalLocal = new Point2D(0, charLocalPos.getY() > 0 ? 1 : -1);
                        penetration = c.radius + dy;
                    }
                }
                // else, the original normalLocal calculation is for when char center is outside.
            }


            // Transform normal back to world coordinates by rotating it by this.angle
            Point2D collisionNormalWorld = normalLocal.rotate(this.angle).normalize();

            handleCollision(c, collisionNormalWorld, penetration, deltaTime);
            return true;
        }
        return false;
    }

    // New handleCollision method for RectangleObstacle
    void handleCollision(Character c, Point2D normal, double penetration, double deltaTime) {
        if (this.fatal) c.revive();

        c.jumpCount = 0; // Reset jump on any collision with obstacle

        // 1. Position Correction (move character out of penetration)
        // Ensure penetration is positive
        penetration = Math.max(0, penetration);
        c.pos.add(normal.getX() * (penetration + epsilon), normal.getY() * (penetration + epsilon));
        c.body.setCenterX(c.pos.getX());
        c.body.setCenterY(c.pos.getY());

        // 2. Velocity Adjustment for Sliding and Bounce
        double vDotN = c.v.dot(normal);

        if (vDotN < 0) { // Character is moving into the surface
            double restitution = 0.5; // Low restitution for less bounce, more slide
            double surfaceFrictionCoefficient = 0.01; // Friction for sliding along the surface

            // Decompose velocity into normal and tangential components
            Point2D vn = normal.scale(vDotN); // Normal component of velocity (points into surface)
            Point2D vt = new Point2D(c.v.getX() - vn.getX(), c.v.getY() - vn.getY()); // Tangential component

            // New velocity:
            // Normal component: Reflects with restitution (bounce)
            // Tangential component: Scaled by friction (slide)
            double newVx = -vn.getX() * restitution + vt.getX() * (1.0 - surfaceFrictionCoefficient);
            double newVy = -vn.getY() * restitution + vt.getY() * (1.0 - surfaceFrictionCoefficient);

            c.v.setX(newVx);
            c.v.setY(newVy);
        }
    }

    @Override
    public void update(double deltaTime) {
        // No active update logic needed for this obstacle type
    }
}

class CutOffObstacle extends Obstacle {
    Shape body;

    CutOffObstacle(Pane pane, Shape main, Shape cut, Color color) {
        this.body = Shape.subtract(main, cut);
        this.body.setFill(color);
        this.body.setStroke(Color.BLACK);
        pane.getChildren().add(this.body);
    }

    boolean checkCollision(Character c, double dispX, double dispY, double deltaTime) {
        // Complex collision for generic shapes, typically involves checking path intersections
        // or using libraries for this. For now, returning false.
        return false;
    }

    // No specific handleCollision defined for CutOffObstacle yet.
    // If it were to be used, it would need its own collision response.

    @Override
    public void update(double deltaTime) {
        // No active update logic needed for this obstacle type
    }
}