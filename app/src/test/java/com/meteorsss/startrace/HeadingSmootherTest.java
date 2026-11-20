package com.meteorsss.startrace;
import org.junit.Test;
import static org.junit.Assert.*;
public class HeadingSmootherTest {
 @Test public void crossesNorthByShortestArc(){HeadingSmoother s=new HeadingSmoother();s.update(359,1_000_000_000L);double h=s.update(1,1_016_000_000L);assertTrue(h>359||h<1);assertEquals(2,HeadingSmoother.delta(359,1),1e-8);}
 @Test public void reverseCrossingNeverSpins(){HeadingSmoother s=new HeadingSmoother();s.update(1,1_000_000_000L);double h=s.update(359,1_016_000_000L);assertTrue(h>=0&&h<1);assertEquals(-2,HeadingSmoother.delta(1,359),1e-8);}
 @Test public void frameRateIndependent(){HeadingSmoother a=new HeadingSmoother(),b=new HeadingSmoother();a.update(0,1_000_000_000L);b.update(0,1_000_000_000L);double av=0,bv=0;for(int i=1;i<=6;i++)av=a.update(90,1_000_000_000L+i*16_000_000L);for(int i=1;i<=12;i++)bv=b.update(90,1_000_000_000L+i*8_000_000L);assertEquals(av,bv,1e-8);assertTrue(av>75);}
 @Test public void firstFrameSnapsWithoutStartupSpin(){assertEquals(270,new HeadingSmoother().update(270,1_000_000_000L),1e-8);}
 @Test public void pauseResets(){HeadingSmoother s=new HeadingSmoother();s.update(10,1_000_000_000L);s.reset();assertEquals(240,s.update(240,2_000_000_000L),1e-8);}
 @Test public void longGapDoesNotAnimateStaleHeading(){HeadingSmoother s=new HeadingSmoother();s.update(10,1_000_000_000L);assertEquals(240,s.update(240,4_000_000_000L),1e-8);}
 @Test public void unknownHeadingRemainsUnknown(){HeadingSmoother s=new HeadingSmoother();assertTrue(Double.isNaN(s.update(Double.NaN,1)));assertEquals(90,s.update(90,100),1e-8);}
}
