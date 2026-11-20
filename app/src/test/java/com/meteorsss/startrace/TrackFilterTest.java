package com.meteorsss.startrace;
import org.junit.Test;
import static org.junit.Assert.*;
public class TrackFilterTest {
    private TrackPoint p(double lat,double lon,double acc,double speed,long elapsed,int segment){return new TrackPoint(lat,lon,10,acc,speed,1700000000000L+elapsed,elapsed,segment,true);}
    @Test public void rejectsStaleAndPoorAccuracy(){assertFalse(TrackFilter.usable(p(0,0,5,0,1000,0),20000));assertFalse(TrackFilter.usable(p(0,0,90,0,1000,0),1000));assertFalse(TrackFilter.usable(p(0,0,Double.NaN,0,1000,0),1000));}
    @Test public void rejectsFutureLocations(){assertFalse(TrackFilter.usable(p(0,0,5,0,20000,0),1000));}
    @Test public void acceptsFreshPoint(){assertTrue(TrackFilter.usable(p(0,0,5,2,1000,0),2000));}
    @Test public void rejectsJumpAndDuplicateTimes(){TrackPoint a=p(0,0,5,0,1000,0);assertFalse(TrackFilter.plausible(a,p(1,1,5,0,2000,0)));assertFalse(TrackFilter.plausible(a,p(0,0,5,0,1000,0)));}
    @Test public void acceptsWalking(){assertTrue(TrackFilter.plausible(p(0,0,5,1,1000,0),p(.00002,0,5,1,3000,0)));}
    @Test public void neverBridgesSegmentsOrOutages(){TrackPoint a=p(0,0,5,1,1000,0);assertEquals(0,TrackFilter.distanceContribution(a,p(.001,0,5,1,5000,1)),0);assertEquals(0,TrackFilter.distanceContribution(a,p(.001,0,5,1,40000,0)),0);}
    @Test public void suppressesStationaryJitter(){assertEquals(0,TrackFilter.distanceContribution(p(0,0,5,0,1000,0),p(.00002,0,5,0,2000,0)),0);}
    @Test public void distanceAccumulatesWhenMoving(){assertTrue(TrackFilter.distanceContribution(p(0,0,5,2,1000,0),p(.0001,0,5,2,6000,0))>10);}
}