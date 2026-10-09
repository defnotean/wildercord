package dev.wildercord.gametest;

import com.google.gson.Gson;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.Awakening;
import dev.wildercord.aura.AwakeningRules;
import dev.wildercord.client.AuraFxClient;
import dev.wildercord.client.fx.Glimmer;
import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.impl.client.gametest.FabricClientGameTestRunner;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * GameTest-only receipt of real AuraFxClient ticks. LIVE retains the original RNG object and every
 * original draw. SCRIPT supplies bounded legal inputs only to motes()'s local RNG, never the player.
 * Neither phase calls tick(), creates a particle, changes a threshold, or waits for an emission.
 */
public final class AuraBodyMoteProbe {
	private AuraBodyMoteProbe() {}
	private static final String SUITE = "dev.wildercord.gametest.WildercordAuraFxTest";
	private static final Gson JSON = new Gson();
	private static Session active;
	private static Call current;

	public enum Mode { LIVE, SCRIPT }
	public record Tick(long time, String camera, boolean paused, boolean invisible, boolean spectator,
			boolean cameraOwn, double distanceSquared, String quality, int stage, boolean lit, float intensity,
			boolean fighting, int momentum, float surge, boolean awakened, boolean spent, int players,
			int budgetBefore, int budgetAfter, int eligibleCalls, int counterDelta) {}
	public record Draw(long time, boolean room, Float sample, float chance, int adds, int counterDelta,
			boolean originalSource, String particle) {}
	public record Receipt(String phase, int entityId, String uuid, List<Tick> ticks, List<Draw> draws,
			int scriptedDraws, boolean scriptExhausted, boolean originalRandomUnchanged, List<String> errors) {}

	/** An unforgeable ownership token; only the activating fixture can close this exact session. */
	public static final class Session {
		private final Mode mode;
		private final Minecraft client;
		private final ClientLevel world;
		private final LocalPlayer player;
		private final int entityId;
		private final UUID uuid;
		private final RandomSource originalRandom;
		private final List<Tick> ticks = new ArrayList<>();
		private final List<Draw> draws = new ArrayList<>();
		private final List<String> errors = new ArrayList<>();
		private AuraBodyMoteScript script;
		private Tick pending;
		private int before;
		private int eligible;
		private Session(Minecraft mc, Mode mode) {
			this.mode = mode;
			client = mc; world = mc.level; player = mc.player;
			entityId = player.getId(); uuid = player.getUUID(); originalRandom = player.getRandom();
			if (mode == Mode.SCRIPT) script = new AuraBodyMoteScript();
		}
		private boolean matches(Minecraft mc, ClientLevel level, AbstractClientPlayer candidate) {
			return client == mc && world == level && player == candidate && candidate.getId() == entityId && candidate.getUUID().equals(uuid);
		}
	}

	public static final class Call {
		private final Session session;
		private final long time;
		private final float chance;
		private final int before;
		private boolean room, roomSeen, sampleSeen, sourceSeen, originalSource;
		private float sample = Float.NaN;
		private int adds;
		private String particle = "none";
		private Call(Session session, float intensity) {
			this.session = session; time = session.world.getGameTime(); chance = 0.03F + 0.08F * intensity;
			before = AuraFxClient.counts()[5];
		}
	}

	private static void require(boolean condition, String detail) {
		if (!condition) throw new AssertionError("Glow mote contract: " + detail);
	}
	private static void clientThread(Minecraft mc) { require(mc.isSameThread(), "scope access must be on the client thread"); }
	private static boolean suite() {
		var entry = FabricClientGameTestRunner.currentlyRunningGameTest;
		return entry != null && SUITE.equals(entry.getDefinition());
	}

