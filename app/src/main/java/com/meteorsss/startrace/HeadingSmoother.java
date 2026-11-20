package com.meteorsss.startrace;

/** Frame-rate-independent shortest-arc interpolation. No sensor/platform dependencies. */
public final class HeadingSmoother {
  private double value = Double.NaN;
  private long previousFrame;

  public static double delta(double from, double to) {
    return (Geo.normalize(to) - Geo.normalize(from) + 540) % 360 - 180;
  }

  public double update(double target, long frameNanos) {
    if (!Double.isFinite(target)) {
      reset();
      return Double.NaN;
    }
    if (!Double.isFinite(value)
        || previousFrame == 0
        || frameNanos - previousFrame > 1_000_000_000L) value = Geo.normalize(target);
    else if (frameNanos > previousFrame) {
      double dt = (frameNanos - previousFrame) / 1_000_000_000.0;
      double alpha = 1 - Math.exp(-dt / 0.045);
      value = Geo.normalize(value + delta(value, target) * alpha);
    }
    previousFrame = frameNanos;
    return value;
  }

  public void reset() {
    value = Double.NaN;
    previousFrame = 0;
  }
}
