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
			.setId(ALTAR_KEY)));

	public static final BlockEntityType<DungeonAltarBlockEntity> ALTAR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Wildercord.id("dungeon_altar"), FabricBlockEntityTypeBuilder.create(DungeonAltarBlockEntity::new, ALTAR).build());

	static {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Wildercord.id("dungeon_altar"));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(ALTAR, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().rarity(Rarity.EPIC)));
	}

	public static void init() {}
	public static final ExpeditionMechanism CLOCK = mechanism("clockwork_control",ExpeditionMechanism.Kind.CLOCK);
	public static final ExpeditionMechanism GARDEN = mechanism("greenhouse_heart",ExpeditionMechanism.Kind.GARDEN);
	public static final ExpeditionMechanism SKY = mechanism("sky_anchor",ExpeditionMechanism.Kind.SKY);
	public static final BlockEntityType<ExpeditionMechanismEntity> MECHANISM_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Wildercord.id("expedition_mechanism"),FabricBlockEntityTypeBuilder.create(ExpeditionMechanismEntity::new,CLOCK,GARDEN,SKY).build());
	private static ExpeditionMechanism mechanism(String path,ExpeditionMechanism.Kind kind) {
		var key=ResourceKey.create(Registries.BLOCK,Wildercord.id(path));
		return Registry.register(BuiltInRegistries.BLOCK,key,new ExpeditionMechanism(kind,BlockBehaviour.Properties.of().setId(key)
			.mapColor(MapColor.COLOR_PURPLE).sound(SoundType.AMETHYST).strength(-1,3600000).noLootTable().lightLevel(s->8)));
	}
}
