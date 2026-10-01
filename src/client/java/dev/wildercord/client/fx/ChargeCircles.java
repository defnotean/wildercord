package dev.wildercord.client.fx;

import dev.wildercord.cast.Charging;
import dev.wildercord.content.SpellCircleOption;
import dev.wildercord.player.Heart;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * The magic circle a caster holds out while charging a spell, drawn on every client from the synced
 * charge: the spell's own circle (see {@link SpellCircleParticle}), opening as the charge builds, so
 * its roundels appear one by one and it flares when full. It floats behind the caster's shoulders,
 * leaving the view and the path of the spell clear.
 *
 * <p>Held past full, it overchannels (see {@link dev.wildercord.spell.Overchannel}): each stage lands
 * with a swell, a ring of sparks and a crackle across its face, and cracks it further; it trembles
 * more and throws sparks off its rim the higher it climbs, and at its last stage it reddens as the
 * moment it would tear loose comes on.</p>
 */
public final class ChargeCircles {
	private ChargeCircles() {}

	/** Which charges already have their circle, by entity id: the charge's start time. */
	private static final Map<Integer, Long> SHOWN = new HashMap<>();
	/** How many notes of each charging caster's melody have played, by entity id. */
	private static final Map<Integer, Integer> SUNG = new HashMap<>();

