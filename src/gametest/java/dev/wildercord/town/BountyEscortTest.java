package dev.wildercord.town;

import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * An escort bounty in the world: the board hands the hunter a named pack llama on a lead, tough and chested; abandoning it
 * drops what it carried and the traveller takes it back; leading it to where it's bound pays the bounty out; and a llama
 * that dies loses the bounty with no pay.
 */
public final class BountyEscortTest implements FabricClientGameTest {
	private static final int ROAD = 24, SPAN = 36;

	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }

	private static Town.Active take(MinecraftServer s) {
		Town.Active offered = new Town.Active("escort", BountyRules.ESCORT_TARGET, ROAD, 0, BountyRules.ESCORT_EMERALDS,
			BountyRules.ESCORT_REPUTATION, 0, 0, "minecraft:overworld", "Old Maren");
		Town.Active bound = BountyEscorts.start(p(s), s.overworld(), new BlockPos(0, 101, 0), offered);
		check(bound != null, "there was room for the llama");
		Town.set(p(s), Town.standing(p(s)).with(Optional.of(bound)));
		return bound;
	}

	private static int emeralds(ServerPlayer player) {
		int n = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(Items.EMERALD)) n += stack.getCount();
		}
		return n;
	}

	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("difficulty normal");
			w.getServer().runCommand("time set day");
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				for (int x = -SPAN; x <= SPAN; x++) for (int z = -SPAN; z <= SPAN; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
					for (int y = 101; y <= 104; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				p(s).setGameMode(GameType.SURVIVAL);
				p(s).getInventory().clearContent();
				Town.set(p(s), Town.Standing.NONE);
				p(s).teleportTo(l, 0.5, 101, 0.5, Set.<Relative>of(), 0, 0, false);
			});
			c.waitTicks(5);

			// The board hands over the llama: named, tough, chested, kept, on the hunter's lead, bound ROAD blocks off.
			w.getServer().runOnServer(s -> {
				Town.Active bound = take(s);
				check(Math.abs(Math.hypot(bound.destX(), bound.destZ()) - ROAD) < 1.5, "it's bound " + ROAD + " blocks from the board");
				var llama = BountyEscorts.llama(p(s));
				check(llama != null, "the pack llama is by the hunter");
				check(llama.isTamed() && llama.hasChest() && llama.isPersistenceRequired(), "it's tame, chested and never despawns");
				check(Math.abs(llama.getMaxHealth() - 40) < 0.01 && Math.abs(llama.getHealth() - 40) < 0.01, "it's as tough as an escort should be (" + llama.getMaxHealth() + ")");
				check(llama.isLeashed() && llama.getLeashHolder() == p(s), "it's on the hunter's lead");
				check(llama.hasCustomName() && llama.getCustomName().getString().contains("Old Maren"), "it carries the traveller's name");
				p(s).teleportTo(s.overworld(), llama.getX(), 101, llama.getZ() + 4, Set.<Relative>of(), 180, 15, false);
			});
			c.waitTicks(20);
			c.takeScreenshot(TestScreenshotOptions.of("bounty_escort_llama").withSize(854, 480));

			// Abandoned: what it carried falls where it stands and the traveller takes it back.
			w.getServer().runOnServer(s -> {
				var llama = BountyEscorts.llama(p(s));
				check(llama.getStrength() == BountyRules.ESCORT_STRENGTH && llama.getInventorySize() == BountyRules.ESCORT_STRENGTH * 3, "the llama has a chest to fill (" + llama.getInventorySize() + " slots)");
				llama.getSlot(AbstractHorse.INVENTORY_SLOT_OFFSET).set(new ItemStack(Items.DIAMOND));
				BlockPos at = llama.blockPosition();
				BountyEscorts.abandon(p(s));
				Town.set(p(s), Town.standing(p(s)).with(Optional.empty()));
				check(BountyEscorts.llama(p(s)) == null, "the traveller took the llama back");
				check(!s.overworld().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(3), e -> e.getItem().is(Items.DIAMOND)).isEmpty(),
					"what it carried fell where it stood");
			});

			// Led home: the traveller pays where it arrives and takes the llama back.
			w.getServer().runOnServer(s -> {
				Town.Active bound = take(s);
				var llama = BountyEscorts.llama(p(s));
				llama.teleportTo(bound.destX() + 0.5, 101, bound.destZ() + 0.5);
				p(s).teleportTo(s.overworld(), bound.destX() + 2.5, 101, bound.destZ() + 0.5, Set.<Relative>of(), 90, 0, false);
			});
			c.waitTicks(45);
			w.getServer().runOnServer(s -> {
				Town.Standing standing = Town.standing(p(s));
				check(standing.bounty().isEmpty(), "the escort was paid out");
				check(standing.reputation() == BountyRules.ESCORT_REPUTATION, "it earned its reputation (" + standing.reputation() + ")");
				check(emeralds(p(s)) == BountyRules.ESCORT_EMERALDS, "it paid its emeralds (" + emeralds(p(s)) + ")");
				check(BountyEscorts.llama(p(s)) == null, "the traveller took the llama back at the end");
			});

			// Lost: a llama that dies loses the bounty, with nothing paid.
			w.getServer().runOnServer(s -> {
				p(s).teleportTo(s.overworld(), 0.5, 101, 0.5, Set.<Relative>of(), 0, 0, false);
				take(s);
				var llama = BountyEscorts.llama(p(s));
				llama.hurtServer(s.overworld(), s.overworld().damageSources().generic(), 1000F);
				check(!llama.isAlive(), "the llama died");
				Town.Standing standing = Town.standing(p(s));
				check(standing.bounty().isEmpty(), "a dead llama loses the escort");
				check(standing.reputation() == BountyRules.ESCORT_REPUTATION && emeralds(p(s)) == BountyRules.ESCORT_EMERALDS, "nothing more was paid");
				Town.set(p(s), Town.Standing.NONE);
			});
		}
	}
}
