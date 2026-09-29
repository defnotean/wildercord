package dev.wildercord.cast;

import dev.wildercord.cast.feel.FeelCtx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * What the fire and blood runes look and sound like, one voice per rune (docs/audit/spell-feel-fire-blood.md, Appendix C).
 * The rules that kept every display from being the same one: a rune has one silhouette, one timing (anticipation, impact,
 * aftermath) and one kit sound from {@code tools/feel/fire.py} or {@code blood.py}; a crowd never multiplies the show (see
 * {@link #budget}); anything timed ends with a cue; and a mechanic the player can't see (a stack, a lock-out, a shield, a
 * pain tier) has a tell.
 *
 * <p>Fire is dry and quick: hard flashes, standing flames, a crack. Blood is wet and close: thin cuts, drips, a heartbeat.
 * Everything goes out through {@link Fx}, so a passive that renews quietly stays quiet.</p>
 */
public final class FireBloodVfx {
	private FireBloodVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int GOLD = 0xFFD060;
	private static final int WHITE = 0xFFFFFF;
	private static final int WHITE_GOLD = 0xFFF3D0;
	private static final int FUSE_PINK = 0xFF6EC7;
	private static final int ASH = 0x8A8480;
	private static final int SOUL = 0x5AD8E6;
	private static final int STEAM_WHITE = 0xE6FAFF;
	private static final int MOSS = 0x5E8A3A;
	private static final int DARK_RED = 0xC03010;

	/** The pentatonic ratios a rising ladder of one sample is played at (see docs/ADDING_RUNES.md). */
	static final float[] LADDER = {1.0F, 1.122F, 1.26F, 1.498F, 1.682F, 2.0F};

	private static Vec3 centre(Entity e) {
		return e.getBoundingBox().getCenter();
	}

	private static double width(Entity e) {
		return Math.max(0.4, e.getBbWidth());
	}

	private static void snd(ServerLevel level, Vec3 at, String kit, float volume, float pitch) {
		Feels.sound(level, at, kit, volume, pitch);
	}

	private static int mix(int a, int b, double t) {
		t = Math.max(0, Math.min(1, t));
		int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
		return r << 16 | g << 8 | bl;
	}

	private static final Map<String, long[]> BUDGETS = new HashMap<>();

	/**
	 * Whether one more of {@code key} may be shown this tick (at most {@code max} a tick): a crowd of thirty burning mobs
	 * gets a handful of licks of flame, not thirty.
	 */
	public static boolean budget(ServerLevel level, String key, int max) {
		long now = level.getGameTime();
		long[] slot = BUDGETS.computeIfAbsent(key, k -> new long[] {-1, 0});
		if (slot[0] != now) {
			slot[0] = now;
			slot[1] = 0;
		}
		return ++slot[1] <= max;
	}

	// ================================================================== fire

	/** Ember: one cinder flies in from the side and lodges in the body, a gold core that glows a moment; a stoked one flares. */
	public static void ember(ServerLevel level, Entity t, boolean stoked) {
		Vec3 c = centre(t);
		if (!budget(level, "ember.show", 8)) {
			Motes.glow(level, c, GOLD, 0.4, 12, Vec3.ZERO, 0.0);
			return;
		}
		Vec3 from = c.add(ElementFx.flatDir(level.getRandom().nextDouble() * Math.PI * 2).scale(1.1)).add(0, 0.3, 0);
		Motes.seek(level, from, c, GOLD, 0.16, 6, 0.4);
		Motes.glow(level, c, GOLD, stoked ? 0.75 : 0.45, stoked ? 26 : 16, Vec3.ZERO, 0.0);
		Vfx.emit(level, ParticleTypes.SMALL_FLAME, c, 3, 0.15, 0.02);
		if (stoked) {
			ElementFx.flames(level, t.position(), width(t) * 0.6, t.getBbHeight() * 0.7, 2);
		}
		snd(level, c, "fire_flick", 0.9F, 1.0F);
	}

	/** Fire: it catches with a low whump, flames climb the body, lick up again over the burn, and a sigh when it goes out. */
	public static void fire(ServerLevel level, Entity t) {
		Vec3 base = t.position();
		double w = Math.max(0.35, t.getBbWidth() * 0.6);
		double h = t.getBbHeight();
		if (!budget(level, "fire.show", 8)) {
			// A crowd catching at once: a lick of flame each, not the whole show.
			ElementFx.flames(level, base, w, h, 1);
			return;
		}
		ElementFx.heatFlare(level, centre(t), 0.9);
		ElementFx.flames(level, base, w, h, 4);
		Scheduler.later(3, () -> ElementFx.flames(level, t.position(), w, h, 3));
		Vfx.emit(level, ParticleTypes.SMALL_FLAME, centre(t), 3, w * 0.4, 0.02);
		snd(level, base, "fire_whump", 0.85F, 1.0F);
		if (t instanceof LivingEntity living && budget(level, "fire.watch", 6)) {
			watchBurn(level, living, 0);
		}
	}

	/** The burn, every second: a lick of flame while it lasts, a sigh and a curl of smoke the moment it goes out. */
	private static void watchBurn(ServerLevel level, LivingEntity t, int age) {
		Scheduler.later(20, () -> {
			if (!t.isAlive() || t.level() != level || age >= 200) {
				return;
			}
			if (!t.isOnFire()) {
				Motes.smoke(level, centre(t), 2, 0.25);
				snd(level, t.position(), "fire_out", 0.5F, 1.0F);
				return;
			}
			if (age % 40 == 20) {
				ElementFx.flames(level, t.position(), Math.max(0.35, t.getBbWidth() * 0.6), t.getBbHeight(), 2);
			}
			watchBurn(level, t, age + 20);
		});
	}

	/** Flashfire: a flat white-gold disc with a hard gold rim, out in a quarter of a second. No smoke, no tail. */
	public static void flashfire(ServerLevel level, Vec3 point, double radius) {
		Vec3 ground = CastEngine.ground(level, point.add(0, 0.5, 0));
		Sigils.flash(level, point.add(0, 0.4, 0), WHITE_GOLD, (float) Math.min(4, 0.8 + radius * 0.8));
		Light.groundRing(level, ground.add(0, 0.04, 0), WHITE_GOLD, 0.2, radius * 1.05, 0.24, 5);
		Light.groundRing(level, ground.add(0, 0.05, 0), GOLD, 0.3, radius * 1.15, 0.06, 6);
		Vfx.radial(level, ParticleTypes.FLAME, point, 6, 0.25);
		snd(level, point, "fire_flare", 1.0F, 1.0F);
	}

	/** One enemy caught by a Flashfire: a small spark, not a whole show. */
	public static void flashSpark(ServerLevel level, Entity t) {
		if (!budget(level, "flashfire.spark", 5)) {
			return;
		}
		Vec3 c = centre(t);
		Motes.glow(level, c, GOLD, 0.6, 8, Vec3.ZERO, 0.0);
		Vfx.emit(level, ParticleTypes.SMALL_FLAME, c, 2, 0.2, 0.02);
	}

	/**
	 * A blast: a white heart, a hard shell (three great circles, orange to white) out in eight ticks, a ring of dust along the
	 * ground and a wisp of smoke after. The first blast of a cast also shakes the ground; the ones after it are lighter.
	 */
	public static void blast(ServerLevel level, Vec3 c, double radius, boolean lead, String sound, float pitch) {
		if (lead) {
			ScreenFx.shake(level, c, (float) Math.min(1, 0.3 + radius * 0.12), radius * 5 + 6);
		}
		Sigils.flash(level, c, WHITE, (float) Math.min(4, 1.0 + radius * 0.55));
		double spin = level.getRandom().nextDouble() * Math.PI;
		ElementFx.ring(level, c, new Vec3(Math.cos(spin), 0, Math.sin(spin)), ElementFx.FIRE.primary(), 0.3, radius, 0.09, 8);
		ElementFx.ring(level, c, new Vec3(Math.cos(spin + Math.PI / 2), 0, Math.sin(spin + Math.PI / 2)), ElementFx.FIRE.secondary(), 0.3, radius, 0.07, 8);
		ElementFx.ring(level, c, UP, WHITE, 0.3, radius, 0.05, 8);
		Vec3 floor = ElementFx.floor(level, c, radius + 1);
		if (floor != null) {
			ElementFx.groundRing(level, floor, 0x9A8A78, 0.4, radius * 1.25, 0.12, 12);
		}
		Vfx.radial(level, ParticleTypes.FLAME, c, lead ? 10 : 5, 0.3);
		Motes.clouds(level, c, lead ? 3 : 2, radius * 0.3, Motes.SMOKE, 1.0 + radius * 0.2, 40, new Vec3(0, 0.04, 0), 0.06, 0.4);
		snd(level, c, sound, lead ? 1.0F : 0.7F, pitch);
	}

	/** Meteor, the fall: the reticle closes over 1.2 s while the rock streaks down out of the sky, a roar climbing with it. */
	public static void meteorFall(ServerLevel level, Vec3 ground, int fall) {
		Vec3 start = ground.add(-6, 18, -3);
		Vec3 path = ground.subtract(start);
		Sigils.target(level, ground, ElementFx.FIRE.primary(), 1.8F, fall + 4);
		Light.groundRing(level, ground.add(0, 0.05, 0), ElementFx.FIRE.secondary(), 3.4, 0.9, 0.06, fall);
		snd(level, ground, "fire_meteor_fall", 1.1F, 1.0F);
		for (int t = 0; t < fall; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				Vec3 p = start.add(path.scale((tick + 1) / (double) fall));
				Vec3 back = start.add(path.scale(Math.max(0, tick - 2) / (double) fall));
				ElementFx.orb(level, p, ElementFx.FIRE.secondary(), 0.4, 2);
				ElementFx.ray(level, back, p, ElementFx.FIRE.primary(), 0.3, 6);
				if (tick % 2 == 0) {
					Vfx.emit(level, ParticleTypes.FLAME, p, 3, 0.3, 0.02);
				}
				if (tick % 3 == 0) {
					Motes.smoke(level, back, 1, 0.3);
				}
			});
		}
	}

	/** Meteor, the landing: one great blow, and a molten scar that glows where it fell for three seconds. */
	public static void meteorLand(ServerLevel level, Vec3 ground, double radius) {
		blast(level, ground.add(0, 0.5, 0), radius, true, "fire_meteor_hit", 1.0F);
		ScreenFx.shake(level, ground, 0.9F, 26);
		ElementFx.flatSigil(level, ground.add(0, 0.06, 0), SigilOption.CRACKED, DARK_RED, radius * 0.9, 60, 0.0);
		Light.groundRing(level, ground.add(0, 0.05, 0), ElementFx.FIRE.accent(), radius * 0.15, radius * 0.7, 0.3, 50);
		Vfx.emit(level, ParticleTypes.LAVA, ground.add(0, 0.4, 0), 3, radius * 0.3, 0.0);
	}

	/** The crater burning on: a few low flames and, when it has burned down, a sigh of smoke. */
	public static void craterPulse(ServerLevel level, Vec3 point, double radius, boolean last) {
		for (int i = 0; i < 3; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			double r = Math.sqrt(level.getRandom().nextDouble()) * radius * 0.8;
			ElementFx.flames(level, point.add(Math.cos(a) * r, 0, Math.sin(a) * r), 0.22, 0.6, 1);
		}
		if (last) {
			Motes.smoke(level, point.add(0, 0.5, 0), 3, radius * 0.4);
			snd(level, point, "fire_out", 0.7F, 0.8F);
		}
	}

	/** Inferno, one pulse: a standing ring of low flames on the rim that breathes with a low roar; a sigh when the last has gone. */
	public static void infernoPulse(ServerLevel level, Vec3 point, double radius, int index, int total) {
		int flames = (int) Math.min(8, Math.max(4, Math.round(radius * 2)));
		double phase = index * 0.7;
		for (int i = 0; i < flames; i++) {
			double a = phase + Math.PI * 2 * i / flames;
			ElementFx.flames(level, point.add(Math.cos(a) * radius * 0.92, 0, Math.sin(a) * radius * 0.92), 0.22, 0.75, 1);
		}
		Light.groundRing(level, point.add(0, 0.05, 0), ElementFx.FIRE.primary(), radius * 0.85, radius, 0.09, 20);
		if (index == 0) {
			Light.groundRing(level, point.add(0, 0.06, 0), GOLD, radius * 0.3, radius * 0.9, 0.05, 14);
		}
		snd(level, point, "fire_field", 0.8F, 1.0F);
		if (index == total - 1) {
			Motes.smoke(level, point.add(0, 0.5, 0), 3, radius * 0.4);
			snd(level, point, "fire_out", 0.8F, 0.9F);
		}
	}

	/** Primer, lit: the target flares pink and a fuse ring starts to close on it. */
	public static void fuse(ServerLevel level, Entity t) {
		Vec3 c = centre(t);
		Sigils.flash(level, c, FUSE_PINK, 1.0F);
		ElementFx.ring(level, c, UP, FUSE_PINK, width(t) + 1.2, width(t) * 0.5 + 0.3, 0.05, 8);
		snd(level, c, "fire_fuse", 0.9F, 1.0F);
	}

	/**
	 * Primer, a beat of the fuse (every 6th tick of 40): the ring closes tighter and turns from pink through orange to white, and
	 * the tick climbs the scale. The last tick is at 36; the four ticks after it are silent, and then it goes off.
	 */
	public static void fuseTick(ServerLevel level, Entity t, int tick, int total) {
		double f = Math.max(0, Math.min(1, tick / (double) total));
		Vec3 c = centre(t);
		int colour = f < 0.6 ? mix(FUSE_PINK, ElementFx.FIRE.primary(), f / 0.6) : mix(ElementFx.FIRE.primary(), WHITE, (f - 0.6) / 0.4);
		double r = (width(t) * 0.5 + 0.35) + (width(t) + 0.8) * (1 - f);
		ElementFx.ring(level, c, UP, colour, r + 0.5, r, 0.045, 6);
		Motes.glow(level, t.position().add(0, t.getBbHeight() + 0.25, 0), colour, 0.3 + 0.25 * f, 6, Vec3.ZERO, 0.0);
		snd(level, c, "fire_fuse_tick", 0.7F, LADDER[Math.max(0, Math.min(LADDER.length - 1, tick / 6 - 1))]);
	}

	/** Kindling, a stack: one gold ember more circling over the head, and the tick a step higher up the scale. */
	public static void kindle(ServerLevel level, Entity t, int stacks, boolean quiet) {
		Vec3 head = t.position().add(0, t.getBbHeight() + 0.45, 0);
		ElementFx.orbit(level, head, 0.4 + 0.03 * stacks, stacks, 26, GOLD, ElementFx.FIRE.primary());
		Sigils.flash(level, centre(t), ElementFx.FIRE.secondary(), 0.6F);
		if (!quiet) {
			snd(level, centre(t), "fire_stack", 0.7F + 0.06F * stacks, LADDER[Math.max(0, Math.min(LADDER.length - 1, stacks - 1))]);
		}
	}

	/** Kindling, the fifth stack: the ring of embers bursts, a high crack, a ring across the ground. */
	public static void kindleBurst(ServerLevel level, Entity t, double radius) {
		Vec3 c = centre(t);
		ElementFx.fireImpact(level, c, 1.8);
		ElementFx.groundRing(level, t.position().add(0, 0.05, 0), ElementFx.FIRE.primary(), 0.3, radius, 0.1, 9);
		Vfx.radial(level, ParticleTypes.SMALL_FLAME, c.add(0, 0.4, 0), 8, 0.3);
		snd(level, c, "fire_blast", 0.9F, 1.35F);
	}

	/** Steam, on a creature it scalds: a pale dome edge and a billow of white, a long soft hiss. */
	public static void steam(ServerLevel level, Entity t) {
		Vec3 c = centre(t);
		Light.ring(level, c, UP, STEAM_WHITE, 0.1, width(t) + 1.0, 0.07, 8);
		Motes.clouds(level, c, 4, width(t) * 0.5, Motes.STEAM, 1.5, 46, new Vec3(0, 0.04, 0), 0.04, 0.55);
		ElementFx.heatFlare(level, c, 0.6);
		snd(level, c, "fire_steam", 0.9F, 1.0F);
	}

	/** Firestorm, the first fire on a creature: a whirl of orange over wind-white, and a low catch. No gust ring, no wall of flames. */
	public static void firestorm(ServerLevel level, Entity t) {
		Vec3 feet = t.position();
		ElementFx.swirl(level, feet, Math.max(0.6, t.getBbWidth()), t.getBbHeight() + 0.5, 3, ElementFx.FIRE.primary(), 0xE6F0FF);
		ElementFx.flames(level, feet, Math.max(0.5, t.getBbWidth() * 0.7), t.getBbHeight(), 4);
		Sigils.flash(level, centre(t), ElementFx.FIRE.primary(), 1.2F);
		snd(level, feet, "fire_whump", 0.8F, 1.1F);
	}

	/** Firestorm, the fire leaping from one burning creature to the next: an arc, a lick, a pop. */
	public static void firestormHop(ServerLevel level, Entity from, Entity to) {
		ElementFx.arc(level, centre(from), centre(to), ElementFx.FIRE.primary(), 0.05, 1, false, 6);
		ElementFx.flames(level, to.position(), Math.max(0.4, to.getBbWidth() * 0.6), to.getBbHeight() * 0.8, 3);
		snd(level, centre(to), "fire_hop", 0.6F, 1.0F);
	}

	/** Sunscorch: a lens ring closes down the beam, then the white column drops, heavier under an open sky. */
	public static void sunscorch(ServerLevel level, LivingEntity t, boolean sunlit) {
		Vec3 at = centre(t);
		Vec3 top = at.add(0, sunlit ? 12 : 6, 0);
		ElementFx.ring(level, at.add(0, sunlit ? 4 : 2.5, 0), UP, WHITE_GOLD, 1.4, 0.2, 0.05, 5);
		ElementFx.ray(level, top, t.position(), ElementFx.FIRE.secondary(), sunlit ? 0.35 : 0.22, 8);
		ElementFx.ray(level, top, t.position(), WHITE, 0.08, 6);
		ElementFx.flatSigil(level, t.position(), SigilOption.STAR, ElementFx.FIRE.secondary(), 1.6, 12, 0.2);
		ElementFx.fireImpact(level, at, sunlit ? 1.1 : 0.8);
		snd(level, at, "fire_sun", sunlit ? 1.0F : 0.8F, sunlit ? 1.0F : 0.85F);
	}

	/** Soulfire, first lit: blue tongues and a flare, a hollow whisper. */
	public static void soulfireLit(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position();
		ElementFx.tongues(level, feet, t.getBbWidth() * 0.6, t.getBbHeight(), 4, SOUL, 0xB0F4FF, 5, 10);
		ElementFx.flatSigil(level, feet, SigilOption.CIRCLE, SOUL, 1.4, 14, 0.2);
		Vfx.emit(level, ParticleTypes.SOUL_FIRE_FLAME, feet.add(0, t.getBbHeight() * 0.5, 0), 8, 0.3, 0.02);
		snd(level, feet, "fire_soul", 0.9F, 1.0F);
	}

	/** Soulfire, each second it burns: a couple of tongues and a soul rising. Quiet. */
	public static void soulfireBurn(ServerLevel level, LivingEntity t) {
		ElementFx.tongues(level, t.position(), t.getBbWidth() * 0.6, t.getBbHeight(), 2, SOUL, 0xB0F4FF, 5, 10);
		Vfx.emit(level, ParticleTypes.SOUL, t.position().add(0, t.getBbHeight(), 0), 1, 0.2, 0.02);
	}

	/** Soulfire paid the caster some mana: a cyan mote flies from the burning creature to them. */
	public static void soulfireRefund(ServerLevel level, LivingEntity t, LivingEntity caster) {
		Motes.seek(level, centre(t), centre(caster), SOUL, 0.14, 12, 0.8);
	}

	/** Soulfire, when it has burned out: the last wisps and a low sigh. */
	public static void soulfireOut(ServerLevel level, LivingEntity t) {
		Vfx.emit(level, ParticleTypes.SOUL, centre(t), 3, 0.25, 0.03);
		snd(level, t.position(), "fire_out", 0.5F, 0.8F);
	}

	/** Blazecall, one fireball: a round head streaking down its fan, a burst where it lands. */
	public static void fireball(ServerLevel level, LivingEntity t, int shot) {
		Vec3 at = centre(t);
		double a = shot * 2.1;
		Vec3 from = at.add(Math.cos(a) * 2.5, 6, Math.sin(a) * 2.5);
		ElementFx.ray(level, from, at, ElementFx.FIRE.primary(), 0.12, 5);
		ElementFx.orb(level, from.lerp(at, 0.55), ElementFx.FIRE.secondary(), 0.4, 3);
		ElementFx.fireImpact(level, at, 0.6);
		ElementFx.groundRing(level, t.position().add(0, 0.05, 0), 0xB09070, 0.2, width(t) + 0.5, 0.05, 5);
		snd(level, from, "fire_launch", 0.8F, 0.95F + 0.1F * shot);
	}

	/** Cinderbrand: a brand seared onto the body, a hiss, and a small glyph that hangs over the head for as long as it holds. */
	public static void brand(ServerLevel level, LivingEntity t) {
		Vec3 at = centre(t);
		ElementFx.flatSigil(level, t.position(), SigilOption.STAR, ElementFx.FIRE.accent(), 1.4, 20, 0.3);
		ElementFx.heatFlare(level, at, 0.8);
		ElementFx.embers(level, at, 0.4, 5);
		brandGlyph(level, t);
		snd(level, at, "fire_brand", 0.9F, 1.0F);
	}

	/** Cinderbrand's glyph over the head, renewed each second while the brand holds. */
	public static void brandGlyph(ServerLevel level, LivingEntity t) {
		Sigils.layer(level, t.position().add(0, t.getBbHeight() + 0.45, 0), UP, SigilOption.STAR, ElementFx.FIRE.accent(), 0.4F, 24, 0.15F);
	}

	/** Cinderbrand stoking a burn: a small tongue where the brand sits. Quiet. */
	public static void brandStoke(ServerLevel level, LivingEntity t) {
		if (budget(level, "brand.stoke", 4)) {
			ElementFx.flames(level, t.position(), width(t) * 0.5, t.getBbHeight() * 0.6, 1);
		}
	}

	/** The brand fading: the glyph goes out in a curl of smoke. */
	public static void brandOut(ServerLevel level, LivingEntity t) {
		Motes.smoke(level, t.position().add(0, t.getBbHeight() + 0.45, 0), 1, 0.15);
		snd(level, t.position(), "fire_out", 0.35F, 1.2F);
	}

	/** Ashen Veil, on: grey ash whirls up round the body and settles. */
	public static void ashLit(ServerLevel level, LivingEntity t) {
		ElementFx.swirl(level, t.position(), 0.7, t.getBbHeight(), 3, ASH, ElementFx.FIRE.primary());
		Vfx.emit(level, ParticleTypes.ASH, centre(t), 12, 0.4, 0.02);
		snd(level, t.position(), "fire_ash", 0.7F, 1.0F);
	}

	/** Ashen Veil, each second: the cloak drifting. Quiet. */
	public static void ashDrift(ServerLevel level, LivingEntity t) {
		ElementFx.swirl(level, t.position(), 0.7, t.getBbHeight(), 1, ASH, ElementFx.FIRE.primary());
		Vfx.emit(level, ParticleTypes.ASH, centre(t), 4, 0.4, 0.02);
	}

	/** Ashen Veil struck: a puff of ash off the cloak into the attacker's face, and the attacker catches. */
	public static void ashPuff(ServerLevel level, LivingEntity cloak, LivingEntity attacker, boolean blinded) {
		Vec3 a = centre(attacker);
		ElementFx.ray(level, centre(cloak), a, ElementFx.FIRE.primary(), 0.05, 4);
		ElementFx.fireImpact(level, a, 0.5);
		Vfx.emit(level, ParticleTypes.ASH, a, blinded ? 14 : 6, 0.3, 0.05);
		snd(level, a, "fire_ash", 0.6F, blinded ? 1.3F : 1.1F);
	}

	/** Ashen Veil, when it has gone: the cloak blows away. */
	public static void ashOut(ServerLevel level, LivingEntity t) {
		Vfx.emit(level, ParticleTypes.ASH, centre(t), 10, 0.5, 0.06);
		snd(level, t.position(), "fire_out", 0.5F, 1.1F);
	}

	/** Cinderheart, lit: a flare at the heart, a 4-block ring on the ground and a star sigil; the furnace door opens. */
	public static void heartLit(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position();
		ElementFx.heatFlare(level, centre(t), 1.3);
		ElementFx.flatSigil(level, feet, SigilOption.STAR, ElementFx.FIRE.accent(), 3.2, 30, 0.1);
		ElementFx.groundRing(level, feet, ElementFx.FIRE.primary(), 0.4, 4.0, 0.1, 12);
		ElementFx.tongues(level, feet, 1.6, 1.2, 5, ElementFx.FIRE.primary(), ElementFx.FIRE.secondary(), 5, 10);
		snd(level, feet, "fire_coals", 1.0F, 0.9F);
		snd(level, feet, "fire_whump", 0.7F, 0.8F);
	}

	/** Cinderheart, each second: the ring pulses out to 4 blocks, embers circle the body, the coals thrum. */
	public static void heartPulse(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position();
		ElementFx.groundRing(level, feet, ElementFx.FIRE.primary(), 0.4, 4.0, 0.08, 12);
		ElementFx.orbit(level, centre(t), 1.0, 3, 20, ElementFx.FIRE.primary(), GOLD);
		ElementFx.tongues(level, feet, 1.6, 1.2, 2, ElementFx.FIRE.primary(), ElementFx.FIRE.secondary(), 5, 10);
		snd(level, feet, "fire_coals", 0.6F, 1.0F);
	}

	/** Cinderheart refused (still burning, or the coals have not cooled): a grey ring falls in on a bed of dead ash. */
	public static void heartRefused(ServerLevel level, LivingEntity t) {
		ElementFx.groundRing(level, t.position().add(0, 0.05, 0), ASH, 1.4, 0.3, 0.05, 8);
		Vfx.emit(level, ParticleTypes.ASH, centre(t), 8, 0.3, 0.03);
		Motes.smoke(level, centre(t), 2, 0.3);
		snd(level, t.position(), "fire_out", 0.6F, 0.7F);
	}

	/** Cinderheart, when the fire has died down: the coals go dark. */
	public static void heartOut(ServerLevel level, LivingEntity t) {
		Motes.smoke(level, centre(t), 4, 0.5);
		Vfx.emit(level, ParticleTypes.ASH, centre(t), 8, 0.5, 0.04);
		snd(level, t.position(), "fire_out", 0.9F, 0.8F);
	}

	/** Fireward: amber rings fold in on the body, one over the feet and one over the chest. */
	public static void ward(ServerLevel level, Entity t) {
		Vec3 base = t.position();
		double w = Math.max(0.4, t.getBbWidth() * 0.65);
		ElementFx.ring(level, base.add(0, 0.2, 0), UP, ElementFx.FIRE.secondary(), w + 1.1, w + 0.1, 0.05, 12);
		ElementFx.ring(level, base.add(0, t.getBbHeight() * 0.6, 0), UP, ElementFx.FIRE.primary(), w + 0.9, w + 0.1, 0.04, 14);
		Vfx.emit(level, ParticleTypes.SMALL_FLAME, centre(t), 3, 0.35, 0.01);
		snd(level, base, "fire_ward", 0.6F, 1.0F);
	}

	// ================================================================== blood

	/** Leech: a thin thread of blood streams from the target to the caster's hand and a sip is drunk; overhealing rings the caster. */
	public static void leech(ServerLevel level, Entity t, Entity caster, boolean shielded) {
		Vec3 c = centre(t);
		Vec3 to = centre(caster);
		if (!budget(level, "leech.show", 6)) {
			ElementFx.ray(level, c, to, ElementFx.BLOOD.primary(), 0.02, 6);
			return;
		}
		ElementFx.ray(level, c, to, ElementFx.BLOOD.primary(), 0.02, 6);
		for (int i = 0; i < 2; i++) {
			Motes.seek(level, c, to, ElementFx.BLOOD.secondary(), 0.11, 9 + 2 * i, 0.5 + i);
		}
		Sigils.flash(level, c, ElementFx.BLOOD.primary(), 0.8F);
		Vfx.emit(level, new net.minecraft.core.particles.DustParticleOptions(0x8A0A1A, 0.9F), c, 3, 0.2, 0.0);
		if (shielded) {
			Light.ring(level, to, UP, 0xFF8A9A, 0.3, width(caster) + 0.6, 0.05, 9);
		}
		snd(level, to, "blood_sip", 0.9F, shielded ? 1.15F : 1.0F);
	}

	/** Bleed, the cut: one thin crimson stroke and the slice. */
	public static void bleedCut(ServerLevel level, Entity t) {
		Vec3 c = centre(t);
		if (!budget(level, "bleed.cut", 8)) {
			ElementFx.drip(level, c, 0.2, 2);
			return;
		}
		double yaw = Math.toRadians(t.getYRot());
		Vec3 look = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 side = look.cross(UP).normalize();
		Light.slash(level, c.add(look.scale(0.4)), look, side.add(0, -0.6, 0).normalize(), ElementFx.BLOOD.primary(), Math.max(0.5, t.getBbHeight() * 0.4), 1.8, 0.07, 1, 6);
		snd(level, c, "blood_slice", 0.7F, 1.25F);
	}

	/** Bleed, each half second: drops falling from the wound and a soft tick; while it moves the wound tears wider (a ring). */
	public static void bleedDrip(ServerLevel level, Entity t, boolean moving) {
		Vec3 c = centre(t);
		for (int i = 0; i < 2; i++) {
			Vec3 p = c.add((level.getRandom().nextDouble() - 0.5) * t.getBbWidth(), (level.getRandom().nextDouble() - 0.5) * t.getBbHeight() * 0.6,
				(level.getRandom().nextDouble() - 0.5) * t.getBbWidth());
			Vfx.fling(level, new net.minecraft.core.particles.DustParticleOptions(0x8A0A1A, 1.0F), p, new Vec3(0, -1, 0), 0.05);
		}
		if (moving) {
			ElementFx.ring(level, c, UP, ElementFx.BLOOD.primary(), 0.15, width(t) + 0.5, 0.04, 6);
		}
		if (budget(level, "bleed.drip", 3)) {
			snd(level, c, "blood_drip", moving ? 0.7F : 0.5F, moving ? 1.15F : 1.0F);
		}
	}

	/** A gash weeping: the drops, and the tick of one landing. */
	public static void gashWeep(ServerLevel level, Entity t) {
		Vec3 c = centre(t);
		for (int i = 0; i < 2; i++) {
			Vec3 p = c.add((level.getRandom().nextDouble() - 0.5) * t.getBbWidth(), (level.getRandom().nextDouble() - 0.5) * t.getBbHeight() * 0.5,
				(level.getRandom().nextDouble() - 0.5) * t.getBbWidth());
			Vfx.fling(level, new net.minecraft.core.particles.DustParticleOptions(0x8A0A1A, 1.0F), p, new Vec3(0, -1, 0), 0.05);
		}
		if (budget(level, "gash.weep", 3)) {
			snd(level, c, "blood_drip", 0.5F, 0.85F);
		}
	}

	/** Rend: cracks open across the chest one after another and spread over two seconds, steel flecks flying. */
	public static void rend(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		if (!budget(level, "rend.show", 6)) {
			Light.ring(level, c, UP, ElementFx.BLOOD.primary(), 0.2, width(t) + 0.5, 0.04, 8);
			return;
		}
		double r = Math.max(0.5, t.getBbHeight() * 0.4);
		double yaw = Math.toRadians(t.getYRot());
		Vec3 look = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 side = look.cross(UP).normalize();
		for (int i = 0; i < 3; i++) {
			int k = i;
			Scheduler.later(i * 10, () -> {
				if (!t.isAlive() || t.level() != level) {
					return;
				}
				Vec3 toward = side.scale(k == 1 ? -1 : 1).add(0, k == 2 ? -0.8 : 0.7, 0).normalize();
				Light.slash(level, c.add(look.scale(0.4)).add(0, (k - 1) * 0.2, 0), look, toward, k == 1 ? 0xFF8090 : ElementFx.BLOOD.primary(), r,
					1.3 + 0.2 * k, 0.05, 1, 44 - 10 * k);
			});
		}
		Vfx.radial(level, new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.Items.IRON_NUGGET), c, 4, 0.15);
		snd(level, c, "blood_rend", 0.9F, 1.0F);
	}

	/** Rend, when its armour comes back: a pale ring closes on the chest. */
	public static void rendMend(ServerLevel level, LivingEntity t) {
		Light.ring(level, centre(t), UP, 0xC8D0E0, width(t) + 0.6, width(t) * 0.5, 0.03, 8);
	}

	/**
	 * Dismantle, one of three hairline cuts: each on a steeper tilt than the last; the third (from behind) is longer and rings
	 * white-gold.
	 */
	public static void dismantle(ServerLevel level, Entity t, int slash, boolean back) {
		Vec3 c = centre(t);
		if (slash == 0 && !budget(level, "dismantle.show", 8)) {
			ElementFx.drip(level, c, 0.2, 2);
			return;
		}
		double yaw = Math.toRadians(t.getYRot());
		Vec3 normal = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 bulge = ElementFx.inPlane(normal, -0.9 + slash * 0.9);
		double r = Math.max(0.7, t.getBbHeight() * 0.55) * (back ? 2.1 : 1.6);
		ElementFx.slash(level, c.subtract(bulge.scale(r * 0.97)), normal, bulge, ElementFx.BLOOD.primary(), r, 1.1, 0.12, 1, 5);
		ElementFx.slash(level, c.subtract(bulge.scale(r)), normal, bulge, back ? WHITE_GOLD : 0xFFE0E4, r, 1.3, back ? 0.09 : 0.06, 1, 4);
		ElementFx.drip(level, c, 0.2, 2);
		if (slash == 0) {
			snd(level, c, "blood_triple", 0.9F, 1.0F);
		}
		if (back) {
			Vfx.emit(level, ParticleTypes.CRIT, c, 6, 0.25, 0.25);
		}
	}

	/** Cleave, the axe swinging on: a thin stroke to each bystander it also cut, and a small cut where it lands. */
	public static void cleaveSweep(ServerLevel level, Entity from, Entity to) {
		Vec3 a = centre(from);
		Vec3 b = centre(to);
		ElementFx.ray(level, a, b, ElementFx.BLOOD.primary(), 0.05, 5);
		Vec3 dir = b.subtract(a);
		Vec3 normal = dir.lengthSqr() < 1.0E-4 ? UP : dir.normalize();
		ElementFx.cut(level, b, normal, ElementFx.perp(normal), 0.4, 0.07);
		ElementFx.drip(level, b, 0.2, 2);
	}

	/**
	 * Overdrive: the surge (a heartbeat racing away, the screen edge flushing red) and, each time it drains a heart, a beat with a
	 * ring more per pain tier so it can be told how hard it is running.
	 */
	public static void overdrive(ServerLevel level, LivingEntity t, boolean start, int tier) {
		Vec3 c = centre(t);
		int beats = 1 + Math.max(0, Math.min(2, tier));
		for (int i = 0; i < beats; i++) {
			Scheduler.later(i * 3, () -> {
				if (t.isAlive() && t.level() == level) {
					ElementFx.pulse(level, centre(t), UP, start ? 1.6 : 0.9 + 0.15 * beats);
				}
			});
		}
		if (start) {
			ElementFx.groundRing(level, t.position(), ElementFx.BLOOD.primary(), 0.3, 1.8, 0.07, 9);
			ElementFx.tongues(level, t.position(), Math.max(0.4, t.getBbWidth() * 0.6), t.getBbHeight(), 4, ElementFx.BLOOD.primary(), ElementFx.BLOOD.secondary(), 2, 8);
			Sigils.flash(level, c, ElementFx.BLOOD.primary(), 1.5F);
			snd(level, c, "blood_race", 1.0F, 1.0F);
		} else {
			ElementFx.drip(level, c, 0.25, 2);
			snd(level, c, "blood_heart", 0.7F + 0.1F * tier, 1.0F + 0.1F * tier);
		}
		if (t instanceof ServerPlayer player) {
			ScreenFx.tint(player, ElementFx.BLOOD.primary(), start ? 30 : 16);
		}
	}

	/** Overdrive, when it ends: the heart settles, one slow beat and a last ring, the screen edge clearing. */
	public static void overdriveEnd(ServerLevel level, LivingEntity t) {
		ElementFx.ring(level, centre(t), UP, ElementFx.BLOOD.secondary(), width(t) + 0.8, width(t) * 0.5, 0.04, 10);
		snd(level, centre(t), "blood_heart", 0.6F, 0.7F);
	}

	/** Warcry: the horn, and three rings of crimson running out over the ground, one after another. */
	public static void warcry(ServerLevel level, Vec3 centre, double radius) {
		Vec3 ground = CastEngine.ground(level, centre.add(0, 0.5, 0));
		for (int i = 0; i < 3; i++) {
			int k = i;
			Runnable ring = () -> ElementFx.groundRing(level, ground.add(0, 0.05, 0), k == 1 ? ElementFx.BLOOD.secondary() : ElementFx.BLOOD.primary(), 0.4,
				radius * (1.0 - 0.15 * k), 0.09 - 0.02 * k, 12);
			if (i == 0) {
				ring.run();
			} else {
				Scheduler.later(i * 4, ring);
			}
		}
		snd(level, centre, "blood_horn", 0.9F, 1.0F);
	}

	/** A rallied creature took the life of a kill: a small red ring and a sip. */
	public static void bloodlust(ServerLevel level, LivingEntity killer) {
		Vec3 c = centre(killer);
		ElementFx.drip(level, c, 0.3, 3);
		Light.ring(level, c, UP, ElementFx.BLOOD.secondary(), 0.2, width(killer) + 0.5, 0.04, 7);
		snd(level, c, "blood_sip", 0.7F, 1.25F);
	}

	/** Blood Moss, first laid: a patch of moss and blood on the ground under the target, spores lifting off it. */
	public static void mossPatch(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position().add(0, 0.05, 0);
		Light.groundRing(level, feet, MOSS, 0.2, width(t) + 0.9, 0.3, 40);
		Light.groundRing(level, feet, ElementFx.BLOOD.primary(), width(t) + 0.2, width(t) + 0.9, 0.05, 40);
		snd(level, feet, "blood_moss", 0.9F, 1.0F);
	}

	/** Lifesteal's siphon feeding the caster: a mote of blood from the marked creature to them (at most a few a tick, so a Barrage stays readable). */
	public static void siphonFeed(ServerLevel level, LivingEntity from, LivingEntity caster) {
		if (budget(level, "siphon.feed", 3)) {
			Motes.seek(level, centre(from), centre(caster), ElementFx.BLOOD.secondary(), 0.1, 10, 0.5);
		}
	}

	/** Blood Thread, a share running along the strand: a bright pulse from the one hurt to the one that shares it, the hop a step higher. */
	public static void threadPulse(ServerLevel level, LivingEntity from, LivingEntity to, int hop) {
		Vec3 a = centre(from);
		Vec3 b = centre(to);
		ElementFx.ray(level, a, b, 0xFF5060, 0.05, 5);
		Motes.seek(level, a, b, ElementFx.BLOOD.secondary(), 0.14, 6, 0.0);
		Vfx.emit(level, new net.minecraft.core.particles.DustParticleOptions(ElementFx.BLOOD.primary(), 0.8F), b, 3, 0.2, 0.0);
		snd(level, b, "blood_twang", 0.6F, LADDER[Math.max(0, Math.min(LADDER.length - 1, hop))]);
	}

	// ================================================================== the shapes' anticipation (Feel hooks)

	/** Explode, anticipation: three sparks pull in to the hand, white to orange. */
	public static void cueBlast(FeelCtx ctx) {
		ServerLevel level = ctx.level();
		for (int i = 0; i < 3; i++) {
			Vec3 from = ctx.at().add(ElementFx.randomDir(level.getRandom()).scale(0.7));
			Motes.seek(level, from, ctx.at(), i == 0 ? WHITE : ElementFx.FIRE.secondary(), 0.12, 5, 0.0);
		}
	}

	/** Meteor, anticipation: a ring of fire lifts off the hand and closes, the arm raised. */
	public static void cueCall(FeelCtx ctx) {
		ElementFx.ring(ctx.level(), ctx.at().add(0, 0.25, 0), UP, ElementFx.FIRE.primary(), 0.7, 0.1, 0.04, 6);
	}

	/** Cleave and Sanguine Rite, anticipation: a thin red line drawn in the air at the hand. */
	public static void cueCut(FeelCtx ctx) {
		Vec3 d = ctx.dir();
		Vec3 side = ElementFx.perp(d);
		ElementFx.ray(ctx.level(), ctx.at().add(side.scale(-0.4)), ctx.at().add(side.scale(0.4)), ElementFx.BLOOD.primary(), 0.03, 5);
	}

	/** Blood runes with a pulse (Overdrive, Bloodboil): a drop of blood gathers at the hand. */
	public static void cueDrop(FeelCtx ctx) {
		ElementFx.drip(ctx.level(), ctx.at(), 0.1, 2);
	}
}