	/** Readiness is attachment/camera synchronization, never a request for a successful random sample. */
	public static boolean ready(Minecraft mc) {
		if (mc.level == null || mc.player == null || mc.isPaused() || mc.getCameraEntity() != mc.player
				|| mc.options.getCameraType() != CameraType.THIRD_PERSON_FRONT || MagicQuality.bodyAura != MagicQuality.BodyAura.FULL
				|| mc.player.isInvisible() || mc.player.isSpectator()
				|| mc.player.position().distanceToSqr(mc.gameRenderer.mainCamera().position()) > 1600) return false;
		var look = Aura.look(mc.player);
		long now = mc.level.getGameTime();
		return mc.level.players().size() == 1 && mc.level.players().contains(mc.player)
			&& look.stage() == AuraRules.GLOW && look.lit() && AuraPresence.look(mc.player).fighting(now)
			&& AuraPresence.look(mc.player).momentum() == 0 && AuraFxClient.surge(mc.player.getId(), now) == 0
			&& Awakening.state(mc.player).stage() != AwakeningRules.Phase.AWAKENED && !Awakening.spent(mc.player)
			&& Math.abs(AuraFxClient.bodyIntensity(mc.player, now) - AuraFxRules.FIGHTING) < 0.00001F;
	}

	public static Session open(Minecraft mc, Mode mode) {
		clientThread(mc);
		require(active == null && current == null, "nested or leaked scope");
		require(suite() && ready(mc), "fixture is not a ready Glow fight in the exact AuraFx suite");
		return active = new Session(mc, mode);
	}

	public static Receipt close(Minecraft mc, Session token) {
		clientThread(mc);
		require(active == token, "wrong scope token");
		try {
			if (current != null || token.pending != null) token.errors.add("scope closed during a native invocation");
			boolean unchanged = token.player.getRandom() == token.originalRandom;
			return new Receipt(token.mode.name(), token.entityId, token.uuid.toString(), List.copyOf(token.ticks), List.copyOf(token.draws),
				token.script == null ? 0 : token.script.consumed(), token.script == null || token.script.exhausted(), unchanged, List.copyOf(token.errors));
		} finally {
			active = null; current = null; token.pending = null; token.script = null;
		}
	}

	/** Called around the original native event handler, so missing eligibility cannot look like a valid zero sample. */
	public static Session beginTick(Minecraft mc, int budget) {
		Session s = active;
		if (s == null) return null;
		if (!suite() || !s.matches(mc, mc.level, mc.player)) {
			s.errors.add("fixture world/player/suite changed");
			return null;
		}
		require(s.pending == null && current == null, "nested native tick");
		if (s.ticks.size() >= (s.mode == Mode.LIVE ? 30 : 2)) {
			s.errors.add("native tick exceeded the fixed window");
			return null;
		}
		var look = Aura.look(s.player);
		long now = s.world.getGameTime();
		s.pending = new Tick(now, mc.options.getCameraType().name(), mc.isPaused(), s.player.isInvisible(), s.player.isSpectator(),
			mc.getCameraEntity() == s.player, s.player.position().distanceToSqr(mc.gameRenderer.mainCamera().position()),
			MagicQuality.bodyAura.name(), look.stage(), look.lit(), AuraFxClient.bodyIntensity(s.player, now),
			AuraPresence.look(s.player).fighting(now), AuraPresence.look(s.player).momentum(), AuraFxClient.surge(s.entityId, now),
			Awakening.state(s.player).stage() == AwakeningRules.Phase.AWAKENED, Awakening.spent(s.player), s.world.players().size(), budget, budget, 0, 0);
		s.before = AuraFxClient.counts()[5]; s.eligible = 0;
		if (!ready(mc)) s.errors.add("ineligible native tick at " + now);
		return s;
	}

	public static void endTick(Session s, int budget) {
		if (s == null) return;
		Tick t = s.pending;
		if (t == null) { s.errors.add("missing tick entry"); return; }
		s.ticks.add(new Tick(t.time, t.camera, t.paused, t.invisible, t.spectator, t.cameraOwn, t.distanceSquared, t.quality,
			t.stage, t.lit, t.intensity, t.fighting, t.momentum, t.surge, t.awakened, t.spent, t.players,
			t.budgetBefore, budget, s.eligible, AuraFxClient.counts()[5] - s.before));
		s.pending = null;
	}

