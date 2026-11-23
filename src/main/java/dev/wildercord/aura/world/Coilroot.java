package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The Coilroot: the herb the Rootcoil Serpent guards, growing between the roots of its tree. Once the serpent is dead, anyone may
 * eat of it, once: from then on their breathing stance draws in more aura, and a little trickles in all the time besides. The root
 * itself cannot be dug up; it stays for whoever comes down after.
 */
public final class Coilroot {
	private Coilroot() {}

	private static final ResourceKey<Block> KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("coilroot"));
	public static final CoilrootBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, KEY, new CoilrootBlock(BlockBehaviour.Properties.of()
		