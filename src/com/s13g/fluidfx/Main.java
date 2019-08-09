package com.s13g.fluidfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {
  private static final int SIM_SIZE = 512;
  private static final int RENDER_SCALE = 2;
  private FlowField flowField = new FlowField(SIM_SIZE, 0.1, 0, 0);
  private GraphicsContext gc = null;
  private FlowFieldRenderer2d renderer;

  private final int FRAME_TIME_MS = 10;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  @Override
  public void start(Stage primaryStage) throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("fluidfx.fxml"));
    primaryStage.setTitle("Fluid FX - s13g");
    primaryStage.setScene(new Scene(root, SIM_SIZE * RENDER_SCALE, SIM_SIZE * RENDER_SCALE));
    primaryStage.show();

    Canvas canvas = (Canvas) primaryStage.getScene().lookup("#canvas");
    canvas.setWidth(SIM_SIZE * RENDER_SCALE);
    canvas.setHeight(SIM_SIZE * RENDER_SCALE);
    System.out.println("Canvas found: " + canvas.getHeight());

    gc = canvas.getGraphicsContext2D();
    gc.setFill(Color.BLUE);
    gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    renderer = new FlowFieldRenderer2d(canvas.getGraphicsContext2D(), flowField, RENDER_SCALE);
    executor.submit(this::step);
  }

  private void step() {
    List<Source> sources = generateSources(100);
    while (true) {
      try {
        long start = System.currentTimeMillis();
        // Add mouse input to simulate sources...
        for (Source source : sources) {
          flowField.addDieDensity(source.x, source.y, source.density);
          flowField.addVelocity(source.x, source.y, source.velX, source.velY);
        }

        flowField.step();
        long simDuration = System.currentTimeMillis() - start;
        long startRender = System.currentTimeMillis();

        renderer.render();
        int renderDuration = (int) (System.currentTimeMillis() - startRender);
        System.out.println(String.format("Simulation: %d ms / Rendering %d ms", simDuration,
            renderDuration));

        int totalDuration = (int) (System.currentTimeMillis() - start);
        int waitMs = Math.max(0, FRAME_TIME_MS - totalDuration);
        Thread.sleep(waitMs);
      } catch (Exception ex) {
        System.err.println(" Something went wrong: ");
        ex.printStackTrace();
      }
    }
  }

  private static List<Source> generateSources(int num) {
    List<Source> sources = new ArrayList<>();
    for (int i = 0; i < num; ++i) {
      int x = (int) (Math.random() * (SIM_SIZE - 10));
      int y = (int) (Math.random() * (SIM_SIZE - 10));
      double density = Math.random() * 2;
      double velX = Math.random() * 2 - 1;
      double velY = Math.random() * 2 - 1;
      sources.add(new Source(x, y, density, velX, velY));
    }
    return sources;
  }


  public static void main(String[] args) {
    launch(args);
  }

  private static class Source {
    final int x, y;
    final double density;
    final double velX, velY;

    Source(int x, int y, double density, double velX, double velY) {
      this.x = x;
      this.y = y;
      this.density = density;
      this.velX = velX;
      this.velY = velY;
    }
  }
}
