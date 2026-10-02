package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.aura.AuraVfx;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.TechniqueRules;
import dev.wildercord.aura.Techniques;
import dev.wildercord.aura.WayBanner;
import dev.wildercord.aura.WayRules;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Statuses;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A technique of a swordsman's own, performed: its stroke's shape, carried by its release, meaning its intent, carrying its method's
 * element (every number from {@link TechniqueRules.Profile}). The framework has already priced it and will rest it, and gives it its
 * banner (its writer's name for it), a body flare and the method's technique sound; this draws the rest and lands its strikes through
 * {@link ArtKit.Hits}, so momentum, stance (at the technique's own weight), the PvP caps and a boss's rules ride every one.
 *
 * <ul>
 * <li><b>Strokes</b>: a thrust's lance down the look; a rising cut's crescent up across the front, foes thrown up; a falling cut's
 * narrow crescent down onto one or two, the ground cracked where it lands; a sweep's wide level crescent; a spin's whole turn; a draw's
 * fast low crescent.</li>
 * <li><b>Releases</b>: on the blade, where it reaches; a wave flying on past it (a spin's a ring racing out), stopped by walls; a burst
 * all round at once; an afterimage left standing where it was struck, which strikes it again a moment later from there.</li>
 * <li><b>Intents</b>: pierce (further, through more), sunder (a stance worn hard), bind (rooted), echo (struck again), ward (its
 * swordsman steadied), rally (they and their allies steadied, allied swordsmen building momentum), infuse (the element twice over), or
 * an add-on's own ({@link AuraApi#registerTechniqueIntent}).</li>
 * </ul>
 *
 * <p>First person: what lies across the swordsman's own line of sight (a lance, a crescent rising before the face, a ring racing out from
 * the feet, a shell round the body) is {@link ArtLight#spectacle}; what they see is the thin trail, the impacts on their foes, and still
 * marks low on the ground or well ahead.</p>
 */
public final class TechniqueArts {
	private TechniqueArts() {}

	/** How often each release has been performed since the server started, and the last technique's profile (the game tests read them). */
	private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();
	private static TechniqueRules.Profile last;

	public static Map<String, Integer> counts() {
		synchronized (COUNTS) {
			return Map.copyOf(COUNTS);
		}
	}

	public static TechniqueRules.Profile last() {
		return last;
	}

	/** The performer every written technique plays through. */
	public static boolean perform(ServerPlayer player, AuraApi.StringContext context) {
		int slot = TechniqueRules.slotOf(context.art().id());
		if (slot < 0 || !Techniques.usable(player, slot)) {
			return false;
		}
		Techniques.Written w = Techniques.book(player).slot(slot);
		TechniqueRules.Profile p = Techniques.profile(player, w);
		new Cast(player, context, p, Techniques.use(player, slot)).play();
		last = p;
		synchronized (COUNTS) {
			COUNTS.merge(p.release().id(), 1, Integer::sum);
			COUNTS.merge(p.intent().id(), 1, Integer::sum);
			COUNTS.merge(p.stroke().id(), 1, Integer::sum);
		}
		return true;
	}

	// ------------------------------------------------------------------ one performance

	/** One technique being performed: where its swordsman stood and faced, its strikes, what it has done to whom. */
	private static final class Cast {
		final ServerPlayer player;
		final ServerLevel level;
		final AuraApi.StringContext context;
		final TechniqueRules.Profile p;
		final Techniques.Use use;
		final AuraFx.Art fx;
		final ArtKit.Hits hits;
		final int color;
		final int hot;
		final int second;
		final Vec3 feet;
		final Vec3 look;
		final Vec3 aim;
		final Vec3 eye;
		final float yaw;
		/** Every foe struck, in order (for an echo), and what the method's element has given so far. */
		final List<LivingEntity> struck = new ArrayList<>();
		final Set<UUID> wounded = new HashSet<>();
		double mended;
		double given;
		boolean sparked;
		int echoed;
		final ArtKit.Drink drink;

		Cast(ServerPlayer player, AuraApi.StringContext context, TechniqueRules.Profile p, Techniques.Use use) {
			this.player = player;
			this.level = player.level();
			this.context = context;
			this.p = p;
			this.use = use;
			this.color = ArtKit.color(player);
			this.hot = ArtKit.hot(color, 0.45);
			this.second = dev.wildercord.aura.Aura.method(player).map(BreathingMethod::highlight).orElse(0xFFFFFF);
			this.feet = player.position();
			this.look = ArtKit.flat(player);
			this.aim = player.getViewVector(1.0F);
			this.eye = player.getEyePosition();
			this.yaw = player.getYRot();
			float power = (float) (1.3 + 0.06 * p.rank());
			this.fx = AuraFx.art(player).trail(p.stroke().trail(), false, power);
			this.hits = ArtKit.hits(player, fx).scaled(Config.get().aura().damageScale() * Config.get().aura().techniques().techniqueDamage())
				.wearing(p.stance());
			TechniqueRules.Flavour fl = p.flavour();
			double k = p.flavourScale() * p.temperScale();
			this.drink = fl.drink() > 0 ? new ArtKit.Drink(player, fl.drink() * k, fl.drinkCap() * k, foe -> CrimsonArts.drops(level, foe.getBoundingBox().getCenter(), 0.25, 3)) : null;
		}

		void play() {
			Feels.sound(level, feet.add(0, 1, 0), "aura_technique_" + p.release().id(), 1.0F, pitch());
			switch (p.release().id()) {
				case TechniqueRules.WAVE -> wave();
				case TechniqueRules.BURST -> burst();
				case TechniqueRules.AFTERIMAGE -> afterimage();
				default -> blade(feet, look, aim, eye, p.factor(), true);
			}
			swordsman();
			if (p.echo() > 0) {
				echo();
			}
			if (p.rank() >= TechniqueRules.PEERLESS) {
				// Peerless: a ring of the aura's light round the feet as its name rings out (seen from outside; a whisper in your own view).
				AuraFx.burst(level, player, feet.add(0, 0.1, 0), ArtKit.UP, hot, 2.2F, AuraFx.Burst.RING | AuraFx.Burst.STAR);
			}
		}

		/** A heavier stroke sounds lower, a lighter one higher. */
		float pitch() {
			return (float) Math.max(0.7, Math.min(1.35, 1.0 / Math.sqrt(Math.max(0.3, p.factor() / 0.8))));
		}

		// ---------------------------------------------------------------- the stroke on the blade

		/**
		 * The stroke struck from {@code from} facing {@code dir} (its foes, its light): on the blade at once, or an afterimage's strike
		 * later from where its swordsman stood ({@code live} false: seen from outside, by them too).
		 */
		void blade(Vec3 from, Vec3 dir, Vec3 look3, Vec3 eyeAt, double factor, boolean live) {
			List<LivingEntity> foes = switch (p.shape()) {
				case LINE -> ArtKit.beam(player, eyeAt.subtract(0, 0.25, 0), eyeAt.subtract(0, 0.25, 0).add(look3.scale(p.reach())), p.width(), p.targets());
				case CONE -> live ? ArtKit.arc(player, context.struck(), p.reach(), p.width(), p.targets())
					: ArtKit.arcFrom(player, from, dir, p.reach(), p.width(), p.targets());
				case RING -> ArtKit.around(player, from, p.reach(), 1.0, 2.6, p.targets());
			};
			drawStroke(from, dir, look3, eyeAt, live, 1.0);
			strikeAll(foes, factor, from, AuraFxRules.Weight.HEAVY);
		}

		/** Strikes each of {@code foes}, the first at {@code factor}, the rest at the stroke's share of it. */
		void strikeAll(List<LivingEntity> foes, double factor, Vec3 heart, AuraFxRules.Weight weight) {
			for (int i = 0; i < foes.size(); i++) {
				strike(foes.get(i), i == 0 ? factor : factor * p.others(), heart, weight);
			}
		}

		/** One strike: its blow, its experience, then the intent's and the element's touch on the foe (only on one it hurt). */
		float strike(LivingEntity foe, double factor, Vec3 heart, AuraFxRules.Weight weight) {
			float taken = hits.strike(foe, factor, weight);
			use.landed(foe, taken);
			if (taken <= 0) {
				return 0;
			}
			if (!struck.contains(foe)) {
				struck.add(foe);
			}
			intent(foe, taken, heart);
			element(foe, taken, heart);
			if (p.lift() > 0 && foe.isAlive()) {
				ArtKit.lift(foe, p.lift(), 24);
			}
			return taken;
		}

		/** The stroke's light, seen as it should be from wherever it's struck (the swordsman's own view keeps only what's low and ahead). */
		void drawStroke(Vec3 from, Vec3 dir, Vec3 look3, Vec3 eyeAt, boolean live, double strength) {
			ArtLight world = ArtLight.world(player);
			// A stroke struck from where the swordsman stood a moment ago (an afterimage's) is out in the world for them too.
			ArtLight show = live ? ArtLight.spectacle(player) : world;
			double r = p.reach();
			int c = strength < 1 ? ArtKit.mix(color, 0xFFFFFF, 0.35) : color;
			double w = strength;
			switch (p.stroke().id()) {
				case TechniqueRules.THRUST -> {
					Vec3 hand = eyeAt.subtract(0, 0.35, 0).add(look3.scale(0.6));
					show.ray(hand, hand.add(look3.scale(r)), c, 0.09 * w, 8).bare().ray(hand, hand.add(look3.scale(r)), 0xFFFFFF, 0.03 * w, 6);
					// Low along the ground, a streak of light out to its reach (what the swordsman sees of it).
					Vec3 low = from.add(0, 0.06, 0);
					world.ray(low.add(dir.scale(1.4)), low.add(dir.scale(r)), c, 0.16 * w, 9);
				}
				case TechniqueRules.RISING -> {
					Vec3 centre = from.add(0, 0.35, 0).add(dir.scale(Math.min(2.2, r * 0.6)));
					show.slash(centre, dir, ArtKit.UP.scale(0.9).add(ArtKit.right(dir).scale(-0.4)).normalize(), c, 1.25, 2.3, 0.42 * w, 1, 10)
						.bare().slash(centre.subtract(dir.scale(0.04)), dir, ArtKit.UP, hot, 1.2, 1.9, 0.12 * w, 1, 9);
					world.slash(from.add(0, 0.08, 0).add(dir.scale(1.4)), ArtKit.UP, dir, c, Math.max(1.2, r * 0.55), 2.0, 0.22 * w, 2, 9);
				}
				case TechniqueRules.FALLING -> {
					Vec3 centre = from.add(0, 1.2, 0).add(dir.scale(Math.min(2.4, r * 0.65)));
					show.slash(centre, ArtKit.right(dir), dir.scale(0.5).add(ArtKit.UP.scale(0.85)).normalize(), c, 1.35, 2.4, 0.46 * w, 2, 10)
						.bare().slash(centre, ArtKit.right(dir), ArtKit.UP, hot, 1.3, 2.0, 0.13 * w, 2, 9);
					Vec3 land = ArtKit.floor(level, from.add(dir.scale(Math.min(r - 0.6, 2.8))), 1.0, 2.0);
					if (land != null) {
						world.groundRing(land, c, 0.15, 1.3, 0.09 * w, 10).ray(land.add(ArtKit.right(dir).scale(-0.9)).add(0, 0.05, 0),
							land.add(ArtKit.right(dir).scale(0.9)).add(0, 0.05, 0), hot, 0.07 * w, 12);
						if (live) {
							AuraFx.burst(level, player, land.add(0, 0.1, 0), ArtKit.UP, color, 1.3F, AuraFx.Burst.RING | AuraFx.Burst.SPARKS);
						}
					}
				}
				case TechniqueRules.SWEEP -> {
					Vec3 centre = from.add(0, 0.55, 0).add(dir.scale(0.9));
					world.slash(centre, ArtKit.UP, dir, c, r * 0.8, Math.toRadians(Math.min(300, p.width())), 0.42 * w, 2, 10)
						.bare().slash(centre.add(0, 0.03, 0), ArtKit.UP, dir, hot, r * 0.78, Math.toRadians(Math.min(300, p.width())) * 0.92, 0.12 * w, 2, 9);
				}
				case TechniqueRules.SPIN -> {
					show.slash(from.add(0, 1.0, 0), ArtKit.UP, dir, c, r * 0.85, Math.PI * 2, 0.36 * w, 4, 11)
						.slash(from.add(0, 0.7, 0), new Vec3(0.18, 1, 0).normalize(), dir.scale(-1), hot, r * 0.7, Math.PI * 2, 0.14 * w, 3, 10);
					// What the swordsman sees: a still ring on the ground at its reach.
					world.ring(from.add(0, 0.08, 0), ArtKit.UP, c, r, r, 0.1 * w, 10);
				}
				default -> {
					// The draw: a fast crescent at the knees, a bright line drawn across the chest (from outside).
					Vec3 centre = from.add(0, 0.5, 0).add(dir.scale(1.1));
					world.slash(centre, ArtKit.UP, dir, c, Math.max(1.6, r * 0.62), Math.toRadians(Math.min(300, p.width())), 0.28 * w, 1, 8)
						.bare().slash(centre.add(0, 0.02, 0), ArtKit.UP, dir, 0xFFFFFF, Math.max(1.5, r * 0.6), Math.toRadians(Math.min(300, p.width())) * 0.85,
							0.07 * w, 1, 7);
					show.slash(from.add(0, 1.15, 0).add(dir.scale(0.9)), ArtKit.UP, dir, hot, 1.3, 2.0, 0.08 * w, 1, 6);
				}
			}
		}

		// ---------------------------------------------------------------- a wave

		/** The stroke thrown on: struck where it reaches, then flying on past it (a spin's ring racing out), each foe once, stopped by a wall. */
		void wave() {
			blade(feet, look, aim, eye, p.factor(), true);
			double start = p.reach();
			double end = p.total();
			int ticks = (int) Math.ceil((end - start) / TechniqueRules.WAVE_SPEED);
			boolean ring = p.shape() == TechniqueRules.Shape.RING;
			double half = switch (p.shape()) {
				case LINE -> p.width();
				case CONE -> start * Math.sin(Math.toRadians(Math.min(180, p.width()) / 2));
				case RING -> 0;
			};
			Set<UUID> hit = new HashSet<>();
			for (LivingEntity foe : struck) {
				hit.add(foe.getUUID());
			}
			int[] count = {struck.size()};
			boolean[] stopped = {false};
			Vec3 flat = look;
			Vec3 up = new Vec3(0, ring ? 0.08 : p.shape() == TechniqueRules.Shape.LINE ? 1.05 : 0.85, 0);
			for (int t = 1; t <= ticks; t++) {
				double from = start + TechniqueRules.WAVE_SPEED * (t - 1);
				double to = Math.min(end, start + TechniqueRules.WAVE_SPEED * t);
				Scheduler.later(t, () -> {
					if (!player.isAlive() || player.level() != level || stopped[0]) {
						return;
					}
					List<LivingEntity> band;
					if (ring) {
						band = new ArrayList<>();
						for (LivingEntity foe : ArtKit.around(player, feet, to, 1.0, 2.6, 32)) {
							double d = Math.hypot(foe.getX() - feet.x, foe.getZ() - feet.z);
							if (d >= from - 0.6 && clear(feet.add(0, 1.0, 0), foe.getBoundingBox().getCenter())) {
								band.add(foe);
							}
						}
						ArtLight.spectacle(player).ring(feet.add(0, 0.12, 0), ArtKit.UP, color, from, to, 0.22, 4);
						ArtLight.world(player).ring(feet.add(0, 0.06, 0), ArtKit.UP, hot, to, to, 0.08, 4);
					} else {
						Vec3 a = feet.add(up).add(flat.scale(from));
						Vec3 b = feet.add(up).add(flat.scale(to));
						HitResult wall = level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
						if (wall.getType() != HitResult.Type.MISS) {
							stopped[0] = true;
							b = wall.getLocation();
							ArtLight.world(player).flash(b, hot, 1.4F).shards(b, 0.8, 5, color, hot);
						}
						band = ArtKit.line(player, feet.add(flat.scale(from)), flat, b.subtract(a).length() + 0.4, half, 2.2, 32);
						drawWave(feet.add(flat.scale(to)), flat, half, to);
					}
					for (LivingEntity foe : band) {
						if (count[0] >= p.targets() || !hit.add(foe.getUUID())) {
							continue;
						}
						count[0]++;
						strike(foe, count[0] == 1 ? p.factor() : p.factor() * p.others(), feet.add(flat.scale(to)), AuraFxRules.Weight.FULL);
					}
				});
			}
		}

		/** Whether nothing solid stands between two points. */
		boolean clear(Vec3 a, Vec3 b) {
			return level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
		}

		/** A wave's front at {@code at}: a crescent of aura (a lance for a thrust), seen by everyone once it's out ahead, never in your own eyes up close. */
		void drawWave(Vec3 at, Vec3 dir, double half, double out) {
			ArtLight light = out >= 2.6 ? ArtLight.world(player) : ArtLight.spectacle(player);
			if (p.shape() == TechniqueRules.Shape.LINE) {
				Vec3 tip = at.add(0, 1.05, 0);
				light.ray(tip.subtract(dir.scale(1.6)), tip, color, 0.13, 3).bare().ray(tip.subtract(dir.scale(1.2)), tip, 0xFFFFFF, 0.04, 3);
				return;
			}
			Vec3 centre = at.add(0, 0.85, 0).subtract(dir.scale(Math.max(0.6, half * 0.45)));
			double span = Math.max(1.4, Math.min(3.0, 2 * Math.asin(Math.min(1, half / Math.max(0.5, half * 1.1)))));
			light.slash(centre, ArtKit.UP, dir, color, Math.max(1.0, half * 1.05), span, 0.34, 1, 3)
				.bare().slash(centre.add(dir.scale(0.05)), ArtKit.UP, dir, hot, Math.max(0.95, half), span * 0.85, 0.1, 1, 3);
			if (level.getRandom().nextInt(2) == 0) {
				Motes.glows(level, at.add(0, 0.9, 0), 2, Math.max(0.3, half * 0.5), second, 0.08, 14, dir.scale(0.05), 0.02);
			}
		}

		// ---------------------------------------------------------------- a burst

		/** The stroke's force breaking out all round at once: every foe within its ring. */
		void burst() {
			double r = p.reach();
			List<LivingEntity> foes = ArtKit.around(player, feet, r, 1.0, 2.6, p.targets());
			ArtLight world = ArtLight.world(player);
			ArtLight show = ArtLight.spectacle(player);
			// Racing out from the feet: seen from outside; the swordsman's own view keeps the ring standing still at its edge.
			show.ring(feet.add(0, 0.12, 0), ArtKit.UP, color, 0.4, r, 0.3, 9).ring(feet.add(0, 0.9, 0), ArtKit.UP, hot, 0.3, r * 0.9, 0.12, 8);
			world.ring(feet.add(0, 0.07, 0), ArtKit.UP, color, r, r, 0.12, 11);
			switch (p.stroke().id()) {
				case TechniqueRules.THRUST -> {
					// Lances bursting out every way round, laid low on the ground as they go.
					for (int i = 0; i < 6; i++) {
						double a = Math.toRadians(yaw) + Math.PI * 2 * i / 6;
						Vec3 d = new Vec3(-Math.sin(a), 0, Math.cos(a));
						show.ray(feet.add(0, 1.0, 0).add(d.scale(0.6)), feet.add(0, 1.0, 0).add(d.scale(r)), hot, 0.08, 8);
						world.ray(feet.add(0, 0.06, 0).add(d.scale(1.2)), feet.add(0, 0.06, 0).add(d.scale(r)), color, 0.1, 9);
					}
				}
				case TechniqueRules.RISING -> {
					for (int i = 0; i < 4; i++) {
						double a = Math.toRadians(yaw) + Math.PI * 2 * i / 4 + Math.PI / 4;
						Vec3 d = new Vec3(-Math.sin(a), 0, Math.cos(a));
						show.slash(feet.add(0, 0.4, 0).add(d.scale(r * 0.6)), d, ArtKit.UP, color, 1.0, 2.0, 0.3, 1, 9);
					}
				}
				case TechniqueRules.FALLING -> {
					show.slash(feet.add(0, 2.2, 0), ArtKit.right(look), ArtKit.UP, hot, 1.2, 2.6, 0.36, 2, 9);
					world.ring(feet.add(0, 0.06, 0), ArtKit.UP, hot, r * 0.55, r * 0.55, 0.08, 12);
				}
				default -> show.slash(feet.add(0, 0.95, 0), ArtKit.UP, look, hot, r * 0.8, Math.PI * 2, 0.22, 4, 10);
			}
			AuraFx.burst(level, player, feet.add(0, 0.9, 0), Vec3.ZERO, hot, (float) Math.min(3.5, r * 0.8), AuraFx.Burst.FLASH | AuraFx.Burst.SPARKS);
			strikeAll(foes, p.factor(), feet, AuraFxRules.Weight.HEAVY);
		}

		// ---------------------------------------------------------------- an afterimage

		/** A lighter stroke now, and an afterimage left where it was struck that strikes it again from there a moment later. */
		void afterimage() {
			blade(feet, look, aim, eye, p.factor(), true);
			int color = this.color;
			AuraStep.afterimages(player, feet, feet, look, color, p.laterDelay() + 8);
			Vec3 from = feet;
			Vec3 dir = look;
			Vec3 look3 = aim;
			Vec3 eyeAt = eye;
			Scheduler.later(p.laterDelay(), () -> {
				if (!player.isAlive() || player.level() != level) {
					return;
				}
				// The afterimage strikes where its swordsman stood: everyone sees it there, them too.
				ArtLight.world(player).bare().flash(from.add(0, 1.0, 0), color | ArtLight.DARK, 1.2F).flash(from.add(0, 1.0, 0), hot, 0.7F);
				Feels.sound(level, from.add(0, 1, 0), "aura_technique_afterimage_strike", 0.9F, pitch());
				blade(from, dir, look3, eyeAt, p.later(), false);
			});
		}

		// ---------------------------------------------------------------- an echo

		/** The intent of an echo: every foe struck is struck again a moment later, wherever it has gone, if it's still near. */
		void echo() {
			Scheduler.later(TechniqueRules.ECHO_DELAY, () -> {
				if (!player.isAlive() || player.level() != level) {
					return;
				}
				double reach = p.total() + TechniqueRules.ECHO_REACH;
				Feels.sound(level, player.position().add(0, 1, 0), "aura_technique_echo", 0.85F, pitch() * 1.12F);
				AuraFx.trail(player, p.stroke().trail(), true, 1.0F);
				for (LivingEntity foe : List.copyOf(struck)) {
					if (!foe.isAlive() || foe.level() != level || foe.distanceToSqr(feet) > reach * reach) {
						continue;
					}
					ArtLight.world(player).ring(foe.getBoundingBox().getCenter(), aim.scale(-1), ArtKit.mix(color, 0xFFFFFF, 0.5), 0.2, 0.9, 0.05, 6);
					float taken = hits.strike(foe, p.factor() * p.echo(), AuraFxRules.Weight.LIGHT);
					use.landed(foe, taken);
					echoed++;
				}
			});
		}

		// ---------------------------------------------------------------- the intent, on each foe

		void intent(LivingEntity foe, float taken, Vec3 heart) {
			ArtLight world = ArtLight.world(player);
			Vec3 centre = foe.getBoundingBox().getCenter();
			switch (p.intent().id()) {
				case TechniqueRules.PIERCE -> {
					Vec3 through = centre.subtract(feet.add(0, 1, 0));
					through = through.lengthSqr() < 1.0E-4 ? look : through.normalize();
					world.ray(centre, centre.add(through.scale(1.3)), hot, 0.05, 6).bare().ray(centre, centre.add(through.scale(0.9)), 0xFFFFFF, 0.02, 5);
				}
				case TechniqueRules.SUNDER -> {
					world.shards(centre, 0.95, 6, color, hot);
					Feels.sound(level, centre, "aura_technique_sunder", 0.7F, 1.0F);
				}
				case TechniqueRules.BIND -> {
					if (ArtKit.root(player, foe, p.bind())) {
						Vec3 floor = ArtKit.floor(level, foe.position(), 0.5, 3.0);
						Vec3 at = floor == null ? foe.position() : floor;
						world.ring(at.add(0, 0.08, 0), ArtKit.UP, color, foe.getBbWidth() + 0.9, foe.getBbWidth() * 0.5 + 0.15, 0.08, Math.max(6, p.bind()));
						for (int i = 0; i < 4; i++) {
							double a = Math.PI / 2 * i + level.getRandom().nextDouble() * 0.6;
							Vec3 base = at.add(Math.cos(a) * (foe.getBbWidth() * 0.5 + 0.35), 0.05, Math.sin(a) * (foe.getBbWidth() * 0.5 + 0.35));
							world.ray(base, at.add(0, foe.getBbHeight() * 0.45, 0), second, 0.04, Math.max(6, p.bind()));
						}
					}
				}
				case TechniqueRules.ECHO, TechniqueRules.WARD, TechniqueRules.RALLY, TechniqueRules.INFUSE -> {
					// An echo strikes later; a ward and a rally are on the swordsman; an infusing is in the element.
				}
				default -> {
					AuraApi.IntentEffect effect = AuraApi.intentEffect(p.intent().id());
					if (effect != null) {
						try {
							effect.struck(player, foe, taken, p);
						} catch (RuntimeException e) {
							dev.wildercord.Wildercord.LOGGER.warn("Technique intent {} threw", p.intent().id(), e);
						}
					}
				}
			}
		}

		/** A ward or a rally: on the swordsman (and theirs) once, as it's struck. */
		void swordsman() {
			if (p.ward() > 0) {
				WayBanner.steady(player, p.ward(), p.wardTicks());
				int blue = ArtKit.mix(color, WayRules.BULWARK_COLOR, 0.45);
				ArtLight.spectacle(player).ring(feet.add(0, 1.0, 0), ArtKit.UP, blue, 0.9, 0.9, 0.1, 14).ring(feet.add(0, 0.4, 0), ArtKit.UP, blue, 0.8, 0.8, 0.06, 12)
					.ring(feet.add(0, 1.6, 0), ArtKit.UP, blue, 0.7, 0.7, 0.06, 12);
				ArtLight.world(player).ring(feet.add(0, 0.07, 0), ArtKit.UP, blue, 1.0, 1.0, 0.07, 14);
				AuraFx.bodyAuraFlare(player, 40, 0.75F);
				Feels.sound(level, feet.add(0, 1, 0), "aura_technique_ward", 0.85F, 1.0F);
			}
			if (p.rally() > 0) {
				Vec3 chest = feet.add(0, 1.2, 0);
				int gold = ArtKit.mix(color, WayRules.BANNER_COLOR, 0.5);
				for (LivingEntity body : WayBanner.company(player, p.rallyRadius())) {
					WayBanner.steady(body, p.rally(), p.rallyTicks());
					if (body != player) {
						Vec3 heart = body.getBoundingBox().getCenter();
						ArtLight.world(player).ray(chest, heart, ArtKit.hot(gold, 0.3), 0.04, 9).groundRing(body.position(), gold, 0.2, 0.9, 0.05, 12);
					}
				}
				for (ServerPlayer ally : WayBanner.swordsmen(player, p.rallyRadius())) {
					AuraApi.addMomentum(ally, p.rallyMomentum(), "rally");
				}
				ArtLight.spectacle(player).ray(feet.add(0, 1.7, 0), feet.add(0, 2.7, 0), ArtKit.hot(gold, 0.4), 0.05, 12)
					.ray(feet.add(0, 2.65, 0), feet.add(0, 2.65, 0).add(ArtKit.right(look).scale(0.8)).add(0, -0.2, 0), gold, 0.07, 12);
				AuraFx.burst(level, player, feet.add(0, 0.08, 0), ArtKit.UP, gold, (float) Math.min(4.0, p.rallyRadius() * 0.4), AuraFx.Burst.RING);
				Feels.sound(level, chest, "aura_technique_rally", 0.9F, 1.0F);
			}
		}

		// ---------------------------------------------------------------- the method's element, on each foe

		void element(LivingEntity foe, float taken, Vec3 heart) {
			TechniqueRules.Flavour fl = p.flavour();
			double k = p.flavourScale() * p.temperScale();
			boolean infused = p.flavourScale() > 1.01;
			Vec3 centre = foe.getBoundingBox().getCenter();
			ArtLight world = ArtLight.world(player);
			switch (fl.of()) {
				case IGNITE -> {
					ArtKit.ignite(foe, p.ignite());
					world.tongues(foe.position(), foe.getBbWidth() * 0.5 + 0.1, foe.getBbHeight() * 0.8, infused ? 4 : 2, color, second, 8);
				}
				case CHILL -> {
					ArtKit.chill(player, foe, p.chill(), infused ? 1 : 0);
					world.shards(centre, infused ? 0.9 : 0.6, infused ? 6 : 3, 0xBFEFFF, 0xFFFFFF);
				}
				case SPARK -> {
					if (!sparked) {
						sparked = true;
						Statuses.interrupt(foe);
						LivingEntity next = ArtKit.nearest(player, centre, 4.0, List.of(foe));
						if (next != null) {
							Vec3 to = next.getBoundingBox().getCenter();
							world.arc(centre, to, 0xFFF4A0, 0.06, 1, false, 5);
							float got = hits.strike(next, p.factor() * fl.spark() * k, AuraFxRules.Weight.LIGHT);
							use.landed(next, got);
						}
					}
				}
				case GALE -> {
					ArtKit.knock(foe, heart, fl.knock() * k, 0.12);
					world.swirl(foe.position(), foe.getBbWidth() * 0.6 + 0.3, foe.getBbHeight(), infused ? 3 : 2, color, second);
				}
				case STONE -> {
					if (!(foe instanceof Player) && p.stagger() > 0) {
						ArtKit.slow(player, foe, p.stagger(), 1);
					}
					Vec3 floor = ArtKit.floor(level, foe.position(), 0.5, 3.0);
					if (floor != null) {
						world.groundRing(floor, color, 0.15, infused ? 1.4 : 0.9, 0.07, 9);
					}
				}
				case MEND -> {
					double room = fl.mendCap() * k - mended;
					if (room > 0) {
						mended += ArtKit.mend(player, player, Math.min(room, fl.mend() * k));
						Motes.glows(level, player.position().add(0, 1.0, 0), 3, 0.35, second, 0.1, 22, new Vec3(0, 0.03, 0), 0.01);
					}
					Motes.glows(level, centre, infused ? 5 : 3, 0.3, color, 0.1, 18, new Vec3(0, 0.02, 0), 0.02);
				}
				case PULL -> {
					Vec3 to = p.shape() == TechniqueRules.Shape.RING || p.release().id().equals(TechniqueRules.BURST) ? feet : heart.add(look.scale(p.reach() * 0.45));
					ArtKit.draw(foe, to, fl.pull() * k);
					world.bare().flash(centre, color | ArtLight.DARK, infused ? 1.3F : 0.9F);
				}
				case STARLIT -> {
					double room = fl.auraCap() * k - given;
					if (room > 0) {
						double back = Math.min(room, fl.aura() * k);
						given += back;
						ArtKit.giveBack(player, back);
					}
					world.flash(centre, 0xFFFFFF, infused ? 1.0F : 0.6F).flash(centre, color, infused ? 1.4F : 0.9F);
				}
				case HASTE -> {
					if (echoed < 2 || infused && echoed < 3) {
						echoed++;
						Scheduler.later(fl.echoDelay(), () -> {
							if (foe.isAlive() && player.isAlive()) {
								ArtLight.world(player).ring(foe.getBoundingBox().getCenter(), ArtKit.UP, HourglassArts.gold(player), 0.3, 1.0, 0.05, 6);
								float got = hits.strike(foe, p.factor() * fl.echo() * k, null);
								use.landed(foe, got);
							}
						});
					}
				}
				case LEECH -> {
					if (wounded.add(foe.getUUID())) {
						ArtKit.wound(hits, foe, fl.bleed() * k, fl.bleeds(), drink, f -> CrimsonArts.drops(level, f.getBoundingBox().getCenter(), 0.2, 2));
					}
					if (drink != null) {
						drink.from(foe, taken);
					}
				}
				case NONE -> world.flash(centre, hot, 0.7F);
			}
		}
	}

	/** Every technique performed since the server started (the game tests). */
	public static int performed() {
		synchronized (COUNTS) {
			int n = 0;
			for (TechniqueRules.Release r : TechniqueRules.RELEASES) {
				n += COUNTS.getOrDefault(r.id(), 0);
			}
			return n;
		}
	}
}
