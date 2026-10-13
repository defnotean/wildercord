package dev.wildercord.world.hollowbell;

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

public final class HollowBellContent {
	private HollowBellContent() {}
	private static final ResourceKey<Block> BELFRY_KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("listening_belfry"));
	public static final ListeningBelfryBlock BELFRY = Registry.register(BuiltInRegistries.BLOCK, BELFRY_KEY,
		new ListeningBelfryBlock(BlockBehaviour.Properties.of().setId(BELFRY_KEY).strength(-1, 3600000).noOcclusion().noLootTable().lightLevel(s -> 9).sound(SoundType.COPPER)));
	public static final Item BELFRY_ITEM = Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Wildercord.id("listening_belfry")),
		new BlockItem(BELFRY, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Wildercord.id("listening_belfry"))).useBlockDescriptionPrefix()));
	public static final BlockEntityType<ListeningBelfryEntity> BELFRY_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildercord.id("listening_belfry"),
		FabricBlockEntityTypeBuilder.create(ListeningBelfryEntity::new, BELFRY).build());
	private static final ResourceKey<Item> COMPASS_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("quiet_bell_compass"));
	public static final Item COMPASS = Registry.register(BuiltInRegistries.ITEM, COMPASS_KEY,
		new AuraWorld.Lore("quiet_bell_compass", new Item.Properties().setId(COMPASS_KEY).stacksTo(1).rarity(Rarity.UNCOMMON)));
	private static final ResourceKey<EntityType<?>> ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("hollow_bell"));
	public static final EntityType<HollowBell> BELL = Registry.register(BuiltInRegistries.ENTITY_TYPE, ENTITY_KEY,
		EntityType.Builder.of(HollowBell::new, MobCategory.MONSTER).sized(2.8F, 3.4F).eyeHeight(2.6F).clientTrackingRange(14).build(ENTITY_KEY));

	public static ItemStack compassTo(ServerLevel level, BlockPos pos) {
		var stack = new ItemStack(COMPASS);
		stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), pos.immutable())), false));
		return stack;
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(BELL, HollowBell.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:hollow_bell", FieldGuide.Group.MONSTER, 0x5EA89E));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(o -> {
			o.accept(BELFRY_ITEM);
			o.accept(COMPASS);
		});
	}
}
