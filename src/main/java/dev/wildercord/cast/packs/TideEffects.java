package dev.wildercord.cast.packs;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Casters;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.TemporaryBlocks;
import dev.wildercord.cast.Vfx;
import dev.wildercord.mixin.FishingHookAccessor;
import dev.wildercord.mixin.ItemEntityAccessor;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * The fishing, water and coast pack (fx-fish): 48 gentle effects, handed here by name from
 * {@code Effects}. None of them hurts anything. Every block they touch is checked for claims, the world
 * border and build height, never takes a block another spell is holding for a while, and comes out of the
 * cast's block budget; creatures come out of its creature budget. Fishing is only ever hurried or read, never
 * given loot of its own: Bait Blessing is vanilla Luck, which vanilla fishing already reads.
 * Numbers live in {@link TideRules}.
 */
public final class TideEffects {
	private TideEffects() {}

	private static final Identifier DIVERS_HANDS = Wildercord.id("divers_hands");
	/** Bobbers Angler's Lure already hurried (once each). */
	private static final Set<FishingHook> LURED = Collections.newSetFromMap(new WeakHashMap<>());
	/** Each caster's one Tide Marker. */
	private static final Map<UUID, Object> MARKERS = new ConcurrentHashMap<>();
	/** When each creature's Diver's Hands ends. */
	private static final Map<UUID, Long> DIVING = new ConcurrentHashMap<>();

