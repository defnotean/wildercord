package dev.wildercord.town;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Set;

/**
 * A stay at a Wayfarer Inn in the real game: a Room Key does nothing away from a keeper, rents a room beside one (and is
 * spent), a second key adds its days to the stay, the traveller wakes at the inn after dying with room and chest kept, and
 * a stay that runs out ends on the server's own check, taking the inn's spawn with it but leaving the chest.
 */
public final class InnRoomStayTest implements FabricClientGameTest {
	private static final BlockPos ROOM = new BlockPos(0, 101, 0);
	private static final double FAR = 40.5;

	private static void check(boolean x, String why) {
		if (!x) throw new AssertionError(why);
	}

	private static ServerPlayer p(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private static void stand(MinecraftServer s, double z) {
		p(s).teleportTo(s.overworld(), 0.5, 101, z, Set.<Relative>of(), 0, 10, false);
		p(s).setDeltaMovement(0, 0, 0);
	}

	private static boolean useKey(MinecraftServer s) {
		ServerPlayer p = p(s);
		p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Town.ROOM_KEY, 2));
		Town.ROOM_KEY.use(p.level(), p, InteractionHand.MAIN_HAND);
		return p.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1;
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("gamerule keep_inventory false");
			w.getServer().runCommand("time set day");
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				for (int x = -6; x <= 6; x++) for (int z = -6; z <= 46; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.OAK_PLANKS.defaultBlockState(), 2);
					for (int y = 101; y <= 104; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				WayfarerKeeper cook = Town.KEEPER.create(l, EntitySpawnReason.COMMAND);
				cook.setRole(WayfarerKeeper.Role.COOK);
				cook.snapTo(0.5, 101, 4.5, 180, 0);
				cook.setNoAi(true);
				l.addFreshEntity(cook);
				p(s).setGameMode(GameType.SURVIVAL);

				// Away from any keeper the key is refused and kept.
				stand(s, FAR);
				check(!useKey(s), "Away from an inn the key isn't spent");
				check(InnRooms.room(p(s)).isEmpty(), "and no room is let");

				// Beside the cook it lets a room for its days, spends the key and makes the inn the traveller's spawn.
				stand(s, ROOM.getZ() + 0.5);
				long now = p(s).level().getGameTime();
				check(useKey(s), "At an inn the key is spent");
				var room = InnRooms.room(p(s));
				check(room.isPresent() && room.get().bed().pos().equals(ROOM), "A room is let where the key was used");
				check(InnRoomRules.daysLeft(room.get().until(), now) == InnRoomRules.RENT_DAYS, "for a key's days");
				var respawn = p(s).getRespawnConfig();
				check(respawn != null && respawn.forced() && respawn.respawnData().pos().equals(ROOM), "The traveller now wakes at the inn");

				// A second key adds to the stay rather than starting it over.
				check(useKey(s), "A second key is spent");
				check(InnRoomRules.daysLeft(InnRooms.room(p(s)).orElseThrow().until(), now) == 2 * InnRoomRules.RENT_DAYS, "A second key adds its days");
				p(s).setAttached(InnRooms.CHEST, List.of(new ItemStack(Items.DIAMOND, 3)));
			});
			c.waitTicks(3);
			c.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
			c.takeScreenshot(TestScreenshotOptions.of("inn_room_rented").disableCounterPrefix());

			// Dying far off, the traveller wakes in their room, with the room and its chest kept.
			w.getServer().runOnServer(s -> {
				stand(s, FAR);
				ServerPlayer old = p(s);
				old.kill(old.level());
				old.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
				ServerPlayer fresh = p(s);
				check(fresh != old, "The traveller respawns");
				check(fresh.blockPosition().distManhattan(ROOM) <= 2, "and wakes at the inn, not where they fell: " + fresh.blockPosition());
				check(InnRooms.room(fresh).isPresent(), "The room is kept through death");
				var kept = fresh.getAttachedOrElse(InnRooms.CHEST, List.<ItemStack>of());
				check(!kept.isEmpty() && kept.getFirst().is(Items.DIAMOND) && kept.getFirst().getCount() == 3, "and so is its chest");
			});
			c.waitTicks(10);
			c.takeScreenshot(TestScreenshotOptions.of("inn_room_woke").disableCounterPrefix());

			// A stay about to run out ends on the server's own check: no more waking at the inn, the chest stays.
			w.getServer().runOnServer(s -> {
				var room = InnRooms.room(p(s)).orElseThrow();
				p(s).setAttached(InnRooms.ROOM, new InnRooms.Room(p(s).level().getGameTime() + 5, room.bed()));
			});
			c.waitTicks(210);
			w.getServer().runOnServer(s -> {
				check(!p(s).hasAttached(InnRooms.ROOM), "A stay that ran out ends");
				check(p(s).getRespawnConfig() == null, "and the traveller no longer wakes at the inn");
				check(!p(s).getAttachedOrElse(InnRooms.CHEST, List.<ItemStack>of()).isEmpty(), "Their room chest is kept for the next stay");
			});
		}
	}
}
