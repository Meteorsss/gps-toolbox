package com.meteorsss.startrace;

import java.util.*;

/** A single snapshot: count satellites separately from Android status entries. */
public final class SatelliteStats {
  public static class Signal {
    public int system, id;
    public float azimuth, elevation, cn0, frequency;
    public boolean used, hasFrequency;
    public String key() { return system + ":" + id; }
  }
  public static final class Counts {
    private final Set<String> reportedIds = new HashSet<>();
    private final Set<String> usedIds = new HashSet<>();
    private final Set<String> trackedIds = new HashSet<>();
    private int entries, usedEntries;
    private void add(Signal s) { entries++;reportedIds.add(s.key());if(s.used){usedIds.add(s.key());usedEntries++;}if(Float.isFinite(s.cn0)&&s.cn0>0)trackedIds.add(s.key()); }
    public int received() { return reportedIds.size(); }
    public int used() { return usedIds.size(); }
    public int tracked() { return trackedIds.size(); }
    public int entries() { return entries; }
    public int usedEntries() { return usedEntries; }
  }
  public static final class Snapshot {
    public final Counts total = new Counts();
    private final Map<Integer,Counts> systems = new HashMap<>();
    public Counts system(int type) { return systems.getOrDefault(type,new Counts()); }
  }
  public static Snapshot of(List<? extends Signal> signals) {
    Snapshot result=new Snapshot();
    for(Signal signal:signals){result.total.add(signal);result.systems.computeIfAbsent(signal.system,s->new Counts()).add(signal);}
    return result;
  }
  private SatelliteStats() {}
}