	/** The circle's radius behind the shoulders. */
	private static final float RADIUS = 0.42F;

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null) {
			SHOWN.clear();
			return;
		}
		SHOWN.keySet().removeIf(id -> {
			Entity e = level.getEntity(id);
			return !(e instanceof Player p) || !p.hasAttached(WildercordAttachments.CHARGE);
		});
		SUNG.keySet().retainAll(SHOWN.keySet());
		for (Player player : level.players()) {
			WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
			if (charge != null && SHOWN.containsKey(player.getId())) {
				sing(level, player, charge);
			}
		}
		for (Player player : level.players()) {
			WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
			if (charge == null) {
				continue;
			}
			Long shown = SHOWN.get(player.getId());
			if (shown != null && shown == charge.start()) {
				continue;
			}
			SHOWN.put(player.getId(), charge.start());
			SUNG.put(player.getId(), 0);
			mc.particleEngine.add(new Circle(level, player, charge, color(player, charge), size(charge)));
		}
	}

	/**
	 * The spell's melody: as each rune's roundel opens on the circle, its note plays (a knock for a shape, a glass pluck for an
	 * effect, a bell for a modifier, a clink for a link), on a degree of the scale that is the rune's own. Every spell has a tune.
	 */
	private static void sing(ClientLevel level, Player player, WildercordAttachments.Charge charge) {
		int n = charge.runes().size();
		if (n == 0) {
			return;
		}
		double progress = Charging.progress(player, charge, level.getGameTime());
		int sung = SUNG.getOrDefault(player.getId(), 0);
		while (sung < n && progress >= 0.35 + 0.55 * sung / n) {
			note(level, player, charge.runes().get(sung), sung);
			sung++;
		}
		SUNG.put(player.getId(), sung);
	}

	/** One rune's note, at the caster. */
	public static void note(ClientLevel level, Player player, String id, int index) {
		RuneDef rune = Runes.get(id).orElse(null);
		if (rune == null) {
			return;
		}
		String sound = switch (rune.family()) {
			case SHAPE -> "note_shape";
			case MODIFIER -> "note_mod";
			case LINK -> "note_link";
			default -> "note_effect";
		};
		net.minecraft.sounds.SoundEvent event = dev.wildercord.content.WildercordSounds.kit(sound);
		if (event == null) {
			return;
		}
		// The rune's own degree of the scale, and an octave up for the second half of a long spell.
		int degree = Math.floorMod(rune.id().hashCode(), 5) + (index >= 6 ? 5 : 0);
		level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), event, net.minecraft.sounds.SoundSource.PLAYERS, 0.35F,
			dev.wildercord.cast.feel.Feels.step(degree), false);
	}

	/** How big a charging circle is drawn: by the spell's scale (a costly, high-tier spell opens a bigger one). */
	private static float size(WildercordAttachments.Charge charge) {
		java.util.List<RuneDef> runes = new java.util.ArrayList<>();
		int tier = 1;
		for (String id : charge.runes()) {
			RuneDef rune = Runes.get(id).orElse(null);
			if (rune != null) {
				runes.add(rune);
				tier = Math.max(tier, rune.tier());
			}
		}
		double cost = runes.isEmpty() ? 0 : dev.wildercord.spell.SpellCompiler.compile(runes).cost();
		return switch (dev.wildercord.cast.feel.Band.of(dev.wildercord.cast.feel.Feel.scaleOf(cost, 0, tier))) {
			case S -> 0.85F;
			case M -> 1.0F;
			case L -> 1.2F;
			case XL -> 1.4F;
		};
	}

	/** The spell's first effect's element, or "". */
	public static String element(WildercordAttachments.Charge charge) {
		for (String id : charge.runes()) {
			RuneDef rune = Runes.get(id).orElse(null);
			if (rune != null && rune.family() == RuneFamily.EFFECT) {
				return rune.element();
			}
		}
		return "";
	}

	/** The spell's first element; with none, the caster's leaning; else gold. */
	private static int color(Player player, WildercordAttachments.Charge charge) {
		for (String id : charge.runes()) {
			RuneDef rune = Runes.get(id).orElse(null);
			if (rune != null && rune.family() == RuneFamily.EFFECT) {
				return RuneColors.of(rune);
			}
		}
		String leaning = Heart.leaning(player);
		return leaning.isEmpty() ? 0xF5D56A : RuneColors.element(leaning);
	}

	/** A charging caster's circle, following their hand. */
	private static final class Circle extends SpellCircleParticle {
		private final Player caster;
		private final long start;
		private final float scale;
		private int fading = -1;
		/** The overchannel stage the circle shows and when it landed, and the charge as last seen (for its stages and timing). */
		private int stage;
		private long stageTime;
		private WildercordAttachments.Charge seen;
		/** Each crack's place and wander, made once from the charge's start, so every client draws the same cracks. */
		private final float[] crackAngles = new float[12];
		private final float[] crackKinks = new float[12 * 4];

		Circle(ClientLevel level, Player caster, WildercordAttachments.Charge charge, int color, float scale) {
			super(level, caster.getX(), caster.getEyeY(), caster.getZ(), new SpellCircleOption(charge.runes(), color, RADIUS, 0, 0, 20 * 20));
			this.caster = caster;
			this.start = charge.start();
			this.scale = scale;
			this.seen = charge;
			java.util.Random cracks = new java.util.Random(charge.start() * 31 + caster.getId());
			for (int i = 0; i < crackAngles.length; i++) {
				// Spread round the circle: each stage's four cracks fall between the last stage's.
				crackAngles[i] = Mth.TWO_PI * ((i % 4) / 4F + (i / 4) / 12F) + (cracks.nextFloat() - 0.5F) * 0.35F;
			}
			for (int i = 0; i < crackKinks.length; i++) {
				crackKinks[i] = cracks.nextFloat() * 2 - 1;
			}
			move(0);
			xo = x;
			yo = y;
			zo = z;
		}

		private boolean firstPerson() {
			Minecraft mc = Minecraft.getInstance();
			return caster == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
		}

		private double progress(float partial) {
			return Mth.clamp((caster.level().getGameTime() - start + partial) / (double) Charging.fullTicks(caster), 0, 1);
		}

		/** Behind the caster's shoulders, facing the same direction as the spell. */
		private Vec3 anchor(float partial) {
			Vec3 eye = caster.getEyePosition(partial);
			Vec3 look = caster.getViewVector(partial);
			return eye.subtract(look.scale(1.55)).add(0, -0.3, 0);
		}

		private void move(float partial) {
			Vec3 at = anchor(partial);
			x = at.x;
			y = at.y;
			z = at.z;
		}

		@Override
		public void tick() {
			super.tick();
			age = Math.min(age, 10);
			WildercordAttachments.Charge charge = caster.getAttached(WildercordAttachments.CHARGE);
			boolean still = !caster.isRemoved() && charge != null && charge.start() == start;
			if (!still && fading < 0) {
				fading = 5;
			}
			if (fading >= 0 && fading-- == 0) {
				remove();
			}
			move(1);
			if (still) {
				seen = charge;
				if (charge.stage() > stage) {
					stage = charge.stage();
					stageTime = charge.stageTime();
					landed();
				}
				if (stage > 0 && fading < 0) {
					sparks();
				}
			}
		}

		/** How near the channel is to tearing loose at its last stage, 0 to 1 (0 short of it). */
		private float tension(float partial) {
			if (seen == null || seen.stages() <= 0 || stage < seen.stages()) {
				return 0;
			}
			return Mth.clamp((caster.level().getGameTime() - stageTime + partial) / (float) dev.wildercord.spell.Overchannel.GRACE, 0, 1);
		}

		/** A point on the circle's rim in the world, at angle {@code a}. */
		private Vec3 rim(float a, float partial) {
			org.joml.Vector3f p = orientation(partial).transform(new org.joml.Vector3f(Mth.cos(a), Mth.sin(a), 0).mul(size(partial)));
			return centre(partial).add(p.x, p.y, p.z);
		}

		/** A stage lands: a ring of sparks flies off the rim and a crackle of light runs across the face. */
		private void landed() {
			Minecraft mc = Minecraft.getInstance();
			ClientLevel level = (ClientLevel) caster.level();
			Vec3 c = centre(1);
			int n = 8 + 4 * stage;
			for (int i = 0; i < n; i++) {
				float a = Mth.TWO_PI * i / n + random.nextFloat() * 0.3F;
				Vec3 at = rim(a, 1);
				Vec3 out = at.subtract(c).normalize().scale(0.06 + random.nextDouble() * 0.05);
				mc.particleEngine.add(Glimmer.mote(level, at, i % 3 == 0 ? 0xFFFFFF : lighter(color, 0.4F), 0.07F + random.nextFloat() * 0.04F,
					0.95F, 8 + random.nextInt(6), out.x, out.y + 0.01, out.z, 0.01F));
			}
			for (int k = 0; k < stage; k++) {
				Vec3 a = rim(random.nextFloat() * Mth.TWO_PI, 1);
				Vec3 b = rim(random.nextFloat() * Mth.TWO_PI, 1);
				Vec3 d = b.subtract(a);
				mc.particleEngine.add(new LightParticle(level, a.x, a.y, a.z, new dev.wildercord.content.LightOption(
					dev.wildercord.content.LightOption.ARC, lighter(color, 0.6F), (float) d.x, (float) d.y, (float) d.z, 0.012F, 1, 0, 0.6F, 4)));
			}
		}

		/** Sparks thrown off the rim: more the higher it climbs, and more again as it nears tearing loose. */
		private void sparks() {
			float tension = tension(0);
			float rate = 0.35F * stage * stage * (1 + 2 * tension);
			Minecraft mc = Minecraft.getInstance();
			ClientLevel level = (ClientLevel) caster.level();
			Vec3 c = centre(1);
			for (float left = rate; left > 0; left -= 1) {
				if (left < 1 && random.nextFloat() > left) {
					break;
				}
				Vec3 at = rim(random.nextFloat() * Mth.TWO_PI, 1);
				Vec3 out = at.subtract(c).normalize().scale(0.03 + random.nextDouble() * 0.04);
				int tint = tension > 0.5F && random.nextBoolean() ? 0xFF5A3A : random.nextInt(3) == 0 ? 0xFFFFFF : lighter(color, 0.45F);
				mc.particleEngine.add(Glimmer.mote(level, at, tint, 0.05F + random.nextFloat() * 0.03F, 0.9F, 6 + random.nextInt(6),
					out.x, out.y - 0.004, out.z, 0.015F));
			}
		}

		/** The cracks of every stage it has climbed, each running in from the frame as its stage lands. */
		@Override
		protected void extras(float r, float a, float fine, float partial) {
			if (stage <= 0) {
				return;
			}
			float since = caster.level().getGameTime() - stageTime + partial;
			float tension = tension(partial);
			int hot = lighter(color, 0.7F);
			for (int s = 0; s < stage; s++) {
				// The newest stage's cracks run in over three ticks; the older ones are already there.
				float grown = s < stage - 1 ? 1 : Mth.clamp(since / 3F, 0, 1);
				float flicker = 0.75F + 0.25F * Mth.sin((age + partial) * 1.7F + s * 2.1F);
				int ink = argb(a * (0.55F + 0.45F * flicker), tension > 0 ? mix(hot, 0xFF5A3A, tension) : hot);
				for (int k = 0; k < 4; k++) {
					crack(r, s * 4 + k, grown, fine * 1.4F, ink);
				}
			}
			if (tension > 0) {
				// The frame itself reddening as it strains.
				ring(0, 0, r * dev.wildercord.spell.SpellSigil.FRAME, fine * 2.2F, argb(a * tension * 0.8F, 0xFF5A3A), 0.009F);
			}
		}

		/** One crack: in from the frame toward the middle, kinking as it goes, with a short fork off its second kink. */
		private void crack(float r, int i, float grown, float width, int ink) {
			float angle = crackAngles[i];
			float outer = r * dev.wildercord.spell.SpellSigil.FRAME;
			float inner = r * (0.38F + 0.12F * Math.abs(crackKinks[i * 4]));
			float length = (outer - inner) * grown;
			if (length <= 0) {
				return;
			}
			float pu = Mth.cos(angle) * outer;
			float pv = Mth.sin(angle) * outer;
			float nu = -Mth.sin(angle);
			float nv = Mth.cos(angle);
			for (int k = 1; k <= 3; k++) {
				float d = outer - length * k / 3F;
				float off = crackKinks[i * 4 + k] * r * 0.06F;
				float qu = Mth.cos(angle) * d + nu * off;
				float qv = Mth.sin(angle) * d + nv * off;
				line(pu, pv, qu, qv, width * (1.2F - 0.25F * k), ink, 0.0085F);
				if (k == 2) {
					float fork = angle + 0.5F * Math.signum(crackKinks[i * 4 + 1] + 0.01F);
					line(qu, qv, qu - Mth.cos(fork) * length * 0.3F, qv - Mth.sin(fork) * length * 0.3F, width * 0.6F, ink, 0.0085F);
				}
				pu = qu;
				pv = qv;
			}
		}

		private static int mix(int a, int b, float t) {
			int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
			int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
			int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
			return (r << 16) | (g << 8) | bl;
		}

		@Override
		protected float opening(float partial) {
			return (float) progress(partial);
		}

		@Override
		protected float fade(float partial) {
			float leaving = fading >= 0 ? Math.max(0, (fading - partial) / 5F) : 1F;
			return Math.min(1, (age + partial) / 2F) * leaving;
		}

		@Override
		protected float size(float partial) {
			// A flare when the charge is full, a little more for each overchannel stage, and a swell as one lands.
			float full = progress(partial) >= 1 ? 1.06F : 1F;
			float swell = 0;
			if (stage > 0) {
				float since = caster.level().getGameTime() - stageTime + partial;
				swell = 0.1F * Math.max(0, 1 - since / 8F);
			}
			return RADIUS * scale * (full + 0.03F * stage + swell);
		}

		/** How hard it trembles: more with each stage, and more again as it nears tearing loose. */
		private float tremble(float partial) {
			return stage <= 0 ? 0 : 0.0035F * stage * stage * (1 + 2.5F * tension(partial));
		}

		@Override
		protected Vec3 centre(float partial) {
			Vec3 at = anchor(partial);
			float t = tremble(partial);
			if (t <= 0) {
				return at;
			}
			float time = caster.level().getGameTime() + partial;
			return at.add(t * Mth.sin(time * 2.9F), t * Mth.sin(time * 3.7F + 1.3F), t * Mth.sin(time * 3.1F + 2.6F));
		}

		@Override
		protected Quaternionf orientation(float partial) {
			Quaternionf q = new Quaternionf().rotationYXZ((float) Math.toRadians(-caster.getViewYRot(partial)),
				(float) Math.toRadians(caster.getViewXRot(partial)), 0);
			float t = tremble(partial);
			if (t > 0) {
				float time = caster.level().getGameTime() + partial;
				q.rotateZ(t * 6 * Mth.sin(time * 2.3F));
			}
			return q;
		}
	}
}
