package com.s13g.fluidfx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.nio.IntBuffer;
import java.util.concurrent.*;

public class FlowFieldRenderer2d {
  private final GraphicsContext gc;
  private final FlowField flowField;
  private final int simSize;
  private final int scale;
  private final IntBuffer buffer;
  private final ExecutorService executor = Executors.newCachedThreadPool();

  public FlowFieldRenderer2d(GraphicsContext gc, FlowField flowField, int scale) {
    this.gc = gc;
    this.flowField = flowField;
    this.simSize = flowField.getSize();
    this.scale = scale;
    this.buffer = IntBuffer.allocate(simSize * scale * simSize * scale);
  }

  public void render() {
    int threadCount = 2;
    int rowCount = simSize / threadCount;
    Future[] futures = new Future[rowCount];
    for (int t = 0; t < threadCount; ++t) {
      final int tt = t;
      futures[t] = executor.submit(() -> render(tt * rowCount, rowCount));
    }

    try {
      for (int i = 0; i < threadCount; ++i) {
        futures[i].get();
      }
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }

//    for (int y = 0; y < simSize; ++y) {
//      for (int x = 0; x < simSize; ++x) {
//        double v = Math.min(flowField.getValueAt(x, y), 1.0);
//        for (int sy = y * scale, yy = sy; yy < sy + scale; ++yy) {
//          for (int sx = x * scale, xx = sx; xx < sx + scale; ++xx) {
//            buffer.put(idx(xx, yy), toColorByte(v));
//          }
//        }
//      }
//    }
    gc.getPixelWriter().setPixels(0, 0, simSize * scale, simSize * scale,
        PixelFormat.getIntArgbInstance(),
        buffer, simSize * scale);
  }

  private void render(int yStart, int rowCount) {
    for (int y = yStart; y < yStart + rowCount; ++y) {
      for (int x = 0; x < simSize; ++x) {
        double v = Math.min(flowField.getValueAt(x, y), 1.0);
        for (int sy = y * scale, yy = sy; yy < sy + scale; ++yy) {
          for (int sx = x * scale, xx = sx; xx < sx + scale; ++xx) {
            buffer.put(idx(xx, yy), toColorByte(v));
          }
        }
      }
    }
  }

  public void renderNaive() {
    PixelWriter pw = gc.getPixelWriter();
    for (int y = 0; y < simSize; ++y) {
      for (int x = 0; x < simSize; ++x) {
        double v = Math.min(flowField.getValueAt(x, y), 1.0);
        for (int sy = y * scale, yy = sy; yy < sy + scale; ++yy) {
          for (int sx = x * scale, xx = sx; xx < sx + scale; ++xx) {
            pw.setColor(xx, yy, Color.hsb(0, 0, v));
          }
        }
      }
    }
  }

  private int idx(int x, int y) {
    return x + (y * simSize * scale);
  }

  private static int toColorByte(double value) {
    int opacity = 255;
    int red = (int) (value * 255.0);
    int green = red;
    int blue = red;
    return opacity << 24 | red << 16 | green << 8 | blue;
  }
}
