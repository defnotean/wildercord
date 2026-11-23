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
		new CrossingBellBlock(BlockBehaviour.Properties.of()