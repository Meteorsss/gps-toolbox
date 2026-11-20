package com.meteorsss.startrace;

import static org.junit.Assert.*;

import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppSmokeTest {
  private void shell(String command) throws Exception {
    try (ParcelFileDescriptor p =
            InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .executeShellCommand(command);
        InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(p)) {
      while (in.read() != -1) {}
    }
  }

  private void screenshot(String name) throws Exception {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    SystemClock.sleep(700);
    Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    if (image != null) {
      File dir =
          new File(
              InstrumentationRegistry.getInstrumentation()
                  .getTargetContext()
                  .getExternalFilesDir(null),
              "screenshots");
      dir.mkdirs();
      try (OutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
        image.compress(Bitmap.CompressFormat.PNG, 100, out);
      }
      image.recycle();
    }
  }

  @Test
  public void allPagesAndBothThemesRender() throws Exception {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      String[] expected = {"此刻，在哪里", "头顶的卫星", "把走过的路留下", "随手可用"};
      for (int i = 0; i < 4; i++) {
        final int page = i;
        scenario.onActivity(a -> {
          a.goPage(page);
          assertTrue(a.visibleText().contains(expected[page]));
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        screenshot("dark-" + i);
      }
      scenario.onActivity(a -> {
        a.goPage(3);
        a.navigate("compass");
      });
      shell("input keyevent 4");
      SystemClock.sleep(300);
      scenario.onActivity(a -> {
        assertEquals("", a.mode);
        assertTrue(a.visibleText().contains("随手可用"));
      });
      String[] modes = {"settings", "places", "compass", "speed", "diagnostics"};
      for (String mode : modes) {
        scenario.onActivity(a -> a.navigate(mode));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        screenshot(mode);
      }
      scenario.onActivity(a -> {
        a.goPage(0);
        a.theme();
        assertTrue(a.u.light);
      });
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      screenshot("light-home");
      scenario.recreate();
      scenario.onActivity(a -> {
        assertTrue(a.u.light);
        a.theme();
      });
    }
  }

  @Test
  public void storageExportAndRecoveryPreserveData() throws Exception {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Store db = ((App) context.getApplicationContext()).store;
    long id = db.begin("A&B 测试");
    try {
      db.add(id, new TrackPoint(31, 121, 42, 5, 1, 1700000000000L, 1000, 0, true), false);
      db.add(
          id,
          new TrackPoint(31.1, 121.1, Double.NaN, 90, Double.NaN, 1700000001000L, 2000, 1, false),
          true);
      db.add(id, new TrackPoint(31.2, 121.2, 43, 4, 2, 1700000040000L, 41000, 2, true), false);
      db.update(id, 1000, 5, "recording");
      db.recoverInterrupted();
      assertEquals("interrupted", db.track(id).status);
      assertEquals(2, db.track(id).points);
      ByteArrayOutputStream gpx = new ByteArrayOutputStream();
      db.export(id, gpx, false);
      String text = gpx.toString("UTF-8");
      assertTrue(text.contains("A&amp;B"));
      assertEquals(2, text.split("<trkseg>", -1).length - 1);
      assertFalse(text.contains("31.10000000"));
      ByteArrayOutputStream csv = new ByteArrayOutputStream();
      db.export(id, csv, true);
      assertEquals(5, csv.toString("UTF-8").split("\n", -1).length);
      long place = db.savePlace("Test", "note", 31, 121);
      assertTrue(db.places().stream().anyMatch(p -> p.id == place));
      db.deletePlace(place);
    } finally {
      db.delete(id);
    }
    assertNull(db.track(id));
  }

  @Test
  public void foregroundRecordingPausesResumesAndStops() throws Exception {
    String pkg = InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
    shell("pm grant " + pkg + " android.permission.ACCESS_COARSE_LOCATION");
    shell("pm grant " + pkg + " android.permission.ACCESS_FINE_LOCATION");
    shell("pm grant " + pkg + " android.permission.POST_NOTIFICATIONS");
    shell("cmd location set-location-enabled true");
    long[] id = {-1};
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> a.startForegroundService(new Intent(a, TrackService.class).setAction("START")));
      SystemClock.sleep(1500);
      scenario.onActivity(a -> {
        assertNotNull(TrackService.active);
        id[0] = TrackService.active.trackId;
        assertTrue(id[0] > 0);
        a.startService(new Intent(a, TrackService.class).setAction("PAUSE"));
      });
      SystemClock.sleep(500);
      scenario.onActivity(a -> {
        assertTrue(TrackService.active.paused);
        a.startService(new Intent(a, TrackService.class).setAction("RESUME"));
      });
      SystemClock.sleep(500);
      scenario.onActivity(a -> {
        assertFalse(TrackService.active.paused);
        a.startService(new Intent(a, TrackService.class).setAction("STOP"));
      });
      SystemClock.sleep(500);
      scenario.onActivity(a -> {
        assertNull(TrackService.active);
        assertEquals("finished", a.app.store.track(id[0]).status);
        a.app.store.delete(id[0]);
      });
    } finally {
      Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
      c.stopService(new Intent(c, TrackService.class));
      if (id[0] > 0) ((App) c.getApplicationContext()).store.delete(id[0]);
    }
  }

  private void grantLocation() throws Exception {
    String pkg = InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
    shell("pm grant " + pkg + " android.permission.ACCESS_COARSE_LOCATION");
    shell("pm grant " + pkg + " android.permission.ACCESS_FINE_LOCATION");
    shell("cmd location set-location-enabled true");
  }

  private <T extends android.view.View> T find(android.view.View view, Class<T> type) {
    if (type.isInstance(view)) return type.cast(view);
    if (view instanceof android.view.ViewGroup) {
      android.view.ViewGroup group = (android.view.ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        T child = find(group.getChildAt(i), type);
        if (child != null) return child;
      }
    }
    return null;
  }

  private android.widget.EditText field(android.view.View view, String description) {
    if (view instanceof android.widget.EditText
        && description.contentEquals(
            view.getContentDescription() == null ? "" : view.getContentDescription()))
      return (android.widget.EditText) view;
    if (view instanceof android.view.ViewGroup) {
      android.view.ViewGroup group = (android.view.ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        android.widget.EditText found = field(group.getChildAt(i), description);
        if (found != null) return found;
      }
    }
    return null;
  }

  @Test
  public void compassFramesAreIndependentAndStopWhenHidden() throws Exception {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      CompassView[] view = {null};
      long[] before = {0}, after = {0};
      scenario.onActivity(a -> {
        a.navigate("compass");
        view[0] = find(a.body, CompassView.class);
        assertNotNull(view[0]);
        assertNotEquals(android.view.View.LAYER_TYPE_SOFTWARE, view[0].getLayerType());
      });
      SystemClock.sleep(300);
      scenario.onActivity(a -> {
        assertTrue(view[0].hasScheduledFrame());
        before[0] = view[0].renderedFrames();
      });
      SystemClock.sleep(1200);
      scenario.onActivity(a -> {
        after[0] = view[0].renderedFrames();
        assertTrue(
            "Compass must redraw independently of 2Hz page tick", after[0] - before[0] >= 12);
      });
      System.out.println("COMPASS_DRAW_FRAMES_IN_1200MS=" + (after[0] - before[0]));
      scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(() -> {
            assertFalse(view[0].hasScheduledFrame());
            assertFalse(
                ((App)
                        InstrumentationRegistry.getInstrumentation()
                            .getTargetContext()
                            .getApplicationContext())
                    .engine.compassSubscribed());
            before[0] = view[0].renderedFrames();
          });
      SystemClock.sleep(300);
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(() -> assertEquals(before[0], view[0].renderedFrames()));
      scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
      scenario.onActivity(a -> {
        assertTrue(view[0].hasScheduledFrame());
        a.goPage(0);
        assertFalse(view[0].hasScheduledFrame());
        assertFalse(a.e.compassSubscribed());
      });
    }
  }

  @Test
  public void dashboardCoordinateSystemsAndSatelliteBreakdown() throws Exception {
    grantLocation();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(a -> {
        android.location.Location l = new android.location.Location("gps");
        l.setLatitude(39.911954);
        l.setLongitude(116.377817);
        l.setAccuracy(4);
        l.setSpeed(10);
        l.setTime(System.currentTimeMillis());
        l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        a.e.onLocationChanged(l);
        a.goPage(0);
        assertTrue(a.visibleText().indexOf("当前速度") < a.visibleText().indexOf("水平精度估计"));
        assertTrue(a.visibleText().contains("36.0"));
        a.setCoordinateSystem(false);
        assertTrue(a.visibleText().contains("39.9119540"));
        a.setCoordinateSystem(true);
        assertTrue(a.visibleText().contains("39.9133455"));
        assertEquals(39.911954, a.e.location.getLatitude(), 0);
        a.copyHere();
        android.content.ClipboardManager cb =
            a.getSystemService(android.content.ClipboardManager.class);
        assertTrue(cb.getPrimaryClip().getItemAt(0).getText().toString().contains("GCJ-02"));
      });
      screenshot("gcj02-dashboard");
      scenario.recreate();
      scenario.onActivity(a -> {
        assertTrue(a.gcj());
        a.setCoordinateSystem(false);
        java.util.List<GnssEngine.Satellite> entries = new java.util.ArrayList<>();
        for (int i = 1; i <= 30; i++) {
          GnssEngine.Satellite s = new GnssEngine.Satellite();
          s.system = i <= 20 ? 5 : 1;
          s.id = i;
          s.cn0 = 45 - i;
          s.used = i % 2 == 0;
          s.azimuth = i * 12;
          s.elevation = 45;
          entries.add(s);
        }
        GnssEngine.Satellite second = new GnssEngine.Satellite();
        second.system = 5;
        second.id = 1;
        second.cn0 = 20;
        second.used = true;
        entries.add(second);
        a.e.acceptSatelliteSnapshot(entries);
        a.goPage(1);
        assertEquals(30, a.e.uniqueCount(false));
        assertEquals(16, a.e.uniqueCount(true));
        assertTrue(a.visibleText().contains("20 / 11"));
        assertTrue(a.visibleText().contains("10 / 5"));
        assertTrue(a.visibleText().contains("31 条信号记录"));
        assertTrue(a.visibleText().contains("全部信号明细"));
      });
      screenshot("satellite-breakdown-fixture");
      scenario.onActivity(a -> a.e.acceptSatelliteSnapshot(java.util.Collections.emptyList()));
    }
  }

  @Test
  public void editPlacePreservesCoordinatesAndPersists() throws Exception {
    final long[] id = {-1};
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(a -> {
        id[0] = a.app.store.savePlace("旧名称", "旧备注", 31.23, 121.47);
        Store.Place p = a.app.store.places().stream().filter(x -> x.id == id[0]).findFirst().get();
        long created = p.time;
        a.navigate("places");
        android.app.AlertDialog d = a.editPlaceDialog(p);
        android.widget.EditText name = field(d.getWindow().getDecorView(), "地点名称"),
            note = field(d.getWindow().getDecorView(), "地点备注");
        assertNotNull(name);
        assertNotNull(note);
        name.setText("未保存");
        d.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();
        assertEquals(
            "旧名称", a.app.store.places().stream().filter(x -> x.id == id[0]).findFirst().get().name);
        d = a.editPlaceDialog(p);
        name = field(d.getWindow().getDecorView(), "地点名称");
        note = field(d.getWindow().getDecorView(), "地点备注");
        name.setText("   ");
        d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
        assertTrue(d.isShowing());
        name.setText(" 停车点 ");
        note.setText("地下二层\n靠近电梯");
        d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
        assertFalse(d.isShowing());
        Store.Place saved =
            a.app.store.places().stream().filter(x -> x.id == id[0]).findFirst().get();
        assertEquals("停车点", saved.name);
        assertEquals("地下二层\n靠近电梯", saved.note);
        assertEquals(p.lat, saved.lat, 0);
        assertEquals(p.lon, saved.lon, 0);
        assertEquals(created, saved.time);
        try (Store reopened = new Store(a)) {
          assertEquals(
              "停车点", reopened.places().stream().filter(x -> x.id == id[0]).findFirst().get().name);
        }
        assertTrue(a.visibleText().contains("停车点"));
      });
      screenshot("edited-place");
      scenario.recreate();
      scenario.onActivity(a -> {
        Store.Place saved =
            a.app.store.places().stream().filter(x -> x.id == id[0]).findFirst().get();
        a.app.store.updatePlace(saved.id, saved.name, "");
        assertEquals(
            "", a.app.store.places().stream().filter(x -> x.id == id[0]).findFirst().get().note);
        a.app.store.deletePlace(id[0]);
      });
    } finally {
      if (id[0] > 0)
        ((App)
                InstrumentationRegistry.getInstrumentation()
                    .getTargetContext()
                    .getApplicationContext())
            .store.deletePlace(id[0]);
    }
  }
}
