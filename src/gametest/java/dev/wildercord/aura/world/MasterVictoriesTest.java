package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.TechniqueRules;
import dev.wildercord.aura.Techniques;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.Set;
import java.util.UUID;

/** Real victory credit and player-attachment serialization; no fixture replaces the native damage/death path. */
public final class MasterVictoriesTest implements FabricClientGameTest {
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}

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
				ServerLevel level = player.level();
				BlockPos stage = new BlockPos(player.getBlockX(), 180, player.getBlockZ());
				for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) level.setBlockAndUpdate(stage.offset(x, 0, z), Blocks.STONE.defaultBlockState());
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(level, stage.getX() + .5, 181, stage.getZ() + .5, Set.of(), 0, 0, false);
				Challenger ally = add(level, "ClearSupport", stage, 2);
				Challenger outsider = add(level, "Unenrolled", stage, 4);
				player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FORM, 0, 110, 0));
				Techniques.teach(ally, TechniqueRules.ECHO, "fixture");
				int knownBefore = Techniques.book(ally).learned().size();
				int experienceBefore = ally.totalExperience;
				SwordMaster first = trial(level, player, ally, MastersRules.EMBER, stage);
				check(first.hurtServer(level, level.damageSources().playerAttack(player), 100_000) && !first.isAlive(), "The real enrolled player's damage defeats the master");
				check(MasterVictories.progress(player).cleared(MastersRules.EMBER), "The finishing challenger receives the school record");
				check(MasterVictories.progress(ally).cleared(MastersRules.EMBER), "An enrolled living support player receives co-op credit without a last hit");
				check(MasterVictories.progress(outsider).schools() == 0, "A nearby unaccepted player receives no credit");
				check(Techniques.learned(player, TechniqueRules.ECHO), "Ember teaches its existing horizontal Echo part through the normal path");
				check(Techniques.book(ally).learned().size() == knownBefore && ally.totalExperience == experienceBefore,
					"An already-known part grants the clear record without repeatable compensation");
				check(!MasterVictories.award(player, MastersRules.EMBER), "The first-clear award is idempotent");
				check(dev.wildercord.aura.Aura.stage(player) == AuraRules.FORM && dev.wildercord.aura.Aura.stage(ally) == AuraRules.NONE,
					"Part rewards never advance an Aura stage or bypass technique-use restrictions");

				SwordMaster commandKilled = trial(level, player, ally, MastersRules.STONE, stage);
				commandKilled.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
				check(!MasterVictories.progress(player).cleared(MastersRules.STONE), "An admin/bypass death gives no clear");
				SwordMaster inert = trial(level, player, ally, MastersRules.GALE, stage);
				inert.setNoAi(true);
				inert.hurtServer(level, level.damageSources().playerAttack(player), 100_000);
				check(!MasterVictories.progress(player).cleared(MastersRules.GALE), "An inert test/admin target gives no clear");

				SwordMaster summonedKill = trial(level, player, ally, MastersRules.GALE, stage);
				var wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.MOB_SUMMONED);
				check(wolf != null, "The owned-summon fixture exists");
				wolf.tame(player);
				check(MasterVictories.owner(wolf) == player && summonedKill.acceptsHarmFrom(wolf), "A real owned summon resolves to its enrolled player");
				check(summonedKill.hurtServer(level, level.damageSources().mobAttack(wolf), 100_000), "An enrolled summon can land the decisive hit");
				check(MasterVictories.progress(player).cleared(MastersRules.GALE) && MasterVictories.progress(ally).cleared(MastersRules.GALE),
					"Owned-summon finishing damage grants the same bounded co-op credit");
				check(Techniques.learned(player, TechniqueRules.AFTERIMAGE), "Gale teaches its existing horizontal part");

				SwordMaster projectileKill = trial(level, player, ally, MastersRules.STONE, stage);
				var arrow = EntityTypes.ARROW.create(level, EntitySpawnReason.MOB_SUMMONED);
				check(arrow != null, "The owned-projectile fixture exists");
				arrow.setOwner(ally);
				check(MasterVictories.owner(arrow) == ally, "Projectile attribution resolves to the real enrolled shooter");
				check(projectileKill.hurtServer(level, level.damageSources().arrow(arrow, ally), 100_000), "An enrolled projectile can land the decisive hit");
				check(MasterVictories.progress(player).cleared(MastersRules.STONE) && Techniques.learned(ally, TechniqueRules.SUNDER),
					"Stone's first-clear record and normal lesson follow owned projectile damage");

				// Actual native entity save/load includes Fabric's persistent attachment payload and the learned book.
				var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, server.registryAccess());
				ally.saveWithoutId(saved);
				Challenger restored = new Challenger(level, "LoadedClear");
				check(MasterVictories.progress(restored).schools() == 0, "The replacement begins with no supplied progress");
				restored.load(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), saved.buildResult()));
				check(MasterVictories.progress(restored).equals(MasterVictories.progress(ally)), "Native player NBT save/load preserves first-clear records");
				check(Techniques.learned(restored, TechniqueRules.AFTERIMAGE) && !MasterVictories.award(restored, MastersRules.GALE),
					"Reload preserves the learned reward and cannot pay a clear again");
				restored.discard();
				wolf.discard();
				ally.discard();
				outsider.discard();
				first.discard(); commandKilled.discard(); inert.discard(); summonedKill.discard(); projectileKill.discard(); arrow.discard();
			});
		}
	}

	private static Challenger add(ServerLevel level, String name, BlockPos stage, int x) {
		Challenger player = new Challenger(level, name);
		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(stage.getX() + x + .5, 181, stage.getZ() + .5, 0, 0);
		level.addNewPlayer(player);
		return player;
	}

	private static SwordMaster trial(ServerLevel level, ServerPlayer player, ServerPlayer ally, int school, BlockPos stage) {
		SwordMaster master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
		check(master != null, "A real master fixture exists");
		master.setDiscipline(school);
		master.snapTo(stage.getX() + .5, 181, stage.getZ() + 3.5, 180, 0);
		level.addFreshEntity(master);
		master.mobInteract(player, InteractionHand.MAIN_HAND);
		master.mobInteract(player, InteractionHand.MAIN_HAND);
		master.mobInteract(ally, InteractionHand.MAIN_HAND);
		master.mobInteract(ally, InteractionHand.MAIN_HAND);
		check(SwordMaster.ready(player) == 1, "The real consenting roster is ready");
		master.customServerAiStep(level);
		check(master.started() && master.challengerCount() == 2 && !master.isNoAi(), "Credit requires an active encounter with its locked roster");
		return master;
	}
}
