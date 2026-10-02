package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.Stance;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The finishers: the strike that falls on an opened foe (see {@code aura.Stance}), one a breathing method, each its method's own
 * look and voice, and a common one for a method without its own. What a finisher does (a share of what the foe has lost, aura
 * back, momentum) is the same for all and decided in {@code Stance}; here is only how each one looks and sounds, and the small
 * touch of its element it leaves (a burn, a chill, a shock, a root...), never more than its method's arts would.
 *
 * <ul>
 * <li><b>Ember, Pyrebrand</b>: an X of fire branded across the foe, flames licking up round it, a pillar of flame through it; it
 *     burns.</li>
 * <li><b>Rime, Winterbreak</b>: the foe locked in frost for a breath, then the ice bursts outward in shards, spires of ice round its
 *     feet.</li>
 * <li><b>Thunder, Skysunder</b>: the blade brings the sky down: a bolt through the foe, arcs racing out over the ground.</li>
 * <li><b>Gale, Windscour</b>: a spiral of wind wraps the foe and lifts it, crescents whirling round it.</li>
 * <li><b>Stone, Faultline</b>: an overhead cleave; the ground cracks round the foe and stone bursts up behind it.</li>
 * <li><b>Verdant, Thornbloom</b>: roots seize the foe and a flower of light opens under it; its petals mend the swordsman and their
 *     allies.</li>
 * <li><b>Hollow, Nullfall</b>: the world falls into a black point at the foe's heart, rings collapsing in, then bursting out.</li>
 * <li><b>Starlit, Starbreak</b>: a star kindles in the foe and bursts, rays of light racing out (and more aura back than any).</li>
 * <li><b>Hourglass, Hour's End</b>: a clock face stands behind the foe and under it, its hands at the hour; the hour strikes and a
 *     second cut lands where the first did.</li>
 * <li><b>Crimson, Heartrend</b>: a crimson crescent tears through the foe and back; it bleeds, and the swordsman drinks.</li>
 * <li><b>Decisive Cut</b>, the common one: two white-gold crescents crossing through the foe.</li>
 * </ul>
 *
 * <p>Every finisher gets the same frame first ({@link #frame}): its trail, a grand impact (the longest hit-stop, for the swordsman
 * and a struck player), the body's aura blazing, the shared finisher stinger under its method's own voice, and a seal on the ground
 * at the foe's feet. Its banner comes from {@code Stance}. The swordsman's own first-person view keeps what lies low (rings and
 * seals on the ground, the trail, the impact); whatever stands up across the middle of the view (a pillar, an X, a clock, a
 * sphere) is {@link ArtLight#spectacle}, seen by everyone else and by the swordsman in third person.</p>
 */
public final class Finishers {
	private Finishers() {}

	public static final String PYREBRAND = "pyrebrand";
	public static final String WINTERBREAK = "winterbreak";
	public static final String SKYSUNDER = "skysunder";
	public static final String WINDSCOUR = "windscour";
	public static final String FAULTLINE = "faultline";
	public static final String THORNBLOOM = "thornbloom";
	public static final String NULLFALL = "nullfall";
	public static final String STARBREAK = "starbreak";
	public static final String HOURS_END = "hours_end";
	public static final String HEARTREND = "heartrend";
	public static final String DECISIVE_CUT = "decisive_cut";

	/** Every finisher's id, the ten methods' in element order, then the common one. */
	public static final List<String> IDS = List.of(PYREBRAND, WINTERBREAK, SKYSUNDER, WINDSCOUR, FAULTLINE, THORNBLOOM, NULLFALL, STARBREAK, HOURS_END,
		HEARTREND, DECISIVE_CUT);

	/** Each finisher's own voice (tools/feel/aura_finishers.py), for the tests: every one must exist. */
	public static final List<String> SOUNDS = List.of("aura_finisher_ember", "aura_finisher_rime", "aura_finisher_thunder", "aura_finisher_gale",
		"aura_finisher_stone", "aura_finisher_verdant", "aura_finisher_hollow", "aura_finisher_starlit", "aura_finisher_hourglass",
		"aura_finisher_crimson");

	private static final int WHITE = 0xFFFFFF;

	static void init() {
		AuraApi.registerFinisher(EmberArts.METHOD, new AuraApi.Finisher(PYREBRAND, Finishers::pyrebrand));
		AuraApi.registerFinisher(RimeArts.METHOD, new AuraApi.Finisher(WINTERBREAK, Finishers::winterbreak));
		AuraApi.registerFinisher(ThunderArts.METHOD, new AuraApi.Finisher(SKYSUNDER, Finishers::skysunder));
		AuraApi.registerFinisher(GaleArts.METHOD, new AuraApi.Finisher(WINDSCOUR, Finishers::windscour));
		AuraApi.registerFinisher(StoneArts.METHOD, new AuraApi.Finisher(FAULTLINE, Finishers::faultline));
		AuraApi.registerFinisher(VerdantArts.METHOD, new AuraApi.Finisher(THORNBLOOM, Finishers::thornbloom));
		AuraApi.registerFinisher(HollowArts.METHOD, new AuraApi.Finisher(NULLFALL, Finishers::nullfall));
		AuraApi.registerFinisher(StarlitArts.METHOD, new AuraApi.Finisher(STARBREAK, Finishers::starbreak));
		AuraApi.registerFinisher(HourglassArts.METHOD, new AuraApi.Finisher(HOURS_END, Finishers::hoursEnd));
		AuraApi.registerFinisher(CrimsonArts.METHOD, new AuraApi.Finisher(HEARTREND, Finishers::heartrend));
		AuraApi.commonFinisher(new AuraApi.Finisher(DECISIVE_CUT, Finishers::decisiveCut));
	}

	// ------------------------------------------------------------------ the frame every finisher shares

	/** Where a finisher falls: the foe's heart, its feet (on the ground under it), toward the swordsman, and square to that. */
	private record Where(Vec3 heart, Vec3 feet, Vec3 toward, Vec3 across, double size) {}

	private static Where where(ServerPlayer player, LivingEntity foe) {
		Vec3 heart = foe.getBoundingBox().getCenter();
		Vec3 floor = ArtKit.floor(player.level(), foe.position().add(0, 0.3, 0), 0.3, 4);
		Vec3 feet = floor == null ? foe.position() : floor;
		Vec3 toward = player.position().subtract(foe.position());
		toward = new Vec3(toward.x, 0, toward.z);
		toward = toward.lengthSqr() < 1.0E-4 ? ArtKit.flat(player).scale(-1) : toward.normalize();
		Vec3 across = new Vec3(-toward.z, 0, toward.x);
		double size = Math.max(0.9, Math.min(2.2, Math.max(foe.getBbWidth() * 1.4, foe.getBbHeight() * 0.6)));
		return new Where(heart, feet, toward, across, size);
	}

	/**
	 * The frame: the swordsman's trail ({@code stroke}), a grand impact on the foe (the longest hit-stop), the body's aura blazing, the
	 * shared stinger and the method's own voice ({@code voice}, or none), and a seal on the ground in {@code color}.
	 */
	private static Where frame(ServerPlayer player, LivingEntity foe, AuraFxRules.Stroke stroke, boolean mirror, int color, String voice) {
		ServerLevel level = player.level();
		Where w = where(player, foe);
		AuraFx.art(player).color(color).trail(stroke, mirror, 1.7F).impact(foe, AuraFxRules.Weight.GRAND).flare(40, 1.0F);
		Feels.sound(level, w.heart(), "aura_finisher", 1.1F, 1.0F);
		if (voice != null) {
			Feels.sound(level, w.heart(), voice, 1.0F, 1.0F);
		}
		AuraFx.sound(player, AuraFx.Sound.IMPACT, 1.0F, 0.72F);
		ArtLight world = ArtLight.world(player);
		world.groundRing(w.feet(), color, 0.3, 1.6 * w.size(), 0.12, 12);
		world.bare().groundRing(w.feet(), WHITE, 0.2, 1.1 * w.size(), 0.04, 9);
		AuraFx.burst(level, player, w.heart(), Vec3.ZERO, color, (float) (1.4 * w.size()), AuraFx.Burst.FLASH | AuraFx.Burst.STAR | AuraFx.Burst.SPARKS);
		return w;
	}

	private static int color(ServerPlayer player) {
		return Aura.color(player);
	}

	// ------------------------------------------------------------------ Ember: Pyrebrand

	/** An X of fire branded across the foe, flames licking up round it, a pillar of flame through it; it burns. */
	static void pyrebrand(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		ServerLevel level = player.level();
		int color = color(player);
		int gold = ElementFx.FIRE.secondary();
		int red = ElementFx.FIRE.accent();
		Where w = frame(player, foe, AuraFxRules.Stroke.CROSS, false, color, "aura_finisher_ember");
		ArtLight show = ArtLight.spectacle(player);
		// The brand: two crescents crossing at its heart, facing the swordsman.
		Vec3 up = ArtKit.UP;
		for (int i = -1; i <= 1; i += 2) {
			Vec3 bulge = w.across().scale(i).add(up.scale(0.75)).normalize();
			show.slash(w.heart().subtract(bulge.scale(0.5 * w.size())), w.toward(), bulge, i < 0 ? color : gold, 0.95 * w.size(), 2.4, 0.2, 1, 10);
			show.bare().slash(w.heart().subtract(bulge.scale(0.5 * w.size())).add(w.toward().scale(0.04)), w.toward(), bulge, WHITE, 0.9 * w.size(), 2.0,
				0.06, 1, 9);
		}
		show.tongues(w.feet(), 0.45 * w.size(), 1.9 * w.size(), 8, color, red, 14);
		// The pillar, a moment after: flame rising through it, a white heart.
		Scheduler.later(3, () -> {
			show.bare().ray(w.feet(), w.feet().add(0, 3.4 * w.size(), 0), color, 0.3 * w.size(), 12);
			show.bare().ray(w.feet(), w.feet().add(0, 3.0 * w.size(), 0), gold, 0.12 * w.size(), 10);
			show.tongues(w.feet().add(0, 0.8, 0), 0.32 * w.size(), 2.6 * w.size(), 6, gold, color, 12);
			show.flash(w.heart().add(0, 0.6, 0), gold, (float) (1.6 * w.size()));
			ArtLight.world(player).ground(w.feet(), SigilOption.CRACKED, red | ArtLight.DARK, 1.6 * w.size(), 50, 0);
			embers(level, w.heart(), 14, color, gold);
		});
		if (foe.isAlive()) {
			ArtKit.ignite(foe, 100);
		}
	}

	private static void embers(ServerLevel level, Vec3 at, int count, int a, int b) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < count; i++) {
			Vec3 dir = ElementFx.randomDir(r).add(0, 0.9, 0).normalize();
			Motes.fling(level, at, dir, 0.18 + r.nextDouble() * 0.16, i % 3 == 0 ? WHITE : i % 2 == 0 ? a : b, 0.07, 16 + r.nextInt(10), new Vec3(0, 0.01, 0));
		}
	}

	// ------------------------------------------------------------------ Rime: Winterbreak

	/** The foe locked in frost for a breath, then the ice bursts outward in shards, spires of ice round its feet. */
	static void winterbreak(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		ServerLevel level = player.level();
		int color = color(player);
		int frost = ElementFx.FROST.primary();
		int pale = ElementFx.FROST.secondary();
		Where w = frame(player, foe, AuraFxRules.Stroke.FALLING, false, color, "aura_finisher_rime");
		ArtLight show = ArtLight.spectacle(player);
		// The lock: rings of frost closing round it, low to high.
		for (int i = 0; i < 3; i++) {
			show.ring(w.feet().add(0, 0.25 + 0.6 * i * w.size(), 0), ArtKit.UP, i == 1 ? pale : frost, 0.95 * w.size(), 0.45 * w.size(), 0.07, 8);
		}
		AuraPhysicalFx.frostCreep(level, w.feet(), 1.4 * w.size(), 40);
		BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
		RandomSource r = level.getRandom();
		double phase = Math.atan2(w.toward().z, w.toward().x);
		for (int i = 0; i < 6; i++) {
			// Ice spires round its feet, leaning out; short ones on the near side, so they never stand up across the swordsman's view.
			double a = phase + Math.PI * 2 * i / 6 + Math.PI / 6;
			Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
			boolean near = out.dot(w.toward()) > 0.3;
			float height = (float) ((near ? 0.55 : 1.1 + 0.4 * r.nextDouble()) * w.size());
			float yaw = (float) Math.atan2(out.x, out.z);
			ArtBlocks.spire(level, w.feet().add(out.scale(0.75 * w.size())), ice, 0.32F, height, yaw, 0.45F, 14, false);
		}
		// Then it bursts: shards out of its heart, a ring standing up and snapping out, chips of ice.
		Scheduler.later(4, () -> {
			show.bare().shards(w.heart(), 2.2 * w.size(), 14, frost, WHITE);
			show.ring(w.heart(), w.toward(), pale, 0.3, 2.4 * w.size(), 0.1, 9);
			show.flash(w.heart(), WHITE, (float) (1.8 * w.size()));
			ArtLight.world(player).groundRing(w.feet(), WHITE, 0.4, 2.6 * w.size(), 0.06, 10);
			RimeArts.chips(level, w.heart(), 12, 0.24);
			Feels.sound(level, w.heart(), "frost_break", 0.9F, 1.1F);
		});
		if (foe.isAlive()) {
			ArtKit.chill(player, foe, 60, 2);
		}
	}

	// ------------------------------------------------------------------ Thunder: Skysunder

	/** The blade brings the sky down: a bolt through the foe, arcs racing out over the ground; it's left ionised. */
	static void skysunder(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		ServerLevel level = player.level();
		int color = color(player);
		int white = ElementFx.STORM.secondary();
		int blue = ElementFx.STORM.accent();
		Where w = frame(player, foe, AuraFxRules.Stroke.FALLING, false, color, "aura_finisher_thunder");
		ArtLight show = ArtLight.spectacle(player);
		ArtLight world = ArtLight.world(player);
		Vec3 sky = w.feet().add(0, 12, 0);
		show.arc(sky, w.heart(), white, 0.3 * w.size(), 4, false, 6);
		show.ray(sky, w.feet(), color, 0.28 * w.size(), 7);
		show.bare().ray(sky, w.feet(), WHITE, 0.09 * w.size(), 6);
		show.flash(w.heart().add(0, 0.8, 0), white, (float) (2.6 * w.size()));
		// Arcs racing out over the ground: flat, low, everyone's.
		RandomSource r = level.getRandom();
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6 + r.nextDouble() * 0.5;
			Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a)).scale(2.0 + r.nextDouble() * 1.2);
			world.arc(w.feet().add(0, 0.1, 0), w.feet().add(out).add(0, 0.1, 0), i % 2 == 0 ? color : blue, 0.08, 2, true, 6);
		}
		world.groundRing(w.feet(), white, 0.3, 2.8 * w.size(), 0.06, 8);
		world.ground(w.feet(), SigilOption.CRACKED, color | ArtLight.DARK, 1.5 * w.size(), 40, 0);
		ElementFx.sparks(level, w.feet().add(0, 0.3, 0), 14, 0.45);
		Feels.sound(level, w.feet(), "storm_boom", 1.0F, 1.15F);
		if (foe.isAlive()) {
			Reactions.mark(foe, Reactions.Mark.IONISED, 100);
		}
	}

	// ------------------------------------------------------------------ Gale: Windscour

	/** A spiral of wind wraps the foe and lifts it, crescents whirling round it, rings of wind racing over the ground. */
	static void windscour(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		ServerLevel level = player.level();
		int color = color(player);
		int teal = ElementFx.WIND.accent();
		Where w = frame(player, foe, AuraFxRules.Stroke.SPIN, false, color, "aura_finisher_gale");
		ArtLight show = ArtLight.spectacle(player);
		show.swirl(w.feet(), 1.1 * w.size(), 2.6 * w.size(), 7, color, WHITE);
		show.whirl(w.heart(), 1.0 * w.size(), 6, color, teal, WHITE);
		Scheduler.later(4, () -> {
			show.swirl(w.feet().add(0, 0.6, 0), 0.8 * w.size(), 2.8 * w.size(), 5, teal, WHITE);
			ArtLight.world(player).groundRing(w.feet(), teal, 0.5, 3.0 * w.size(), 0.07, 10);
		});
		ArtLight.world(player).groundRing(w.feet(), WHITE, 0.3, 2.2 * w.size(), 0.05, 8);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 10; i++) {
			double a = Math.PI * 2 * i / 10;
			Motes.fling(level, w.feet().add(Math.cos(a) * 0.8, 0.2, Math.sin(a) * 0.8), new Vec3(-Math.sin(a), 0.6, Math.cos(a)), 0.22 + r.nextDouble() * 0.1,
				i % 2 == 0 ? WHITE : color, 0.06, 16, new Vec3(0, 0.02, 0));
		}
		if (foe.isAlive()) {
			ArtKit.lift(foe, 0.55, 30);
		}
	}

	// ------------------------------------------------------------------ Stone: Faultline

	/** An overhead cleave: the ground cracks round the foe, stone bursts up behind it, it's left cracked and its footing slow. */
	static void faultline(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		ServerLevel level = player.level();
		int color = color(player);
		Where w = frame(player, foe, AuraFxRules.Stroke.FALLING, false, color, "aura_finisher_stone");
		BlockState earth = ArtBlocks.ground(level, w.feet());
		StoneArts.crackUnder(player, w.feet(), 2.2 * w.size(), 50, earth);
		ArtLight world = ArtLight.world(player);
		world.groundRing(w.feet(), ElementFx.EARTH.secondary(), 0.4, 3.0 * w.size(), 0.1, 11);
		// Stone bursting up on the far side, leaning away (the near side only low slabs, never across the swordsman's view).
		RandomSource r = level.getRandom();
		Vec3 away = w.toward().scale(-1);
		for (int i = 0; i < 5; i++) {
			double turn = Math.toRadians(-70 + 35 * i);
			Vec3 out = new Vec3(away.x * Math.cos(turn) - away.z * Math.sin(turn), 0, away.x * Math.sin(turn) + away.z * Math.cos(turn));
			float yaw = (float) Math.atan2(out.x, out.z);
			float height = (float) ((1.5 + 0.6 * r.nextDouble()) * w.size());
			int delay = 1 + i % 3;
			Vec3 base = w.feet().add(out.scale(1.2 * w.size()));
			Scheduler.later(delay, () -> ArtBlocks.spire(level, base, earth, 0.55F, height, yaw, 0.35F, 18));
		}
		for (int i = 0; i < 3; i++) {
			double turn = Math.toRadians(-45 + 45 * i);
			Vec3 out = new Vec3(w.toward().x * Math.cos(turn) - w.toward().z * Math.sin(turn), 0,
				w.toward().x * Math.sin(turn) + w.toward().z * Math.cos(turn));
			ArtBlocks.slab(level, w.feet().add(out.scale(1.0 * w.size())), earth, (float) Math.atan2(out.x, out.z), 0.5F, -0.4F, 2, 14);
		}
		AuraPhysicalFx.earthImpact(level, w.heart(), 1.2 * w.size());
		Feels.sound(level, w.feet(), "earth_quake", 1.0F, 0.9F);
		if (foe.isAlive()) {
			Reactions.mark(foe, Reactions.Mark.CRACKED, 100);
			foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, foe instanceof Player ? 0 : 1, false, true), player);
		}
	}

	// ------------------------------------------------------------------ Verdant: Thornbloom

	/** Roots seize the foe and a flower of light opens under it; its petals mend the swordsman and their allies near. */
	static void thornbloom(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		ServerLevel level = player.level();
		int color = color(player);
		Where w = frame(player, foe, AuraFxRules.Stroke.RISING, false, color, "aura_finisher_verdant");
		VerdantArts.bloom(ArtLight.world(player), w.feet(), color, 1.15 * w.size(), 40);
		if (foe.isAlive()) {
			VerdantArts.rootsOn(player, foe, 40, 5, (float) (1.3 * w.size()));
			ArtKit.root(player, foe, 30);
		}
		ArtLight show = ArtLight.spectacle(player);
		Scheduler.later(3, () -> {
			show.flash(w.heart(), 0xFFE8F4, (float) (1.6 * w.size()));
			VerdantArts.petals(level, w.heart().add(0, 0.4, 0), 0.8, 12);
		});
		// The petals mend: the swordsman and the allies near (held to every art's mending bucket).
		for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5.0), e -> ArtKit.helpable(player, e))) {
			float took = ArtKit.mend(player, body, context.practice() ? 0 : 3.0);
			VerdantArts.mended(player, body, took);
		}
	}

	// ------------------------------------------------------------------ Hollow: Nullfall

	/** The world falls into a black point at the foe's heart, rings collapsing in, then bursting out; foes near are drawn in. */
	static void nullfall(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		ServerLevel level = player.level();
		int color = color(player);
		int lilac = ElementFx.VOID.secondary();
		int abyss = 0x1A0830 | ArtLight.DARK;
		Where w = frame(player, foe, AuraFxRules.Stroke.DRAW, false, color, "aura_finisher_hollow");
		ArtLight show = ArtLight.spectacle(player);
		ArtLight world = ArtLight.world(player);
		world.ground(w.feet(), SigilOption.BAND, abyss, 2.0 * w.size(), 40, -0.2);
		// Falling in: rings on every tilt closing on the point, a black heart.
		for (int i = 0; i < 4; i++) {
			show.bare().ring(w.heart(), ElementFx.tilted(0.9, i * Math.PI / 2), i % 2 == 0 ? color : lilac, 2.4 * w.size(), 0.15, 0.06, 6);
		}
		show.shade(w.heart(), 0x1A0830, (float) (1.4 * w.size()));
		HollowArts.hole(show, w.heart(), 0.35 * w.size(), color, 6);
		world.groundRing(w.feet(), color, 2.6 * w.size(), 0.3, 0.07, 8);
		// Then out: a burst of violet.
		Scheduler.later(6, () -> {
			show.ring(w.heart(), w.toward(), color, 0.2, 2.6 * w.size(), 0.12, 8);
			show.bare().ring(w.heart(), ArtKit.UP, lilac, 0.2, 2.2 * w.size(), 0.05, 7);
			world.groundRing(w.feet(), lilac, 0.3, 2.8 * w.size(), 0.06, 9);
			AuraPhysicalFx.voidImpact(level, w.heart(), 1.2 * w.size());
			Feels.sound(level, w.heart(), "void_phantom_burst", 0.8F, 1.0F);
		});
		for (LivingEntity near : ArtKit.around(player, w.feet(), 4.0, 2, 2, 6)) {
			if (near != foe) {
				ArtKit.pull(near, w.feet(), 0.45);
			}
		}
		if (foe.isAlive()) {
			HollowArts.shadow(player, foe, 0.8);
		}
	}

	// ------------------------------------------------------------------ Starlit: Starbreak

	/** A star kindles in the foe and bursts, rays of light racing out; a star seal on the ground under it. */
	static void starbreak(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		ServerLevel level = player.level();
		int color = color(player);
		int pale = ElementFx.ARCANE.secondary();
		Where w = frame(player, foe, AuraFxRules.Stroke.THRUST, false, color, "aura_finisher_starlit");
		ArtLight show = ArtLight.spectacle(player);
		ArtLight world = ArtLight.world(player);
		world.ground(w.feet(), SigilOption.STAR, color, 1.8 * w.size(), 40, 0.15);
		show.bare().orb(w.heart(), WHITE, 0.25 * w.size(), 4);
		Scheduler.later(3, () -> {
			show.flash(w.heart(), WHITE, (float) (2.2 * w.size()));
			show.sigil(w.heart(), w.toward(), SigilOption.STAR, color, 1.4 * w.size(), 12, 0.3);
			for (int i = 0; i < 8; i++) {
				double a = Math.PI * 2 * i / 8 + Math.PI / 8;
				Vec3 dir = w.across().scale(Math.cos(a)).add(ArtKit.UP.scale(Math.sin(a)));
				show.ray(w.heart().add(dir.scale(0.3)), w.heart().add(dir.scale((i % 2 == 0 ? 2.6 : 1.7) * w.size())), i % 2 == 0 ? pale : color, 0.07, 9);
			}
			world.groundRing(w.feet(), pale, 0.3, 2.8 * w.size(), 0.06, 10);
			Motes.burst(level, w.heart(), 16, pale, 0.08, 18, 0.32);
			Feels.sound(level, w.heart(), "arcane_star_chime", 0.8F, 1.5F);
		});
	}

	// ------------------------------------------------------------------ Hourglass: Hour's End

	/** A clock face stands behind the foe and lies under it, its hands at the hour; the hour strikes and a second cut lands. */
	static void hoursEnd(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		ServerLevel level = player.level();
		int color = HourglassArts.gold(player);
		Where w = frame(player, foe, AuraFxRules.Stroke.CROSS, false, color, "aura_finisher_hourglass");
		ArtLight show = ArtLight.spectacle(player);
		ArtLight world = ArtLight.world(player);
		// Under it, flat on the ground (the swordsman's own view keeps this one), and behind it, standing (everyone else's).
		HourglassArts.clockFace(world, w.feet().add(0, 0.05, 0), ArtKit.UP, 1.4 * w.size(), Math.atan2(w.toward().z, w.toward().x), color, 24);
		Vec3 behind = w.heart().subtract(w.toward().scale(0.9)).add(0, 0.5, 0);
		HourglassArts.clockFace(show, behind, w.toward(), 1.3 * w.size(), Math.PI / 2, color, 24);
		ElementFx.goldenTicks(level, w.heart(), 0.5, 6);
		Scheduler.later(10, () -> {
			if (!player.isAlive()) {
				return;
			}
			// The hour strikes: a second cut where the first fell, time snapping back.
			Vec3 feet = player.position();
			HourglassArts.goldCut(player, feet, ArtKit.flat(player), color, true, 2.6, 1.1F);
			world.groundRing(w.feet(), color, 0.3, 2.6 * w.size(), 0.08, 9);
			show.flash(w.heart(), WHITE, (float) (1.6 * w.size()));
			Feels.sound(level, w.heart(), "time_resume", 0.9F, 0.9F);
			if (foe.isAlive()) {
				AuraFx.impact(player, foe, color, Aura.stage(player), AuraFxRules.Weight.HEAVY);
			}
		});
	}

	// ------------------------------------------------------------------ Crimson: Heartrend

	/** A crimson crescent tears through the foe and back; it bleeds, and the swordsman drinks from it. */
	static void heartrend(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		ServerLevel level = player.level();
		int color = color(player);
		int pale = ElementFx.BLOOD.secondary();
		Where w = frame(player, foe, AuraFxRules.Stroke.CUT, false, color, "aura_finisher_crimson");
		ArtLight show = ArtLight.spectacle(player);
		for (int i = 0; i < 2; i++) {
			boolean back = i == 1;
			Scheduler.later(back ? 3 : 0, () -> {
				Vec3 bulge = w.across().scale(back ? -1 : 1).add(0, back ? -0.55 : 0.55, 0).normalize();
				show.slash(w.heart().subtract(bulge.scale(0.7 * w.size())), w.toward().add(0, 0.2, 0).normalize(), bulge, color, 1.3 * w.size(), 2.4, 0.22,
					1, 9);
				show.bare().slash(w.heart().subtract(bulge.scale(0.7 * w.size())).add(w.toward().scale(0.04)), w.toward().add(0, 0.2, 0).normalize(), bulge,
					pale, 1.24 * w.size(), 2.0, 0.07, 1, 8);
				CrimsonArts.drops(level, w.heart(), 0.3, 8);
			});
		}
		AuraPhysicalFx.pulse(level, w.feet().add(0, 0.1, 0), ArtKit.UP, 1.4 * w.size());
		ArtLight.world(player).groundRing(w.feet(), ElementFx.BLOOD.accent() | ArtLight.DARK, 0.3, 2.0 * w.size(), 0.1, 12);
		if (foe.isAlive()) {
			Reactions.mark(foe, Reactions.Mark.BLEEDING, 100);
		}
		// It drinks: a share of what the finisher added, held to every art's mending bucket.
		float drunk = context.practice() ? 0 : ArtKit.drink(player, Math.min(4.0, context.extra() * 0.3));
		if (drunk > 0) {
			CrimsonArts.drinkLook(player, foe);
		}
	}

	// ------------------------------------------------------------------ the common one: Decisive Cut

	/** Two white-gold crescents crossing through the foe. */
	static void decisiveCut(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		int color = color(player);
		int gold = Stance.OPENED_COLOR;
		Where w = frame(player, foe, AuraFxRules.Stroke.CROSS, false, color == 0 ? gold : color, null);
		ArtLight show = ArtLight.spectacle(player);
		for (int i = -1; i <= 1; i += 2) {
			Vec3 bulge = w.across().scale(i).add(0, 0.8, 0).normalize();
			show.slash(w.heart().subtract(bulge.scale(0.5 * w.size())), w.toward(), bulge, i < 0 ? gold : color == 0 ? WHITE : color, 1.0 * w.size(), 2.3,
				0.18, 1, 10);
		}
		show.flash(w.heart(), WHITE, (float) (1.6 * w.size()));
	}
}
