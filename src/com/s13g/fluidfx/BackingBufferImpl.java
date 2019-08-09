package com.s13g.fluidfx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;

import java.nio.IntBuffer;

public class BackingBufferImpl implements BackingBuffer {
  private final int size;
  private final int scale;
  private final IntBuffer data;

  public BackingBufferImpl(int size, int scale) {
    this.size = size;
    this.scale = scale;
    this.data = IntBuffer.allocate(size * scale * size * scale);
  }

  @Override
  public void set(int x, int y, double value) {
    for (int sy = y * scale, yy = sy; yy < sy + scale; ++yy) {
      for (int sx = x * scale, xx = sx; xx < sx + scale; ++xx) {
        data.put(idx(x, y), toColorByte(value));
      }
    }

  }

  @Override
  public void render(GraphicsContext gc) {
    gc.getPixelWriter().setPixels(0, 0, size * scale, size * scale,
        PixelFormat.getIntArgbInstance(),
        data, size * scale);
  }

  private int idx(int x, int y) {
    return x + (y * size * scale);
  }

  private static int toColorByte(double value) {
    int opacity = 255;
    int red = (int) (value * 255.0);
    int green = red;
    int blue = red;
    return opacity << 24 | red << 16 | green << 8 | blue;
  }

}
