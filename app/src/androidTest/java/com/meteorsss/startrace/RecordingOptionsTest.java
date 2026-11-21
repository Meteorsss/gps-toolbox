package com.meteorsss.startrace;

import static org.junit.Assert.*;

import android.app.UiModeManager;
import android.content.*;
import android.location.Location;
import android.os.*;
import android.view.Gravity;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RecordingOptionsTest {
  private App app() {
    return (App)
        InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
  }

  private void shell(String command) throws Exception {
    ParcelFileDescriptor fd =
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
    try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
      while (in.read() != -1) {}
    }
  }

  private void shot(String name) throws Exception {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    android.graphics.Bitmap bitmap =
        InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    if (bitmap == null) return;
    File folder = new File(app().getExternalFilesDir(null), "screenshots");
    folder.mkdirs();
    try (OutputStream out = new FileOutputStream(new File(folder, name + ".png"))) {
      bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    } finally {
      bitmap.recycle();
    }
  }

  private void awaitTheme(ActivityScenario<MainActivity> scenario, boolean light) {
    long end = SystemClock.elapsedRealtime() + 15000;
    boolean[] matches = {false};
    while (SystemClock.elapsedRealtime() < end) {
      scenario.onActivity(a -> matches[0] = a.u.light == light
          && ((a.getWindow().getInsetsController().getSystemBarsAppearance()
              & android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS) != 0) == light);
      if (matches[0]) return;
      SystemClock.sleep(100);
    }
    fail("system appearance did not update");
  }

  private Location fix(long elapsed, double latitude) {
    Location l = new Location("gps");
    l.setLatitude(latitude);
    l.setLongitude(121);
    l.setTime(1700000000000L + elapsed);
    l.setElapsedRealtimeNanos(elapsed * 1000000L);
    l.setAccuracy(3);
    l.setSpeed(2);
    l.setSpeedAccuracyMetersPerSecond(.1f);
    return l;
  }

  @Test
  public void serviceActuallyThrottlesChangesAndPreservesRawBreaks() {
    App app = app();
    long id = app.store.begin("Synthetic interval test");
    TrackService previous = TrackService.active;
    int saved = Preferences.interval(app);
    TrackService live = new TrackService();
    try {
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(() -> {
            live.setIntervalSeconds(3);
            live.prepareForTests(app, id);
            TrackService.active = live;
            long base = SystemClock.elapsedRealtime() - 12000;
            for (int i = 0; i <= 6; i++) live.fixForTests(fix(base + i * 1000, 31 + i * .00001));
            assertEquals(3, app.store.track(id).raw);
            assertEquals(3, app.store.track(id).points);
            assertEquals(base + 6000, app.store.preview(id, 100).get(2).elapsed);
            Preferences.interval(app, 1);
            live.setIntervalSeconds(Preferences.interval(app));
            live.fixForTests(fix(base + 7000, 31.00007));
            assertEquals(4, app.store.track(id).raw);
            assertNotEquals(
                app.store.preview(id, 100).get(2).segment,
                app.store.preview(id, 100).get(3).segment);
            assertEquals(1, Preferences.interval(app));
            // A rejected callback between two saved timestamps must still prevent a bridging line.
            live.setIntervalSeconds(3);
            live.fixForTests(fix(base + 8000, 31.00008));
            Location poor = fix(base + 9000, 31.00009);
            poor.setAccuracy(1000);
            live.fixForTests(poor);
            live.fixForTests(fix(base + 11000, 31.00011));
            assertEquals(6, app.store.track(id).raw);
            java.util.List<TrackPoint> points = app.store.preview(id, 100);
            assertNotEquals(points.get(4).segment, points.get(5).segment);
            long count = app.store.track(id).raw;
            live.paused = true;
            live.fixForTests(fix(base + 12000, 31.00012));
            assertEquals(count, app.store.track(id).raw);
          });
    } finally {
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(() -> {
            TrackService.active = previous;
            Preferences.interval(app, saved);
          });
      app.store.delete(id);
    }
  }

  @Test
  public void largeSpeedStatisticsMatchLiveTrackAndClearWithoutRecorder() throws Exception {
    App app = app();
    long id = app.store.begin("Synthetic large speed test");
    TrackService previous = TrackService.active;
    TrackService live = new TrackService();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(a -> {
        live.prepareForTests(app, id);
        TrackService.active = live;
        long base = SystemClock.elapsedRealtime() - 6000;
        for (int i = 0; i < 4; i++) live.fixForTests(fix(base + 1000 * i, 31 + i * .0001));
        live.distance = 1234;
        live.paused = true;
        a.navigate("speed");
        TextView stats = a.body.findViewWithTag("large-speed-recording-stats");
        TextView current = a.body.findViewWithTag("large-speed-current");
        assertNotNull(stats);
        assertNotNull(current);
        assertEquals(Gravity.CENTER, current.getGravity());
        assertTrue(stats.getText().toString().contains("最高速度  7.2 km/h"));
        assertTrue(stats.getText().toString().contains("累计里程  " + Geo.meters(1234)));
        assertTrue(stats.getText().toString().contains("平均速度  74.0 km/h"));
        assertTrue(stats.getText().toString().contains("已暂停"));
        live.distance = 2345;
        a.refresh();
        assertTrue(stats.getText().toString().contains(Geo.meters(2345)));
        TrackService.active = null;
        a.refresh();
        assertTrue(stats.getText().toString().contains("未开始记录"));
        assertFalse(stats.getText().toString().contains(Geo.meters(2345)));
        TrackService.active = live;
        a.refresh();
      });
      shot("large-speed-live-statistics");
    } finally {
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(() -> TrackService.active = previous);
      app.store.delete(id);
    }
  }

  @Test
  public void threeThemeChoicesPersistAndSystemUpdatesAutomatically() throws Exception {
    App app = app();
    ThemeMode saved = Preferences.theme(app);
    int originalNight = app.getSystemService(UiModeManager.class).getNightMode();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      for (ThemeMode mode : ThemeMode.values()) {
        scenario.onActivity(a -> {
          Preferences.theme(a, mode);
          a.buildRoot();
          a.navigate("settings");
          assertTrue(a.visibleText().contains("色彩模式：" + mode.label));
          assertEquals(mode, Preferences.theme(a));
          assertEquals(Preferences.light(a), a.u.light);
        });
        scenario.recreate();
        scenario.onActivity(a -> {
          assertEquals(mode, Preferences.theme(a));
          assertEquals(Preferences.light(a), a.u.light);
        });
      }
      shell("cmd uimode night yes");
      awaitTheme(scenario, false);
      shell("cmd uimode night no");
      awaitTheme(scenario, true);
      scenario.onActivity(a -> {
        Preferences.theme(a, ThemeMode.DARK);
        a.buildRoot();
      });
      shell("cmd uimode night yes");
      awaitTheme(scenario, false);
      shell("cmd uimode night no");
      awaitTheme(scenario, false);
      scenario.onActivity(a -> {
        Preferences.theme(a, ThemeMode.LIGHT);
        a.buildRoot();
      });
      shell("cmd uimode night yes");
      awaitTheme(scenario, true);
      scenario.onActivity(a -> a.navigate("settings"));
      shot("three-mode-settings");
    } finally {
      String night =
          originalNight == UiModeManager.MODE_NIGHT_YES
              ? "yes"
              : originalNight == UiModeManager.MODE_NIGHT_NO ? "no" : "auto";
      shell("cmd uimode night " + night);
      Preferences.theme(app, saved);
    }
  }

  @Test
  public void mapUsesTheSameManualAndSystemThemePreference() {
    App app = app();
    ThemeMode saved = Preferences.theme(app);
    try {
      for (ThemeMode mode : ThemeMode.values()) {
        Preferences.theme(app, mode);
        try (ActivityScenario<MapActivity> scenario =
            ActivityScenario.launch(
                new Intent(app, MapActivity.class).putExtra("test_local_style", true))) {
          scenario.onActivity(a -> assertEquals(Preferences.light(a), a.lightForTests()));
        }
      }
    } finally {
      Preferences.theme(app, saved);
    }
  }
}
