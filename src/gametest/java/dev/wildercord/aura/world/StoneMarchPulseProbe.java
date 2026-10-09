package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** GameTest-only receipt of the original eligibility return, before its caller can attempt damage. */
public final class StoneMarchPulseProbe {
	private StoneMarchPulseProbe() {}
	private static Session active;
	record Geometry(Vec3 origin, Vec3 aim, Vec3 side, int prefix) {}
	record State(long acceptedTick, long gameTick, long lastMarchTick, int pulse, int resolvingPulse, int consumedMask,
		boolean installedSource, boolean finished, boolean released, boolean alreadyAttempted,
		boolean originalRosterBody, boolean currentLevelBody, int attackId, float attackAge) {}
	record Body(Vec3 position, Vec3 relative, Vec3 velocity, Input receivedInput, float health, float absorption,
		float yaw, float pitch, boolean alive, boolean onGround, boolean spectator, int band) {}
	record Decision(State state, Geometry geometry, Body body, boolean originalCanHit) {}
	static final class Receipt {
		private final Session issuer;
		private final Decision decision;
		Receipt(Session issuer, Decision decision) { this.issuer = issuer; this.decision = decision; }
	}
	static final class Session {
		private final Object source, body;
		private final long acceptedTick;
		private final Geometry geometry;
		private final List<Receipt> issued = new ArrayList<>(2);
		private Throwable failure;
		private boolean closed;
		Session(Object source, Object body, long acceptedTick, Geometry geometry) {
			this.source = source; this.body = body; this.acceptedTick = acceptedTick; this.geometry = geometry;
		}
		boolean matches(Object source, Object body) { return !closed && this.source == source && this.body == body; }
		Receipt recordOriginalReturn(Object source, Object body, Decision decision) {
			if (!matches(source, body) || decision.state.pulse != 1) return null;
			if (issued.size() == 2) { fail(new AssertionError("Extra original second-pulse eligibility return")); return null; }
			Receipt receipt = new Receipt(this, decision); issued.add(receipt); return receipt;
		}
		void fail(Throwable problem) { if (failure == null) failure = problem; }
		Receipt first() { return issued.isEmpty() ? null : issued.getFirst(); }
		private boolean common(Receipt receipt) {
			if (closed || failure != null || receipt == null || receipt.issuer != this || !issued.contains(receipt)) return false;
			Decision d = receipt.decision; State s = d.state; Body b = d.body;
			return s.acceptedTick == acceptedTick && s.gameTick == acceptedTick + StoneMarchRules.SECOND
				&& s.lastMarchTick == s.gameTick && s.pulse == 1 && s.resolvingPulse == 1 && s.consumedMask == 3
				&& s.installedSource && !s.finished && !s.released && !s.alreadyAttempted
				&& s.originalRosterBody && s.currentLevelBody && s.attackId == 10 && s.attackAge == StoneMarchRules.SECOND
				&& geometry.prefix == StoneMarchRules.BANDS && geometry.equals(d.geometry)
				&& b.alive && b.onGround && !b.spectator && Math.abs(b.health - 200) < .02 && b.absorption == 0;
		}
		boolean provesInward(Receipt receipt) {
			return issued.size() == 1 && receipt == first() && common(receipt)
				&& !receipt.decision.originalCanHit && receipt.decision.body.band == 0;
		}
		boolean provesHold() {
			return issued.size() == 2 && issued.stream().allMatch(receipt -> common(receipt)
				&& receipt.decision.originalCanHit && receipt.decision.body.band == 1);
		}
		void requireInward() { StoneMarchFixture.check(provesInward(first()), "Original second-pulse decision must exclude the exact challenger on spent ground: " + summary()); }
		void requireHold() { StoneMarchFixture.check(provesHold(), "Held challenger must reach both original second-pulse damage decisions: " + summary()); }
		String summary() {
			return new com.google.gson.GsonBuilder().serializeSpecialFloatingPointValues().create().toJson(
				new Summary(acceptedTick, closed, failure == null ? null : failure.toString(), issued.stream().map(receipt -> receipt.decision).toList()));
		}
	}
	private record Summary(long acceptedTick, boolean closed, String observerFailure, List<Decision> originalReturns) {}

	static Session open(Object source, Object body, long acceptedTick, Geometry geometry) {
		if (active != null) throw new AssertionError("Previous March pulse observer was not closed");
		return active = new Session(source, body, acceptedTick, geometry);
	}
	static Session open(StoneMarch accepted, ServerPlayer body, long began) {
		return open(accepted, body, began, geometry(accepted));
	}
	static void close(Session session) {
		if (session == null) return;
		session.closed = true;
		if (active == session) active = null;
	}
	static boolean observing(Session session) { return active == session && !session.closed; }
	static void report(Session session) {
		if (session != null) Wildercord.LOGGER.info("WILDERCORD_MARCH_PULSE {}", session.summary());
	}
	private static Geometry geometry(StoneMarch accepted) {
		return new Geometry((Vec3) StoneMarchFixture.field(accepted, "origin"), (Vec3) StoneMarchFixture.field(accepted, "aim"),
			(Vec3) StoneMarchFixture.field(accepted, "side"), (int) StoneMarchFixture.field(accepted, "prefix"));
	}

	/** Called only by the non-cancellable RETURN injection; never re-evaluates canHit or its geometry/permission queries. */
	@SuppressWarnings("unchecked")
	public static void originalReturn(Object source, ServerPlayer body, int pulse, boolean result) {
		Session session = active;
		if (session == null || !session.matches(source, body) || pulse != 1) return;
		try {
			StoneMarch accepted = (StoneMarch) source;
			SwordMaster master = (SwordMaster) StoneMarchFixture.field(accepted, "master");
			ServerLevel level = (ServerLevel) StoneMarchFixture.field(accepted, "level");
			Geometry geometry = geometry(accepted);
			Vec3 position = body.position(), relative = position.subtract(geometry.origin);
			State state = new State((long) StoneMarchFixture.field(accepted, "began"), level.getGameTime(),
				(long) StoneMarchFixture.field(accepted, "lastTick"), pulse, (int) StoneMarchFixture.field(accepted, "resolvingPulse"),
				(int) StoneMarchFixture.field(accepted, "consumed"), StoneMarchFixture.field(master, "march") == source,
				(boolean) StoneMarchFixture.field(accepted, "finished"), (boolean) StoneMarchFixture.field(accepted, "released"),
				((Set<UUID>) StoneMarchFixture.field(accepted, "attempted")).contains(body.getUUID()),
				((Map<UUID, ServerPlayer>) StoneMarchFixture.field(accepted, "bodies")).get(body.getUUID()) == body,
				level.getPlayerByUUID(body.getUUID()) == body, master.attackAnimation(), master.attackElapsed(0));
			Body snapshot = new Body(position, relative, body.getDeltaMovement(), body.getLastClientInput(), body.getHealth(),
				body.getAbsorptionAmount(), body.getYRot(), body.getXRot(), body.isAlive(), body.onGround(), body.isSpectator(),
				StoneMarchRules.band(relative.dot(geometry.aim), relative.dot(geometry.side), relative.y));
			session.recordOriginalReturn(source, body, new Decision(state, geometry, snapshot, result));
		} catch (RuntimeException | Error failure) {
			// Observational failure must not change the original eligibility result or prevent native damage.
			session.fail(failure);
		}
	}
}
