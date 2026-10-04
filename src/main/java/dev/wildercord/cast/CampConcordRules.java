package dev.wildercord.cast;
/** Fixed utility bounds never multiply with spell power, repeated groups or affinity. */
public final class CampConcordRules {
 private CampConcordRules(){}
 public static final int CAP=64,RAW=24,WATCH=900,WATCH_REST=1800,OFFER=60,DONOR_REST=600,RECEIVER_REST=200;
 public record Gift(int debit,int gain){public static final Gift NONE=new Gift(0,0);}
 public static Gift gift(float donor,float target,int capacity){
  if(!Float.isFinite(donor)||!Float.isFinite(target)||donor<0||target<0||capacity<0)return Gift.NONE;
  int available=(int)Math.min(24,Math.max(0,Math.floor(donor-2)));
  int need=(int)Math.min(16,Math.max(0,Math.floor(capacity-target)));
  int gain=Math.min(need,available*3/4),debit=(gain*4+2)/3;
  return gain>0&&debit<=available?new Gift(debit,gain):Gift.NONE;
 }
 public static long ready(long stored,long now,int maximum){return stored<0||stored-now>maximum?0:stored;}
 public static boolean edge(boolean before,boolean now){return !before&&now;}
}
