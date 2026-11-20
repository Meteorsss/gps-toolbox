package com.meteorsss.startrace;

public final class TrackFilter {
  private TrackFilter() {}

  public static boolean usable(TrackPoint p, long nowElapsed) {
    return Geo.valid(p.lat, p.lon)
        && Double.isFinite(p.accuracy)
        && p.accuracy >= 0
        && p.accuracy <= 80
        && p.elapsed <= nowElapsed + 1000
        && nowElapsed - p.elapsed <= 15000;
  }

  public static boolean plausible(TrackPoint previous, TrackPoint p) {
    if (previous == null) return true;
    long dt = p.elapsed - previous.elapsed;
    if (dt <= 0) return false;
    if (dt > 30000) return true;
    double limit = Math.max(100, Double.isFinite(p.speed) ? p.speed * 3 + 15 : 100);
    return Geo.distance(previous.lat, previous.lon, p.lat, p.lon) / (dt / 1000.0) <= limit;
  }

  public static double distanceContribution(TrackPoint anchor, TrackPoint p) {
    if (anchor == null || anchor.segment != p.segment || p.elapsed - anchor.elapsed > 30000)
      return 0;
    double distance = Geo.distance(anchor.lat, anchor.lon, p.lat, p.lon);
    double floor = Math.max(3, (anchor.accuracy + p.accuracy) * 0.6);
    boolean moving = Double.isFinite(p.speed) && p.speed >= 0.5;
    return distance >= floor && (moving || distance >= Math.max(10, p.accuracy * 1.5))
        ? distance
        : 0;
  }
}
