package dev.wildercord.cast;

import com.mojang.math.Transformation;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.player.Heart;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.WorldRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Magic that changes the world where it lands, so terrain matters: fire burns the grass and boils
 * puddles into steam, frost freezes water to walk on and puts fires out, storm runs through water,
 * wind knocks arrows out of the air, earth heaves the ground, life makes it bloom and void draws
 * loose things in. What each element does, and how far and how much, is in {@link WorldRules}.
 *
 * <p>Called once for every effect a shape applies ({@link Effects#apply}), after the effect itself.
 * Block changes go through {@link Casters#mayEdit} (so never for a monster, and never inside spawn
 * protection or a claim), take from the cast's block budget and from {@link WorldRules#EDITS_PER_CAST},
 * and are only ever vanilla's own temporary or natural ones: fire (lit only where fire may spread,
 * so it burns out), frosted ice (which melts back, and is thawed after {@link WorldRules#THAW_TICKS}
 * anyway, a thaw saved with the world: see {@link Thaws}), grass and flowers. A passive renewing itself changes no blocks,
 * and nor does anything on a server whose config switches {@code features.world_changing_magic} off.
 * Everything else (the steam, the shock through water, the gusts, the heaved ground, which is only
 * block displays) works for monsters too, and with that switch off.</p>
 */
public final class WorldMagic {
	private WorldMagic() {}

	/** Per cast (every part of it shares {@link Cast#identity()}): world edits, conductions and steam clouds used, slabs heaved. */
	private static final Map<Object, int[]> USED = Collections.synchronizedMap(new WeakHashMap<>());
	private static final int EDITS = 0;
	private static final int CONDUCTS = 1;
	private static final int STEAMS = 2;
	private static final int SLABS = 3;
	private static final int[] LIMITS = {WorldRules.EDITS_PER_CAST, WorldRules.CONDUCTS_PER_CAST, 2, 3 * WorldRules.HEAVE_SLABS};

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** How many of {@code kind} this cast has left. */
	private static int left(Cast cast, int kind) {
		int[] used = USED.computeIfAbsent(cast.identity(), k -> new int[LIMITS.length]);
		return Math.max(0, LIMITS[kind] - used[kind]);
	}

	private static void spend(Cast cast, int kind, int amount) {
		USED.computeIfAbsent(cast.identity(), k -> new int[LIMITS.length])[kind] += amount;
	}

	/** Whether this cast may change the block at {@code pos}: building rights there, and room in its budgets. Takes the block if so. */
	private static boolean edit(Cast cast, BlockPos pos) {
		if (left(cast, EDITS) <= 0 || !Casters.mayEdit(cast.caster, cast.level, pos) || !cast.takeBlock()) {
			return false;
		}
		spend(cast, EDITS, 1);
		return true;
	}

	// ------------------------------------------------------------------ being wet

	/** Wet: in water or rain, or still dripping from Tidebreath, steam or a popped Bubble. */
	public static boolean wet(Entity target) {
		return WorldRules.wet(target.isInWater(), target.isInWaterOrRain(), Reactions.has(target, Reactions.Mark.WET) || Reactions.has(target, Reactions.Mark.SOAKED));
	}

	/** The damage multiplier for a hit of {@code element} on {@code target}: fire is weaker on the wet. */
	static double wetDamage(LivingEntity target, String element) {
		return "fire".equals(element) ? WorldRules.wetDamage(element, wet(target)) : 1.0;
	}

	// ------------------------------------------------------------------ the hook

	/** After an effect lands: whatever its element does to the world there. */
	static void onSpell(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, double groupPower) {
		RuneDef rune = node.effect;
		LivingEntity caster = cast.caster;
		if (WorldRules.wets(rune)) {
			for (Entity e : hit.entities()) {
				if (Targets.canHelp(caster, e)) {
					Reactions.mark(e, Reactions.Mark.WET, WorldRules.WET_TICKS);
				}
			}
		}
		WorldRules.Interaction interaction = WorldRules.of(rune);
		if (interaction == WorldRules.Interaction.NONE || hit.point() == null) {
			return;
		}
		double power = SpellNumbers.power(node) * groupPower * cast.power;
		soak(cast, interaction, hit);
		if (hit.self()) {
			return;
		}
		// A passive renewing itself (an Orbit of fire, say) never changes blocks as it goes, and a server
		// may keep this magic off its blocks altogether (the rest, steam, shocks and gusts, still comes).
		boolean edits = interaction.editsBlocks() && !cast.passive && Casters.mayBuild(caster)
			&& dev.wildercord.config.Config.get().worldChangingMagic();
		Vec3 at = hit.point();
		switch (interaction) {
			case IGNITE -> ignite(cast, at, edits);
			case FREEZE -> freeze(cast, at, Math.min(2.0, SpellNumbers.effectRadius(node)), edits);
			case CONDUCT -> conduct(cast, hit, power);
			case GUST -> gust(cast, hit, edits);
			case HEAVE -> heave(cast, at, power);
			case BLOOM -> {
				if (edits) {
					bloom(cast, hit);
				}
			}
			case DRAW -> draw(cast, at);
			default -> { }
		}
	}

	/** How being wet meets the element: fire dries what it burns, frost freezes the wet faster. */
	private static void soak(Cast cast, WorldRules.Interaction interaction, Cast.Hit hit) {
		if (interaction != WorldRules.Interaction.IGNITE && interaction != WorldRules.Interaction.FREEZE) {
			return;
		}
		for (Entity e : hit.entities()) {
			if (!(e instanceof LivingEntity t) || !Targets.canHarm(cast.caster, e)) {
				continue;
			}
			if (interaction == WorldRules.Interaction.IGNITE) {
				if (!t.isInWater()) {
					Reactions.clear(t, Reactions.Mark.WET);
					Reactions.clear(t, Reactions.Mark.SOAKED);
				}
			} else if (wet(t)) {
				t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + WorldRules.WET_FREEZE_TICKS));
				Reactions.mark(t, Reactions.Mark.FROZEN);
				Vfx.emit(cast.level, ParticleTypes.SNOWFLAKE, t.getBoundingBox().getCenter(), 6, 0.3, 0.02);
			}
		}
	}

	// ------------------------------------------------------------------ fire

	/**
	 * Fire: in water it boils, flashing a small puddle away and filling the air with steam; on land
	 * it melts snow and ice and sets whatever will burn alight.
	 */
	private static void ignite(Cast cast, Vec3 at, boolean edits) {
		ServerLevel level = cast.level;
		BlockPos water = waterAt(level, at);
		if (water != null) {
			BlockPos surface = surface(level, water);
			if (left(cast, STEAMS) > 0) {
				spend(cast, STEAMS, 1);
				steam(cast, Vec3.atBottomCenterOf(surface.above()));
			}
			if (edits) {
				evaporate(cast, surface);
			}
			return;
		}
		if (!edits) {
			return;
		}
		BlockPos centre = BlockPos.containing(at);
		double r = WorldRules.IGNITE_RADIUS;
		List<BlockPos> melt = new ArrayList<>();
		List<BlockPos> burn = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-2, -1, -2), centre.offset(2, 1, 2))) {
			if (pos.distToCenterSqr(at) > (r + 0.5) * (r + 0.5)) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.SNOW) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
				melt.add(pos.immutable());
			} else if (flammableSpot(level, pos, state)) {
				burn.add(pos.immutable());
			}
		}
		Comparator<BlockPos> nearest = Comparator.comparingDouble(p -> p.distToCenterSqr(at));
		melt.sort(nearest);
		burn.sort(nearest);
		int melted = 0;
		for (BlockPos pos : melt) {
			if (melted >= WorldRules.MELT_MAX || !edit(cast, pos)) {
				break;
			}
			BlockState state = level.getBlockState(pos);
			boolean ice = state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE);
			if (ice && !level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)) {
				level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
			} else {
				level.removeBlock(pos, false);
			}
			Motes.clouds(level, Vec3.atCenterOf(pos), 2, 0.25, Motes.STEAM, 0.9, 30, new Vec3(0, 0.03, 0), 0.01, 0.35);
			Vfx.emit(level, ParticleTypes.FALLING_WATER, Vec3.atCenterOf(pos), 3, 0.35, 0.0);
			melted++;
		}
		if (melted > 0) {
			Fx.sound(level, at, SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.4F);
		}
		int lit = 0;
		for (BlockPos pos : burn) {
			if (lit >= WorldRules.IGNITE_MAX) {
				break;
			}
			// Re-checked: a fire just lit next door may have changed what's here.
			if (!flammableSpot(level, pos, level.getBlockState(pos)) || !edit(cast, pos)) {
				continue;
			}
			level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
			ElementFx.flames(level, Vec3.atBottomCenterOf(pos), 0.4, 0.9, 2);
			ElementFx.embers(level, Vec3.atCenterOf(pos), 0.3, 4);
			lit++;
		}
		if (lit > 0) {
			ElementFx.groundRing(level, Vec3.atBottomCenterOf(centre), ElementFx.FIRE.primary(), 0.2, r + 0.5, 0.06, 10);
			Fx.sound(level, at, SoundEvents.FIRECHARGE_USE, 0.6F, 1.1F);
		}
	}

	/**
	 * Somewhere fire may catch: open air (or a flammable plant, like grass) touching something that
	 * burns, where vanilla fire could stand, and where fire spreads and burns out (the
	 * {@code fire_spread_radius_around_player} game rule); fire lit where it doesn't tick would never go out.
	 */
	private static boolean flammableSpot(ServerLevel level, BlockPos pos, BlockState state) {
		boolean plant = !state.isAir() && state.canBeReplaced() && state.ignitedByLava() && state.getFluidState().isEmpty();
		if (!state.isAir() && !plant) {
			return false;
		}
		if (!level.canSpreadFireAround(pos)) {
			return false;
		}
		BlockState fire = BaseFireBlock.getState(level, pos);
		if (!fire.canSurvive(level, pos)) {
			return false;
		}
		if (plant) {
			return true;
		}
		for (Direction direction : Direction.values()) {
			if (level.getBlockState(pos.relative(direction)).ignitedByLava()) {
				return true;
			}
		}
		return false;
	}

	/** Boils a puddle away: {@link WorldRules#PUDDLE_MAX} connected source blocks or fewer. A pond only steams. */
	private static void evaporate(Cast cast, BlockPos seed) {
		ServerLevel level = cast.level;
		List<BlockPos> puddle = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> open = new ArrayDeque<>();
		open.add(seed);
		seen.add(seed);
		while (!open.isEmpty()) {
			BlockPos pos = open.poll();
			BlockState state = level.getBlockState(pos);
			if (!state.is(Blocks.WATER)) {
				continue;
			}
			if (state.getFluidState().isSource()) {
				puddle.add(pos);
				if (puddle.size() > WorldRules.PUDDLE_MAX) {
					return;
				}
			}
			for (Direction direction : Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (seen.size() < 32 && seen.add(next)) {
					open.add(next);
				}
			}
		}
		for (BlockPos pos : puddle) {
			if (!edit(cast, pos)) {
				return;
			}
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
		Fx.sound(level, Vec3.atCenterOf(seed), SoundEvents.LAVA_EXTINGUISH, 0.7F, 1.2F);
	}

	/**
	 * A cloud of steam hangs where fire met water for {@link WorldRules#STEAM_TICKS}: soft white
	 * billows that swell as they rise, drift off on the air and thin away, thick enough to hide what's
	 * behind them, and anyone in it the caster may harm is blinded and left wet each second.
	 */
	private static void steam(Cast cast, Vec3 at) {
		ServerLevel level = cast.level;
		double r = WorldRules.STEAM_RADIUS;
		Fx.sound(level, at, SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.8F);
		Fx.sound(level, at, SoundEvents.LAVA_EXTINGUISH, 0.6F, 1.3F);
		ElementFx.ring(level, at, UP, 0xF2F6FF, 0.3, r + 0.6, 0.08, 12);
		Vfx.emit(level, ParticleTypes.SPLASH, at, 16, r * 0.5, 0.1);
		// The first gout: a burst of billows off the water, low and thick.
		Motes.clouds(level, at.add(0, 0.4, 0), 7, r * 0.35, Motes.STEAM, 2.2, 70, new Vec3(0, 0.05, 0), 0.03, 0.62);
		// The whole cloud drifts off one way on the air as it rises.
		double a = level.getRandom().nextDouble() * Math.PI * 2;
		Vec3 air = new Vec3(Math.cos(a) * 0.012, 0.035, Math.sin(a) * 0.012);
		for (int t = 0; t < WorldRules.STEAM_TICKS; t += 5) {
			int tick = t;
			Runnable billow = () -> {
				double k = 1 - tick / (double) WorldRules.STEAM_TICKS;
				Vec3 c = at.add(0, 0.35 + tick * 0.006, 0);
				Motes.clouds(level, c, (int) Math.round(3 * k) + 2, r * 0.45, Motes.STEAM, 1.7 + 0.7 * k, 55 + (int) (25 * k), air, 0.012,
					0.3 + 0.3 * k);
				if (tick % 20 == 0) {
					AABB box = new AABB(at, at).inflate(r, 1.8, r).move(0, 0.8, 0);
					for (Entity e : level.getEntities((Entity) null, box, e -> Targets.canHarm(cast.caster, e))) {
						LivingEntity living = (LivingEntity) e;
						living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, WorldRules.STEAM_BLIND_TICKS, 0, false, false));
						Reactions.mark(living, Reactions.Mark.WET, WorldRules.WET_TICKS);
					}
				}
			};
			if (t == 0) {
				billow.run();
			} else {
				Scheduler.later(t, billow);
			}
		}
	}

	// ------------------------------------------------------------------ frost

	/** Frost: puts out fire and campfires, then freezes the water's surface into frosted ice you can walk on. */
	private static void freeze(Cast cast, Vec3 at, double widen, boolean edits) {
		if (!edits) {
			return;
		}
		ServerLevel level = cast.level;
		double r = WorldRules.FREEZE_RADIUS * Math.max(1.0, widen);
		int ri = (int) Math.ceil(r);
		BlockPos centre = BlockPos.containing(at);
		int snuffed = 0;
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-2, -1, -2), centre.offset(2, 2, 2))) {
			if (snuffed >= WorldRules.SNUFF_MAX) {
				break;
			}
			if (snuff(cast, pos.immutable())) {
				snuffed++;
			}
		}
		if (snuffed > 0) {
			Fx.sound(level, at, SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.7F, 1.3F);
		}
		BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
		List<BlockPos> frozen = new ArrayList<>();
		List<BlockPos> columns = new ArrayList<>();
		for (int dx = -ri; dx <= ri; dx++) {
			for (int dz = -ri; dz <= ri; dz++) {
				if (dx * dx + dz * dz <= r * r) {
					columns.add(centre.offset(dx, 0, dz));
				}
			}
		}
		columns.sort(Comparator.comparingInt(p -> p.distManhattan(centre)));
		for (BlockPos column : columns) {
			if (frozen.size() >= WorldRules.FREEZE_MAX) {
				break;
			}
			// A spell that sank to the bottom (bolts fly through water) still freezes the top, up to 4 blocks up.
			for (int dy = 4; dy >= -2; dy--) {
				BlockPos pos = column.above(dy);
				BlockState state = level.getBlockState(pos);
				if (!state.is(Blocks.WATER) || !state.getFluidState().isSource() || !level.getBlockState(pos.above()).isAir()) {
					continue;
				}
				// Never around a creature swimming in it (frost walker's rule): it would be stuck in the ice.
				if (level.isUnobstructed(ice, pos, CollisionContext.empty()) && edit(cast, pos)) {
					level.setBlockAndUpdate(pos, ice);
					level.scheduleTick(pos, Blocks.FROSTED_ICE, 60 + level.getRandom().nextInt(60));
					frozen.add(pos);
				}
				break;
			}
		}
		if (frozen.isEmpty()) {
			return;
		}
		BlockPos first = frozen.getFirst();
		Vec3 top = new Vec3(at.x, first.getY() + 1.02, at.z);
		ElementFx.frostCreep(level, top, Math.min(3.5, r), 26);
		ElementFx.shatterRing(level, top.add(0, 0.1, 0), r);
		for (int i = 0; i < Math.min(6, frozen.size()); i++) {
			Vfx.emit(level, ParticleTypes.SNOWFLAKE, Vec3.atCenterOf(frozen.get(i)).add(0, 0.6, 0), 2, 0.3, 0.01);
		}
		Fx.sound(level, top, SoundEvents.GLASS_PLACE, 0.8F, 1.5F);
		Fx.sound(level, top, WildercordSounds.impact("frost"), 0.4F, 1.2F);
		thawLater(level, frozen);
		watchBridge(cast, frozen);
	}

	/** Puts out a fire, or snuffs a lit campfire; true if it did. */
	private static boolean snuff(Cast cast, BlockPos pos) {
		ServerLevel level = cast.level;
		BlockState state = level.getBlockState(pos);
		if (state.is(BlockTags.FIRE)) {
			if (!edit(cast, pos)) {
				return false;
			}
			level.removeBlock(pos, false);
			Motes.smoke(level, Vec3.atCenterOf(pos), 2, 0.25);
			Vfx.emit(level, ParticleTypes.SNOWFLAKE, Vec3.atCenterOf(pos), 3, 0.25, 0.02);
			return true;
		}
		if (CampfireBlock.isLitCampfire(state)) {
			if (!edit(cast, pos)) {
				return false;
			}
			CampfireBlock.douse(cast.caster, level, pos, state);
			level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
			Motes.smoke(level, Vec3.atCenterOf(pos).add(0, 0.3, 0), 3, 0.2);
			return true;
		}
		return false;
	}

	/**
	 * Frosted ice melts by itself in the light; this makes sure ice frozen in the dark melts back too.
	 * The thaw is saved with the world ({@link Thaws}), so it happens even across a restart, or when the
	 * chunk next loads if nobody was near when its time came.
	 */
	private static void thawLater(ServerLevel level, List<BlockPos> frozen) {
		Thaws.schedule(level, frozen, level.getGameTime() + WorldRules.THAW_TICKS + level.getRandom().nextInt(60));
	}

	/** Ice each player froze, for Icebridge: walk on it before it thaws. */
	private static final Map<UUID, Set<BlockPos>> BRIDGES = new HashMap<>();
	/** Until when each player's ice is being watched (game time). */
	private static final Map<UUID, Long> WATCHED = new HashMap<>();

	/** The ice being watched goes with the server (its watchers are scheduled, and the schedule is cleared too). */
	public static void init() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BRIDGES.clear();
			WATCHED.clear();
		});
	}

	private static void watchBridge(Cast cast, List<BlockPos> frozen) {
		if (!(cast.caster instanceof ServerPlayer player) || Heart.discovered(player, "feat:" + Feats.ICEBRIDGE)) {
			return;
		}
		UUID id = player.getUUID();
		long now = cast.level.getGameTime();
		Long until = WATCHED.get(id);
		boolean watching = until != null && until > now && until - now <= WorldRules.THAW_TICKS;
		if (!watching) {
			BRIDGES.put(id, new HashSet<>());
		}
		BRIDGES.computeIfAbsent(id, k -> new HashSet<>()).addAll(frozen);
		WATCHED.put(id, now + WorldRules.THAW_TICKS);
		if (!watching) {
			watch(player, cast.level);
		}
	}

	private static void watch(ServerPlayer player, ServerLevel level) {
		UUID id = player.getUUID();
		Long until = WATCHED.get(id);
		Set<BlockPos> ice = BRIDGES.get(id);
		if (until == null || ice == null || player.isRemoved() || player.level() != level || level.getGameTime() > until) {
			BRIDGES.remove(id);
			WATCHED.remove(id);
			return;
		}
		BlockPos under = player.getOnPos();
		if (player.onGround() && ice.contains(under) && level.getBlockState(under).is(Blocks.FROSTED_ICE)) {
			Grimoire.feat(player, Feats.ICEBRIDGE);
			BRIDGES.remove(id);
			WATCHED.remove(id);
			return;
		}
		Scheduler.later(5, () -> watch(player, level));
	}

	// ------------------------------------------------------------------ storm

	/**
	 * Storm in water: the shock runs through all the water connected to where it landed (up to
	 * {@link WorldRules#CONDUCT_RADIUS} blocks), striking every creature the caster may harm that's
	 * in it, a little less the further off. Creatures the spell hit directly aren't struck twice.
	 */
	private static void conduct(Cast cast, Cast.Hit hit, double power) {
		ServerLevel level = cast.level;
		if (left(cast, CONDUCTS) <= 0) {
			return;
		}
		BlockPos seed = waterAt(level, hit.point());
		Vec3 landed = hit.point();
		for (int i = 0; seed == null && i < hit.entities().size(); i++) {
			Entity e = hit.entities().get(i);
			if (e.isInWater()) {
				seed = waterAt(level, e.position().add(0, 0.2, 0));
				landed = e.position();
			}
		}
		if (seed == null) {
			return;
		}
		spend(cast, CONDUCTS, 1);
		Set<BlockPos> water = connectedWater(level, seed);
		int r = WorldRules.CONDUCT_RADIUS;
		Vec3 origin = Vec3.atCenterOf(seed);
		List<LivingEntity> struck = new ArrayList<>();
		for (Entity e : level.getEntities((Entity) null, new AABB(seed).inflate(r, 4, r), e -> Targets.canHarm(cast.caster, e) && e.isInWater())) {
			if (hit.entities().contains(e)) {
				continue;
			}
			BlockPos feet = e.blockPosition();
			if (water.contains(feet) || water.contains(feet.above()) || water.contains(feet.below())) {
				struck.add((LivingEntity) e);
			}
		}
		struck.sort(Comparator.comparingDouble(e -> e.distanceToSqr(origin)));
		if (struck.size() > WorldRules.CONDUCT_TARGETS) {
			struck = struck.subList(0, WorldRules.CONDUCT_TARGETS);
		}
		BlockPos surface = surface(level, seed);
		double waterY = surface.getY() + level.getFluidState(surface).getHeight(level, surface);
		Vec3 strike = new Vec3(landed.x, waterY + 0.1, landed.z);
		conductFlash(level, strike, water);
		for (int i = 0; i < struck.size(); i++) {
			LivingEntity t = struck.get(i);
			shockThrough(level, strike, t, waterY, i);
			double amount = WorldRules.conductDamage(Math.sqrt(t.distanceToSqr(origin))) * power;
			Effects.hurt(cast, t, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), amount);
		}
		if (!struck.isEmpty()) {
			Reactions.callout(cast, "conduct", 0xFFE650);
		}
		if (struck.size() >= WorldRules.CONDUCTOR_FEAT) {
			Grimoire.feat(cast.caster, Feats.CONDUCTOR);
		}
	}

	/** Arcs and the water's glow: pale blue-white, like lightning seen through water. */
	private static final int SHOCK = 0x9AD8FF;

	/**
	 * Where storm meets water: a flash on the surface, rings racing out over it and a few arcs
	 * skittering off across it (only over the water that's joined up), whether or not anything is in
	 * it to be struck.
	 */
	private static void conductFlash(ServerLevel level, Vec3 strike, Set<BlockPos> water) {
		Sigils.flash(level, strike.add(0, 0.25, 0), 0xEAF6FF, 2.6F);
		Sigils.flash(level, strike.add(0, 0.2, 0), ElementFx.STORM.primary(), 1.4F);
		ElementFx.ring(level, strike, UP, 0x4AA8FF, 0.3, 3.8, 0.07, 11);
		ElementFx.ring(level, strike, UP, ElementFx.STORM.primary(), 0.2, 2.6, 0.045, 8);
		ElementFx.ring(level, strike, UP, SHOCK, 0.1, 1.4, 0.03, 6);
		RandomSource random = level.getRandom();
		double phase = random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < 5; i++) {
			double a = phase + Math.PI * 2 * i / 5 + (random.nextDouble() - 0.5) * 0.7;
			double reach = 1.2 + random.nextDouble() * 1.6;
			Vec3 end = strike.add(Math.cos(a) * reach, 0, Math.sin(a) * reach);
			// Only as far as the water goes: never out over the bank.
			if (!water.contains(BlockPos.containing(end.x, strike.y - 0.3, end.z))) {
				end = strike.add(Math.cos(a) * reach * 0.45, 0, Math.sin(a) * reach * 0.45);
				if (!water.contains(BlockPos.containing(end.x, strike.y - 0.3, end.z))) {
					continue;
				}
			}
			ElementFx.arc(level, strike, end, i % 2 == 0 ? SHOCK : ElementFx.STORM.primary(), 0.04, 1, true, 5 + random.nextInt(3));
		}
		ElementFx.sparks(level, strike.add(0, 0.2, 0), 12, 0.35);
		Vfx.emit(level, ParticleTypes.BUBBLE, strike.add(0, -0.6, 0), 12, 1.2, 0.05);
		Fx.sound(level, strike, WildercordSounds.impact("storm"), 0.6F, 1.3F);
	}

	/**
	 * The shock reaching one creature in the water, a tick after the one before it: branching arcs
	 * skitter over the surface from where it struck to the creature, jump up it, and sparks burst off it.
	 */
	private static void shockThrough(ServerLevel level, Vec3 strike, LivingEntity t, double waterY, int order) {
		Vec3 centre = t.getBoundingBox().getCenter();
		Vec3 onWater = new Vec3(t.getX(), waterY + 0.1, t.getZ());
		// Somewhere on it that shows above the water.
		Vec3 above = new Vec3(centre.x, Math.max(centre.y, waterY + Math.min(0.5, t.getBbHeight() * 0.5)), centre.z);
		double width = t.getBbWidth();
		Scheduler.later(1 + order, () -> {
			ElementFx.arc(level, strike, onWater, SHOCK, 0.07, 2, true, 9);
			ElementFx.arc(level, strike, onWater, ElementFx.STORM.primary(), 0.035, 1, true, 6);
			ElementFx.arc(level, onWater, above, ElementFx.STORM.secondary(), 0.04, 1, false, 7);
			ElementFx.ring(level, onWater, UP, SHOCK, 0.2, width + 1.0, 0.04, 7);
			Sigils.flash(level, above, ElementFx.STORM.secondary(), (float) (0.8 + width * 0.6));
			ElementFx.sparks(level, above, 8, 0.3);
			if (order < 4) {
				Fx.sound(level, above, SoundEvents.TRIDENT_THUNDER.value(), 0.25F, 1.9F);
			}
		});
	}

	/** The water joined to {@code seed} (source, flowing or waterlogged), within reach and a fixed number of blocks. */
	private static Set<BlockPos> connectedWater(ServerLevel level, BlockPos seed) {
		Set<BlockPos> water = new HashSet<>();
		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> open = new ArrayDeque<>();
		open.add(seed);
		seen.add(seed);
		int r = WorldRules.CONDUCT_RADIUS;
		while (!open.isEmpty() && water.size() < WorldRules.CONDUCT_WATER_MAX) {
			BlockPos pos = open.poll();
			if (!level.isLoaded(pos) || !level.getFluidState(pos).is(FluidTags.WATER)) {
				continue;
			}
			water.add(pos);
			for (Direction direction : Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (Math.abs(next.getX() - seed.getX()) <= r && Math.abs(next.getY() - seed.getY()) <= r && Math.abs(next.getZ() - seed.getZ()) <= r
						&& seen.add(next)) {
					open.add(next);
				}
			}
		}
		return water;
	}

	// ------------------------------------------------------------------ wind

	/**
	 * Wind: arrows, tridents, fireballs and enemy bolts near where it lands are flung back the way
	 * the wind blows; small fires blow out; loose items and experience scatter.
	 */
	private static void gust(Cast cast, Cast.Hit hit, boolean edits) {
		ServerLevel level = cast.level;
		Vec3 at = hit.point();
		Vec3 from = hit.origin() != null ? hit.origin() : cast.caster.position();
		double r = WorldRules.GUST_RADIUS;
		AABB box = new AABB(at, at).inflate(r);
		int deflected = 0;
		for (Projectile p : level.getEntitiesOfClass(Projectile.class, box, p -> foreign(cast, p))) {
			if (deflected >= WorldRules.GUST_PROJECTILES) {
				break;
			}
			Vec3 away = blowing(p.position(), from, hit.dir());
			double speed = Math.max(0.8, p.getDeltaMovement().length() * 0.9);
			p.setDeltaMovement(away.scale(speed).add(0, 0.15, 0));
			p.needsSync = true;
			ElementFx.slash(level, p.position(), ElementFx.perp(away), away, ElementFx.WIND.secondary(), 0.5, 2.4, 0.06, 2, 6);
			deflected++;
		}
		if (deflected > 0) {
			Fx.sound(level, at, SoundEvents.BREEZE_DEFLECT, 0.8F, 1.1F);
		}
		int loose = 0;
		for (Entity e : level.getEntities((Entity) null, box, e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
			if (loose++ >= WorldRules.GUST_LOOSE) {
				break;
			}
			Vec3 away = blowing(e.position(), from, hit.dir());
			e.setDeltaMovement(e.getDeltaMovement().add(away.scale(0.55)).add(0, 0.25, 0));
			e.needsSync = true;
		}
		if (loose > 0) {
			Vfx.emit(level, ParticleTypes.SMALL_GUST, at, 2, r * 0.3, 0.0);
		}
		if (!edits) {
			return;
		}
		BlockPos centre = BlockPos.containing(at);
		int out = 0;
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-2, -1, -2), centre.offset(2, 1, 2))) {
			if (out >= WorldRules.GUST_FIRES) {
				break;
			}
			if (level.getBlockState(pos).is(BlockTags.FIRE) && edit(cast, pos.immutable())) {
				level.removeBlock(pos, false);
				Motes.smoke(level, Vec3.atCenterOf(pos), 2, 0.3);
				out++;
			}
		}
		if (out > 0) {
			ElementFx.gustRing(level, Vec3.atBottomCenterOf(centre), 2.5);
			Fx.sound(level, at, SoundEvents.FIRE_EXTINGUISH, 0.6F, 1.5F);
		}
	}

	/** A projectile in flight that isn't the caster's or an ally's: the wind's to turn. */
	private static boolean foreign(Cast cast, Projectile p) {
		if (p instanceof FishingHook || p.getDeltaMovement().lengthSqr() < 0.01) {
			return false;
		}
		Entity owner = p.getOwner();
		return owner == null || owner != cast.caster && !(owner instanceof LivingEntity && Targets.isAlly(cast.caster, owner));
	}

	/** The way the wind blows at {@code pos}: out from where it came, flat, or along the spell when it's right on top. */
	private static Vec3 blowing(Vec3 pos, Vec3 from, Vec3 fallback) {
		Vec3 flat = new Vec3(pos.x - from.x, 0, pos.z - from.z);
		if (flat.lengthSqr() < 0.04) {
			flat = fallback != null ? new Vec3(fallback.x, 0, fallback.z) : new Vec3(1, 0, 0);
		}
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
	}

	// ------------------------------------------------------------------ earth

	/**
	 * Earth: slabs of the ground heave up round where it lands and settle back (block displays; the
	 * world's blocks are never touched), throwing creatures standing there upward.
	 */
	private static void heave(Cast cast, Vec3 at, double power) {
		ServerLevel level = cast.level;
		Vec3 floor = ElementFx.floor(level, at, 1.5);
		if (floor == null) {
			return;
		}
		int slabs = Math.min(WorldRules.HEAVE_SLABS, left(cast, SLABS));
		if (slabs > 0) {
			spend(cast, SLABS, slabs);
			BlockState ground = ElementFx.groundBlock(level, floor);
			if (ground.getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL) {
				ground = Blocks.DIRT.defaultBlockState();
			}
			double phase = level.getRandom().nextDouble() * Math.PI * 2;
			for (int i = 0; i < slabs; i++) {
				double a = phase + i * Math.PI * 2 / slabs;
				double d = 0.7 + level.getRandom().nextDouble() * (WorldRules.HEAVE_RADIUS - 0.7);
				slab(level, floor.add(Math.cos(a) * d, 0, Math.sin(a) * d), ground, (float) a, 2 + i);
			}
			ElementFx.crack(level, floor, WorldRules.HEAVE_RADIUS, 20);
			Fx.sound(level, floor, SoundEvents.ROOTED_DIRT_BREAK, 0.9F, 0.7F);
		}
		double lift = WorldRules.HEAVE_LIFT * Math.sqrt(Math.min(4.0, Math.max(0.25, power)));
		AABB box = new AABB(floor, floor).inflate(WorldRules.HEAVE_RADIUS, 1.0, WorldRules.HEAVE_RADIUS);
		for (Entity e : level.getEntities((Entity) null, box, e -> Targets.canHarm(cast.caster, e) && e.onGround())) {
			Effects.push((LivingEntity) e, new Vec3(0, lift, 0));
		}
	}

	/** One slab of ground: a block display that tilts up out of the floor, holds, and sinks back. */
	private static void slab(ServerLevel level, Vec3 base, BlockState state, float yaw, int delay) {
		Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (display == null) {
			return;
		}
		float w = 0.7F;
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		Quaternionf tilt = new Quaternionf().rotateY(yaw).rotateX(0.35F);
		display.snapTo(base.x, base.y, base.z);
		display.setBlockState(state);
		display.setTransformation(new Transformation(new Vector3f(-w / 2, -0.55F, -w / 2), turn, new Vector3f(w, 0.5F, w), new Quaternionf()));
		BlockFx.fresh(display);
		level.addFreshEntity(display);
		Scheduler.later(delay, () -> {
			if (!display.isRemoved()) {
				display.setTransformationInterpolationDelay(0);
				display.setTransformationInterpolationDuration(3);
				display.setTransformation(new Transformation(new Vector3f(-w / 2, -0.05F, -w / 2), tilt, new Vector3f(w, 0.5F, w), new Quaternionf()));
				Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, state), base.add(0, 0.2, 0), 4, 0.25, 0.1);
			}
		});
		Scheduler.later(delay + 18, () -> {
			if (!display.isRemoved()) {
				display.setTransformationInterpolationDelay(0);
				display.setTransformationInterpolationDuration(8);
				display.setTransformation(new Transformation(new Vector3f(-w / 2, -0.6F, -w / 2), turn, new Vector3f(w, 0.5F, w), new Quaternionf()));
			}
		});
		Scheduler.later(delay + 27, display::discard);
	}

	// ------------------------------------------------------------------ life

	private static final Block[] FLOWERS = {
		Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS};

	/**
	 * Life: grass and flowers spring up round where it lands (on ground they can grow on, like bone
	 * meal), and a crop or sapling it lands on grows a stage.
	 */
	private static void bloom(Cast cast, Cast.Hit hit) {
		ServerLevel level = cast.level;
		Vec3 floor = ElementFx.floor(level, hit.point(), 2.0);
		if (floor == null) {
			return;
		}
		BlockPos centre = BlockPos.containing(floor.x, floor.y + 0.01, floor.z);
		int grown = 0;
		BlockPos plant = hit.block() != null ? hit.block() : centre;
		BlockState crop = level.getBlockState(plant);
		if ((crop.is(BlockTags.CROPS) || crop.is(BlockTags.SAPLINGS)) && edit(cast, plant)) {
			BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, plant);
			level.levelEvent(net.minecraft.world.level.block.LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, plant, 15);
			grown++;
		}
		List<BlockPos> spots = new ArrayList<>();
		int ri = (int) Math.ceil(WorldRules.BLOOM_RADIUS);
		for (int dx = -ri; dx <= ri; dx++) {
			for (int dz = -ri; dz <= ri; dz++) {
				if (dx * dx + dz * dz > WorldRules.BLOOM_RADIUS * WorldRules.BLOOM_RADIUS) {
					continue;
				}
				for (int dy = 1; dy >= -1; dy--) {
					BlockPos pos = centre.offset(dx, dy, dz);
					if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).is(BlockTags.DIRT)) {
						spots.add(pos);
						break;
					}
				}
			}
		}
		Collections.shuffle(spots, new java.util.Random(level.getRandom().nextLong()));
		for (BlockPos pos : spots) {
			if (grown >= WorldRules.BLOOM_MAX) {
				break;
			}
			BlockState flower = FLOWERS[level.getRandom().nextInt(FLOWERS.length)].defaultBlockState();
			if (!flower.canSurvive(level, pos) || !edit(cast, pos)) {
				continue;
			}
			level.setBlockAndUpdate(pos, flower);
			ElementFx.petals(level, Vec3.atCenterOf(pos), 0.25, 3);
			Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(pos), 3, 0.3, 0.0);
			grown++;
		}
		if (grown > 0) {
			ElementFx.bloom(level, floor.add(0, 0.4, 0), floor.add(0, 0.05, 0), 1.2);
			Fx.sound(level, floor, SoundEvents.BONE_MEAL_USE, 0.8F, 1.1F);
		}
	}

	// ------------------------------------------------------------------ void

	/** Void: loose items and experience nearby are drawn in toward where it lands, into a little knot of darkness. */
	private static void draw(Cast cast, Vec3 at) {
		ServerLevel level = cast.level;
		int drawn = 0;
		for (Entity e : level.getEntities((Entity) null, new AABB(at, at).inflate(WorldRules.DRAW_RADIUS),
				e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
			if (drawn >= WorldRules.DRAW_LOOSE) {
				break;
			}
			Vec3 towards = at.subtract(e.position());
			double d = towards.length();
			if (d < 0.5) {
				continue;
			}
			e.setDeltaMovement(towards.normalize().scale(Math.min(0.6, 0.15 + d * 0.1)).add(0, 0.12, 0));
			e.needsSync = true;
			drawn++;
		}
		if (drawn > 0) {
			ElementFx.implode(level, at.add(0, 0.3, 0), 1.6, 8);
			ElementFx.blackCore(level, at.add(0, 0.3, 0), 0.25, 8);
		}
	}

	// ------------------------------------------------------------------ helpers

	/** The water where a spell landed (or just above or below it), or null on dry land. */
	private static BlockPos waterAt(ServerLevel level, Vec3 at) {
		BlockPos pos = BlockPos.containing(at);
		for (BlockPos p : new BlockPos[] {pos, pos.above(), pos.below()}) {
			if (level.getFluidState(p).is(FluidTags.WATER)) {
				return p;
			}
		}
		return null;
	}

	/** The top of the water column {@code pos} is in (bolts sink to the bottom), up to 6 blocks up. */
	private static BlockPos surface(ServerLevel level, BlockPos pos) {
		BlockPos top = pos;
		for (int i = 0; i < 6 && level.getFluidState(top.above()).is(FluidTags.WATER); i++) {
			top = top.above();
		}
		return top;
	}
}
