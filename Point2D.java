package com.binge;

public class Point2D {
    double x, y;

    Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    double getX() {
        return this.x;
    }

    void setX(double x) {
        this.x = x;
    }

    double getY() {
        return this.y;
    }

    void setY(double y) {
        this.y = y;
    }

    void add(double x, double y) {
        this.x += x;
        this.y += y;
    }

    public Point2D subtract(Point2D other) {
        return new Point2D(this.x - other.x, this.y - other.y);
    }

    double magnitude() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    double getDistance(Point2D p) {
        Point2D dis = new Point2D(this.x - p.x, this.y - p.y);
        return dis.magnitude();
    }

    double dot(Point2D p) {
        return this.x * p.x + this.y * p.y;
    }

    Point2D normalize() {
        double mag = this.magnitude();
        if (mag == 0) return new Point2D(0,0); // Avoid division by zero
        return new Point2D(this.x / mag, this.y / mag);
    }

    Point2D scale(double s) {
        return new Point2D(this.x * s, this.y * s);
    }

    // Rotates this point around the origin by the given angle in radians
    public Point2D rotate(double angleRadians) {
        double cosA = Math.cos(angleRadians);
        double sinA = Math.sin(angleRadians);
        double newX = this.x * cosA - this.y * sinA;
        double newY = this.x * sinA + this.y * cosA;
        return new Point2D(newX, newY);
    }


    @Override
    public String toString() {
        return "Point2D{" + "x=" + x + ", y=" + y + '}';
    }
}