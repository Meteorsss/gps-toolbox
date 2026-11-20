package com.meteorsss.startrace;
import org.junit.Test;
import static org.junit.Assert.*;
public class GeoTest {
    @Test public void identicalPointsHaveZeroDistance(){assertEquals(0,Geo.distance(31,121,31,121),.0001);}
    @Test public void equatorDegreeIsAbout111km(){assertEquals(111195,Geo.distance(0,0,0,1),2);}
    @Test public void crossesDateLineByShortPath(){assertTrue(Geo.distance(0,179.9,0,-179.9)<23000);}
    @Test public void northAndEastBearings(){assertEquals(0,Geo.bearing(0,0,1,0),.001);assertEquals(90,Geo.bearing(0,0,0,1),.001);}
    @Test public void coordinatesAreRangeChecked(){assertFalse(Geo.valid(Double.NaN,0));assertFalse(Geo.valid(91,0));assertFalse(Geo.valid(0,181));assertTrue(Geo.valid(-90,-180));}
    @Test public void decimalAndDmsParsing(){assertEquals(31.5,Geo.parse("31° 30′ 00″ N",true),.000001);assertEquals(-121.25,Geo.parse("121° 15′ W",false),.000001);assertEquals(-12.5,Geo.parse("-12.5",true),.000001);}
    @Test(expected=IllegalArgumentException.class) public void rejectsInvalidMinutes(){Geo.parse("31° 61′",true);}
    @Test(expected=IllegalArgumentException.class) public void rejectsWrongHemisphere(){Geo.parse("31 E",true);}
    @Test(expected=IllegalArgumentException.class) public void rejectsGarbage(){Geo.parse("abc 31",true);}
    @Test(expected=IllegalArgumentException.class) public void rejectsOutOfRange(){Geo.parse("181",false);}
    @Test public void dmsCarriesSeconds(){assertEquals("32° 00′ 00.00″ N",Geo.dms(31.99999999,true));}
    @Test public void durationAndNormalization(){assertEquals("01:01:01",Geo.duration(3661000));assertEquals(350,Geo.normalize(-10),.001);}
}