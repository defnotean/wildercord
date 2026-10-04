package dev.wildercord.wildlife;

/** Finite local playback schedule: server clock stalls never renew an admitted burst. */
public final class TidewardPlayback {
 private final long started;private final int lifetime;private long emitted=-1;
 public TidewardPlayback(long started,int lifetime){if(started<0||lifetime<1||lifetime>100)throw new IllegalArgumentException("Invalid Tideward playback");this.started=started;this.lifetime=lifetime;}
 public boolean alive(long tick){long age=tick-started;return age>=0&&age<lifetime;}
 public boolean firstBurst(long tick){if(!alive(tick)||emitted>=0)return false;emitted=tick-started;return true;}
 public boolean burst(long tick){long age=tick-started;if(!alive(tick)||age%10!=0||age==emitted)return false;emitted=age;return true;}
}
