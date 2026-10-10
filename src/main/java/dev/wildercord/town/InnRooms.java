package dev.wildercord.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Rooms at the Wayfarer Inns: who has one, until when, and their room's chest. See {@link InnRoomRules}. */
public final class InnRooms {
	private InnRooms() {}

	/** A room let until {@code until} (game ticks), where its traveller wakes. */
	public record Room(long until, GlobalPos bed) {
		public static final Codec<Room> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("until").forGetter(Room::until),
			GlobalPos.CODEC.fieldOf("bed").forGetter(Room::bed)).apply(i, Room::new));
	}

	public static final AttachmentType<Room> ROOM = AttachmentRegistry.create(Wildercord.id("inn_room"),
		builder -> builder.persistent(Room.CODEC).copyOnDeath());
	/** The room chest: kept between stays, and through death. */
	public static final AttachmentType<List<ItemStack>> CHEST = AttachmentRegistry.create(Wildercord.id("inn_chest"),
		builder -> builder.persistent(ItemStack.OPTIONAL_CODEC.listOf()).copyOnDeath());

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 200 != 0) return;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) expire(player);
		});
	}

	public static Optional<Room> room(ServerPlayer player) {
		Room room = player.getAttached(ROOM);
		return room != null && InnRoomRules.rented(room.until(), player.level().getGameTime()) ? Optional.of(room) : Optional.empty();
	}

	/** A Room Key used at an inn: lets {@code player} a room here, or adds days to the one they have. */
	public static void rent(ServerPlayer player) {
		long now = player.level().getGameTime();
		Room old = player.getAttached(ROOM);
		Room room = new Room(InnRoomRules.extend(old == null ? 0 : old.until(), now), GlobalPos.of(player.level().dimension(), player.blockPosition()));
		player.setAttached(ROOM, room);
		player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(room.bed().dimension(), room.bed().pos(),
			player.getYRot(), 0), true), false);
		player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 1.4F);
		boolean extended = old != null && InnRoomRules.rented(old.until(), now);
		player.sendSystemMessage(Component.translatable(extended ? "message.wildercord.room.extended" : "message.wildercord.room.rented",
			InnRoomRules.daysLeft(room.until(), now)).withStyle(ChatFormatting.GOLD));
	}

	/** Sneak-clicking a keeper: opens the traveller's room chest, if they have a room. */
	public static void open(ServerPlayer player) {
		if (room(player).isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.wildercord.room.none").withStyle(ChatFormatting.GRAY));
			return;
		}
		int slots = InnRoomRules.CHEST_ROWS * 9;
		List<ItemStack> saved = player.getAttachedOrElse(CHEST, List.of());
		SimpleContainer chest = new SimpleContainer(slots) {
			@Override
			public void setChanged() {
				super.setChanged();
				List<ItemStack> out = new ArrayList<>(getContainerSize());
				for (int i = 0; i < getContainerSize(); i++) out.add(getItem(i).copy());
				player.setAttached(CHEST, out);
			}
		};
		for (int i = 0; i < Math.min(slots, saved.size()); i++) chest.setItem(i, saved.get(i).copy());
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inventory, chest, InnRoomRules.CHEST_ROWS),
			Component.translatable("container.wildercord.inn_room")));
		player.level().playSound(null, player.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.6F, 1.0F);
	}

	/** Ends a stay that has run out: the traveller no longer wakes at the inn, though their chest is kept. */
	public static void expire(ServerPlayer player) {
		Room room = player.getAttached(ROOM);
		if (room == null || InnRoomRules.rented(room.until(), player.level().getGameTime())) return;
		player.removeAttached(ROOM);
		ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
		if (respawn != null && respawn.forced() && respawn.respawnData().dimension() == room.bed().dimension()
			&& respawn.respawnData().pos().equals(room.bed().pos())) {
			player.setRespawnPosition(null, false);
		}
		player.sendSystemMessage(Component.translatable("message.wildercord.room.ended").withStyle(ChatFormatting.GRAY));
	}
}
