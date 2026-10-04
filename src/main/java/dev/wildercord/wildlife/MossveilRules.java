package dev.wildercord.wildlife;
/** Pure finite bounds used by the real feeding and active-instance filter owners. */
public final class MossveilRules{
 private MossveilRules(){}
 public static final int MAX_TRIM=60;
 public static int trim(int remaining){return remaining<8?0:Math.min(MAX_TRIM,remaining/4);}
 public static boolean expired(long now,long until){return now>=until;}
 public static boolean planted(double totalDistanceSquared){return Double.isFinite(totalDistanceSquared)&&totalDistanceSquared>=0&&totalDistanceSquared<=.0225;}
}
