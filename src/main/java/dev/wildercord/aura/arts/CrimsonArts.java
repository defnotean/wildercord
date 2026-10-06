package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Crimson Breath's arts: bleeding, drinking, frenzy, and the price of it. Crimson hurts the most of any method but Ember: its cuts
 * open wounds that bleed on (harder while their bearer runs, and marked for a wind spell's Rupture), and it drinks a share of what it
 * deals back as health, through the same bucket as any art's mending ({@link ArtKit#mend}: a body takes at most
 * {@link ArtRules#MEND_CAP} from arts, a health a second after), so a Crimson swordsman lasts longer in a fight but is never beyond
 * killing. Its Final Art is paid for in its own health: a gamble that never kills ({@link ArtRules#moonToll}). Its light is blood
 * red and dark, its voice a heartbeat.
 * <ul>
 * <li><b>Bloodletting</b> (I): a deep cut that opens a wound in the foes in front; it bleeds three seconds, and you drink a quarter
 * of what it bleeds.</li>
 * <li><b>Red Rain</b> (II): a falling cut that bursts where it lands, and a red rain there for two seconds: every foe under it
 * bleeds, and you drink from all of it.</li>
 * <li><b>Sanguine Parry</b> (III): the blow your guard caught turned into a wound in whoever struck it, bleeding its worth back out;
 * you drink half of all of it.</li>
 * <li><b>Frenzy</b> (IV): a second rush through foes; each cut, and each of your blows that lands after, quickens your blade, up to
 * a fifth faster, for five seconds.</li>
 * <li><b>Crimson Moon</b> (V): a great arc of blood-light that wounds every foe before you and drinks deeply from them, paid for with
 * a quarter of your health: a gamble, though it never leaves you under a heart.</li>
 * </ul>
 */
public final class CrimsonArts {
	private CrimsonArts() {}

	public static final String METHOD = "crimson";
	public static final String BLOODLETTING = "bloodletting";
	public static final String RED_RAIN = "red_rain";
	public static final String SANGUINE_PARRY = "sanguine_parry";
	public static final String FRENZY = "frenzy";
	public static final String CRIMSON_MOON = "crimson_moon";

	/** The kinds of field Crimson leaves (for the tests). */
	public static final String RAIN = "crimson_rain";

	/** The blood palette's pale red and its dark, drawn as darkness. */
	private static final int PALE = 0xFF6474;
	private static final int DARK = 0x5A0A14 | ArtLight.DARK;
	private static final int DEEP = 0x8A0E22;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, BLOODLETTING, CrimsonArts::bloodletting),
			MethodArts.art(AuraApi.ArtSlot.SECOND, RED_RAIN, CrimsonArts::redRain),
			MethodArts.art(AuraApi.ArtSlot.THIRD, SANGUINE_PARRY, CrimsonArts::sanguineParry),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, FRENZY, CrimsonArts::frenzy),
			MethodArts.art(AuraApi.ArtSlot.FINAL, CRIMSON_MOON, CrimsonArts::crimsonMoon));
	}

	// ------------------------------------------------------------------ the look they share

	/** A deep cut across {@code foe}: a crimson crescent, a paler one inside it, blood thrown off. */
	static void gash(ServerPlayer player, LivingEntity foe, boolean mirror) {
		ServerLevel level = player.level();
		Vec3 c = foe.getBoundingBox().getCenter();
		Vec3 toward = player.position().subtract(foe.position());
		toward = new Vec3(toward.x, 0, toward.z);
		toward = toward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : toward.normalize();
		Vec3 across = new Vec3(-toward.z, 0, toward.x).scale(mirror ? -1 : 1);
		Vec3 normal = toward.add(0, 0.25, 0).normalize();
		Vec3 bulge = across.add(0, mirror ? -0.6 : 0.6, 0).normalize();
		double r = Math.max(0.55, foe.getBbHeight() * 0.42);
		ArtLight world = ArtLight.world(player);
		world.slash(c.subtract(bulge.scale(r * 0.6)), normal, bulge, ArtKit.color(player), r, 2.2, 0.16, 1, 7);
		world.bare().slash(c.subtract(bulge.scale(r * 0.6)).add(toward.scale(0.03)), normal, bulge, PALE, r * 0.92, 1.9, 0.05, 1, 6);
		drops(level, c, 0.25, 5);
	}

	/**
	 * Blood thrown off {@code at}: drops of red light flicked out and falling, a little darker dust. (Not the redstone item
	 * particles the old blood drip used: the game test client draws item particles as tan cubes.)
	 */
	static void drops(ServerLevel level, Vec3 at, double spread, int count) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < count; i++) {
			Vec3 p = at.add((r.nextDouble() - 0.5) * 2 * spread, (r.nextDouble() - 0.5) * spread, (r.nextDouble() - 0.5) * 2 * spread);
			Vec3 flick = new Vec3((r.nextDouble() - 0.5) * 0.8, 0.35 + r.nextDouble() * 0.3, (r.nextDouble() - 0.5) * 0.8);
			Motes.fling(level, p, flick, 0.09, i % 3 == 0 ? PALE : i % 3 == 1 ? 0xD2283C : 0xA8142C, 0.055, 13 + r.nextInt(5), new Vec3(0, -0.075, 0));
		}
		Vfx.emit(level, new DustParticleOptions(DEEP, 0.8F), at, Math.max(1, count / 2), spread, 0.0);
	}

	/** A splash of blood where a great cut lands on {@code at}: a red flash, a heartbeat ring, drops thrown. */
	static void splash(ServerPlayer player, Vec3 at, double size) {
		ServerLevel level = player.level();
		ArtLight.world(player).flash(at, ArtKit.color(player), (float) (1.1 * size));
		AuraPhysicalFx.pulse(level, at, ArtKit.UP, 0.9 * size);
		drops(level, at, 0.2 * size, (int) Math.max(3, 5 * size));
	}

	/** Blood drawn to the swordsman: a thin thread of dark red from {@code foe}, drops of it sailing in. */
	static void drinkLook(ServerPlayer player, LivingEntity foe) {
		ServerLevel level = player.level();
		Vec3 from = foe.getBoundingBox().getCenter();
		Vec3 to = player.position().add(0, 0.8, 0);
		ArtLight world = ArtLight.world(player);
		world.bare().ray(from, to, DEEP, 0.05, 5);
		world.bare().ray(from, to, ArtKit.color(player), 0.02, 4);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 2; i++) {
			Motes.seek(level, from.add((r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.4), to, i == 0 ? PALE : DEEP, 0.07,
				10, 0.2);
		}
		if (r.nextInt(3) == 0) {
			Feels.sound(level, to, "blood_sip", 0.35F, 1.1F + r.nextFloat() * 0.2F);
		}
	}

	/** A bleed: drops falling off {@code foe}. */
	static void drip(LivingEntity foe) {
		if (foe.level() instanceof ServerLevel level) {
			drops(level, foe.getBoundingBox().getCenter(), 0.2, 2);
		}
	}

	/** A swordsman in a frenzy, a beat of it (asked by {@link ArtWards}): a heartbeat ring at their feet, red light off the blade. */
	static void frenzyLook(ServerPlayer player, int stacks) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		ArtLight world = ArtLight.world(player);
		world.groundRing(player.position(), color, 0.3, 0.7 + 0.18 * stacks, 0.05, 7);
		Motes.glow(level, ArtKit.hand(player), stacks >= 3 ? PALE : color, 0.06, 10, new Vec3(0, 0.02, 0), 0.02);
		if (stacks > 0) {
			Feels.sound(level, player.position(), "blood_pulse", 0.2F + 0.05F * stacks, 0.9F + 0.06F * stacks);
		}
	}

	// ------------------------------------------------------------------ I. Bloodletting

	static boolean bloodletting(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.CUT, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_bloodletting", 1.0F, 1.0F);
		ArtKit.Drink drink = new ArtKit.Drink(player, ArtRules.BLOOD_DRINK, 3.0, f -> drinkLook(player, f));
		List<LivingEntity> foes = ArtKit.arc(player, context.struck(), ArtRules.BLOOD_REACH, ArtRules.BLOOD_DEGREES, ArtRules.BLOOD_TARGETS);
		for (int i = 0; i < foes.size(); i++) {
			LivingEntity foe = foes.get(i);
			hits.strike(foe, ArtRules.BLOOD_FACTOR, i == 0 ? AuraFxRules.Weight.HEAVY : AuraFxRules.Weight.FULL);
			if (foe.isAlive()) {
				gash(player, foe, i % 2 == 1);
				AuraPhysicalFx.pulse(level, foe.getBoundingBox().getCenter(), ArtKit.UP, 0.9);
				ArtKit.wound(hits, foe, ArtRules.BLOOD_BLEED, ArtRules.BLOOD_BLEEDS, drink, CrimsonArts::drip);
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ II. Red Rain

	static boolean redRain(ServerPlayer player, AuraApi.StringContext context) {
		ReleasedArtOwner released = ReleasedArtOwner.capture(player);
		ServerLevel level = released.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_red_rain", 1.05F, 1.0F);
		Vec3 ahead = feet.add(look.scale(ArtRules.RAIN_AHEAD));
		Vec3 ground = ArtKit.floor(level, ahead.add(0, 1, 0), 1.5, 3);
		Vec3 centre = ground == null ? ahead : ground;
		ArtKit.Drink drink = new ArtKit.Drink(player, ArtRules.RAIN_DRINK, ArtRules.RAIN_DRINK_MAX, f -> drinkLook(player, f));
		// Where the cut lands, a burst of blood-light: rings racing out, a seal of it on the ground.
		ArtLight world = ArtLight.world(player);
		world.groundRing(centre, color, 0.3, ArtRules.RAIN_RADIUS * 1.2, 0.3, 10);
		world.groundRing(centre, PALE, 0.2, ArtRules.RAIN_RADIUS, 0.08, 9);
		world.bare().groundRing(centre, DARK, 0.4, ArtRules.RAIN_RADIUS * 1.35, 0.12, 12);
		world.ground(centre, SigilOption.CRACKED, DEEP, ArtRules.RAIN_RADIUS * 0.8, ArtRules.RAIN_TICKS + 10, 0);
		splash(player, centre.add(0, 0.5, 0), 1.3);
		ScreenFx.shake(level, centre, 0.12F, 8);
		for (LivingEntity foe : ArtKit.aroundVisible(player, centre, ArtRules.RAIN_RADIUS, 1.5, 3.0, ArtRules.RAIN_TARGETS)) {
			// Only this immediate burst gains owner LOS counterplay; delayed rain keeps its existing cover behavior.
			if (!released.valid()) break;
			if (!player.hasLineOfSight(foe)) continue;
			float took = hits.strike(foe, ArtRules.RAIN_FACTOR);
			if (!released.valid()) break;
			drink.from(foe, took);
			if (foe.isAlive()) {
				// Held where the rain falls (the strike's knock would carry it out from under it).
				ArtKit.steady(foe);
				gash(player, foe, foe.getId() % 2 == 0);
			}
		}
		if (!released.valid()) return true;
		// The rain: a red mist overhead, drops falling, a heartbeat over the ground; every foe under it bleeds, and you drink.
		Vec3 sky = centre.add(0, 3.2, 0);
		Motes.clouds(level, sky, 7, ArtRules.RAIN_RADIUS * 0.6, 0x7A1424, 1.0, ArtRules.RAIN_TICKS + 8, Vec3.ZERO, 0.01, 0.5);
		ArtFields.openReleased(player, released, RAIN, ArtFields.disc(() -> centre, ArtRules.RAIN_RADIUS, 2.5), ArtRules.RAIN_TICKS, 2, (field, owner, age) -> {
			ServerLevel lv = field.level();
			RandomSource r = lv.getRandom();
			ArtLight rain = ArtLight.world(owner);
			for (int i = 0; i < 7; i++) {
				double a = r.nextDouble() * Math.PI * 2;
				double d = Math.sqrt(r.nextDouble()) * ArtRules.RAIN_RADIUS;
				// Streaks of red light falling through the air at every height, the rain itself (and a little dust where it hangs).
				Vec3 top = centre.add(Math.cos(a) * d, 0.4 + r.nextDouble() * 2.8, Math.sin(a) * d);
				rain.ray(top, top.subtract(0, 0.9, 0), i % 2 == 0 ? 0xD2283C : PALE, 0.07, 4);
				if (i == 0) {
					Vfx.emit(lv, new DustParticleOptions(DEEP, 1.0F), top, 1, 0.1, 0.0);
				}
			}
			if (age % 4 == 0) {
				// Where it lands: small rings of red spreading on the ground.
				Vec3 splash = centre.add((r.nextDouble() - 0.5) * 2 * ArtRules.RAIN_RADIUS * 0.8, 0, (r.nextDouble() - 0.5) * 2 * ArtRules.RAIN_RADIUS * 0.8);
				rain.bare().groundRing(splash, 0xD2283C, 0.05, 0.45, 0.04, 6);
			}
			if (age % ArtRules.BLEED_PERIOD == 0) {
				AuraPhysicalFx.pulse(lv, centre.add(0, 0.1, 0), ArtKit.UP, ArtRules.RAIN_RADIUS * 0.9);
				for (LivingEntity foe : field.foes(owner)) {
					float took = hits.strike(foe, ArtRules.RAIN_BLEED, null);
					if (!field.active()) break;
					dev.wildercord.cast.Reactions.mark(foe, dev.wildercord.cast.Reactions.Mark.BLEEDING, 30);
					drip(foe);
					drink.from(foe, took);
				}
			}
			if (age % 10 == 0) {
				Feels.sound(lv, centre, "blood_drip", 0.4F, 0.9F + r.nextFloat() * 0.2F);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ III. Sanguine Parry

	static boolean sanguineParry(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 feet = player.position();
		LivingEntity foe = ArtKit.attacker(player, context, 4.0);
		AuraGuard.Caught caught = AuraGuard.caught(player);
		double blow = caught == null ? 0 : caught.damage();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.CROSS, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_sanguine_parry", 1.0F, 1.0F);
		ArtKit.Drink drink = new ArtKit.Drink(player, ArtRules.SANGUINE_DRINK, 6.0, f -> drinkLook(player, f));
		// A heartbeat through you as the blow is caught (a pulse of red low in your own view; round you for everyone else).
		AuraFx.burst(level, player, ArtKit.hand(player), ArtKit.flat(player), color, 1.3F, AuraFx.Burst.RING | AuraFx.Burst.FLASH);
		ArtLight show = ArtLight.spectacle(player);
		show.ring(feet.add(0, 1.0, 0), ArtKit.UP, color, 0.4, 1.6, 0.08, 8);
		show.bare().ring(feet.add(0, 1.0, 0), ArtKit.UP, PALE, 0.3, 1.2, 0.03, 11);
		// Blood thrown off the parry onto those near.
		for (LivingEntity other : ArtKit.around(player, feet, ArtRules.SANGUINE_SPRAY, 1.0, 2.5, 5)) {
			if (other != foe) {
				float took = hits.strike(other, ArtRules.SANGUINE_SPRAY_FACTOR, AuraFxRules.Weight.LIGHT);
				dev.wildercord.cast.Reactions.mark(other, dev.wildercord.cast.Reactions.Mark.BLEEDING, 40);
				drink.from(other, took);
				drip(other);
			}
		}
		if (foe == null) {
			return true;
		}
		float took = hits.strike(foe, ArtRules.SANGUINE_FACTOR);
		drink.from(foe, took);
		if (foe.isAlive()) {
			gash(player, foe, false);
			Scheduler.later(2, () -> {
				if (foe.isAlive()) {
					gash(player, foe, true);
				}
			});
			ArtKit.hold(player, foe, ArtRules.SANGUINE_HOLD);
			// The caught blow, turned into a wound in whoever struck it: bleeding its worth back out (at least a little for an arrow).
			double weapon = ArtKit.weapon(player);
			double wound = Math.max(weapon * 0.3, ArtRules.sanguineWound(weapon, blow));
			ArtKit.wound(hits, foe, wound / weapon / ArtRules.SANGUINE_BLEEDS, ArtRules.SANGUINE_BLEEDS, drink, CrimsonArts::drip);
			splash(player, foe.getBoundingBox().getCenter(), 1.0);
		}
		return true;
	}

	// ------------------------------------------------------------------ IV. Frenzy

	static boolean frenzy(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, ArtRules.FRENZY_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, FRENZY);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 from = path.getFirst();
		Vec3 to = path.getLast();
		AuraStep.afterimages(player, from, to, dir, color);
		Feels.sound(level, from.add(0, 1, 0), "aura_art_frenzy", 1.05F, 1.0F);
		// The frenzy begins as the rush does; each foe cut feeds it.
		ArtWards.frenzy(player, 0, true);
		ArtLight world = ArtLight.world(player);
		ArtKit.dash(player, path, ArtRules.FRENZY_TICKS, (a, b, step, last) -> {
			world.ray(a.add(0, 0.9, 0), b.add(0, 0.9, 0), color, 0.3, 9);
			world.bare().ray(a.add(0, 0.9, 0), b.add(0, 0.9, 0), PALE, 0.07, 7);
			world.bare().ray(a.add(0, 0.12, 0), b.add(0, 0.12, 0), DARK, 0.4, 14);
			drops(level, b.add(0, 0.8, 0), 0.3, 2);
			Vec3 seg = b.subtract(a);
			for (LivingEntity foe : ArtKit.line(player, a, seg, Math.max(0.5, seg.horizontalDistance()) + 0.8, ArtRules.FRENZY_WIDTH / 2 + 0.3, 2.2,
					ArtRules.FRENZY_TARGETS)) {
				if (hits.hurt(foe) || hits.count() >= ArtRules.FRENZY_TARGETS) {
					continue;
				}
				hits.strike(foe, ArtRules.FRENZY_FACTOR);
				if (foe.isAlive()) {
					gash(player, foe, hits.count() % 2 == 0);
				}
				int stacks = ArtWards.frenzy(player, 1, false);
				frenzyLook(player, stacks);
			}
			if (last) {
				AuraFx.burst(level, player, b.add(0, 1.0, 0), Vec3.ZERO, color, 1.4F, AuraFx.Burst.RING | AuraFx.Burst.SPARKS);
				Feels.sound(level, b, "blood_race", 0.7F, 1.1F);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ V. Crimson Moon

	static boolean crimsonMoon(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SWEEP, false, 1.9F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_crimson_moon", 1.3F, 1.0F);
		// The price, first: a quarter of your greatest health, straight from your health (never below a heart, so never your life).
		double toll = ArtRules.moonToll(player.getHealth(), player.getMaxHealth());
		if (toll > 0) {
			player.setHealth((float) (player.getHealth() - toll));
			ScreenFx.tint(player, DEEP, 24);
			Feels.sound(level, feet.add(0, 1, 0), "blood_heart", 1.0F, 0.85F);
			ArtLight.spectacle(player).ring(feet.add(0, 1.1, 0), look, color, 0.2, 1.4, 0.1, 8);
			drops(level, feet.add(0, 1.1, 0), 0.3, 6);
		}
		ArtKit.Drink drink = new ArtKit.Drink(player, ArtRules.MOON_DRINK, ArtRules.MOON_DRINK_MAX, f -> drinkLook(player, f));
		// The moon rising before you (seen from outside): a great crescent of blood-light standing over the foes ahead, its horns
		// down to their knees, dark at its edge, pale at its heart.
		ArtLight show = ArtLight.spectacle(player);
		Vec3 moon = feet.add(0, 0.5, 0).add(look.scale(3.6));
		show.slash(moon, look, ArtKit.UP, DARK, 2.8, 2.5, 0.95, 3, 16);
		show.slash(moon.add(look.scale(0.02)), look, ArtKit.UP, color, 2.68, 2.4, 0.65, 3, 15);
		show.bare().slash(moon.add(look.scale(0.04)), look, ArtKit.UP, PALE, 2.5, 2.1, 0.18, 3, 14);
		show.flash(moon.add(0, 2.4, 0), color, 3.0F);
		// The arc: crescents of it rolling out low over the ground before you, wider as they go.
		ArtLight world = ArtLight.world(player);
		double span = Math.toRadians(ArtRules.MOON_DEGREES);
		for (int k = 0; k < 5; k++) {
			double d = 1.2 + k * 1.2;
			int delay = k;
			Scheduler.later(1 + delay, () -> {
				Vec3 c = feet.add(0, 0.35, 0);
				world.slash(c, ArtKit.UP, look, color, d, span, 0.42, 1, 12);
				world.bare().slash(c.add(0, 0.02, 0), ArtKit.UP, look, PALE, d * 0.97, span * 0.94, 0.1, 1, 10);
				world.bare().slash(c.add(0, -0.01, 0), ArtKit.UP, look, DARK, d * 1.04, span, 0.24, 1, 14);
			});
		}
		ScreenFx.shake(level, feet, 0.3F, 14);
		AuraFx.sound(player, AuraFx.Sound.IMPACT, 1.0F, 0.7F);
		int n = 0;
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), ArtRules.MOON_RADIUS, ArtRules.MOON_DEGREES, ArtRules.MOON_TARGETS)) {
			int index = n++;
			Scheduler.later(1 + Math.min(4, (int) (foe.distanceTo(player) / 1.2)), () -> {
				if (!foe.isAlive() || !player.isAlive()) {
					return;
				}
				float took = hits.strike(foe, ArtRules.MOON_FACTOR, AuraFxRules.Weight.GRAND);
				drink.from(foe, took);
				if (foe.isAlive()) {
					gash(player, foe, index % 2 == 0);
					splash(player, foe.getBoundingBox().getCenter(), 1.2);
					ArtKit.wound(hits, foe, ArtRules.MOON_BLEED, ArtRules.MOON_BLEEDS, drink, CrimsonArts::drip);
				}
			});
		}
		return true;
	}
}
