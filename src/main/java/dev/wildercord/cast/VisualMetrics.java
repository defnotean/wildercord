package dev.wildercord.cast;

/** Cheap server-thread counters used by the reproducible combat benchmark and diagnostic command. */
public final class VisualMetrics {
	private VisualMetrics() {}
	private static long particles, recipients, formations, dropped;
	private static final double[] TICKS=new double[512];private static int tickSamples,tickCursor;private static long tickStart;
	public static void init(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(s->tickStart=System.nanoTime());
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{TICKS[tickCursor++%TICKS.length]=(System.nanoTime()-tickStart)/1e6;tickSamples=Math.min(TICKS.length,tickSamples+1);});}
	private static String timings(){if(tickSamples==0)return "server tick samples: 0";double[] sorted=java.util.Arrays.copyOf(TICKS,tickSamples);java.util.Arrays.sort(sorted);return String.format(java.util.Locale.ROOT,"server tick median=%.2fms p95=%.2fms (%d samples)",sorted[tickSamples/2],sorted[(int)((tickSamples-1)*.95)],tickSamples);}
	public static void particle() { particles++; }
	public static void recipient() { recipients++; }
	public static void formation() { formations++; }
	public static void dropped() { dropped++; }
	public static String report() { return "Particle packets built: " + particles + ", deliveries: " + recipients + ", formations: " + formations
		+ ", decoration limited: " + dropped + ", scheduled parts: " + Scheduler.pending() + "; " + dev.wildercord.spell.CompiledSpellCache.report()+"; "+timings(); }
	public static void reset() { particles = recipients = formations = dropped = 0;tickSamples=tickCursor=0; }
}
