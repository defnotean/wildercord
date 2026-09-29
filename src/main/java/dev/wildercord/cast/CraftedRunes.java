package dev.wildercord.cast;

import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.Trait;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the second batch of new runes does: its ten effects (Spellbrand, Gash, Prospect, Searing Edge,
 * Flash Freeze, Drowse, Galvanize, Prolong, Umbra, Disarm) and the hooks for its modifiers (Kindred shares
 * an effect, Belated lands it late; Thirst is read in {@code Effects.hurt}). {@link Effects} hands the
 * effects here by name. Numbers match the rune descriptions in {@link Runes}; visuals are in {@link CraftedVfx}.
 *
 * <p>What lasts is kept here per creature (brands, gashes, sleeps, seared weapons, snatched weapons, sparks
 * of power), never saved: a restart simply ends it, and anything that took something from the world (a
 * weapon, a block of air) gives it back first.</p>
 */
public final class CraftedRunes {
	private CraftedRunes() {}

	/** Spellbrand: the burst when your magic next hurts a branded creature. */
	private static final double BRAND_BURST = 6.0;
	/** Gash: the cut itself. */
	private static final double GASH_DAMAGE = 3.0;
	/** Prospect: the farthest it listens for ore, however widened, and the most ores it shows. */
	private static final double PROSPECT_MAX_RADIUS = 16.0;
	private static final int PROSPECT_MAX_ORES = 48;
	/** Searing Edge: what each seared blow adds, and how long it sets the foe alight. */
	private static final double SEAR_DAMAGE = 2.0;
	private static final int SEAR_BURN_SECONDS = 4;
	/** Flash Freeze: its cut of cold, and how hard it slows a dry target. */
	private static final double FLASH_FREEZE_DAMAGE = 4.0;
	/** Drowse on a boss: only drowsy (Slowness II for as long as the sleep). */
	private static final int DROWSY_AMPLIFIER = 1;
	/** Prolong: the longest any good effect may be made to last, in ticks (5 minutes). */
	private static final int PROLONG_CAP = 6000;
	/** Umbra: its bite, and the light level at or under which the dark bites twice as hard. */
	private static final double UMBRA_DAMAGE = 4.0;
	private static final int UMBRA_DIM = 7;

