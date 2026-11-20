package com.meteorsss.startrace;

import android.app.Application;
import android.os.*;
import java.util.concurrent.*;

public final class App extends Application {
  public Store store;
  public GnssEngine engine;
  public final ExecutorService io = Executors.newSingleThreadExecutor();
  public final Handler main = new Handler(Looper.getMainLooper());

  @Override
  public void onCreate() {
    super.onCreate();
    store = new Store(this);
    store.recoverInterrupted();
    engine = new GnssEngine(this);
  }
}
