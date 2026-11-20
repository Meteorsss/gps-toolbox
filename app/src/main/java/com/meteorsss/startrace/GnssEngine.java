package com.meteorsss.startrace;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.location.*;
import android.os.*;
import android.view.*;
import java.util.*;

public final class GnssEngine implements LocationListener, SensorEventListener {
  public static final class Satellite {
    public int system, id;
    public float azimuth, elevation, cn0, frequency;
    public boolean used, hasFrequency;

    public String key() {
      return system + ":" + id;
    }

    public String label() {
      return systemName(system) + " " + id;
    }
  }

  public interface Observer {
    void changed();
  }

  public interface FixListener {
    void fix(Location location);
  }

  private final App app;
  private final LocationManager manager;
  private final SensorManager sensors;
  private final Set<Observer> observers = new HashSet<>();
  private boolean visible, recording, registered, sensorRegistered;
  public Location location;
  public List<Satellite> satellites = new ArrayList<>();
  public long started, firstFix = -1, satelliteAt;
  public String problem = "";
  public float magneticHeading = Float.NaN;
  public int sensorAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
  public final boolean hasCompass;
  public FixListener recorder;

  public GnssEngine(App app) {
    this.app = app;
    manager = app.getSystemService(LocationManager.class);
    sensors = app.getSystemService(SensorManager.class);
    hasCompass = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null;
  }

  public boolean precise() {
    return app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED;
  }

  public boolean gpsEnabled() {
    try {
      return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    } catch (Exception e) {
      return false;
    }
  }

  public boolean enabled() {
    return manager.isLocationEnabled();
  }

  public boolean fresh() {
    return precise()
        && gpsEnabled()
        && location != null
        && SystemClock.elapsedRealtime() - location.getElapsedRealtimeNanos() / 1000000L >= 0
        && SystemClock.elapsedRealtime() - location.getElapsedRealtimeNanos() / 1000000L < 15000;
  }

  public long age() {
    return location == null
        ? Long.MAX_VALUE
        : Math.max(
            0, SystemClock.elapsedRealtime() - location.getElapsedRealtimeNanos() / 1000000L);
  }

  public void observe(Observer o) {
    observers.add(o);
  }

  public void unobserve(Observer o) {
    observers.remove(o);
  }

  private void notifyChanged() {
    for (Observer o : new ArrayList<>(observers)) o.changed();
  }

  public void setVisible(boolean on) {
    visible = on;
    reconcile();
    if (on && hasCompass && !sensorRegistered) {
      sensorRegistered =
          sensors.registerListener(
              this,
              sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
              SensorManager.SENSOR_DELAY_UI);
    } else if (!on && sensorRegistered) {
      sensors.unregisterListener(this);
      sensorRegistered = false;
    }
  }

  public void setRecording(boolean on) {
    recording = on;
    reconcile();
  }

