package dev.wildercord.world.cartographer;

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

public final class CartographerContent {
	private CartographerContent() {}
	private static final ResourceKey<Block> ASTROLABE_KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("survey_astrolabe"));
	public static final SurveyAstrolabeBlock ASTROLABE = Registry.register(BuiltInRegistries.BLOCK, ASTROLABE_KEY,
		new SurveyAstrolabeBlock(BlockBehaviour.Properties.of().setId(ASTROLABE_KEY).strength(-1, 3600000).noOcclusion().noLootTable().lightLevel(s -> 9).sound(SoundType.COPPER)));
	public static final Item ASTROLABE_ITEM = Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Wildercord.id("survey_astrolabe")),
		new BlockItem(ASTROLABE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Wildercord.id("survey_astrolabe"))).useBlockDescriptionPrefix()));
	public static final BlockEntityType<SurveyAstrolabeEntity> ASTROLABE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildercord.id("survey_astrolabe"),
		FabricBlockEntityTypeBuilder.create(SurveyAstrolabeEntity::new, ASTROLABE).build());
	private static final ResourceKey<Item> COMPASS_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("high_meridian_compass"));
	public static final Item COMPASS = Registry.register(BuiltInRegistries.ITEM, COMPASS_KEY,
		new AuraWorld.Lore("high_meridian_compass", new Item.Properties().setId(COMPASS_KEY).stacksTo(1).rarity(Rarity.UNCOMMON)));
	private static final ResourceKey<EntityType<?>> ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("glass_cartographer"));
	public static final EntityType<GlassCartographer> CARTOGRAPHER = Registry.register(BuiltInRegistries.ENTITY_TYPE, ENTITY_KEY,
		EntityType.Builder.of(GlassCartographer::new, MobCategory.MONSTER).sized(2.8F, 3.4F).eyeHeight(2.6F).clientTrackingRange(14).build(ENTITY_KEY));

	public static ItemStack compassTo(ServerLevel level, BlockPos pos) {
		var stack = new ItemStack(COMPASS);
		stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), pos.immutable())), false));
		return stack;
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(CARTOGRAPHER, GlassCartographer.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:glass_cartographer", FieldGuide.Group.MONSTER, 0xD8B46A));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(o -> {
			o.accept(ASTROLABE_ITEM);
			o.accept(COMPASS);
		});
	}
}
