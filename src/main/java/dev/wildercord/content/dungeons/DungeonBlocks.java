package dev.wildercord.content.dungeons;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The dungeon altar (see {@link DungeonAltarBlock}): one block, three kinds. */
public final class DungeonBlocks {
	private DungeonBlocks() {}

	private static final ResourceKey<net.minecraft.world.level.block.Block> ALTAR_KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("dungeon_altar"));

	public static final DungeonAltarBlock ALTAR = Registry.register(BuiltInRegistries.BLOCK, ALTAR_KEY,
		new DungeonAltarBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLACK)
			.sound(SoundType.POLISHED_DEEPSLATE)
			.strength(-1.0F, 3600000.0F)
			.noLootTable()
			.noOcclusion()
			.lightLevel(state -> state.getValue(DungeonAltarBlock.AWAKE) ? 4 : 10)
			);
	}
}
