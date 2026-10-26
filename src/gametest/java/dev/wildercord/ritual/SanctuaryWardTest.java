package dev.wildercord.ritual;

import dev.wildercord.cast.events.RiftSiege;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;

/**
 * A Sanctuary in the real game turns aside the common monsters inside it (weakened, and made to forget the player) but
 * leaves a boss and a Riftcaller to their own fight.
 */
public final class SanctuaryWardTest implements FabricClientGameTest {
	private static void check(boolean x, String why) {
		if (!x) throw new AssertionError(why);
	}

	private static ServerPlayer p(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private static Mob spawn(ServerPlayer player, EntityType<? extends Mob> type, double dx) {
		ServerLevel level = player.level();
		Mob mob = type.create(level, EntitySpawnReason.COMMAND);
		mob.setNoAi(true);
		mob.snapTo(player.getX() + dx, player.getY(), player.getZ() + 4);
		level.addFreshEntity(mob);
		mob.setTarget(player);
		return mob;
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runOnServer(s -> {
				ServerPlayer player = p(s);
				player.setGameMode(GameType.SURVIVAL);
				Mob common = spawn(player, EntityTypes.ZOMBIE, -3);
				Mob riftcaller = spawn(player, EntityTypes.PILLAGER, 0);
				riftcaller.addTag(RiftSiege.RIFTCALLER_TAG);
				Mob boss = spawn(player, EntityTypes.ELDER_GUARDIAN, 3);
				try {
					Rituals.sanctuary(player.level(), player.position());
					check(common.hasEffect(MobEffects.WEAKNESS) && common.getTarget() == null, "a common monster is weakened and forgets the player");
					check(!riftcaller.hasEffect(MobEffects.WEAKNESS) && riftcaller.getTarget() == player, "the Riftcaller isn't turned aside");
					check(!boss.hasEffect(MobEffects.WEAKNESS) && boss.getTarget() == player, "a boss isn't turned aside");
				} finally {
					Rituals.liftWards();
				}
			});
		}
	}
}
