package dev.wildercord.cast;

import dev.wildercord.mixin.ItemEntityAccessor;
import dev.wildercord.spell.DelveRules;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The delving pack: 48 working runes for mines, masonry, light, light redstone and hauling. None of them hurts
 * anything. Every block they change goes through the same gates as the other world runes (the caster may build, the
 * claim and protection mods agree, never an unbreakable block, a fluid, a block entity or a spell's passing block) and
 * the cast's block budget ({@link Cast#MAX_BLOCKS}); what they mine drops as a pickaxe would drop it, and what they move
 * between your pack and a chest is moved, never copied. The numbers are in {@link DelveRules}.
 */
final class DelveEffects {
	private DelveEffects() {}

	private static final Set<String> PATHS = Set.copyOf(DelveRules.PATHS);
	/** The hotbar (0-8) and the pack (9-35): never armour or the offhand. */
	private static final int HOTBAR_SLOTS = 9;
	private static final int PACK_SLOTS = 36;

	static boolean handles(String path) {
		return PATHS.contains(path);
	}

	static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
		double power, double duration, int amplify) {
		double scale = SpellNumbers.effectRadius(node);
		boolean amp = amplify > 0;
		switch (cast.caster instanceof Player ? node.effect.path() : "") {
			case "stairdelve" -> stairs(cast, true, amp);
			case "riser" -> stairs(cast, false, amp);
			case "plumbline" -> plumbline(cast, hit, amp);
			case "siftfall" -> siftfall(cast, hit, scale);
			case "gangue" -> gangue(cast, hit, scale, amp);
			case "orepluck" -> orepluck(cast, hit, scale, amp);
			case "luckstrike" -> luckstrike(cast, hit, amplify);
			case "silklift" -> silklift(cast, hit);
			case "deepsound" -> deepsound(cast, hit);
			case "oretally" -> oretally(cast, hit, scale);
			case "lavaseal" -> lavaseal(cast, hit, scale);
			case "deepway" -> deepway(cast, hit);
			case "hollowsense" -> hollowsense(cast, hit, scale);
			case "kilnbake" -> kilnbake(cast, hit, scale);
			case "blockpack" -> blockpack(cast);
			case "unpack" -> unpack(cast);
			case "millstone" -> millstone(cast);
			case "toolmend" -> targets(cast, hit, helped).forEach(t -> toolmend(cast, t));
			case "levelground" -> levelground(cast, hit, scale, amp);
			case "holefill" -> holefill(cast, hit, scale);
			case "shoreup" -> shoreup(cast, hit, scale);
			case "stilt" -> stilt(cast, duration);
			case "plankway" -> plankway(cast);
			case "polish" -> dress(cast, hit, scale, POLISH, "earth_grind");
			case "brickwork" -> dress(cast, hit, scale, BRICK, "earth_press");
			case "agestone" -> dress(cast, hit, scale, AGE, "earth_creak");
			case "concreteset" -> concreteset(cast, hit, scale);
			case "chalkline" -> chalkline(cast, hit, duration);
			case "pitfloor" -> pitfloor(cast, hit, duration);
			case "floorlay" -> floorlay(cast, hit);
			case "torchfall" -> torchfall(cast, hit, scale);
			case "gloomsight" -> gloomsight(cast, hit, scale, duration);
			case "lumenpath" -> lumenpath(cast, hit, duration);
			case "snuffout" -> snuffout(cast, hit, scale);
			case "headlamp" -> targets(cast, hit, helped).forEach(t -> headlamp(cast, t, duration));
			case "leverflip" -> switches(cast, hit, scale, "leverflip");
			case "buttonpush" -> switches(cast, hit, scale, "buttonpush");
			case "doorcall" -> switches(cast, hit, scale, "doorcall");
			case "chestsort" -> chestsort(cast, hit);
			case "stow" -> stow(cast, hit);
			case "restock" -> restock(cast, hit);
			case "stocktake" -> stocktake(cast, hit, scale);
			case "unburden" -> unburden(cast, hit);
			case "packtidy" -> packtidy(cast);
			case "lodepull" -> targets(cast, hit, helped).forEach(t -> lodepull(cast, t, scale, duration));
			case "caveward" -> targets(cast, hit, helped).forEach(t -> caveward(cast, t, duration));
			case "delvemark" -> delvemark(cast);
			case "motherlode" -> motherlode(cast, hit, amplify);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------------ gates and shared pieces

	/** Who a helpful rune of the pack serves: the creatures it may help, or the caster on a Self cast. */
	private static List<LivingEntity> targets(Cast cast, Cast.Hit hit, List<LivingEntity> helped) {
		if (!helped.isEmpty()) {
			return helped;
		}
		return hit.self() ? List.of(cast.caster) : List.of();
	}

	/** The block a spell points at: the one it struck, or the one under where it landed. */
	private static BlockPos target(Cast cast, Cast.Hit hit) {
		if (hit.block() != null) {
			return hit.block();
		}
		return BlockPos.containing(hit.point().x, hit.point().y - 0.5, hit.point().z);
	}

	private static ServerPlayer player(Cast cast) {
		return Casters.player(cast.caster);
	}

	/** Whether a spell of the caster's may take the block at {@code pos} away (offered as the player breaking it), within the budget. */
	private static boolean mayBreak(Cast cast, BlockPos pos) {
		return cast.admitsBlock(pos) && Casters.mayBuild(cast.caster) && !Techniques.isRampart(cast.level, pos) && !Effects.isTemporary(cast.level, pos)
			&& Casters.mayEdit(cast.caster, cast.level, pos) && cast.takeBlock();
	}

	/** Whether a spell of the caster's may change the block at {@code pos} in place (asked of the claim mods as a break, never run as one), within the budget. */
	private static boolean mayChange(Cast cast, BlockPos pos) {
		ServerPlayer player = player(cast);
		if (player == null || !cast.admitsBlock(pos) || !Casters.mayBuild(player) || !cast.level.mayInteract(player, pos)
			|| Techniques.isRampart(cast.level, pos) || Effects.isTemporary(cast.level, pos)) {
			return false;
		}
		return Casters.probeBreak(cast.level, player, pos, cast.level.getBlockState(pos), cast.level.getBlockEntity(pos)) && cast.takeBlock();
	}

	/** Whether the caster may reach into the block at {@code pos} (a chest, a lever) without changing the ground: no budget taken. */
	private static boolean mayUse(Cast cast, BlockPos pos) {
		ServerPlayer player = player(cast);
		return player != null && cast.admitsBlock(pos) && cast.level.mayInteract(player, pos) && !Effects.isTemporary(cast.level, pos)
			&& Casters.probeBreak(cast.level, player, pos, cast.level.getBlockState(pos), cast.level.getBlockEntity(pos));
	}

	private static TagKey<Block> tooHard(boolean diamond) {
		return diamond ? BlockTags.INCORRECT_FOR_DIAMOND_TOOL : BlockTags.INCORRECT_FOR_IRON_TOOL;
	}

	private static ItemStack pick(boolean diamond) {
		return new ItemStack(diamond ? Items.DIAMOND_PICKAXE : Items.IRON_PICKAXE);
	}

	/** A diamond pickaxe carrying one enchantment, so drops come out as they would from it. */
	private static ItemStack enchanted(ServerLevel level, ResourceKey<Enchantment> key, int levelOf) {
		ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
		level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key).ifPresent(holder -> tool.enchant(holder, levelOf));
		return tool;
	}

	/** Whether the block can be mined by the pack at all: solid, dry, breakable, no block entity, soft enough for the tool. */
	private static boolean mineable(ServerLevel level, BlockPos pos, BlockState state, TagKey<Block> tooHard) {
		return !state.isAir() && !(state.getBlock() instanceof LiquidBlock) && state.getFluidState().isEmpty() && !state.hasBlockEntity()
			&& state.getDestroySpeed(level, pos) >= 0 && !state.is(tooHard);
	}

	/**
	 * Mines one block as a player holding {@code tool} would, dropping what that tool drops (and the experience an ore gives) at
	 * the block. Returns whether it was mined.
	 */
	private static boolean mine(Cast cast, BlockPos pos, ItemStack tool, TagKey<Block> tooHard) {
		ServerLevel level = cast.level;
		BlockState state = level.getBlockState(pos);
		if (!mineable(level, pos, state, tooHard) || !mayBreak(cast, pos)) {
			return false;
		}
		boolean harvest = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
		List<ItemStack> loot = harvest ? Block.getDrops(state, level, pos, null, cast.caster, tool) : List.of();
		level.destroyBlock(pos, false, cast.caster);
		crunch(level, pos);
		if (harvest) {
			state.spawnAfterBreak(level, pos, tool, true);
		}
		loot.forEach(stack -> Block.popResource(level, pos, stack));
		return true;
	}

	/** The crunch of a block broken by the pack: once a tick at most. */
	private static long lastCrunch = Long.MIN_VALUE;

	private static void crunch(ServerLevel level, BlockPos pos) {
		Vfx.emit(level, SpellMaterials.of("earth", 0xB48C5A, .12F), Vec3.atCenterOf(pos), 6, .3, .03);
		if (level.getGameTime() != lastCrunch) {
			lastCrunch = level.getGameTime();
			Fx.sound(level, Vec3.atCenterOf(pos), dev.wildercord.content.WildercordSounds.MAGIC_BREAK, .8F, 1F);
		}
	}

	/** Motes of a rune's colour where it worked. */
	private static void motes(ServerLevel level, Vec3 at, String element, int color, int count) {
		Vfx.emit(level, SpellMaterials.of(element, color, .14F), at, count, .4, .03);
	}

	private static void tell(Cast cast, String key, Object... args) {
		Casters.tell(cast.caster, Component.translatable("message.wildercord." + key, args));
	}

	private static boolean wet(ServerLevel level, BlockPos pos) {
		if (!level.getFluidState(pos).isEmpty()) {
			return true;
		}
		for (Direction d : Direction.values()) {
			if (!level.getFluidState(pos.relative(d)).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private static boolean ore(BlockState state) {
		if (state.is(ConventionalBlockTags.ORES)) {
			return true;
		}
		return DelveRules.oreKind(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath()) != null;
	}

	private static String oreName(BlockState state) {
		String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		String kind = DelveRules.oreKind(path);
		return kind == null ? path.replace('_', ' ') : kind.replace('_', ' ');
	}

	/** The blocks within {@code r} of {@code center}, nearest first. */
	private static List<BlockPos> around(BlockPos center, int r) {
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			out.add(p.immutable());
		}
		out.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
		return out;
	}

	private static boolean exposed(ServerLevel level, BlockPos pos) {
		for (Direction d : Direction.values()) {
			if (level.getBlockState(pos.relative(d)).isAir()) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ mining and caves

	/** Stair Delve and Riser: five steps the way you face, stopping short of lava, water or a drop. */
	private static void stairs(Cast cast, boolean down, boolean amp) {
		ServerLevel level = cast.level;
		Direction ahead = cast.caster.getDirection();
		BlockPos feet = cast.caster.blockPosition();
		List<int[]> cells = DelveRules.stairCells(DelveRules.STAIR_STEPS, down);
		int carved = 0;
		boolean halted = false;
		for (int step = 0; step < DelveRules.STAIR_STEPS && !halted; step++) {
			List<BlockPos> stepCells = new ArrayList<>();
			for (int k = 0; k < 3; k++) {
				int[] c = cells.get(step * 3 + k);
				stepCells.add(feet.relative(ahead, c[0]).above(c[1]));
			}
			BlockPos floor = stepCells.get(0).below();
			BlockState under = level.getBlockState(floor);
			if (under.isAir() || !under.getFluidState().isEmpty() || stepCells.stream().anyMatch(p -> wet(level, p))) {
				halted = true;
				break;
			}
			for (BlockPos p : stepCells) {
				if (mine(cast, p, pick(amp), tooHard(amp))) {
					carved++;
				}
			}
		}
		if (halted) {
			tell(cast, "delve_stopped");
		}
		if (carved > 0) {
			dev.wildercord.cast.feel.Feels.sound(level, cast.caster.position(), "earth_dig", .7F, down ? .84F : 1.12F);
			ElementFx.crack(level, cast.caster.position(), 1.2, 20);
		} else {
			dev.wildercord.cast.feel.Feels.sound(level, cast.caster.position(), "fizzle", .6F, 1F);
		}
	}

	/** Plumb Line: a one-wide shaft down from the point, a block short of lava, water or open air. */
	private static void plumbline(Cast cast, Cast.Hit hit, boolean amp) {
		ServerLevel level = cast.level;
		BlockPos start = target(cast, hit);
		int dug = 0;
		for (int d = 0; d < DelveRules.SHAFT_DEPTH; d++) {
			BlockPos p = start.below(d);
			BlockState below = level.getBlockState(p.below());
			if (below.isAir() || !below.getFluidState().isEmpty() || wet(level, p)) {
				tell(cast, "delve_stopped");
				break;
			}
			if (!mine(cast, p, pick(amp), tooHard(amp))) {
				break;
			}
			dug++;
		}
		if (dug > 0) {
			Vfx.stream(level, Vec3.atCenterOf(start), Vec3.atCenterOf(start.below(dug - 1)), Vfx.theme("earth"), 8);
			dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(start), "earth_sink", .7F, 1F);
		}
	}

	private static boolean loose(BlockState state) {
		return !state.hasBlockEntity() && (state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || Blocks.CONCRETE_POWDER.asList().contains(state.getBlock()));
	}

	/** Siftfall: loose blocks hanging over air above the point come down, as items. */
	private static void siftfall(Cast cast, Cast.Hit hit, double scale) {
		ServerLevel level = cast.level;
		BlockPos base = target(cast, hit);
		int r = DelveRules.radius(1, scale);
		int sifted = 0;
		for (int y = 1; y <= DelveRules.SIFT_HEIGHT; y++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					BlockPos p = base.offset(dx, y, dz);
					if (loose(level.getBlockState(p)) && level.getBlockState(p.below()).isAir() && mine(cast, p, pick(false), tooHard(false))) {
						sifted++;
					}
				}
			}
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(base), sifted > 0 ? "earth_rattle" : "fizzle", .7F, 1F);
	}

	private static boolean plainRock(BlockState state) {
		return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) || state.is(BlockTags.DIRT)
			|| state.is(Blocks.GRAVEL) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.COBBLED_DEEPSLATE) || state.is(Blocks.CALCITE)
			|| state.is(Blocks.DRIPSTONE_BLOCK) || state.is(Blocks.SMOOTH_BASALT);
	}

	/** Gangue: plain rock around the point goes, the ores stay standing. */
	private static void gangue(Cast cast, Cast.Hit hit, double scale, boolean amp) {
		ServerLevel level = cast.level;
		BlockPos center = target(cast, hit);
		int cleared = 0;
		for (BlockPos p : around(center, DelveRules.radius(2, scale))) {
			BlockState state = level.getBlockState(p);
			if (plainRock(state) && !ore(state) && mine(cast, p, pick(amp), tooHard(amp))) {
				cleared++;
			}
		}
		if (cleared > 0) {
			ElementFx.groundRing(level, Vec3.atCenterOf(center), ElementFx.EARTH.primary(), .5, 2.5, .08, 14);
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), cleared > 0 ? "earth_rumble" : "fizzle", .7F, 1F);
	}

	/** Ore Pluck: the ores with an open face near the point. */
	private static void orepluck(Cast cast, Cast.Hit hit, double scale, boolean amp) {
		ServerLevel level = cast.level;
		BlockPos center = target(cast, hit);
		int plucked = 0;
		for (BlockPos p : around(center, DelveRules.radius(4, scale))) {
			if (plucked >= DelveRules.ORE_PLUCK_MAX) {
				break;
			}
			BlockState state = level.getBlockState(p);
			if (ore(state) && exposed(level, p)) {
				Vec3 at = Vec3.atCenterOf(p);
				if (mine(cast, p, pick(amp), tooHard(amp))) {
					plucked++;
					Vfx.stream(level, at, cast.caster.getEyePosition(), Vfx.theme("earth"), 4);
				}
			}
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), plucked > 0 ? "earth_snap" : "fizzle", .7F, 1.12F);
	}

	/** Luckstrike: the ore hit, with Fortune. */
	private static void luckstrike(Cast cast, Cast.Hit hit, int amplify) {
		if (hit.block() == null || !ore(cast.level.getBlockState(hit.block()))) {
			tell(cast, "delve_not_ore");
			return;
		}
		ItemStack tool = enchanted(cast.level, Enchantments.FORTUNE, DelveRules.fortune(false, amplify));
		if (mine(cast, hit.block(), tool, tooHard(true))) {
			ElementFx.sparks(cast.level, Vec3.atCenterOf(hit.block()), 10, .15);
			dev.wildercord.cast.feel.Feels.sound(cast.level, Vec3.atCenterOf(hit.block()), "earth_ping", .8F, 1.26F);
		}
	}

	/** Motherlode: every ore near the point, with Fortune II. */
	private static void motherlode(Cast cast, Cast.Hit hit, int amplify) {
		ServerLevel level = cast.level;
		BlockPos center = target(cast, hit);
		ItemStack tool = enchanted(level, Enchantments.FORTUNE, DelveRules.fortune(true, amplify));
		int taken = 0;
		for (BlockPos p : around(center, 3)) {
			if (taken >= DelveRules.MOTHERLODE_MAX) {
				break;
			}
			if (ore(level.getBlockState(p)) && mine(cast, p, tool, tooHard(true))) {
				taken++;
				ElementFx.sparks(level, Vec3.atCenterOf(p), 4, .1);
			}
		}
		if (taken > 0) {
			Vfx.burst(level, Vec3.atCenterOf(center), 3, Vfx.theme("earth"));
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), taken > 0 ? "earth_rumble" : "fizzle", .9F, .75F);
	}

	/** Silklift: the block hit, whole. */
	private static void silklift(Cast cast, Cast.Hit hit) {
		if (hit.block() == null) {
			return;
		}
		ItemStack tool = enchanted(cast.level, Enchantments.SILK_TOUCH, 1);
		if (mine(cast, hit.block(), tool, tooHard(true))) {
			motes(cast.level, Vec3.atCenterOf(hit.block()), "arcane", 0xFFD8FA, 10);
			dev.wildercord.cast.feel.Feels.sound(cast.level, Vec3.atCenterOf(hit.block()), "arcane_glint", .7F, 1F);
		}
	}

	/** Deepsound: the nearest ore under the point, and how deep. */
	private static void deepsound(Cast cast, Cast.Hit hit) {
		ServerLevel level = cast.level;
		BlockPos top = target(cast, hit);
		for (int d = 1; d <= DelveRules.DEEPSOUND_DEPTH; d++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos p = top.offset(dx, -d, dz);
					BlockState state = level.getBlockState(p);
					if (ore(state)) {
						tell(cast, "deepsound_found", oreName(state), d);
						dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(top), "earth_ping", .7F, 1F - d * .02F);
						return;
					}
				}
			}
		}
		tell(cast, "deepsound_none");
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(top), "earth_tick", .6F, .84F);
	}

	/** Ore Tally: the ores near the point, counted by kind. */
	private static void oretally(Cast cast, Cast.Hit hit, double scale) {
		ServerLevel level = cast.level;
		BlockPos center = target(cast, hit);
		Map<String, Integer> counts = new HashMap<>();
		int r = DelveRules.radius(DelveRules.TALLY_RADIUS, scale);
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			BlockState state = level.getBlockState(p);
			if (ore(state)) {
				counts.merge(oreName(state), 1, Integer::sum);
			}
		}
		tell(cast, counts.isEmpty() ? "oretally_none" : "oretally", DelveRules.tally(counts));
		ElementFx.groundRing(level, Vec3.atCenterOf(center), ElementFx.EARTH.secondary(), .5, r, .06, 16);
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), "earth_tick", .6F, 1.12F);
	}

	/** Lava Seal: still lava near the point sets to obsidian, flowing lava to cobblestone. */
	private static void lavaseal(Cast cast, Cast.Hit hit, double scale) {
		ServerLevel level = cast.level;
		BlockPos center = hit.block() != null && hit.face() != null ? hit.block().relative(hit.face()) : BlockPos.containing(hit.point());
		int sealed = 0;
		for (BlockPos p : around(center, DelveRules.radius(3, scale))) {
			if (sealed >= DelveRules.SEAL_MAX) {
				break;
			}
			BlockState state = level.getBlockState(p);
			if (!state.is(Blocks.LAVA) || !mayChange(cast, p)) {
				continue;
			}
			boolean source = level.getFluidState(p).isSourceOfType(Fluids.LAVA);
			level.setBlockAndUpdate(p, (source ? Blocks.OBSIDIAN : Blocks.COBBLESTONE).defaultBlockState());
			level.levelEvent(1501, p, 0);
			sealed++;
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), sealed > 0 ? "fire_steam" : "fizzle", .8F, 1F);
	}

	/** Deepway: a 3x3 road three deep into the wall hit, with torches from the pack. */
	private static void deepway(Cast cast, Cast.Hit hit) {
		if (hit.block() == null || hit.face() == null || !hit.face().getAxis().isHorizontal()) {
			tell(cast, "deepway_wall");
			return;
		}
		ServerLevel level = cast.level;
		Direction into = hit.face().getOpposite();
		Direction side = into.getClockWise();
		BlockPos front = hit.block();
		// The road's floor sits at the caster's feet when they aim at the wall in front of them.
		BlockPos base = front.getY() > cast.caster.getBlockY() ? front.below(front.getY() - cast.caster.getBlockY()) : front;
		int carved = 0;
		for (int d = 0; d < 3; d++) {
			for (int y = 0; y < 3; y++) {
				for (int s = -1; s <= 1; s++) {
					BlockPos p = base.relative(into, d).relative(side, s).above(y);
					if (wet(level, p)) {
						continue;
					}
					if (mine(cast, p, pick(true), tooHard(true))) {
						carved++;
					}
				}
			}
		}
		int lit = 0;
		for (BlockPos spot : List.of(base.relative(side, -1), base.relative(into, 2).relative(side, 1))) {
			if (lit < 2 && placeTorch(cast, spot)) {
				lit++;
			}
		}
		if (carved > 0) {
			Vfx.burst(level, Vec3.atCenterOf(base.relative(into, 1).above()), 2, Vfx.theme("earth"));
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(base), carved > 0 ? "earth_rumble" : "fizzle", 1F, .75F);
	}

	/** Hollow Sense: the way to the nearest open cave. */
	private static void hollowsense(Cast cast, Cast.Hit hit, double scale) {
		ServerLevel level = cast.level;
		BlockPos center = BlockPos.containing(hit.point());
		int r = Math.max(DelveRules.CAVE_RADIUS, DelveRules.radius(DelveRules.CAVE_RADIUS / 2, scale) * 2);
		r = Math.min(16, r);
		BlockPos best = null;
		double bestD = Double.MAX_VALUE;
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (level.getBlockState(p).is(Blocks.CAVE_AIR)) {
				double d = p.distSqr(center);
				if (d < bestD) {
					bestD = d;
					best = p.immutable();
				}
			}
		}
		if (best == null) {
			tell(cast, "hollowsense_none");
			dev.wildercord.cast.feel.Feels.sound(level, hit.point(), "fizzle", .5F, 1F);
			return;
		}
		Casters.tell(cast.caster, Component.translatable("message.wildercord.hollowsense", direction(best.subtract(center)), Math.round(Math.sqrt(bestD))));
		Vfx.stream(level, cast.caster.getEyePosition(), Vec3.atCenterOf(best), Vfx.theme("wind"), 10);
		dev.wildercord.cast.feel.Feels.sound(level, hit.point(), "wind_lift", .6F, 1.12F);
	}

	private static Component direction(BlockPos offset) {
		int dx = offset.getX();
		int dy = offset.getY();
		int dz = offset.getZ();
		String key;
		if (Math.abs(dy) > Math.max(Math.abs(dx), Math.abs(dz))) {
			key = dy > 0 ? "up" : "down";
		} else if (Math.abs(dx) > Math.abs(dz)) {
			key = dx > 0 ? "east" : "west";
		} else {
			key = dz > 0 ? "south" : "north";
		}
		return Component.translatable("message.wildercord.delve_dir." + key);
	}

	// ------------------------------------------------------------------ smelting and crafting

	/** Kiln Bake: blocks near the point bake in place, when their smelting gives a block, one for one. */
	private static void kilnbake(Cast cast, Cast.Hit hit, double scale) {
		ServerLevel level = cast.level;
		BlockPos center = target(cast, hit);
		int baked = 0;
		for (BlockPos p : around(center, DelveRules.radius(1, scale))) {
			if (baked >= DelveRules.BAKE_MAX) {
				break;
			}
			BlockState state = level.getBlockState(p);
			if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty() || state.getDestroySpeed(level, p) < 0) {
				continue;
			}
			Item item = state.getBlock().asItem();
			if (item == Items.AIR) {
				continue;
			}
			SingleRecipeInput input = new SingleRecipeInput(new ItemStack(item));
			var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
			if (recipe.isEmpty()) {
				continue;
			}
			ItemStack out = recipe.get().value().assemble(input);
			if (out.getCount() != 1 || !(out.getItem() instanceof BlockItem block) || block.getBlock().defaultBlockState().hasBlockEntity()) {
				continue;
			}
			if (!mayChange(cast, p)) {
				continue;
			}
			level.setBlockAndUpdate(p, block.getBlock().defaultBlockState());
			ElementFx.embers(level, Vec3.atCenterOf(p), .4, 4);
			baked++;
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), baked > 0 ? "fire_coals" : "fizzle", .7F, 1F);
	}

	/** A plain stack: no name, enchantment or wear, so nine of them may be crafted like any nine. */
	private static boolean plain(ItemStack stack) {
		return !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, new ItemStack(stack.getItem()));
	}

	/** How many plain {@code item} the player carries (hotbar and pack). */
	private static int carried(Inventory inv, Item item) {
		int n = 0;
		for (int i = 0; i < PACK_SLOTS; i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(item) && plain(s)) {
				n += s.getCount();
			}
		}
		return n;
	}

	/** Takes {@code count} plain {@code item} out of the hotbar and pack (the caller has counted them). */
	private static void takeOut(Inventory inv, Item item, int count) {
		int left = count;
		for (int i = PACK_SLOTS - 1; i >= 0 && left > 0; i--) {
			ItemStack s = inv.getItem(i);
			if (s.is(item) && plain(s)) {
				int n = Math.min(left, s.getCount());
				s.shrink(n);
				left -= n;
			}
		}
		inv.setChanged();
	}

	/** Gives the player a stack, dropping at their feet whatever doesn't fit. */
	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack) && !stack.isEmpty()) {
			player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}
	}

	/** The distinct plain items carried, in slot order. */
	private static List<Item> kinds(Inventory inv) {
		Set<Item> seen = new java.util.LinkedHashSet<>();
		for (int i = 0; i < PACK_SLOTS; i++) {
			ItemStack s = inv.getItem(i);
			if (plain(s)) {
				seen.add(s.getItem());
			}
		}
		return new ArrayList<>(seen);
	}

	/** Block Pack: nine of a kind into the block they craft into. */
	private static void blockpack(Cast cast) {
		ServerPlayer player = player(cast);
		ServerLevel level = cast.level;
		Inventory inv = player.getInventory();
		int made = 0;
		for (Item item : kinds(inv)) {
			if (made >= DelveRules.PACK_MAX) {
				break;
			}
			int packs = Math.min(DelveRules.PACK_MAX - made, DelveRules.packs(carried(inv, item)));
			if (packs <= 0) {
				continue;
			}
			List<ItemStack> grid = new ArrayList<>();
			for (int i = 0; i < 9; i++) {
				grid.add(new ItemStack(item));
			}
			CraftingInput input = CraftingInput.of(3, 3, grid);
			var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
			if (recipe.isEmpty()) {
				continue;
			}
			ItemStack out = recipe.get().value().assemble(input);
			if (out.getCount() != 1 || !(out.getItem() instanceof BlockItem) || out.is(item)
				|| recipe.get().value().getRemainingItems(input).stream().anyMatch(s -> !s.isEmpty())) {
				continue;
			}
			takeOut(inv, item, packs * 9);
			give(player, out.copyWithCount(packs));
			made += packs;
		}
		if (made == 0) {
			tell(cast, "blockpack_none");
		}
		dev.wildercord.cast.feel.Feels.sound(level, player.position(), made > 0 ? "earth_press" : "fizzle", .7F, 1F);
		if (made > 0) {
			motes(level, player.position().add(0, 1, 0), "earth", 0xE8C890, 8);
		}
	}

	/** Unpack: storage blocks back into nine pieces. */
	private static void unpack(Cast cast) {
		ServerPlayer player = player(cast);
		ServerLevel level = cast.level;
		Inventory inv = player.getInventory();
		int done = 0;
		for (Item item : kinds(inv)) {
			if (done >= DelveRules.UNPACK_MAX || !(item instanceof BlockItem)) {
				continue;
			}
			CraftingInput input = CraftingInput.of(1, 1, List.of(new ItemStack(item)));
			var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
			if (recipe.isEmpty()) {
				continue;
			}
			ItemStack out = recipe.get().value().assemble(input);
			if (out.getCount() != 9 || out.is(item) || recipe.get().value().getRemainingItems(input).stream().anyMatch(s -> !s.isEmpty())) {
				continue;
			}
			int n = Math.min(DelveRules.UNPACK_MAX - done, carried(inv, item));
			takeOut(inv, item, n);
			for (int i = 0; i < n; i++) {
				give(player, out.copy());
			}
			done += n;
		}
		if (done == 0) {
			tell(cast, "unpack_none");
		}
		dev.wildercord.cast.feel.Feels.sound(level, player.position(), done > 0 ? "earth_crack" : "fizzle", .7F, 1.12F);
	}

	/** Millstone: cobblestone into gravel, or else gravel into sand. */
	private static void millstone(Cast cast) {
		ServerPlayer player = player(cast);
		Inventory inv = player.getInventory();
		Item from = carried(inv, Items.COBBLESTONE) > 0 ? Items.COBBLESTONE : carried(inv, Items.GRAVEL) > 0 ? Items.GRAVEL : null;
		if (from == null) {
			tell(cast, "millstone_none");
			dev.wildercord.cast.feel.Feels.sound(cast.level, player.position(), "fizzle", .6F, 1F);
			return;
		}
		int n = Math.min(DelveRules.GRIND_MAX, carried(inv, from));
		takeOut(inv, from, n);
		give(player, new ItemStack(from == Items.COBBLESTONE ? Items.GRAVEL : Items.SAND, n));
		motes(cast.level, player.position().add(0, 1, 0), "earth", 0xC8B898, 10);
		dev.wildercord.cast.feel.Feels.sound(cast.level, player.position(), "earth_grind", .7F, 1F);
	}

	/** Tool Mend: the tool in the target's hand, mended with its repair material from the caster's pack. */
	private static void toolmend(Cast cast, LivingEntity target) {
		ServerPlayer player = player(cast);
		ItemStack tool = target.getMainHandItem();
		if (tool.isEmpty() || !tool.isDamageableItem() || tool.getDamageValue() <= 0) {
			tell(cast, "toolmend_none");
			return;
		}
		Inventory inv = player.getInventory();
		int carried = 0;
		for (int i = 0; i < PACK_SLOTS; i++) {
			ItemStack s = inv.getItem(i);
			if (s != tool && plain(s) && tool.isValidRepairItem(s)) {
				carried += s.getCount();
			}
		}
		int units = DelveRules.mendUnits(tool.getDamageValue(), tool.getMaxDamage(), carried);
		if (units <= 0) {
			tell(cast, "toolmend_none");
			dev.wildercord.cast.feel.Feels.sound(cast.level, target.position(), "fizzle", .6F, 1F);
			return;
		}
		int left = units;
		for (int i = PACK_SLOTS - 1; i >= 0 && left > 0; i--) {
			ItemStack s = inv.getItem(i);
			if (s != tool && plain(s) && tool.isValidRepairItem(s)) {
				int n = Math.min(left, s.getCount());
				s.shrink(n);
				left -= n;
			}
		}
		inv.setChanged();
		tool.setDamageValue(DelveRules.mended(tool.getDamageValue(), tool.getMaxDamage(), units));
		ElementFx.sparks(cast.level, target.position().add(0, 1, 0), 8, .1);
		dev.wildercord.cast.feel.Feels.sound(cast.level, target.position(), "earth_clamp", .7F, 1.26F);
	}

	// ------------------------------------------------------------------ building and masonry

	/** Plain building blocks the pack lays: stone, dirt, planks and the like, never anything with a block entity or that falls. */
	private static boolean filler(ItemStack stack) {
		if (!plain(stack) || !(stack.getItem() instanceof BlockItem item)) {
			return false;
		}
		BlockState state = item.getBlock().defaultBlockState();
		if (state.hasBlockEntity() || item.getBlock() instanceof net.minecraft.world.level.block.FallingBlock) {
			return false;
		}
		return state.is(BlockTags.PLANKS) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)
			|| state.is(Blocks.COBBLESTONE) || state.is(Blocks.COBBLED_DEEPSLATE) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
			|| state.is(Blocks.MOSSY_COBBLESTONE) || state.is(Blocks.STONE_BRICKS) || state.is(Blocks.PACKED_MUD) || state.is(Blocks.MUD_BRICKS);
	}

	/** The slot of the first filler block carried (planks first when {@code planksFirst}), or -1. */
	private static int fillerSlot(Inventory inv, boolean planksFirst) {
		int any = -1;
		for (int i = 0; i < PACK_SLOTS; i++) {
			ItemStack s = inv.getItem(i);
			if (filler(s)) {
				if (!planksFirst || ((BlockItem) s.getItem()).getBlock().defaultBlockState().is(BlockTags.PLANKS)) {
					return i;
				}
				if (any < 0) {
					any = i;
				}
			}
		}
		return any;
	}

	private static int fillers(Inventory inv) {
		int n = 0;
		for (int i = 0; i < PACK_SLOTS; i++) {
			if (filler(inv.getItem(i))) {
				n += inv.getItem(i).getCount();
			}
		}
		return n;
	}

	/** Lays one filler block from the pack at an empty spot. Returns whether it was laid. */
	private static boolean lay(Cast cast, BlockPos pos, boolean planksFirst) {
		ServerPlayer player = player(cast);
		ServerLevel level = cast.level;
		if (!level.getBlockState(pos).isAir()) {
			return false;
		}
		Inventory inv = player.getInventory();
		int slot = fillerSlot(inv, planksFirst);
		if (slot < 0 || !mayBreak(cast, pos)) {
			return false;
		}
		ItemStack stack = inv.getItem(slot);
		BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
		if (!Casters.creative(player)) {
			stack.shrink(1);
			inv.setChanged();
		}
		level.setBlockAndUpdate(pos, state);
		Vfx.emit(level, SpellMaterials.of("earth", 0xE8C890, .1F), Vec3.atCenterOf(pos), 4, .3, .02);
		return true;
	}

	/** Levelground: what stands up to three over the point's height around it is mined away. */
	private static void levelground(Cast cast, Cast.Hit hit, double scale, boolean amp) {
		ServerLevel level = cast.level;
		BlockPos base = target(cast, hit);
		int r = DelveRules.radius(2, scale);
		int cleared = 0;
		for (int y = DelveRules.LEVEL_HEIGHT; y >= 1; y--) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (mine(cast, base.offset(dx, y, dz), pick(amp), tooHard(amp))) {
						cleared++;
					}
				}
			}
		}
		if (cleared > 0) {
			ElementFx.groundRing(level, Vec3.atCenterOf(base).add(0, .5, 0), ElementFx.EARTH.primary(), .5, r + .5, .08, 12);
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(base), cleared > 0 ? "earth_grind" : "fizzle", .7F, .84F);
	}

	/** Holefill: holes around the point filled up to its height with blocks from the pack. */
	private static void holefill(Cast cast, Cast.Hit hit, double scale) {
		ServerLevel level = cast.level;
		BlockPos base = target(cast, hit);
		int r = DelveRules.radius(2, scale);
		int filled = 0;
		for (int y = -3; y <= 0 && filled < DelveRules.FILL_MAX; y++) {
			for (int dx = -r; dx <= r && filled < DelveRules.FILL_MAX; dx++) {
				for (int dz = -r; dz <= r && filled < DelveRules.FILL_MAX; dz++) {
					BlockPos p = base.offset(dx, y, dz);
					BlockState below = level.getBlockState(p.below());
					if (level.getBlockState(p).isAir() && !below.isAir() && below.getFluidState().isEmpty() && lay(cast, p, false)) {
						filled++;
					}
				}
			}
		}
		if (filled == 0 && fillers(player(cast).getInventory()) == 0) {
			tell(cast, "delve_no_blocks");
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(base), filled > 0 ? "earth_press" : "fizzle", .7F, 1F);
	}

	/** Shore Up: hanging sand and gravel near the point propped up with blocks from the pack. */
	private static void shoreup(Cast cast, Cast.Hit hit, double scale) {
		ServerLevel level = cast.level;
		BlockPos center = target(cast, hit);
		int propped = 0;
		for (BlockPos p : around(center, DelveRules.radius(3, scale))) {
			if (propped >= DelveRules.SHORE_MAX) {
				break;
			}
			if (loose(level.getBlockState(p)) && level.getBlockState(p.below()).isAir() && lay(cast, p.below(), false)) {
				propped++;
			}
		}
		if (propped == 0 && fillers(player(cast).getInventory()) == 0) {
			tell(cast, "delve_no_blocks");
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), propped > 0 ? "earth_clamp" : "fizzle", .7F, 1F);
	}

	/** A spell's passing block: written down so it goes even across a restart, and taken away when its time is up. */
	private static void passing(ServerLevel level, BlockPos pos, BlockState placed, int ticks) {
		level.setBlockAndUpdate(pos, placed);
		BlockPos at = pos.immutable();
		TemporaryBlocks.put(level, at, placed, Blocks.AIR.defaultBlockState(), level.getGameTime() + ticks);
		Scheduler.later(ticks, () -> {
			if (level.isLoaded(at)) {
				if (level.getBlockState(at).is(placed.getBlock())) {
					level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
					Vfx.emit(level, SpellMaterials.of("earth", 0x8A6A44, .1F), Vec3.atCenterOf(at), 4, .3, .02);
				}
				TemporaryBlocks.remove(level, at);
			}
		});
	}

	/** Whether the caster may set a passing block at an empty spot (no budget: the caller reserved it whole). */
	private static boolean mayPlace(Cast cast, BlockPos pos) {
		return cast.level.getBlockState(pos).isAir() && cast.admitsBlock(pos) && Casters.mayBuild(cast.caster)
			&& Casters.mayEdit(cast.caster, cast.level, pos);
	}

	/** Stilt: a passing packed-mud column lifts you, up to five. */
	private static void stilt(Cast cast, double duration) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		BlockPos feet = caster.blockPosition();
		if (!level.getBlockState(feet).isAir()) {
			tell(cast, "delve_blocked");
			return;
		}
		int open = 0;
		while (open < DelveRules.STILT_MAX && level.getBlockState(feet.above(2 + open)).isAir()) {
			open++;
		}
		int h = DelveRules.stiltHeight(open);
		List<BlockPos> column = new ArrayList<>();
		for (int i = 0; i < h; i++) {
			if (!mayPlace(cast, feet.above(i))) {
				break;
			}
			column.add(feet.above(i));
		}
		if (column.isEmpty() || !cast.takeBlocks(column.size())) {
			tell(cast, "delve_blocked");
			dev.wildercord.cast.feel.Feels.sound(level, caster.position(), "fizzle", .6F, 1F);
			return;
		}
		Vec3 up = caster.position().add(0, column.size(), 0);
		caster.teleportTo(level, up.x, up.y, up.z, Set.<Relative>of(), caster.getYRot(), caster.getXRot(), false);
		caster.resetFallDistance();
		int ticks = Effects.ticks(30, duration);
		for (BlockPos p : column) {
			passing(level, p, Blocks.PACKED_MUD.defaultBlockState(), ticks);
		}
		Vfx.stream(level, Vec3.atBottomCenterOf(feet), up, Vfx.theme("earth"), 8);
		dev.wildercord.cast.feel.Feels.sound(level, up, "earth_stomp", .7F, 1.12F);
	}

	/** Pit Floor: a passing 3x3 packed-mud floor over the air under the point. */
	private static void pitfloor(Cast cast, Cast.Hit hit, double duration) {
		ServerLevel level = cast.level;
		BlockPos center = hit.self() ? cast.caster.blockPosition().below()
			: hit.block() != null && hit.face() != null ? hit.block().relative(hit.face()) : BlockPos.containing(hit.point());
		List<BlockPos> cells = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos p = center.offset(dx, 0, dz);
				if (mayPlace(cast, p)) {
					cells.add(p);
				}
			}
		}
		if (cells.isEmpty() || !cast.takeBlocks(cells.size())) {
			tell(cast, "delve_blocked");
			dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), "fizzle", .6F, 1F);
			return;
		}
		int ticks = Effects.ticks(20, duration);
		cells.forEach(p -> passing(level, p, Blocks.PACKED_MUD.defaultBlockState(), ticks));
		ElementFx.groundRing(level, Vec3.atCenterOf(center).add(0, .5, 0), ElementFx.EARTH.secondary(), .2, 1.6, .08, 12);
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), "earth_press", .7F, .84F);
	}

	/** Plankway: a lasting one-wide bridge the way you face, from pack blocks. */
	private static void plankway(Cast cast) {
		ServerLevel level = cast.level;
		Direction ahead = cast.caster.getDirection();
		BlockPos floor = cast.caster.blockPosition().below();
		int open = 0;
		while (open < DelveRules.BRIDGE_MAX && level.getBlockState(floor.relative(ahead, open + 1)).isAir()) {
			open++;
		}
		int length = DelveRules.bridgeLength(open, fillers(player(cast).getInventory()));
		int laid = 0;
		for (int i = 1; i <= length; i++) {
			if (!lay(cast, floor.relative(ahead, i), true)) {
				break;
			}
			laid++;
		}
		if (laid == 0) {
			tell(cast, open == 0 ? "delve_blocked" : "delve_no_blocks");
		}
		dev.wildercord.cast.feel.Feels.sound(level, cast.caster.position(), laid > 0 ? "earth_tick" : "fizzle", .7F, 1F);
	}

	/** Floorlay: a 5x5 floor at the point's height, air only, from pack blocks. */
	private static void floorlay(Cast cast, Cast.Hit hit) {
		ServerLevel level = cast.level;
		BlockPos center = target(cast, hit);
		int half = DelveRules.FLOOR_SIDE / 2;
		int laid = 0;
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				if (lay(cast, center.offset(dx, 0, dz), false)) {
					laid++;
				}
			}
		}
		if (laid == 0 && fillers(player(cast).getInventory()) == 0) {
			tell(cast, "delve_no_blocks");
		}
		if (laid > 0) {
			ElementFx.groundRing(level, Vec3.atCenterOf(center).add(0, .5, 0), ElementFx.EARTH.primary(), .2, 2.8, .1, 14);
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), laid > 0 ? "earth_press" : "fizzle", .8F, .84F);
	}

	/** Polish: stone into its polished form (as a stonecutter would). */
	private static final Map<Block, Block> POLISH = Map.of(
		Blocks.ANDESITE, Blocks.POLISHED_ANDESITE, Blocks.GRANITE, Blocks.POLISHED_GRANITE, Blocks.DIORITE, Blocks.POLISHED_DIORITE,
		Blocks.COBBLED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.BASALT, Blocks.POLISHED_BASALT,
		Blocks.TUFF, Blocks.POLISHED_TUFF);
	/** Brickwork: stone into its bricks (as a stonecutter would). */
	private static final Map<Block, Block> BRICK = Map.of(
		Blocks.STONE, Blocks.STONE_BRICKS, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES,
		Blocks.POLISHED_TUFF, Blocks.TUFF_BRICKS, Blocks.END_STONE, Blocks.END_STONE_BRICKS, Blocks.PACKED_MUD, Blocks.MUD_BRICKS,
		Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS);
	/** Agestone: moss on cobblestone and stone bricks, cracks in the deep bricks. */
	private static final Map<Block, Block> AGE = Map.of(
		Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS,
		Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS,
		Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS);

	/** Polish, Brickwork and Agestone: one block for another in place, nine at most. */
	private static void dress(Cast cast, Cast.Hit hit, double scale, Map<Block, Block> into, String sound) {
		ServerLevel level = cast.level;
		BlockPos center = target(cast, hit);
		int done = 0;
		for (BlockPos p : around(center, DelveRules.radius(1, scale))) {
			if (done >= DelveRules.DRESS_MAX) {
				break;
			}
			BlockState state = level.getBlockState(p);
			Block next = into.get(state.getBlock());
			if (next == null || !mayChange(cast, p)) {
				continue;
			}
			level.setBlockAndUpdate(p, next.withPropertiesOf(state));
			Vfx.emit(level, SpellMaterials.of("earth", 0xE8C890, .1F), Vec3.atCenterOf(p), 3, .45, .02);
			done++;
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), done > 0 ? sound : "fizzle", .7F, 1F);
	}

	/** Concrete Set: powder near the point sets to concrete. */
	private static void concreteset(Cast cast, Cast.Hit hit, double scale) {
		ServerLevel level = cast.level;
		BlockPos center = target(cast, hit);
		List<Block> powders = Blocks.CONCRETE_POWDER.asList();
		List<Block> concrete = Blocks.CONCRETE.asList();
		int set = 0;
		for (BlockPos p : around(center, DelveRules.radius(2, scale))) {
			if (set >= DelveRules.SET_MAX) {
				break;
			}
			int i = powders.indexOf(level.getBlockState(p).getBlock());
			if (i < 0 || !mayChange(cast, p)) {
				continue;
			}
			level.setBlockAndUpdate(p, concrete.get(i).defaultBlockState());
			Vfx.emit(level, SpellMaterials.of("frost", 0xE6FAFF, .1F), Vec3.atCenterOf(p), 3, .45, .02);
			set++;
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), set > 0 ? "fire_steam" : "fizzle", .6F, 1.26F);
	}

	/** Chalk Line: a line of light from you to the point for a while, and its length, rise and fall. */
	private static void chalkline(Cast cast, Cast.Hit hit, double duration) {
		ServerLevel level = cast.level;
		Vec3 from = cast.caster.position();
		Vec3 to = hit.self() ? from : hit.point();
		double length = Math.hypot(to.x - from.x, to.z - from.z);
		int rise = (int) Math.round(to.y - from.y);
		tell(cast, "chalkline", Math.round(length), rise);
		int ticks = Effects.ticks(20, duration);
		ShapeRunners.each(cast, ticks, t -> {
			if (t % 10 == 0) {
				Vfx.stream(level, from.add(0, .1, 0), to.add(0, .1, 0), Vfx.theme("arcane"), Math.max(4, (int) length));
			}
			return true;
		});
		dev.wildercord.cast.feel.Feels.sound(level, to, "arcane_stamp", .6F, 1F);
	}

	// ------------------------------------------------------------------ lighting

	private static int carriedTorches(Inventory inv) {
		return carried(inv, Items.TORCH);
	}

	/** Sets a torch from the pack on solid floor at {@code pos}. Returns whether it was set. */
	private static boolean placeTorch(Cast cast, BlockPos pos) {
		ServerPlayer player = player(cast);
		ServerLevel level = cast.level;
		BlockState torch = Blocks.TORCH.defaultBlockState();
		if (!level.getBlockState(pos).isAir() || !torch.canSurvive(level, pos) || !Casters.creative(player) && carriedTorches(player.getInventory()) <= 0) {
			return false;
		}
		if (!mayBreak(cast, pos)) {
			return false;
		}
		if (!Casters.creative(player)) {
			takeOut(player.getInventory(), Items.TORCH, 1);
		}
		level.setBlockAndUpdate(pos, torch);
		ElementFx.embers(level, Vec3.atCenterOf(pos), .2, 3);
		return true;
	}

	/** Torchfall: torches from the pack on the darkest floor around the point, spaced apart. */
	private static void torchfall(Cast cast, Cast.Hit hit, double scale) {
		ServerLevel level = cast.level;
		ServerPlayer player = player(cast);
		BlockPos center = BlockPos.containing(hit.point());
		int r = DelveRules.radius(8, scale);
		List<BlockPos> spots = new ArrayList<>();
		BlockState torch = Blocks.TORCH.defaultBlockState();
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (level.getBlockState(p).isAir() && level.getBrightness(LightLayer.BLOCK, p) <= 7 && torch.canSurvive(level, p)) {
				spots.add(p.immutable());
			}
		}
		spots.sort(Comparator.<BlockPos>comparingInt(p -> level.getBrightness(LightLayer.BLOCK, p)).thenComparingDouble(p -> p.distSqr(center)));
		List<int[]> chosen = new ArrayList<>();
		int set = 0;
		for (BlockPos p : spots) {
			if (set >= DelveRules.TORCH_MAX) {
				break;
			}
			int[] at = {p.getX(), p.getY(), p.getZ()};
			if (!DelveRules.spaced(chosen, at, DelveRules.TORCH_SPACING)) {
				continue;
			}
			if (!Casters.creative(player) && carriedTorches(player.getInventory()) <= 0) {
				break;
			}
			if (placeTorch(cast, p)) {
				chosen.add(at);
				set++;
			}
		}
		if (set == 0 && !Casters.creative(player) && carriedTorches(player.getInventory()) <= 0) {
			tell(cast, "delve_no_torches");
		}
		dev.wildercord.cast.feel.Feels.sound(level, hit.point(), set > 0 ? "fire_kindly" : "fizzle", .7F, 1F);
	}

	/** Gloomsight: motes on the pitch-dark floor around the point, for a while. */
	private static void gloomsight(Cast cast, Cast.Hit hit, double scale, double duration) {
		ServerLevel level = cast.level;
		BlockPos center = BlockPos.containing(hit.point());
		int r = DelveRules.radius(8, scale);
		List<BlockPos> dark = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (level.getBlockState(p).isAir() && level.getRawBrightness(p, 0) == 0 && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
				dark.add(p.immutable());
			}
		}
		dark.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
		List<BlockPos> shown = dark.subList(0, Math.min(32, dark.size()));
		ShapeRunners.each(cast, Effects.ticks(10, duration), t -> {
			if (t % 20 == 0) {
				shown.forEach(p -> Vfx.emit(level, SpellMaterials.of("arcane", 0xFF6A8A, .12F), Vec3.atBottomCenterOf(p).add(0, .1, 0), 2, .2, 0));
			}
			return true;
		});
		dev.wildercord.cast.feel.Feels.sound(level, hit.point(), shown.isEmpty() ? "fizzle" : "arcane_glyph", .6F, 1F);
	}

	/** A passing light block at an empty spot. Returns whether it was set. */
	private static boolean passingLight(Cast cast, BlockPos pos, int ticks, boolean budget) {
		ServerLevel level = cast.level;
		if (!mayPlace(cast, pos) || budget && !cast.takeBlock()) {
			return false;
		}
		passing(level, pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), ticks);
		return true;
	}

	/** Lumen Path: a light every four blocks from you to the point, for a while. */
	private static void lumenpath(Cast cast, Cast.Hit hit, double duration) {
		ServerLevel level = cast.level;
		Vec3 from = cast.caster.getEyePosition();
		Vec3 to = hit.point();
		double length = from.distanceTo(to);
		int n = DelveRules.lumens(length);
		int ticks = Effects.ticks(120, duration);
		int set = 0;
		for (int i = 1; i <= n; i++) {
			Vec3 at = n == 1 ? to : from.lerp(to, Math.min(1.0, i * DelveRules.LUMEN_SPACING / Math.max(length, 1e-6)));
			BlockPos p = BlockPos.containing(at);
			if (passingLight(cast, p, ticks, true)) {
				Vfx.light(level, Vec3.atCenterOf(p));
				set++;
			}
		}
		if (set == 0) {
			Casters.tell(cast.caster, Component.translatable("message.wildercord.light_blocked"));
		}
	}

	/** Snuff Out: fire, campfires and candles around the point go out. */
	private static void snuffout(Cast cast, Cast.Hit hit, double scale) {
		ServerLevel level = cast.level;
		ServerPlayer player = player(cast);
		BlockPos center = BlockPos.containing(hit.point());
		int out = 0;
		for (BlockPos p : around(center, DelveRules.radius(4, scale))) {
			BlockState state = level.getBlockState(p);
			if (state.is(BlockTags.FIRE)) {
				if (mayChange(cast, p)) {
					level.removeBlock(p, false);
					out++;
				}
			} else if (CampfireBlock.isLitCampfire(state)) {
				if (mayChange(cast, p)) {
					CampfireBlock.douse(player, level, p, state);
					level.setBlockAndUpdate(p, state.setValue(CampfireBlock.LIT, false));
					out++;
				}
			} else if (state.getBlock() instanceof AbstractCandleBlock && state.hasProperty(AbstractCandleBlock.LIT) && state.getValue(AbstractCandleBlock.LIT)) {
				if (mayChange(cast, p)) {
					AbstractCandleBlock.extinguish(player, state, level, p);
					out++;
				}
			}
		}
		if (out > 0) {
			Vfx.emit(level, net.minecraft.core.particles.ParticleTypes.SMOKE, hit.point(), 12, .8, .02);
		}
		dev.wildercord.cast.feel.Feels.sound(level, hit.point(), out > 0 ? "fire_out" : "fizzle", .7F, 1F);
	}

	/** Headlamp: a light that follows the target for a while. */
	private static void headlamp(Cast cast, LivingEntity target, double duration) {
		ServerLevel level = cast.level;
		int ticks = Effects.ticks(60, duration);
		BlockPos[] lit = {null};
		Runnable douse = () -> {
			BlockPos old = lit[0];
			if (old != null && level.isLoaded(old)) {
				if (level.getBlockState(old).is(Blocks.LIGHT)) {
					level.setBlockAndUpdate(old, Blocks.AIR.defaultBlockState());
				}
				TemporaryBlocks.remove(level, old);
			}
			lit[0] = null;
		};
		ShapeRunners.each(cast, ticks + 1, t -> {
			if (t >= ticks || !target.isAlive() || target.level() != level) {
				douse.run();
				return false;
			}
			if (t % 5 != 0) {
				return true;
			}
			BlockPos here = BlockPos.containing(target.getEyePosition());
			if (here.equals(lit[0])) {
				return true;
			}
			douse.run();
			BlockState state = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 14);
			ServerPlayer player = player(cast);
			if (player != null && level.getBlockState(here).isAir() && cast.admitsBlock(here) && Casters.mayBuild(player) && level.mayInteract(player, here)
				&& !Effects.isTemporary(level, here) && Casters.probeBreak(level, player, here, level.getBlockState(here), null)) {
				level.setBlockAndUpdate(here, state);
				// Restored with the rest of the passing blocks if the server stops while it shines.
				TemporaryBlocks.put(level, here, state, Blocks.AIR.defaultBlockState(), level.getGameTime() + Math.max(20, ticks - t));
				lit[0] = here;
			}
			return true;
		});
		Vfx.light(level, target.getEyePosition());
	}

	// ------------------------------------------------------------------ light redstone

	/** Lever Flip, Button Push and Doorcall: the switches near the point, worked as a hand would. */
	private static void switches(Cast cast, Cast.Hit hit, double scale, String which) {
		ServerLevel level = cast.level;
		ServerPlayer player = player(cast);
		BlockPos center = target(cast, hit);
		int worked = 0;
		for (BlockPos p : around(center, DelveRules.radius(3, scale))) {
			if (worked >= DelveRules.SWITCH_MAX) {
				break;
			}
			BlockState state = level.getBlockState(p);
			Block block = state.getBlock();
			boolean fits = switch (which) {
				case "leverflip" -> block instanceof LeverBlock;
				case "buttonpush" -> block instanceof ButtonBlock && !state.getValue(ButtonBlock.POWERED);
				default -> block instanceof DoorBlock door && door.type().canOpenByHand() && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
					|| block instanceof TrapDoorBlock || block instanceof FenceGateBlock;
			};
			if (!fits || !mayUse(cast, p)) {
				continue;
			}
			BlockHitResult press = new BlockHitResult(Vec3.atCenterOf(p), Direction.UP, p, false);
			if (state.useWithoutItem(level, player, press).consumesAction() && !level.getBlockState(p).equals(state)) {
				worked++;
				ElementFx.sparks(level, Vec3.atCenterOf(p), 3, .08);
			}
		}
		dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(center), worked > 0 ? "storm_tick" : "fizzle", .6F, 1F);
	}

	// ------------------------------------------------------------------ storage and hauling

	/** The chest, barrel or shulker box at {@code pos}, opened as the caster would open it; null when it isn't theirs to open. */
	private static BaseContainerBlockEntity box(Cast cast, BlockPos pos, boolean unpack) {
		ServerPlayer player = player(cast);
		if (pos == null || player == null) {
			return null;
		}
		BlockEntity be = cast.level.getBlockEntity(pos);
		if (!(be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity)) {
			return null;
		}
		BaseContainerBlockEntity box = (BaseContainerBlockEntity) be;
		if (!mayUse(cast, pos) || !box.canOpen(player)) {
			return null;
		}
		if (box instanceof RandomizableContainer loot && loot.getLootTable() != null) {
			if (!unpack) {
				return null;
			}
			loot.unpackLootTable(player);
		}
		return box;
	}

	private static BaseContainerBlockEntity aimed(Cast cast, Cast.Hit hit) {
		BaseContainerBlockEntity box = box(cast, hit.block(), true);
		if (box == null) {
			tell(cast, "delve_no_chest");
			dev.wildercord.cast.feel.Feels.sound(cast.level, hit.point(), "fizzle", .6F, 1F);
		}
		return box;
	}

	/** Puts as much of {@code stack} into the container as it takes (topping up like stacks first); what's left stays in {@code stack}. */
	private static int insert(Container box, ItemStack stack, boolean onlyKnown) {
		int before = stack.getCount();
		if (onlyKnown) {
			boolean known = false;
			for (int i = 0; i < box.getContainerSize() && !known; i++) {
				known = ItemStack.isSameItemSameComponents(box.getItem(i), stack);
			}
			if (!known) {
				return 0;
			}
		}
		for (int i = 0; i < box.getContainerSize() && !stack.isEmpty(); i++) {
			ItemStack there = box.getItem(i);
			if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, stack) && there.getCount() < there.getMaxStackSize()) {
				int n = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
				there.grow(n);
				stack.shrink(n);
			}
		}
		for (int i = 0; i < box.getContainerSize() && !stack.isEmpty(); i++) {
			if (box.getItem(i).isEmpty() && box.canPlaceItem(i, stack)) {
				box.setItem(i, stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
			}
		}
		box.setChanged();
		return before - stack.getCount();
	}

	private static void sortedFx(Cast cast, BlockPos pos, int moved, String key) {
		if (moved > 0) {
			Vfx.stream(cast.level, cast.caster.getEyePosition(), Vec3.atCenterOf(pos), Vfx.theme("void"), 6);
			tell(cast, key, moved);
		}
		dev.wildercord.cast.feel.Feels.sound(cast.level, Vec3.atCenterOf(pos), moved > 0 ? "void_collect_suck" : "fizzle", .6F, 1F);
	}

	/** Chest Sort: like items joined and laid out in order. */
	private static void chestsort(Cast cast, Cast.Hit hit) {
		BaseContainerBlockEntity box = aimed(cast, hit);
		if (box == null) {
			return;
		}
		Map<String, List<ItemStack>> groups = new java.util.TreeMap<>();
		List<ItemStack> all = new ArrayList<>();
		for (int i = 0; i < box.getContainerSize(); i++) {
			ItemStack s = box.getItem(i);
			if (!s.isEmpty()) {
				all.add(s.copy());
			}
		}
		// Grouped by item id, then by exactly-alike components (one key per distinct stack kind, numbered in first-seen order).
		List<ItemStack> kinds = new ArrayList<>();
		for (ItemStack s : all) {
			int k = -1;
			for (int j = 0; j < kinds.size(); j++) {
				if (ItemStack.isSameItemSameComponents(kinds.get(j), s)) {
					k = j;
					break;
				}
			}
			if (k < 0) {
				kinds.add(s.copyWithCount(1));
				k = kinds.size() - 1;
			}
			String key = BuiltInRegistries.ITEM.getKey(s.getItem()) + "#" + String.format("%04d", k);
			groups.computeIfAbsent(key, x -> new ArrayList<>()).add(s);
		}
		List<ItemStack> laid = new ArrayList<>();
		for (List<ItemStack> group : groups.values()) {
			int total = group.stream().mapToInt(ItemStack::getCount).sum();
			for (int n : DelveRules.stacks(total, group.get(0).getMaxStackSize())) {
				laid.add(group.get(0).copyWithCount(n));
			}
		}
		if (laid.size() > box.getContainerSize()) {
			// Never: joining only ever needs fewer slots. Left as it was rather than lose anything.
			return;
		}
		for (int i = 0; i < box.getContainerSize(); i++) {
			box.setItem(i, i < laid.size() ? laid.get(i) : ItemStack.EMPTY);
		}
		box.setChanged();
		motes(cast.level, Vec3.atCenterOf(hit.block()).add(0, .6, 0), "void", 0xE0B0FF, 8);
		dev.wildercord.cast.feel.Feels.sound(cast.level, Vec3.atCenterOf(hit.block()), "void_warp_ping", .6F, 1.12F);
	}

	/** Stow: pack items the chest already holds go into it; the hotbar and armour stay. */
	private static void stow(Cast cast, Cast.Hit hit) {
		BaseContainerBlockEntity box = aimed(cast, hit);
		if (box == null) {
			return;
		}
		Inventory inv = player(cast).getInventory();
		int moved = 0;
		for (int i = HOTBAR_SLOTS; i < PACK_SLOTS; i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty()) {
				moved += insert(box, s, true);
			}
		}
		inv.setChanged();
		sortedFx(cast, hit.block(), moved, "delve_stowed");
	}

	/** Unburden: the whole pack into the chest, as far as it has room; the hotbar and armour stay. */
	private static void unburden(Cast cast, Cast.Hit hit) {
		BaseContainerBlockEntity box = aimed(cast, hit);
		if (box == null) {
			return;
		}
		Inventory inv = player(cast).getInventory();
		int moved = 0;
		for (int i = HOTBAR_SLOTS; i < PACK_SLOTS; i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty()) {
				moved += insert(box, s, false);
			}
		}
		inv.setChanged();
		sortedFx(cast, hit.block(), moved, "delve_stowed");
	}

	/** Restock: hotbar stacks topped up from the chest. */
	private static void restock(Cast cast, Cast.Hit hit) {
		BaseContainerBlockEntity box = aimed(cast, hit);
		if (box == null) {
			return;
		}
		Inventory inv = player(cast).getInventory();
		int moved = 0;
		for (int i = 0; i < HOTBAR_SLOTS; i++) {
			ItemStack mine = inv.getItem(i);
			if (mine.isEmpty() || mine.getCount() >= mine.getMaxStackSize()) {
				continue;
			}
			for (int j = 0; j < box.getContainerSize() && mine.getCount() < mine.getMaxStackSize(); j++) {
				ItemStack there = box.getItem(j);
				if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, mine)) {
					int n = Math.min(there.getCount(), mine.getMaxStackSize() - mine.getCount());
					there.shrink(n);
					mine.grow(n);
					moved += n;
				}
			}
		}
		box.setChanged();
		inv.setChanged();
		sortedFx(cast, hit.block(), moved, "delve_restocked");
	}

	/** Stocktake: the biggest stocks across the chests near the point. */
	private static void stocktake(Cast cast, Cast.Hit hit, double scale) {
		ServerLevel level = cast.level;
		BlockPos center = BlockPos.containing(hit.point());
		int r = DelveRules.radius(4, scale);
		Map<Item, Integer> totals = new LinkedHashMap<>();
		int boxes = 0;
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			BaseContainerBlockEntity box = box(cast, p.immutable(), false);
			if (box == null) {
				continue;
			}
			boxes++;
			for (int i = 0; i < box.getContainerSize(); i++) {
				ItemStack s = box.getItem(i);
				if (!s.isEmpty()) {
					totals.merge(s.getItem(), s.getCount(), Integer::sum);
				}
			}
		}
		if (boxes == 0 || totals.isEmpty()) {
			tell(cast, "stocktake_none");
			dev.wildercord.cast.feel.Feels.sound(level, hit.point(), "fizzle", .6F, 1F);
			return;
		}
		List<Map.Entry<Item, Integer>> rows = new ArrayList<>(totals.entrySet());
		rows.sort(Map.Entry.<Item, Integer>comparingByValue().reversed());
		MutableComponent list = Component.empty();
		for (int i = 0; i < Math.min(5, rows.size()); i++) {
			if (i > 0) {
				list.append(", ");
			}
			list.append(rows.get(i).getValue() + " ").append(new ItemStack(rows.get(i).getKey()).getHoverName());
		}
		Casters.tell(cast.caster, Component.translatable("message.wildercord.stocktake", list));
		ElementFx.groundRing(level, hit.point(), ElementFx.ARCANE.secondary(), .3, r, .05, 14);
		dev.wildercord.cast.feel.Feels.sound(level, hit.point(), "arcane_glint", .6F, 1F);
	}

	/** Pack Tidy: like stacks in the pack joined; the hotbar is left alone. */
	private static void packtidy(Cast cast) {
		ServerPlayer player = player(cast);
		Inventory inv = player.getInventory();
		int joined = 0;
		for (int i = HOTBAR_SLOTS; i < PACK_SLOTS; i++) {
			ItemStack a = inv.getItem(i);
			if (a.isEmpty() || a.getCount() >= a.getMaxStackSize()) {
				continue;
			}
			for (int j = i + 1; j < PACK_SLOTS && a.getCount() < a.getMaxStackSize(); j++) {
				ItemStack b = inv.getItem(j);
				if (!b.isEmpty() && ItemStack.isSameItemSameComponents(a, b)) {
					int n = Math.min(b.getCount(), a.getMaxStackSize() - a.getCount());
					a.grow(n);
					b.shrink(n);
					joined += n;
				}
			}
		}
		inv.setChanged();
		dev.wildercord.cast.feel.Feels.sound(cast.level, player.position(), joined > 0 ? "void_warp_ping" : "fizzle", .5F, 1.26F);
		if (joined > 0) {
			motes(cast.level, player.position().add(0, 1, 0), "void", 0xE0B0FF, 6);
		}
	}

	/** Whether a loose item may be pulled to {@code to}: settled, and no one else's drop or reserved pickup. */
	private static boolean pullable(ItemEntity item, LivingEntity to) {
		if (!item.isAlive() || item.hasPickUpDelay()) {
			return false;
		}
		var ownership = (ItemEntityAccessor) item;
		return (ownership.wildercord$target() == null || ownership.wildercord$target().equals(to.getUUID()))
			&& (ownership.wildercord$thrower() == null || ownership.wildercord$thrower().getUUID().equals(to.getUUID()));
	}

	/** Lodepull: loose drops around the target drift to it for a while. */
	private static void lodepull(Cast cast, LivingEntity target, double scale, double duration) {
		ServerLevel level = cast.level;
		double reach = DelveRules.radius(6, scale);
		ShapeRunners.each(cast, Effects.ticks(30, duration), t -> {
			if (!target.isAlive() || target.level() != level) {
				return false;
			}
			if (t % 4 != 0) {
				return true;
			}
			Vec3 to = target.position().add(0, .3, 0);
			int pulled = 0;
			for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(target.blockPosition()).inflate(reach), i -> pullable(i, target))) {
				if (pulled++ >= 16) {
					break;
				}
				Vec3 step = to.subtract(item.position());
				double d = step.length();
				Vec3 next = d <= 1.2 ? to : item.position().add(step.scale(1.2 / d));
				item.setPos(next.x, next.y, next.z);
				item.setDeltaMovement(Vec3.ZERO);
				if (t % 12 == 0) {
					Vfx.emit(level, SpellMaterials.of("void", 0xE0B0FF, .08F), item.position(), 1, .05, 0);
				}
			}
			return true;
		});
		ElementFx.groundRing(level, target.position(), ElementFx.VOID.primary(), reach, .4, .05, 10);
		dev.wildercord.cast.feel.Feels.sound(level, target.position(), "storm_magnet", .6F, 1F);
	}

	// ------------------------------------------------------------------ safety and return

	/** Caveward: sand, gravel or powder snow burying the target's head crumbles into items, for a while. */
	private static void caveward(Cast cast, LivingEntity target, double duration) {
		ServerLevel level = cast.level;
		ShapeRunners.each(cast, Effects.ticks(60, duration), t -> {
			if (!target.isAlive() || target.level() != level) {
				return false;
			}
			if (t % 5 != 0) {
				return true;
			}
			BlockPos head = BlockPos.containing(target.getEyePosition());
			BlockState state = level.getBlockState(head);
			if ((loose(state) || state.is(Blocks.POWDER_SNOW)) && cast.admitsBlock(head) && Casters.mayBuild(cast.caster)
				&& !Effects.isTemporary(level, head) && Casters.mayEdit(cast.caster, level, head)) {
				level.destroyBlock(head, true, cast.caster);
				Vfx.emit(level, SpellMaterials.of("earth", 0xE3C98E, .12F), Vec3.atCenterOf(head), 8, .3, .03);
				dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(head), "earth_crack", .7F, 1.12F);
			}
			return true;
		});
		motes(level, target.position().add(0, 1.6, 0), "earth", 0xE8C890, 8);
		dev.wildercord.cast.feel.Feels.sound(level, target.position(), "earth_clamp", .5F, 1.12F);
	}

	/** A Delvemark: where you were, in which world, until when. Kept only while the server runs. */
	private record Mark(MinecraftServer server, ResourceKey<Level> dimension, Vec3 at, long until) {}

	private static final Map<UUID, Mark> MARKS = new HashMap<>();
	private static final Set<UUID> RETURNING = new HashSet<>();

	/** Delvemark: the first cast marks your spot; the next one, near enough, calls you back after a still moment. */
	private static void delvemark(Cast cast) {
		ServerPlayer player = player(cast);
		ServerLevel level = cast.level;
		UUID id = player.getUUID();
		Mark mark = MARKS.get(id);
		long now = level.getGameTime();
		boolean usable = mark != null && mark.server() == level.getServer() && mark.dimension() == level.dimension() && now <= mark.until();
		Vec3 here = player.position();
		if (!usable || here.distanceToSqr(mark.at()) < 9) {
			MARKS.put(id, new Mark(level.getServer(), level.dimension(), here, now + DelveRules.MARK_TICKS));
			tell(cast, "delvemark_set");
			ElementFx.groundRing(level, here, ElementFx.VOID.primary(), .2, 1.2, .06, 20);
			dev.wildercord.cast.feel.Feels.sound(level, here, "void_warp_ping", .6F, .84F);
			return;
		}
		Vec3 to = mark.at();
		if (!DelveRules.markReach(to.x - here.x, to.y - here.y, to.z - here.z)) {
			tell(cast, "delvemark_far");
			dev.wildercord.cast.feel.Feels.sound(level, here, "fizzle", .6F, 1F);
			return;
		}
		if (!RETURNING.add(id)) {
			return;
		}
		tell(cast, "delvemark_hold");
		float health = player.getHealth();
		ShapeRunners.each(cast, DelveRules.MARK_WARMUP + 1, t -> {
			boolean broken = !player.isAlive() || player.level() != level || player.position().distanceToSqr(here) > .25 || player.getHealth() < health;
			if (broken) {
				RETURNING.remove(id);
				tell(cast, "delvemark_broken");
				dev.wildercord.cast.feel.Feels.sound(level, player.position(), "fizzle", .6F, 1F);
				return false;
			}
			if (t < DelveRules.MARK_WARMUP) {
				if (t % 5 == 0) {
					ElementFx.groundRing(level, player.position(), ElementFx.VOID.primary(), 1.2, .2, .05, 6);
				}
				return true;
			}
			RETURNING.remove(id);
			MARKS.remove(id);
			Vfx.emit(level, SpellMaterials.of("void", 0xB45AF0, .16F), player.position().add(0, 1, 0), 16, .4, .05);
			player.teleportTo(level, to.x, to.y, to.z, Set.<Relative>of(), player.getYRot(), player.getXRot(), false);
			player.resetFallDistance();
			Vfx.emit(level, SpellMaterials.of("void", 0xE0B0FF, .16F), to.add(0, 1, 0), 16, .4, .05);
			dev.wildercord.cast.feel.Feels.sound(level, to, "void_warp_ping", .8F, 1.12F);
			return false;
		});
		// If the cast ends before the moment is out, the return is simply off: nothing waits on it.
		Scheduler.later(DelveRules.MARK_WARMUP + 5, () -> RETURNING.remove(id));
	}

	/** Forgets a player's mark (for tests). */
	static void forgetMark(Entity player) {
		MARKS.remove(player.getUUID());
		RETURNING.remove(player.getUUID());
	}
}
