package dev.wildercord.world.ferryman;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The crossing bell is structure furniture; it has no recipe, automatic proximity trigger or spawn egg. */
public final class FerrymanContent {
	private FerrymanContent() {}
	private static final ResourceKey<Block> BELL_KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("crossing_bell"));
	public static final CrossingBellBlock BELL = Registry.register(BuiltInRegistries.BLOCK, BELL_KEY,
		new CrossingBellBlock(BlockBehaviour.Properties.of().setId(BELL_KEY).strength(-1, 3600000).sound(SoundType.COPPER).noOcclusion().noLootTable()));
	public static final BlockEntityType<CrossingBellEntity> BELL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Wildercord.id("crossing_bell"), FabricBlockEntityTypeBuilder.create(CrossingBellEntity::new, BELL).build());
	private static final ResourceKey<EntityType<?>> ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("reed_crowned_ferryman"));
	public static final EntityType<ReedCrownedFerryman> FERRYMAN = Registry.register(BuiltInRegistries.ENTITY_TYPE, ENTITY_KEY,
		EntityType.Builder.of(ReedCrownedFerryman::new, MobCategory.MONSTER).sized(3.2F, 3.2F).eyeHeight(2.5F).clientTrackingRange(14).build(ENTITY_KEY));
	public static void init() {
		FabricDefaultAttributeRegistry.register(FERRYMAN, ReedCrownedFerryman.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:reed_crowned_ferryman", FieldGuide.Group.MONSTER, 0x83B8A0));
	}
}
