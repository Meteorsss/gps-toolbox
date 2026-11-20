package com.meteorsss.startrace;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.*;
@RunWith(AndroidJUnit4.class)
public class AppSmokeTest {
    private void shell(String command)throws Exception{try(ParcelFileDescriptor p=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(p)){while(in.read()!=-1){}}}
    private void screenshot(String name)throws Exception{Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();if(image!=null){File dir=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"screenshots");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}}
    @Test public void allPagesAndBothThemesRenderWithoutLocationPermission()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            String[] expected={"此刻，在哪里","头顶的卫星","把走过的路留下","随手可用"};
            for(int i=0;i<4;i++){final int page=i;scenario.onActivity(a->{a.goPage(page);assertTrue(a.visibleText().contains(expected[page]));});InstrumentationRegistry.getInstrumentation().waitForIdleSync();screenshot("dark-"+i);}
            String[] modes={"settings","places","compass","speed","diagnostics"};for(String mode:modes){scenario.onActivity(a->a.navigate(mode));InstrumentationRegistry.getInstrumentation().waitForIdleSync();screenshot(mode);}
            scenario.onActivity(a->{a.goPage(0);a.theme();assertTrue(a.u.light);});InstrumentationRegistry.getInstrumentation().waitForIdleSync();screenshot("light-home");scenario.recreate();scenario.onActivity(a->{assertTrue(a.u.light);a.theme();});
        }
    }
    @Test public void storageExportAndRecoveryPreserveData()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();Store db=((App)context.getApplicationContext()).store;long id=db.begin("A&B 测试");try{db.add(id,new TrackPoint(31,121,42,5,1,1700000000000L,1000,0,true),false);db.add(id,new TrackPoint(31.1,121.1,Double.NaN,90,Double.NaN,1700000001000L,2000,1,false),true);db.add(id,new TrackPoint(31.2,121.2,43,4,2,1700000040000L,41000,2,true),false);db.update(id,1000,5,"recording");db.recoverInterrupted();assertEquals("interrupted",db.track(id).status);assertEquals(2,db.track(id).points);ByteArrayOutputStream gpx=new ByteArrayOutputStream();db.export(id,gpx,false);String text=gpx.toString("UTF-8");assertTrue(text.contains("A&amp;B"));assertEquals(2,text.split("<trkseg>",-1).length-1);assertFalse(text.contains("31.10000000"));ByteArrayOutputStream csv=new ByteArrayOutputStream();db.export(id,csv,true);assertEquals(5,csv.toString("UTF-8").split("\n",-1).length);long place=db.savePlace("Test","note",31,121);assertTrue(db.places().stream().anyMatch(p->p.id==place));db.deletePlace(place);}finally{db.delete(id);}assertNull(db.track(id));
    }
    @Test public void foregroundRecordingPausesResumesAndStops()throws Exception{
        String pkg=InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();shell("pm grant "+pkg+" android.permission.ACCESS_COARSE_LOCATION");shell("pm grant "+pkg+" android.permission.ACCESS_FINE_LOCATION");shell("pm grant "+pkg+" android.permission.POST_NOTIFICATIONS");shell("cmd location set-location-enabled true");long[] id={-1};
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->a.startForegroundService(new Intent(a,TrackService.class).setAction("START")));SystemClock.sleep(1500);scenario.onActivity(a->{assertNotNull(TrackService.active);id[0]=TrackService.active.trackId;assertTrue(id[0]>0);a.startService(new Intent(a,TrackService.class).setAction("PAUSE"));});SystemClock.sleep(500);scenario.onActivity(a->{assertTrue(TrackService.active.paused);a.startService(new Intent(a,TrackService.class).setAction("RESUME"));});SystemClock.sleep(500);scenario.onActivity(a->{assertFalse(TrackService.active.paused);a.startService(new Intent(a,TrackService.class).setAction("STOP"));});SystemClock.sleep(500);scenario.onActivity(a->{assertNull(TrackService.active);assertEquals("finished",a.app.store.track(id[0]).status);a.app.store.delete(id[0]);});
        }finally{Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();c.stopService(new Intent(c,TrackService.class));if(id[0]>0)((App)c.getApplicationContext()).store.delete(id[0]);}
    }
}