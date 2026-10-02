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
import dev.wildercord.cast.Statuses;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Verdant Breath's arts: mending and binding. Verdant hurts the least of any method and mends the most: its cuts root foes where
 * they stand (vines round their legs: they can still strike, not walk away), and blossom, brambles and a grove mend the swordsman and
 * their allies while they fight. Every mending is held by {@link ArtKit#mend} (a body takes at most {@link ArtRules#MEND_CAP} from
 * all arts together, a health a second after), so Verdant buys time in a fight, never immunity. The roots, brambles, flowers and
 * trees are block displays ({@link ArtBlocks}): nothing grows in the world's own blocks.
 * <ul>
 * <li><b>Thorn Lash</b> (I): a lash of thorned vine flicked out past the sword's reach; the first foe it catches is rooted and
 * pricked by its thorns while it's held.</li>
 * <li><b>Blossom Fall</b> (II): a falling cut, and a carpet of blossom where it lands: you and your allies on it mended, foes on it
 * slowed.</li>
 * <li><b>Rooted Parry</b> (III): roots seize whoever struck, thorns burst round you, and you mend by what your guard caught.</li>
 * <li><b>Wild Growth</b> (IV): a second rush, the foes in the way cut and snagged, brambles springing up behind you that slow and
 * prick foes and mend allies.</li>
 * <li><b>Grove's Heart</b> (V): the blade planted in the ground, roots bursting under every foe near, and a grove rising round you
 * that mends your allies and binds your foes.</li>
 * </ul>
 */
public final class VerdantArts {
	private VerdantArts() {}

	public static final String METHOD = "verdant";
	public static final String THORN_LASH = "thorn_lash";
	public static final String BLOSSOM_FALL = "blossom_fall";
	public static final String ROOTED_PARRY = "rooted_parry";
	public static final String WILD_GROWTH = "wild_growth";
	public static final String GROVES_HEART = "groves_heart";

	/** The kinds of field Verdant leaves (for the tests). */
	public static final String BLOSSOM = "verdant_blossom";
	public static final String BRAMBLES = "verdant_brambles";
	public static final String GROVE = "verdant_grove";

