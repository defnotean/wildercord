package dev.wildercord.content;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;

/**
 * Wildercord's blocks: the Wellstone (a mana well that wakes on a ley line), the Rune Seal
 * (the Archive's element-locked doors) and the Archive Lectern (where the Archivist waits).
 */
public final class WildercordBlocks {
	private WildercordBlocks() {}

	public static final WellstoneBlock WELLSTONE = register("wellstone", WellstoneBlock::new, BlockBehaviour.Properties.of()
		.mapColor(MapColor.COLOR_PURPLE)
		.instrument(NoteBlockInstrument.BASEDRUM)
		.sound(SoundType.AMETHYST)
		.strength(3.5F, 9.0F)
		.requiresCorrectToolForDrops()
		.lightLevel(state -> state.getValue(WellstoneBlock.ACTIVE) ? 12 : 3), Rarity.UNCOMMON);

	public static final RuneSealBlock RUNE_SEAL = register("rune_seal", RuneSealBlock::new, BlockBehaviour.Properties.of()
		.mapColor(MapColor.DEEPSLATE)
		.instrument(NoteBlockInstrument.BASEDRUM)
		.sound(SoundType.DEEPSLATE_TILES)
		.strength(-1.0F, 3600000.0F)
		.noLootTable()
		.lightLevel(state -> state.getValue(RuneSealBlock.LIT) ? 11 : 4), Rarity.EPIC);

	public static final ArchiveLecternBlock ARCHIVE_LECTERN = register("archive_lectern", ArchiveLecternBlock::new, BlockBehaviour.Properties.of()
		.mapColor(MapColor.COLOR_BLACK)
		.sound(SoundType.CHISELED_BOOKSHELF)
		.strength(-1.0F, 3600000.0F)
		.noLootTable()
		.noOcclusion()
		.lightLevel(state -> state.getValue(ArchiveLecternBlock.AWAKE) ? 4 : 10), Rarity.EPIC);

	public static final FusionAltarBlock FUSION_ALTAR = register("fusion_altar", FusionAltarBlock::new, BlockBehaviour.Properties.of()
		.mapColor(MapColor.COLOR_PURPLE)
		.instrument(NoteBlockInstrument.BASEDRUM)
		.sound(SoundType.AMETHYST)
		.strength(3.5F, 9.0F)
		.requiresCorrectToolForDrops()
		.noOcclusion()
		.lightLevel(state -> 7), Rarity.UNCOMMON);

	public static final dev.wildercord.duel.ArenaStoneBlock ARENA_STONE = register("arena_stone", dev.wildercord.duel.ArenaStoneBlock::new, BlockBehaviour.Properties.of()
		.mapColor(MapColor.DEEPSLATE)
		.instrument(NoteBlockInstrument.BASEDRUM)
		.sound(SoundType.DEEPSLATE_BRICKS)
		.strength(3.5F, 9.0F)
		.requiresCorrectToolForDrops()
		.lightLevel(state -> 5), Rarity.UNCOMMON);

	public static final BlockEntityType<WellstoneBlockEntity> WELLSTONE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Wildercord.id("wellstone"), FabricBlockEntityTypeBuilder.create(WellstoneBlockEntity::new, WELLSTONE).build());

	public static final BlockEntityType<ArchiveLecternBlockEntity> ARCHIVE_LECTERN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Wildercord.id("archive_lectern"), FabricBlockEntityTypeBuilder.create(ArchiveLecternBlockEntity::new, ARCHIVE_LECTERN).build());

	private static <T extends Block> T register(String path, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties, Rarity rarity) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Wildercord.id(path));
		T block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Wildercord.id(path));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().rarity(rarity)));
		return block;
	}

	public static final RunicHearthBlock RUNIC_HEARTH=register("runic_hearth",RunicHearthBlock::new,BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.AMETHYST).strength(3.5F,9).lightLevel(s->3+s.getValue(RunicHearthBlock.CHARGE)*4),Rarity.UNCOMMON);
	public static final BlockEntityType<RunicHearthEntity> HEARTH_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Wildercord.id("runic_hearth"),FabricBlockEntityTypeBuilder.create(RunicHearthEntity::new,RUNIC_HEARTH).build());
	public static void init() {}
}