  public void reconcile() {
    boolean wanted = (visible || recording) && precise();
    if (wanted && !registered) {
      started = SystemClock.elapsedRealtime();
      firstFix = -1;
      location = null;
      problem = "";
      try {
        manager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 1000, 0, this, Looper.getMainLooper());
        manager.registerGnssStatusCallback(app.getMainExecutor(), callback);
        registered = true;
      } catch (SecurityException | IllegalArgumentException e) {
        manager.removeUpdates(this);
        manager.unregisterGnssStatusCallback(callback);
        problem = "无法请求卫星定位：请检查精确位置权限与系统定位";
      }
    } else if (!wanted && registered) {
      manager.removeUpdates(this);
      manager.unregisterGnssStatusCallback(callback);
      registered = false;
      satellites = new ArrayList<>();
      satelliteAt = 0;
    }
    notifyChanged();
  }

  private final GnssStatus.Callback callback =
      new GnssStatus.Callback() {
        @Override
        public void onFirstFix(int ttff) {
          firstFix = ttff;
          notifyChanged();
        }

        @Override
        public void onStopped() {
          satellites = new ArrayList<>();
          satelliteAt = 0;
          notifyChanged();
        }

        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
          List<Satellite> list = new ArrayList<>();
          for (int i = 0; i < status.getSatelliteCount(); i++) {
            Satellite s = new Satellite();
            s.system = status.getConstellationType(i);
            s.id = status.getSvid(i);
            s.azimuth = status.getAzimuthDegrees(i);
            s.elevation = status.getElevationDegrees(i);
            s.cn0 = status.getCn0DbHz(i);
            s.used = status.usedInFix(i);
            s.hasFrequency = status.hasCarrierFrequencyHz(i);
            if (s.hasFrequency) s.frequency = status.getCarrierFrequencyHz(i);
            list.add(s);
          }
          list.sort((a, b) -> Float.compare(b.cn0, a.cn0));
          satellites = list;
          satelliteAt = SystemClock.elapsedRealtime();
          notifyChanged();
        }
      };

  public List<Satellite> currentSatellites() {
    return SystemClock.elapsedRealtime() - satelliteAt > 15000
        ? Collections.emptyList()
        : satellites;
  }

  public int uniqueCount(boolean used) {
    Set<String> ids = new HashSet<>();
    for (Satellite s : currentSatellites()) if (!used || s.used) ids.add(s.key());
    return ids.size();
  }

  @Override
  public void onLocationChanged(Location fix) {
    if (!Geo.valid(fix.getLatitude(), fix.getLongitude())) return;
    location = new Location(fix);
    if (firstFix < 0 && fresh()) firstFix = SystemClock.elapsedRealtime() - started;
    if (recorder != null) recorder.fix(new Location(fix));
    notifyChanged();
  }

  @Override
  public void onProviderEnabled(String provider) {
    problem = "";
    notifyChanged();
  }

  @Override
  public void onProviderDisabled(String provider) {
    location = null;
    satellites = new ArrayList<>();
    problem = "系统卫星定位已关闭";
    notifyChanged();
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    float[] r = new float[9], adjusted = new float[9], orientation = new float[3];
    SensorManager.getRotationMatrixFromVector(r, event.values);
    int rotation = app.getSystemService(WindowManager.class).getDefaultDisplay().getRotation();
    int x = SensorManager.AXIS_X, y = SensorManager.AXIS_Y;
    if (rotation == Surface.ROTATION_90) {
      x = SensorManager.AXIS_Y;
      y = SensorManager.AXIS_MINUS_X;
    } else if (rotation == Surface.ROTATION_180) {
      x = SensorManager.AXIS_MINUS_X;
      y = SensorManager.AXIS_MINUS_Y;
    } else if (rotation == Surface.ROTATION_270) {
      x = SensorManager.AXIS_MINUS_Y;
      y = SensorManager.AXIS_X;
    }
    SensorManager.remapCoordinateSystem(r, x, y, adjusted);
    SensorManager.getOrientation(adjusted, orientation);
    float next = (float) Geo.normalize(Math.toDegrees(orientation[0]));
    if (Float.isNaN(magneticHeading)) magneticHeading = next;
    else {
      float delta = (float) ((next - magneticHeading + 540) % 360 - 180);
      magneticHeading = (float) Geo.normalize(magneticHeading + delta * 0.2);
    }
    sensorAccuracy = event.accuracy;
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    sensorAccuracy = accuracy;
  }

  public float trueHeading() {
    if (Float.isNaN(magneticHeading) || !fresh()) return Float.NaN;
    GeomagneticField field =
        new GeomagneticField(
            (float) location.getLatitude(),
            (float) location.getLongitude(),
            location.hasAltitude() ? (float) location.getAltitude() : 0,
            location.getTime());
    return (float) Geo.normalize(magneticHeading + field.getDeclination());
  }

  public static String systemName(int system) {
    switch (system) {
      case GnssStatus.CONSTELLATION_GPS:
        return "GPS";
      case GnssStatus.CONSTELLATION_BEIDOU:
        return "北斗";
      case GnssStatus.CONSTELLATION_GALILEO:
        return "Galileo";
      case GnssStatus.CONSTELLATION_GLONASS:
        return "GLONASS";
      case GnssStatus.CONSTELLATION_QZSS:
        return "QZSS";
      case GnssStatus.CONSTELLATION_IRNSS:
        return "NavIC";
      case GnssStatus.CONSTELLATION_SBAS:
        return "SBAS";
      default:
        return "未知";
    }
  }
}
