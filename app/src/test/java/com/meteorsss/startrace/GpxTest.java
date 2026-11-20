package com.meteorsss.startrace;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.*;
import org.junit.Test;

public class GpxTest {
  @Test
  public void escapesTrackNames() {
    assertEquals("A&amp;B&lt;C&gt;&quot;&apos;", Gpx.escape("A&B<C>\"'"));
  }

  @Test
  public void emitsValidXmlAndHonestAltitude() throws Exception {
    TrackPoint p = new TrackPoint(31.2, 121.3, 32, 5, 1, 1700000000000L, 1000, 0, true);
    String xml = Gpx.header("A&B") + "<trkseg>" + Gpx.point(p) + "</trkseg>" + Gpx.footer();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    assertNotNull(
        factory
            .newDocumentBuilder()
            .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
    assertFalse(xml.contains("<ele>"));
    assertTrue(xml.contains("ellipsoidHeight"));
    assertTrue(xml.contains("2023-11-14T22:13:20Z"));
  }

  @Test
  public void missingMeasurementsNeverEmitNaN() {
    String point = Gpx.point(new TrackPoint(0, 0, Double.NaN, 3, Double.NaN, 0, 0, 0, true));
    assertFalse(point.contains("NaN"));
    assertFalse(point.contains("ellipsoidHeight"));
  }
}