	public static Call beginMotes(Minecraft mc, ClientLevel level, AbstractClientPlayer player, AuraAttachments.Look look, float intensity) {
		Session s = active;
		if (s == null || !s.matches(mc, level, player)) return null;
		require(current == null, "nested motes call");
		if (s.pending == null) { s.errors.add("motes outside original native tick"); return null; }
		s.eligible++;
		if (look.stage() != AuraRules.GLOW || Math.abs(intensity - AuraFxRules.FIGHTING) >= 0.00001F) s.errors.add("wrong Glow stage/intensity");
		return current = new Call(s, intensity);
	}

	public static RandomSource source(AbstractClientPlayer player, RandomSource original) {
		Call c = current;
		if (c == null || c.session != active || c.session.player != player) return original;
		c.sourceSeen = true; c.originalSource = original == c.session.originalRandom;
		return c.session.mode == Mode.SCRIPT ? c.session.script : original;
	}
	public static void room(boolean room) {
		if (current != null) { current.roomSeen = true; current.room = room; }
	}
	public static void sample(RandomSource random, float sample) {
		Call c = current;
		if (c == null) return;
		c.sampleSeen = true; c.sample = sample;
		if (random != (c.session.mode == Mode.SCRIPT ? c.session.script : c.session.originalRandom)) c.session.errors.add("unexpected sample source");
	}
	public static void added(ParticleEngine engine, Particle particle) {
		Call c = current;
		if (c == null) return;
		c.adds++; c.particle = particle.getClass().getName();
		if (engine != c.session.client.particleEngine || !(particle instanceof Glimmer)) c.session.errors.add("wrong particle/engine at original add");
	}
	public static void endMotes(Call c) {
		if (c == null) return;
		try {
			int delta = AuraFxClient.counts()[5] - c.before;
			if (!c.sourceSeen || !c.originalSource || !c.roomSeen || !c.room || !c.sampleSeen) c.session.errors.add("missing source/room/sample receipt");
			int expected = c.room && c.sampleSeen && c.sample < c.chance ? 1 : 0;
			if (c.adds != expected || delta != expected) c.session.errors.add("sample/add/counter disagreement at " + c.time);
			c.session.draws.add(new Draw(c.time, c.room, c.sampleSeen ? c.sample : null, c.chance, c.adds, delta, c.originalSource, c.particle));
		} finally { current = null; }
	}

	/** The close and receipt run even when the fixed window fails; also exercised by standalone failure controls. */
	public static <T, R> R window(Supplier<T> open, Runnable ticks, Function<T, R> close, Consumer<R> observe) {
		T token = open.get();
		R receipt;
		try { ticks.run(); }
		finally { receipt = close.apply(token); observe.accept(receipt); }
		return receipt;
	}

	private static Receipt measure(ClientGameTestContext context, Mode mode, int ticks) {
		Receipt receipt = window(() -> context.computeOnClient(mc -> open(mc, mode)), () -> context.waitTicks(ticks),
			token -> context.computeOnClient(mc -> close(mc, token)),
			result -> Wildercord.LOGGER.info("WILDERCORD_AURA_BODY_MOTES {}", JSON.toJson(result)));
		validate(receipt, mode, ticks);
		return receipt;
	}

