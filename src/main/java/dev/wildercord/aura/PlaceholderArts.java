package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.Targets;
import dev.wildercord.config.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The common arts: the five a breathing method without arts of its own plays (an add-on's method that registers none; every
 * built-in method has its own). They step aside for a method's own ({@link AuraApi#registerArts}: all ten built-in methods have
 * theirs in {@code aura.arts}). They're
 * simple on purpose: a burst of aura off the blade, in the method's element, landing as projected aura (the slash's rules:
 * armour, and against a player the spell defences and the PvP scale), shaped a little differently a stage so each string can be
 * seen to work. Each cuts its own trail ({@link AuraFx}): the First a cut, the Second a rising cut, the Third an X, the Fourth a
 * thrust, the Final a whole turn, and lands heavily on each foe; its banner, the body's flare and the method's technique sound
 * come with every art performed.
 *
 * <p>The strings are the language every method shares, learned once:</p>
 * <ul>
 * <li><b>First Art</b> (Glow) {@code swing swing low}: an arc of aura in front.</li>
 * <li><b>Second Art</b> (Flow) {@code leap low}: a rising arc that lifts what it cuts (never a boss).</li>
 * <li><b>Third Art</b> (Edge) {@code counter}: straight after a perfect guard, a cut that staggers its foe again.</li>
 * <li><b>Fourth Art</b> (Form) {@code step}: straight after an Aura Step, a line cut ahead through everything in it.</li>
 * <li><b>Final Art</b> (Sovereign) {@code full full full low}: a ring of aura round you that throws foes back; only at the peak of
 *     momentum, as every Final Art ({@link AuraApi#FINAL_GATE}; a full pool where the server has momentum off).</li>
 * </ul>
 * <p>Their strikes go through {@code aura.arts.ArtKit.Hits} as every method's do, so momentum strengthens them, they wear stance
 * and they build momentum alike.</p>
 */
public final class PlaceholderArts {
	private PlaceholderArts() {}

	public static final String FIRST = "first_art";
	public static final String SECOND = "second_art";
	public static final String THIRD = "third_art";
	public static final String FOURTH = "fourth_art";
	public static final String FINAL = "final_art";
	/** The placeholder arts' ids, Art I to the Final Art. */
	public static final List<String> IDS = List.of(FIRST, SECOND, THIRD, FOURTH, FINAL);

	/** The five strings, one a stage, the same for every method ({@link AuraApi.ArtSlot}). */
	public static final SwordString FIRST_STRING = AuraApi.ArtSlot.FIRST.string;
	public static final SwordString SECOND_STRING = AuraApi.ArtSlot.SECOND.string;
	public static final SwordString THIRD_STRING = AuraApi.ArtSlot.THIRD.string;
	public static final SwordString FOURTH_STRING = AuraApi.ArtSlot.FOURTH.string;
	public static final SwordString FINAL_STRING = AuraApi.ArtSlot.FINAL.string;

	/**
	 * A full aura pool: what the Final Art waits on where the server has momentum off (and what it waited on before momentum).
	 * Every Final Art waits on {@link AuraApi#FINAL_GATE}, which is the peak of momentum ({@code Momentum.FINAL_GATE}) with this as
	 * its fallback.
	 */
	public static final AuraApi.ArtCondition FULL_POOL = AuraApi.ArtCondition.of(
		player -> StringRules.poolFull(Aura.aura(player), Aura.capacity(player)), "message.wildercord.aura.art.full_pool");

	/** Whether {@code player} plays the common arts: their method has none of its own. Both sides. */
	public static boolean playsCommon(net.minecraft.world.entity.player.Player player) {
		return !AuraApi.hasArts(Aura.data(player).method());
	}

	public static void register() {
		AuraApi.registerString(AuraApi.ArtSlot.FIRST.art(FIRST, StringRules.FIRST_COST, StringRules.FIRST_COOLDOWN, PlaceholderArts::first)
			.onlyFor(PlaceholderArts::playsCommon));
		AuraApi.registerString(AuraApi.ArtSlot.SECOND.art(SECOND, StringRules.SECOND_COST, StringRules.SECOND_COOLDOWN, PlaceholderArts::second)
			.onlyFor(PlaceholderArts::playsCommon));
		AuraApi.registerString(AuraApi.ArtSlot.THIRD.art(THIRD, StringRules.THIRD_COST, StringRules.THIRD_COOLDOWN, PlaceholderArts::third)
			.onlyFor(PlaceholderArts::playsCommon));
		AuraApi.registerString(AuraApi.ArtSlot.FOURTH.art(FOURTH, StringRules.FOURTH_COST, StringRules.FOURTH_COOLDOWN, PlaceholderArts::fourth)
			.onlyFor(PlaceholderArts::playsCommon));
		AuraApi.registerString(AuraApi.ArtSlot.FINAL.art(FINAL, StringRules.FINAL_COST, StringRules.FINAL_COOLDOWN, PlaceholderArts::last)
			.onlyFor(PlaceholderArts::playsCommon));
	}

	// ------------------------------------------------------------------ the five

	/** The First Art: an arc of aura in front. */
	private static boolean first(ServerPlayer player, AuraApi.StringContext context) {
		int color = Aura.color(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.CUT);
		dev.wildercord.aura.arts.ArtKit.Hits hits = dev.wildercord.aura.arts.ArtKit.hits(player, fx);
		AuraVfx.artArc(player, color, false);
		Aura.sound(player, "aura_slash", 0.8F, 1.3F);
		for (LivingEntity foe : arc(player, context.struck())) {
			hits.raw(foe, damage(player, StringRules.FIRST_FACTOR), AuraFxRules.Weight.HEAVY);
		}
		return true;
	}

	/** The Second Art: a rising arc that lifts what it cuts. */
	private static boolean second(ServerPlayer player, AuraApi.StringContext context) {
		int color = Aura.color(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.RISING);
		dev.wildercord.aura.arts.ArtKit.Hits hits = dev.wildercord.aura.arts.ArtKit.hits(player, fx);
		AuraVfx.artArc(player, color, true);
		Aura.sound(player, "aura_slash", 0.9F, 1.15F);
		Aura.sound(player, "aura_step", 0.4F, 1.5F);
		for (LivingEntity foe : arc(player, context.struck())) {
			hits.raw(foe, damage(player, StringRules.SECOND_FACTOR), AuraFxRules.Weight.HEAVY);
			if (foe.isAlive() && !Spirits.isBoss(foe)) {
				Vec3 v = foe.getDeltaMovement();
				foe.setDeltaMovement(v.x * 0.5, Math.max(v.y, StringRules.SECOND_LIFT), v.z * 0.5);
				foe.syncVelocity = true;
			}
		}
		return true;
	}

	/** The Third Art: a counter that staggers its foe again (the one the counter struck, or the nearest in front). */
	private static boolean third(ServerPlayer player, AuraApi.StringContext context) {
		int color = Aura.color(player);
		List<LivingEntity> foes = arc(player, context.struck());
		// An X cut in the aura's colour; where it lands, the parry's gold.
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.CROSS).color(AuraGuard.PERFECT_COLOR);
		dev.wildercord.aura.arts.ArtKit.Hits hits = dev.wildercord.aura.arts.ArtKit.hits(player, fx);
		AuraVfx.artCounter(player, color);
		Aura.sound(player, "aura_perfect_guard", 0.7F, 1.25F);
		Aura.sound(player, "aura_slash", 0.8F, 1.0F);
		if (!foes.isEmpty()) {
			LivingEntity foe = foes.getFirst();
			hits.raw(foe, damage(player, StringRules.THIRD_FACTOR), AuraFxRules.Weight.HEAVY);
			if (foe.isAlive()) {
				AuraGuard.stagger(player, foe);
			}
		}
		return true;
	}

	/** The Fourth Art: a line cut ahead, through everything in it. */
	private static boolean fourth(ServerPlayer player, AuraApi.StringContext context) {
		int color = Aura.color(player);
		Vec3 ahead = flat(player);
		Vec3 from = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.6F);
		dev.wildercord.aura.arts.ArtKit.Hits hits = dev.wildercord.aura.arts.ArtKit.hits(player, fx);
		AuraVfx.artLine(player, color, ahead, StringRules.FOURTH_LENGTH);
		Aura.sound(player, "aura_step", 0.7F, 1.3F);
		Aura.sound(player, "aura_slash", 0.9F, 0.9F);
		ServerLevel level = player.level();
		List<LivingEntity> line = new ArrayList<>();
		for (Entity e : level.getEntities(player, player.getBoundingBox().expandTowards(ahead.scale(StringRules.FOURTH_LENGTH)).inflate(1.5, 1.0, 1.5),
				e -> e instanceof LivingEntity && Targets.canHarm(player, e))) {
			Vec3 to = e.position().subtract(from);
			double along = to.x * ahead.x + to.z * ahead.z;
			double across = Math.abs(to.x * ahead.z - to.z * ahead.x);
			if (along >= -0.5 && along <= StringRules.FOURTH_LENGTH + 0.5 && across <= 1.25 + e.getBbWidth() / 2 && Math.abs(to.y) <= 2.0) {
				line.add((LivingEntity) e);
			}
		}
		line.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
		for (LivingEntity foe : line.subList(0, Math.min(line.size(), StringRules.ARC_TARGETS + 1))) {
			hits.raw(foe, damage(player, StringRules.FOURTH_FACTOR), AuraFxRules.Weight.HEAVY);
		}
		return true;
	}

	/** The Final Art: a ring of aura round you that throws foes back. */
	private static boolean last(ServerPlayer player, AuraApi.StringContext context) {
		int color = Aura.color(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SPIN, false, 1.4F);
		dev.wildercord.aura.arts.ArtKit.Hits hits = dev.wildercord.aura.arts.ArtKit.hits(player, fx);
		AuraVfx.artRing(player, color, StringRules.FINAL_RADIUS);
		Aura.sound(player, "aura_dominion", 0.7F, 1.3F);
		Aura.sound(player, "aura_slash", 1.0F, 0.8F);
		ServerLevel level = player.level();
		List<LivingEntity> ring = new ArrayList<>();
		for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(StringRules.FINAL_RADIUS, 2.5, StringRules.FINAL_RADIUS),
				e -> e instanceof LivingEntity && Targets.canHarm(player, e))) {
			if (e.position().subtract(player.position()).horizontalDistance() <= StringRules.FINAL_RADIUS + e.getBbWidth() / 2) {
				ring.add((LivingEntity) e);
			}
		}
		ring.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
		for (LivingEntity foe : ring.subList(0, Math.min(ring.size(), StringRules.FINAL_TARGETS))) {
			hits.raw(foe, damage(player, StringRules.FINAL_FACTOR), AuraFxRules.Weight.GRAND);
			Vec3 away = foe.position().subtract(player.position());
			if (foe.isAlive() && !Spirits.isBoss(foe) && away.horizontalDistanceSqr() > 1.0E-4) {
				foe.knockback(0.9, -away.x, -away.z, player.damageSources().playerAttack(player), 0.0F);
				foe.syncVelocity = true;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ helpers

	/** What a placeholder art lands at: the weapon's damage times {@code factor}, times the server's aura damage scale. */
	private static double damage(ServerPlayer player, double factor) {
		return Math.max(1.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE)) * factor * Config.get().aura().damageScale();
	}

	/** The foes in the arc in front (the one the string's last swing struck first, if it's still standing), nearest first. */
	private static List<LivingEntity> arc(ServerPlayer player, LivingEntity struck) {
		Vec3 look = flat(player);
		Vec3 at = player.position();
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(StringRules.ARC_REACH + 1, 1.5, StringRules.ARC_REACH + 1),
				e -> e instanceof LivingEntity && Targets.canHarm(player, e))) {
			Vec3 to = e.position().subtract(at);
			if (Math.abs(to.y) <= 2.0 && StringRules.inArc(to.x, to.z, look.x, look.z, StringRules.ARC_REACH + e.getBbWidth() / 2, StringRules.ARC_DEGREES)) {
				out.add((LivingEntity) e);
			}
		}
		out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
		if (struck != null && struck.isAlive() && Targets.canHarm(player, struck) && struck.distanceToSqr(player) < 36) {
			out.remove(struck);
			out.addFirst(struck);
		}
		return out.size() > StringRules.ARC_TARGETS ? new ArrayList<>(out.subList(0, StringRules.ARC_TARGETS)) : out;
	}

	private static Vec3 flat(ServerPlayer player) {
		Vec3 look = player.getViewVector(1.0F);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}
}
