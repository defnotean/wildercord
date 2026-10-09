package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Statuses;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Thunder Breath's arts: faster than the eye, and from foe to foe. A little on each of many, and every one breaks what its foe
 * was winding up (a charge, a bow, a fuse) and leaves it ionised for a storm spell to conduct through.
 * <ul>
 * <li><b>Crackle</b> (I): three cuts on one foe faster than the eye, each throwing a spark to another near.</li>
 * <li><b>Skyfall</b> (II): a bolt called down on the foe (or the ground ahead) a moment later, arcing on to two more.</li>
 * <li><b>Static Riposte</b> (III): whoever struck, then lightning leaping on through the foes near, a little weaker each time.</li>
 * <li><b>Bolt Step</b> (IV): blinking from foe to foe, up to four, a cut on each.</li>
 * <li><b>Heaven's Spear</b> (V): lightning gathered on the blade, then a lance of it down your line of sight, and the sky
 * answering on everything it ran through.</li>
 * </ul>
 */
public final class ThunderArts {
	private ThunderArts() {}

	public static final String METHOD = "thunder";
	public static final String CRACKLE = "crackle";
	public static final String SKYFALL = "skyfall";
	public static final String STATIC_RIPOSTE = "static_riposte";
	public static final String BOLT_STEP = "bolt_step";
	public static final String HEAVENS_SPEAR = "heavens_spear";

