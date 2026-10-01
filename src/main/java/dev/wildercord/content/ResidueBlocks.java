package dev.wildercord.content;

import dev.wildercord.Wildercord;
import dev.wildercord.world.ResidueRules.Kind;
import net.fabricmc.fabric.api.registry.LandPathTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.EnumMap;
import java.util.Map;

/**
 * The residues' blocks, one for each element's mark (see {@link dev.wildercord.world.ResidueRules}). None
 * has an item: they're only ever left by magic, and each gives its reagent when harvested (its loot table).
 * A residue that took the place of the ground (everfrost, riven stone, a void scar) can't be pushed by a
 * piston, so the record of what it replaced always stands where it does; one lying on the ground goes the
 * way a flower would.
 */
public final class ResidueBlocks {
	private ResidueBlocks() {}

	private static final Map<Kind, ResidueBlock> BY_KIND = new EnumMap<>(Kind.class);

	public static final ResidueBlock SMOULDERING_ASH = register(Kind.SMOULDERING_ASH, props(MapColor.COLOR_GRAY, SoundType.SAND, 0.2F, 6).noCollision());
	public static final ResidueBlock EVERFROST = register(Kind.EVERFROST, props(MapColor.ICE, SoundType.GLASS, 0.9F, 2).friction(0.98F));
	public static final ResidueBlock FULGURITE = register(Kind.FULGURITE, props(MapColor.COLOR_YELLOW, SoundType.GLASS, 0.4F, 5).noCollision().noOcclusion());
	public static final ResidueBlock LINGERING_EDDY = register(Kind.LINGERING_EDDY, props(MapColor.NONE, SoundType.WOOL, 0.0F, 0).noCollision().noOcclusion()
		.replaceable());
	public static final ResidueBlock RIVEN_STONE = register(Kind.RIVEN_STONE, props(MapColor.DIRT, SoundType.TUFF, 0.6F, 3));
	public static final ResidueBlock WILDBLOOM = register(Kind.WILDBLOOM, props(MapColor.COLOR_PINK, SoundType.GRASS, 0.0F, 6).noCollision()
		.offsetType(BlockBehaviour.OffsetType.XZ));
	public static final ResidueBlock VOID_SCAR = register(Kind.VOID_SCAR, props(MapColor.COLOR_BLACK, SoundType.SCULK, 0.8F, 1));
	public static final ResidueBlock STAR_GLYPH = register(Kind.STAR_GLYPH, props(MapColor.COLOR_MAGENTA, SoundType.AMETHYST, 0.1F, 7).noCollision());
	public static final ResidueBlock STILLED_SAND = register(Kind.STILLED_SAND, props(MapColor.SAND, SoundType.SAND, 0.2F, 4).noCollision());
	public static final ResidueBlock BLOODMOSS = register(Kind.BLOODMOSS, props(MapColor.CRIMSON_NYLIUM, SoundType.MOSS_CARPET, 0.1F, 2).noCollision());

	private static BlockBehaviour.Properties props(MapColor color, SoundType sound, float strength, int light) {
		return BlockBehaviour.Properties.of().mapColor(color).sound(sound).strength(strength, strength * 2).lightLevel(s -> light);
	}

	private static ResidueBlock register(Kind kind, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Wildercord.id(kind.path));
		properties.setId(key).pushReaction(kind.placement == dev.wildercord.world.ResidueRules.Placement.COVER ? PushReaction.IMMOVEABLE : PushReaction.POPPED);
		ResidueBlock block = Registry.register(BuiltInRegistries.BLOCK, key, new ResidueBlock(kind, properties));
		BY_KIND.put(kind, block);
		return block;
	}

	/** The block of one element's mark. */
	public static ResidueBlock of(Kind kind) {
		return BY_KIND.get(kind);
	}

	/** The mark a block is, or null if it isn't one. */
	public static Kind kindOf(BlockState state) {
		return state.getBlock() instanceof ResidueBlock residue ? residue.kind : null;
	}

	public static boolean is(BlockState state) {
		return state.getBlock() instanceof ResidueBlock;
	}

	public static void init() {
		// Animals (any mob that paths) keep off a void scar and step round it, and round a patch of bloodmoss.
		LandPathTypeRegistry.register(VOID_SCAR, PathType.DAMAGING, PathType.DAMAGING_IN_NEIGHBOR);
		LandPathTypeRegistry.register(BLOODMOSS, PathType.DAMAGING_IN_NEIGHBOR, PathType.DAMAGING_IN_NEIGHBOR);
	}
}
