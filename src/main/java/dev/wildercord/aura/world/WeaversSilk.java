package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.literal;

/**
 * Weaver's Silk: a skein of the Hollow Matron's own silk, wound round a bone under her nest. Once she is dead, anyone may take it up,
 * once: from then on they climb walls the way she does, walking up into them, sliding slowly down when they stop and clinging fast
 * when they sneak; the silk holds them a while off the ground, then lets go until they have stood on it again. The skein stays for
 * whoever comes after. The silk key (or {@code /silk}) puts it away and takes it out again, for when it would only catch on walls.
 */
public final class WeaversSilk {
	private WeaversSilk() {}

	private static final ResourceKey<Block> KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("weavers_silk"));
	public static final WeaversSilkBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, KEY, new WeaversSilkBlock(BlockBehaviour.Properties.of()
		.setId(KEY).noOcclusion().strength(-1, 3600000).noLootTable().sound(SoundType.WOOL).lightLevel(state -> 3)
		.pushReaction(PushReaction.IMMOVEABLE)));
	private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("weavers_silk"));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
		new BlockItem(BLOCK, new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix().rarity(Rarity.EPIC)));

	/** When each player last stood on the ground, by their own tick count; the client and the server keep their own. */
	private static final Map<UUID, Integer> GROUNDED = new HashMap<>(), GROUNDED_CLIENT = new HashMap<>();

	/** Whether the silk is with this player. */
	public static boolean bound(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.WEAVERS_SILK, false);
	}

	/** Whether this player has put the silk away. */
	public static boolean stowed(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.WEAVERS_SILK_STOWED, false);
	}

	/** Puts the silk away or takes it out again, and says which; a player without it is told so. */
	public static void toggle(ServerPlayer player) {
		if (!bound(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.weavers_silk.none").withColor(0xC4BEB2));
			return;
		}
		boolean stow = !stowed(player);
		player.setAttached(WildercordAttachments.WEAVERS_SILK_STOWED, stow);
		player.sendOverlayMessage(Component.translatable(stow ? "message.wildercord.weavers_silk.stowed" : "message.wildercord.weavers_silk.unstowed")
			.withColor(0xE2DCCC));
	}

	/** How long this player has been off the ground, in ticks. */
	private static int offGround(Player player) {
		var seen = player.level().isClientSide() ? GROUNDED_CLIENT : GROUNDED;
		if (player.onGround() || player.isInWater() || player.isInLava() || player.isPassenger()) seen.put(player.getUUID(), player.tickCount);
		// No mark yet, or one from before the count restarted: start counting from now, so the grip still runs out.
		int mark = seen.computeIfAbsent(player.getUUID(), id -> player.tickCount);
		if (mark > player.tickCount) seen.put(player.getUUID(), mark = player.tickCount);
		return (int) HollowMatronRules.offGround(player.tickCount, mark);
	}

	/** Whether a wall is right against this player: something solid just beside them, clear of the floor under their feet. */
	private static boolean walled(Player player) {
		return !player.level().noCollision(player, player.getBoundingBox().inflate(.08, -.1, .08));
	}

	/**
	 * Whether the silk holds this player to the wall beside them as a ladder would: off the ground with a wall against them, or on it
	 * and walking into one, while the silk still grips.
	 */
	public static boolean clings(Player player) {
		if (!bound(player) || player.isSpectator() || player.getAbilities().flying || player.isFallFlying()) return false;
		if (!HollowMatronRules.holds(stowed(player), offGround(player)) || !walled(player)) return false;
		return !player.onGround() || player.horizontalCollision;
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(literal("silk")
			.executes(ctx -> {
				toggle(ctx.getSource().getPlayerOrException());
				return 1;
			})));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> GROUNDED.remove(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> GROUNDED.clear());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var player : server.getPlayerList().getPlayers()) {
				if (!bound(player) || stowed(player)) continue;
				if (clings(player)) {
					player.resetFallDistance();
					if (!player.onGround() && player.tickCount % 8 == 0)
						player.level().sendParticles(ParticleTypes.ITEM_COBWEB, player.getX(), player.getY() + 1, player.getZ(), 2, .3, .4, .3, .01);
				} else if (offGround(player) == HollowMatronRules.SILK_GRIP && walled(player)) {
					player.sendOverlayMessage(Component.translatable("message.wildercord.weavers_silk.slip").withColor(0xC4BEB2));
				}
			}
		});
	}
}
