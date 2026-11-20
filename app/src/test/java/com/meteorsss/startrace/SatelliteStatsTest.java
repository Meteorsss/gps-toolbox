package com.meteorsss.startrace;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class SatelliteStatsTest {
  private SatelliteStats.Signal signal(int system, int id, boolean used, float cn0) {
    SatelliteStats.Signal s = new SatelliteStats.Signal();
    s.system = system;
    s.id = id;
    s.used = used;
    s.cn0 = cn0;
    return s;
  }

  @Test
  public void multiFrequencyCountsOneSatelliteAndOrsUsed() {
    SatelliteStats.Snapshot s =
        SatelliteStats.of(Arrays.asList(signal(5, 10, false, 30), signal(5, 10, true, 20)));
    assertEquals(1, s.total.received());
    assertEquals(1, s.total.used());
    assertEquals(2, s.total.entries());
    assertEquals(1, s.total.usedEntries());
  }

  @Test
  public void sameNumberInDifferentSystemsIsNotMerged() {
    SatelliteStats.Snapshot s =
        SatelliteStats.of(Arrays.asList(signal(5, 1, true, 30), signal(1, 1, false, 25)));
    assertEquals(2, s.total.received());
    assertEquals(1, s.system(5).received());
    assertEquals(1, s.system(5).used());
    assertEquals(0, s.system(1).used());
  }

  @Test
  public void includesZeroCn0InReportedButNotTracked() {
    SatelliteStats.Snapshot s =
        SatelliteStats.of(Arrays.asList(signal(1, 1, false, 0), signal(1, 2, true, 30)));
    assertEquals(2, s.total.received());
    assertEquals(1, s.total.tracked());
  }

  @Test
  public void moreThanTwentyNeverTruncated() {
    List<SatelliteStats.Signal> all = new ArrayList<>();
    for (int i = 1; i <= 50; i++) all.add(signal(i % 2 == 0 ? 5 : 1, i, i < 30, 25));
    SatelliteStats.Snapshot s = SatelliteStats.of(all);
    assertEquals(50, s.total.received());
    assertEquals(29, s.total.used());
    assertEquals(25, s.system(5).received());
    assertEquals(25, s.system(1).received());
  }

  @Test
  public void snapshotDoesNotAccumulateOldSatellites() {
    assertEquals(1, SatelliteStats.of(Arrays.asList(signal(1, 1, true, 30))).total.used());
    assertEquals(0, SatelliteStats.of(Collections.emptyList()).total.used());
  }

  @Test
  public void emptyAndUnknownAreCounted() {
    SatelliteStats.Snapshot s = SatelliteStats.of(Arrays.asList(signal(0, 10, false, Float.NaN)));
    assertEquals(1, s.system(0).received());
    assertEquals(0, s.total.tracked());
    assertEquals(0, s.system(7).received());
    assertEquals(0, s.total.used());
  }

  @Test
  public void positiveEntriesAreDifferentFromPositiveSatellites() {
    SatelliteStats.Snapshot s =
        SatelliteStats.of(
            Arrays.asList(
                signal(4, 194, true, 35),
                signal(4, 194, true, 25),
                signal(4, 195, true, 40),
                signal(4, 195, false, 0)));
    assertEquals(4, s.total.entries());
    assertEquals(3, s.total.usedEntries());
    assertEquals(2, s.total.received());
    assertEquals(2, s.total.used());
    assertEquals(3, s.total.trackedEntries());
    assertEquals(2, s.total.tracked());
  }

  @Test
  public void constellationEntrySumsMatchTotal() {
    SatelliteStats.Snapshot s =
        SatelliteStats.of(
            Arrays.asList(
                signal(5, 6, true, 30),
                signal(5, 6, true, 38),
                signal(5, 6, false, 0),
                signal(4, 194, true, 35),
                signal(4, 194, true, 25),
                signal(4, 195, true, 40),
                signal(4, 195, false, 0)));
    assertEquals(7, s.total.entries());
    assertEquals(5, s.total.usedEntries());
    assertEquals(3, s.total.received());
    assertEquals(3, s.total.used());
    assertEquals(s.total.entries(), s.system(4).entries() + s.system(5).entries());
    assertEquals(s.total.usedEntries(), s.system(4).usedEntries() + s.system(5).usedEntries());
    assertEquals(s.total.received(), s.system(4).received() + s.system(5).received());
  }

  @Test
  public void zeroSignalUsedFlagIsPreservedRatherThanGuessed() {
    SatelliteStats.Snapshot s = SatelliteStats.of(Arrays.asList(signal(1, 9, true, 0)));
    assertEquals(1, s.total.usedEntries());
    assertEquals(1, s.total.used());
    assertEquals(0, s.total.trackedEntries());
  }

  @Test
  public void missingFrequencyDoesNotDropEntries() {
    SatelliteStats.Signal a = signal(1, 10, true, 20), b = signal(1, 10, false, 15);
    a.hasFrequency = false;
    b.hasFrequency = false;
    SatelliteStats.Snapshot s = SatelliteStats.of(Arrays.asList(a, b));
    assertEquals(2, s.total.entries());
    assertEquals(1, s.total.received());
  }
}
