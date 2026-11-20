package com.meteorsss.startrace;
import org.junit.Test;
import static org.junit.Assert.*;
/** Reference vectors from eviltransform/javascript/test.js; bundled BSD-2-Clause license. */
public class Gcj02Test {
 private void check(double a,double b,double x,double y){Gcj02.Coordinate c=Gcj02.fromWgs84(a,b);assertTrue(c.converted);assertEquals(x,c.latitude,0.0000005);assertEquals(y,c.longitude,0.0000005);}
 @Test public void shanghaiFixture(){check(31.1774276,121.5272106,31.17530398364597,121.531541859215);}
 @Test public void shenzhenFixture(){check(22.543847,113.912316,22.540796131694766,113.9171774808363);}
 @Test public void beijingFixture(){check(39.911954,116.377817,39.91334545536069,116.38404722455657);}
 @Test public void outsideCoverageIsNotFalselyLabelledConverted(){Gcj02.Coordinate c=Gcj02.fromWgs84(40.7,-74);assertFalse(c.converted);assertEquals(40.7,c.latitude,0);assertEquals(-74,c.longitude,0);}
 @Test public void boundaryIsOnlyApproximateCoverage(){assertFalse(Gcj02.inApproximateCoverage(0,0));assertTrue(Gcj02.inApproximateCoverage(31,121));assertFalse(Gcj02.inApproximateCoverage(56,121));}
 @Test(expected=IllegalArgumentException.class) public void rejectsNan(){Gcj02.fromWgs84(Double.NaN,121);}
 @Test(expected=IllegalArgumentException.class) public void rejectsRange(){Gcj02.fromWgs84(91,121);}
}
