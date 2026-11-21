package com.meteorsss.startrace;

import android.content.*;
import android.content.res.Configuration;

/** Shared by MainActivity, map and recording service; retains existing preference keys. */
public final class Preferences {
  private Preferences() {}
  private static SharedPreferences settings(Context c) {
    return c.getSharedPreferences("MainActivity", Context.MODE_PRIVATE);
  }
  public static ThemeMode theme(Context c) {
    SharedPreferences p = settings(c);
    return ThemeMode.restore(p.getString("theme-mode", null), p.contains("light"), p.getBoolean("light", false));
  }
  public static boolean light(Context c) {
    boolean dark = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
        == Configuration.UI_MODE_NIGHT_YES;
    return theme(c).isLight(dark);
  }
  public static void theme(Context c, ThemeMode mode) {
    settings(c).edit().putString("theme-mode", mode.name()).apply();
  }
  public static int interval(Context c) {
    return RecordingCadence.sanitize(settings(c).getInt("record-interval-seconds", 1));
  }
  public static void interval(Context c, int seconds) {
    settings(c).edit().putInt("record-interval-seconds", RecordingCadence.sanitize(seconds)).apply();
  }
}
