package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.ExplorerNumbers;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.IntConsumer;

/**
 * What the runes of the world do: the effects found only in particular places (structures,
 * biomes through Attunement, Wildercord's dungeons and bosses, and world events), plus the hooks
 * for their modifiers (Trial Key, Kindled, Unstable) and their lasting marks (Cinderbrand,
 * Eclipse). {@link Effects} hands any effect it doesn't know itself to {@link #apply}. Numbers match
 * the rune descriptions in {@link Runes}; visuals are in {@link ExplorerVfx}.
 */
public final class ExplorerEffects {
	private ExplorerEffects() {}

	/** At most this many creatures a single area effect of these runes lights up or reveals. */
	private static final int MAX_REVEALED = 32;

	static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration) {
		RuneDef rune = node.effect;
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		double radius = SpellNumbers.effectRadius(node);
		switch (Effects.builtIn(rune) ? rune.path() : "") {
			// ---- Vanilla structures
			case "echolocate" -> echolocate(cast, hit, harmed, 16.0 * radius, duration);
			case "resonant_shriek" -> harmed.forEach(t -> resonantShriek(cast, t, power, duration));
			case "tidecall" -> tidecall(cast, hit.point(), 3.5 * radius, power);
			case "infest" -> harmed.forEach(t -> infest(cast, t, power, duration));
			case "sandstorm" -> sandstorm(cast, hit.point(), 3.5 * radius, power, Effects.ticks(4, duration));
			case "vinelash" -> harmed.forEach(t -> vinelash(cast, t, power));
			case "remedy" -> remedy(cast, hit, helped, power, duration);
			case "warcry" -> warcry(cast, hit, helped, 8.0 * radius, Effects.ticks(12, duration));
			case "fangs" -> harmed.forEach(t -> fangs(cast, t, power));
			case "undertow" -> harmed.forEach(t -> undertow(cast, t, power, duration));
			case "treasure_sense" -> helped.forEach(t -> treasureSense(cast, t, Effects.ticks(60, duration)));
			case "tusk_charge" -> tuskCharge(cast, power);
			case "blazecall" -> harmed.forEach(t -> blazecall(cast, t, power));
			case "shulkershell" -> helped.forEach(t -> shulkershell(cast, t, Effects.ticks(4, duration)));
			case "portalfall" -> harmed.forEach(t -> portalfall(cast, t, power));
			case "ancient_seed" -> ancientSeed(cast, hit, radius);
			// ---- Attuned in the biomes
			case "moonpetal" -> moonpetal(cast, hit.point(), 3.0 * radius, power);
			case "hoarfrost" -> harmed.forEach(t -> hoarfrost(cast, t, power, duration));
			case "hush" -> hush(cast, hit.point(), 4.0 * radius, Effects.ticks(6, duration));
			case "sporebloom" -> sporebloom(cast, hit.point(), 3.0 * radius, duration);
			case "sunscorch" -> harmed.forEach(t -> sunscorch(cast, t, power, duration));
			case "mire" -> harmed.forEach(t -> mire(cast, t, Effects.ticks(5, duration)));
			case "glowvine" -> glowvine(cast, hit, radius);
			case "rootsnare" -> rootsnare(cast, hit.point(), 3.0 * radius, power, Effects.ticks(2, duration));
			case "stalactite" -> harmed.forEach(t -> stalactite(cast, t, power));
			case "summit_wind" -> summitWind(cast, hit, 3.0 * radius, power);
			case "soulfire" -> harmed.forEach(t -> soulfire(cast, t, power, Effects.ticks(5, duration)));
			case "warp_step" -> warpStep(cast, hit);
			case "blood_moss" -> harmed.forEach(t -> bloodMoss(cast, t, power, Effects.ticks(6, duration)));
			case "basalt_surge" -> basaltSurge(cast, hit, 1.3 * radius, power);
			case "starlight_tether" -> harmed.forEach(t -> starlightTether(cast, t, hit.point(), power, Effects.ticks(6, duration)));
			// ---- Wildercord's dungeons, their bosses and world events
			case "cinderbrand" -> harmed.forEach(t -> cinderbrand(cast, t, power, Effects.ticks(6, duration)));
			case "ashen_veil" -> helped.forEach(t -> ashenVeil(cast, t, Effects.ticks(10, duration)));
			case "cinderheart" -> helped.forEach(t -> cinderheart(cast, t, power, Effects.ticks(12, duration)));
			case "eclipse" -> eclipse(cast, hit.point(), 4.0 * radius, power, Effects.ticks(5, duration));
			case "starmaw" -> harmed.forEach(t -> starmaw(cast, t, power));
			case "drowning_word" -> harmed.forEach(t -> drowningWord(cast, t, power, Effects.ticks(5, duration)));
			case "tidewrit" -> tidewrit(cast, hit, 7.0 * radius, power);
			case "starshard" -> harmed.forEach(t -> starshard(cast, t, power));
			case "riftcall" -> riftcall(cast, hit.point(), 5.0 * radius, power, Effects.ticks(3, duration));
			case "manaburn" -> harmed.forEach(t -> manaburn(cast, t, power));
			case "manatide" -> helped.forEach(t -> manatide(cast, t, Effects.ticks(10, duration)));
			default -> { }
		}
	}

	// ------------------------------------------------------------------ modifiers and marks

	/** Unstable: this landing's power swing (1 without it), with a flicker where it lands. */
	static double swing(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit) {
		if (node.count(Runes.UNSTABLE) == 0) {
			return 1.0;
		}
		double swing = SpellNumbers.unstableSwing(node, cast.level.getRandom().nextDouble());
		ExplorerVfx.unstable(cast.level, hit.point(), swing);
		return swing;
	}

	/** Kindled: whatever the effect struck is set alight too. */
	static void kindle(Cast cast, SpellPlan.EffectNode node, List<LivingEntity> harmed, double duration) {
		int seconds = SpellNumbers.kindledSeconds(node);
		if (seconds <= 0) {
			return;
		}
		for (LivingEntity t : harmed) {
			if (t.isAlive()) {
				t.igniteForSeconds((float) (seconds * duration));
				ExplorerVfx.kindled(cast.level, t);
			}
		}
	}

	/** A mark one caster left on a target, until a game time. */
	private record Mark(UUID caster, long until) {}

	/** Cinderbrand's brands and Eclipse's shadow, by target. */
	private static final Map<UUID, Mark> BRANDED = new HashMap<>();
	private static final Map<UUID, Mark> ECLIPSED = new HashMap<>();

	private static void mark(Map<UUID, Mark> marks, Cast cast, LivingEntity t, int ticks) {
		long now = cast.level.getGameTime();
		marks.put(t.getUUID(), new Mark(cast.caster.getUUID(), now + ticks));
		if (marks.size() > 256) {
			marks.values().removeIf(m -> m.until() < now);
		}
	}

	private static boolean marked(Map<UUID, Mark> marks, Cast cast, LivingEntity t) {
		Mark mark = marks.get(t.getUUID());
		return mark != null && mark.caster().equals(cast.caster.getUUID()) && mark.until() >= cast.level.getGameTime();
	}

	/** Extra damage from marks this caster left: a Cinderbrand burns fire hotter, an Eclipse makes everything land harder. */
	static double bonus(Cast cast, LivingEntity target, String element) {
		double bonus = 1.0;
		if ("fire".equals(element) && marked(BRANDED, cast, target)) {
			bonus *= 1.5;
		}
		if (marked(ECLIPSED, cast, target)) {
			bonus *= 1.2;
		}
		return bonus;
	}

	/** Marks and timers go when the server stops. */
	public static void init() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
	}

	/** Forgets every mark and ritual timer. */
	static void clear() {
		BRANDED.clear();
		ECLIPSED.clear();
		DRANK.clear();
		LINGERING.clear();
	}

	/**
	 * A lingering effect on a creature: whose it is (null when anyone's refreshes it, as for a
	 * shared attribute modifier) and of which rune. A new one from the same source refreshes the
	 * old instead of stacking another copy on top.
	 */
	private record Linger(UUID target, UUID caster, String rune) {}

	private static final Map<Linger, Object> LINGERING = new HashMap<>();

	/** Starts (or restarts) a lingering effect; the token stays current until a newer one replaces it. */
	private static Object linger(Linger key) {
		Object token = new Object();
		LINGERING.put(key, token);
		return token;
	}

	private static boolean current(Linger key, Object token) {
		return LINGERING.get(key) == token;
	}

	private static void release(Linger key, Object token) {
		LINGERING.remove(key, token);
	}

	/** Soulfire's mana given back so far, and Manaburn's mana taken from each target so far, per cast. */
	private static final Map<Object, double[]> REFUNDED = java.util.Collections.synchronizedMap(new WeakHashMap<>());
	private static final Map<Object, Map<UUID, double[]>> BURNED = java.util.Collections.synchronizedMap(new WeakHashMap<>());

	// ------------------------------------------------------------------ helpers

	/**
	 * Runs {@code step} every {@code every} ticks for {@code ticks} ticks (the first at once, the last
	 * before the time is up: see {@link ExplorerNumbers#pulses}), while the cast lasts, then {@code end}.
	 * What they deal is lingering damage, which a Shield can block but not parry.
	 */
	private static void repeat(Cast cast, int ticks, int every, IntConsumer step, Runnable end) {
		for (int tick : ExplorerNumbers.pulses(ticks, every)) {
			Scheduler.later(Math.max(1, tick), Effects.carryContext(() -> {
				if (cast.alive()) {
					Effects.lingering(() -> step.accept(tick));
				}
			}));
		}
		Scheduler.later(ticks + 1, Effects.carryContext(() -> Effects.lingering(end)));
	}

	/** Every enemy within {@code radius} of the point (by the middle of its body). */
	private static List<LivingEntity> enemiesAround(Cast cast, Vec3 point, double radius) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius + 1), e -> Targets.canHarm(cast.caster, e))) {
			if (e.getBoundingBox().getCenter().distanceTo(point) <= radius + e.getBbWidth() / 2 && out.size() < MAX_REVEALED) {
				out.add((LivingEntity) e);
			}
		}
		return out;
	}

	/** You and every ally within {@code radius} of the point. */
	private static List<LivingEntity> alliesAround(Cast cast, Vec3 point, double radius) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius + 1),
				e -> e instanceof LivingEntity && e.isAlive() && Targets.canHelp(cast.caster, e))) {
			if (e.getBoundingBox().getCenter().distanceTo(point) <= radius + e.getBbWidth() / 2 && out.size() < MAX_REVEALED) {
				out.add((LivingEntity) e);
			}
		}
		return out;
	}

	private static boolean onHand(Cast cast, LivingEntity t) {
		return t.isAlive() && t.level() == cast.level;
	}

	private static DamageSource magic(Cast cast) {
		return cast.level.damageSources().indirectMagic(cast.caster, cast.caster);
	}

	private static DamageSource fire(Cast cast) {
		return cast.level.damageSources().source(DamageTypes.IN_FIRE, cast.caster);
	}

	private static DamageSource frost(Cast cast) {
		return cast.level.damageSources().source(DamageTypes.FREEZE, cast.caster);
	}

	private static void effect(LivingEntity t, Holder<MobEffect> effect, int ticks, int amplifier, Cast cast) {
		t.addEffect(new MobEffectInstance(effect, Math.max(1, ticks), amplifier, false, true), cast.caster);
	}

	private static boolean fits(ServerLevel level, Entity entity, Vec3 feet) {
		return level.noCollision(entity, entity.getDimensions(entity.getPose()).makeBoundingBox(feet));
	}

	private static void teleport(Entity entity, ServerLevel level, Vec3 to) {
		entity.teleportTo(level, to.x, to.y, to.z, Set.<Relative>of(), entity.getYRot(), entity.getXRot(), false);
		entity.resetFallDistance();
		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
		}
	}

	/** Changing blocks: never for monsters, never where the caster couldn't build, and within the cast's block budget. */
	private static boolean mayEdit(Cast cast, BlockPos pos) {
		return Casters.mayBuild(cast.caster) && !Techniques.isRampart(cast.level, pos) && Casters.mayEdit(cast.caster, cast.level, pos)
			&& cast.takeBlock();
	}

	// ------------------------------------------------------------------ vanilla structures

	/**
	 * Echolocate: a sonar pulse that lights up every creature around the point the caster could harm
	 * (never allies or bystanders, so an invisible player only shows when they're a PvP enemy), and
	 * dazes what it hit.
	 */
	private static void echolocate(Cast cast, Cast.Hit hit, List<LivingEntity> harmed, double radius, double duration) {
		Vec3 point = hit.point();
		ExplorerVfx.echolocate(cast.level, point, radius);
		int seen = 0;
		for (Entity e : cast.level.getEntities(cast.caster, new AABB(point, point).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
			if (seen++ >= MAX_REVEALED || e.position().distanceTo(point) > radius) {
				continue;
			}
			LivingEntity t = (LivingEntity) e;
			t.addEffect(new MobEffectInstance(MobEffects.GLOWING, Effects.ticks(10, duration), 0, false, false));
			// The echo comes back from each creature a moment later, by distance.
			int delay = 1 + (int) (e.position().distanceTo(point) / 2);
			Scheduler.later(delay, () -> ExplorerVfx.echo(cast.level, t));
		}
		for (LivingEntity t : harmed) {
			effect(t, MobEffects.SLOWNESS, Effects.ticks(3, duration), 1, cast);
			// Dazed in the dark: shadowed, so life damage sets off Blight.
			Reactions.mark(t, Reactions.Mark.SHADOWED, Effects.ticks(3, duration));
		}
	}

	/** Resonant Shriek: sound that goes straight through armour, darkness, and an echo a second later. */
	private static void resonantShriek(Cast cast, LivingEntity t, double power, double duration) {
		ExplorerVfx.shriek(cast.level, cast.caster, t, false);
		Effects.hurt(cast, t, cast.level.damageSources().sonicBoom(cast.caster), 8 * power);
		effect(t, MobEffects.DARKNESS, Effects.ticks(6, duration), 0, cast);
		Reactions.mark(t, Reactions.Mark.SHADOWED, Effects.ticks(6, duration));
		Scheduler.later(20, Effects.carryContext(() -> {
			if (cast.alive() && onHand(cast, t)) {
				ExplorerVfx.shriek(cast.level, cast.caster, t, true);
				Effects.hurt(cast, t, cast.level.damageSources().sonicBoom(cast.caster), 4 * power);
			}
		}));
	}

	/** Tidecall: a wave crashes in on the point from every side, dragging what's there into the middle, soaked. */
	private static void tidecall(Cast cast, Vec3 point, double radius, double power) {
		Vec3 centre = CastEngine.ground(cast.level, point.add(0, 0.5, 0));
		ExplorerVfx.tidecall(cast.level, centre, radius);
		for (LivingEntity t : enemiesAround(cast, centre.add(0, 1, 0), radius)) {
			Effects.hurt(cast, t, magic(cast), 6 * power);
			Reactions.mark(t, Reactions.Mark.SOAKED);
			if (!Spirits.isBoss(t)) {
				Vec3 in = centre.subtract(t.position());
				Effects.push(t, new Vec3(in.x, 0, in.z).scale(0.25).add(0, 0.25, 0));
			}
		}
	}

	/** Infest: silverfish chew at the target from the stone around it. Infesting it again starts the chewing over. */
	private static void infest(Cast cast, LivingEntity t, double power, double duration) {
		int ticks = Effects.ticks(4, duration);
		effect(t, MobEffects.SLOWNESS, ticks, 0, cast);
		ExplorerVfx.infest(cast.level, t, true);
		Linger key = new Linger(t.getUUID(), cast.caster.getUUID(), "infest");
		Object token = linger(key);
		repeat(cast, ticks, 10, tick -> {
			if (current(key, token) && onHand(cast, t)) {
				ExplorerVfx.infest(cast.level, t, false);
				Effects.hurt(cast, t, magic(cast), 1 * power);
			}
		}, () -> release(key, token));
	}

	/** Sandstorm: a whirl of sand that blinds, slows and scours whatever is in it. */
	private static void sandstorm(Cast cast, Vec3 point, double radius, double power, int ticks) {
		Vec3 centre = CastEngine.ground(cast.level, point.add(0, 0.5, 0));
		ExplorerVfx.sandstormOpen(cast.level, centre, radius, ticks);
		repeat(cast, ticks, 5, tick -> {
			ExplorerVfx.sandstorm(cast.level, centre, radius, tick);
			if (tick % 20 == 0) {
				for (LivingEntity t : enemiesAround(cast, centre.add(0, 1, 0), radius)) {
					Effects.hurt(cast, t, magic(cast), 2 * power);
					effect(t, MobEffects.BLINDNESS, 30, 0, cast);
					effect(t, MobEffects.SLOWNESS, 30, 1, cast);
				}
			}
		}, () -> { });
	}

	/** Vinelash: a thorned vine lashes the target and hauls it toward the caster. */
	private static void vinelash(Cast cast, LivingEntity t, double power) {
		ExplorerVfx.vinelash(cast.level, cast.caster, t);
		Effects.hurt(cast, t, magic(cast), 5 * power);
		if (!Spirits.isBoss(t) && t.isAlive()) {
			Vec3 toward = cast.caster.position().subtract(t.position());
			double distance = toward.horizontalDistance();
			if (distance > 1.5) {
				Vec3 flat = new Vec3(toward.x, 0, toward.z).normalize();
				Effects.push(t, flat.scale(Math.min(1.2, 0.35 + Math.min(4.0, distance - 1.5) * 0.22)).add(0, 0.35, 0));
			}
		}
	}

	/** Remedy: cures allies, and readies a zombie villager for its golden apple. */
	private static void remedy(Cast cast, Cast.Hit hit, List<LivingEntity> helped, double power, double duration) {
		for (LivingEntity t : helped) {
			List<Holder<MobEffect>> bad = new ArrayList<>();
			for (MobEffectInstance effect : t.getActiveEffects()) {
				if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
					bad.add(effect.getEffect());
				}
			}
			bad.forEach(t::removeEffect);
			t.heal((float) (4 * power));
			t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Effects.ticks(6, duration), 0, false, true));
			ExplorerVfx.remedy(cast.level, t);
		}
		for (Entity e : hit.entities()) {
			if (e instanceof ZombieVillager zombie && zombie.isAlive() && !zombie.isConverting()) {
				zombie.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0, false, true));
				ExplorerVfx.remedy(cast.level, zombie);
			}
		}
	}

	/** Warcry: a war horn that stirs the caster and every ally around the target. */
	private static void warcry(Cast cast, Cast.Hit hit, List<LivingEntity> helped, double radius, int ticks) {
		Vec3 centre = hit.self() ? cast.caster.position() : hit.point();
		Set<LivingEntity> rallied = new HashSet<>(helped);
		rallied.addAll(alliesAround(cast, centre, radius));
		ExplorerVfx.warcry(cast.level, centre, radius);
		for (LivingEntity t : rallied) {
			t.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, 0, false, true));
			t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0, false, true));
			ExplorerVfx.rallied(cast.level, t);
		}
	}

	/**
	 * Fangs: a ring of evoker fangs snaps up round the target. The fangs are vanilla's own (they bite
	 * whatever enemy stands on them too); the target's bite goes through {@link Effects#hurt}, so it
	 * meets Shields, Execute and the rest like any spell.
	 */
	private static void fangs(Cast cast, LivingEntity t, double power) {
		ServerLevel level = cast.level;
		Vec3 feet = t.position();
		double spin = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < 5; i++) {
			double a = spin + Math.PI * 2 * i / 5;
			Vec3 spot = CastEngine.ground(level, feet.add(Math.cos(a) * 1.15, 0.6, Math.sin(a) * 1.15));
			// Never where an ally (or a bystander the caster may not harm) stands: the fangs would bite them.
			boolean clear = level.getEntities((Entity) null, new AABB(spot, spot).inflate(0.9, 1.0, 0.9),
				e -> e instanceof LivingEntity && e != t && !Targets.canHarm(cast.caster, e)).isEmpty();
			if (clear && Math.abs(spot.y - feet.y) < 2.5) {
				level.addFreshEntity(new EvokerFangs(level, spot.x, spot.y, spot.z, (float) a, i, cast.caster));
			}
		}
		ExplorerVfx.fangs(level, t);
		Scheduler.later(8, Effects.carryContext(() -> {
			if (cast.alive() && onHand(cast, t)) {
				Effects.hurt(cast, t, magic(cast), 6 * power);
			}
		}));
	}

	/** Undertow: drags the target down; in water, under. */
	private static void undertow(Cast cast, LivingEntity t, double power, double duration) {
		effect(t, MobEffects.SLOWNESS, Effects.ticks(3, duration), 2, cast);
		Reactions.mark(t, Reactions.Mark.SOAKED, Effects.ticks(3, duration) + 40);
		ExplorerVfx.undertow(cast.level, t);
		if (!Spirits.isBoss(t)) {
			Vec3 v = t.getDeltaMovement();
			Effects.push(t, new Vec3(-v.x * 0.5, Math.min(0, -v.y) - (t.isInWater() ? 0.9 : 0.5), -v.z * 0.5));
		}
		if (t.isInWater()) {
			Effects.hurt(cast, t, cast.level.damageSources().source(DamageTypes.DROWN, cast.caster), 5 * power);
		}
	}

	/** Treasure Sense: luck, and a glint over every container and suspicious block nearby. */
	private static void treasureSense(Cast cast, LivingEntity t, int ticks) {
		t.addEffect(new MobEffectInstance(MobEffects.LUCK, ticks, 1, false, true));
		ExplorerVfx.treasureSense(cast.level, t);
		if (!(t instanceof Player)) {
			return;
		}
		ServerLevel level = cast.level;
		BlockPos centre = t.blockPosition();
		int reach = 24;
		List<BlockPos> finds = new ArrayList<>();
		for (int cx = (centre.getX() - reach) >> 4; cx <= (centre.getX() + reach) >> 4; cx++) {
			for (int cz = (centre.getZ() - reach) >> 4; cz <= (centre.getZ() + reach) >> 4; cz++) {
				if (!level.hasChunk(cx, cz)) {
					continue;
				}
				for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
					if ((be instanceof RandomizableContainerBlockEntity || be instanceof BrushableBlockEntity)
							&& be.getBlockPos().distSqr(centre) <= reach * reach && finds.size() < 24) {
						finds.add(be.getBlockPos());
					}
				}
			}
		}
		if (finds.isEmpty()) {
			return;
		}
		repeat(cast, ticks, 40, tick -> {
			if (onHand(cast, t)) {
				finds.forEach(pos -> ExplorerVfx.treasureGlint(level, pos));
			}
		}, () -> { });
	}

	/** Tusk Charge: the caster charges forward like a hoglin, tossing whatever it runs into. A movement rune: it always moves the caster. */
	private static void tuskCharge(Cast cast, double power) {
		LivingEntity runner = cast.caster;
		Vec3 dir = Effects.horizontal(runner.getLookAngle(), runner.getLookAngle());
		Effects.push(runner, dir.scale(2.0).add(0, 0.2, 0));
		runner.resetFallDistance();
		ExplorerVfx.tuskCharge(cast.level, runner, dir, true);
		Set<UUID> tossed = new HashSet<>();
		repeat(cast, 8, 1, tick -> {
			if (!onHand(cast, runner)) {
				return;
			}
			if (tick % 2 == 0) {
				ExplorerVfx.tuskCharge(cast.level, runner, dir, false);
			}
			for (LivingEntity t : enemiesAround(cast, runner.position().add(dir.scale(0.8)).add(0, 1, 0), 1.6)) {
				if (t != runner && tossed.add(t.getUUID())) {
					Effects.hurt(cast, t, magic(cast), 5 * power);
					if (!Spirits.isBoss(t)) {
						Effects.push(t, dir.scale(0.5).add(0, 0.8, 0));
						Reactions.mark(t, Reactions.Mark.WINDSWEPT);
					}
					ExplorerVfx.tossed(cast.level, t);
				}
			}
		}, runner::resetFallDistance);
	}

	/** Blazecall: three blaze fireballs fall on the target, a third of a second apart. */
	private static void blazecall(Cast cast, LivingEntity t, double power) {
		for (int i = 0; i < 3; i++) {
			int shot = i;
			Scheduler.later(1 + i * 7, Effects.carryContext(() -> {
				if (!cast.alive() || !onHand(cast, t)) {
					return;
				}
				ExplorerVfx.blazeFireball(cast.level, t, shot);
				double react = Reactions.fire(cast, t);
				t.igniteForSeconds(3);
				Effects.hurt(cast, t, fire(cast), 2 * power * react);
			}));
		}
	}

	private static final Identifier SHELL_ID = Wildercord.id("shulkershell");

	/** Shulkershell: a shell that turns nearly every blow, holds the target still, and lifts its attackers when it opens. */
	private static void shulkershell(Cast cast, LivingEntity t, int ticks) {
		t.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 3, false, true));
		t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 6, false, false));
		AttributeInstance knockback = t.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (knockback != null) {
			knockback.addOrUpdateTransientModifier(new AttributeModifier(SHELL_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
		}
		t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
		ExplorerVfx.shellClose(cast.level, t);
		// One shell at a time: a newer one (anyone's) takes over, and only the last to close lets go of the modifier.
		Linger key = new Linger(t.getUUID(), null, "shulkershell");
		Object token = linger(key);
		repeat(cast, ticks, 10, tick -> {
			if (current(key, token) && onHand(cast, t) && tick > 0) {
				ExplorerVfx.shellHold(cast.level, t, tick);
			}
		}, () -> {
			if (!current(key, token)) {
				return;
			}
			release(key, token);
			if (knockback != null) {
				knockback.removeModifier(SHELL_ID);
			}
			if (cast.alive() && onHand(cast, t)) {
				ExplorerVfx.shellOpen(cast.level, t);
				for (LivingEntity near : enemiesAround(cast, t.getBoundingBox().getCenter(), 3.0)) {
					near.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0, false, true), cast.caster);
					Reactions.mark(near, Reactions.Mark.WINDSWEPT, 60);
				}
			}
		});
	}

	/** Portalfall: the target drops through a portal under it and out of one high above. */
	private static void portalfall(Cast cast, LivingEntity t, double power) {
		ServerLevel level = cast.level;
		Vec3 from = t.position();
		if (Spirits.isBoss(t)) {
			ExplorerVfx.portalfall(level, from, null);
			Effects.hurt(cast, t, magic(cast), 2 * power);
			return;
		}
		for (int up = 7; up >= 3; up--) {
			Vec3 spot = from.add(0, up, 0);
			if (fits(level, t, spot) && level.clip(new ClipContext(from.add(0, t.getBbHeight(), 0), spot.add(0, t.getBbHeight(), 0),
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, t)).getType() == HitResult.Type.MISS) {
				ExplorerVfx.portalfall(level, from, spot);
				Effects.hurt(cast, t, magic(cast), 2 * power);
				t.teleportTo(level, spot.x, spot.y, spot.z, Set.<Relative>of(), t.getYRot(), t.getXRot(), false);
				t.setDeltaMovement(0, -0.2, 0);
				t.needsSync = true;
				if (t instanceof Mob mob) {
					mob.getNavigation().stop();
				}
				return;
			}
		}
		ExplorerVfx.portalfall(level, from, null);
		Effects.hurt(cast, t, magic(cast), 2 * power);
	}

	/** Ancient Seed: a flower from before the world was young, and a stage of growth for the crops around it. */
	private static void ancientSeed(Cast cast, Cast.Hit hit, double radiusScale) {
		ServerLevel level = cast.level;
		BlockPos ground = hit.block() != null ? hit.block() : BlockPos.containing(CastEngine.ground(level, hit.point().add(0, 0.5, 0)).subtract(0, 0.5, 0));
		BlockPos above = ground.above();
		ExplorerVfx.ancientSeed(level, Vec3.atBottomCenterOf(above));
		if (level.getBlockState(above).isAir()) {
			boolean pitcher = level.getRandom().nextFloat() < 0.35F;
			BlockState flower = pitcher ? Blocks.PITCHER_PLANT.defaultBlockState() : Blocks.TORCHFLOWER.defaultBlockState();
			if (flower.canSurvive(level, above) && (!pitcher || level.getBlockState(above.above()).isAir()) && mayEdit(cast, above)) {
				if (pitcher) {
					DoublePlantBlock.placeAt(level, flower, above, 3);
				} else {
					level.setBlock(above, flower, 3);
				}
			}
		}
		int reach = (int) Math.round(4 * radiusScale);
		int grown = 0;
		for (BlockPos pos : BlockPos.betweenClosed(ground.offset(-reach, -1, -reach), ground.offset(reach, 2, reach))) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state) && grown < 32 && mayEdit(cast, pos)) {
				level.setBlock(pos, crop.getStateForAge(crop.getAge(state) + 1), 2);
				ExplorerVfx.sprout(level, pos.immutable());
				grown++;
			}
		}
	}

	// ------------------------------------------------------------------ attuned in the biomes

	/** Moonpetal: moonlit petals cut enemies and mend allies alike. */
	private static void moonpetal(Cast cast, Vec3 point, double radius, double power) {
		ExplorerVfx.moonpetal(cast.level, point, radius);
		for (LivingEntity t : enemiesAround(cast, point, radius)) {
			Effects.hurt(cast, t, magic(cast), 4 * power);
		}
		for (LivingEntity t : alliesAround(cast, point, radius)) {
			t.heal((float) (3 * power));
			ExplorerVfx.petalMend(cast.level, t);
		}
	}

	/** Hoarfrost: rime that slows its target more each second, then freezes it solid. */
	private static void hoarfrost(Cast cast, LivingEntity t, double power, double duration) {
		for (int i = 0; i < 3; i++) {
			int stage = i;
			Scheduler.later(1 + i * 20, () -> {
				if (cast.alive() && onHand(cast, t)) {
					effect(t, MobEffects.SLOWNESS, 25, stage, cast);
					t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() - 1, t.getTicksFrozen() + 50));
					ExplorerVfx.hoarfrost(cast.level, t, stage);
				}
			});
		}
		Scheduler.later(60, Effects.carryContext(() -> {
			if (cast.alive() && onHand(cast, t)) {
				Spirits.freeze(t, Effects.ticks(2, duration));
				Effects.hurt(cast, t, frost(cast), 6 * power);
				ExplorerVfx.hoarfrost(cast.level, t, 3);
			}
		}));
	}

	/** Hush: a pocket of silence where monsters forget what they were after. */
	private static void hush(Cast cast, Vec3 point, double radius, int ticks) {
		Vec3 centre = CastEngine.ground(cast.level, point.add(0, 0.5, 0));
		ExplorerVfx.hushOpen(cast.level, centre, radius, ticks);
		repeat(cast, ticks, 10, tick -> {
			if (tick % 20 == 0) {
				ExplorerVfx.hush(cast.level, centre, radius);
			}
			for (LivingEntity t : enemiesAround(cast, centre.add(0, 1, 0), radius)) {
				if (t instanceof Mob mob && !Spirits.isBoss(mob)) {
					mob.setTarget(null);
				}
				effect(t, MobEffects.WEAKNESS, 15, 0, cast);
				effect(t, MobEffects.DARKNESS, 30, 0, cast);
				effect(t, MobEffects.BLINDNESS, 25, 0, cast);
				Reactions.mark(t, Reactions.Mark.SHADOWED, 30);
			}
		}, () -> { });
	}

	/** Sporebloom: spores that sicken enemies and feed allies. */
	private static void sporebloom(Cast cast, Vec3 point, double radius, double duration) {
		Vec3 centre = CastEngine.ground(cast.level, point.add(0, 0.5, 0));
		ExplorerVfx.sporebloom(cast.level, centre, radius);
		for (LivingEntity t : enemiesAround(cast, centre.add(0, 1, 0), radius)) {
			effect(t, MobEffects.POISON, Effects.ticks(6, duration), 0, cast);
			effect(t, MobEffects.NAUSEA, Effects.ticks(6, duration), 0, cast);
		}
		for (LivingEntity t : alliesAround(cast, centre.add(0, 1, 0), radius)) {
			if (t instanceof Player player) {
				player.getFoodData().eat(4, 0.4F);
			}
		}
	}

	/** Sunscorch: the noon sun, focused; fiercer under an open sky by day. */
	private static void sunscorch(Cast cast, LivingEntity t, double power, double duration) {
		boolean sunlit = cast.level.isBrightOutside() && cast.level.canSeeSky(t.blockPosition().above());
		ExplorerVfx.sunscorch(cast.level, t, sunlit);
		double react = Reactions.fire(cast, t);
		t.igniteForSeconds((float) (5 * duration));
		Effects.hurt(cast, t, fire(cast), 8 * power * react * (sunlit ? 1.5 : 1.0));
	}

	private static final Identifier MIRE_ID = Wildercord.id("mire");

	/** Mire: the ground turns to mud under the target. */
	private static void mire(Cast cast, LivingEntity t, int ticks) {
		effect(t, MobEffects.SLOWNESS, ticks, 3, cast);
		Reactions.mark(t, Reactions.Mark.SOAKED, ticks + 20);
		AttributeInstance jump = t.getAttribute(Attributes.JUMP_STRENGTH);
		if (jump != null) {
			jump.addOrUpdateTransientModifier(new AttributeModifier(MIRE_ID, -0.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		// One mire at a time: a newer one (anyone's) takes over, and only the last to dry lets go of the modifier.
		Linger key = new Linger(t.getUUID(), null, "mire");
		Object token = linger(key);
		repeat(cast, ticks, 10, tick -> {
			if (current(key, token) && onHand(cast, t)) {
				ExplorerVfx.mire(cast.level, t, tick == 0);
			}
		}, () -> {
			if (!current(key, token)) {
				return;
			}
			release(key, token);
			if (jump != null) {
				jump.removeModifier(MIRE_ID);
			}
		});
	}

	/** Glowvine: cave vines heavy with glow berries grow down from the ceiling round the point. */
	private static void glowvine(Cast cast, Cast.Hit hit, double radiusScale) {
		ServerLevel level = cast.level;
		Vec3 point = hit.point();
		int spread = Math.max(1, (int) Math.round(2 * radiusScale));
		int placed = 0;
		List<BlockPos> columns = new ArrayList<>();
		BlockPos base = BlockPos.containing(point);
		columns.add(base);
		for (int i = 0; i < 6; i++) {
			columns.add(base.offset(level.getRandom().nextIntBetweenInclusive(-spread, spread), 0, level.getRandom().nextIntBetweenInclusive(-spread, spread)));
		}
		for (BlockPos column : columns) {
			if (placed >= 5) {
				break;
			}
			// Find the ceiling above this spot, within 8 blocks.
			BlockPos ceiling = null;
			for (int up = 0; up <= 8; up++) {
				BlockPos at = column.above(up);
				BlockState state = level.getBlockState(at);
				if (!state.isAir()) {
					ceiling = state.isFaceSturdy(level, at, Direction.DOWN) && up > 1 ? at : null;
					break;
				}
			}
			if (ceiling == null) {
				continue;
			}
			int length = 1 + level.getRandom().nextInt(3);
			BlockPos top = ceiling.below();
			for (int i = 0; i < length; i++) {
				BlockPos at = top.below(i);
				if (!level.getBlockState(at).isAir() || !level.getBlockState(at.below()).isAir()) {
					break;
				}
				boolean last = i == length - 1;
				BlockState vine = (last ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState().setValue(CaveVines.BERRIES, true);
				if (!mayEdit(cast, at)) {
					break;
				}
				level.setBlock(at, vine, 3);
				if (!last && !level.getBlockState(at.below()).isAir()) {
					break;
				}
			}
			// A plant piece left without a head below it becomes the head, so the vine stays whole.
			for (int i = length - 1; i >= 0; i--) {
				BlockPos at = top.below(i);
				BlockState state = level.getBlockState(at);
				if (state.is(Blocks.CAVE_VINES_PLANT) && !level.getBlockState(at.below()).is(Blocks.CAVE_VINES) && !level.getBlockState(at.below()).is(Blocks.CAVE_VINES_PLANT)) {
					level.setBlock(at, Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVines.BERRIES, true), 3);
				}
			}
			ExplorerVfx.glowvine(level, Vec3.atCenterOf(top));
			placed++;
		}
		if (placed == 0) {
			ExplorerVfx.glowvineFizzle(level, point);
		}
	}

	/** Rootsnare: mangrove roots burst up and hold everything around the point. */
	private static void rootsnare(Cast cast, Vec3 point, double radius, double power, int ticks) {
		Vec3 centre = CastEngine.ground(cast.level, point.add(0, 0.5, 0));
		ExplorerVfx.rootsnare(cast.level, centre, radius);
		for (LivingEntity t : enemiesAround(cast, centre.add(0, 1, 0), radius)) {
			Spirits.hold(t, ticks);
			t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
			Effects.hurt(cast, t, magic(cast), 3 * power);
			ExplorerVfx.rooted(cast.level, t);
		}
	}

	/** Stalactite: a spike of dripstone drops on the target; worse on a bare head. */
	private static void stalactite(Cast cast, LivingEntity t, double power) {
		ExplorerVfx.stalactiteWarn(cast.level, t);
		Scheduler.later(8, Effects.carryContext(() -> {
			if (!cast.alive() || !onHand(cast, t)) {
				return;
			}
			ExplorerVfx.stalactite(cast.level, t);
			boolean bare = t.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
			Effects.hurt(cast, t, cast.level.damageSources().source(DamageTypes.FALLING_STALACTITE, cast.caster), 7 * power * (bare ? 1.5 : 1.0));
		}));
	}

	/** Summit Wind: a mountain gale that hurls enemies up and away, or carries the caster aloft. */
	private static void summitWind(Cast cast, Cast.Hit hit, double radius, double power) {
		LivingEntity caster = cast.caster;
		if (hit.self()) {
			Vec3 v = caster.getDeltaMovement();
			Effects.push(caster, new Vec3(0, 1.55 - Math.max(0, v.y), 0).add(Effects.horizontal(caster.getLookAngle(), caster.getLookAngle()).scale(0.3)));
			caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, true));
			caster.resetFallDistance();
			ExplorerVfx.summitWind(cast.level, caster.position(), 1.5, true);
			return;
		}
		Vec3 centre = hit.point();
		ExplorerVfx.summitWind(cast.level, centre, radius, false);
		for (LivingEntity t : enemiesAround(cast, centre, radius)) {
			Effects.hurt(cast, t, cast.level.damageSources().source(DamageTypes.WIND_CHARGE, caster), 5 * power);
			if (!Spirits.isBoss(t)) {
				Vec3 away = Effects.horizontal(t.position().subtract(centre), hit.dir());
				Effects.push(t, away.scale(0.9 * Math.sqrt(power)).add(0, 1.1, 0));
				Reactions.mark(t, Reactions.Mark.WINDSWEPT);
			}
		}
	}

	/**
	 * Soulfire: blue flames that burn on through water, and feed their caster a little of the damage
	 * they really deal back as mana (see {@link ExplorerNumbers#soulfireRefund}; a blocked burn, or one
	 * the target shrugs off, gives nothing). Lighting it again refreshes the burn rather than adding one.
	 */
	private static void soulfire(Cast cast, LivingEntity t, double power, int ticks) {
		ExplorerVfx.soulfire(cast.level, t, true);
		Linger key = new Linger(t.getUUID(), cast.caster.getUUID(), "soulfire");
		Object token = linger(key);
		repeat(cast, ticks, 20, tick -> {
			if (!current(key, token) || !onHand(cast, t)) {
				return;
			}
			ExplorerVfx.soulfire(cast.level, t, false);
			double react = tick == 0 ? Reactions.fire(cast, t) : 1.0;
			float before = t.getHealth() + t.getAbsorptionAmount();
			Effects.hurt(cast, t, fire(cast), 3 * power * react);
			float dealt = Math.max(0.0F, before - (t.getHealth() + t.getAbsorptionAmount()));
			if (cast.caster instanceof ServerPlayer player) {
				double[] refunded = REFUNDED.computeIfAbsent(cast.identity(), k -> new double[1]);
				double back = ExplorerNumbers.soulfireRefund(dealt, refunded[0]);
				if (back > 0) {
					refunded[0] += back;
					Mana.restore(player, (float) back);
				}
			}
		}, () -> release(key, token));
	}

	/** Warp Step: a step to the point, and back again a moment later unless the caster is sneaking. */
	private static void warpStep(Cast cast, Cast.Hit hit) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		Vec3 from = caster.position();
		Vec3 target = hit.entities().isEmpty() ? hit.point() : hit.entities().getFirst().position();
		if (hit.self() || target.distanceTo(from) < 1.5) {
			return;
		}
		if (target.distanceTo(from) > 24) {
			target = from.add(target.subtract(from).normalize().scale(24));
		}
		Vec3 spot = null;
		Vec3 back = target.subtract(from).normalize();
		for (double d = 0; d < 6; d += 0.5) {
			Vec3 at = CastEngine.ground(level, target.subtract(back.scale(d)).add(0, 1.0, 0));
			if (fits(level, caster, at)) {
				spot = at;
				break;
			}
		}
		if (spot == null) {
			ExplorerVfx.warpFizzle(level, target);
			return;
		}
		ExplorerVfx.warpStep(level, from, spot);
		teleport(caster, level, spot);
		Vec3 home = from;
		Scheduler.later(60, () -> {
			if (!cast.alive() || caster.isShiftKeyDown() || !fits(level, caster, home)) {
				if (cast.alive()) {
					ExplorerVfx.warpStay(level, caster);
				}
				return;
			}
			ExplorerVfx.warpStep(level, caster.position(), home);
			teleport(caster, level, home);
		});
	}

	/** Blood Moss: moss that drinks the target and gives the caster what it takes. Spreading it again starts it over. */
	private static void bloodMoss(Cast cast, LivingEntity t, double power, int ticks) {
		ExplorerVfx.bloodMoss(cast.level, t, cast.caster, true);
		Linger key = new Linger(t.getUUID(), cast.caster.getUUID(), "blood_moss");
		Object token = linger(key);
		repeat(cast, ticks, 20, tick -> {
			if (!current(key, token) || !onHand(cast, t)) {
				return;
			}
			float before = t.getHealth();
			Effects.hurt(cast, t, magic(cast), 1 * power);
			float taken = Math.max(0.0F, before - t.getHealth());
			if (taken > 0 && cast.caster.isAlive()) {
				cast.caster.heal(taken);
			}
			ExplorerVfx.bloodMoss(cast.level, t, cast.caster, false);
		}, () -> release(key, token));
	}

	/** Basalt Surge: columns of basalt burst up in a line from the caster to the point. */
	private static void basaltSurge(Cast cast, Cast.Hit hit, double width, double power) {
		ServerLevel level = cast.level;
		Vec3 from = cast.caster.position();
		Vec3 to = hit.self() ? from.add(Effects.horizontal(cast.caster.getLookAngle(), cast.caster.getLookAngle()).scale(10)) : hit.point();
		Vec3 flat = new Vec3(to.x - from.x, 0, to.z - from.z);
		double length = Math.min(16.0, Math.max(3.0, flat.length()));
		Vec3 dir = flat.lengthSqr() < 1.0E-4 ? Effects.horizontal(cast.caster.getLookAngle(), cast.caster.getLookAngle()) : flat.normalize();
		Set<UUID> struck = new HashSet<>();
		for (double d = 1.5; d <= length; d += 1.25) {
			double step = d;
			Scheduler.later(1 + (int) (d * 0.8), Effects.carryContext(() -> {
				if (!cast.alive()) {
					return;
				}
				Vec3 at = CastEngine.ground(level, from.add(dir.scale(step)).add(0, 1.5, 0));
				ExplorerVfx.basaltColumn(level, at, width);
				for (LivingEntity t : enemiesAround(cast, at.add(0, 1, 0), width)) {
					if (struck.add(t.getUUID())) {
						Effects.hurt(cast, t, magic(cast), 7 * power);
						if (!Spirits.isBoss(t)) {
							Effects.push(t, new Vec3(0, 0.95, 0).add(dir.scale(0.2)));
							Reactions.mark(t, Reactions.Mark.WINDSWEPT);
						}
					}
				}
			}));
		}
	}

	/** Starlight Tether: a thread of starlight that drags the target back whenever it strays. */
	private static void starlightTether(Cast cast, LivingEntity t, Vec3 point, double power, int ticks) {
		Vec3 anchor = CastEngine.ground(cast.level, point.add(0, 0.5, 0));
		ExplorerVfx.tether(cast.level, anchor, t, true);
		if (Spirits.isBoss(t)) {
			effect(t, MobEffects.SLOWNESS, ticks, 1, cast);
			return;
		}
		long[] lastYank = {-100};
		repeat(cast, ticks, 2, tick -> {
			if (!onHand(cast, t)) {
				return;
			}
			double d = t.position().distanceTo(anchor);
			if (d > 3.0) {
				Vec3 back = anchor.subtract(t.position()).normalize().scale(Math.min(1.3, 0.4 + (d - 3.0) * 0.35));
				t.setDeltaMovement(back.x, Math.max(t.getDeltaMovement().y, back.y), back.z);
				t.needsSync = true;
				if (t instanceof ServerPlayer player) {
					player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
				}
				if (tick - lastYank[0] >= 10) {
					lastYank[0] = tick;
					Effects.hurt(cast, t, magic(cast), 2 * power);
					ExplorerVfx.tether(cast.level, anchor, t, true);
				}
			}
			if (tick % 4 == 0) {
				ExplorerVfx.tether(cast.level, anchor, t, false);
			}
		}, () -> { });
	}

	// ------------------------------------------------------------------ dungeons, bosses and events

	/** Cinderbrand: a brand that makes the caster's fire burn the target hotter. */
	private static void cinderbrand(Cast cast, LivingEntity t, double power, int ticks) {
		ExplorerVfx.cinderbrand(cast.level, t);
		Effects.hurt(cast, t, fire(cast), 3 * power * Reactions.fire(cast, t));
		mark(BRANDED, cast, t, ticks);
	}

	/** Ashen Veil: ash that keeps fire off the target and sets whoever strikes it alight. */
	private static void ashenVeil(Cast cast, LivingEntity t, int ticks) {
		t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0, false, true));
		t.clearFire();
		ExplorerVfx.ashenVeil(cast.level, t, true);
		int[] seen = {t.getLastHurtByMobTimestamp()};
		repeat(cast, ticks, 2, tick -> {
			if (!onHand(cast, t)) {
				return;
			}
			if (tick % 20 == 0) {
				ExplorerVfx.ashenVeil(cast.level, t, false);
			}
			int stamp = t.getLastHurtByMobTimestamp();
			if (stamp == seen[0]) {
				return;
			}
			seen[0] = stamp;
			LivingEntity attacker = t.getLastHurtByMob();
			if (attacker != null && attacker != t && attacker.isAlive() && attacker.distanceTo(t) <= 4.5 && Targets.canHarm(cast.caster, attacker)) {
				attacker.igniteForSeconds(4);
				ExplorerVfx.ashIgnite(cast.level, t, attacker);
			}
		}, () -> { });
	}

	/** Cinderheart: strength, a body fire can't touch, and a furnace of heat around it. */
	private static void cinderheart(Cast cast, LivingEntity t, double power, int ticks) {
		t.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, 1, false, true));
		t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0, false, true));
		t.clearFire();
		ExplorerVfx.cinderheart(cast.level, t, true);
		repeat(cast, ticks, 20, tick -> {
			if (!onHand(cast, t)) {
				return;
			}
			ExplorerVfx.cinderheart(cast.level, t, false);
			for (LivingEntity near : enemiesAround(cast, t.getBoundingBox().getCenter(), 4.0)) {
				// The heat doesn't go through walls.
				if (near != t && t.hasLineOfSight(near)) {
					Effects.hurt(cast, near, fire(cast), 3 * power);
					near.igniteForSeconds(2);
				}
			}
		}, () -> { });
	}

	/** Eclipse: a disc of darkness that blinds and burns what's under it, and leaves it open to the caster's spells. */
	private static void eclipse(Cast cast, Vec3 point, double radius, double power, int ticks) {
		Vec3 centre = CastEngine.ground(cast.level, point.add(0, 0.5, 0));
		ExplorerVfx.eclipseOpen(cast.level, centre, radius, ticks);
		repeat(cast, ticks, 20, tick -> {
			ExplorerVfx.eclipse(cast.level, centre, radius);
			for (LivingEntity t : enemiesAround(cast, centre.add(0, 1, 0), radius)) {
				mark(ECLIPSED, cast, t, 25);
				effect(t, MobEffects.BLINDNESS, 30, 0, cast);
				Reactions.mark(t, Reactions.Mark.SHADOWED, 30);
				Effects.hurt(cast, t, magic(cast), 2 * power);
			}
		}, () -> { });
	}

	/** Starmaw: devours the target's light, and bites harder for every good effect it swallows. */
	private static void starmaw(Cast cast, LivingEntity t, double power) {
		List<Holder<MobEffect>> good = new ArrayList<>();
		for (MobEffectInstance effect : t.getActiveEffects()) {
			if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
				good.add(effect.getEffect());
			}
		}
		good.forEach(t::removeEffect);
		ExplorerVfx.starmaw(cast.level, t, good.size());
		Effects.hurt(cast, t, magic(cast), (14 + 3 * good.size()) * power);
	}

	/** Drowning Word: water fills the target's lungs wherever it stands. Speaking it again starts it over. */
	private static void drowningWord(Cast cast, LivingEntity t, double power, int ticks) {
		Reactions.mark(t, Reactions.Mark.SOAKED, ticks + 40);
		ExplorerVfx.drowningWord(cast.level, t, true);
		Linger key = new Linger(t.getUUID(), cast.caster.getUUID(), "drowning_word");
		Object token = linger(key);
		repeat(cast, ticks, 10, tick -> {
			if (!current(key, token) || !onHand(cast, t)) {
				return;
			}
			t.setAirSupply(Math.min(t.getAirSupply(), 0));
			ExplorerVfx.drowningWord(cast.level, t, false);
			if (tick % 20 == 0) {
				Effects.hurt(cast, t, cast.level.damageSources().source(DamageTypes.DROWN, cast.caster), 2 * power);
			}
		}, () -> release(key, token));
	}

	/** Tidewrit: a wall of water rolls out from the caster through the point, sweeping everything on. */
	private static void tidewrit(Cast cast, Cast.Hit hit, double width, double power) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		Vec3 from = caster.position();
		Vec3 flat = hit.self() ? Effects.horizontal(caster.getLookAngle(), caster.getLookAngle()) : new Vec3(hit.point().x - from.x, 0, hit.point().z - from.z);
		Vec3 dir = flat.lengthSqr() < 1.0E-4 ? Effects.horizontal(caster.getLookAngle(), caster.getLookAngle()) : flat.normalize();
		Vec3 side = new Vec3(-dir.z, 0, dir.x);
		double length = Math.max(10.0, Math.min(20.0, flat.length() + 4.0));
		Set<UUID> swept = new HashSet<>();
		int steps = (int) Math.ceil(length / 1.4);
		for (int i = 1; i <= steps; i++) {
			double reach = i * 1.4;
			Scheduler.later(i, Effects.carryContext(() -> {
				if (!cast.alive()) {
					return;
				}
				Vec3 front = from.add(dir.scale(reach));
				ExplorerVfx.tidewrit(level, front, side, width);
				for (Entity e : level.getEntities(caster, new AABB(front, front).inflate(width / 2 + 1, 3, width / 2 + 1), e -> Targets.canHarm(caster, e))) {
					Vec3 rel = e.position().subtract(from);
					double along = rel.dot(dir);
					if (Math.abs(along - reach) > 1.2 || Math.abs(rel.dot(side)) > width / 2 || !swept.add(e.getUUID())) {
						continue;
					}
					LivingEntity t = (LivingEntity) e;
					Effects.hurt(cast, t, magic(cast), 10 * power);
					Reactions.mark(t, Reactions.Mark.SOAKED);
					if (!Spirits.isBoss(t)) {
						Effects.push(t, dir.scale(1.6).add(0, 0.35, 0));
					}
				}
			}));
		}
	}

	/** Starshard: a shard of the fallen star that splinters into sparks. */
	private static void starshard(Cast cast, LivingEntity t, double power) {
		ExplorerVfx.starshard(cast.level, t);
		Effects.hurt(cast, t, magic(cast), 9 * power);
		List<LivingEntity> near = enemiesAround(cast, t.getBoundingBox().getCenter(), 8.0);
		near.remove(t);
		// Sparks only leap to what the shard can see.
		near.removeIf(other -> !t.hasLineOfSight(other));
		near.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(t)));
		Vec3 from = t.getBoundingBox().getCenter();
		for (int i = 0; i < Math.min(3, near.size()); i++) {
			LivingEntity other = near.get(i);
			Scheduler.later(4 + i * 2, Effects.carryContext(() -> {
				if (cast.alive() && onHand(cast, other)) {
					ExplorerVfx.starSpark(cast.level, from, other);
					Effects.hurt(cast, other, magic(cast), 3 * power);
				}
			}));
		}
	}

	/** Riftcall: a rift that drags enemies in, gnaws at them, then snaps shut. */
	private static void riftcall(Cast cast, Vec3 point, double radius, double power, int ticks) {
		Vec3 centre = CastEngine.ground(cast.level, point.add(0, 0.5, 0)).add(0, 1.0, 0);
		ExplorerVfx.riftOpen(cast.level, centre, radius, ticks);
		repeat(cast, ticks, 5, tick -> {
			ExplorerVfx.rift(cast.level, centre, radius, tick);
			for (LivingEntity t : enemiesAround(cast, centre, radius)) {
				Reactions.mark(t, Reactions.Mark.PULLED);
				if (!Spirits.isBoss(t)) {
					Vec3 in = centre.subtract(t.getBoundingBox().getCenter());
					double d = in.length();
					if (d > 0.8) {
						Effects.push(t, in.normalize().scale(Math.min(0.5, 0.12 + d * 0.06)));
					}
				}
				if (tick % 20 == 0) {
					Effects.hurt(cast, t, magic(cast), 2 * power);
				}
			}
		}, Effects.carryContext(() -> {
			if (!cast.alive()) {
				return;
			}
			ExplorerVfx.riftClose(cast.level, centre, radius);
			for (LivingEntity t : enemiesAround(cast, centre, radius * 0.5)) {
				Effects.hurt(cast, t, magic(cast), 6 * power);
			}
		}));
	}

	/**
	 * Manaburn: arcane fire that burns hotter in anything that carries magic. What it takes from a
	 * player's mana scales with the hit's power (so with the shape's strength) and PvP's scale, and one
	 * cast never takes more than {@link ExplorerNumbers#MANABURN_DRAIN_MAX} from anyone.
	 */
	private static void manaburn(Cast cast, LivingEntity t, double power) {
		boolean caster = false;
		if (t instanceof ServerPlayer player && Spellbooks.tier(player) != null) {
			double scale = cast.caster instanceof Player ? dev.wildercord.config.Config.get().pvpDamageScale() : 1.0;
			double[] taken = BURNED.computeIfAbsent(cast.identity(), k -> new HashMap<>()).computeIfAbsent(player.getUUID(), k -> new double[1]);
			double drain = ExplorerNumbers.manaburnDrain(power, scale, taken[0]);
			if (drain > 0) {
				taken[0] += drain;
				Spellbooks.setMana(player, (float) Math.max(0, Spellbooks.mana(player) - drain));
			}
			caster = true;
		} else if (t instanceof Mob mob && !Runebound.spellOf(mob).isEmpty()) {
			caster = true;
		}
		ExplorerVfx.manaburn(cast.level, t, caster);
		Effects.hurt(cast, t, magic(cast), (caster ? 9 : 5) * power);
	}

	/** When each caster last drank from a Manatide (game time), so it's once a minute. */
	private static final Map<UUID, Long> DRANK = new HashMap<>();
	private static final int MANATIDE_WAIT = 1200;

	/** Manatide: mana flows back into the target for a while. Only players have mana to fill. */
	private static void manatide(Cast cast, LivingEntity t, int ticks) {
		if (!(t instanceof ServerPlayer player) || Spellbooks.tier(player) == null) {
			return;
		}
		long now = cast.level.getGameTime();
		Long last = DRANK.get(player.getUUID());
		if (last != null && now - last < MANATIDE_WAIT && now >= last) {
			ExplorerVfx.manatideSpent(cast.level, player);
			Casters.tell(player, net.minecraft.network.chat.Component.translatable("message.wildercord.manatide_wait",
				(MANATIDE_WAIT - (now - last) + 19) / 20));
			return;
		}
		DRANK.put(player.getUUID(), now);
		ExplorerVfx.manatide(cast.level, player, true);
		repeat(cast, ticks, 20, tick -> {
			if (onHand(cast, player)) {
				Mana.restore(player, 3);
				ExplorerVfx.manatide(cast.level, player, false);
			}
		}, () -> { });
	}
}
