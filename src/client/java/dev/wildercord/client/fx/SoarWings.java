package dev.wildercord.client.fx;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.SoarRules;
import dev.wildercord.cast.SoarVfx;
import dev.wildercord.content.LightOption;
import dev.wildercord.player.WildercordAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * The wings of a Soar flight, drawn on every client for every flier from their synced note of it
 * ({@link WildercordAttachments#SOARING}): a pair of wings of wind at the back, three strokes of light to a
 * side fanned out from the shoulders and bowed like feathers ({@link SoarVfx#wing}), beating slowly (faster
 * on the move), folded small while the flier stands on the ground, and thinning and flickering as the flight
 * fades. Behind a flier on the move the wind runs off the wingtips in two soft streams and curls away in
 * little crescents. Each stroke is renewed every couple of ticks and moves with the body it's on, so the
 * wings keep up however fast it flies. Nothing is drawn for your own flight in first person: it would all be
 * behind you or in your eyes.
 */
public final class SoarWings {
	private SoarWings() {}

	private static final double RANGE = 64;
	/** A fresh set of strokes this often, each lasting a little longer, so the wings stay whole as they're renewed. */
	private static final int PERIOD = 2;
	private static final int LIFE = 5;
	/** Faster than this (blocks a tick) a flier leaves a wake. */
	private static final double WAKE_SPEED = 0.08;

	/** The wake's colours: the pale sky of the lifting runes, and the mint of wind itself. */
	private static final int SKY = 0xBFE3FF;
	private static final int MINT = ElementFx.WIND.accent();

	/** Where each flier's wingtips were last tick (left, right), by entity id, for the streams off them. */
	private static final Map<Integer, Vec3[]> TIPS = new HashMap<>();
	private static final Glimmer.Budget WAKE = new Glimmer.Budget(240);

	public static void tick(Minecraft mc) {
		WAKE.tick();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			TIPS.clear();
			return;
		}
		long now = level.getGameTime();
		Vec3 camera = mc.gameRenderer.mainCamera().position();
		for (AbstractClientPlayer flier : level.players()) {
			WildercordAttachments.Soaring note = flier.getAttached(WildercordAttachments.SOARING);
			boolean own = flier == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
			if (note == null || note.falling() || note.until() <= now || own || flier.isInvisible() || flier.isSpectator()
				|| flier.position().distanceToSqr(camera) > RANGE * RANGE) {
				TIPS.remove(flier.getId());
				continue;
			}
			long left = note.until() - now;
			boolean aloft = !flier.onGround();
			Frame frame = new Frame(flier, now, aloft);
			if ((now + flier.getId()) % PERIOD == 0 && visible(left, now)) {
				wings(mc, level, flier, frame, strokes(left, aloft));
			}
			wake(mc, level, flier, frame, aloft);
		}
		if (now % 200 == 0) {
			TIPS.keySet().removeIf(id -> level.getEntity(id) == null);
		}
	}

	/** Fading, the wings flicker: now and then a set is left out. */
	private static boolean visible(long left, long now) {
		return left > SoarRules.WARNING_TICKS || now / PERIOD % 3 != 0;
	}

	/** Strokes to a wing: three aloft, two folded on the ground, and fewer as the flight fades. */
	private static int strokes(long left, boolean aloft) {
		int full = aloft ? 3 : 2;
		if (left > SoarRules.WARNING_TICKS) {
			return full;
		}
		return Math.max(1, full - (int) ((SoarRules.WARNING_TICKS - left) / 25));
	}

	/** A flier's body as the wings see it this tick: which way it faces, where the wings grow from, and the beat. */
	private record Frame(Vec3 forward, Vec3 right, Vec3 root, double beat) {
		Frame(Player flier, long now, boolean aloft) {
			this(forward(flier), right(flier), SoarVfx.root(flier), beat(flier, now, aloft));
		}

		private static Vec3 forward(Player flier) {
			double yaw = Math.toRadians(flier.yBodyRot);
			return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		}

		private static Vec3 right(Player flier) {
			Vec3 f = forward(flier);
			return new Vec3(-f.z, 0, f.x);
		}

		/** A slow beat, quicker on the move, and each flier on its own phase. */
		private static double beat(Player flier, long now, boolean aloft) {
			double speed = new Vec3(flier.getX() - flier.xo, flier.getY() - flier.yo, flier.getZ() - flier.zo).length();
			double rate = aloft ? 0.18 + Math.min(0.2, speed * 0.4) : 0.08;
			return Math.sin(now * rate + flier.getId() * 1.7) * (aloft ? 0.14 : 0.05);
		}

		/** The direction {@code angle} radians above level, out to one side ({@code side} -1 left, 1 right), in the plane of the back. */
		Vec3 out(int side, double angle) {
			return right.scale(side * Math.cos(angle)).add(0, Math.sin(angle), 0);
		}
	}

	/** The wings: both sides' strokes (see {@link SoarVfx#wing}), each carried along with the flier. */
	private static void wings(Minecraft mc, ClientLevel level, Player flier, Frame frame, int strokes) {
		for (int side = -1; side <= 1; side += 2) {
			for (SoarVfx.Stroke stroke : SoarVfx.wing(frame.root, frame.forward, side, frame.beat, flier.onGround(), strokes, 1, LIFE)) {
				mc.particleEngine.add(new Feather(level, stroke.centre(), stroke.light(), flier));
			}
		}
	}

	/** Where a wing's leading edge ends, the wind streams off (steadier than the beat, so the stream runs smooth). */
	private static Vec3 tip(Frame frame, int side) {
		return frame.root.add(frame.out(side, 0.7 + frame.beat * 0.5).scale(1.3));
	}

	/** The wake: two soft streams of specks off the wingtips, and little crescents of wind curling away behind. */
	private static void wake(Minecraft mc, ClientLevel level, Player flier, Frame frame, boolean aloft) {
		Vec3 motion = new Vec3(flier.getX() - flier.xo, flier.getY() - flier.yo, flier.getZ() - flier.zo);
		double speed = motion.length();
		Vec3[] was = TIPS.get(flier.getId());
		Vec3[] tips = {tip(frame, -1), tip(frame, 1)};
		TIPS.put(flier.getId(), tips);
		if (!aloft || speed < WAKE_SPEED || was == null) {
			return;
		}
		RandomSource random = flier.getRandom();
		Vec3 back = motion.scale(-0.08 / speed);
		for (int i = 0; i < 2; i++) {
			Vec3 step = tips[i].subtract(was[i]);
			if (step.lengthSqr() > 9) {
				continue;
			}
			// A soft stream of specks along the way the wingtip came, drifting back and a little down as they fade.
			for (int k = 0; k < 2 && WAKE.hasRoom(); k++) {
				Vec3 at = was[i].add(step.scale(0.25 + 0.5 * k));
				int life = 14 + random.nextInt(8);
				mc.particleEngine.add(Glimmer.mote(level, at, k == 0 ? SKY : 0xF2FCFF, 0.07F + random.nextFloat() * 0.04F, 0.6F, life,
					back.x, back.y - 0.006, back.z, 0.002F));
				WAKE.spend(life);
			}
		}
		if ((level.getGameTime() + flier.getId()) % 4 == 0 && WAKE.hasRoom()) {
			Vec3 dir = motion.scale(1 / speed);
			Vec3 at = frame.root.subtract(dir.scale(0.7)).add((random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.5,
				(random.nextDouble() - 0.5) * 0.6);
			Vec3 toward = ElementFx.inPlane(dir, random.nextDouble() * Mth.TWO_PI);
			mc.particleEngine.add(new LightParticle(level, at.x, at.y, at.z,
				ElementFx.slashOption(dir, toward, random.nextBoolean() ? MINT : SKY, 0.25 + 0.2 * random.nextDouble(), 2.2, 0.035, 2, 10)));
			WAKE.spend(10);
		}
	}

	/** One stroke of a wing, carried along with the flier it's on. */
	private static final class Feather extends LightParticle {
		private final Player flier;
		private final Vec3 offset;

		Feather(ClientLevel level, Vec3 at, LightOption option, Player flier) {
			super(level, at.x, at.y, at.z, option);
			this.flier = flier;
			this.offset = at.subtract(flier.position());
		}

		@Override
		public void tick() {
			super.tick();
			if (flier.isRemoved()) {
				remove();
				return;
			}
			Vec3 at = flier.position().add(offset);
			x = at.x;
			y = at.y;
			z = at.z;
		}
	}
}
