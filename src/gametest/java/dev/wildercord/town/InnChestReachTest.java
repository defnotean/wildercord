package dev.wildercord.town;
import dev.wildercord.content.SpellScrollItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.List;
import java.util.Set;
/** A keeper's room chest shuts once its traveller walks off, and the Cord screen writes no scroll for a spectator. */
public final class InnChestReachTest implements FabricClientGameTest {
	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
	private static void stand(MinecraftServer s, double z) {
		p(s).teleportTo(s.overworld(), 0.5, 101, z, Set.<Relative>of(), 0, 10, false);
		p(s).setDeltaMovement(0, 0, 0);
	}
	private static int scrolls(ServerPlayer p) { return p.getInventory().countItem(WildercordItems.SPELL_SCROLL); }
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("time set day");
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				for (int x = -4; x <= 4; x++) for (int z = -4; z <= 24; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.OAK_PLANKS.defaultBlockState(), 2);
					for (int y = 101; y <= 104; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				p(s).setGameMode(GameType.SURVIVAL);
				stand(s, 0.5);
				InnRooms.rent(p(s));
				p(s).setAttached(InnRooms.CHEST, List.of(new ItemStack(Items.EMERALD, 7)));
				InnRooms.open(p(s));
			});
			c.waitTicks(5);
			check(c.computeOnClient(mc -> mc.gui.screen() instanceof ContainerScreen), "A keeper opens the room chest");
			w.getServer().runOnServer(s -> check(p(s).containerMenu instanceof ChestMenu, "The room chest is open on the server"));
			c.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
			c.takeScreenshot(TestScreenshotOptions.of("inn_room_chest_open").disableCounterPrefix());

			// A few steps leave it open.
			w.getServer().runOnServer(s -> stand(s, 5.5));
			c.waitTicks(5);
			w.getServer().runOnServer(s -> check(p(s).containerMenu instanceof ChestMenu, "Within reach, the chest stays open"));

			// Walked off, it shuts, and what was in it stays put.
			w.getServer().runOnServer(s -> stand(s, 20.5));
			c.waitTicks(5);
			w.getServer().runOnServer(s -> {
				check(p(s).containerMenu == p(s).inventoryMenu, "Walked off, the room chest shuts");
				var kept = p(s).getAttachedOrElse(InnRooms.CHEST, List.<ItemStack>of());
				check(!kept.isEmpty() && kept.getFirst().is(Items.EMERALD) && kept.getFirst().getCount() == 7, "Its things stay in it");
			});
			c.waitTicks(3);
			check(c.computeOnClient(mc -> mc.gui.screen() == null), "The client's chest screen closes too");

			// The Cord screen's inscribe writes a scroll for a player, and none for a spectator.
			w.getServer().runOnServer(s -> {
				var p = p(s);
				p.setGameMode(GameType.CREATIVE);
				Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
				Spellbooks.set(p, new Spellbook(List.of(Runes.BEAM.id(), Runes.HARM.id()), List.of(List.of(Runes.BEAM.id(), Runes.HARM.id())), 0, true));
				int before = scrolls(p);
				SpellScrollItem.inscribe(p, 0);
				check(scrolls(p) == before + 1, "A player inscribes their spell");
				p.setGameMode(GameType.SPECTATOR);
				SpellScrollItem.inscribe(p, 0);
				check(scrolls(p) == before + 1, "A spectator inscribes nothing");
				p.setGameMode(GameType.SURVIVAL);
			});
		}
	}
}
