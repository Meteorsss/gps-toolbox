package com.meteorsss.startrace;

public final class TrackPoint {
  public final double lat, lon, altitude, accuracy, speed;
  public final long time, elapsed;
  public final int segment;
  public final boolean accepted;

  public TrackPoint(
      double lat,
      double lon,
      double altitude,
      double accuracy,
      double speed,
      long time,
      long elapsed,
      int segment,
      boolean accepted) {
    this.lat = lat;
    this.lon = lon;
    this.altitude = altitude;
    this.accuracy = accuracy;
    this.speed = speed;
    this.time = time;
    this.elapsed = elapsed;
    this.segment = segment;
    this.accepted = accepted;
  }
}