	public static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, double power, double duration,
			int amplify, boolean passive) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		double radius = SpellNumbers.effectRadius(node);
		switch (node.effect.path()) {
			// ---------------------------------------------------------------- fishing
			case "angler_lure" -> angler(cast);
			case "bait_blessing" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.LUCK, ticks(60, duration, passive), TideRules.luckAmplifier(amplify), false, true));
				emit(level, ParticleTypes.HAPPY_VILLAGER, t.position().add(0, 1, 0), 10, 0.4);
				sound(level, t.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F);
			});
			case "reeling_tide" -> reel(cast);
			case "school_sight" -> glowAquatic(cast, hit.point(), Math.min(24.0, 16.0 * radius), ticks(12, duration, passive));
			case "tackle_mend" -> helped.forEach(t -> mendRod(cast, t, power));
			case "bobber_bell" -> bobberBell(cast, ticks(60, duration, passive));
			case "water_reading" -> waterReading(cast);
			case "dolphin_call" -> dolphinCall(cast, ticks(20, duration, passive));
			case "axolotl_kinship" -> axolotls(cast, ticks(30, duration, passive));
			case "shoal_herd" -> shoal(cast, hit.point(), Math.min(16.0, 10.0 * radius));
			case "refloat" -> refloat(cast, hit.point());
			// ---------------------------------------------------------------- water work
			case "reed_cut" -> reedCut(cast, hit.point());
			case "wring" -> wring(cast, hit.point());
			case "spring_draw" -> springDraw(cast, hit);
			case "brimming" -> brimming(cast, hit.point());
			case "diving_bell" -> divingBell(cast, hit.point(), ticks(15, duration, passive));
			case "tide_lantern" -> tideLantern(cast, hit, ticks(60, duration, passive));
			case "sluice" -> {
				if (sluice(cast, hit.point(), 4, TideRules.SLUICE_MAX) == 0) nothing(cast);
			}
			case "soak_through" -> soak(cast, hit.point());
			case "rain_cloud" -> rainCloud(cast, hit.point(), ticks(8, duration, passive));
			case "storm_glass" -> stormGlass(cast);
			case "kelpsong" -> kelpsong(cast, hit.point());
			case "coral_mend" -> coralMend(cast, hit.point());
			case "nest_tend" -> nestTend(cast, hit.point());
			case "lily_path" -> surfacePath(cast, Blocks.LILY_PAD.defaultBlockState(), true, TideRules.LILY_MAX, ticks(30, duration, passive));
			case "sandbar" -> surfacePath(cast, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), false, TideRules.SANDBAR_LENGTH, ticks(20, duration, passive));
			case "ice_auger" -> iceAuger(cast, hit);
			case "tide_marker" -> tideMarker(cast, hit.point());
			case "shore_sense" -> shoreSense(cast);
			case "fathom" -> fathom(cast, hit.point());
			case "wreck_sense" -> wreckSense(cast, hit.point(), ticks(15, duration, passive));
			case "drift_net" -> driftNet(cast, hit.point());
			case "mooring_call" -> mooringCall(cast);
			// ---------------------------------------------------------------- movement
			case "fair_wind" -> fairWind(cast, ticks(10, duration, passive));
			case "upwell" -> swim(cast, 0.9, 6);
			case "sounding" -> swim(cast, -0.7, Math.max(10, ticks(2, duration, passive)));
			case "porpoise" -> porpoise(cast);
			// ---------------------------------------------------------------- helpful
			case "oceans_favor" -> helped.forEach(t -> {
				int ticks = ticks(120, duration, passive);
				for (Holder<MobEffect> e : List.of(MobEffects.WATER_BREATHING, MobEffects.DOLPHINS_GRACE, MobEffects.CONDUIT_POWER, MobEffects.NIGHT_VISION)) {
					t.addEffect(new MobEffectInstance(e, ticks, 0, false, true));
				}
				emit(level, ParticleTypes.NAUTILUS, t.position().add(0, 1, 0), 30, 0.8);
				sound(level, t.position(), SoundEvents.CONDUIT_ACTIVATE, 1.0F);
			});
			case "skimstep" -> helped.forEach(t -> skimstep(cast, t, ticks(15, duration, passive)));
			case "skaters_edge" -> helped.forEach(t -> {
				boolean ice = level.getBlockState(t.getBlockPosBelowThatAffectsMyMovement()).is(BlockTags.ICE);
				t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks(30, duration, passive), TideRules.skateAmplifier(ice), false, true));
				emit(level, ParticleTypes.SNOWFLAKE, t.position(), 12, 0.4);
			});
			case "air_pocket" -> helped.forEach(t -> {
				t.setAirSupply(t.getMaxAirSupply());
				emit(level, ParticleTypes.BUBBLE, t.getEyePosition(), 16, 0.3);
				sound(level, t.position(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.2F);
			});
			case "drown_ward" -> helped.forEach(t -> drownWard(cast, t, ticks(60, duration, passive)));
			case "pearl_sight" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, ticks(30, duration, passive), 0, false, true));
				emit(level, ParticleTypes.NAUTILUS, t.getEyePosition(), 16, 0.5);
				sound(level, t.position(), SoundEvents.CONDUIT_ACTIVATE, 1.4F);
			});
			case "sea_breeze" -> helped.forEach(t -> {
				t.removeEffect(MobEffects.MINING_FATIGUE);
				t.removeEffect(MobEffects.NAUSEA);
				t.removeEffect(MobEffects.HUNGER);
				emit(level, ParticleTypes.CLOUD, t.position().add(0, 1, 0), 10, 0.5);
				sound(level, t.position(), SoundEvents.BOAT_PADDLE_WATER, 1.3F);
			});
			case "inkveil" -> helped.forEach(t -> {
				int seconds = TideRules.inkveilSeconds(t.isInWater());
				t.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks(seconds, duration, passive), 0, false, true));
				emit(level, ParticleTypes.SQUID_INK, t.position().add(0, 1, 0), 24, 0.6);
				sound(level, t.position(), SoundEvents.SQUID_SQUIRT, 1.0F);
			});
			case "shellback" -> helped.forEach(t -> {
				int seconds = TideRules.shellbackSeconds(t.isInWaterOrRain());
				t.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks(seconds, duration, passive), 0, false, true));
				emit(level, ParticleTypes.NAUTILUS, t.position().add(0, 1, 0), 10, 0.5);
				sound(level, t.position(), SoundEvents.TURTLE_EGG_CRACK, 0.8F);
			});
			case "dewcatch" -> dewcatch(cast);
			case "divers_hands" -> helped.forEach(t -> diversHands(t, ticks(60, duration, passive)));
			default -> { }
		}
	}

	// ==================================================================== fishing

	private static FishingHook bobber(Cast cast) {
		if (cast.caster instanceof Player player && player.fishing != null && player.fishing.isAlive()
				&& player.fishing.level() == cast.level && player.fishing.distanceTo(player) <= 32.0) {
			return player.fishing;
		}
		Casters.tell(cast.caster, Component.translatable("message.wildercord.tide.no_bobber"));
		return null;
	}

	private static void angler(Cast cast) {
		FishingHook hook = bobber(cast);
		if (hook == null) {
			return;
		}
		FishingHookAccessor waits = (FishingHookAccessor) (Object) hook;
		if (LURED.add(hook)) {
			if (waits.wildercord$timeUntilLured() > 0) {
				waits.wildercord$setTimeUntilLured(TideRules.lured(waits.wildercord$timeUntilLured()));
			} else if (waits.wildercord$timeUntilHooked() > 0) {
				waits.wildercord$setTimeUntilHooked(TideRules.lured(waits.wildercord$timeUntilHooked()));
			}
			Casters.tell(cast.caster, Component.translatable("message.wildercord.tide.lured"));
		}
		emit(cast.level, ParticleTypes.FISHING, hook.position(), 12, 0.4);
		sound(cast.level, hook.position(), SoundEvents.FISHING_BOBBER_SPLASH, 1.3F);
	}

	private static void reel(Cast cast) {
		FishingHook hook = bobber(cast);
		if (hook == null) {
			return;
		}
		Player player = (Player) cast.caster;
		InteractionHand hand = player.getMainHandItem().is(Items.FISHING_ROD) ? InteractionHand.MAIN_HAND
			: player.getOffhandItem().is(Items.FISHING_ROD) ? InteractionHand.OFF_HAND : null;
		if (hand == null) {
			Casters.tell(cast.caster, Component.translatable("message.wildercord.tide.no_rod"));
			return;
		}
		ItemStack rod = player.getItemInHand(hand);
		Vec3 at = hook.position();
		// Exactly what using the rod does: vanilla's own catch, and the rod wears as it would.
		int wear = hook.retrieve(rod);
		if (wear > 0) {
			rod.hurtAndBreak(wear, player, hand);
		}
		emit(cast.level, ParticleTypes.SPLASH, at, 16, 0.4);
		sound(cast.level, player.position(), SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0F);
	}

	private static void mendRod(Cast cast, LivingEntity t, double power) {
		if (!cast.once("tackle_mend:" + t.getUUID())) {
			return;
		}
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack rod = t.getItemInHand(hand);
			if (rod.is(Items.FISHING_ROD) && rod.isDamaged()) {
				rod.setDamageValue(Math.max(0, rod.getDamageValue() - TideRules.mend(power)));
				emit(cast.level, ParticleTypes.HAPPY_VILLAGER, t.position().add(0, 1, 0), 8, 0.4);
				sound(cast.level, t.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F);
				return;
			}
		}
	}

	private static void bobberBell(Cast cast, int ticks) {
		if (!(cast.caster instanceof Player player)) {
			return;
		}
		emit(cast.level, ParticleTypes.NOTE, player.position().add(0, 2, 0), 1, 0);
		Fx.sound(cast.level, player.position(), SoundEvents.NOTE_BLOCK_BELL, 0.6F, 1.6F);
		boolean[] rung = {false};
		every(2, ticks / 2, cast::alive, () -> {
			FishingHook hook = player.fishing;
			int nibble = hook == null ? 0 : ((FishingHookAccessor) (Object) hook).wildercord$nibble();
			if (nibble > 0 && !rung[0]) {
				rung[0] = true;
				Fx.sound(cast.level, player.position(), SoundEvents.NOTE_BLOCK_BELL, 1.0F, 1.8F);
				emit(cast.level, ParticleTypes.SPLASH, hook.position(), 20, 0.3);
			} else if (nibble <= 0) {
				rung[0] = false;
			}
		});
	}

	private static void waterReading(Cast cast) {
		FishingHook hook = bobber(cast);
		if (hook == null) {
			return;
		}
		Casters.tell(cast.caster, Component.translatable(hook.isOpenWaterFishing() ? "message.wildercord.tide.open_water" : "message.wildercord.tide.closed_water"));
		Casters.tell(cast.caster, Component.translatable(cast.level.isRainingAt(hook.blockPosition().above()) ? "message.wildercord.tide.rain_helps" : "message.wildercord.tide.no_rain"));
		emit(cast.level, ParticleTypes.GLOW, hook.position(), 10, 0.5);
	}

	// ==================================================================== creatures

	private static boolean aquatic(Entity e) {
		return e instanceof WaterAnimal || e instanceof AgeableWaterCreature || e instanceof Axolotl || e instanceof Turtle;
	}

	private static <T extends Mob> List<T> near(Cast cast, Class<T> type, Vec3 at, double radius, java.util.function.Predicate<T> test, int cap) {
		List<T> found = new ArrayList<>(cast.level.getEntitiesOfClass(type, new AABB(at, at).inflate(radius),
			e -> e.isAlive() && e.distanceToSqr(at) <= radius * radius && test.test(e)));
		found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));
		int granted = cast.takeEntities(Math.min(cap, found.size()));
		return found.subList(0, Math.max(0, granted));
	}

	private static void glowAquatic(Cast cast, Vec3 at, double radius, int ticks) {
		List<Mob> seen = near(cast, Mob.class, at, radius, TideEffects::aquatic, 32);
		for (Mob m : seen) {
			m.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
			emit(cast.level, ParticleTypes.GLOW, m.position(), 4, 0.3);
		}
		Vfx.shockwave(cast.level, at, radius, Vfx.theme("arcane"), 12);
		if (seen.isEmpty()) nothing(cast);
	}

	private static void dolphinCall(Cast cast, int ticks) {
		LivingEntity caster = cast.caster;
		List<Dolphin> dolphins = near(cast, Dolphin.class, caster.position(), 24.0, d -> true, 8);
		dolphins.forEach(d -> d.getNavigation().moveTo(caster, 1.4));
		if (!dolphins.isEmpty()) {
			caster.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, ticks, 0, false, true));
		}
		emit(cast.level, ParticleTypes.DOLPHIN, caster.position(), 30, 1.0);
		sound(cast.level, caster.position(), SoundEvents.DOLPHIN_PLAY, 1.0F);
	}

	private static void axolotls(Cast cast, int ticks) {
		LivingEntity caster = cast.caster;
		List<Axolotl> found = near(cast, Axolotl.class, caster.position(), 12.0, a -> true, 8);
		for (Axolotl a : found) {
			a.heal(4.0F);
			emit(cast.level, ParticleTypes.HEART, a.position().add(0, 0.5, 0), 2, 0.3);
		}
		every(20, ticks / 20, cast::alive, () -> found.forEach(a -> {
			if (a.isAlive() && a.distanceTo(caster) > 2.5) a.getNavigation().moveTo(caster, 1.0);
		}));
		sound(cast.level, caster.position(), SoundEvents.AXOLOTL_IDLE_WATER, 1.0F);
		if (found.isEmpty()) nothing(cast);
	}

	private static void shoal(Cast cast, Vec3 at, double radius) {
		List<AbstractFish> fish = near(cast, AbstractFish.class, at, radius, f -> true, 16);
		fish.forEach(f -> f.getNavigation().moveTo(at.x, at.y, at.z, 1.2));
		every(40, 5, () -> true, () -> fish.forEach(f -> {
			if (f.isAlive()) f.getNavigation().moveTo(at.x, at.y, at.z, 1.2);
		}));
		emit(cast.level, ParticleTypes.BUBBLE, at, 20, 1.0);
		sound(cast.level, at, SoundEvents.PLAYER_SWIM, 1.2F);
		if (fish.isEmpty()) nothing(cast);
	}

	private static void refloat(Cast cast, Vec3 at) {
		List<Mob> stranded = near(cast, Mob.class, at, 6.0, m -> (m instanceof WaterAnimal || m instanceof AgeableWaterCreature) && !m.isInWater(), 8);
		int saved = 0;
		for (Mob m : stranded) {
			BlockPos water = nearestWater(cast.level, m.blockPosition(), 8);
			if (water != null) {
				emit(cast.level, ParticleTypes.SPLASH, m.position(), 10, 0.3);
				m.teleportTo(water.getX() + 0.5, water.getY() + 0.1, water.getZ() + 0.5);
				emit(cast.level, ParticleTypes.BUBBLE, m.position(), 10, 0.3);
				saved++;
			}
		}
		if (saved == 0) nothing(cast); else sound(cast.level, at, SoundEvents.PLAYER_SPLASH, 1.2F);
	}

	/** The nearest still water block within {@code reach} (a source with water or air over it), or null. */
	private static BlockPos nearestWater(ServerLevel level, BlockPos from, int reach) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos p : BlockPos.betweenClosed(from.offset(-reach, -reach, -reach), from.offset(reach, reach, reach))) {
			if (!level.isLoaded(p)) continue;
			BlockState s = level.getBlockState(p);
			if (s.is(Blocks.WATER) && level.getFluidState(p).isSource()) {
				double d = p.distSqr(from);
				if (d < bestDistance) {
					bestDistance = d;
					best = p.immutable();
				}
			}
		}
		return best;
	}

	// ==================================================================== blocks

	/** A block a spell may touch, before its budget: loaded, in the world, not held by another spell, and the caster may build there. */
	private static boolean editable(Cast cast, BlockPos p) {
		ServerLevel level = cast.level;
		return level.isLoaded(p) && !level.isOutsideBuildHeight(p) && level.getWorldBorder().isWithinBounds(p)
			&& !TemporaryBlocks.recorded(level, p) && Casters.mayBuild(cast.caster) && Casters.mayEdit(cast.caster, level, p) && cast.admitsBlock(p);
	}

	/** {@link #editable} and one block from the budget. */
	private static boolean take(Cast cast, BlockPos p) {
		return editable(cast, p) && cast.takeBlock();
	}

	private static List<BlockPos> around(Vec3 at, int reach) {
		BlockPos c = BlockPos.containing(at);
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-reach, -reach, -reach), c.offset(reach, reach, reach))) {
			if (p.distSqr(c) <= (reach + 0.5) * (reach + 0.5)) out.add(p.immutable());
		}
		out.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingDouble(p -> p.distSqr(c)));
		return out;
	}

	private static boolean evaporates(ServerLevel level, Vec3 at) {
		return level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, at);
	}

	private static void reedCut(Cast cast, Vec3 at) {
		ServerLevel level = cast.level;
		int cut = 0;
		for (BlockPos p : around(at, 3)) {
			if (cut >= TideRules.REED_MAX) break;
			BlockState s = level.getBlockState(p);
			BlockState below = level.getBlockState(p.below());
			boolean loose = s.is(Blocks.SEAGRASS) || s.is(Blocks.TALL_SEAGRASS) || s.is(Blocks.LILY_PAD);
			boolean stalk = (s.is(Blocks.KELP) || s.is(Blocks.KELP_PLANT)) && (below.is(Blocks.KELP_PLANT) || below.is(Blocks.KELP))
				|| s.is(Blocks.SUGAR_CANE) && below.is(Blocks.SUGAR_CANE);
			if ((loose || stalk) && take(cast, p)) {
				// Broken as by hand: the usual drops, and whatever stood on it comes down too.
				level.destroyBlock(p, true, cast.caster);
				emit(level, ParticleTypes.SPLASH, Vec3.atCenterOf(p), 4, 0.3);
				cut++;
			}
		}
		if (cut == 0) nothing(cast); else sound(level, at, SoundEvents.GRASS_BREAK, 1.2F);
	}

	private static void wring(Cast cast, Vec3 at) {
		ServerLevel level = cast.level;
		int soaked = 0;
		List<BlockPos> cells = around(at, 2);
		Collections.reverse(cells);
		for (BlockPos p : cells) {
			if (soaked >= TideRules.WRING_MAX) break;
			BlockState s = level.getBlockState(p);
			if (!level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)) continue;
			if (s.is(Blocks.WATER) && take(cast, p)) {
				level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
			} else if (s.hasProperty(BlockStateProperties.WATERLOGGED) && s.getValue(BlockStateProperties.WATERLOGGED) && take(cast, p)) {
				level.setBlockAndUpdate(p, s.setValue(BlockStateProperties.WATERLOGGED, false));
			} else {
				continue;
			}
			emit(level, ParticleTypes.SPLASH, Vec3.atCenterOf(p), 3, 0.3);
			soaked++;
		}
		if (soaked == 0) nothing(cast); else sound(level, at, SoundEvents.SPONGE_ABSORB, 1.0F);
	}

	private static void springDraw(Cast cast, Cast.Hit hit) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		if (evaporates(level, caster.position())) {
			Casters.tell(caster, Component.translatable("message.wildercord.tide.no_water"));
			return;
		}
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack held = caster.getItemInHand(hand);
			if (held.is(Items.BUCKET)) {
				if (held.getCount() == 1) {
					caster.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
				} else if (caster instanceof Player player && player.getInventory().add(new ItemStack(Items.WATER_BUCKET))) {
					held.shrink(1);
				} else {
					continue;
				}
				emit(level, ParticleTypes.SPLASH, caster.position().add(0, 1, 0), 10, 0.3);
				sound(level, caster.position(), SoundEvents.BUCKET_FILL, 1.0F);
				return;
			}
		}
		BlockPos p = hit.block() != null && hit.face() != null ? hit.block().relative(hit.face()) : BlockPos.containing(hit.point());
		if (evaporates(level, Vec3.atCenterOf(p)) || !level.getBlockState(p).isAir() || !take(cast, p)) {
			nothing(cast);
			return;
		}
		level.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
		emit(level, ParticleTypes.SPLASH, Vec3.atCenterOf(p), 10, 0.3);
		sound(level, Vec3.atCenterOf(p), SoundEvents.BUCKET_EMPTY, 1.0F);
	}

	private static void brimming(Cast cast, Vec3 at) {
		ServerLevel level = cast.level;
		int filled = 0;
		BlockState full = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL);
		for (BlockPos p : around(at, 4)) {
			if (filled >= TideRules.CAULDRON_MAX) break;
			BlockState s = level.getBlockState(p);
			boolean room = s.is(Blocks.CAULDRON) || s.is(Blocks.WATER_CAULDRON) && s.getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL;
			if (room && take(cast, p)) {
				level.setBlockAndUpdate(p, full);
				emit(level, ParticleTypes.SPLASH, Vec3.atCenterOf(p).add(0, 0.5, 0), 8, 0.3);
				filled++;
			}
		}
		if (filled == 0) nothing(cast); else sound(level, at, SoundEvents.BUCKET_EMPTY, 1.2F);
	}

	private static boolean water(ServerLevel level, BlockPos p) {
		return level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER);
	}

	private static void divingBell(Cast cast, Vec3 at, int ticks) {
		ServerLevel level = cast.level;
		BlockPos low = BlockPos.containing(at);
		BlockPos high = low.above();
		if (!level.getBlockState(low).is(Blocks.WATER) || !level.getBlockState(high).is(Blocks.WATER)) {
			Casters.tell(cast.caster, Component.translatable("message.wildercord.tide.not_in_water"));
			return;
		}
		List<BlockPos> walls = new ArrayList<>();
		for (BlockPos cell : List.of(low, high)) {
			for (Direction d : Direction.Plane.HORIZONTAL) walls.add(cell.relative(d));
		}
		walls.add(high.above());
		walls.add(low.below());
		List<BlockPos> glass = new ArrayList<>();
		for (BlockPos w : walls) {
			if (!water(level, w)) continue;
			// A waterlogged chest or the like stays as it is, and a bell that would leak there isn't raised.
			if (level.getBlockState(w).hasBlockEntity() || !editable(cast, w)) {
				nothing(cast);
				return;
			}
			glass.add(w);
		}
		if (!editable(cast, low) || !editable(cast, high) || !cast.takeBlocks(glass.size() + 2)) {
			nothing(cast);
			return;
		}
		long due = level.getGameTime() + ticks;
		BlockState pane = Blocks.STAINED_GLASS.lightBlue().defaultBlockState();
		for (BlockPos w : glass) {
			TemporaryBlocks.put(level, w, pane, level.getBlockState(w), due);
			level.setBlockAndUpdate(w, pane);
		}
		BlockState waterState = level.getBlockState(low);
		for (BlockPos cell : List.of(low, high)) {
			TemporaryBlocks.put(level, cell, Blocks.AIR.defaultBlockState(), waterState, due);
			level.setBlockAndUpdate(cell, Blocks.AIR.defaultBlockState());
		}
		emit(level, ParticleTypes.BUBBLE_POP, Vec3.atCenterOf(high), 30, 0.6);
		sound(level, Vec3.atCenterOf(high), SoundEvents.GLASS_PLACE, 0.9F);
	}

	private static void tideLantern(Cast cast, Cast.Hit hit, int ticks) {
		ServerLevel level = cast.level;
		BlockPos p = hit.block() != null && hit.face() != null ? hit.block().relative(hit.face()) : BlockPos.containing(hit.point());
		BlockState was = level.getBlockState(p);
		boolean wet = was.is(Blocks.WATER) && level.getFluidState(p).isSource();
		if (!wet && !was.isAir() || !take(cast, p)) {
			nothing(cast);
			return;
		}
		BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15).setValue(LightBlock.WATERLOGGED, wet);
		TemporaryBlocks.put(level, p, light, was, level.getGameTime() + ticks);
		level.setBlockAndUpdate(p, light);
		Vec3 c = Vec3.atCenterOf(p);
		every(20, ticks / 20, () -> level.getBlockState(p).equals(light), () -> emit(level, ParticleTypes.GLOW, c, 3, 0.25));
		emit(level, ParticleTypes.GLOW, c, 16, 0.4);
		sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F);
	}

	/** Puts out fire round {@code at}: fire blocks, lit campfires and burning creatures. The blocks put out, at most {@code cap}. */
	private static int sluice(Cast cast, Vec3 at, int reach, int cap) {
		ServerLevel level = cast.level;
		int out = 0;
		for (BlockPos p : around(at, reach)) {
			if (out >= cap) break;
			BlockState s = level.getBlockState(p);
			if (s.is(BlockTags.FIRE) && take(cast, p)) {
				level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
			} else if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT) && take(cast, p)) {
				level.setBlockAndUpdate(p, s.setValue(CampfireBlock.LIT, false));
			} else {
				continue;
			}
			emit(level, ParticleTypes.CLOUD, Vec3.atCenterOf(p), 4, 0.3);
			out++;
		}
		int doused = 0;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(reach), LivingEntity::isOnFire)) {
			e.clearFire();
			emit(level, ParticleTypes.SPLASH, e.position().add(0, 1, 0), 10, 0.4);
			doused++;
		}
		emit(level, ParticleTypes.FALLING_WATER, at.add(0, 2, 0), 30, reach * 0.5);
		if (out + doused > 0) sound(level, at, SoundEvents.FIRE_EXTINGUISH, 1.0F);
		return out + doused;
	}

	private static void soak(Cast cast, Vec3 at) {
		ServerLevel level = cast.level;
		int set = 0;
		for (BlockPos p : around(at, 2)) {
			if (set >= TideRules.SOAK_MAX) break;
			BlockState s = level.getBlockState(p);
			BlockState into = null;
			String concrete = TideRules.concreteFor(BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath());
			if (concrete != null) {
				Block b = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(concrete));
				if (b != Blocks.AIR) into = b.defaultBlockState();
			} else if (s.is(Blocks.DIRT)) {
				into = Blocks.MUD.defaultBlockState();
			}
			if (into != null && take(cast, p)) {
				level.setBlockAndUpdate(p, into);
				emit(level, ParticleTypes.DRIPPING_WATER, Vec3.atCenterOf(p).add(0, 0.6, 0), 4, 0.3);
				set++;
			}
		}
		if (set == 0) nothing(cast); else sound(level, at, SoundEvents.MUD_PLACE, 1.0F);
	}

	private static void rainCloud(Cast cast, Vec3 at, int ticks) {
		ServerLevel level = cast.level;
		if (evaporates(level, at)) {
			Casters.tell(cast.caster, Component.translatable("message.wildercord.tide.no_water"));
			return;
		}
		Vfx.rainCloud(level, at.add(0, 3, 0), 4.0, Vfx.theme("storm"), ticks);
		int grown = 0;
		boolean cauldron = false;
		int soaked = 0;
		for (BlockPos p : around(at, 4)) {
			BlockState s = level.getBlockState(p);
			if (s.getBlock() instanceof FarmlandBlock && s.getValue(FarmlandBlock.MOISTURE) < FarmlandBlock.MAX_MOISTURE && editable(cast, p)) {
				// Wetting farmland makes nothing and takes nothing: it costs no blocks.
				level.setBlockAndUpdate(p, s.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
				soaked++;
			} else if (grown < TideRules.CROP_MAX && s.getBlock() instanceof CropBlock crop && !crop.isMaxAge(s) && take(cast, p)) {
				level.setBlockAndUpdate(p, s.setValue(CropBlock.AGE, Math.min(crop.getMaxAge(), crop.getAge(s) + 1)));
				emit(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(p), 3, 0.3);
				grown++;
			} else if (!cauldron && (s.is(Blocks.CAULDRON) || s.is(Blocks.WATER_CAULDRON) && s.getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL)
					&& take(cast, p)) {
				int levelNow = s.is(Blocks.CAULDRON) ? 0 : s.getValue(LayeredCauldronBlock.LEVEL);
				level.setBlockAndUpdate(p, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, levelNow + 1));
				cauldron = true;
			}
		}
		int doused = sluice(cast, at, 4, 8);
		sound(level, at, SoundEvents.WEATHER_RAIN, 0.8F);
		if (grown + soaked + doused == 0 && !cauldron) nothing(cast);
	}

	private static void stormGlass(Cast cast) {
		var weather = cast.level.getWeatherData();
		int turn = TideRules.weatherTurn(weather.isRaining(), weather.getRainTime(), weather.getClearWeatherTime());
		Component said = turn > 0 ? Component.translatable("message.wildercord.tide.rain_ends", turn)
			: turn < 0 ? Component.translatable("message.wildercord.tide.rain_starts", -turn)
			: Component.translatable("message.wildercord.tide.no_weather");
		Casters.tell(cast.caster, said);
		emit(cast.level, ParticleTypes.CLOUD, cast.caster.getEyePosition().add(cast.caster.getLookAngle()), 6, 0.2);
		sound(cast.level, cast.caster.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F);
	}

	private static void kelpsong(Cast cast, Vec3 at) {
		ServerLevel level = cast.level;
		int grew = 0;
		List<BlockPos> cells = around(at, 4);
		for (BlockPos p : cells) {
			if (grew >= TideRules.KELP_MAX) break;
			BlockState s = level.getBlockState(p);
			BlockPos up = p.above();
			boolean roomAbove = level.getBlockState(up).is(Blocks.WATER) && level.getFluidState(up).isSource();
			if (s.is(Blocks.KELP) && roomAbove && editable(cast, p) && editable(cast, up) && cast.takeBlocks(2)) {
				level.setBlockAndUpdate(p, Blocks.KELP_PLANT.defaultBlockState());
				level.setBlockAndUpdate(up, Blocks.KELP.defaultBlockState());
			} else if (s.is(Blocks.SEAGRASS) && roomAbove && editable(cast, p) && editable(cast, up) && cast.takeBlocks(2)) {
				DoublePlantBlock.placeAt(level, Blocks.TALL_SEAGRASS.defaultBlockState(), p, Block.UPDATE_CLIENTS);
			} else if (s.is(Blocks.SEA_PICKLE) && s.getValue(SeaPickleBlock.WATERLOGGED) && s.getValue(SeaPickleBlock.PICKLES) < SeaPickleBlock.MAX_PICKLES
					&& take(cast, p)) {
				level.setBlockAndUpdate(p, s.setValue(SeaPickleBlock.PICKLES, s.getValue(SeaPickleBlock.PICKLES) + 1));
			} else {
				continue;
			}
			emit(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(p), 4, 0.3);
			grew++;
		}
		if (grew == 0) nothing(cast); else sound(level, at, SoundEvents.BONE_MEAL_USE, 1.0F);
	}

	private static void coralMend(Cast cast, Vec3 at) {
		ServerLevel level = cast.level;
		int mended = 0;
		for (BlockPos p : around(at, 3)) {
			if (mended >= TideRules.CORAL_MAX) break;
			BlockState s = level.getBlockState(p);
			String living = TideRules.livingCoral(BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath());
			if (living == null || !touchesWater(level, p, s)) continue;
			Block b = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(living));
			if (b == Blocks.AIR || !take(cast, p)) continue;
			level.setBlockAndUpdate(p, b.withPropertiesOf(s));
			emit(level, ParticleTypes.BUBBLE, Vec3.atCenterOf(p), 6, 0.3);
			mended++;
		}
		if (mended == 0) nothing(cast); else sound(level, at, SoundEvents.BONE_MEAL_USE, 1.3F);
	}

	private static boolean touchesWater(ServerLevel level, BlockPos p, BlockState s) {
		if (s.hasProperty(BlockStateProperties.WATERLOGGED) && s.getValue(BlockStateProperties.WATERLOGGED)) return true;
		for (Direction d : Direction.values()) {
			if (water(level, p.relative(d))) return true;
		}
		return false;
	}

	private static void nestTend(Cast cast, Vec3 at) {
		ServerLevel level = cast.level;
		int tended = 0;
		for (BlockPos p : around(at, 4)) {
			if (tended >= TideRules.NEST_MAX) break;
			BlockState s = level.getBlockState(p);
			if (s.is(Blocks.TURTLE_EGG) && s.getValue(TurtleEggBlock.HATCH) < TurtleEggBlock.MAX_HATCH_LEVEL && take(cast, p)) {
				level.setBlockAndUpdate(p, s.setValue(TurtleEggBlock.HATCH, TideRules.nextHatch(s.getValue(TurtleEggBlock.HATCH))));
				emit(level, ParticleTypes.EGG_CRACK, Vec3.atCenterOf(p), 4, 0.2);
				tended++;
			}
		}
		for (Turtle t : near(cast, Turtle.class, at, 4.0, Turtle::isBaby, 8)) {
			t.ageUp(120);
			emit(level, ParticleTypes.HAPPY_VILLAGER, t.position(), 4, 0.3);
			tended++;
		}
		if (tended == 0) nothing(cast); else sound(level, at, SoundEvents.TURTLE_EGG_CRACK, 1.2F);
	}

	/** Lily Path and Sandbar: a line of temporary blocks across the water the way the caster looks. */
	private static void surfacePath(Cast cast, BlockState block, boolean onTop, int count, int ticks) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		Vec3 look = caster.getLookAngle();
		Vec3 dir = new Vec3(look.x, 0, look.z);
		if (dir.lengthSqr() < 1.0E-4) dir = Vec3.directionFromRotation(0, caster.getYRot());
		dir = dir.normalize();
		long due = level.getGameTime() + ticks;
		int laid = 0;
		BlockPos last = null;
		for (double d = 1.0; d <= count + 4 && laid < count; d += 1.0) {
			Vec3 c = caster.position().add(dir.scale(d));
			BlockPos column = BlockPos.containing(c.x, caster.getY(), c.z);
			if (column.equals(last)) continue;
			last = column;
			BlockPos surface = null;
			for (int dy = 1; dy >= -2; dy--) {
				BlockPos p = column.offset(0, dy, 0);
				if (level.getBlockState(p).is(Blocks.WATER) && level.getFluidState(p).isSource() && level.getBlockState(p.above()).isAir()) {
					surface = p;
					break;
				}
			}
			if (surface == null) continue;
			BlockPos at = onTop ? surface.above() : surface;
			BlockState was = level.getBlockState(at);
			if (!take(cast, at)) break;
			TemporaryBlocks.put(level, at, block, was, due);
			level.setBlockAndUpdate(at, block);
			emit(level, ParticleTypes.SPLASH, Vec3.atCenterOf(at).add(0, 0.5, 0), 4, 0.3);
			laid++;
		}
		if (laid == 0) nothing(cast); else sound(level, caster.position(), onTop ? SoundEvents.LILY_PAD_PLACE : SoundEvents.SAND_PLACE, 1.0F);
	}

	private static void iceAuger(Cast cast, Cast.Hit hit) {
		ServerLevel level = cast.level;
		BlockPos p = hit.block() != null ? hit.block() : BlockPos.containing(hit.point());
		if (!level.getBlockState(p).is(Blocks.ICE)) p = p.below();
		boolean dry = evaporates(level, Vec3.atCenterOf(p));
		int bored = 0;
		while (bored < TideRules.AUGER_DEPTH && level.getBlockState(p).is(Blocks.ICE) && take(cast, p)) {
			// As ice melts when broken: water, or nothing where water can't be.
			level.setBlockAndUpdate(p, dry ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState());
			emit(level, ParticleTypes.SNOWFLAKE, Vec3.atCenterOf(p), 8, 0.4);
			bored++;
			p = p.below();
		}
		if (bored == 0) nothing(cast); else sound(level, hit.point(), SoundEvents.GLASS_BREAK, 1.3F);
	}

	private static void tideMarker(Cast cast, Vec3 at) {
		ServerLevel level = cast.level;
		Object token = new Object();
		MARKERS.put(cast.caster.getUUID(), token);
		UUID owner = cast.caster.getUUID();
		Vec3 buoy = at.add(0, 0.5, 0);
		every(10, 600, () -> MARKERS.get(owner) == token, () -> {
			emit(level, ParticleTypes.END_ROD, buoy.add(0, 0.6, 0), 1, 0.05);
			emit(level, ParticleTypes.GLOW, buoy, 2, 0.15);
		});
		Scheduler.later(6000, () -> MARKERS.remove(owner, token));
		Casters.tell(cast.caster, Component.translatable("message.wildercord.tide.marker",
			(int) Math.floor(at.x), (int) Math.floor(at.y), (int) Math.floor(at.z)));
		sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F);
	}

	private static void shoreSense(Cast cast) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		BlockPos from = caster.blockPosition();
		BlockPos found = null;
		for (int r = 2; r <= 48 && found == null; r += 2) {
			double best = Double.MAX_VALUE;
			for (int dx = -r; dx <= r; dx += 2) {
				for (int dz = -r; dz <= r; dz += 2) {
					if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
					int x = from.getX() + dx, z = from.getZ() + dz;
					if (!level.hasChunkAt(new BlockPos(x, from.getY(), z))) continue;
					int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
					BlockPos top = new BlockPos(x, y, z);
					if (level.getFluidState(top).isEmpty() && !level.getBlockState(top).isAir() && top.distSqr(from) < best) {
						best = top.distSqr(from);
						found = top.above();
					}
				}
			}
		}
		if (found == null) {
			Casters.tell(caster, Component.translatable("message.wildercord.tide.no_shore"));
			return;
		}
		Vfx.stream(level, caster.position().add(0, 1, 0), Vec3.atCenterOf(found), Vfx.theme("arcane"), 2);
		Casters.tell(caster, Component.translatable("message.wildercord.tide.shore", (int) Math.round(Math.sqrt(found.distSqr(from)))));
		sound(level, caster.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F);
	}

	private static void fathom(Cast cast, Vec3 at) {
		ServerLevel level = cast.level;
		BlockPos p = BlockPos.containing(at);
		if (!water(level, p) && water(level, p.below())) p = p.below();
		if (!water(level, p)) {
			Casters.tell(cast.caster, Component.translatable("message.wildercord.tide.no_water"));
			return;
		}
		BlockPos top = p;
		for (int i = 0; i < 256 && water(level, top.above()); i++) top = top.above();
		BlockPos bottom = p;
		for (int i = 0; i < 256 && water(level, bottom.below()); i++) bottom = bottom.below();
		int depth = top.getY() - bottom.getY() + 1;
		emit(level, ParticleTypes.GLOW, Vec3.atBottomCenterOf(bottom).add(0, 0.2, 0), 16, 0.4);
		Casters.tell(cast.caster, Component.translatable("message.wildercord.tide.depth", depth));
		sound(level, Vec3.atCenterOf(top), SoundEvents.FISHING_BOBBER_SPLASH, 0.7F);
	}

	private static void wreckSense(Cast cast, Vec3 at, int ticks) {
		ServerLevel level = cast.level;
		BlockPos c = BlockPos.containing(at);
		int reach = 20;
		List<BlockPos> found = new ArrayList<>();
		for (int cx = (c.getX() - reach) >> 4; cx <= (c.getX() + reach) >> 4; cx++) {
			for (int cz = (c.getZ() - reach) >> 4; cz <= (c.getZ() + reach) >> 4; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) continue;
				for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
					BlockPos p = e.getKey();
					if (p.distSqr(c) > reach * reach) continue;
					BlockEntity be = e.getValue();
					boolean sunken = (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity)
						&& (water(level, p) || water(level, p.above()));
					if (sunken || be instanceof BrushableBlockEntity) found.add(p.immutable());
				}
			}
		}
		found.sort(Comparator.comparingDouble(p -> p.distSqr(c)));
		List<BlockPos> shown = found.subList(0, Math.min(TideRules.WRECK_MAX, found.size()));
		every(10, ticks / 10, () -> true, () -> shown.forEach(p -> emit(level, ParticleTypes.GLOW, Vec3.atCenterOf(p).add(0, 0.7, 0), 2, 0.2)));
		shown.forEach(p -> Vfx.stream(level, at, Vec3.atCenterOf(p), Vfx.theme("arcane"), 1));
		Vfx.shockwave(level, at, reach, Vfx.theme("arcane"), 14);
		if (shown.isEmpty()) nothing(cast); else sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F);
	}

	private static void driftNet(Cast cast, Vec3 at) {
		LivingEntity caster = cast.caster;
		int moved = 0;
		for (ItemEntity item : cast.level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(6.0),
				i -> i.isAlive() && i.isInWater() && i.distanceToSqr(at) <= 36.0 && ownable(caster, i))) {
			if (moved >= TideRules.NET_MAX) break;
			Vfx.stream(cast.level, item.position(), caster.position().add(0, 1, 0), Vfx.theme("frost"), 1);
			item.teleportTo(caster.getX(), caster.getY() + 0.5, caster.getZ());
			item.setNoPickUpDelay();
			moved++;
		}
		if (moved == 0) nothing(cast); else sound(cast.level, caster.position(), SoundEvents.FISHING_BOBBER_RETRIEVE, 1.2F);
	}

	/** As Collect: another owner's drops, or an item reserved for someone else, stay where they are. */
	private static boolean ownable(LivingEntity caster, ItemEntity item) {
		if (item.hasPickUpDelay()) return false;
		var ownership = (ItemEntityAccessor) item;
		return (ownership.wildercord$target() == null || ownership.wildercord$target().equals(caster.getUUID()))
			&& (ownership.wildercord$thrower() == null || ownership.wildercord$thrower().getUUID().equals(caster.getUUID()));
	}

	private static void mooringCall(Cast cast) {
		LivingEntity caster = cast.caster;
		AbstractBoat boat = cast.level.getEntitiesOfClass(AbstractBoat.class, caster.getBoundingBox().inflate(24.0),
				b -> b.isAlive() && b.getPassengers().isEmpty() && b.distanceToSqr(caster) <= 24.0 * 24.0).stream()
			.min(Comparator.comparingDouble(b -> b.distanceToSqr(caster))).orElse(null);
		if (boat == null) {
			nothing(cast);
			return;
		}
		Vec3 look = caster.getLookAngle();
		Vec3 side = caster.position().add(new Vec3(look.x, 0, look.z).normalize().scale(1.5));
		emit(cast.level, ParticleTypes.SPLASH, boat.position(), 12, 0.5);
		boat.teleportTo(side.x, caster.getY() + 0.1, side.z);
		boat.setDeltaMovement(Vec3.ZERO);
		emit(cast.level, ParticleTypes.SPLASH, boat.position(), 12, 0.5);
		sound(cast.level, side, SoundEvents.BOAT_PADDLE_WATER, 1.0F);
	}

	// ==================================================================== movement

	private static void fairWind(Cast cast, int ticks) {
		LivingEntity caster = cast.caster;
		if (!(caster.getVehicle() instanceof AbstractBoat boat)) {
			Casters.tell(caster, Component.translatable("message.wildercord.tide.not_in_boat"));
			return;
		}
		every(1, ticks, () -> cast.alive() && caster.getVehicle() == boat, () -> {
			Vec3 look = caster.getLookAngle();
			Vec3 flat = new Vec3(look.x, 0, look.z);
			if (flat.lengthSqr() < 1.0E-4) return;
			flat = flat.normalize().scale(0.6);
			boat.setDeltaMovement(flat.x, boat.getDeltaMovement().y, flat.z);
			boat.needsSync = true;
			if (boat.tickCount % 4 == 0) emit(cast.level, ParticleTypes.CLOUD, boat.position().add(0, 1.5, 0), 2, 0.4);
		});
		sound(cast.level, caster.position(), SoundEvents.BOAT_PADDLE_WATER, 0.8F);
	}

	/** Upwell and Sounding: a push straight up or down while the caster is in water. */
	private static void swim(Cast cast, double vertical, int ticks) {
		LivingEntity caster = cast.caster;
		if (!caster.isInWater()) {
			Casters.tell(caster, Component.translatable("message.wildercord.tide.not_in_water"));
			return;
		}
		every(1, ticks, () -> cast.alive() && caster.isInWater(), () -> {
			Vec3 v = caster.getDeltaMovement();
			caster.setDeltaMovement(v.x * 0.6, vertical, v.z * 0.6);
			caster.needsSync = true;
			emit(cast.level, ParticleTypes.BUBBLE, caster.position(), 3, 0.3);
		});
		sound(cast.level, caster.position(), vertical > 0 ? SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE : SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.0F);
	}

	private static void porpoise(Cast cast) {
		LivingEntity caster = cast.caster;
		if (!caster.isInWater()) {
			Casters.tell(caster, Component.translatable("message.wildercord.tide.not_in_water"));
			return;
		}
		Vec3 look = caster.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
		caster.setDeltaMovement(flat.x * 1.2, 0.75, flat.z * 1.2);
		caster.needsSync = true;
		every(1, 60, cast::alive, caster::resetFallDistance);
		emit(cast.level, ParticleTypes.DOLPHIN, caster.position(), 20, 0.5);
		emit(cast.level, ParticleTypes.SPLASH, caster.position(), 20, 0.5);
		sound(cast.level, caster.position(), SoundEvents.PLAYER_SPLASH, 1.1F);
	}

	// ==================================================================== helpful

	private static void skimstep(Cast cast, LivingEntity t, int ticks) {
		emit(cast.level, ParticleTypes.SPLASH, t.position(), 12, 0.4);
		every(1, ticks, t::isAlive, () -> {
			if (t.isShiftKeyDown()) return;
			Vec3 v = t.getDeltaMovement();
			BlockPos feet = BlockPos.containing(t.getX(), t.getY() - 0.1, t.getZ());
			if (t.isInWater() && water(cast.level, feet.above())) {
				// Under: bob back up.
				t.setDeltaMovement(v.x, 0.3, v.z);
				t.needsSync = true;
			} else if (v.y < 0 && water(cast.level, feet) && !t.isInWater()) {
				// On top: the water holds.
				t.setDeltaMovement(v.x, 0.0, v.z);
				t.resetFallDistance();
				t.needsSync = true;
				if (t.tickCount % 3 == 0) emit(cast.level, ParticleTypes.SPLASH, t.position(), 2, 0.2);
			}
		});
		sound(cast.level, t.position(), SoundEvents.PLAYER_SPLASH, 1.4F);
	}

	private static void drownWard(Cast cast, LivingEntity t, int ticks) {
		int[] left = {TideRules.WARD_REFILLS};
		emit(cast.level, ParticleTypes.BUBBLE, t.getEyePosition(), 10, 0.3);
		every(10, ticks / 10, () -> t.isAlive() && left[0] > 0, () -> {
			if (TideRules.wardRefills(t.getAirSupply(), left[0])) {
				t.setAirSupply(t.getMaxAirSupply());
				left[0]--;
				emit(cast.level, ParticleTypes.BUBBLE_POP, t.getEyePosition(), 12, 0.3);
				sound(cast.level, t.position(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.4F);
			}
		});
	}

	private static void dewcatch(Cast cast) {
		if (!(cast.caster instanceof Player player)) {
			return;
		}
		if (evaporates(cast.level, player.position())) {
			Casters.tell(player, Component.translatable("message.wildercord.tide.no_water"));
			return;
		}
		Inventory inventory = player.getInventory();
		int filled = 0;
		for (int i = 0; i < inventory.getContainerSize() && filled < TideRules.BOTTLE_MAX; i++) {
			ItemStack stack = inventory.getItem(i);
			while (stack.is(Items.GLASS_BOTTLE) && filled < TideRules.BOTTLE_MAX) {
				ItemStack bottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
				if (stack.getCount() == 1) {
					inventory.setItem(i, bottle);
				} else {
					stack.shrink(1);
					if (!inventory.add(bottle)) {
						// No room for it: the empty bottle stays empty.
						stack.grow(1);
						break;
					}
				}
				filled++;
				stack = inventory.getItem(i);
			}
		}
		emit(cast.level, ParticleTypes.DRIPPING_WATER, player.position().add(0, 1.2, 0), 12, 0.4);
		if (filled == 0) nothing(cast); else sound(cast.level, player.position(), SoundEvents.BOTTLE_FILL, 1.0F);
	}

	private static void diversHands(LivingEntity t, int ticks) {
		AttributeInstance speed = t.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
		if (speed == null) return;
		// Vanilla's 0.2 underwater raised to the full 1.0.
		speed.addOrUpdateTransientModifier(new AttributeModifier(DIVERS_HANDS, 0.8, AttributeModifier.Operation.ADD_VALUE));
		long until = t.level().getGameTime() + ticks;
		DIVING.put(t.getUUID(), until);
		Scheduler.later(ticks, () -> {
			Long due = DIVING.get(t.getUUID());
			if (due == null || t.level().getGameTime() >= due) {
				DIVING.remove(t.getUUID());
				speed.removeModifier(DIVERS_HANDS);
			}
		});
		if (t.level() instanceof ServerLevel level) {
			emit(level, ParticleTypes.BUBBLE, t.position().add(0, 1, 0), 12, 0.4);
			sound(level, t.position(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.6F);
		}
	}

	// ==================================================================== small helpers

	private static int ticks(double seconds, double duration, boolean passive) {
		int ticks = (int) Math.round(seconds * 20 * duration);
		return passive ? dev.wildercord.spell.Passives.effectTicks(ticks) : ticks;
	}

	/** Runs {@code step} every {@code period} ticks, {@code times} times, while {@code keep} holds. */
	private static void every(int period, int times, BooleanSupplier keep, Runnable step) {
		if (times <= 0) return;
		Scheduler.later(Math.max(1, period), () -> {
			if (!keep.getAsBoolean()) return;
			step.run();
			every(period, times - 1, keep, step);
		});
	}

	private static void emit(ServerLevel level, ParticleOptions p, Vec3 at, int count, double spread) {
		Vfx.emit(level, p, at, count, spread, 0.02);
	}

	private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float pitch) {
		Fx.sound(level, at, sound, 0.8F, pitch);
	}

	private static void nothing(Cast cast) {
		Casters.tell(cast.caster, Component.translatable("message.wildercord.tide.nothing"));
	}
}
