package com.meteorsss.startrace;

/**
 * Offline approximate WGS84 to GCJ-02 conversion, adapted from eviltransform. See
 * assets/third-party/eviltransform-LICENSE.txt for BSD-2-Clause attribution. The rectangular
 * applicability check is NOT a national or administrative boundary.
 */
public final class Gcj02 {
  private static final double EARTH_R = 6378137.0;
  private static final double EE = 0.00669342162296594323;

  private Gcj02() {}

  public static final class Coordinate {
    public final double latitude, longitude;
    public final boolean converted;

    private Coordinate(double latitude, double longitude, boolean converted) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.converted = converted;
    }
  }

  public static boolean inApproximateCoverage(double latitude, double longitude) {
    return Geo.valid(latitude, longitude)
        && longitude >= 72.004
        && longitude <= 137.8347
        && latitude >= 0.8293
        && latitude <= 55.8271;
  }

  public static Coordinate fromWgs84(double latitude, double longitude) {
    if (!Geo.valid(latitude, longitude)) throw new IllegalArgumentException("WGS84 坐标超出范围");
    if (!inApproximateCoverage(latitude, longitude))
      return new Coordinate(latitude, longitude, false);
    double x = longitude - 105,
        y = latitude - 35,
        xy = x * y,
        absX = Math.sqrt(Math.abs(x)),
        xp = x * Math.PI,
        yp = y * Math.PI;
    double common = 20 * Math.sin(6 * xp) + 20 * Math.sin(2 * xp);
    double dlat =
        (common
                + 20 * Math.sin(yp)
                + 40 * Math.sin(yp / 3)
                + 160 * Math.sin(yp / 12)
                + 320 * Math.sin(yp / 30))
            * 2
            / 3;
    double dlon =
        (common
                + 20 * Math.sin(xp)
                + 40 * Math.sin(xp / 3)
                + 150 * Math.sin(xp / 12)
                + 300 * Math.sin(xp / 30))
            * 2
            / 3;
    dlat += -100 + 2 * x + 3 * y + 0.2 * y * y + 0.1 * xy + 0.2 * absX;
    dlon += 300 + x + 2 * y + 0.1 * x * x + 0.1 * xy + 0.1 * absX;
    double rad = Math.toRadians(latitude),
        sin = Math.sin(rad),
        magic = 1 - EE * sin * sin,
        sqrt = Math.sqrt(magic);
    dlat = dlat * 180 / ((EARTH_R * (1 - EE)) / (magic * sqrt) * Math.PI);
    dlon = dlon * 180 / (EARTH_R / sqrt * Math.cos(rad) * Math.PI);
    return new Coordinate(latitude + dlat, longitude + dlon, true);
  }
}
