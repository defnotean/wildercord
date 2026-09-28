package dev.wildercord.cast.events;

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
import net.minecraft.world.level.material.PushReaction;

/**
 * The world events' blocks: only the Fallen Star, which has no item (it's found, never carried) and
 * can't be mined, blown up or pushed.
 */
public final class EventContent {
	private EventContent() {}

	public static final FallenStarBlock FALLEN_STAR = register("fallen_star", BlockBehaviour.Properties.of()
		.mapColor(MapColor.COLOR_LIGHT_BLUE)
		.sound(SoundType.AMETHYST)
		.strength(-1.0F, 3600000.0F)
		.noLootTable()
		.noOcclusion()
		.pushReaction(PushReaction.IMMOVEABLE)
		.lightLevel(state -> 15));

	public static final BlockEntityType<FallenStarBlockEntity> FALLEN_STAR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Wildercord.id("fallen_star"), FabricBlockEntityTypeBuilder.create(FallenStarBlockEntity::new, FALLEN_STAR).build());

	private static FallenStarBlock register(String path, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Wildercord.id(path));
		return Registry.register(BuiltInRegistries.BLOCK, key, new FallenStarBlock(properties.setId(key)));
	}

	public static void init() {}
}
