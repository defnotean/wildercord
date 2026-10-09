package dev.wildercord.cast.packs;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Casters;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Targets;
import dev.wildercord.cast.Vfx;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The farmstead runes: tilling, watering, sowing and ripening a field; tending a herd; cooking from
 * the pack; keeping bees; and working a wood. Every block they change is offered to the claim mods
 * first and counts against the cast's block budget; nothing is made from nothing (seeds, saplings,
 * buckets and food come from the caster's own inventory), and a creature that belongs to another
 * player is never touched. The numbers are in {@link FarmRules}.
 */
public final class FarmEffects {
	private FarmEffects() {}

	/** When each hen was last hurried along (Henhouse), and each hive (Hive Hum), by game time. */
	private static final Map<UUID, Long> HENS = new ConcurrentHashMap<>();
	private static final Map<GlobalPos, Long> HIVES = new ConcurrentHashMap<>();

	private static final List<Block> FLOWERS = List.of(Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY,
		Blocks.AZURE_BLUET, Blocks.ALLIUM);

	public static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
		double power, double duration, int amplify) {
		double scale = SpellNumbers.effectRadius(node);
		switch (node.effect.path()) {
			case "tillage" -> furrow(cast, hit, scale);
			case "dewfall" -> wet(cast, target(hit), FarmRules.patch(scale), true);
			case "tilth" -> tilth(cast, hit, scale);
			case "plowline" -> plowline(cast, hit, power);
			case "sow" -> sow(cast, hit, scale);
			case "ripen" -> ripen(cast, hit, scale, FarmRules.ripenSteps(power));
			case "dewkeep" -> dewkeep(cast, hit, scale, duration);
			case "fieldsense" -> fieldsense(cast, hit, scale);
			case "thawfield" -> thawfield(cast, hit, scale);
			case "cloche" -> cloche(cast, hit, scale, duration);
			case "scarecrow" -> scarecrow(cast, hit, scale, duration);
			case "fallow" -> fallow(cast, hit, scale);
			case "ditchwater" -> ditchwater(cast, hit);
			case "compost" -> compost(cast, hit, power);
			case "stalkrise" -> stalkrise(cast, hit, scale);
			case "gourdcall" -> gourdcall(cast, hit, scale);
			case "berrybless" -> berrybless(cast, hit, scale);
			case "courtship" -> courtship(cast, hit, scale);
			case "herdcall" -> herdcall(cast, hit, scale);
			case "fleece" -> fleece(cast, hit, scale);
			case "milkmaid" -> milkmaid(cast, hit, scale);
			case "henhouse" -> henhouse(cast, hit, scale);
			case "gentlehand" -> gentlehand(cast, hit, scale, duration);
			case "fodder" -> fodder(cast, hit, scale);
			case "barnwarmth" -> barnwarmth(cast, hit, scale, power);
			case "herdsense" -> herdsense(cast, hit, scale, duration);
			case "hearthcook" -> hearthcook(cast, power);
			case "stewpot" -> stewpot(cast);
			case "bakehouse" -> bakehouse(cast, power);
			case "pollinate" -> pollinate(cast, hit, scale, power);
			case "hivehum" -> hivehum(cast, hit, scale);
			case "calmsmoke" -> calmsmoke(cast, hit, scale);
			case "wildflower" -> wildflower(cast, hit, scale);
			case "saplingrise" -> saplingrise(cast, hit, scale);
			case "saplingsow" -> saplingsow(cast, hit, scale);
			case "leaffall" -> leaffall(cast, hit, scale);
			case "barkstrip" -> barkstrip(cast, hit, scale);
			case "coppice" -> coppice(cast, hit);
			case "feastday" -> helped.forEach(t -> {
				if (t instanceof Player player) {
					player.getFoodData().eat(FarmRules.FEAST_HUNGER, 0.6F);
				}
				t.addEffect(new MobEffectInstance(MobEffects.SATURATION, FarmRules.ticks(6, duration), 0, false, true));
				t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, FarmRules.ticks(10, duration), 0, false, true));
				glow(cast.level, t, ParticleTypes.HAPPY_VILLAGER, 14);
				Fx.sound(cast.level, t.position(), SoundEvents.PLAYER_BURP, 0.7F, 1.1F);
			});
			case "picnic" -> helped.forEach(t -> {
				if (t instanceof Player player) {
					player.getFoodData().eat(FarmRules.PICNIC_HUNGER, 0.6F);
				}
				t.heal((float) (2 * power));
				glow(cast.level, t, ParticleTypes.HAPPY_VILLAGER, 8);
				Fx.sound(cast.level, t.position(), SoundEvents.GENERIC_EAT, 0.6F, 1.0F);
			});
			case "honeydew" -> helped.forEach(t -> {
				t.removeEffect(MobEffects.POISON);
				t.heal((float) (3 * power));
				glow(cast.level, t, ParticleTypes.FALLING_HONEY, 10);
				Fx.sound(cast.level, t.position(), SoundEvents.HONEY_DRINK, 0.7F, 1.0F);
			});
			case "leafshade" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FarmRules.ticks(15, duration), 0, false, true));
				glow(cast.level, t, ParticleTypes.COMPOSTER, 12);
				Fx.sound(cast.level, t.position(), SoundEvents.AZALEA_LEAVES_BREAK, 0.8F, 1.0F);
			});
			case "barkhide" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, FarmRules.ticks(12, duration), 0, false, true));
				glow(cast.level, t, ParticleTypes.COMPOSTER, 12);
				Fx.sound(cast.level, t.position(), SoundEvents.WOOD_BREAK, 0.8F, 0.8F);
			});
			case "sapflow" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, FarmRules.ticks(8, duration), 0, false, true));
				glow(cast.level, t, ParticleTypes.FALLING_NECTAR, 10);
				Fx.sound(cast.level, t.position(), SoundEvents.BONE_MEAL_USE, 0.6F, 1.2F);
			});
			case "trot" -> trot(cast, hit, helped, duration);
			case "beeline" -> beeline(cast, hit, power);
			case "fieldstride" -> movers(cast, hit, helped).forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SPEED, FarmRules.ticks(20, duration), 0, false, true));
				t.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, FarmRules.ticks(20, duration), 0, false, true));
				glow(cast.level, t, ParticleTypes.CLOUD, 6);
				Fx.sound(cast.level, t.position(), SoundEvents.GRASS_BREAK, 0.6F, 1.3F);
			});
			case "hayloft" -> movers(cast, hit, helped).forEach(t -> {
				Vec3 v = t.getDeltaMovement();
				move(t, new Vec3(v.x, FarmRules.tossSpeed(power), v.z));
				t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, FarmRules.ticks(3, 1.0) + 20, 0, false, true));
				t.resetFallDistance();
				Scheduler.later(60, t::resetFallDistance);
				Vfx.emit(cast.level, ParticleTypes.POOF, t.position(), 10, 0.4, 0.05);
				Fx.sound(cast.level, t.position(), SoundEvents.GRASS_PLACE, 0.8F, 0.8F);
			});
			default -> {
			}
		}
	}

	// ---- The field.

	private static void furrow(Cast cast, Cast.Hit hit, double scale) {
		int tilled = 0;
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 1)) {
			if (till(cast, p)) {
				tilled++;
			}
		}
		if (tilled > 0) {
			Vfx.emit(cast.level, ParticleTypes.COMPOSTER, Vec3.atCenterOf(target(hit)).add(0, 0.6, 0), 6 + tilled, 1.5, 0.02);
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.HOE_TILL, 1.0F, 1.0F);
		}
	}

	/** Tills one block as a hoe would: grass, dirt or a path with air above becomes farmland. */
	private static boolean till(Cast cast, BlockPos p) {
		BlockState state = cast.level.getBlockState(p);
		boolean tillable = state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.DIRT_PATH);
		if (!tillable || !cast.level.getBlockState(p.above()).isAir() || !mayChange(cast, p) || !cast.takeBlock()) {
			return false;
		}
		cast.level.setBlockAndUpdate(p, Blocks.FARMLAND.defaultBlockState());
		return true;
	}

	/** Wets the farmland of a patch to full moisture; the first wetting spends the cast's budget, a later one its own. */
	private static int wet(Cast cast, BlockPos center, int r, boolean first) {
		int wetted = 0;
		for (BlockPos p : patch(center, r, 1)) {
			BlockState state = cast.level.getBlockState(p);
			if (!state.is(Blocks.FARMLAND) || state.getValue(FarmlandBlock.MOISTURE) >= FarmlandBlock.MAX_MOISTURE || !mayChange(cast, p)) {
				continue;
			}
			if (first ? !cast.takeBlock() : wetted >= FarmRules.PULSE_BLOCKS) {
				break;
			}
			cast.level.setBlock(p, state.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
			wetted++;
		}
		Vec3 at = Vec3.atCenterOf(center).add(0, 1.5, 0);
		Vfx.emit(cast.level, ParticleTypes.FALLING_WATER, at, 10 + 2 * wetted, r, 0.0);
		if (first) {
			Fx.sound(cast.level, at, SoundEvents.WEATHER_RAIN, 0.4F, 1.4F);
		}
		return wetted;
	}

	private static void tilth(Cast cast, Cast.Hit hit, double scale) {
		int loosened = 0;
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 1)) {
			BlockState state = cast.level.getBlockState(p);
			boolean rooted = state.is(Blocks.ROOTED_DIRT);
			if (!(rooted || state.is(Blocks.COARSE_DIRT)) || !mayChange(cast, p) || !cast.takeBlock()) {
				continue;
			}
			cast.level.setBlockAndUpdate(p, Blocks.DIRT.defaultBlockState());
			if (rooted) {
				// A hoe on rooted dirt pops its hanging roots out of the face below it.
				Block.popResource(cast.level, p.below(), new ItemStack(Items.HANGING_ROOTS));
			}
			loosened++;
		}
		if (loosened > 0) {
			Vfx.emit(cast.level, ParticleTypes.COMPOSTER, Vec3.atCenterOf(target(hit)).add(0, 0.6, 0), 4 + loosened, 1.5, 0.02);
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 1.0F);
		}
	}

	private static void plowline(Cast cast, Cast.Hit hit, double power) {
		Direction facing = cast.caster.getDirection();
		BlockPos start = target(hit);
		int length = FarmRules.plowLength(power);
		int tilled = 0;
		BlockPos p = start;
		for (int i = 0; i < length; i++) {
			// The furrow follows the lie of the land a block up or down.
			BlockPos at = p;
			if (!tillableAt(cast, at)) {
				at = tillableAt(cast, p.above()) ? p.above() : tillableAt(cast, p.below()) ? p.below() : p;
			}
			if (till(cast, at)) {
				tilled++;
				BlockPos tilledAt = at;
				int delay = i;
				Scheduler.later(delay, () -> Vfx.emit(cast.level, ParticleTypes.COMPOSTER, Vec3.atCenterOf(tilledAt).add(0, 0.6, 0), 3, 0.3, 0.02));
			}
			p = at.relative(facing);
		}
		if (tilled > 0) {
			Fx.sound(cast.level, Vec3.atCenterOf(start), SoundEvents.HOE_TILL, 1.0F, 0.8F);
		}
	}

	private static boolean tillableAt(Cast cast, BlockPos p) {
		BlockState state = cast.level.getBlockState(p);
		return (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.DIRT_PATH)) && cast.level.getBlockState(p.above()).isAir();
	}

	private static void sow(Cast cast, Cast.Hit hit, double scale) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		int sown = 0;
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 1)) {
			if (!cast.level.getBlockState(p).is(Blocks.FARMLAND) || !cast.level.getBlockState(p.above()).isAir()) {
				continue;
			}
			BlockPos above = p.above();
			ItemStack seed = find(player, s -> s.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().is(BlockTags.CROPS)
				&& bi.getBlock().defaultBlockState().canSurvive(cast.level, above));
			if (seed.isEmpty()) {
				break;
			}
			if (!Casters.mayEdit(cast.caster, cast.level, above) || !cast.takeBlock()) {
				continue;
			}
			cast.level.setBlockAndUpdate(above, ((BlockItem) seed.getItem()).getBlock().defaultBlockState());
			use(player, seed, 1);
			sown++;
		}
		if (sown > 0) {
			Vfx.emit(cast.level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(target(hit)).add(0, 1, 0), 3 + sown, 1.5, 0.02);
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.CROP_PLANTED, 1.0F, 1.0F);
		}
	}

	private static void ripen(Cast cast, Cast.Hit hit, double scale, int steps) {
		int grown = 0;
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 1)) {
			if (ripenOne(cast, p, steps, true)) {
				grown++;
			}
		}
		if (grown > 0) {
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.BONE_MEAL_USE, 1.0F, 0.9F);
		}
	}

	/** Moves one plant {@code steps} stages on (never past ripe); the block budget is spent when {@code budget}. */
	private static boolean ripenOne(Cast cast, BlockPos p, int steps, boolean budget) {
		BlockState state = cast.level.getBlockState(p);
		BlockState aged = aged(state, steps);
		if (aged == null || !mayChange(cast, p) || budget && !cast.takeBlock()) {
			return false;
		}
		cast.level.setBlock(p, aged, Block.UPDATE_ALL);
		Vfx.emit(cast.level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(p), 4, 0.3, 0.02);
		return true;
	}

	/** The plant a stage or more on, or null when it isn't one Ripen knows or it is ripe already. */
	static BlockState aged(BlockState state, int steps) {
		Block block = state.getBlock();
		if (block instanceof CropBlock crop) {
			int age = crop.getAge(state);
			int next = FarmRules.nextAge(age, crop.getMaxAge(), steps);
			return next == age ? null : crop.getStateForAge(next);
		}
		IntegerProperty property = block instanceof StemBlock ? StemBlock.AGE
			: block instanceof SweetBerryBushBlock ? SweetBerryBushBlock.AGE
			: block instanceof NetherWartBlock ? NetherWartBlock.AGE
			: block instanceof CocoaBlock ? CocoaBlock.AGE : null;
		if (property == null || !state.hasProperty(property)) {
			return null;
		}
		int age = state.getValue(property);
		int max = property.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(age);
		int next = FarmRules.nextAge(age, max, steps);
		return next == age ? null : state.setValue(property, next);
	}

	private static void dewkeep(Cast cast, Cast.Hit hit, double scale, double duration) {
		BlockPos center = target(hit);
		int r = FarmRules.patch(scale);
		wet(cast, center, r, true);
		int pulses = FarmRules.pulses(FarmRules.DEWKEEP_SECONDS, duration, FarmRules.DEWKEEP_EVERY_TICKS);
		for (int i = 1; i < pulses; i++) {
			Scheduler.later(i * FarmRules.DEWKEEP_EVERY_TICKS, () -> {
				if (cast.alive() && cast.level.isLoaded(center)) {
					wet(cast, center, r, false);
				}
			});
		}
	}

	private static void fieldsense(Cast cast, Cast.Hit hit, double scale) {
		BlockPos center = target(hit);
		int r = FarmRules.reach(FarmRules.FIELDSENSE_REACH, scale);
		int ripe = 0;
		int growing = 0;
		for (BlockPos p : patch(center, r, 2)) {
			BlockState state = cast.level.getBlockState(p);
			if (!isPlant(state)) {
				continue;
			}
			if (aged(state, 1) == null) {
				ripe++;
				if (ripe <= 24) {
					Vfx.emit(cast.level, ParticleTypes.GLOW, Vec3.atCenterOf(p).add(0, 0.4, 0), 3, 0.2, 0.0);
				}
			} else {
				growing++;
			}
		}
		Fx.sound(cast.level, Vec3.atCenterOf(center), SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.4F);
		Casters.tell(cast.caster, Component.translatable("message.wildercord.fieldsense", ripe, growing));
	}

	/** A plant Field Sense, Cloche and Pollinate know: a crop, stem, berry bush, nether wart, cocoa or sapling. */
	private static boolean isPlant(BlockState state) {
		Block block = state.getBlock();
		return block instanceof CropBlock || block instanceof StemBlock || block instanceof SweetBerryBushBlock
			|| block instanceof NetherWartBlock || block instanceof CocoaBlock;
	}

	private static void thawfield(Cast cast, Cast.Hit hit, double scale) {
		int melted = 0;
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 2)) {
			if (!cast.level.getBlockState(p).is(Blocks.SNOW) || !Casters.mayEdit(cast.caster, cast.level, p) || !cast.takeBlock()) {
				continue;
			}
			cast.level.removeBlock(p, false);
			Vfx.emit(cast.level, ParticleTypes.CLOUD, Vec3.atCenterOf(p), 2, 0.3, 0.01);
			melted++;
		}
		if (melted > 0) {
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.6F);
		}
	}

	private static void cloche(Cast cast, Cast.Hit hit, double scale, double duration) {
		BlockPos center = target(hit);
		int r = FarmRules.patch(scale);
		int seconds = Math.max(1, FarmRules.ticks(FarmRules.CLOCHE_SECONDS, duration) / 20);
		Fx.sound(cast.level, Vec3.atCenterOf(center), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F);
		for (int i = 0; i < seconds; i++) {
			Scheduler.later(i * 20, () -> {
				if (!cast.alive() || !cast.level.isLoaded(center)) {
					return;
				}
				int ticked = 0;
				for (BlockPos p : patch(center, r, 1)) {
					BlockState state = cast.level.getBlockState(p);
					if (!(isPlant(state) || state.is(BlockTags.SAPLINGS)) || !state.isRandomlyTicking() || !mayChange(cast, p)) {
						continue;
					}
					if (ticked++ >= FarmRules.CLOCHE_PLANTS) {
						break;
					}
					state.randomTick(cast.level, p, cast.level.getRandom());
				}
				Vfx.emit(cast.level, ParticleTypes.END_ROD, Vec3.atCenterOf(center).add(0, 1.2, 0), 4, r * 0.6, 0.0);
			});
		}
	}

	private static void scarecrow(Cast cast, Cast.Hit hit, double scale, double duration) {
		Vec3 center = hit.point();
		double reach = FarmRules.reach(FarmRules.SCARE_REACH, scale);
		int seconds = Math.max(1, FarmRules.ticks(FarmRules.SCARECROW_SECONDS, duration) / 20);
		Fx.sound(cast.level, center, SoundEvents.WOOL_PLACE, 1.0F, 0.7F);
		for (int i = 0; i < seconds; i++) {
			Scheduler.later(i * 20, () -> {
				if (!cast.alive()) {
					return;
				}
				AABB box = new AABB(center, center).inflate(reach);
				for (Mob mob : cast.level.getEntitiesOfClass(Mob.class, box, m -> shooed(cast, m))) {
					Vec3 away = mob.position().subtract(center).multiply(1, 0, 1);
					Vec3 dir = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
					move(mob, dir.scale(0.7).add(0, 0.25, 0));
				}
				Vfx.emit(cast.level, ParticleTypes.CLOUD, center.add(0, 1.0, 0), 6, reach * 0.4, 0.02);
			});
		}
	}

	/** What a Scarecrow shoos: rabbits, foxes and monsters, never a pet, and never anything as big as a boss. */
	private static boolean shooed(Cast cast, Mob mob) {
		if (!mob.isAlive() || Targets.playerPet(mob) || Targets.isAlly(cast.caster, mob) || mob.getMaxHealth() > 100) {
			return false;
		}
		return mob instanceof Rabbit || mob instanceof Fox || mob instanceof Enemy;
	}

	private static void fallow(Cast cast, Cast.Hit hit, double scale) {
		int rested = 0;
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 1)) {
			if (!cast.level.getBlockState(p).is(Blocks.FARMLAND) || !cast.level.getBlockState(p.above()).isAir() || !mayChange(cast, p)
				|| !cast.takeBlock()) {
				continue;
			}
			cast.level.setBlockAndUpdate(p, Blocks.DIRT.defaultBlockState());
			rested++;
		}
		if (rested > 0) {
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.GRAVEL_BREAK, 0.8F, 1.0F);
		}
	}

	private static void ditchwater(Cast cast, Cast.Hit hit) {
		BlockPos pos = hit.block() != null && hit.face() != null ? hit.block().relative(hit.face()) : target(hit).above();
		ServerLevel level = cast.level;
		boolean hot = level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, Vec3.atCenterOf(pos));
		if (hot || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
			|| !nearFarmland(level, pos)) {
			return;
		}
		if (!Casters.mayEdit(cast.caster, level, pos) || !cast.takeBlock()) {
			return;
		}
		level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
		Vfx.emit(level, ParticleTypes.SPLASH, Vec3.atCenterOf(pos).add(0, 0.5, 0), 16, 0.4, 0.1);
		Fx.sound(level, Vec3.atCenterOf(pos), SoundEvents.BUCKET_EMPTY, 1.0F, 1.0F);
	}

	private static boolean nearFarmland(ServerLevel level, BlockPos pos) {
		int r = FarmRules.DITCH_FARMLAND_REACH;
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -1, -r), pos.offset(r, 1, r))) {
			if (level.getBlockState(p).is(Blocks.FARMLAND)) {
				return true;
			}
		}
		return false;
	}

	private static void compost(Cast cast, Cast.Hit hit, double power) {
		if (!(cast.caster instanceof ServerPlayer player) || hit.block() == null) {
			return;
		}
		BlockPos pos = hit.block();
		BlockState state = cast.level.getBlockState(pos);
		if (!state.is(Blocks.COMPOSTER) || !mayChange(cast, pos)) {
			return;
		}
		int fed = 0;
		int limit = FarmRules.batch(power);
		List<ItemStack> items = player.getInventory().getNonEquipmentItems();
		for (ItemStack stack : items) {
			while (fed < limit && !stack.isEmpty() && state.getValue(ComposterBlock.LEVEL) < ComposterBlock.MAX_LEVEL - 1) {
				int before = stack.getCount();
				// The composter takes the item itself (one off the stack), as a dropped-in one would be.
				state = ComposterBlock.insertItem(player, state, cast.level, stack, pos);
				if (stack.getCount() == before) {
					break;
				}
				fed++;
			}
		}
		if (fed > 0) {
			Vfx.emit(cast.level, ParticleTypes.COMPOSTER, Vec3.atCenterOf(pos).add(0, 0.6, 0), 6 + fed, 0.3, 0.02);
			Fx.sound(cast.level, Vec3.atCenterOf(pos), SoundEvents.COMPOSTER_FILL, 1.0F, 1.0F);
		}
	}

	private static void stalkrise(Cast cast, Cast.Hit hit, double scale) {
		int grown = 0;
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 2)) {
			BlockState state = cast.level.getBlockState(p);
			BlockPos above = p.above();
			if (state.getBlock() instanceof BambooStalkBlock) {
				if (!cast.level.getBlockState(above).is(state.getBlock()) && mayChange(cast, p) && Casters.mayEdit(cast.caster, cast.level, above)
					&& cast.takeBlock() && BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), cast.level, p)) {
					grown++;
				}
				continue;
			}
			boolean stalk = state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS);
			if (!stalk || !cast.level.getBlockState(above).isAir()) {
				continue;
			}
			int height = 1;
			while (height <= FarmRules.STALK_MAX && cast.level.getBlockState(p.below(height)).is(state.getBlock())) {
				height++;
			}
			BlockState grownState = state.getBlock().defaultBlockState();
			if (!FarmRules.mayRise(height) || !grownState.canSurvive(cast.level, above) || !Casters.mayEdit(cast.caster, cast.level, above)
				|| !cast.takeBlock()) {
				continue;
			}
			cast.level.setBlockAndUpdate(above, grownState);
			Vfx.emit(cast.level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(above), 4, 0.3, 0.02);
			grown++;
		}
		if (grown > 0) {
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.BONE_MEAL_USE, 1.0F, 1.1F);
		}
	}

	private static void gourdcall(Cast cast, Cast.Hit hit, double scale) {
		int set = 0;
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 1)) {
			BlockState state = cast.level.getBlockState(p);
			Block fruit = state.is(Blocks.PUMPKIN_STEM) ? Blocks.PUMPKIN : state.is(Blocks.MELON_STEM) ? Blocks.MELON : null;
			Block attached = state.is(Blocks.PUMPKIN_STEM) ? Blocks.ATTACHED_PUMPKIN_STEM : Blocks.ATTACHED_MELON_STEM;
			if (fruit == null || state.getValue(StemBlock.AGE) < StemBlock.MAX_AGE || !mayChange(cast, p)) {
				continue;
			}
			for (Direction side : Direction.Plane.HORIZONTAL.shuffledCopy(cast.level.getRandom())) {
				BlockPos spot = p.relative(side);
				BlockState ground = cast.level.getBlockState(spot.below());
				if (!cast.level.getBlockState(spot).isAir() || !(ground.is(BlockTags.DIRT) || ground.is(Blocks.FARMLAND))) {
					continue;
				}
				if (!Casters.mayEdit(cast.caster, cast.level, spot) || !cast.takeBlock()) {
					break;
				}
				cast.level.setBlockAndUpdate(spot, fruit.defaultBlockState());
				cast.level.setBlockAndUpdate(p, attached.defaultBlockState().setValue(AttachedStemBlock.FACING, side));
				Vfx.emit(cast.level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(spot), 8, 0.4, 0.02);
				set++;
				break;
			}
		}
		if (set > 0) {
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.CROP_PLANTED, 1.0F, 0.7F);
		}
	}

	private static void berrybless(Cast cast, Cast.Hit hit, double scale) {
		int blessed = 0;
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 2)) {
			BlockState state = cast.level.getBlockState(p);
			BlockState ripe = null;
			if (state.getBlock() instanceof SweetBerryBushBlock && state.getValue(SweetBerryBushBlock.AGE) < SweetBerryBushBlock.MAX_AGE) {
				ripe = state.setValue(SweetBerryBushBlock.AGE, SweetBerryBushBlock.MAX_AGE);
			} else if (state.hasProperty(CaveVines.BERRIES) && !state.getValue(CaveVines.BERRIES)) {
				ripe = state.setValue(CaveVines.BERRIES, true);
			}
			if (ripe == null || !mayChange(cast, p) || !cast.takeBlock()) {
				continue;
			}
			cast.level.setBlock(p, ripe, Block.UPDATE_ALL);
			Vfx.emit(cast.level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(p), 4, 0.3, 0.02);
			blessed++;
		}
		if (blessed > 0) {
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 0.8F, 1.2F);
		}
	}

	// ---- The herd.

	private static void courtship(Cast cast, Cast.Hit hit, double scale) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		int paired = 0;
		for (Animal animal : herd(cast, hit.point(), FarmRules.reach(FarmRules.HERD_REACH, scale))) {
			if (paired >= FarmRules.COURTSHIP_MAX) {
				break;
			}
			if (animal.isBaby() || animal.getAge() != 0 || !animal.canFallInLove()) {
				continue;
			}
			animal.setInLove(player);
			paired++;
		}
		if (paired > 0) {
			Vfx.emit(cast.level, ParticleTypes.HEART, hit.point().add(0, 1, 0), paired * 2, 1.5, 0.02);
			Fx.sound(cast.level, hit.point(), SoundEvents.NOTE_BLOCK_BELL.value(), 0.6F, 1.6F);
		}
	}

	private static void herdcall(Cast cast, Cast.Hit hit, double scale) {
		Vec3 to = hit.self() ? cast.caster.position() : hit.point();
		Vec3 from = hit.self() ? cast.caster.position() : hit.point();
		int called = 0;
		for (Animal animal : herd(cast, from, FarmRules.reach(FarmRules.HERDCALL_REACH, scale))) {
			if (called >= FarmRules.HERDCALL_MAX) {
				break;
			}
			if (animal.isLeashed() || animal.isPassenger()) {
				continue;
			}
			animal.getNavigation().moveTo(to.x, to.y, to.z, 1.2);
			Vfx.emit(cast.level, ParticleTypes.NOTE, animal.position().add(0, animal.getBbHeight() + 0.3, 0), 1, 0.1, 0.0);
			called++;
		}
		Fx.sound(cast.level, to, SoundEvents.NOTE_BLOCK_BELL.value(), 0.8F, 1.0F);
	}

	private static void fleece(Cast cast, Cast.Hit hit, double scale) {
		int shorn = 0;
		for (Animal animal : herd(cast, hit.point(), FarmRules.reach(FarmRules.HERD_REACH, scale))) {
			if (shorn >= FarmRules.HERD_MAX) {
				break;
			}
			if (animal instanceof Sheep sheep && sheep.readyForShearing()) {
				sheep.shear(cast.level, SoundSource.PLAYERS, new ItemStack(Items.SHEARS));
				shorn++;
			}
		}
		if (shorn > 0) {
			Vfx.emit(cast.level, ParticleTypes.CLOUD, hit.point().add(0, 1, 0), 4 + shorn, 1.0, 0.02);
		}
	}

	private static void milkmaid(Cast cast, Cast.Hit hit, double scale) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		int milked = 0;
		for (Animal animal : herd(cast, hit.point(), FarmRules.reach(FarmRules.HERD_REACH, scale))) {
			if (milked >= FarmRules.HERD_MAX) {
				break;
			}
			if (animal.isBaby() || !(animal instanceof AbstractCow || animal instanceof Goat)) {
				continue;
			}
			ItemStack bucket = find(player, s -> s.is(Items.BUCKET));
			if (bucket.isEmpty()) {
				break;
			}
			use(player, bucket, 1);
			give(player, new ItemStack(Items.MILK_BUCKET));
			Vfx.emit(cast.level, ParticleTypes.SNOWFLAKE, animal.position().add(0, 0.8, 0), 4, 0.3, 0.01);
			Fx.sound(cast.level, animal.position(), SoundEvents.COW_MILK, 0.8F, 1.0F);
			milked++;
		}
	}

	private static void henhouse(Cast cast, Cast.Hit hit, double scale) {
		long now = cast.level.getGameTime();
		int hurried = 0;
		for (Animal animal : herd(cast, hit.point(), FarmRules.reach(FarmRules.HERD_REACH, scale))) {
			if (hurried >= FarmRules.HERD_MAX) {
				break;
			}
			if (!(animal instanceof Chicken hen) || hen.isBaby() || !FarmRules.ready(HENS.get(hen.getUUID()), now, FarmRules.HEN_COOLDOWN_TICKS)) {
				continue;
			}
			HENS.put(hen.getUUID(), now);
			// The hen lays on her own next tick or so, by her own rules (a chicken jockey's or a baby's never).
			hen.eggTime = Math.min(hen.eggTime, 2 + cast.level.getRandom().nextInt(10));
			Vfx.emit(cast.level, ParticleTypes.END_ROD, hen.position().add(0, 0.6, 0), 3, 0.2, 0.01);
			hurried++;
		}
		HENS.values().removeIf(last -> now - last > FarmRules.HEN_COOLDOWN_TICKS);
	}

	private static void gentlehand(Cast cast, Cast.Hit hit, double scale, double duration) {
		for (Animal animal : herd(cast, hit.point(), FarmRules.reach(FarmRules.HERD_REACH, scale))) {
			// A panicking animal runs from what last hurt it: forgetting that, and a slow, calm step, settles it.
			animal.setLastHurtByMob(null);
			animal.getNavigation().stop();
			animal.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FarmRules.ticks(8, duration), 1, false, false));
			Vfx.emit(cast.level, ParticleTypes.NOTE, animal.position().add(0, animal.getBbHeight() + 0.3, 0), 1, 0.1, 0.0);
		}
		Fx.sound(cast.level, hit.point(), SoundEvents.NOTE_BLOCK_BELL.value(), 0.5F, 0.6F);
	}

	private static void fodder(Cast cast, Cast.Hit hit, double scale) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		int fed = 0;
		for (Animal animal : herd(cast, hit.point(), FarmRules.reach(FarmRules.HERD_REACH, scale))) {
			if (fed >= FarmRules.HERD_MAX) {
				break;
			}
			ItemStack food = find(player, animal::isFood);
			if (food.isEmpty()) {
				continue;
			}
			use(player, food, 1);
			animal.heal(4);
			if (animal.isBaby()) {
				animal.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-animal.getAge()), true);
			}
			Vfx.emit(cast.level, ParticleTypes.HAPPY_VILLAGER, animal.position().add(0, 0.8, 0), 4, 0.3, 0.02);
			fed++;
		}
		if (fed > 0) {
			Fx.sound(cast.level, hit.point(), SoundEvents.GENERIC_EAT, 0.8F, 1.0F);
		}
	}

	private static void barnwarmth(Cast cast, Cast.Hit hit, double scale, double power) {
		int warmed = 0;
		for (Animal animal : herd(cast, hit.point(), FarmRules.reach(FarmRules.HERD_REACH, scale))) {
			if (warmed >= FarmRules.HERD_MAX * 2) {
				break;
			}
			animal.heal((float) (4 * power));
			Vfx.emit(cast.level, ParticleTypes.HEART, animal.position().add(0, animal.getBbHeight(), 0), 1, 0.2, 0.0);
			warmed++;
		}
		Vfx.emit(cast.level, ParticleTypes.CAMPFIRE_COSY_SMOKE, hit.point(), 3, 0.5, 0.01);
		Fx.sound(cast.level, hit.point(), SoundEvents.CAMPFIRE_CRACKLE, 0.8F, 1.0F);
	}

	private static void herdsense(Cast cast, Cast.Hit hit, double scale, double duration) {
		Vec3 from = hit.self() ? cast.caster.position() : hit.point();
		int count = 0;
		int ready = 0;
		AABB box = new AABB(from, from).inflate(FarmRules.reach(FarmRules.HERDSENSE_REACH, scale));
		for (Animal animal : cast.level.getEntitiesOfClass(Animal.class, box, a -> a.isAlive() && !(Targets.playerPet(a) && !Targets.isAlly(cast.caster, a)))) {
			if (count >= Cast.MAX_ENTITIES) {
				break;
			}
			animal.addEffect(new MobEffectInstance(MobEffects.GLOWING, FarmRules.ticks(10, duration), 0, false, false));
			count++;
			if (!animal.isBaby() && animal.getAge() == 0 && animal.canFallInLove()) {
				ready++;
			}
		}
		Fx.sound(cast.level, from, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.2F);
		Casters.tell(cast.caster, Component.translatable("message.wildercord.herdsense", count, ready));
	}

	/**
	 * The animals a husbandry rune may tend around {@code at}: alive, not another player's pet, and
	 * standing where the caster may interact and claim mods agree (asked as for the block they stand in).
	 */
	private static List<Animal> herd(Cast cast, Vec3 at, int reach) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return List.of();
		}
		AABB box = new AABB(at, at).inflate(reach);
		List<Animal> found = cast.level.getEntitiesOfClass(Animal.class, box,
			a -> a.isAlive() && !(Targets.playerPet(a) && !Targets.isAlly(player, a)) && mayTend(cast, player, a));
		found.sort(java.util.Comparator.comparingDouble(a -> a.distanceToSqr(at)));
		return found.size() > Cast.MAX_ENTITIES ? found.subList(0, Cast.MAX_ENTITIES) : found;
	}

	private static boolean mayTend(Cast cast, ServerPlayer player, Entity entity) {
		BlockPos pos = entity.blockPosition();
		return cast.level.mayInteract(player, pos) && Casters.probeBreak(cast.level, player, pos, cast.level.getBlockState(pos), null);
	}

	// ---- The kitchen.

	private static void hearthcook(Cast cast, double power) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		int limit = FarmRules.batch(power);
		int cooked = 0;
		List<ItemStack> items = player.getInventory().getNonEquipmentItems();
		List<ItemStack> made = new ArrayList<>();
		for (ItemStack stack : items) {
			while (cooked < limit && !stack.isEmpty()) {
				SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
				var recipe = cast.level.recipeAccess().getRecipeFor(RecipeType.SMOKING, input, cast.level);
				if (recipe.isEmpty()) {
					break;
				}
				ItemStack out = recipe.get().value().assemble(input);
				if (out.isEmpty()) {
					break;
				}
				stack.shrink(1);
				made.add(out);
				cooked++;
			}
		}
		made.forEach(out -> give(player, out));
		if (cooked > 0) {
			Vfx.emit(cast.level, ParticleTypes.CAMPFIRE_COSY_SMOKE, player.position().add(0, 1.2, 0), 4, 0.3, 0.02);
			Vfx.emit(cast.level, ParticleTypes.FLAME, player.position().add(0, 1.0, 0), 6, 0.3, 0.01);
			Fx.sound(cast.level, player.position(), SoundEvents.SMOKER_SMOKE, 1.0F, 1.0F);
		}
	}

	private static void stewpot(Cast cast) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		int stews = FarmRules.stews(count(player, Items.RED_MUSHROOM), count(player, Items.BROWN_MUSHROOM), count(player, Items.BOWL), FarmRules.STEW_MAX);
		take(player, Items.RED_MUSHROOM, stews);
		take(player, Items.BROWN_MUSHROOM, stews);
		take(player, Items.BOWL, stews);
		int soups = FarmRules.soups(count(player, Items.BEETROOT), count(player, Items.BOWL), FarmRules.STEW_MAX - stews);
		take(player, Items.BEETROOT, soups * 6);
		take(player, Items.BOWL, soups);
		for (int i = 0; i < stews; i++) {
			give(player, new ItemStack(Items.MUSHROOM_STEW));
		}
		for (int i = 0; i < soups; i++) {
			give(player, new ItemStack(Items.BEETROOT_SOUP));
		}
		if (stews + soups > 0) {
			Vfx.emit(cast.level, ParticleTypes.CAMPFIRE_COSY_SMOKE, player.position().add(0, 1.2, 0), 3, 0.3, 0.02);
			Fx.sound(cast.level, player.position(), SoundEvents.BREWING_STAND_BREW, 0.7F, 0.8F);
		}
	}

	private static void bakehouse(Cast cast, double power) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		int budget = FarmRules.bakes(power);
		int loaves = FarmRules.loaves(count(player, Items.WHEAT), budget);
		take(player, Items.WHEAT, loaves * 3);
		int pies = FarmRules.pies(count(player, Items.PUMPKIN), count(player, Items.SUGAR), count(player, Items.EGG), budget - loaves);
		take(player, Items.PUMPKIN, pies);
		take(player, Items.SUGAR, pies);
		take(player, Items.EGG, pies);
		if (loaves > 0) {
			give(player, new ItemStack(Items.BREAD, loaves));
		}
		if (pies > 0) {
			give(player, new ItemStack(Items.PUMPKIN_PIE, pies));
		}
		if (loaves + pies > 0) {
			Vfx.emit(cast.level, ParticleTypes.FLAME, player.position().add(0, 1.0, 0), 6, 0.3, 0.01);
			Fx.sound(cast.level, player.position(), SoundEvents.CAMPFIRE_CRACKLE, 1.0F, 1.2F);
		}
	}

	// ---- The hive.

	private static void pollinate(Cast cast, Cast.Hit hit, double scale, double power) {
		Vec3 at = hit.point();
		List<Bee> bees = cast.level.getEntitiesOfClass(Bee.class, new AABB(at, at).inflate(FarmRules.BEE_REACH), Bee::isAlive);
		int stages = FarmRules.pollinateStages(bees.size(), power);
		if (stages == 0) {
			return;
		}
		int given = 0;
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 1)) {
			if (given >= stages) {
				break;
			}
			if (ripenOne(cast, p, 1, true)) {
				given++;
			}
		}
		for (Bee bee : bees.subList(0, Math.min(6, bees.size()))) {
			Vfx.stream(cast.level, bee.position(), at, Vfx.theme("wind"), 6);
		}
		Vfx.emit(cast.level, ParticleTypes.FALLING_NECTAR, at.add(0, 1.0, 0), 6 + given * 2, 1.5, 0.0);
		Fx.sound(cast.level, at, SoundEvents.BEE_POLLINATE, 1.0F, 1.0F);
	}

	private static void hivehum(Cast cast, Cast.Hit hit, double scale) {
		long now = cast.level.getGameTime();
		int hummed = 0;
		for (BlockPos p : patch(target(hit), FarmRules.reach(FarmRules.HERD_REACH, scale), 2)) {
			BlockState state = cast.level.getBlockState(p);
			if (!state.is(BlockTags.BEEHIVES) || !state.hasProperty(BeehiveBlock.HONEY_LEVEL)
				|| state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS) {
				continue;
			}
			GlobalPos hive = GlobalPos.of(cast.level.dimension(), p.immutable());
			if (!FarmRules.ready(HIVES.get(hive), now, FarmRules.HIVE_COOLDOWN_TICKS) || !mayChange(cast, p) || !cast.takeBlock()) {
				continue;
			}
			HIVES.put(hive, now);
			cast.level.setBlock(p, state.setValue(BeehiveBlock.HONEY_LEVEL, state.getValue(BeehiveBlock.HONEY_LEVEL) + 1), Block.UPDATE_ALL);
			Vfx.emit(cast.level, ParticleTypes.FALLING_HONEY, Vec3.atCenterOf(p).add(0, 0.6, 0), 6, 0.4, 0.0);
			hummed++;
		}
		HIVES.values().removeIf(last -> now - last > FarmRules.HIVE_COOLDOWN_TICKS);
		if (hummed > 0) {
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.BEEHIVE_WORK, 1.0F, 1.0F);
		}
	}

	private static void calmsmoke(Cast cast, Cast.Hit hit, double scale) {
		Vec3 at = hit.point();
		AABB box = new AABB(at, at).inflate(FarmRules.reach(FarmRules.CALM_REACH, scale));
		for (Bee bee : cast.level.getEntitiesOfClass(Bee.class, box, Bee::isAlive)) {
			if (bee.isAngry()) {
				bee.stopBeingAngry();
			}
			bee.setLastHurtByMob(null);
		}
		Vfx.emit(cast.level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at.add(0, 0.5, 0), 6, 1.0, 0.01);
		Fx.sound(cast.level, at, SoundEvents.CAMPFIRE_CRACKLE, 1.0F, 0.8F);
	}

	// ---- The wood.

	private static void wildflower(Cast cast, Cast.Hit hit, double scale) {
		int max = FarmRules.WILDFLOWER_MAX * Math.max(1, (int) Math.round(scale));
		int sown = 0;
		List<BlockPos> spots = new ArrayList<>();
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 1)) {
			if (cast.level.getBlockState(p).is(Blocks.GRASS_BLOCK) && cast.level.getBlockState(p.above()).isAir()) {
				spots.add(p.above());
			}
		}
		java.util.Collections.shuffle(spots, new java.util.Random(cast.level.getRandom().nextLong()));
		for (BlockPos spot : spots) {
			if (sown >= max) {
				break;
			}
			BlockState flower = FLOWERS.get(cast.level.getRandom().nextInt(FLOWERS.size())).defaultBlockState();
			if (!flower.canSurvive(cast.level, spot) || !Casters.mayEdit(cast.caster, cast.level, spot) || !cast.takeBlock()) {
				continue;
			}
			cast.level.setBlockAndUpdate(spot, flower);
			Vfx.emit(cast.level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(spot), 3, 0.3, 0.02);
			sown++;
		}
		if (sown > 0) {
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.GRASS_PLACE, 1.0F, 1.2F);
		}
	}

	private static void saplingrise(Cast cast, Cast.Hit hit, double scale) {
		int urged = 0;
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 1)) {
			if (urged >= FarmRules.SAPLINGRISE_MAX) {
				break;
			}
			BlockState state = cast.level.getBlockState(p);
			boolean young = state.is(BlockTags.SAPLINGS) || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM);
			if (!young || !Casters.mayEdit(cast.caster, cast.level, p) || !cast.takeBlock()) {
				continue;
			}
			for (int i = 0; i < FarmRules.SAPLINGRISE_MEALS && cast.level.getBlockState(p).is(state.getBlock()); i++) {
				BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), cast.level, p);
			}
			cast.level.levelEvent(null, 1505, p, 15);
			urged++;
		}
		if (urged > 0) {
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.BONE_MEAL_USE, 1.0F, 0.7F);
		}
	}

	private static void saplingsow(Cast cast, Cast.Hit hit, double scale) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		List<BlockPos> planted = new ArrayList<>();
		for (BlockPos p : patch(target(hit), FarmRules.patch(scale), 1)) {
			if (planted.size() >= FarmRules.SAPLINGSOW_MAX) {
				break;
			}
			BlockPos spot = p.above();
			if (!cast.level.getBlockState(p).is(BlockTags.DIRT) || !cast.level.getBlockState(spot).isAir()
				|| planted.stream().anyMatch(o -> !FarmRules.spaced(o.getX() - spot.getX(), o.getZ() - spot.getZ()))) {
				continue;
			}
			ItemStack sapling = find(player, s -> s.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().is(BlockTags.SAPLINGS)
				&& bi.getBlock().defaultBlockState().canSurvive(cast.level, spot));
			if (sapling.isEmpty()) {
				break;
			}
			if (!Casters.mayEdit(cast.caster, cast.level, spot) || !cast.takeBlock()) {
				continue;
			}
			cast.level.setBlockAndUpdate(spot, ((BlockItem) sapling.getItem()).getBlock().defaultBlockState());
			use(player, sapling, 1);
			planted.add(spot.immutable());
			Vfx.emit(cast.level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(spot), 4, 0.3, 0.02);
		}
		if (!planted.isEmpty()) {
			Fx.sound(cast.level, Vec3.atCenterOf(target(hit)), SoundEvents.CROP_PLANTED, 1.0F, 0.8F);
		}
	}

	private static void leaffall(Cast cast, Cast.Hit hit, double scale) {
		int fallen = 0;
		BlockPos center = target(hit);
		int r = FarmRules.patch(scale);
		for (BlockPos p : patch(center, r, r)) {
			BlockState state = cast.level.getBlockState(p);
			if (!state.is(BlockTags.LEAVES) || !state.hasProperty(LeavesBlock.PERSISTENT) || state.getValue(LeavesBlock.PERSISTENT)) {
				continue;
			}
			if (!Casters.mayEdit(cast.caster, cast.level, p) || !cast.takeBlock()) {
				continue;
			}
			// What decay would drop (a sapling, a stick, an apple now and then): the leaves' loot with no tool.
			Block.dropResources(state, cast.level, p);
			cast.level.removeBlock(p, false);
			fallen++;
		}
		if (fallen > 0) {
			Vfx.emit(cast.level, ParticleTypes.COMPOSTER, Vec3.atCenterOf(center), 6 + fallen, r, 0.02);
			Fx.sound(cast.level, Vec3.atCenterOf(center), SoundEvents.AZALEA_LEAVES_BREAK, 1.0F, 0.9F);
		}
	}

	private static void barkstrip(Cast cast, Cast.Hit hit, double scale) {
		int stripped = 0;
		BlockPos center = target(hit);
		int r = FarmRules.patch(scale);
		for (BlockPos p : patch(center, r, r)) {
			BlockState state = cast.level.getBlockState(p);
			BlockState bare = stripped(state);
			if (bare == null || !mayChange(cast, p) || !cast.takeBlock()) {
				continue;
			}
			cast.level.setBlock(p, bare, Block.UPDATE_ALL);
			Vfx.emit(cast.level, ParticleTypes.COMPOSTER, Vec3.atCenterOf(p), 2, 0.4, 0.01);
			stripped++;
		}
		if (stripped > 0) {
			Fx.sound(cast.level, Vec3.atCenterOf(center), SoundEvents.AXE_STRIP, 1.0F, 1.0F);
		}
	}

	/** A log or wood with its bark stripped (its axis kept), or null when it isn't one or is stripped already. */
	static BlockState stripped(BlockState state) {
		if (!state.is(BlockTags.LOGS)) {
			return null;
		}
		Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		String path = FarmRules.strippedPath(id.getPath());
		if (path == null) {
			return null;
		}
		Block bare = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(), path)).orElse(null);
		if (bare == null || bare == Blocks.AIR || bare == state.getBlock()) {
			return null;
		}
		BlockState out = bare.defaultBlockState();
		if (state.hasProperty(RotatedPillarBlock.AXIS) && out.hasProperty(RotatedPillarBlock.AXIS)) {
			out = out.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
		}
		return out;
	}

	private static void coppice(Cast cast, Cast.Hit hit) {
		BlockPos start = target(hit);
		ServerLevel level = cast.level;
		if (!level.getBlockState(start).is(BlockTags.LOGS)) {
			return;
		}
		// The tree: logs touching (diagonals too) from the one struck, never below it, at most a budget's worth.
		List<BlockPos> logs = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> open = new ArrayDeque<>();
		open.add(start);
		seen.add(start);
		boolean living = false;
		while (!open.isEmpty() && logs.size() < FarmRules.COPPICE_MAX) {
			BlockPos p = open.poll();
			logs.add(p);
			for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, 0, -1), p.offset(1, 1, 1))) {
				BlockPos q = n.immutable();
				BlockState state = level.getBlockState(q);
				if (state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT)) {
					living = true;
				}
				if (q.getY() >= start.getY() && state.is(BlockTags.LOGS) && seen.add(q)) {
					open.add(q);
				}
			}
		}
		if (!living) {
			return;
		}
		BlockPos base = start;
		while (level.getBlockState(base.below()).is(BlockTags.LOGS) && base.getY() > start.getY() - 2) {
			base = base.below();
		}
		int felled = 0;
		for (BlockPos p : logs) {
			BlockState state = level.getBlockState(p);
			if (!Casters.mayEdit(cast.caster, level, p) || !cast.takeBlock()) {
				continue;
			}
			List<ItemStack> drops = Block.getDrops(state, level, p, level.getBlockEntity(p), cast.caster, new ItemStack(Items.IRON_AXE));
			level.destroyBlock(p, false, cast.caster);
			drops.forEach(d -> Block.popResource(level, start, d));
			felled++;
		}
		if (felled == 0) {
			return;
		}
		Vfx.emit(level, ParticleTypes.COMPOSTER, Vec3.atCenterOf(start), 10, 0.8, 0.03);
		Fx.sound(level, Vec3.atCenterOf(start), SoundEvents.WOOD_BREAK, 1.0F, 0.8F);
		// The stump is replanted from the caster's own saplings.
		BlockPos stump = level.getBlockState(start).isAir() && level.getBlockState(start.below()).is(BlockTags.DIRT) ? start : null;
		if (stump != null && cast.caster instanceof ServerPlayer player) {
			BlockPos at = stump;
			ItemStack sapling = find(player, s -> s.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().is(BlockTags.SAPLINGS)
				&& bi.getBlock().defaultBlockState().canSurvive(level, at));
			if (!sapling.isEmpty() && Casters.mayEdit(player, level, at)) {
				level.setBlockAndUpdate(at, ((BlockItem) sapling.getItem()).getBlock().defaultBlockState());
				use(player, sapling, 1);
				Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(at), 6, 0.3, 0.02);
			}
		}
	}

	// ---- Movement.

	private static void trot(Cast cast, Cast.Hit hit, List<LivingEntity> helped, double duration) {
		List<LivingEntity> mounts = new ArrayList<>();
		if (cast.caster.getVehicle() instanceof LivingEntity vehicle) {
			mounts.add(vehicle);
		}
		if (!hit.self()) {
			for (LivingEntity t : helped) {
				if (t instanceof AbstractHorse && !mounts.contains(t)) {
					mounts.add(t);
				}
			}
		}
		for (LivingEntity mount : mounts) {
			mount.addEffect(new MobEffectInstance(MobEffects.SPEED, FarmRules.ticks(20, duration), 1, false, true));
			mount.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, FarmRules.ticks(20, duration), 0, false, true));
			Vfx.emit(cast.level, ParticleTypes.CLOUD, mount.position(), 8, 0.5, 0.02);
			Fx.sound(cast.level, mount.position(), SoundEvents.HORSE_GALLOP, 0.8F, 1.0F);
		}
	}

	private static void beeline(Cast cast, Cast.Hit hit, double power) {
		LivingEntity caster = cast.caster;
		Vec3 look = caster.getLookAngle();
		Vec3 aim = hit.self() ? look : hit.point().subtract(caster.getEyePosition());
		if (aim.lengthSqr() < 1.0E-4) {
			aim = look;
		}
		Vec3 dir = aim.normalize();
		// Mostly level, a touch of lift so a zip along the ground doesn't scrape it.
		Vec3 v = new Vec3(dir.x, Math.max(-0.3, Math.min(0.5, dir.y)) + 0.15, dir.z).normalize().scale(FarmRules.zipSpeed(power));
		move(caster, v);
		caster.resetFallDistance();
		Scheduler.later(20, caster::resetFallDistance);
		Scheduler.later(40, caster::resetFallDistance);
		Vfx.stream(cast.level, caster.position(), caster.position().add(v.scale(3)), Vfx.theme("wind"), 8);
		Fx.sound(cast.level, caster.position(), SoundEvents.BEE_LOOP, 1.0F, 1.4F);
	}

	/** Who a movement rune moves: you, cast on yourself; otherwise the allies it struck (never a foe). */
	private static List<LivingEntity> movers(Cast cast, Cast.Hit hit, List<LivingEntity> helped) {
		return hit.self() ? List.of(cast.caster) : helped;
	}

	// ---- Helpers.

	private static BlockPos target(Cast.Hit hit) {
		if (hit.block() != null) {
			return hit.block();
		}
		return BlockPos.containing(hit.point().x, hit.point().y - 0.5, hit.point().z);
	}

	/** The blocks of a square patch {@code r} out and {@code dy} up and down, centre first, as immutable positions. */
	private static List<BlockPos> patch(BlockPos center, int r, int dy) {
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -dy, -r), center.offset(r, dy, r))) {
			out.add(p.immutable());
		}
		out.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(center)));
		return out;
	}

	/**
	 * Whether a spell of the caster's may change a block in place (till it, wet it, age it): they may
	 * build there, it isn't a spell's passing block, and the claim mods agree, asked as for a break.
	 */
	private static boolean mayChange(Cast cast, BlockPos pos) {
		if (!(cast.caster instanceof ServerPlayer player) || !Casters.mayBuild(player) || !cast.level.mayInteract(player, pos)
			|| Effects.isTemporary(cast.level, pos) || !cast.admitsBlock(pos)) {
			return false;
		}
		return Casters.probeBreak(cast.level, player, pos, cast.level.getBlockState(pos), cast.level.getBlockEntity(pos));
	}

	private static void move(LivingEntity t, Vec3 v) {
		t.setDeltaMovement(v);
		t.needsSync = true;
		if (t instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	private static void glow(ServerLevel level, LivingEntity t, ParticleOptions particle, int count) {
		Vfx.emit(level, particle, t.position().add(0, t.getBbHeight() * 0.6, 0), count, 0.4, 0.02);
	}

	private static ItemStack find(ServerPlayer player, java.util.function.Predicate<ItemStack> wanted) {
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (!stack.isEmpty() && wanted.test(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static int count(ServerPlayer player, Item item) {
		int n = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(item)) {
				n += stack.getCount();
			}
		}
		return n;
	}

	/** Takes {@code n} of an item out of the inventory (all of it must be there: counted first). */
	private static void take(ServerPlayer player, Item item, int n) {
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (n <= 0) {
				return;
			}
			if (stack.is(item)) {
				int k = Math.min(n, stack.getCount());
				stack.shrink(k);
				n -= k;
			}
		}
	}

	private static void use(ServerPlayer player, ItemStack stack, int n) {
		stack.shrink(n);
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack) && !stack.isEmpty()) {
			Block.popResource(player.level(), player.blockPosition(), stack);
		}
	}
}
