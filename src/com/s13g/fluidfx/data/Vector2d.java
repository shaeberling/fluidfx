package com.s13g.fluidfx.data;

public class Vector2d {
  private double x;
  private double y;

  public Vector2d(double x, double y) {
    this.x = x;
    this.y = y;
  }


  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public void add(double dX, double dY) {
    this.x += dX;
    this.y += dY;
  }
}
