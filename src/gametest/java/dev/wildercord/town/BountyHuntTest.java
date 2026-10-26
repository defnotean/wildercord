package dev.wildercord.town;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * A named-elite bounty in the world: taking it sets one champion loose 48 to 96 blocks from the board, named, marked for the
 * hunter and kept from despawning; a plain creature of the same kind doesn't count, and the hunter's kill of the elite
 * finishes the bounty.
 */
public final class BountyHuntTest implements FabricClientGameTest {
	private static final String TARGET = "wildercord:bramblewalker";
	private static final int Y = 150, SPAN = 104;

	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }

	private static List<Mob> elites(MinecraftServer s) {
		return s.overworld().getEntitiesOfClass(Mob.class, new AABB(-SPAN, Y - 8, -SPAN, SPAN, Y + 8, SPAN),
			m -> m.entityTags().contains(Town.markTag(p(s).getUUID())));
	}

	private static Town.Active bounty(MinecraftServer s) {
		return Town.standing(p(s)).bounty().orElseThrow(() -> new AssertionError("the bounty is still held"));
	}

	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("difficulty normal");
			w.getServer().runCommand("time set day");
			w.getServer().runCommand("forceload add -" + SPAN + " -" + SPAN + " " + SPAN + " " + SPAN);
			w.getServer().runOnServer(s -> {
				p(s).setGameMode(GameType.CREATIVE);
				p(s).teleportTo(s.overworld(), 0.5, Y + 1, 0.5, Set.<Relative>of(), 0, 0, false);
			});
			for (int i = 0; i < 60 && !w.getServer().computeOnServer(s -> s.overworld().hasChunkAt(new BlockPos(SPAN, Y, SPAN))
				&& s.overworld().hasChunkAt(new BlockPos(-SPAN, Y, -SPAN))); i++) {
				c.waitTicks(20);
			}
			// A stone sky floor over the whole range, so wherever the elite lands it's dry, open ground.
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				check(l.hasChunkAt(new BlockPos(SPAN, Y, SPAN)) && l.hasChunkAt(new BlockPos(-SPAN, Y, -SPAN)), "the range is loaded");
				for (int x = -SPAN; x <= SPAN; x++) for (int z = -SPAN; z <= SPAN; z++) {
					l.setBlock(new BlockPos(x, Y, z), Blocks.STONE.defaultBlockState(), 2);
					for (int y = Y + 1; y <= Y + 4; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
			});
			c.waitTicks(5);

			BlockPos board = new BlockPos(0, Y + 1, 0);
			w.getServer().runOnServer(s -> {
				Town.Active taken = new Town.Active("elite", TARGET, 1, 0, BountyRules.ELITE_EMERALDS, BountyRules.ELITE_REPUTATION,
					board.getX(), board.getZ(), "minecraft:overworld", "Grimjaw");
				Town.set(p(s), Town.Standing.NONE.with(Optional.of(taken)));
				Direction way = BountyHunts.loose(p(s), s.overworld(), board, taken);
				check(way != null, "a spot was found for the elite");
				List<Mob> found = elites(s);
				check(found.size() == 1, "exactly one marked elite is loose (found " + found.size() + ")");
				Mob elite = found.getFirst();
				double dx = elite.getX() - board.getX(), dz = elite.getZ() - board.getZ(), d = Math.sqrt(dx * dx + dz * dz);
				check(d >= BountyRules.ELITE_NEAR - 2 && d <= BountyRules.ELITE_FAR + 2, "the elite is 48 to 96 blocks out (" + d + ")");
				check(way == Direction.getApproximateNearest(dx, 0, dz), "the board points the right way");
				check(BuiltInRegistries.ENTITY_TYPE.getKey(elite.getType()).toString().equals(TARGET), "it's the bounty's creature");
				check(elite.hasCustomName() && elite.getCustomName().getString().contains("Grimjaw"), "it carries the bounty's name");
				check(elite.isCustomNameVisible() && elite.isPersistenceRequired(), "its name shows and it never despawns");
				check(elite.getMaxHealth() > ((Mob) elite.getType().create(s.overworld(), EntitySpawnReason.COMMAND)).getMaxHealth() * 2,
					"a champion is far tougher than a plain one");
				// Stand a few blocks off, facing it, for the screenshot.
				double back = 5 / d;
				double px = elite.getX() - dx * back, pz = elite.getZ() - dz * back;
				float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
				p(s).teleportTo(s.overworld(), px, Y + 1, pz, Set.<Relative>of(), yaw, 10, false);
				elite.setNoAi(true);
			});
			c.waitTicks(30);
			c.takeScreenshot(TestScreenshotOptions.of("bounty_named_elite").withSize(854, 480));

			// A plain creature of the same kind doesn't count; the hunter's kill of the marked elite does.
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(TARGET));
				Mob plain = (Mob) type.create(l, EntitySpawnReason.COMMAND);
				plain.snapTo(p(s).getX() + 2, Y + 1, p(s).getZ(), 0, 0);
				l.addFreshEntity(plain);
				plain.hurtServer(l, p(s).damageSources().playerAttack(p(s)), 1.0E6F);
				check(!plain.isAlive(), "the plain one died");
				check(bounty(s).kills() == 0 && !bounty(s).done(), "a plain creature doesn't count toward a named elite");
				Mob elite = elites(s).getFirst();
				elite.hurtServer(l, p(s).damageSources().playerAttack(p(s)), 1.0E6F);
				check(!elite.isAlive(), "the elite died");
				check(bounty(s).kills() == 1 && bounty(s).done(), "the hunter's kill finishes the bounty");
				Town.set(p(s), Town.Standing.NONE);
			});
			w.getServer().runCommand("forceload remove all");
		}
	}
}
