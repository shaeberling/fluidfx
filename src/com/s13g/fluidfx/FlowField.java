package com.s13g.fluidfx;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FlowField {
  private static final int ITERATIONS = 1;

  private final int size;
  private final double dt;
  private final double diffusion;
  private final double viscosity;

  private double[] s;
  private double[] density;

  private double[] vX;
  private double[] vY;
  private double[] vX0;
  private double[] vY0;

  private final ExecutorService executor = Executors.newCachedThreadPool();

  FlowField(int size, double dt, int diffusion, int viscosity) {
    this.size = size;
    this.dt = dt;
    this.diffusion = diffusion;
    this.viscosity = viscosity;

    this.s = new double[size * size];
    this.density = new double[size * size];
//    this.velocity = new Vector2d[size * size];
//    this.velocity0 = new Vector2d[size * size];
    this.vX = new double[size * size];
    this.vY = new double[size * size];
    this.vX0 = new double[size * size];
    this.vY0 = new double[size * size];
  }

  // Diffuse all three velocity components.
  // Fix up velocities so they keep things incompressible
  // Move the velocities around according to the velocities of the fluid (confused yet?).
  // Fix up the velocities again
  // Diffuse the dye.
  // Move the dye around according to the velocities.

  void step() {
    try {
      stepInternal();
    } catch (ExecutionException | InterruptedException e) {
      System.err.println("Something went wrong.");
      e.getCause().printStackTrace();
    }
  }

  private void stepInternal() throws ExecutionException, InterruptedException {
    Future<?> diffuse1 = executor.submit(() -> {
      diffuse(1, vX0, vX, viscosity);
    });
    Future<?> diffuse2 = executor.submit(() -> {
      diffuse(2, vY0, vY, viscosity);
    });

    diffuse1.get();
    diffuse2.get();

    project(vX0, vY0, vX, vY);

    Future<?> advect1 = executor.submit(() -> {
      advect(1, vX, vX0, vX0, vY0);
    });

    Future<?> advect2 = executor.submit(() -> {
      advect(2, vY, vY0, vX0, vY0);
    });

    advect1.get();
    advect2.get();

    project(vX, vY, vX0, vY0);

    diffuse(0, s, density, diffusion);
    advect(0, density, s, vX, vY);
  }

  int getSize() {
    return size;
  }

  double getValueAt(int x, int y) {
    return density[idx(x, y)];
  }

  void addDieDensity(int x, int y, double amount) {
    density[idx(x, y)] += amount;
  }

  void addVelocity(int x, int y, double amountX, double amountY) {
//    this.velocity[idx(x, y)].add(amountX, amountY);

    this.vX[idx(x, y)] += amountX;
    this.vY[idx(x, y)] += amountY;
  }

  private int idx(int x, int y) {
    return x + (y * size);
  }

  // CHECKED: OK
  private void diffuse(int b, double[] x, double[] x0, double foo) {
    double a = dt * foo * (size - 2) * (size - 2);
    lin_solve(b, x, x0, a, 1 + 6 * a);
  }

  // CHECKED: OK
  private void project(double[] velocX, double[] velocY, double[] p, double[] div) {
    for (int j = 1; j < size - 1; j++) {
      for (int i = 1; i < size - 1; i++) {
        div[idx(i, j)] = -0.5 * (
            velocX[idx(i + 1, j)]
                - velocX[idx(i - 1, j)]
                + velocY[idx(i, j + 1)]
                - velocY[idx(i, j - 1)]
        ) / size;
        p[idx(i, j)] = 0;
      }
    }
    set_bnd(0, div);
    set_bnd(0, p);
    lin_solve(0, p, div, 1, 6);

    for (int j = 1; j < size - 1; j++) {
      for (int i = 1; i < size - 1; i++) {
        velocX[idx(i, j)] -= 0.5 * (p[idx(i + 1, j)]
            - p[idx(i - 1, j)]) * size;
        velocY[idx(i, j)] -= 0.5 * (p[idx(i, j + 1)]
            - p[idx(i, j - 1)]) * size;
      }
    }
    set_bnd(1, velocX);
    set_bnd(2, velocY);
  }

  // CHECKED: OK
  private void advect(int b, double[] d, double[] d0, double[] velocX, double[] velocY) {
    int i0, i1, j0, j1;

    double dtx = dt * (size - 2);
    double dty = dt * (size - 2);

    double s0, s1, t0, t1;
    double x, y;

    int i, j;

    for (j = 1; j < size - 40; j++) {
      for (i = 1; i < size - 40; i++) {
        x = i - (dtx * velocX[idx(i, j)]);
        y = j - (dty * velocY[idx(i, j)]);

        x = Math.min(Math.max(x, 0.5), size - 0.5);
        i0 = (int) Math.floor(x);
        i1 = i0 + 1;

        y = Math.min(Math.max(y, 0.5), size - 0.5);
        j0 = (int) Math.floor(y);
        j1 = j0 + 1;

        s1 = x - i0;
        s0 = 1.0 - s1;
        t1 = y - j0;
        t0 = 1.0 - t1;

        d[idx(i, j)] =
            s0 * (t0 * d0[idx(i0, j0)] + t1 * d0[idx(i0, j1)]) +
                s1 * (t0 * d0[idx(i1, j0)] + t1 * d0[idx(i1, j1)]);
      }
    }
    set_bnd(b, d);
  }

  // CHECKED: OK
  private void lin_solve(int b, double[] x, double[] x0, double a, double c) {
    double cRecip = 1.0 / c;
    for (int k = 0; k < ITERATIONS; k++) {
      for (int j = 1; j < size - 1; j++) {
        for (int i = 1; i < size - 1; i++) {
          x[idx(i, j)] =
              (x0[idx(i, j)]
                  + a * (x[idx(i + 1, j)]
                  + x[idx(i - 1, j)]
                  + x[idx(i, j + 1)]
                  + x[idx(i, j - 1)]
              )) * cRecip;
        }
      }
    }
    set_bnd(b, x);
  }


  // FIXME
  private void set_bnd(int b, double[] x) {
    for (int i = 1; i < size - 1; i++) {
      x[idx(0, i)] = b == 1 ? -x[idx(1, i)] : x[idx(1, i)];
      x[idx(size - 1, i)] = b == 1 ? -x[idx(size - 1, i)] : x[idx(size - 1, i)];
    }
    for (int i = 1; i < size - 1; i++) {
      x[idx(i, 0)] = b == 2 ? -x[idx(i, 1)] : x[idx(i, 1)];
      x[idx(i, size - 1)] = b == 2 ? -x[idx(i, size - 1)] : x[idx(i, size - 1)];
    }
    x[idx(0, 0)] = 0.5 * (x[idx(1, 0)] + x[idx(0, 1)]);
    x[idx(0, size - 1)] = 0.5 * (x[idx(1, size - 1)] + x[idx(0, size - 1)]);
    x[idx(size - 1, 0)] = 0.5 * (x[idx(size - 2, 0)] + x[idx(size - 1, 1)]);
    x[idx(size - 1, size - 1)] = 0.5 * (x[idx(size - 2, size - 1)] + x[idx(size - 1, size - 2)]);
  }

}
