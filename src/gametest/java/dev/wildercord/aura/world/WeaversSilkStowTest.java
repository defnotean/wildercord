package dev.wildercord.aura.world;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.Arrays;
import java.util.Set;
/** The silk key and /silk put the Weaver's Silk away and take it out again: a stowed silk never holds its wearer to a wall, on either side, and a relog never makes the grip endless. */
public final class WeaversSilkStowTest implements FabricClientGameTest {
	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
	/** Hangs the player in the air against the stone wall, with nothing under them. */
	private static void hang(MinecraftServer s) {
		var p = p(s);
		p.teleportTo(s.overworld(), .7, 103, .5, Set.<Relative>of(), 90, 0, false);
		p.setDeltaMovement(0, 0, 0);
	}
	@SuppressWarnings("unchecked")
	private static java.util.Map<java.util.UUID, Integer> grounded() {
		try {
			var f = WeaversSilk.class.getDeclaredField("GROUNDED");
			f.setAccessible(true);
			return (java.util.Map<java.util.UUID, Integer>) f.get(null);
		} catch (ReflectiveOperationException e) { throw new AssertionError(e); }
	}
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				for (int x = -4; x <= 4; x++) for (int z = -3; z <= 3; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
					for (int y = 101; y <= 107; y++) l.setBlock(new BlockPos(x, y, z), (x == 1 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
				}
				var p = p(s);
				p.setGameMode(GameType.SURVIVAL);
				p.setNoGravity(true);
				p.setAttached(WildercordAttachments.WEAVERS_SILK, true);
				hang(s);
			});
			c.waitTicks(10);
			w.getServer().runOnServer(s -> {
				check(!WeaversSilk.stowed(p(s)), "The silk starts out, not put away");
				check(p(s).onClimbable(), "Bound and out, the silk holds its wearer to the wall on the server");
			});
			check(c.computeOnClient(mc -> mc.player.onClimbable()), "Bound and out, the silk holds its wearer to the wall on the client");

			KeyMapping silk = c.computeOnClient(mc -> Arrays.stream(mc.options.keyMappings).filter(k -> k.getName().equals("key.wildercord.weavers_silk")).findFirst().orElse(null));
			check(silk != null, "The silk key is registered");
			check(c.computeOnClient(mc -> silk.getDefaultKey().getValue() == com.mojang.blaze3d.platform.InputConstants.KEY_G), "The silk key defaults to G");
			c.getInput().pressKey(silk);
			c.waitTicks(6);
			w.getServer().runOnServer(s -> {
				hang(s);
				check(WeaversSilk.stowed(p(s)), "The real silk key puts the silk away");
				check(!p(s).onClimbable(), "Put away, the silk no longer holds its wearer to the wall on the server");
			});
			c.waitTicks(3);
			check(c.computeOnClient(mc -> mc.player.getAttachedOrElse(WildercordAttachments.WEAVERS_SILK_STOWED, false)), "The stowed silk reaches its wearer's client");
			check(!c.computeOnClient(mc -> mc.player.onClimbable()), "Put away, the silk no longer holds its wearer to the wall on the client");

			w.getServer().runOnServer(s -> {
				s.getCommands().performPrefixedCommand(p(s).createCommandSourceStack(), "silk");
				hang(s);
				check(!WeaversSilk.stowed(p(s)), "/silk takes the silk out again");
				check(p(s).onClimbable(), "Out again, the silk holds its wearer to the wall again");
			});

			// A grounded mark from before a relog or respawn (a later tick count than now) must not give an endless grip.
			w.getServer().runOnServer(s -> {
				grounded().put(p(s).getUUID(), p(s).tickCount + 90_000);
				hang(s);
				check(p(s).onClimbable(), "A stale grounded mark counts as having just landed");
			});
			for (int t = 0; t < HollowMatronRules.SILK_GRIP + 20; t += 10) {
				c.waitTicks(10);
				w.getServer().runOnServer(WeaversSilkStowTest::hang);
			}
			w.getServer().runOnServer(s -> check(!p(s).onClimbable(), "After a relog the grip still runs out off the ground"));

			w.getServer().runOnServer(s -> {
				var p = p(s);
				p.setAttached(WildercordAttachments.WEAVERS_SILK, false);
				s.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "silk");
				check(!WeaversSilk.stowed(p), "A player without the silk cannot stow it");
				p.setNoGravity(false);
			});
		}
	}
}
