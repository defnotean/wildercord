package dev.wildercord.trials;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.List;
import java.util.function.Function;

/** Registers Multiplayer Co-Op Trial blocks, items, relics, and Vaelkhor entity. */
public final class CoopTrialContent {
	private CoopTrialContent() {}

	private static ResourceKey<Block> blockKey(String id) {
		return ResourceKey.create(Registries.BLOCK, Wildercord.id(id));
	}

	private static ResourceKey<Item> itemKey(String id) {
		return ResourceKey.create(Registries.ITEM, Wildercord.id(id));
	}

	private static Item registerItem(String id, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = itemKey(id);
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}

	public static final TrialGateBlock TRIAL_GATE = Registry.register(
		BuiltInRegistries.BLOCK, blockKey("trial_gate"),
		new TrialGateBlock(BlockBehaviour.Properties.of()
			.setId(blockKey("trial_gate"))
			.mapColor(MapColor.COLOR_BLACK)
			.strength(-1.0F, 3600000.0F)
			.sound(SoundType.DEEPSLATE_TILES)
			.lightLevel(s -> 10)
			.noLootTable()));

	public static final TrialTreasuryBlock TRIAL_TREASURY = Registry.register(
		BuiltInRegistries.BLOCK, blockKey("trial_treasury"),
		new TrialTreasuryBlock(BlockBehaviour.Properties.of()
			.setId(blockKey("trial_treasury"))
			.mapColor(MapColor.GOLD)
			.strength(-1.0F, 3600000.0F)
			.sound(SoundType.AMETHYST)
			.lightLevel(s -> 12)
			.noLootTable()));

	public static final LeylineConduitBlock LEYLINE_CONDUIT = Registry.register(
		BuiltInRegistries.BLOCK, blockKey("leyline_conduit"),
		new LeylineConduitBlock(BlockBehaviour.Properties.of()
			.setId(blockKey("leyline_conduit"))
			.mapColor(MapColor.COLOR_CYAN)
			.strength(-1.0F, 3600000.0F)
			.sound(SoundType.AMETHYST_CLUSTER)
			.lightLevel(s -> s.getValue(LeylineConduitBlock.LIT) ? 14 : 6)
			.noLootTable()));

	public static final BlockEntityType<TrialGateBlockEntity> TRIAL_GATE_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildercord.id("trial_gate"),
		FabricBlockEntityTypeBuilder.create(TrialGateBlockEntity::new, TRIAL_GATE).build());

	public static final BlockEntityType<TrialTreasuryBlockEntity> TRIAL_TREASURY_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildercord.id("trial_treasury"),
		FabricBlockEntityTypeBuilder.create(TrialTreasuryBlockEntity::new, TRIAL_TREASURY).build());

	public static final Item TRIAL_GATE_ITEM = registerItem("trial_gate",
		p -> new BlockItem(TRIAL_GATE, p.useBlockDescriptionPrefix().rarity(Rarity.RARE)));
	public static final Item TRIAL_TREASURY_ITEM = registerItem("trial_treasury",
		p -> new BlockItem(TRIAL_TREASURY, p.useBlockDescriptionPrefix().rarity(Rarity.EPIC)));
	public static final Item LEYLINE_CONDUIT_ITEM = registerItem("leyline_conduit",
		p -> new BlockItem(LEYLINE_CONDUIT, p.useBlockDescriptionPrefix().rarity(Rarity.RARE)));

	public static final Item SOVEREIGN_HALBERD = registerItem("sovereign_halberd",
		p -> new SovereignHalberdItem(p.sword(ToolMaterial.NETHERITE, 7.0F, -2.9F).durability(2048).rarity(Rarity.EPIC)));
	public static final Item LEYLINE_BELL_CHARM = registerItem("leyline_bell_charm",
		p -> new LeylineBellCharmItem(p.stacksTo(1).rarity(Rarity.EPIC)));
	public static final Item LEYLINE_SIGIL = registerItem("leyline_sigil",
		p -> new AuraWorld.Lore("leyline_sigil", p.stacksTo(16).rarity(Rarity.RARE)));

	private static final ResourceKey<EntityType<?>> VAELKHOR_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("vaelkhor"));
	public static final EntityType<VaelkhorBoss> VAELKHOR = Registry.register(
		BuiltInRegistries.ENTITY_TYPE, VAELKHOR_KEY,
		EntityType.Builder.of(VaelkhorBoss::new, MobCategory.MONSTER)
			.sized(1.9F, 4.0F)
			.eyeHeight(3.3F)
			.clientTrackingRange(16)
			.fireImmune()
			.build(VAELKHOR_KEY));

	public static void init() {
		FabricDefaultAttributeRegistry.register(VAELKHOR, VaelkhorBoss.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:vaelkhor", FieldGuide.Group.MONSTER, 0x60F5E0));
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (level.getBlockState(pos).is(LEYLINE_CONDUIT)) {
				LeylineConduitBlock.interactConduit(level, pos, player);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
			.register(out -> List.of(
				TRIAL_GATE_ITEM, TRIAL_TREASURY_ITEM, LEYLINE_CONDUIT_ITEM,
				SOVEREIGN_HALBERD, LEYLINE_BELL_CHARM, LEYLINE_SIGIL
			).forEach(out::accept));
	}
}