	/** Registers what these runes listen for (blows landed, creatures leaving, the server stopping); called once at startup. */
	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (!blocked && damage > 0) {
				wake(entity);
			}
			sear(entity, source, blocked);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> forget(entity.getUUID()));
		// A snatched weapon goes back before its creature is saved with its chunk.
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && DISARMED.containsKey(mob.getUUID())) {
				rearm(mob);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Taken taken : List.copyOf(DISARMED.values())) {
				rearm(taken.mob());
			}
			for (GlobalPos spark : List.copyOf(SPARKS)) {
				ServerLevel level = server.getLevel(spark.dimension());
				if (level != null) {
					discharge(level, spark.pos());
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
	}

	private static void forget(UUID id) {
		BRANDS.remove(id);
		GASHED.remove(id);
		ASLEEP.remove(id);
		EDGES.remove(id);
		DISARMED.remove(id);
	}

	static void clear() {
		BRANDS.clear();
		GASHED.clear();
		ASLEEP.clear();
		EDGES.clear();
		DISARMED.clear();
		SPARKS.clear();
		PROSPECTED.clear();
		sharing = false;
	}

	static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed, double power,
			double duration) {
		RuneDef rune = node.effect;
		switch (Effects.builtIn(rune) ? rune.path() : "") {
			case "spellbrand" -> harmed.forEach(t -> spellbrand(cast, t, power, Effects.ticks(8, duration)));
			case "gash" -> harmed.forEach(t -> gash(cast, t, power, Effects.ticks(8, duration)));
			case "prospect" -> prospect(cast, hit, 12.0 * SpellNumbers.effectRadius(node), Effects.ticks(20, duration));
			case "searing_edge" -> helped.forEach(t -> searingEdge(cast, t, power, Effects.ticks(15, duration)));
			case "flash_freeze" -> harmed.forEach(t -> flashFreeze(cast, t, power, duration));
			case "drowse" -> harmed.forEach(t -> drowse(cast, t, Effects.ticks(t instanceof Player ? 2 : 6, duration)));
			case "galvanize" -> galvanize(cast, hit, Effects.ticks(5, duration));
			case "prolong" -> helped.forEach(t -> prolong(cast, t, Effects.ticks(15, duration)));
			case "umbra" -> harmed.forEach(t -> umbra(cast, t, power));
			case "disarm" -> harmed.forEach(t -> disarm(cast, t, hit, Effects.ticks(5, duration)));
			default -> { }
		}
	}

	private static DamageSource magic(Cast cast) {
		return cast.level.damageSources().indirectMagic(cast.caster, cast.caster);
	}

	// ------------------------------------------------------------------ Kindred and Belated

	/** Set while Kindred's share is being applied, so a share is never shared again. */
	private static boolean sharing;

	/**
	 * Kindred: a helpful effect that has just landed lands again, at half power, on its caster (if it missed
	 * them) and on the nearest ally within reach of where it landed that it missed. Both count against the
	 * cast's creature budget.
	 */
	static void share(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, double groupPower) {
		if (sharing || node.count(Runes.KINDRED) == 0 || node.effect.kind() != EffectKind.HELPFUL || !node.effect.has(Trait.SHARE)) {
			return;
		}
		LivingEntity caster = cast.caster;
		Vec3 at = hit.point();
		List<Entity> extra = new ArrayList<>();
		if (!helped.contains(caster) && caster.isAlive()) {
			extra.add(caster);
		}
		double reach = SpellNumbers.KINDRED_REACH;
		cast.level.getEntities(caster, new AABB(at, at).inflate(reach),
				e -> Targets.canHelp(caster, e) && !helped.contains(e) && e.getBoundingBox().getCenter().distanceTo(at) <= reach)
			.stream().min(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(at))).ifPresent(extra::add);
		int granted = cast.takeEntities(extra.size());
		if (granted <= 0) {
			return;
		}
		extra = extra.subList(0, granted);
		Vfx.Theme theme = Vfx.theme(node.effect);
		extra.forEach(e -> CraftedVfx.share(cast.level, at, e, theme));
		sharing = true;
		try {
			Effects.apply(cast, node, new Cast.Hit(List.copyOf(extra), at, hit.dir(), hit.origin(), null, null, false), groupPower * SpellNumbers.KINDRED_SHARE);
		} finally {
			sharing = false;
		}
	}

	/** Belated: a small clock over each creature (or the point) the effect will land on {@code ticks} from now. */
	static void belated(Cast cast, Cast.Hit hit, int ticks) {
		if (hit.entities().isEmpty()) {
			CraftedVfx.belated(cast.level, hit.point().add(0, 0.6, 0), ticks);
			return;
		}
		for (Entity e : hit.entities()) {
			CraftedVfx.belated(cast.level, e.position().add(0, e.getBbHeight() + 0.35, 0), ticks);
		}
	}

	// ------------------------------------------------------------------ Spellbrand

	/** A brand one caster left on a creature: whose, the cast it came from, how hard it bursts, and from when until when. */
	private record Brand(UUID caster, Cast cast, double power, long at, long until) {}

	private static final Map<UUID, Brand> BRANDS = new ConcurrentHashMap<>();

	private static void spellbrand(Cast cast, LivingEntity t, double power, int ticks) {
		long now = cast.level.getGameTime();
		BRANDS.put(t.getUUID(), new Brand(cast.caster.getUUID(), cast, power, now, now + ticks));
		if (BRANDS.size() > 256) {
			BRANDS.values().removeIf(b -> b.until() < now);
		}
		CraftedVfx.spellbrand(cast.level, t, ticks);
	}

	/**
	 * After any spell damage ({@code Effects.hurt}): a brand its caster left on the target bursts, from the
	 * tick after it was set on (so the spell that set it can't set it off in the same breath). The burst is
	 * the brand's own damage, as arcane, a moment later.
	 */
	static void afterSpellHit(Cast cast, LivingEntity target) {
		if (BRANDS.isEmpty()) {
			return;
		}
		Brand brand = BRANDS.get(target.getUUID());
		if (brand == null) {
			return;
		}
		long now = cast.level.getGameTime();
		if (brand.until() < now) {
			BRANDS.remove(target.getUUID(), brand);
			return;
		}
		if (!brand.caster().equals(cast.caster.getUUID()) || now <= brand.at() || !BRANDS.remove(target.getUUID(), brand)) {
			return;
		}
		Cast owner = brand.cast();
		Scheduler.later(2, () -> {
			if (!owner.alive() || !target.isAlive() || target.level() != owner.level) {
				return;
			}
			CraftedVfx.spellbrandBurst(owner.level, target);
			Effects.asElement("arcane", () -> Effects.hurt(owner, target, magic(owner), BRAND_BURST * brand.power()));
		});
	}

	// ------------------------------------------------------------------ Gash

	/** Creatures that can't heal, until a game time (see {@code LivingEntityHealMixin}). */
	private static final Map<UUID, Long> GASHED = new ConcurrentHashMap<>();

	private static void gash(Cast cast, LivingEntity t, double power, int ticks) {
		CraftedVfx.gash(cast.level, t);
		Effects.hurt(cast, t, magic(cast), GASH_DAMAGE * power);
		if (!t.isAlive()) {
			return;
		}
		long now = cast.level.getGameTime();
		GASHED.merge(t.getUUID(), now + ticks, Math::max);
		// Bleeding as long as the wound stays open: wind damage on it sets off Rupture.
		Reactions.mark(t, Reactions.Mark.BLEEDING, ticks);
		ShapeRunners.each(cast, ticks, tick -> {
			if (!t.isAlive() || !gashed(t)) {
				return false;
			}
			if (tick % 20 == 19) {
				CraftedVfx.gashDrip(cast.level, t);
			}
			return true;
		});
	}

	/** Whether {@code entity} is gashed and can't heal right now. Server side only; the client's copy heals as it's told. */
	public static boolean gashed(LivingEntity entity) {
		if (GASHED.isEmpty() || entity.level().isClientSide()) {
			return false;
		}
		Long until = GASHED.get(entity.getUUID());
		if (until == null) {
			return false;
		}
		if (until < entity.level().getGameTime()) {
			GASHED.remove(entity.getUUID(), until);
			return false;
		}
		return true;
	}

	// ------------------------------------------------------------------ Prospect

	/** Each caster's ores glowing from their last Prospect: a new one puts the old out, so spamming it never piles glows up. */
	private static final Map<UUID, List<net.minecraft.world.entity.Display>> PROSPECTED = new ConcurrentHashMap<>();

	/**
	 * Prospect: every ore within {@code radius} of where it landed (loaded ground only, never loading more),
	 * nearest first and at most {@link #PROSPECT_MAX_ORES}, glows through the rock for {@code ticks}. It
	 * only shows them: nothing in the world changes.
	 */
	private static void prospect(Cast cast, Cast.Hit hit, double radius, int ticks) {
		ServerLevel level = cast.level;
		double r = Math.min(PROSPECT_MAX_RADIUS, radius);
		Vec3 centre = hit.point();
		BlockPos c = BlockPos.containing(centre);
		int reach = (int) Math.ceil(r);
		List<BlockPos> ores = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-reach, -reach, -reach), c.offset(reach, reach, reach))) {
			if (p.distSqr(c) > r * r || !level.hasChunkAt(p)) {
				continue;
			}
			if (isOre(level.getBlockState(p))) {
				ores.add(p.immutable());
			}
		}
		ores.sort(Comparator.comparingDouble(p -> p.distSqr(c)));
		CraftedVfx.prospectRing(level, centre, r, ores.size());
		List<net.minecraft.world.entity.Display> glows = new ArrayList<>();
		for (BlockPos p : ores.subList(0, Math.min(PROSPECT_MAX_ORES, ores.size()))) {
			// Each ore lights as the ring racing out over the ground reaches it (the ring takes 16 ticks to its edge).
			int delay = (int) Math.round(16 * Math.sqrt(p.distSqr(c)) / Math.max(1.0, r));
			Scheduler.later(Math.max(1, delay), () -> {
				if (PROSPECTED.get(cast.caster.getUUID()) != glows) {
					return;
				}
				BlockState ore = level.getBlockState(p);
				net.minecraft.world.entity.Display glow = CraftedVfx.oreGlow(level, p, ore, oreColor(ore), Math.max(1, ticks - delay));
				if (glow != null) {
					glows.add(glow);
				}
			});
		}
		List<net.minecraft.world.entity.Display> old = PROSPECTED.put(cast.caster.getUUID(), glows);
		if (old != null) {
			old.forEach(net.minecraft.world.entity.Display::discard);
		}
	}

	private static boolean isOre(BlockState state) {
		return state.is(BlockTags.ORES) || state.is(ConventionalBlockTags.ORES);
	}

	/** Each ore glows in the colour of what it gives (by vanilla's metal tags, then by its name, so another mod's ores match too). */
	private static int oreColor(BlockState ore) {
		if (ore.is(BlockTags.GOLD_ORES)) {
			return 0xFFD24A;
		}
		if (ore.is(BlockTags.IRON_ORES)) {
			return 0xE8C7A8;
		}
		if (ore.is(BlockTags.COPPER_ORES)) {
			return 0xE8804A;
		}
		String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(ore.getBlock()).getPath();
		if (name.contains("diamond")) {
			return 0x5CF0E8;
		}
		if (name.contains("emerald")) {
			return 0x3CE26A;
		}
		if (name.contains("redstone")) {
			return 0xFF3A2A;
		}
		if (name.contains("lapis")) {
			return 0x3A6CFF;
		}
		if (name.contains("coal")) {
			return 0x5A5A66;
		}
		if (ore.is(Blocks.ANCIENT_DEBRIS)) {
			return 0x8A5A44;
		}
		return 0xF2F0EA;
	}

	// ------------------------------------------------------------------ Searing Edge

	/** A seared weapon: the cast that seared it (its caster decides who may be burned), how hard, and until when. */
	private record Edge(Cast cast, double power, long until) {}

	private static final Map<UUID, Edge> EDGES = new ConcurrentHashMap<>();

	private static void searingEdge(Cast cast, LivingEntity t, double power, int ticks) {
		long now = cast.level.getGameTime();
		Edge old = EDGES.put(t.getUUID(), new Edge(cast, power, now + ticks));
		if (old == null || old.until() < now || !cast.passive) {
			CraftedVfx.searingEdge(cast.level, t);
		}
		if (old != null && old.until() >= now) {
			// Renewed: the one smouldering already carries on.
			return;
		}
		ShapeRunners.each(cast, ticks + 1, tick -> {
			Edge edge = EDGES.get(t.getUUID());
			if (edge == null || edge.until() < cast.level.getGameTime() || !t.isAlive()) {
				return false;
			}
			if (tick % 6 == 0) {
				CraftedVfx.searingGlow(cast.level, t);
			}
			return true;
		});
	}

	/** A blow landed: if the one who struck it with their own hand carries a seared weapon, the foe burns. */
	private static void sear(LivingEntity target, DamageSource source, boolean blocked) {
		// Only a blow struck by hand: never a spell's own damage landing (whatever its source says).
		if (EDGES.isEmpty() || blocked || Dungeons.spellLanding() || !(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker
				|| !(source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))) {
			return;
		}
		Edge edge = EDGES.get(attacker.getUUID());
		if (edge == null) {
			return;
		}
		Cast cast = edge.cast();
		if (edge.until() < cast.level.getGameTime() || !cast.alive()) {
			EDGES.remove(attacker.getUUID(), edge);
			return;
		}
		// The spell's caster says who may be burned: an ally with a seared blade never burns the caster's friends.
		if (!target.isAlive() || target.level() != cast.level || !Targets.canHarm(cast.caster, target)) {
			return;
		}
		target.igniteForSeconds(SEAR_BURN_SECONDS);
		double react = Reactions.fire(cast, target);
		CraftedVfx.searingHit(cast.level, target);
		Effects.asElement("fire", () -> Effects.hurt(cast, target, cast.level.damageSources().source(DamageTypes.IN_FIRE, attacker), SEAR_DAMAGE * edge.power() * react));
	}

	// ------------------------------------------------------------------ Flash Freeze

	private static void flashFreeze(Cast cast, LivingEntity t, double power, double duration) {
		boolean wet = WorldMagic.wet(t);
		Effects.hurt(cast, t, cast.level.damageSources().source(DamageTypes.FREEZE, cast.caster), FLASH_FREEZE_DAMAGE * power);
		if (!t.isAlive()) {
			return;
		}
		if (wet) {
			// The water on it freezes in an instant: soaked becomes frozen (fire then sets off Shatter, earth Fracture).
			int ticks = Effects.ticks(t instanceof Player ? 1.5 : 3, duration);
			Reactions.clear(t, Reactions.Mark.WET);
			Reactions.clear(t, Reactions.Mark.SOAKED);
			Spirits.freeze(t, ticks);
			BlockFx.encase(cast.level, t, ticks);
		} else {
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Effects.ticks(3, duration), 1, false, true), cast.caster);
		}
		CraftedVfx.flashFreeze(cast.level, t, wet);
	}

	// ------------------------------------------------------------------ Drowse

	/** A creature asleep, until a game time. */
	private static final Map<UUID, Long> ASLEEP = new ConcurrentHashMap<>();

	private static void drowse(Cast cast, LivingEntity t, int ticks) {
		CraftedVfx.drowse(cast.level, t);
		if (Spirits.isBoss(t)) {
			// A boss's fight must never stop: it only grows drowsy.
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, DROWSY_AMPLIFIER, false, true), cast.caster);
			return;
		}
		long now = cast.level.getGameTime();
		Spirits.hold(t, ticks);
		ASLEEP.merge(t.getUUID(), now + ticks, Math::max);
		if (t instanceof Mob mob) {
			mob.setTarget(null);
		}
		ShapeRunners.each(cast, ticks, tick -> {
			Long until = ASLEEP.get(t.getUUID());
			if (until == null || until < cast.level.getGameTime() || !t.isAlive()) {
				return false;
			}
			if (tick % 12 == 0) {
				CraftedVfx.sleeping(cast.level, t);
			}
			return true;
		});
	}

	/**
	 * Any damage wakes a sleeper: its sleep's hold ends at once, unless something else holds it longer (a
	 * Freeze), and the crawl the sleep put on it goes with it.
	 */
	private static void wake(LivingEntity t) {
		if (ASLEEP.isEmpty()) {
			return;
		}
		Long until = ASLEEP.remove(t.getUUID());
		if (until == null || !(t.level() instanceof ServerLevel level)) {
			return;
		}
		long now = level.getGameTime();
		if (until <= now) {
			return;
		}
		int left = (int) (until - now);
		if (t instanceof Mob mob) {
			Long frozen = mob.getAttached(WildercordAttachments.FROZEN_UNTIL);
			if (frozen != null && frozen <= until) {
				Spirits.thawNow(mob);
			}
		}
		// Spirits.hold's crawl, if it's still the sleep's (no longer than the sleep had left).
		MobEffectInstance slow = t.getEffect(MobEffects.SLOWNESS);
		if (slow != null && slow.getAmplifier() == 6 && slow.getDuration() <= left + 2) {
			t.removeEffect(MobEffects.SLOWNESS);
		}
		MobEffectInstance weak = t.getEffect(MobEffects.WEAKNESS);
		if (weak != null && weak.getAmplifier() == 4 && weak.getDuration() <= left + 2) {
			t.removeEffect(MobEffects.WEAKNESS);
		}
		CraftedVfx.wake(level, t);
	}

	/** Whether {@code t} is asleep under Drowse (for the tests). */
	public static boolean asleep(LivingEntity t) {
		Long until = ASLEEP.get(t.getUUID());
		return until != null && until >= t.level().getGameTime();
	}

	// ------------------------------------------------------------------ Galvanize

	/** Galvanize's sparks of power (redstone blocks) still standing, so they go when the server stops, and pistons leave them be. */
	private static final Set<GlobalPos> SPARKS = ConcurrentHashMap.newKeySet();

	/**
	 * Galvanize: a redstone block in the air against the face the spell struck, for a while, written down
	 * with its world (see {@link TemporaryBlocks}) so a crash never leaves it; it drops nothing however it
	 * goes. Only in empty air nobody stands in, only where the caster may build, and out of the cast's
	 * block budget.
	 */
	private static void galvanize(Cast cast, Cast.Hit hit, int ticks) {
		ServerLevel level = cast.level;
		BlockPos pos = hit.block() != null && hit.face() != null ? hit.block().relative(hit.face()) : BlockPos.containing(hit.point());
		BlockState there = level.getBlockState(pos);
		if (!there.isAir() || !level.getEntities((Entity) null, new AABB(pos), e -> e instanceof LivingEntity).isEmpty()
				|| !Casters.mayBuild(cast.caster) || !Casters.mayEdit(cast.caster, level, pos) || !cast.takeBlock()) {
			CraftedVfx.galvanizeFizzle(level, hit.point());
			return;
		}
		BlockState spark = Blocks.REDSTONE_BLOCK.defaultBlockState();
		SPARKS.add(GlobalPos.of(level.dimension(), pos.immutable()));
		TemporaryBlocks.put(level, pos, spark, Blocks.AIR.defaultBlockState(), level.getGameTime() + ticks);
		level.setBlockAndUpdate(pos, spark);
		CraftedVfx.galvanize(level, pos);
		BlockPos at = pos.immutable();
		for (int t = 10; t < ticks; t += 10) {
			Scheduler.later(t, () -> {
				if (isSpark(level, at) && level.isLoaded(at)) {
					CraftedVfx.galvanizeHum(level, at);
				}
			});
		}
		// It always goes, even if its caster is gone.
		Scheduler.later(ticks, () -> discharge(level, at));
	}

	private static void discharge(ServerLevel level, BlockPos pos) {
		SPARKS.remove(GlobalPos.of(level.dimension(), pos));
		if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.REDSTONE_BLOCK)) {
			StormEarthFx.discharge(level, Vec3.atCenterOf(pos));
		}
		// Out of loaded ground now: it goes as its chunk loads (see TemporaryBlocks), never loaded just for this.
		if (level.isLoaded(pos)) {
			if (level.getBlockState(pos).is(Blocks.REDSTONE_BLOCK)) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
			TemporaryBlocks.remove(level, pos);
		}
	}

	/** Whether the block at {@code pos} is a Galvanize spark, only there for a while. */
	public static boolean isSpark(ServerLevel level, BlockPos pos) {
		return !SPARKS.isEmpty() && SPARKS.contains(GlobalPos.of(level.dimension(), pos));
	}

	// ------------------------------------------------------------------ Prolong

	/** Prolong: every good effect on {@code t} lasts {@code ticks} longer, up to {@link #PROLONG_CAP}; endless ones stay as they are. */
	private static void prolong(Cast cast, LivingEntity t, int ticks) {
		boolean any = false;
		for (MobEffectInstance effect : List.copyOf(t.getActiveEffects())) {
			if (effect.isInfiniteDuration() || effect.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) {
				continue;
			}
			int now = effect.getDuration();
			int longer = Math.min(now + ticks, Math.max(now, PROLONG_CAP));
			if (longer <= now) {
				continue;
			}
			// The same strength for longer updates the one it has in place (an absorption's hearts aren't refilled).
			t.addEffect(new MobEffectInstance(effect.getEffect(), longer, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()),
				cast.caster);
			any = true;
		}
		CraftedVfx.prolong(cast.level, t, any);
	}

	// ------------------------------------------------------------------ Umbra

	private static void umbra(Cast cast, LivingEntity t, double power) {
		int light = cast.level.getMaxLocalRawBrightness(BlockPos.containing(t.getEyePosition()));
		boolean dim = light <= UMBRA_DIM;
		CraftedVfx.umbra(cast.level, t, dim);
		Effects.hurt(cast, t, magic(cast), UMBRA_DAMAGE * power * (dim ? 2 : 1));
		// The dark stays on it: life damage then sets off Blight.
		Reactions.mark(t, Reactions.Mark.SHADOWED);
	}

	// ------------------------------------------------------------------ Disarm

	/** A weapon Disarm took: the creature it was taken from, the weapon, and when it goes back. */
	private record Taken(Mob mob, ItemStack stack, long until) {}

	private static final Map<UUID, Taken> DISARMED = new ConcurrentHashMap<>();

	/**
	 * Disarm: the gust throws the target a little and leaves it windswept (fire then sets off Wildfire);
	 * a creature holding something loses it for a while, then gets it back. Players and bosses keep hold,
	 * and nothing is dropped, so no weapon is ever won (or lost) this way.
	 */
	private static void disarm(Cast cast, LivingEntity t, Cast.Hit hit, int ticks) {
		Reactions.mark(t, Reactions.Mark.WINDSWEPT);
		if (!Spirits.isBoss(t)) {
			Vec3 away = Effects.horizontal(t.position().subtract(hit.origin()), hit.dir());
			Effects.push(t, away.scale(0.35).add(0, 0.2, 0));
		}
		if (!(t instanceof Mob mob) || Spirits.isBoss(t) || DISARMED.containsKey(mob.getUUID()) || mob.getMainHandItem().isEmpty()) {
			CraftedVfx.disarm(cast.level, t, ItemStack.EMPTY);
			return;
		}
		ItemStack held = mob.getMainHandItem().copy();
		mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		DISARMED.put(mob.getUUID(), new Taken(mob, held, cast.level.getGameTime() + ticks));
		CraftedVfx.disarm(cast.level, mob, held);
		Scheduler.later(ticks, () -> rearm(mob));
	}

	/** The weapon goes back into its creature's hand, if the hand is still empty (one it picked up meanwhile is kept). */
	private static void rearm(Mob mob) {
		Taken taken = DISARMED.remove(mob.getUUID());
		if (taken == null || !mob.isAlive()) {
			return;
		}
		if (mob.getMainHandItem().isEmpty()) {
			mob.setItemSlot(EquipmentSlot.MAINHAND, taken.stack());
			if (mob.level() instanceof ServerLevel level && !mob.isRemoved()) {
				CraftedVfx.rearm(level, mob);
			}
		}
	}

	/** Whether {@code mob}'s weapon is away under Disarm (for the tests). */
	public static boolean disarmed(Mob mob) {
		return DISARMED.containsKey(mob.getUUID());
	}
}
