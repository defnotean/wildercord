package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.cast.Effects;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;
import java.util.UUID;

/** Pillar-height targets are reachable by a committed 3D shot; new solid cover still blocks the real projectile. */
public final class MasterAntiAirTest implements FabricClientGameTest {
	private static final class PillarMage extends FakePlayer {
		PillarMage(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "PillarMage")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}

	private SwordMaster master;
	private PillarMage mage;
	private BlockPos stage;
	private float health;
	private float lockedPitch;

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule natural_health_regeneration false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				stage = new BlockPos(player.getBlockX(), 180, player.getBlockZ());
				for (int x = -14; x <= 14; x++) for (int z = -14; z <= 14; z++) level.setBlockAndUpdate(stage.offset(x, 0, z), Blocks.STONE.defaultBlockState());
				for (int y = 1; y <= 8; y++) level.setBlockAndUpdate(stage.offset(5, y, 3), Blocks.STONE.defaultBlockState());
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(level, stage.getX() + .5, 181, stage.getZ() + .5, Set.of(), 0, 0, false);
				mage = new PillarMage(level);
				mage.setGameMode(GameType.SURVIVAL);
				mage.setNoGravity(true);
				mage.snapTo(stage.getX() + 5.5, 189, stage.getZ() + 3.5, 90, 0);
				level.addNewPlayer(mage);
				master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
				check(master != null, "The anti-air master fixture exists");
				master.setNoAi(true);
				master.snapTo(stage.getX() + .5, 181, stage.getZ() + 3.5, -90, 0);
				level.addFreshEntity(master);
				master.mobInteract(player, InteractionHand.MAIN_HAND); master.mobInteract(player, InteractionHand.MAIN_HAND);
				master.mobInteract(mage, InteractionHand.MAIN_HAND); master.mobInteract(mage, InteractionHand.MAIN_HAND);
				check(SwordMaster.ready(player) == 1, "Both challengers consented to the test encounter");
				master.customServerAiStep(level);
				master.setTarget(mage);
				health = mage.getHealth();
			});
			context.waitTicks(21);
			world.getServer().runOnServer(server -> {
				master.customServerAiStep((ServerLevel) master.level());
				check(master.attackAnimation() == MastersRules.Move.CRESCENT.ordinal() + 1, "An elevated mage prompts a real ranged tell instead of unreachable ground navigation");
				check(master.attackAimPitch() < -45, "The synchronized attack pose aims up toward the elevated mage");
				check(mage.getHealth() == health, "The anti-air tell does not deal unavoidable immediate damage");
				Effects.withSource(mage, () -> check(master.interruptWindup() && master.attackAnimation() == 0 && master.attackAimPitch() == 0,
					"Cancelling an elevated tell immediately clears both attack and aim pitch"));
			});
			context.waitTicks(21);
			world.getServer().runOnServer(server -> {
				master.customServerAiStep((ServerLevel) master.level());
				check(master.attackAnimation() == MastersRules.Move.CRESCENT.ordinal() + 1 && master.attackAimPitch() < -45,
					"A fresh tell recaptures elevation after the cancelled attack's recovery");
			});
			context.waitTicks(MastersRules.Move.CRESCENT.tell - MastersRules.AIM_LOCK);
			world.getServer().runOnServer(server -> {
				master.customServerAiStep((ServerLevel) master.level());
				lockedPitch = master.attackAimPitch();
				var direction = mage.getBoundingBox().getCenter().subtract(master.slashOrigin());
				float expected = (float) -Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
				check(Math.abs(expected - lockedPitch) < .0001F, "The visible pitch is exactly the locked projectile direction");
			});
			context.waitTicks(MastersRules.AIM_LOCK);
			world.getServer().runOnServer(server -> {
				master.customServerAiStep((ServerLevel) master.level());
				check(master.attackAimPitch() == lockedPitch, "Release retains the committed elevation for body recovery");
			});
			context.waitFor(mc -> mc.level.getEntity(master.getId()) instanceof SwordMaster copy
				&& Math.abs(copy.attackAimPitch() - lockedPitch) < .0001F, 20);
			context.waitTicks(25);
			world.getServer().runOnServer(server -> {
				check(mage.getHealth() < health, "The actual linear crescent reaches a player eight blocks above the master");
				mage.setHealth(20);
				mage.snapTo(stage.getX() + 5.5, 189, stage.getZ() + 3.5, 90, 0);
				master.setTarget(mage);
				master.customServerAiStep((ServerLevel) master.level());
				check(master.attackAnimation() == MastersRules.Move.CRESCENT.ordinal() + 1, "The next anti-air attack has its own full tell");
				health = mage.getHealth();
			});
			context.waitTicks(MastersRules.Move.CRESCENT.tell - MastersRules.AIM_LOCK);
			world.getServer().runOnServer(server -> {
				ServerLevel level = (ServerLevel) master.level();
				master.customServerAiStep(level); // Lock the elevated target's old position first.
				lockedPitch = master.attackAimPitch();
				mage.snapTo(stage.getX() + 5.5, 192, stage.getZ() + 3.5, 90, 0);
				for (int y = 181; y <= 192; y++) for (int z = -4; z <= 10; z++) {
					level.setBlockAndUpdate(new BlockPos(stage.getX() + 3, y, stage.getZ() + z), Blocks.STONE.defaultBlockState());
				}
			});
			context.waitTicks(MastersRules.AIM_LOCK);
			world.getServer().runOnServer(server -> {
				master.customServerAiStep((ServerLevel) master.level());
				check(master.attackAimPitch() == lockedPitch, "Moving vertically after lock cannot retarget the blade during release");
			});
			context.waitTicks(40);
			world.getServer().runOnServer(server -> {
				check(mage.getHealth() == health, "Solid cover placed after aim lock still stops the real anti-air projectile");
				master.setTarget(mage);
				master.customServerAiStep((ServerLevel) master.level());
				check(master.attackAnimation() == 0 && master.attackAimPitch() == 0, "An expired timeline clears its synchronized elevation");
				master.discard();
				mage.discard();
			});
		}
	}
}
