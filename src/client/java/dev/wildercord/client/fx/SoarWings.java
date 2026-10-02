package dev.wildercord.client.fx;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.SoarVfx;
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

/** Feathered flight wings rooted to each flier, with a small wind wake behind the tips. */
public final class SoarWings {
	private SoarWings() {}

	private static final double RANGE = 64;
	/** Faster than this (blocks a tick) a flier leaves a wake. */
	private static final double WAKE_SPEED = 0.08;

	/** The wake's colours: the pale sky of the lifting runes, and the mint of wind itself. */
	private static final int SKY = 0xBFE3FF;
	private static final int MINT = ElementFx.WIND.accent();

	/** One persistent wing model per visible flier. */
	private static final Map<Integer, SoarWingModel> MODELS = new HashMap<>();
	public static int showing() { return MODELS.size(); }

	/** Last wingtip positions for the wake. */
	private static final Map<Integer, Vec3[]> TIPS = new HashMap<>();
	private static final Glimmer.Budget WAKE = new Glimmer.Budget(240);

	public static void tick(Minecraft mc) {
		WAKE.tick();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			TIPS.clear();
			MODELS.values().forEach(SoarWingModel::remove);
			MODELS.clear();
			return;
		}
		MODELS.values().removeIf(model -> {
			if (!model.belongsTo(level)) model.remove();
			return !model.isAlive();
		});
		long now = level.getGameTime();
		Vec3 camera = mc.gameRenderer.mainCamera().position();
		for (AbstractClientPlayer flier : level.players()) {
			WildercordAttachments.Soaring note = flier.getAttached(WildercordAttachments.SOARING);
			boolean own = flier == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
			if (note == null || note.falling() || note.until() <= now || flier.isInvisible() || flier.isSpectator()
				|| flier.position().distanceToSqr(camera) > RANGE * RANGE) {
				TIPS.remove(flier.getId());
				continue;
			}
			SoarWingModel model = MODELS.get(flier.getId());
			if (model == null && MODELS.size() < 64) {
				model = new SoarWingModel(level, flier);
				MODELS.put(flier.getId(), model);
				mc.particleEngine.add(model);
			}
			boolean aloft = !flier.onGround();
			Frame frame = new Frame(flier, now, aloft);
			if (!own) wake(mc, level, flier, frame, aloft);
		}
		if (now % 200 == 0) {
			TIPS.keySet().removeIf(id -> level.getEntity(id) == null);
		}
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

}
