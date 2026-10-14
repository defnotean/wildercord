package dev.wildercord.world.kilnwidow;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.Optional;

public final class KilnWidowContent {
	private KilnWidowContent() {}
	private static final ResourceKey<Block> HEARTSTONE_KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("kiln_heartstone"));
	public static final KilnHeartstoneBlock HEARTSTONE = Registry.register(BuiltInRegistries.BLOCK, HEARTSTONE_KEY,
		new KilnHeartstoneBlock(BlockBehaviour.Properties.of().setId(HEARTSTONE_KEY).strength(-1, 3600000).noOcclusion().noLootTable().lightLevel(s -> 9).sound(SoundType.DEEPSLATE_BRICKS)));
	public static final Item HEARTSTONE_ITEM = Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Wildercord.id("kiln_heartstone")),
		new BlockItem(HEARTSTONE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Wildercord.id("kiln_heartstone"))).useBlockDescriptionPrefix()));
	public static final BlockEntityType<KilnHeartstoneEntity> HEARTSTONE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildercord.id("kiln_heartstone"),
		FabricBlockEntityTypeBuilder.create(KilnHeartstoneEntity::new, HEARTSTONE).build());
	private static final ResourceKey<Item> COMPASS_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("cooled_kiln_compass"));
	public static final Item COMPASS = Registry.register(BuiltInRegistries.ITEM, COMPASS_KEY,
		new AuraWorld.Lore("cooled_kiln_compass", new Item.Properties().setId(COMPASS_KEY).stacksTo(1).rarity(Rarity.UNCOMMON)));
	private static final ResourceKey<EntityType<?>> ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("kiln_widow"));
	public static final EntityType<KilnWidow> WIDOW = Registry.register(BuiltInRegistries.ENTITY_TYPE, ENTITY_KEY,
		EntityType.Builder.of(KilnWidow::new, MobCategory.MONSTER).sized(2.8F, 3.2F).eyeHeight(2.4F).clientTrackingRange(14).build(ENTITY_KEY));

	public static ItemStack compassTo(ServerLevel level, BlockPos pos) {
		var stack = new ItemStack(COMPASS);
		stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), pos.immutable())), false));
		return stack;
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(WIDOW, KilnWidow.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:kiln_widow", FieldGuide.Group.MONSTER, 0xC86B3C));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(o -> {
			o.accept(HEARTSTONE_ITEM);
			o.accept(COMPASS);
		});
	}
}
