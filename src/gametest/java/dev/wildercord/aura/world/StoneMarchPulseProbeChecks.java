package dev.wildercord.aura.world;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

/** Pure adversarial receipt checks, run before the native opponent world and independently by main. */
public final class StoneMarchPulseProbeChecks {
	private StoneMarchPulseProbeChecks() {}
	private static final long BEGAN = 100;
	private static final StoneMarchPulseProbe.Geometry GEOMETRY = new StoneMarchPulseProbe.Geometry(
		Vec3.ZERO, new Vec3(0, 0, 1), new Vec3(-1, 0, 0), 3);
	public static void main(String[] args) { run(); System.out.println("PASS March pulse receipt ownership, timing, counterexample and cleanup checks"); }
	public static void run() {
		Object source = new Object(), body = new Object();
		StoneMarchPulseProbe.Session session = StoneMarchPulseProbe.open(source, body, BEGAN, GEOMETRY);
		StoneMarchPulseProbe.Receipt genuine;
		try {
			check(!session.provesInward(null), "Missing original decision fails closed");
			var decision = sample(140, 1, 1, 3, 3, false, 0, false, 200);
			check(!session.provesInward(new StoneMarchPulseProbe.Receipt(session, decision)), "Forged data has no issued identity");
			check(session.recordOriginalReturn(new Object(), body, decision) == null, "Foreign attack cannot issue evidence");
			check(session.recordOriginalReturn(source, new Object(), decision) == null, "Same description with foreign body cannot issue evidence");
			genuine = session.recordOriginalReturn(source, body, decision);
			check(session.provesInward(genuine), "Exact original second-pulse spent-ground refusal passes");
			check(!session.provesInward(new StoneMarchPulseProbe.Receipt(session, decision)), "Cloned genuine data is not the issued receipt");
			var foreign = new StoneMarchPulseProbe.Session(source, body, BEGAN, GEOMETRY);
			check(!foreign.provesInward(genuine), "Receipt cannot transfer across sessions");
			StoneMarchPulseProbe.close(foreign);
			check(StoneMarchPulseProbe.observing(session), "Foreign cleanup cannot detach the owned observer");
		} finally { StoneMarchPulseProbe.close(session); }
		check(!StoneMarchPulseProbe.observing(session) && !session.provesInward(genuine), "Cleanup invalidates the old session and receipt");
		check(session.recordOriginalReturn(source, body, sample(140, 1, 1, 3, 3, false, 0, false, 200)) == null, "Closed session cannot issue evidence");
		StoneMarchPulseProbe.close(session);
		for (var wrong : new StoneMarchPulseProbe.Decision[] {
			sample(139, 1, 1, 3, 3, false, 0, false, 200), // stale observation
			sample(141, 1, 1, 3, 3, false, 0, false, 200), // later escape cannot repair the actual pulse
			sample(140, 0, 1, 3, 3, false, 0, false, 200), // wrong pulse
			sample(140, 1, -1, 3, 3, false, 0, false, 200), // sampled after decision finished
			sample(140, 1, 1, 1, 3, false, 0, false, 200), // pulse not consumed
			sample(140, 1, 1, 3, 2, false, 0, false, 200), // clipped geometry
			sample(140, 1, 1, 3, 3, true, 0, false, 200), // already spent personal attempt
			sample(140, 1, 1, 3, 3, false, 1, true, 200), // original decision hits even if endpoint later escapes
			sample(140, 1, 1, 3, 3, false, 1, false, 200), // other refusal is not spent-ground counterplay
			sample(140, 1, 1, 3, 3, false, 0, true, 200), // original boolean may not be replaced
			sample(140, 1, 1, 3, 3, false, 0, false, 173.6F)
		}) {
			var bad = StoneMarchPulseProbe.open(source, body, BEGAN, GEOMETRY);
			try { check(!bad.provesInward(bad.recordOriginalReturn(source, body, wrong)), "Invalid original pulse evidence cannot pass"); }
			finally { StoneMarchPulseProbe.close(bad); }
		}
		var failed = StoneMarchPulseProbe.open(source, body, BEGAN, GEOMETRY);
		try {
			var receipt = failed.recordOriginalReturn(source, body, sample(140, 1, 1, 3, 3, false, 0, false, 200));
			failed.fail(new AssertionError("observer failed"));
			check(!failed.provesInward(receipt), "Latched observer failure cannot certify gameplay");
		} finally { StoneMarchPulseProbe.close(failed); }
		var held = StoneMarchPulseProbe.open(source, body, BEGAN, GEOMETRY);
		try {
			var decision = sample(140, 1, 1, 3, 3, false, 1, true, 200);
			held.recordOriginalReturn(source, body, decision);
			check(!held.provesHold(), "Hold requires both native selection and recheck");
			held.recordOriginalReturn(source, body, decision);
			check(held.provesHold(), "Both original positive decisions preserve the hold damage control");
			held.recordOriginalReturn(source, body, decision);
			check(!held.provesHold(), "Receipt overflow fails closed instead of dropping contrary evidence");
		} finally { StoneMarchPulseProbe.close(held); }
		var afterFailure = StoneMarchPulseProbe.open(source, body, BEGAN, GEOMETRY);
		try { throw new AssertionError("simulated native assertion"); }
		catch (AssertionError expected) { /* The same finally path must release the observer. */ }
		finally { StoneMarchPulseProbe.close(afterFailure); }
		check(!StoneMarchPulseProbe.observing(afterFailure), "Exceptional cleanup detaches the observer");
		var reused = StoneMarchPulseProbe.open(source, body, BEGAN, GEOMETRY);
		try { check(!reused.provesInward(genuine), "A later trial cannot reuse a historical receipt even with identical clock/geometry"); }
		finally { StoneMarchPulseProbe.close(reused); }
	}
	private static StoneMarchPulseProbe.Decision sample(long tick, int pulse, int resolving, int mask, int prefix,
		boolean attempted, int band, boolean hit, float health) {
		var state = new StoneMarchPulseProbe.State(BEGAN, tick, tick, pulse, resolving, mask, true, false, false,
			attempted, true, true, 10, 40);
		var geometry = new StoneMarchPulseProbe.Geometry(GEOMETRY.origin(), GEOMETRY.aim(), GEOMETRY.side(), prefix);
		Vec3 position = new Vec3(0, 0, band == 0 ? 3.457569 : 4.5);
		var body = new StoneMarchPulseProbe.Body(position, position, Vec3.ZERO, Input.EMPTY, health, 0, 180, 10, true, true, false, band);
		return new StoneMarchPulseProbe.Decision(state, geometry, body, hit);
	}
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
