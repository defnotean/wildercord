package dev.wildercord.gametest;

import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.content.RuneItem;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.WovenRunes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Places the two smaller dungeons in a real world and uses their relics. */
public class WildercordRuinsTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
			placeAndInspect(context, world, "rootbound_maze", 480, 0);
			placeAndInspect(context, world, "storm_spire", 720, 0);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.CREATIVE);
				var woven = WovenRunes.bind(Runes.FIRE, Runes.HEAL);
				if (!RuneItem.runeOf(RuneItem.stack(woven)).orElseThrow().equals(woven)) {
					throw new AssertionError("the woven rune item should keep its two effects in its id");
				}
				for (var item : new net.minecraft.world.item.Item[] {WildercordItems.ROOTBOUND_RELIC, WildercordItems.STORMGLASS_RELIC}) {
					ItemStack stack = new ItemStack(item);
					player.setItemInHand(InteractionHand.MAIN_HAND, stack);
					stack.use(player.level(), player, InteractionHand.MAIN_HAND);
					if (!player.getCooldowns().isOnCooldown(stack)) {
						throw new AssertionError(item + " should enter cooldown after use");
					}
				}
			});
		}
	}

	private static void placeAndInspect(ClientGameTestContext context, TestSingleplayerContext world, String name, int x, int z) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.setGameMode(GameType.CREATIVE);
			player.teleportTo(x, 120, z);
		});
		world.getServer().runCommand("forceload add " + (x - 64) + " " + (z - 64) + " " + (x + 64) + " " + (z + 64));
		world.getServer().waitFor(server -> {
			ServerLevel level = server.overworld();
			for (int cx = (x - 64) >> 4; cx <= (x + 64) >> 4; cx++) {
				for (int cz = (z - 64) >> 4; cz <= (z + 64) >> 4; cz++) {
					if (!level.hasChunk(cx, cz)) return false;
				}
			}
			return true;
		}, 2400);
		context.waitTicks(20);
		world.getServer().runCommand("place structure wildercord:" + name + " " + x + " 100 " + z);
		context.waitTicks(40);
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			int chests = 0;
			int seals = 0;
			for (BlockPos pos : BlockPos.betweenClosed(x - 60, level.getMinY(), z - 60, x + 60, level.getMaxY() - 1, z + 60)) {
				if (level.getBlockState(pos).is(Blocks.CHEST)) chests++;
				if (level.getBlockState(pos).is(WildercordBlocks.RUNE_SEAL)) seals++;
			}
			if (chests < 4 || seals < 12) {
				throw new AssertionError(name + " should have its four chests and sealed vault; found " + chests + " chests and " + seals + " seals");
			}
		});
	}
}