	/** The storm palette's white and blue (cast.ElementFx.STORM). */
	private static final int WHITE = 0xFFFBE0;
	private static final int BLUE = 0xA8C8FF;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, CRACKLE, ThunderArts::crackle),
			MethodArts.art(AuraApi.ArtSlot.SECOND, SKYFALL, ThunderArts::skyfall),
			MethodArts.art(AuraApi.ArtSlot.THIRD, STATIC_RIPOSTE, ThunderArts::staticRiposte),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, BOLT_STEP, ThunderArts::boltStep),
			MethodArts.art(AuraApi.ArtSlot.FINAL, HEAVENS_SPEAR, ThunderArts::heavensSpear));
	}

	// ------------------------------------------------------------------ I. Crackle

	static boolean crackle(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		var continuation = dev.wildercord.aura.MastersArts.continuation(player);
		Vec3 committedFacing = ArtKit.flat(player);
		int color = ArtKit.color(player);
		LivingEntity foe = ArtKit.primary(player, context, ArtRules.CRACKLE_REACH, 120);
		AuraFx.Art fx = AuraFx.art(player);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_crackle", 1.0F, 1.0F);
		AuraFxRules.Stroke[] strokes = {AuraFxRules.Stroke.CUT, AuraFxRules.Stroke.CUT, AuraFxRules.Stroke.THRUST};
		for (int i = 0; i < ArtRules.CRACKLE_CUTS; i++) {
			int cut = i;
			Runnable go = () -> {
				if (!continuation.getAsBoolean() || !player.isAlive() || player.level() != level) {
					return;
				}
				fx.trail(strokes[cut], cut == 1, 1.15F + 0.1F * cut);
				Vec3 at = foe != null && foe.isAlive() ? foe.getBoundingBox().getCenter()
					: player.getEyePosition().add(committedFacing.scale(2.2)).subtract(0, 0.5, 0);
				// A cut of white lightning across it, on its own tilt each time.
				RandomSource r = level.getRandom();
				Vec3 normal = new Vec3(r.nextDouble() - 0.5, 0.6 + r.nextDouble() * 0.4, r.nextDouble() - 0.5).normalize();
				ArtLight world = ArtLight.world(player);
				world.slash(at, normal, ElementFx.perp(normal), WHITE, 0.75, 2.2, 0.07, 1, 4);
				world.slash(at, normal, ElementFx.perp(normal), color, 0.8, 2.3, 0.18, 1, 5);
				world.flash(at, WHITE, 0.9F);
				ElementFx.sparks(level, at, 5, 0.25);
				if (foe == null || !foe.isAlive() || !ArtKit.harmable(player, foe) || !player.hasLineOfSight(foe)
					|| foe.distanceToSqr(player) > Math.pow(ArtRules.CRACKLE_REACH + foe.getBbWidth() / 2, 2)) {
					return;
				}
				hits.strike(foe, ArtRules.CRACKLE_FACTOR, cut == 2 ? AuraFxRules.Weight.HEAVY : AuraFxRules.Weight.FULL);
				if (cut == 0) {
					Statuses.interrupt(foe);
				}
				// The spark it throws to another near.
				LivingEntity next = ArtKit.nearest(player, at, ArtRules.CRACKLE_SPARK_REACH, List.of(foe));
				if (next != null) {
					Vec3 to = next.getBoundingBox().getCenter();
					world.arc(at, to, color, 0.06, 1, false, 4);
					hits.strike(next, ArtRules.CRACKLE_SPARK, AuraFxRules.Weight.LIGHT);
					Feels.sound(level, to, "storm_zap", 0.45F, 1.3F + 0.15F * cut);
				}
			};
			if (i == 0) {
				go.run();
			} else {
				Scheduler.later(i * ArtRules.CRACKLE_GAP, go);
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ II. Skyfall

	static boolean skyfall(ServerPlayer player, AuraApi.StringContext context) {
		var release = dev.wildercord.aura.MastersArts.releaseTargets(player, SKYFALL);
		if (release == null) return false;
		ServerLevel level = player.level();
		ReleasedArtOwner owner = ReleasedArtOwner.capture(player);
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.RISING, false, 1.35F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		// Selection happened at acceptance; this is the one authorized release sample, never a fresh primary search.
		Vec3 point = release.point();
		ArtLight world = ArtLight.world(player);
		world.ground(point, SigilOption.TARGET, color, ArtRules.SKYFALL_RADIUS * 1.1, ArtRules.SKYFALL_DELAY + 6, 0.12);
		world.groundRing(point, WHITE, ArtRules.SKYFALL_RADIUS * 1.6, 0.3, 0.05, ArtRules.SKYFALL_DELAY);
		// The sky answering: a crackle gathering overhead.
		world.arc(point.add(0, 9, 0), point.add(0.6, 11.5, -0.4), color, 0.06, 2, false, ArtRules.SKYFALL_DELAY);
		Feels.sound(level, point.add(0, 1, 0), "aura_art_skyfall", 1.2F, 1.0F);
		Scheduler.later(ArtRules.SKYFALL_DELAY, () -> {
			if (!owner.valid()) {
				return;
			}
			Vec3 at = release.skyfallPoint(player);
			strike(player, at, color, 1.0F);
			Set<LivingEntity> struck = new HashSet<>();
			for (LivingEntity foe : ArtKit.around(player, at, ArtRules.SKYFALL_RADIUS, 1.0, 3.0, 6)) {
				hits.strike(foe, ArtRules.SKYFALL_FACTOR);
				ArtKit.shock(player, foe, ArtRules.SKYFALL_SHOCK);
				struck.add(foe);
			}
			// Arcing on to two more.
			Vec3 from = at.add(0, 1.0, 0);
			for (int i = 0; i < ArtRules.SKYFALL_ARCS; i++) {
				LivingEntity next = ArtKit.nearest(player, from, ArtRules.SKYFALL_ARC_REACH, struck);
				if (next == null) {
					break;
				}
				struck.add(next);
				Vec3 to = next.getBoundingBox().getCenter();
				ArtLight.world(player).arc(from, to, color, 0.08, 2, false, 6);
				AuraPhysicalFx.stormImpact(level, to, 0.5);
				hits.strike(next, ArtRules.SKYFALL_ARC_FACTOR, AuraFxRules.Weight.FULL);
				ArtKit.shock(player, next, ArtRules.SKYFALL_SHOCK / 2);
			}
			AuraFx.sound(player, AuraFx.Sound.IMPACT, 0.9F, 0.85F);
		});
		return true;
	}

	/** A bolt out of the sky at {@code at}: the lightning itself, a flash, rings racing over the ground and a scorch. */
	static void strike(ServerPlayer player, Vec3 at, int color, float size) {
		ServerLevel level = player.level();
		ArtLight world = ArtLight.world(player);
		ElementFx.bolt(level, at.add(0, 13 * size, 0), at, 0.13 * size, 3, 3, WHITE, color);
		world.flash(at.add(0, 1.0, 0), WHITE, 3.0F * size);
		world.groundRing(at, color, 0.3, 2.4 * size, 0.12, 9);
		world.groundRing(at, WHITE, 0.2, 1.6 * size, 0.05, 7);
		world.ground(at, SigilOption.CRACKED, color | ArtLight.DARK, 1.3 * size, 30, 0);
		ElementFx.sparks(level, at.add(0, 0.3, 0), 12, 0.4 * size);
		Feels.sound(level, at, "storm_boom", 1.0F * size, 1.1F);
	}

	// ------------------------------------------------------------------ III. Static Riposte

	static boolean staticRiposte(ServerPlayer player, AuraApi.StringContext context) {
		var counter = dev.wildercord.aura.MastersArts.earnedCounter(player);
		if (counter == null || !counter.art().equals(STATIC_RIPOSTE) || !counter.valid()) return false;
		ReleasedArtOwner owner = ReleasedArtOwner.capture(player);
		ServerLevel level = owner.level();
		int color = ArtKit.color(player);
		LivingEntity foe = counter.target();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_static_riposte", 1.0F, 1.0F);
		// Lightning running up the blade first (seen from outside; through your own eyes, a crackle low in the view).
		Vec3 hand = ArtKit.hand(player);
		ArtLight show = ArtLight.spectacle(player);
		for (int i = 0; i < 3; i++) {
			double a = Math.PI * 2 * i / 3;
			show.arc(hand.add(Math.cos(a) * 0.5, 0.5, Math.sin(a) * 0.5), hand, color, 0.04, 1, false, 4);
		}
		AuraFx.burst(level, player, hand, Vec3.ZERO, color, 1.2F, AuraFx.Burst.SPARKS | AuraFx.Burst.FLASH);
		if (foe == null) {
			return true;
		}
		ReleasedCounterBody primary = ReleasedCounterBody.capture(foe);
		hits.strike(foe, ArtRules.RIPOSTE_FACTOR);
		if (!counter.valid() || !primary.conducts(player, owner)) return true;
		ArtKit.shock(player, foe, ArtRules.RIPOSTE_SHOCK);
		AuraPhysicalFx.stormImpact(level, foe.getBoundingBox().getCenter(), 0.8);
		// Four original one-tick hops. No LOS or owner-range check is introduced between conductors.
		List<ReleasedCounterBody> chain = new ArrayList<>();
		chain.add(primary);
		boolean[] retired = {false};
		for (int n = 1; n <= ArtRules.RIPOSTE_JUMPS; n++) {
			int jump = n;
			Scheduler.later(n, () -> {
				if (retired[0] || !owner.valid() || chain.size() != jump) return;
				ReleasedCounterBody from = chain.getLast();
				if (!from.conducts(player, owner)) { retired[0] = true; return; }
				Vec3 a = from.entity().getBoundingBox().getCenter();
				LivingEntity candidate = ArtKit.nearest(player, a, ArtRules.RIPOSTE_REACH,
					chain.stream().map(ReleasedCounterBody::entity).toList());
				if (candidate == null) { retired[0] = true; return; }
				ReleasedCounterBody next = ReleasedCounterBody.capture(candidate);
				if (!next.targetFrom(player, owner, from, ArtRules.RIPOSTE_REACH)) { retired[0] = true; return; }
				chain.add(next);
				Vec3 b = candidate.getBoundingBox().getCenter();
				ArtLight w = ArtLight.world(player);
				w.arc(a, b, color, 0.09, 2, false, 6);
				w.arc(a, b, WHITE, 0.035, 0, false, 4);
				w.flash(b, WHITE, 1.1F);
				ElementFx.sparks(level, b, 6, 0.3);
				RiposteHit boundary = new RiposteHit(player, owner, from, next);
				dev.wildercord.aura.ArtHitScope.within(player, boundary,
					() -> hits.strike(candidate, ArtRules.chain(ArtRules.RIPOSTE_FACTOR, ArtRules.RIPOSTE_KEEP, jump), AuraFxRules.Weight.FULL));
				// The same envelope guards nested passive/resonance/rune callbacks before they can resume.
				if (!boundary.valid()) { retired[0] = true; return; }
				if (candidate.isAlive() && next.targetFrom(player, owner, from, ArtRules.RIPOSTE_REACH))
					ArtKit.shock(player, candidate, ArtRules.RIPOSTE_SHOCK);
				Feels.sound(level, b, "storm_zap", 0.7F, 1.0F + 0.12F * jump);
			});
		}
		return true;
	}

	// ------------------------------------------------------------------ IV. Bolt Step

	static boolean boltStep(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 eye = player.getEyePosition();
		// Up to four foes near and in sight, those ahead first, then the nearest.
		List<LivingEntity> foes = new ArrayList<>(ArtKit.around(player, player.position(), ArtRules.BOLT_REACH, 3, 4, 16));
		foes.removeIf(e -> !player.hasLineOfSight(e));
		foes.sort(Comparator.comparingDouble((LivingEntity e) -> {
			Vec3 to = e.position().subtract(player.position());
			double ahead = to.x * look.x + to.z * look.z;
			return (ahead < 0 ? 100 : 0) + to.length();
		}));
		if (foes.isEmpty()) {
			MethodArts.blocked(player, BOLT_STEP);
			return false;
		}
		List<LivingEntity> chosen = foes.subList(0, Math.min(ArtRules.BOLT_FOES, foes.size()));
		AuraFx.Art fx = AuraFx.art(player);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		List<LivingEntity> order = new ArrayList<>(chosen);
		for (int i = 0; i < order.size(); i++) {
			LivingEntity foe = order.get(i);
			int blink = i;
			Runnable go = () -> {
				if (!player.isAlive() || player.level() != level || !foe.isAlive()) {
					return;
				}
				Vec3 from = player.position();
				Vec3 spot = ArtKit.beside(player, foe, from);
				if (spot == null) {
					return;
				}
				Vec3 dir = foe.position().subtract(from);
				dir = new Vec3(dir.x, 0, dir.z);
				dir = dir.lengthSqr() < 1.0E-4 ? look : dir.normalize();
				ArtKit.blink(player, spot);
				AuraStep.afterimages(player, from, spot, dir, color);
				// The way it went, a jag of lightning; a flash where it arrives.
				ElementFx.bolt(level, from.add(0, 1.0, 0), spot.add(0, 1.0, 0), 0.07, 1, 2, WHITE, color);
				ArtLight w = ArtLight.world(player);
				w.flash(spot.add(0, 1.0, 0), WHITE, 1.4F);
				w.groundRing(spot, color, 0.2, 1.4, 0.06, 7);
				fx.trail(blink % 2 == 0 ? AuraFxRules.Stroke.CUT : AuraFxRules.Stroke.CROSS, blink % 2 == 1, 1.3F);
				hits.strike(foe, ArtRules.BOLT_FACTOR);
				ArtKit.shock(player, foe, ArtRules.BOLT_SHOCK);
				ElementFx.sparks(level, foe.getBoundingBox().getCenter(), 8, 0.3);
				Feels.sound(level, spot.add(0, 1, 0), "aura_art_bolt_step", 1.0F, 1.0F + 0.08F * blink);
			};
			if (i == 0) {
				go.run();
			} else {
				Scheduler.later(i * ArtRules.BOLT_GAP, go);
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ V. Heaven's Spear

	static boolean heavensSpear(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		AuraFx.Art fx = AuraFx.art(player);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_heavens_spear", 1.3F, 1.0F);
		// The charge: a deliberate stance, lightning gathering on the blade and crackling round the swordsman.
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ArtRules.SPEAR_CHARGE, 2, false, false, true));
		fx.flare(ArtRules.SPEAR_CHARGE + 10, 1.0F);
		ArtLight show = ArtLight.spectacle(player);
		for (int t = 0; t < ArtRules.SPEAR_CHARGE; t += 2) {
			int tick = t;
			Scheduler.later(1 + t, () -> {
				if (!player.isAlive() || player.level() != level) {
					return;
				}
				RandomSource r = level.getRandom();
				Vec3 hand = ArtKit.hand(player);
				Vec3 around = player.position().add((r.nextDouble() - 0.5) * 3.2, 0.2 + r.nextDouble() * 2.2, (r.nextDouble() - 0.5) * 3.2);
				show.arc(around, hand, tick % 4 == 0 ? WHITE : color, 0.05, 1, false, 3);
				show.orb(hand.add(ArtKit.flat(player).scale(0.4)), WHITE, 0.15 + 0.02 * tick, 3);
				show.groundRing(player.position(), color, 2.4, 0.4, 0.05, 6);
				Motes.seek(level, around, hand, WHITE, 0.07, 5, 0.2);
				if (tick % 4 == 0) {
					// Through your own eyes: only a crackle low at the edge of the view.
					AuraFx.burst(level, player, hand, Vec3.ZERO, color, 0.9F, AuraFx.Burst.SPARKS);
				}
			});
		}
		Scheduler.later(ArtRules.SPEAR_CHARGE, () -> {
			if (!player.isAlive() || player.level() != level) {
				return;
			}
			loose(player, hits, color);
		});
		return true;
	}

	/**
	 * The lance: level down the way the swordsman faces, or tilted with their aim when they clearly look up or down (its tilt held
	 * to thirty degrees), until something solid stops it.
	 */
	private static void loose(ServerPlayer player, ArtKit.Hits hits, int color) {
		ServerLevel level = player.level();
		Vec3 look = player.getViewVector(1.0F);
		double pitch = ArtRules.spearTilt(look.y);
		Vec3 flat = ArtKit.flat(player);
		Vec3 dir = new Vec3(flat.x * Math.sqrt(1 - pitch * pitch), pitch, flat.z * Math.sqrt(1 - pitch * pitch)).normalize();
		Vec3 from = player.getEyePosition().subtract(0, 0.4, 0).add(dir.scale(0.6));
		Vec3 far = from.add(dir.scale(ArtRules.SPEAR_LENGTH));
		HitResult wall = level.clip(new ClipContext(from, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 end = wall.getType() == HitResult.Type.BLOCK ? wall.getLocation() : far;
		AuraFx.Art fx = hits.fx();
		fx.trail(AuraFxRules.Stroke.THRUST, false, 1.8F);
		// The lance itself, seen from outside: a beam of the aura's lightning, white-hot down its heart, jagging as it holds.
		ArtLight show = ArtLight.spectacle(player);
		show.ray(from, end, color, 0.75, 12);
		show.bare().ray(from, end, WHITE, 0.22, 10);
		show.arc(from, end, color, 0.14, 5, false, 9);
		// Through your own eyes: a thin white thread down the line, so you see where it went without it filling the view.
		ArtLight.world(player).bare().ray(from.add(dir.scale(1.6)), end, WHITE, 0.07, 8);
		// Where it ends, for everyone: a burst of lightning against the wall or the open air.
		AuraPhysicalFx.stormImpact(level, end, 1.4);
		ArtLight.world(player).ring(end, dir, color, 0.3, 2.4, 0.1, 9);
		ScreenFx.shake(level, player.position(), 0.3F, 12);
		AuraFx.sound(player, AuraFx.Sound.IMPACT, 1.0F, 0.7F);
		List<LivingEntity> struck = ArtKit.beam(player, from, end, ArtRules.SPEAR_HALF, ArtRules.SPEAR_TARGETS);
		for (int i = 0; i < struck.size(); i++) {
			LivingEntity foe = struck.get(i);
			hits.strike(foe, ArtRules.SPEAR_FACTOR, AuraFxRules.Weight.GRAND);
			ArtKit.shock(player, foe, ArtRules.SPEAR_SHOCK);
			// And the sky answering on each, one after another down the line.
			Scheduler.later(2 + i, () -> {
				if (foe.isAlive() || foe.isDeadOrDying()) {
					strike(player, foe.position(), color, 0.75F);
				}
			});
		}
	}
}