	public static void validate(Receipt receipt, Mode mode, int ticks) {
		require(receipt.phase.equals(mode.name()) && receipt.errors.isEmpty(), "receipt errors: " + receipt.errors);
		require(receipt.originalRandomUnchanged, "shared player RNG was replaced");
		require(receipt.ticks.size() == ticks && receipt.draws.size() == ticks, "missing native ticks or eligible calls");
		for (int i = 0; i < ticks; i++) {
			Tick tick = receipt.ticks.get(i); Draw draw = receipt.draws.get(i);
			require(tick.eligibleCalls == 1 && tick.counterDelta == draw.counterDelta && tick.time == draw.time,
				"tick/emission receipt mismatch at " + i);
			require(!tick.paused && !tick.invisible && !tick.spectator && tick.cameraOwn
				&& tick.camera.equals(CameraType.THIRD_PERSON_FRONT.name()) && Double.isFinite(tick.distanceSquared)
				&& tick.distanceSquared >= 0 && tick.distanceSquared <= 1600 && tick.quality.equals("FULL")
				&& tick.stage == AuraRules.GLOW && tick.lit && tick.fighting && tick.momentum == 0 && tick.surge == 0
				&& !tick.awakened && !tick.spent && tick.players == 1
				&& Math.abs(tick.intensity - AuraFxRules.FIGHTING) < 0.00001F,
				"ineligible recorded state at " + i);
			require(i == 0 || tick.time == receipt.ticks.get(i - 1).time + 1, "nonconsecutive client game time at " + i);
			require(draw.room && draw.originalSource && draw.chance == 0.03F + 0.08F * AuraFxRules.FIGHTING
				&& draw.sample != null && Float.isFinite(draw.sample) && draw.sample >= 0 && draw.sample < 1,
				"budget/source/probability/sample changed at " + i);
			int admitted = draw.sample < draw.chance ? 1 : 0;
			require(draw.adds == admitted && draw.counterDelta == admitted
				&& draw.particle.equals(admitted == 1 ? "dev.wildercord.client.fx.Glimmer" : "none"),
				"actual add does not match the original sampled branch at " + i);
		}
		if (mode == Mode.SCRIPT) {
			require(receipt.scriptExhausted && receipt.scriptedDraws == 8, "script not consumed exactly");
			require(receipt.draws.get(0).sample == 0.5F && receipt.draws.get(0).adds == 0 && receipt.draws.get(0).counterDelta == 0,
				"rejected original branch emitted");
			require(receipt.draws.get(1).sample == 0 && receipt.draws.get(1).adds == 1 && receipt.draws.get(1).counterDelta == 1,
				"admitted original branch failed to emit exactly one Glimmer");
		} else require(receipt.scriptedDraws == 0, "LIVE used controlled draws");
	}

	private static String readiness(Minecraft mc) {
		if (mc.level == null || mc.player == null) return "world=" + (mc.level != null) + ", player=" + (mc.player != null);
		var player = mc.player;
		return "time=" + mc.level.getGameTime() + ", camera=" + mc.options.getCameraType() + ", cameraOwn=" + (mc.getCameraEntity() == player)
			+ ", distanceSquared=" + player.position().distanceToSqr(mc.gameRenderer.mainCamera().position())
			+ ", paused=" + mc.isPaused() + ", invisible=" + player.isInvisible() + ", spectator=" + player.isSpectator()
			+ ", quality=" + MagicQuality.bodyAura + ", look=" + Aura.look(player) + ", presence=" + AuraPresence.look(player)
			+ ", intensity=" + AuraFxClient.bodyIntensity(player, mc.level.getGameTime()) + ", awakening=" + Awakening.state(player)
			+ ", players=" + mc.level.players().size() + ", articulated=" + dev.wildercord.client.combat.ArticulatedCombat.enabled()
			+ ", shaders=" + dev.wildercord.client.compat.ShaderCompat.active();
	}

	public static void observeFight(ClientGameTestContext context) {
		try { context.waitFor(AuraBodyMoteProbe::ready, 40); }
		catch (AssertionError failure) {
			throw new AssertionError("Glow attachment/camera readiness: " + context.computeOnClient(AuraBodyMoteProbe::readiness), failure);
		}
		Wildercord.LOGGER.info("WILDERCORD_AURA_BODY_CONFIG {}", context.computeOnClient(AuraBodyMoteProbe::readiness));
		measure(context, Mode.LIVE, 30);
	}
	public static void proveBranches(ClientGameTestContext context) {
		measure(context, Mode.SCRIPT, 2);
	}
}
