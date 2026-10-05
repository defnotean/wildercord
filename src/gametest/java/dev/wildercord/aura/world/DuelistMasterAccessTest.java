package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.BreathingMethods;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/** Survival access is a conversation with a teacher, followed by separate explicit consent at the invited master. */
public final class DuelistMasterAccessTest implements FabricClientGameTest {
	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				var level = player.level();
				BlockPos stage = new BlockPos(player.getBlockX(), 180, player.getBlockZ());
				for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) level.setBlockAndUpdate(stage.offset(x, 0, z), Blocks.STONE.defaultBlockState());
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(level, stage.getX() + .5, 181, stage.getZ() + .5, Set.of(), 0, 0, false);
				player.setShiftKeyDown(true);
				Duelist teacher = AuraWorld.DUELIST.create(level, EntitySpawnReason.COMMAND);
				check(teacher != null, "Teacher fixture exists");
				teacher.setNoAi(true);
				teacher.setMethod(BreathingMethods.GALE);
				teacher.snapTo(stage.getX() + 2.5, 181, stage.getZ() + .5, 90, 0);
				level.addFreshEntity(teacher);
				player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE);
				teacher.mobInteract(player, InteractionHand.MAIN_HAND);
				check(level.getEntitiesOfClass(SwordMaster.class, player.getBoundingBox().inflate(20)).isEmpty(),
					"An early character gets a progression explanation instead of an endgame encounter");
				check(!teacher.inDuel(), "The Master's dialogue never starts an ordinary lesson duel");
				player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.FORM, 0, 110, 0));
				teacher.mobInteract(player, InteractionHand.MAIN_HAND);
				check(level.getEntitiesOfClass(SwordMaster.class, player.getBoundingBox().inflate(20)).isEmpty(), "The teacher first offers an invitation");
				teacher.mobInteract(player, InteractionHand.OFF_HAND);
				check(level.getEntitiesOfClass(SwordMaster.class, player.getBoundingBox().inflate(20)).isEmpty(), "Offhand interaction cannot accept the invitation");
				teacher.mobInteract(player, InteractionHand.MAIN_HAND);
				var masters = level.getEntitiesOfClass(SwordMaster.class, player.getBoundingBox().inflate(20));
				check(masters.size() == 1, "The second deliberate conversation brings exactly one master on safe ground");
				SwordMaster master = masters.getFirst();
				check(!master.started() && master.challengerCount() == 0, "Meeting the invited master never enrolls or attacks anyone");
				check(master.method().equals(BreathingMethods.GALE), "The Gale teacher introduces the Gale prototype");
				check(!teacher.inDuel() && teacher.stage() == AuraRules.GLOW, "The teacher's own progression and lesson state are unchanged");
				// Reserve the other seven slots in the real registry; the introduced eighth is already counted.
				java.util.Set<SwordMaster> active;
				try {
					var field = SwordMaster.class.getDeclaredField("ACTIVE");
					field.setAccessible(true);
					@SuppressWarnings("unchecked") var registry = (java.util.Set<SwordMaster>) field.get(null);
					active = registry;
				} catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
				var reserved = new java.util.ArrayList<SwordMaster>();
				for (int i = 0; i < MastersRules.MAX_ENCOUNTERS - 1; i++) {
					SwordMaster other = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
					check(other != null, "Capacity fixture exists");
					reserved.add(other);
					active.add(other);
				}
				check(active.size() == MastersRules.MAX_ENCOUNTERS, "All encounter slots are reserved before acceptance");
				master.mobInteract(player, InteractionHand.OFF_HAND);
				master.mobInteract(player, InteractionHand.MAIN_HAND);
				check(master.challengerCount() == 0, "The invited master gives its own explicit lethal-trial warning");
				master.mobInteract(player, InteractionHand.MAIN_HAND);
				check(master.challengerCount() == 1 && !master.started(), "Only the player's second master conversation joins the lobby");
				check(active.size() == MastersRules.MAX_ENCOUNTERS, "Opening the eighth reservation never consumes a ninth slot");
				reserved.forEach(active::remove);
				player.setShiftKeyDown(false);
				master.discard();
				teacher.discard();
			});
		}
	}
}
