package dev.wildercord.aura.world;

import java.util.List;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.BondedBlades;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Consumer;

/** The sleeping blade, claimed by intent and kept by the existing authoritative bond registry. */
public final class SleepingBlades {
	private SleepingBlades() {}
	private static final ResourceKey<net.minecraft.world.level.block.Block> STONE_KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("sleeping_blade_stone"));
	public static final SleepingBladeStone STONE = Registry.register(BuiltInRegistries.BLOCK, STONE_KEY,
		new SleepingBladeStone(BlockBehaviour.Properties.of()