	/** The life palette's pale green and blossom pink (cast.ElementFx.LIFE), a vine's deep green, and bark. */
	private static final int PALE = 0xE8FFB0;
	private static final int PINK = 0xFFA8D8;
	private static final int VINE = 0x3E8A34;
	private static final int BARK = 0x7A5634;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, THORN_LASH, VerdantArts::thornLash),
			MethodArts.art(AuraApi.ArtSlot.SECOND, BLOSSOM_FALL, VerdantArts::blossomFall),
			MethodArts.art(AuraApi.ArtSlot.THIRD, ROOTED_PARRY, VerdantArts::rootedParry),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, WILD_GROWTH, VerdantArts::wildGrowth),
			MethodArts.art(AuraApi.ArtSlot.FINAL, GROVES_HEART, VerdantArts::grovesHeart));
	}

	// ------------------------------------------------------------------ the look they share

	/**
	 * Roots bursting up round {@code foe}'s legs and closing on it: knotted stalks of root leaning in from each side (block displays,
	 * gone when the hold is), vines of light twisting up round it with blossom at their tips, and the ground cracking under it.
	 */
	static void rootsOn(ServerPlayer player, LivingEntity foe, int ticks, int stalks, float height) {
		ServerLevel level = player.level();
		Vec3 base = foe.position();
		double r = Math.max(0.4, foe.getBbWidth() * 0.5) + 0.35;
		RandomSource rand = level.getRandom();
		double phase = rand.nextDouble() * Math.PI * 2;
		BlockState roots = Blocks.MANGROVE_ROOTS.defaultBlockState();
		for (int i = 0; i < stalks; i++) {
			double a = phase + Math.PI * 2 * i / stalks;
			Vec3 at = base.add(Math.cos(a) * r, 0, Math.sin(a) * r);
			// Leaning in over the foe: the display's yaw turns its lean toward the middle.
			float yaw = (float) (Math.atan2(-Math.cos(a), -Math.sin(a)));
			ArtBlocks.spire(level, at, roots, 0.26F, height * (0.85F + 0.3F * rand.nextFloat()), yaw, 0.42F, Math.max(4, ticks - 10));
		}
		ArtLight world = ArtLight.world(player);
		for (int vine = 0; vine < 3; vine++) {
			for (int s = 0; s < 3; s++) {
				double a = phase + vine * Math.PI * 2 / 3 + s * 0.9;
				int c = s == 2 ? PINK : vine % 2 == 0 ? VINE : ArtKit.color(player);
				world.bare().slash(base.add(0, 0.15 + s * 0.38, 0), ElementFx.tilted(0.5, a + Math.PI / 2), ElementFx.flatDir(a), c, r * (1.05 - s * 0.15), 1.1,
					0.08, 2 + s * 2, Math.min(60, ticks));
			}
		}
		world.groundRing(base, VINE, r * 2.2, r * 0.8, 0.07, 10);
		Vfx.emit(level, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState()),
			base.add(0, 0.2, 0), 6, 0.3, 0.05);
		Feels.sound(level, base, "life_thorn_grow", 0.8F, 0.95F + rand.nextFloat() * 0.1F);
	}

	/** Thorns pricking: a few small pink and green splinters out of {@code foe}, and a tick of sound. */
	static void prick(ServerPlayer player, LivingEntity foe) {
		Vec3 c = foe.getBoundingBox().getCenter().subtract(0, foe.getBbHeight() * 0.2, 0);
		ArtLight.world(player).bare().shards(c, 0.45, 4, PINK, VINE);
		Vfx.emit(player.level(), ParticleTypes.COMPOSTER, c, 3, 0.25, 0.02);
		Feels.sound(player.level(), c, "life_thorn", 0.35F, 1.3F + player.level().getRandom().nextFloat() * 0.2F);
	}

	/** A body mended by an art: a soft bloom round it, petals, and a leaf spiral climbing it (seen from outside for its swordsman). */
	static void mended(ServerPlayer player, LivingEntity body, float amount) {
		if (amount <= 0) {
			return;
		}
		ServerLevel level = player.level();
		Vec3 c = body.getBoundingBox().getCenter();
		Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, c, (int) Math.max(2, Math.min(6, amount * 2)), body.getBbWidth() * 0.6, 0.02);
		if (body == player) {
			// Through your own eyes: a green glow low at the bottom of the view; the spiral round you is for everyone else.
			AuraFx.burst(level, player, c, Vec3.ZERO, ArtKit.color(player), 1.2F, AuraFx.Burst.FLASH | AuraFx.Burst.RING);
			ArtLight show = ArtLight.spectacle(player);
			double phase = level.getRandom().nextDouble() * Math.PI * 2;
			for (int i = 0; i < 3; i++) {
				double a = phase + i * 1.4;
				show.bare().slash(body.position().add(0, 0.3 + i * 0.5, 0), ElementFx.tilted(0.3, a + Math.PI / 2), ElementFx.flatDir(a), i % 2 == 0 ? PALE : PINK,
					0.7 - 0.08 * i, 1.4, 0.06, 1 + i, 9 + i);
			}
		} else {
			ElementFx.leafSpiral(level, body.position(), Math.max(0.5, body.getBbWidth()) * 0.8, body.getBbHeight() + 0.3, 3);
		}
	}

	// ------------------------------------------------------------------ I. Thorn Lash

	static boolean thornLash(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.DRAW, false, 1.45F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_thorn_lash", 1.0F, 1.0F);
		// The lash: a long thin arc of vine flicked out low across the front, past the sword's reach, pale at its heart, thorns
		// along it, and a crack of light at its tip.
		ArtLight world = ArtLight.world(player);
		Vec3 centre = feet.add(0, 0.55, 0).add(look.scale(0.4));
		double span = Math.toRadians(ArtRules.THORN_DEGREES) * 1.3;
		world.slash(centre, ArtKit.UP, look, color, ArtRules.THORN_REACH, span, 0.22, 2, 10);
		world.bare().slash(centre.add(0, 0.02, 0), ArtKit.UP, look, PALE, ArtRules.THORN_REACH - 0.06, span * 0.85, 0.06, 2, 9);
		double yaw = Math.atan2(look.z, look.x);
		for (int i = -2; i <= 2; i++) {
			double a = yaw + i * span / 5;
			Vec3 at = centre.add(Math.cos(a) * ArtRules.THORN_REACH * 0.85, 0, Math.sin(a) * ArtRules.THORN_REACH * 0.85);
			world.bare().shards(at, 0.3, 2, PINK, VINE);
		}
		Vec3 tip = centre.add(look.scale(ArtRules.THORN_REACH));
		Scheduler.later(2, () -> {
			world.flash(tip, PALE, 0.9F);
			ElementFx.petals(level, tip, 0.3, 4);
		});
		List<LivingEntity> foes = ArtKit.arc(player, context.struck(), ArtRules.THORN_REACH, ArtRules.THORN_DEGREES, ArtRules.THORN_TARGETS);
		for (int i = 0; i < foes.size(); i++) {
			LivingEntity foe = foes.get(i);
			hits.strike(foe, ArtRules.THORN_FACTOR, i == 0 ? AuraFxRules.Weight.HEAVY : AuraFxRules.Weight.FULL);
			ElementFx.petals(level, foe.getBoundingBox().getCenter(), 0.3, 3);
		}
		if (!foes.isEmpty()) {
			// The first it caught: bound where it stands, the thorns pricking it while the roots hold.
			LivingEntity bound = foes.getFirst();
			if (bound.isAlive()) {
				ArtKit.root(player, bound, ArtRules.THORN_ROOT);
				rootsOn(player, bound, ArtRules.THORN_ROOT, 4, 1.05F);
				for (int p = 1; p <= ArtRules.THORN_PRICKS; p++) {
					Scheduler.later(p * 10, () -> {
						if (bound.isAlive() && player.isAlive()) {
							hits.strike(bound, ArtRules.THORN_PRICK, AuraFxRules.Weight.LIGHT);
							prick(player, bound);
						}
					});
				}
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ II. Blossom Fall

	static boolean blossomFall(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.45F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_blossom_fall", 1.0F, 1.0F);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), ArtRules.BLOSSOM_REACH, ArtRules.BLOSSOM_DEGREES, ArtRules.BLOSSOM_TARGETS)) {
			hits.strike(foe, ArtRules.BLOSSOM_FACTOR);
			ElementFx.lifeImpact(level, foe.getBoundingBox().getCenter(), 0.8);
		}
		// Where the cut lands, a carpet of blossom bursts open.
		Vec3 ahead = feet.add(look.scale(ArtRules.BLOSSOM_AHEAD));
		Vec3 ground = ArtKit.floor(level, ahead.add(0, 1, 0), 1.5, 3);
		Vec3 centre = ground == null ? ahead : ground;
		ArtLight world = ArtLight.world(player);
		world.ground(centre, SigilOption.STAR, PINK, 1.5, ArtRules.BLOSSOM_TICKS, 0.03);
		world.ground(centre, SigilOption.CIRCLE, color, ArtRules.BLOSSOM_RADIUS, ArtRules.BLOSSOM_TICKS, -0.015);
		world.groundRing(centre, color, 0.3, ArtRules.BLOSSOM_RADIUS * 1.1, 0.16, 12);
		world.groundRing(centre, PINK, 0.2, ArtRules.BLOSSOM_RADIUS * 0.85, 0.06, 10);
		ElementFx.petals(level, centre.add(0, 1.2, 0), 1.6, 26);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 12; i++) {
			Vec3 at = centre.add((r.nextDouble() - 0.5) * 1.2, 0.2, (r.nextDouble() - 0.5) * 1.2);
			Motes.fling(level, at, new Vec3((r.nextDouble() - 0.5) * 1.4, 1.0, (r.nextDouble() - 0.5) * 1.4).normalize(), 0.16 + r.nextDouble() * 0.1,
				i % 3 == 0 ? PALE : PINK, 0.08, 30, new Vec3(0, -0.004, 0));
		}
		// Flowers spring up across it, and wither when it's gone.
		BlockState[] flowers = {Blocks.PINK_TULIP.defaultBlockState(), Blocks.ALLIUM.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(),
			Blocks.CORNFLOWER.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState()};
		for (int i = 0; i < 7; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double d = 0.6 + Math.sqrt(r.nextDouble()) * (ArtRules.BLOSSOM_RADIUS - 0.9);
			Vec3 at = centre.add(Math.cos(a) * d, 0, Math.sin(a) * d);
			Vec3 floor = ArtKit.floor(level, at.add(0, 1, 0), 1.2, 2);
			if (floor != null) {
				ArtBlocks.sprout(level, floor, flowers[i % flowers.length], 0.55F + 0.2F * r.nextFloat(), 0.0F, (float) (r.nextDouble() * Math.PI * 2), 1 + i,
					ArtRules.BLOSSOM_TICKS - 10);
			}
		}
		Feels.sound(level, centre, "life_bloom", 0.9F, 1.05F);
		// Mended at once as it opens: you and your allies on it.
		for (LivingEntity ally : alliesIn(player, centre, ArtRules.BLOSSOM_RADIUS)) {
			mended(player, ally, ArtKit.mend(player, ally, ArtRules.BLOSSOM_MEND));
		}
		ArtFields.open(player, BLOSSOM, ArtFields.disc(() -> centre, ArtRules.BLOSSOM_RADIUS, 1.8), ArtRules.BLOSSOM_TICKS, 5, (field, owner, age) -> {
			ServerLevel lv = field.level();
			RandomSource rr = lv.getRandom();
			if (age % 5 == 0) {
				double a = rr.nextDouble() * Math.PI * 2;
				double d = Math.sqrt(rr.nextDouble()) * ArtRules.BLOSSOM_RADIUS;
				ElementFx.petals(lv, centre.add(Math.cos(a) * d, 1.4 + rr.nextDouble(), Math.sin(a) * d), 0.4, 3);
			}
			if (age % 20 == 0) {
				ArtLight.world(owner).groundRing(centre, field.left() < 20 ? ArtKit.mix(PINK, 0x402030, 0.5) : PINK, ArtRules.BLOSSOM_RADIUS * 0.4,
					ArtRules.BLOSSOM_RADIUS, 0.05, 14);
				for (LivingEntity ally : field.allies(owner)) {
					mended(owner, ally, ArtKit.mend(owner, ally, ArtRules.BLOSSOM_PULSE));
				}
				for (LivingEntity foe : field.foes(owner)) {
					ArtKit.slow(owner, foe, ArtRules.BLOSSOM_SLOW, 0);
					Vfx.emit(lv, ParticleTypes.SPORE_BLOSSOM_AIR, foe.getBoundingBox().getCenter(), 4, 0.3, 0.0);
				}
				Feels.sound(lv, centre, "life_petals", 0.35F, 1.1F);
			}
		});
		return true;
	}

	/** The swordsman and their allies within {@code radius} of {@code centre} (level). */
	private static List<LivingEntity> alliesIn(ServerPlayer player, Vec3 centre, double radius) {
		List<LivingEntity> out = new ArrayList<>();
		for (net.minecraft.world.entity.Entity e : player.level().getEntities((net.minecraft.world.entity.Entity) null,
				new net.minecraft.world.phys.AABB(centre, centre).inflate(radius + 1, 3, radius + 1),
				e -> e instanceof LivingEntity && ArtKit.helpable(player, e))) {
			double dx = e.getX() - centre.x;
			double dz = e.getZ() - centre.z;
			if (dx * dx + dz * dz <= (radius + e.getBbWidth() / 2) * (radius + e.getBbWidth() / 2)) {
				out.add((LivingEntity) e);
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ III. Rooted Parry

	static boolean rootedParry(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 feet = player.position();
		LivingEntity foe = ArtKit.attacker(player, context, 4.0);
		AuraGuard.Caught caught = AuraGuard.caught(player);
		double blow = caught == null ? 0 : caught.damage();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.RISING, false, 1.35F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_rooted_parry", 1.0F, 1.0F);
		// Thorns bursting up round you, short and low (block displays), leaning out; a ring of vine racing out over the ground.
		BlockState roots = Blocks.MANGROVE_ROOTS.defaultBlockState();
		RandomSource r = level.getRandom();
		double phase = r.nextDouble() * Math.PI * 2;
		for (int i = 0; i < 6; i++) {
			double a = phase + Math.PI * 2 * i / 6;
			Vec3 at = feet.add(Math.cos(a) * 1.25, 0, Math.sin(a) * 1.25);
			float yaw = (float) Math.atan2(Math.cos(a), Math.sin(a));
			ArtBlocks.spire(level, at, roots, 0.24F, 0.55F + 0.25F * r.nextFloat(), yaw, 0.5F, 18);
		}
		ArtLight world = ArtLight.world(player);
		world.groundRing(feet, VINE, 0.4, ArtRules.ROOTED_THORNS * 1.1, 0.14, 10);
		world.groundRing(feet, PINK, 0.3, ArtRules.ROOTED_THORNS * 0.9, 0.05, 8);
		ElementFx.petals(level, feet.add(0, 0.6, 0), 1.0, 8);
		for (LivingEntity other : ArtKit.around(player, feet, ArtRules.ROOTED_THORNS, 1.0, 2.5, 6)) {
			if (other != foe) {
				hits.strike(other, ArtRules.ROOTED_THORN_FACTOR, AuraFxRules.Weight.LIGHT);
				ArtKit.slow(player, other, 20, 0);
				prick(player, other);
			}
		}
		if (foe != null) {
			hits.strike(foe, ArtRules.ROOTED_FACTOR);
			if (foe.isAlive()) {
				ArtKit.root(player, foe, ArtRules.ROOTED_ROOT);
				rootsOn(player, foe, ArtRules.ROOTED_ROOT, 5, 1.25F);
				ElementFx.lifeImpact(level, foe.getBoundingBox().getCenter(), 1.0);
			}
		}
		// And you mend by what your guard caught: the blow drawn up through the roots into you.
		float mend = ArtKit.mend(player, player, ArtRules.rootedMend(blow));
		Scheduler.later(3, () -> {
			if (player.isAlive()) {
				mended(player, player, Math.max(mend, 0.5F));
				Feels.sound(level, player.position(), "life_mend", 0.7F, 1.15F);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ IV. Wild Growth

	static boolean wildGrowth(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, ArtRules.WILD_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, WILD_GROWTH);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.LOW, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 from = path.getFirst();
		Vec3 to = path.getLast();
		AuraStep.afterimages(player, from, to, dir, color);
		Feels.sound(level, from.add(0, 1, 0), "aura_art_wild_growth", 1.05F, 1.0F);
		ArtLight world = ArtLight.world(player);
		ArtKit.dash(player, path, ArtRules.WILD_TICKS, (a, b, step, last) -> {
			// A green streak low along the way, leaves torn up behind it.
			world.ray(a.add(0, 0.12, 0), b.add(0, 0.12, 0), color, 0.42, 14);
			world.bare().ray(a.add(0, 0.14, 0), b.add(0, 0.14, 0), PALE, 0.1, 10);
			ElementFx.petals(level, b.add(0, 0.6, 0), 0.5, 4);
			Vec3 seg = b.subtract(a);
			for (LivingEntity foe : ArtKit.line(player, a, seg, Math.max(0.5, seg.horizontalDistance()) + 0.8, ArtRules.WILD_WIDTH / 2 + 0.3, 2.2,
					ArtRules.WILD_TARGETS)) {
				if (hits.hurt(foe) || hits.count() >= ArtRules.WILD_TARGETS) {
					continue;
				}
				hits.strike(foe, ArtRules.WILD_FACTOR);
				ArtKit.root(player, foe, ArtRules.WILD_SNAG);
				prick(player, foe);
			}
		});
		// The brambles: berry-bushes and roots springing up in turn behind you along the way, standing while the field does.
		RandomSource r = level.getRandom();
		List<Vec3> trail = new ArrayList<>();
		BlockState bush = Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3);
		BlockState young = Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 2);
		double length = from.distanceTo(to);
		for (double d = 0.8; d <= length + 0.01; d += 0.9) {
			Vec3 p = from.add(to.subtract(from).normalize().scale(d));
			trail.add(p);
			Vec3 side = ArtKit.right(dir).scale((r.nextDouble() - 0.5) * 0.9);
			Vec3 floor = ArtKit.floor(level, p.add(side).add(0, 0.8, 0), 1.0, 2);
			if (floor != null) {
				int index = trail.size();
				ArtBlocks.sprout(level, floor, index % 3 == 1 ? young : bush, 0.85F + 0.3F * r.nextFloat(), 0.0F, (float) (r.nextDouble() * Math.PI * 2),
					1 + index, ArtRules.WILD_FIELD - index - 12);
			}
		}
		trail.add(to);
		ArtFields.open(player, BRAMBLES, ArtFields.strip(trail, 0.9, 1.8), ArtRules.WILD_FIELD, 5, (field, owner, age) -> {
			ServerLevel lv = field.level();
			if (age % 10 == 0) {
				RandomSource rr = lv.getRandom();
				Vec3 p = trail.get(rr.nextInt(trail.size()));
				ElementFx.petals(lv, p.add(0, 0.8, 0), 0.4, 2);
				Vfx.emit(lv, ParticleTypes.COMPOSTER, p.add(0, 0.4, 0), 2, 0.35, 0.01);
			}
			if (age % 20 == 0) {
				for (LivingEntity foe : field.foes(owner)) {
					if (Statuses.claim(foe, "verdant_brambles", 18)) {
						ArtKit.slow(owner, foe, 30, 1);
						hits.strike(foe, ArtRules.WILD_PRICK, AuraFxRules.Weight.LIGHT);
						prick(owner, foe);
					}
				}
				for (LivingEntity ally : field.allies(owner)) {
					mended(owner, ally, ArtKit.mend(owner, ally, ArtRules.WILD_MEND));
				}
				Feels.sound(lv, field.shape().centre(), "life_grow", 0.3F, 1.2F);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ V. Grove's Heart

	static boolean grovesHeart(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.8F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_groves_heart", 1.3F, 1.0F);
		AuraFx.sound(player, AuraFx.Sound.IMPACT, 0.9F, 0.75F);
		ScreenFx.shake(level, feet, 0.2F, 12);
		// The blade planted: a seal of the grove opening on the ground, rings racing out, a column of green light where it went in
		// (seen from outside; round your own body it would fill the view).
		ArtLight world = ArtLight.world(player);
		ArtLight show = ArtLight.spectacle(player);
		Vec3 heart = feet.add(look.scale(0.9));
		world.ground(feet, SigilOption.CIRCLE, color, ArtRules.GROVE_RADIUS, ArtRules.GROVE_TICKS, 0.01);
		world.ground(feet, SigilOption.STAR, PINK, 2.4, ArtRules.GROVE_TICKS, -0.02);
		world.groundRing(feet, color, 0.5, ArtRules.GROVE_RADIUS * 1.2, 0.3, 14);
		world.groundRing(feet, PALE, 0.4, ArtRules.GROVE_RADIUS, 0.1, 12);
		show.ray(heart, heart.add(0, 7, 0), color, 0.8, 16);
		show.bare().ray(heart, heart.add(0, 6, 0), PALE, 0.22, 14);
		show.flash(heart.add(0, 1.0, 0), PALE, 3.0F);
		show.swirl(feet, 1.6, 4.0, 5, color, PINK);
		// Roots burst under every foe near: each struck, bound where it stands.
		for (LivingEntity foe : ArtKit.around(player, feet, ArtRules.GROVE_RADIUS, 1.5, 3.5, ArtRules.GROVE_TARGETS)) {
			hits.strike(foe, ArtRules.GROVE_FACTOR, AuraFxRules.Weight.GRAND);
			if (foe.isAlive()) {
				ArtKit.root(player, foe, ArtRules.GROVE_ROOT);
				rootsOn(player, foe, ArtRules.GROVE_ROOT, 3, 1.5F);
				ElementFx.crack(level, foe.position(), 1.0, 30);
				// A thread of root light from the heart of the grove to it, low over the ground.
				world.ray(feet.add(0, 0.08, 0), foe.position().add(0, 0.08, 0), VINE, 0.12, 16);
			}
		}
		// The grove: young trees rising round you (none straight ahead, so your own view stays open), a canopy of blossom on each.
		RandomSource r = level.getRandom();
		double facing = Math.atan2(look.z, look.x);
		List<Vec3> trees = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			double a = facing + Math.toRadians(45 + i * 54) + (r.nextDouble() - 0.5) * 0.25;
			Vec3 at = feet.add(Math.cos(a) * (ArtRules.GROVE_RADIUS - 0.8), 0, Math.sin(a) * (ArtRules.GROVE_RADIUS - 0.8));
			Vec3 floor = ArtKit.floor(level, at.add(0, 1, 0), 1.5, 3);
			if (floor == null) {
				continue;
			}
			trees.add(floor);
			int delay = 2 + i * 2;
			float height = 2.3F + 0.5F * r.nextFloat();
			BlockState canopy = (i % 2 == 0 ? Blocks.CHERRY_LEAVES : Blocks.FLOWERING_AZALEA_LEAVES).defaultBlockState();
			Scheduler.later(delay, () -> {
				ArtBlocks.spire(level, floor, Blocks.OAK_LOG.defaultBlockState(), 0.42F, height, (float) (r.nextDouble() * Math.PI), (float) ((r.nextDouble() - 0.5) * 0.15),
					ArtRules.GROVE_TICKS - delay - 10);
				ArtBlocks.sprout(level, floor, canopy, 1.7F + 0.4F * r.nextFloat(), height * 0.82F, (float) (r.nextDouble() * Math.PI), 3,
					ArtRules.GROVE_TICKS - delay - 18);
				Feels.sound(level, floor, "life_regrow", 0.6F, 0.9F + 0.05F * delay);
			});
		}
		ArtFields.open(player, GROVE, ArtFields.disc(() -> feet, ArtRules.GROVE_RADIUS, 2.5), ArtRules.GROVE_TICKS, 5, (field, owner, age) -> {
			ServerLevel lv = field.level();
			RandomSource rr = lv.getRandom();
			// Blossom drifting down from the canopies, and pollen of light rising through the grove.
			if (!trees.isEmpty()) {
				Vec3 tree = trees.get(rr.nextInt(trees.size()));
				ElementFx.petals(lv, tree.add(0, 2.6, 0), 0.8, 3);
			}
			Vec3 mote = feet.add((rr.nextDouble() - 0.5) * ArtRules.GROVE_RADIUS * 1.6, 0.2, (rr.nextDouble() - 0.5) * ArtRules.GROVE_RADIUS * 1.6);
			Motes.glow(lv, mote, rr.nextBoolean() ? PALE : PINK, 0.07, 30, new Vec3(0, 0.03, 0), 0.01);
			if (age % 20 == 0) {
				ArtLight.world(owner).groundRing(feet, field.left() < 30 ? ArtKit.mix(ArtKit.color(owner), 0x203018, 0.5) : ArtKit.color(owner),
					ArtRules.GROVE_RADIUS * 0.3, ArtRules.GROVE_RADIUS, 0.06, 16);
				for (LivingEntity ally : field.allies(owner)) {
					mended(owner, ally, ArtKit.mend(owner, ally, ArtRules.GROVE_MEND));
				}
				for (LivingEntity foe : field.foes(owner)) {
					foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 0, false, true), owner);
					hits.strike(foe, ArtRules.GROVE_PRICK, AuraFxRules.Weight.LIGHT);
					prick(owner, foe);
				}
			}
			if (age % 40 == 0) {
				Feels.sound(lv, feet, "life_lull", 0.45F, 1.0F);
			}
		});
		return true;
	}
}
