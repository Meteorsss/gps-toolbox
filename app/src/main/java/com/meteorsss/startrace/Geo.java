package com.meteorsss.startrace;
import java.util.Locale;
import java.util.regex.*;
public final class Geo {
    private Geo() {}
    public static boolean valid(double lat,double lon) { return Double.isFinite(lat)&&Double.isFinite(lon)&&lat>=-90&&lat<=90&&lon>=-180&&lon<=180; }
    public static double distance(double lat1,double lon1,double lat2,double lon2) {
        double p1=Math.toRadians(lat1),p2=Math.toRadians(lat2),dp=p2-p1,dl=Math.toRadians(lon2-lon1);
        double a=Math.sin(dp/2)*Math.sin(dp/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(dl/2)*Math.sin(dl/2);
        return 6371008.8*2*Math.atan2(Math.sqrt(Math.max(0,Math.min(1,a))),Math.sqrt(Math.max(0,1-a)));
    }
    public static double bearing(double lat1,double lon1,double lat2,double lon2) {
        double a=Math.toRadians(lat1),b=Math.toRadians(lat2),d=Math.toRadians(lon2-lon1);
        return normalize(Math.toDegrees(Math.atan2(Math.sin(d)*Math.cos(b),Math.cos(a)*Math.sin(b)-Math.sin(a)*Math.cos(b)*Math.cos(d))));
    }
    public static double normalize(double v) { return ((v%360)+360)%360; }
    public static String dms(double value,boolean latitude) {
        long cs=Math.round(Math.abs(value)*360000); long deg=cs/360000; long min=(cs%360000)/6000; double sec=(cs%6000)/100.0;
        String h=latitude?(value<0?"S":"N"):(value<0?"W":"E");
        return String.format(Locale.US,"%d° %02d′ %05.2f″ %s",deg,min,sec,h);
    }
    public static double parse(String text,boolean latitude) {
        String s=text.trim().toUpperCase(Locale.ROOT);
        boolean neg=s.startsWith("-")||s.matches(".*[SW南西].*");
        if(latitude && s.matches(".*[EW东西].*")) throw new IllegalArgumentException("纬度应使用 N/S");
        if(!latitude && s.matches(".*[NS南北].*")) throw new IllegalArgumentException("经度应使用 E/W");
        String cleaned=s.replaceAll("[NSEW北南东西]", "").trim();
        if(!cleaned.matches("[+\\-0-9.°º′'″\"\\s:]+")) throw new IllegalArgumentException("坐标格式无效");
        Matcher m=Pattern.compile("[+-]?\\d+(?:\\.\\d+)?").matcher(cleaned);
        Matcher h=Pattern.compile("[NSEW北南东西]").matcher(s);int hc=0;while(h.find())hc++;
        if(hc>1)throw new IllegalArgumentException("仅允许一个方向标记");
        if(hc==1&&!s.matches("^(?:[NSEW北南东西].*|.*[NSEW北南东西])$"))throw new IllegalArgumentException("方向标记只能位于开头或末尾");
        if(cleaned.startsWith("-")&&s.matches(".*[NE北东].*"))throw new IllegalArgumentException("负号与方向标记冲突");
        double[] v=new double[3]; int n=0,lastEnd=0;
        while(m.find()){
            String gap=cleaned.substring(lastEnd,m.start());
            if(n==3||(n==0&&!gap.trim().isEmpty())||(n>0&&!gap.matches("[\\s°º′'″\":]+")))throw new IllegalArgumentException("坐标格式无效");
            v[n++]=Double.parseDouble(m.group());lastEnd=m.end();
        }
        if(!cleaned.substring(lastEnd).matches("[\\s°º′'″\"]*"))throw new IllegalArgumentException("坐标格式无效");
        if(n>1&&Math.abs(v[0])!=Math.floor(Math.abs(v[0])))throw new IllegalArgumentException("度分秒的度必须是整数");
        if(n==0)throw new IllegalArgumentException("请输入坐标");
        if(n>1&&(v[1]<0||v[1]>=60))throw new IllegalArgumentException("分应在 0 到 60 之间");
        if(n>2&&(v[2]<0||v[2]>=60))throw new IllegalArgumentException("秒应在 0 到 60 之间");
        double r=(Math.abs(v[0])+v[1]/60+v[2]/3600)*(neg?-1:1);
        if(!Double.isFinite(r)||Math.abs(r)>(latitude?90:180))throw new IllegalArgumentException("坐标超出范围");
        return r;
    }
    public static String meters(double v) { return v>=1000?String.format(Locale.US,"%.2f km",v/1000):String.format(Locale.US,"%.0f m",v); }
    public static String duration(long millis) { long t=Math.max(0,millis/1000);return String.format(Locale.US,"%02d:%02d:%02d",t/3600,(t/60)%60,t%60); }
}