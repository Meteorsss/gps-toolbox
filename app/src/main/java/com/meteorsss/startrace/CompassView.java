package com.meteorsss.startrace;

import android.graphics.*;
import android.os.SystemClock;
import android.view.*;

/** A vsync-driven compass. Only the dial/needle redraw, never the containing page. */
public final class CompassView extends View implements Choreographer.FrameCallback {
  private final MainActivity activity;
  private final Ui ui;
  private final Store.Place destination;
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  private final Typeface typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);
  private final Path arrow=new Path();
  private final HeadingSmoother smoothing=new HeadingSmoother();
  private Bitmap dial;
  private boolean active,attached,scheduled,locationFresh;
  private float centerX,centerY,radius,displayHeading=Float.NaN,targetBearing,declination;
  private int labelHeading=-1;
  private String headingText="";
  private long frames;
  public CompassView(MainActivity activity,Store.Place destination){
    super(activity);this.activity=activity;this.ui=activity.u;this.destination=destination;
    setContentDescription(destination==null?"平滑指南针，磁北基准":"目标方向指南针，真北基准");
    // Default hardware acceleration; no forced software layer.
  }
  public void setActive(boolean active){this.active=active;if(!active){cancelFrame();smoothing.reset();}else scheduleFrame();}
  private void scheduleFrame(){if(active&&attached&&getWindowVisibility()==VISIBLE&&!scheduled){scheduled=true;Choreographer.getInstance().postFrameCallback(this);}}
  private void cancelFrame(){if(scheduled){Choreographer.getInstance().removeFrameCallback(this);scheduled=false;}}
  @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;scheduleFrame();}
  @Override protected void onDetachedFromWindow(){attached=false;cancelFrame();super.onDetachedFromWindow();}
  @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE)scheduleFrame();else{cancelFrame();smoothing.reset();}}
  public void updateSlowState(){
    locationFresh=activity.e.fresh();declination=activity.e.declination();
    if(destination!=null&&locationFresh)targetBearing=(float)Geo.bearing(activity.e.location.getLatitude(),activity.e.location.getLongitude(),destination.lat,destination.lon);
  }
  @Override public void doFrame(long frameNanos){
    scheduled=false;if(!active||!attached||getWindowVisibility()!=VISIBLE)return;
    float target=activity.e.magneticHeading;
    if(activity.e.orientationAt==0||SystemClock.elapsedRealtime()-activity.e.orientationAt>2000)target=Float.NaN;
    if(destination!=null)target=locationFresh?(float)Geo.normalize(target+declination):Float.NaN;
    displayHeading=(float)smoothing.update(target,frameNanos);
    int next=Float.isNaN(displayHeading)?-1:Math.round(displayHeading)%360;
    if(next!=labelHeading){labelHeading=next;headingText=next<0?"":next+"°";}
    invalidate();scheduleFrame();
  }
  @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
    super.onSizeChanged(w,h,oldw,oldh);if(w<=0||h<=0)return;
    centerX=w/2f;centerY=h/2f;radius=Math.min(centerX,centerY)-ui.dp(30);
    // Allocate/cache dial only on layout changes; no bitmap or path allocation in each frame.
    dial=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(dial);
    paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(ui.dp(1));paint.setColor(ui.border);canvas.drawCircle(centerX,centerY,radius,paint);
    for(int deg=0;deg<360;deg+=5){double angle=Math.toRadians(deg);float inner=radius-ui.dp(deg%30==0?15:7);paint.setColor(deg==0?ui.accent:ui.border);paint.setStrokeWidth(ui.dp(deg%30==0?2:1));canvas.drawLine(centerX+(float)Math.sin(angle)*inner,centerY-(float)Math.cos(angle)*inner,centerX+(float)Math.sin(angle)*radius,centerY-(float)Math.cos(angle)*radius,paint);}
    String[] labels={"N","E","S","W"};for(int i=0;i<4;i++){double angle=Math.toRadians(i*90);text(canvas,labels[i],centerX+(float)Math.sin(angle)*(radius-ui.dp(33)),centerY-(float)Math.cos(angle)*(radius-ui.dp(33))+ui.dp(5),i==0?ui.accent:ui.text,16);}
    arrow.reset();arrow.moveTo(centerX,centerY-radius*.48f);arrow.lineTo(centerX-ui.dp(14),centerY+ui.dp(22));arrow.lineTo(centerX,centerY+ui.dp(12));arrow.lineTo(centerX+ui.dp(14),centerY+ui.dp(22));arrow.close();
    // Avoid recycling a bitmap that may still be referenced by a previous hardware display list.
  }
  private void text(Canvas c,String value,float x,float y,int color,int size){paint.setStyle(Paint.Style.FILL);paint.setTypeface(typeface);paint.setTextSize(ui.dp(size));paint.setTextAlign(Paint.Align.CENTER);paint.setColor(color);c.drawText(value,x,y,paint);}
  @Override protected void onDraw(Canvas canvas){
    super.onDraw(canvas);frames++;
    if(dial==null)return;
    canvas.save();if(!Float.isNaN(displayHeading))canvas.rotate(-displayHeading,centerX,centerY);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);canvas.drawBitmap(dial,0,0,paint);canvas.restore();
    if(Float.isNaN(displayHeading)){text(canvas,destination!=null&&!locationFresh?"等待有效位置与方向":"等待方向数据",centerX,centerY,ui.muted,14);return;}
    canvas.save();if(destination!=null)canvas.rotate(targetBearing-displayHeading,centerX,centerY);paint.setColor(ui.accent);canvas.drawPath(arrow,paint);canvas.restore();
    text(canvas,headingText,centerX,centerY+radius*.62f,ui.text,24);
  }
  long renderedFrames(){return frames;}
  boolean hasScheduledFrame(){return scheduled;}
}
