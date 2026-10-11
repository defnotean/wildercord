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
import net.minecraft.world.level.material.PushReaction;

/**
 * The Coilroot: the herb the Rootcoil Serpent guards, growing between the roots of its tree. Once the serpent is dead, anyone may
 * eat of it, once: from then on their breathing stance draws in more aura, and a little trickles in all the time besides. The root
 * itself cannot be dug up; it stays for whoever comes down after.
 */
public final class Coilroot {
	private Coilroot() {}

	private static final ResourceKey<Block> KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("coilroot"));
	public static final CoilrootBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, KEY, new CoilrootBlock(BlockBehaviour.Properties.of()
		.setId(KEY).noCollision().noOcclusion().strength(-1, 3600000).noLootTable().sound(SoundType.AZALEA).lightLevel(state -> 6)
		.pushReaction(PushReaction.IMMOVEABLE)));
	private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("coilroot"));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
		new BlockItem(BLOCK, new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix().rarity(Rarity.EPIC)));

	/** Whether this player has eaten of it. */
	public static boolean eaten(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.COILROOT, false);
	}

	public static void init() {
		AuraApi.onGain((player, amount, source) -> eaten(player) ? SerpentRules.coilroot(source, amount) : amount);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			for (var player : server.getPlayerList().getPlayers()) {
				if (player.isAlive() && eaten(player)) Aura.gain(player, SerpentRules.COILROOT_TRICKLE, "coilroot");
			}
		});
	}
}
