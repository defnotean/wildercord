package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.cast.BlockFx;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.WorldMagic;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Rime Breath's arts: slow, then freeze, then shatter. A Rime swordsman's foes grow heavier with frost the longer the fight goes:
 * crusts that freeze at the third, hail that slows, a counter that freezes whoever struck, and every frozen foe a thing to shatter
 * (frozen by an art they're frozen for a mage's Shatter too, the mod's own freeze).
 * <ul>
 * <li><b>Frostbite</b> (I): a cut that crusts and slows; the third crust soon after freezes a foe solid.</li>
 * <li><b>Hailfall</b> (II): a cut, and a cloud ahead that rains hail on everything under it.</li>
 * <li><b>Glacier Mirror</b> (III): whoever struck frozen solid, and a mirror of ice raised that turns projectiles back.</li>
 * <li><b>Skate</b> (IV): a glide along a path of ice laid as you go (over water too), the path quick for you and your allies and
 * slow for foes; a frozen foe in the way shatters.</li>
 * <li><b>Winter's Hush</b> (V): a cone of silence that freezes everything in it, and a moment later shatters what's still frozen.</li>
 * </ul>
 */
public final class RimeArts {
	private RimeArts() {}

	public static final String METHOD = "rime";
	public static final String FROSTBITE = "frostbite";
	public static final String HAILFALL = "hailfall";
	public static final String GLACIER_MIRROR = "glacier_mirror";
	public static final String SKATE = "skate";
	public static final String WINTERS_HUSH = "winters_hush";

	public static final String ICE_PATH = "rime_ice_path";
	public static final String MIRROR = "rime_mirror";

	/** The frost palette's white and deep blue (cast.ElementFx.FROST). */
	private static final int WHITE = 0xE6FAFF;
	private static final int DEEP = 0x3A8CFF;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, FROSTBITE, RimeArts::frostbite),
			MethodArts.art(AuraApi.ArtSlot.SECOND, HAILFALL, RimeArts::hailfall),
			MethodArts.art(AuraApi.ArtSlot.THIRD, GLACIER_MIRROR, RimeArts::glacierMirror),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, SKATE, RimeArts::skate),
			MethodArts.art(AuraApi.ArtSlot.FINAL, WINTERS_HUSH, RimeArts::wintersHush));
	}

	/** Whether {@code foe} is frozen now (by an art, a spell, or the third crust): what Skate and Winter's Hush shatter. */
	public static boolean frozen(LivingEntity foe) {
		return Reactions.has(foe, Reactions.Mark.FROZEN);
	}

	// ------------------------------------------------------------------ I. Frostbite

	static boolean frostbite(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.CUT, true, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		// A crescent of frost cut across in front, hard white at its edge (seen from outside: in your own first person it would
		// cut across the middle of the view), and ice flung off its tips for everyone.
		ArtLight show = ArtLight.spectacle(player);
		Vec3 centre = feet.add(0, 0.7, 0).add(look.scale(1.2));
		Vec3 normal = ArtKit.UP.add(ArtKit.bladeSide(player, look).scale(0.35)).normalize();
		show.slash(centre, normal, look, color, 2.0, 2.5, 0.4, 1, 10);
		show.slash(centre.add(0, 0.03, 0), normal, look, WHITE, 1.94, 2.2, 0.1, 1, 9);
		for (int tip = -1; tip <= 1; tip += 2) {
			double a = Math.atan2(look.z, look.x) + tip * 1.25;
			ice(level, centre.add(Math.cos(a) * 1.8, 0, Math.sin(a) * 1.8), 0.6, 3);
		}
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, centre.add(look.scale(0.6)), 6, 0.8, 0.02);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_frostbite", 1.0F, 1.0F);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), ArtRules.FROSTBITE_REACH, ArtRules.FROSTBITE_DEGREES, ArtRules.FROSTBITE_TARGETS)) {
			hits.strike(foe, ArtRules.FROSTBITE_FACTOR);
			if (!foe.isAlive()) {
				continue;
			}
			ArtKit.chill(player, foe, ArtRules.FROSTBITE_SLOW, 1);
			int crusts = ArtWards.crust(foe);
			crustLook(player, foe, crusts);
			if (crusts >= ArtRules.CRUSTS_TO_FREEZE) {
				freezeSolid(player, foe, ArtRules.FROSTBITE_FREEZE);
			}
		}
		return true;
	}

	/** The crusts a foe carries: a hard white ring at its feet for each, frost creeping under it, ice chipping off. */
	private static void crustLook(ServerPlayer player, LivingEntity foe, int crusts) {
		ServerLevel level = player.level();
		ArtLight world = ArtLight.world(player);
		Vec3 feet = foe.position();
		double w = Math.max(0.5, foe.getBbWidth());
		for (int i = 0; i < crusts; i++) {
			world.ring(feet.add(0, 0.12 + 0.32 * i, 0), ArtKit.UP, i == crusts - 1 ? WHITE : ElementFx.FROST.primary(), w * 0.9, w * 0.62, 0.06, 26);
		}
		ElementFx.frostCreep(level, feet, w * 0.9, 24);
		chips(level, feet.add(0, 0.5, 0), 3 + crusts * 2, 0.16);
	}

	/**
	 * Chips of ice: bright motes thrown out and falling, glinting, and snowflakes. (Not block or item fragments: those draw as
	 * dull little cubes, which read as clods of earth, not ice.)
	 */
	private static void chips(ServerLevel level, Vec3 at, int count, double speed) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < count; i++) {
			Vec3 dir = ElementFx.randomDir(r).add(0, 0.5, 0).normalize();
			Motes.fling(level, at, dir, speed * (0.6 + 0.8 * r.nextDouble()), i % 2 == 0 ? WHITE : ElementFx.FROST.primary(), 0.06,
				14 + r.nextInt(8), new Vec3(0, -0.02, 0));
		}
		Vfx.radial(level, ParticleTypes.SNOWFLAKE, at, Math.max(2, count / 2), speed * 0.5);
	}

	/** Ice flying apart: thin bright splinters out from {@code at}, and chips of ice. */
	private static void ice(ServerLevel level, Vec3 at, double reach, int count) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < count; i++) {
			Vec3 dir = ElementFx.randomDir(r).add(0, 0.25, 0).normalize();
			double length = reach * (0.55 + 0.45 * r.nextDouble());
			ElementFx.ray(level, at.add(dir.scale(0.12)), at.add(dir.scale(length)), i % 2 == 0 ? ElementFx.FROST.primary() : WHITE,
				0.035 + 0.02 * r.nextDouble(), 7 + r.nextInt(3));
		}
		chips(level, at, count + 2, 0.2);
	}

	/** Frozen solid: still, iced over (a shell of ice closing round it, a player's only briefly), a ring snapping out. */
	static boolean freezeSolid(ServerPlayer player, LivingEntity foe, int ticks) {
		boolean froze = ArtKit.freeze(player, foe, ticks);
		ServerLevel level = player.level();
		if (froze) {
			int t = foe instanceof Player ? ArtRules.PVP_HOLD_TICKS : ticks;
			BlockFx.encase(level, foe, t);
		}
		Vec3 heart = foe.getBoundingBox().getCenter();
		ArtLight.world(player).flash(heart, ElementFx.FROST.primary(), 1.2F);
		ElementFx.shatterRing(level, heart, 1.2);
		ice(level, heart, 0.8, 5);
		Feels.sound(level, foe.position(), "frost_lock", 0.8F, 1.1F);
		return froze;
	}

	// ------------------------------------------------------------------ II. Hailfall

	static boolean hailfall(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.RISING, true, 1.35F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_hailfall", 1.0F, 1.0F);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), ArtRules.HAIL_CUT_REACH, 120, 4)) {
			hits.strike(foe, ArtRules.HAIL_CUT_FACTOR, AuraFxRules.Weight.FULL);
			ArtKit.chill(player, foe, ArtRules.HAIL_SLOW, 0);
		}
		// The cloud: over the foe struck, or ahead on the ground.
		LivingEntity struck = context.struck();
		Vec3 centre;
		if (struck != null && struck.isAlive() && struck.distanceToSqr(player) < 7 * 7) {
			centre = struck.position();
		} else {
			Vec3 ahead = feet.add(look.scale(ArtRules.HAIL_AHEAD));
			Vec3 ground = ArtKit.floor(level, ahead, 1.5, 3);
			centre = ground == null ? ahead : ground;
		}
		ArtLight world = ArtLight.world(player);
		Vec3 sky = centre.add(0, 4.6, 0);
		world.sigil(sky, ArtKit.UP, SigilOption.CIRCLE, color, ArtRules.HAIL_RADIUS * 1.1, ArtRules.HAIL_TICKS + 12, 0.05);
		world.sigil(sky.add(0, -0.02, 0), ArtKit.UP, SigilOption.RING, WHITE, ArtRules.HAIL_RADIUS * 1.35, ArtRules.HAIL_TICKS + 12, -0.07);
		world.ground(centre, SigilOption.TARGET, color, ArtRules.HAIL_RADIUS, ArtRules.HAIL_TICKS + 8, 0.04);
		Motes.clouds(level, sky, 8, ArtRules.HAIL_RADIUS * 0.7, 0xD8F0FF, 0.9, ArtRules.HAIL_TICKS + 10, Vec3.ZERO, 0.01, 0.45);
		Map<UUID, Integer> struckBy = new HashMap<>();
		RandomSource r = level.getRandom();
		for (int i = 0; i < ArtRules.HAIL_STONES; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(r.nextDouble()) * ArtRules.HAIL_RADIUS * (i == 0 ? 0.2 : 1.0);
			Vec3 drop = centre.add(Math.cos(a) * d, 0, Math.sin(a) * d);
			int delay = 2 + i * ArtRules.HAIL_TICKS / ArtRules.HAIL_STONES;
			Scheduler.later(delay, () -> {
				if (!player.isAlive() || player.level() != level) {
					return;
				}
				// A stone of ice streaking down, and breaking where it lands.
				world.ray(drop.add(0, 4.4, 0), drop.add(0, 0.15, 0), WHITE, 0.09, 4);
				world.ray(drop.add(0, 4.4, 0), drop.add(0, 0.15, 0), color, 0.2, 3);
				Scheduler.later(2, () -> stone(player, hits, drop, color, struckBy));
			});
		}
		return true;
	}

	private static void stone(ServerPlayer player, ArtKit.Hits hits, Vec3 at, int color, Map<UUID, Integer> struckBy) {
		ServerLevel level = player.level();
		Vec3 ground = ArtKit.floor(level, at.add(0, 1, 0), 1.5, 3);
		Vec3 p = ground == null ? at : ground;
		ArtLight.world(player).ring(p.add(0, 0.1, 0), ArtKit.UP, WHITE, 0.1, 0.9, 0.05, 6);
		ice(level, p.add(0, 0.2, 0), 0.5, 3);
		Feels.sound(level, p, "frost_hail", 0.55F, 0.9F + level.getRandom().nextFloat() * 0.3F);
		for (LivingEntity foe : ArtKit.around(player, p, ArtRules.HAIL_STONE_REACH, 1.0, 3.0, 4)) {
			int n = struckBy.getOrDefault(foe.getUUID(), 0);
			if (n >= ArtRules.HAIL_PER_FOE) {
				continue;
			}
			struckBy.put(foe.getUUID(), n + 1);
			hits.strike(foe, ArtRules.HAIL_STONE_FACTOR, AuraFxRules.Weight.LIGHT);
			ArtKit.chill(player, foe, ArtRules.HAIL_SLOW, 0);
		}
	}

	// ------------------------------------------------------------------ III. Glacier Mirror

	static boolean glacierMirror(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		LivingEntity foe = ArtKit.attacker(player, context, 4.0);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SWEEP, false, 1.3F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_glacier_mirror", 1.0F, 1.0F);
		if (foe != null) {
			hits.strike(foe, ArtRules.MIRROR_FACTOR);
			if (foe.isAlive()) {
				freezeSolid(player, foe, ArtRules.MIRROR_FREEZE);
			}
		}
		for (LivingEntity other : ArtKit.arc(player, null, ArtRules.MIRROR_CHILL_REACH, 120, 6)) {
			if (other != foe) {
				ArtKit.chill(player, other, ArtRules.MIRROR_CHILL, 1);
				ElementFx.frostCreep(level, other.position(), 0.7, 16);
			}
		}
		// The mirror: a pane of ice held before the swordsman while it lasts (seen from outside; in your own view, a cold glint low).
		ArtWards.mirror(player, ArtRules.MIRROR_TICKS);
		mirrorLook(player, color, true);
		ArtFields.open(player, MIRROR, ArtFields.disc(player::position, 1.0, 2.0), ArtRules.MIRROR_TICKS, 5,
			(field, owner, age) -> mirrorLook(owner, ArtKit.color(owner), false));
		ElementFx.frostCreep(level, feet, 1.6, 30);
		return true;
	}

	/** The mirror of ice before the swordsman: a frosted pane of light facing forward, its rim white. */
	private static void mirrorLook(ServerPlayer player, int color, boolean first) {
		Vec3 look = ArtKit.flat(player);
		Vec3 at = player.position().add(0, 1.1, 0).add(look.scale(1.05));
		ArtLight show = ArtLight.spectacle(player);
		show.ring(at, look, color, 0.95, 1.0, 0.05, 6);
		show.ring(at.add(look.scale(0.01)), look, WHITE, 1.02, 1.05, 0.03, 6);
		show.sigil(at.add(look.scale(0.02)), look, SigilOption.STAR, color, 1.6, 6, 0.0);
		if (first) {
			show.flash(at, WHITE, 1.8F);
			// Through your own eyes: a thin cold glint low in the view, nothing across it.
			AuraFx.burst(player.level(), player, at, look, WHITE, 2.0F, AuraFx.Burst.RING | AuraFx.Burst.STAR);
		}
	}

	/** A projectile turned back by the mirror: a ringing glint where it struck the ice. */
	static void mirrorTurns(ServerPlayer player, Vec3 at) {
		ServerLevel level = player.level();
		ArtLight.world(player).ring(at, at.subtract(player.getEyePosition()), WHITE, 0.1, 0.8, 0.05, 6);
		ice(level, at, 0.4, 3);
		Feels.sound(level, at, "frost_mirror", 0.9F, 1.3F);
	}

	// ------------------------------------------------------------------ IV. Skate

	static boolean skate(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, ArtRules.SKATE_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, SKATE);
			return false;
		}
		// Frost laid on any water under the way first, so the glide carries on over it (vanilla's frosted ice, thawed in time).
		if (Config.get().aura().strings().artTerrain()) {
			List<BlockPos> under = new ArrayList<>();
			for (Vec3 p : path) {
				BlockPos below = BlockPos.containing(p.x, p.y - 0.5, p.z);
				if (!under.contains(below)) {
					under.add(below);
				}
			}
			WorldMagic.frostWater(player, under, 24);
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.LOW, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 from = path.getFirst();
		Vec3 to = path.getLast();
		AuraStep.afterimages(player, from, to, dir, color);
		Feels.sound(level, from.add(0, 1, 0), "aura_art_skate", 1.0F, 1.0F);
		// The path: sheets of ice laid flat along the way (only shapes over the ground, nothing the world keeps).
		float yaw = (float) Math.atan2(dir.x, dir.z);
		List<Vec3> trail = new ArrayList<>();
		for (int i = 0; i < path.size(); i += 5) {
			trail.add(path.get(i));
		}
		trail.add(to);
		for (int i = 0; i < trail.size(); i++) {
			Vec3 p = trail.get(i);
			Vec3 ground = ArtKit.floor(level, p.add(0, 0.5, 0), 0.6, 1.5);
			if (ground != null) {
				ArtBlocks.sheet(level, ground, i % 3 == 1 ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState(), 1.15F,
					yaw + (i % 2 == 0 ? 0.12F : -0.1F), 1 + i / 2, ArtRules.SKATE_PATH_TICKS);
			}
		}
		ArtLight world = ArtLight.world(player);
		Set<UUID> splintered = new HashSet<>();
		ArtKit.dash(player, path, ArtRules.SKATE_TICKS, (a, b, step, last) -> {
			world.ray(a.add(0, 0.08, 0), b.add(0, 0.08, 0), WHITE, 0.5, 16);
			world.ray(a.add(0, 0.6, 0), b.add(0, 0.6, 0), color, 0.18, 7);
			Vfx.emit(level, ParticleTypes.SNOWFLAKE, b.add(0, 0.3, 0), 4, 0.4, 0.02);
			Vec3 seg = b.subtract(a);
			for (LivingEntity foe : ArtKit.line(player, a, seg, Math.max(0.5, seg.horizontalDistance()) + 0.8, ArtRules.SKATE_WIDTH / 2 + 0.3, 2.2,
					ArtRules.SKATE_TARGETS)) {
				if (hits.hurt(foe) || hits.count() >= ArtRules.SKATE_TARGETS) {
					continue;
				}
				if (frozen(foe)) {
					shatter(player, hits, foe, ArtRules.SKATE_SHATTER, ArtRules.SKATE_SHARDS, ArtRules.SKATE_SHARD_FACTOR, splintered);
				} else {
					hits.strike(foe, ArtRules.SKATE_FACTOR);
					ArtKit.chill(player, foe, 40, 1);
					int crusts = ArtWards.crust(foe);
					crustLook(player, foe, crusts);
					if (crusts >= ArtRules.CRUSTS_TO_FREEZE) {
						freezeSolid(player, foe, ArtRules.FROSTBITE_FREEZE);
					}
				}
			}
			if (last) {
				ElementFx.frostCreep(level, b, 1.4, 30);
				ElementFx.shatterRing(level, b.add(0, 0.2, 0), 1.6);
			}
		});
		ArtFields.open(player, ICE_PATH, ArtFields.strip(trail, 0.8, 1.5), ArtRules.SKATE_PATH_TICKS, 5, (field, owner, age) -> {
			for (LivingEntity ally : field.allies(owner)) {
				ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 15, 0, false, false, true));
			}
			for (LivingEntity foe : field.foes(owner)) {
				ArtKit.chill(owner, foe, 15, 1);
			}
			if (age % 10 == 0) {
				RandomSource r = field.level().getRandom();
				Vec3 p = trail.get(r.nextInt(trail.size()));
				Vfx.emit(field.level(), ParticleTypes.SNOWFLAKE, p.add(0, 0.2, 0), 2, 0.4, 0.01);
				Motes.glow(field.level(), p.add((r.nextDouble() - 0.5) * 0.8, 0.12, (r.nextDouble() - 0.5) * 0.8), WHITE, 0.07, 14, new Vec3(0, 0.01, 0), 0.0);
			}
		});
		return true;
	}

	/**
	 * A frozen foe shattered: it takes {@code factor} weapons, the ice flies off it and cuts whoever stands within {@code shards}
	 * ({@code shardFactor}), and it thaws. Each foe is cut by flying ice once an art, however many shatter beside it ({@code cut}
	 * remembers who was): the balance pass found a packed crowd under Winter's Hush taking every neighbour's shards on top of its own.
	 */
	static void shatter(ServerPlayer player, ArtKit.Hits hits, LivingEntity foe, double factor, double shards, double shardFactor, Set<UUID> cut) {
		ServerLevel level = player.level();
		Vec3 c = foe.getBoundingBox().getCenter();
		hits.strike(foe, factor, AuraFxRules.Weight.GRAND);
		Spirits.thawNow(foe);
		ice(level, c, 1.6, 9);
		ElementFx.shatterRing(level, c, shards);
		chips(level, c, 12, 0.32);
		ArtLight.world(player).flash(c, WHITE, 1.6F);
		Feels.sound(level, c, "frost_break", 1.0F, 0.9F);
		for (LivingEntity other : ArtKit.around(player, foe.position(), shards, 1.5, 3.0, 6)) {
			if (other != foe && cut.add(other.getUUID())) {
				hits.strike(other, shardFactor, AuraFxRules.Weight.LIGHT);
			}
		}
	}

	// ------------------------------------------------------------------ V. Winter's Hush

	static boolean wintersHush(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SWEEP, false, 1.6F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_winters_hush", 1.2F, 1.0F);
		// The hush rolling out: crescents of frost low over the ground, wider as they go, frost seals laid behind them, snow hanging.
		ArtLight world = ArtLight.world(player);
		double span = Math.toRadians(ArtRules.HUSH_DEGREES);
		for (int k = 0; k < 6; k++) {
			double d = 1.2 + k * 1.15;
			int delay = k;
			Scheduler.later(1 + delay, () -> {
				Vec3 centre = feet.add(0, 0.35, 0);
				world.slash(centre, ArtKit.UP, look, color, d, span, 0.38, 1, 12);
				world.slash(centre.add(0, 0.03, 0), ArtKit.UP, look, WHITE, d * 0.98, span * 0.92, 0.1, 1, 10);
				Vfx.emit(level, ParticleTypes.SNOWFLAKE, feet.add(look.scale(d)).add(0, 0.8, 0), 6, d * 0.4, 0.01);
			});
		}
		for (int k = 1; k <= 3; k++) {
			Vec3 seal = feet.add(look.scale(k * 2.2));
			Vec3 ground = ArtKit.floor(level, seal.add(0, 1, 0), 1.5, 3);
			if (ground != null) {
				world.ground(ground, SigilOption.STAR, color, 1.1 + 0.4 * k, 50, 0.01);
			}
		}
		List<LivingEntity> frozen = new ArrayList<>();
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), ArtRules.HUSH_REACH, ArtRules.HUSH_DEGREES, ArtRules.HUSH_TARGETS)) {
			hits.strike(foe, ArtRules.HUSH_FACTOR);
			if (!foe.isAlive()) {
				continue;
			}
			freezeSolid(player, foe, ArtRules.HUSH_FREEZE);
			frozen.add(foe);
			if (foe instanceof ServerPlayer other) {
				ScreenFx.tint(other, WHITE, 30);
			}
		}
		// A moment later, the ice breaks: everything still frozen shatters.
		Scheduler.later(ArtRules.HUSH_SHATTER_DELAY, () -> {
			if (!player.isAlive() || player.level() != level) {
				return;
			}
			boolean any = false;
			Set<UUID> splintered = new HashSet<>();
			for (LivingEntity foe : frozen) {
				if (foe.isAlive()) {
					shatter(player, hits, foe, ArtRules.HUSH_SHATTER, ArtRules.HUSH_SHARDS, ArtRules.HUSH_SHARD_FACTOR, splintered);
					any = true;
				}
			}
			if (any) {
				Feels.sound(level, feet.add(look.scale(3)), "aura_art_winters_hush_shatter", 1.3F, 1.0F);
				ScreenFx.shake(level, feet.add(look.scale(3)), 0.25F, 12);
			}
		});
		return true;
	}
}
