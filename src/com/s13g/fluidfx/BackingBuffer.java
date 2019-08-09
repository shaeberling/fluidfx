package com.s13g.fluidfx;

import javafx.scene.canvas.GraphicsContext;

public interface BackingBuffer {
  void set(int x, int y, double value);

  void render(GraphicsContext gc);
}
