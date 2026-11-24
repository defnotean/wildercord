package dev.wildercord.ocean;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Registers Deep Ocean architectural & functional blocks and BlockEntity types.
 */
public final class OceanBlocks {
	private OceanBlocks() {}

	private static ResourceKey<Block> key(String id) {
		return ResourceKey.create(Registries.BLOCK, Wildercord.id(id));
	}

	public static final Block SIREN_PEARL_LANTERN = Registry.register(
		BuiltInRegistries.BLOCK,
		key("siren_pearl_lantern"),
		new SirenPearlLanternBlock(BlockBehaviour.Properties.of()
			
			.mapColor(MapColor.COLOR_MAGENTA)
			.strength(2.0f, 6.0f)
			.sound(SoundType.LANTERN)
			.lightLevel(s -> 15)
			.noOcclusion())
	);

	public static final Block TRENCH_ANVIL = Registry.register(
		BuiltInRegistries.BLOCK,
		key("trench_anvil"),
		new TrenchAnvilBlock(BlockBehaviour.Properties.of()
			
			.mapColor(MapColor.COLOR_BLACK)
			.strength(5.0f, 1200.0f)
			.requiresCorrectToolForDrops()
			.sound(SoundType.ANVIL)
			.lightLevel(s -> 7)
			.noOcclusion())
	);

	public static final Block SOVEREIGN_ALTAR = Registry.register(
		BuiltInRegistries.BLOCK,
		key("sovereign_altar"),
		new SovereignAltarBlock(BlockBehaviour.Properties.of()
			
			.mapColor(MapColor.COLOR_CYAN)
			.strength(50.0f, 1200.0f)
			.requiresCorrectToolForDrops()
			.sound(SoundType.DEEPSLATE_TILES)
			.lightLevel(s -> 12))
	);

	public static final BlockEntityType<SovereignAltarBlockEntity> SOVEREIGN_ALTAR_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Wildercord.id("sovereign_altar"),
		FabricBlockEntityTypeBuilder.create(SovereignAltarBlockEntity::new, SOVEREIGN_ALTAR).build()
	);

	public static void init() {
		// Class initialization triggers registration.
	}
}
