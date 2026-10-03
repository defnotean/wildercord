package dev.wildercord.wildlife;
/** Small, finite, nondestructive cave observation loop. */
public final class SporebackRules {
 private SporebackRules() {}
 public static final int GATHER_REST=2400, FORAGE_REST=1200, RESPONSE_REST=200, HIDE_TICKS=120;
 public static boolean room(int count) {return count<2;}
 public static boolean ready(long now,long deadline) {return now>=deadline;}
}
