package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.Techniques;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityInLevelCallback;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/** Native removal, real chunk unload/reload and server-stop checks within the existing Masters trial group. */
final class MasterLifecycleChecks {
	private static volatile MasterLifecycleChecks running;
	static {
		ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
			MasterLifecycleChecks probe = running;
			if (probe != null && probe.unloaded != null && probe.unloaded.level() == level
				&& chunk.getPos().equals(probe.remoteChunk)) probe.chunkUnloadAt = level.getGameTime();
		});
	}
	private static final class Challenger extends FakePlayer {
		private int rewards;
		Challenger(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public void sendSystemMessage(Component message) {
			if (message.toString().contains("message.wildercord.master.first_clear")) rewards++;
			super.sendSystemMessage(message);
		}
	}

	private SwordMaster direct, other, unloaded, stopped;
	private Challenger organizer, invited, remote;
	private BlockPos stage;
	private final BlockPos remoteStage = new BlockPos(16008, 180, 16008);
	private ChunkPos remoteChunk;
	private long startedAt, releasedAt;
	private long chunkUnloadAt = -1;
	private int unloadReceipt;
	private int remoteRewards, remoteExperience, remoteLessons;
	private MasterVictoryRules.Progress remoteProgress;

	void run(ClientGameTestContext context) {
		running = this;
		try { runWorld(context); }
		finally { running = null; } // The permanent event listener must not retain a completed test world.
	}

	private void runWorld(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				stage = new BlockPos(player.getBlockX(), 180, player.getBlockZ());
				floor(level, stage);
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(level, stage.getX() + .5, 181, stage.getZ() + .5, Set.of(), 0, 0, false);
				organizer = challenger(level, "NextTrial", stage.offset(2, 0, 0));
				invited = challenger(level, "InvitedOnly", stage.offset(4, 0, 0));
				direct = lobby(player, stage);
				direct.mobInteract(invited, InteractionHand.MAIN_HAND);
				// Prepare this lobby before removal; no spawn/interaction pruning can hide a stale ACTIVE entry later.
				other = lobby(organizer, stage.offset(2, 0, 0));
				check(SwordMaster.ready(player) == 1, "The direct-unload encounter closes its real lobby");
				direct.customServerAiStep(level);
				startedAt = level.getGameTime();
			});
			world.getServer().waitFor(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				long now = player.level().getGameTime();
				if (now < startedAt + 21 || now % 10 != 0) return false;
				direct.customServerAiStep(player.level());
				check(direct.attackAnimation() != 0 && direct.state(AuraFighter.WINDUP) && direct.getTarget() == player,
					"The exact native removal path starts with a real pending attack and target");
				check(active().contains(direct) && bar(direct).getPlayers().contains(player)
					&& !((Map<?, ?>) field(direct, "invitations")).isEmpty(),
					"The encounter retains a roster, bossbar viewer and invitation before removal");
				float health = direct.getHealth();
				var progress = MasterVictories.progress(player);
				direct.setNoAi(false);
				// PersistentEntitySectionManager.unloadEntity calls this final method, bypassing remove.
				direct.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
				direct.setLevelCallback(EntityInLevelCallback.NULL); // The native unload method's next operation.
				check(direct.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK,
					"The regression uses the exact native setRemoved unload reason");
				cleaned(direct);
				check(direct.getHealth() == health && MasterVictories.progress(player).equals(progress),
					"Unloading neither kills the master nor awards a victory");
				check(SwordMaster.join(player) == 1 && other.challengers().contains(player.getUUID()),
					"The former challenger joins a preexisting lobby without any stale-roster pruning");
				direct.onRemoval(Entity.RemovalReason.UNLOADED_TO_CHUNK);
				direct.discard();
				cleaned(direct);
				other.discard();
				cleaned(other);
				legitimateDeath(organizer);
				organizer.discard();
				invited.discard();
				return true;
			}, 40);
			realChunkUnload(world);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				stopped = lobby(player, stage);
			});
			world.getServer().waitFor(server -> {
				if (stopped.level().getGameTime() % 10 != 0) return false;
				stopped.customServerAiStep((ServerLevel) stopped.level());
				check(active().contains(stopped) && !bar(stopped).getPlayers().isEmpty(),
					"A live lobby and bossbar remain for the real server-stop hook");
				return true;
			}, 20);
		}
		// Closing the test world joins its server thread before these retained-object assertions.
		cleaned(stopped);
		cleaned(direct);
		cleaned(unloaded);
		check(active().isEmpty(), "Stopping the server releases every active encounter");
	}

	private void realChunkUnload(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
			remoteChunk = ChunkPos.containing(remoteStage);
			level.setChunkForced(remoteChunk.x(), remoteChunk.z(), true);
			floor(level, remoteStage);
		});
		world.getServer().waitFor(server -> server.overworld().areEntitiesActuallyLoadedAndTicking(remoteChunk), 200);
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			remote = challenger(level, "UnloadTrial", remoteStage);
			unloaded = lobby(remote, remoteStage);
			check(!unloaded.shouldBeSaved(), "Temporary encounters retain their no-save contract");
			check(SwordMaster.ready(remote) == 1, "The remote encounter has a locked challenger");
			unloaded.customServerAiStep(level);
			remoteProgress = MasterVictories.progress(remote);
			remoteRewards = remote.rewards;
			remoteExperience = remote.totalExperience;
			remoteLessons = Techniques.book(remote).learned().size();
		});
		world.getServer().waitFor(server -> {
			ServerLevel level = server.overworld();
			if (level.getGameTime() % 10 != 0) return false;
			unloaded.customServerAiStep(level);
			check(bar(unloaded).getPlayers().contains(remote) && active().contains(unloaded),
				"The real unload begins with retained bossbar and encounter references");
			remote.discard();
			check(!level.players().contains(remote), "The remote FakePlayer leaves the level before its chunk ticket is released");
			check(unloaded.isNoAi(), "The no-AI fixture excludes ordinary abandonment as the source of cleanup");
			releasedAt = level.getGameTime();
			level.setChunkForced(remoteChunk.x(), remoteChunk.z(), false);
			unloadReceipt(level, "released");
			return true;
		}, 20);
		world.getServer().waitFor(server -> {
			ServerLevel level = server.overworld();
			long elapsed = level.getGameTime() - releasedAt;
			int[] observations = {20, 100, 400, 800};
			if (unloadReceipt < observations.length && elapsed >= observations[unloadReceipt]) {
				unloadReceipt(level, "waiting");
				unloadReceipt++;
			}
			// getChunkNow is observational; it never reloads the chunk whose absence this test proves.
			if (chunkUnloadAt < releasedAt || level.getChunkSource().getChunkNow(remoteChunk.x(), remoteChunk.z()) != null) return false;
			check(unloaded.isRemoved(), "A real chunk unload removes the otherwise unsaved, unticked encounter");
			cleaned(unloaded);
			check(level.getEntity(unloaded.getUUID()) == null, "The unloaded master is absent from the native entity lookup");
			noUnloadReward();
			Wildercord.LOGGER.info("[masters-unload] chunk={} elapsed={} unloadEventAt={} reason={} active={} roster={} viewers={}",
				remoteChunk, level.getGameTime() - releasedAt, chunkUnloadAt, unloaded.getRemovalReason(), active().size(),
				unloaded.challengers().size(), bar(unloaded).getPlayers().size());
			level.setChunkForced(remoteChunk.x(), remoteChunk.z(), true);
			return true;
		}, 800);
		world.getServer().waitFor(server -> server.overworld().areEntitiesActuallyLoadedAndTicking(remoteChunk), 200);
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			check(level.getChunkSource().getChunkNow(remoteChunk.x(), remoteChunk.z()) != null,
				"The chunk is really loaded again for the resurrection check");
			check(level.getEntity(unloaded.getUUID()) == null && level.getEntitiesOfClass(SwordMaster.class,
				unloaded.getBoundingBox().inflate(16)).isEmpty(), "Reload restores no master or replacement encounter");
			cleaned(unloaded);
			noUnloadReward();
			level.setChunkForced(remoteChunk.x(), remoteChunk.z(), false);
		});
	}

	/** Read-only receipts distinguish live tickets, incomplete saving and an undrained native unload queue. */
	private void unloadReceipt(ServerLevel level, String phase) {
		var cache = level.getChunkSource();
		ChunkMap chunks = cache.chunkMap;
		long key = remoteChunk.pack();
		var tickets = level.getDataStorage().get(TicketStorage.TYPE);
		var pending = (Map<?, ?>) readField(chunks, ChunkMap.class, "pendingUnloads");
		ChunkHolder holder = chunks.getUpdatingChunkIfPresent(key);
		if (holder == null) holder = (ChunkHolder) pending.get(key);
		var playerChunks = (Map<?, ?>) readField(chunks.getDistanceManager(), DistanceManager.class, "playersPerChunk");
		var nearbyTickets = new java.util.ArrayList<String>();
		int nearbyTicketChunks = 0;
		// A neighboring ticket can hold this chunk too; record all sources within the player's maximum radius.
		if (tickets != null) for (int x = -33; x <= 33; x++) for (int z = -33; z <= 33; z++) {
			long nearby = ChunkPos.pack(remoteChunk.x() + x, remoteChunk.z() + z);
			var found = tickets.getTickets(nearby);
			if (found != null && !found.isEmpty()) {
				nearbyTicketChunks++;
				if (nearbyTickets.size() < 16) nearbyTickets.add(x + "," + z + "=" + found);
			}
		}
		Wildercord.LOGGER.info("[masters-unload-wait] phase={} elapsed={} eventAt={} forced={} chunkPresent={} entityTicking={} noSave={} masterRemoved={} masterReason={} remoteRemoved={} remotePlayers={} nativePlayerChunk={} holderLevel={} saveReady={} saveFutureDone={} pendingUnload={} toDrop={} unloadQueue={} ownTickets={} nearbyTicketChunks={} nearbyTickets={}",
			phase, level.getGameTime() - releasedAt, chunkUnloadAt, level.getForceLoadedChunks().contains(key),
			cache.getChunkNow(remoteChunk.x(), remoteChunk.z()) != null, level.areEntitiesActuallyLoadedAndTicking(remoteChunk),
			level.noSave(), unloaded.isRemoved(), unloaded.getRemovalReason(), remote.isRemoved(),
			level.players().stream().filter(player -> player.chunkPosition().equals(remoteChunk)).map(Entity::getUUID).toList(),
			playerChunks.get(key), holder == null ? null : holder.getTicketLevel(), holder == null ? null : holder.isReadyForSaving(),
			holder == null ? null : holder.getSaveSyncFuture().isDone(), pending.containsKey(key),
			((Set<?>) readField(chunks, ChunkMap.class, "toDrop")).contains(key),
			((Queue<?>) readField(chunks, ChunkMap.class, "unloadQueue")).size(), tickets == null ? null : tickets.getTickets(key),
			nearbyTicketChunks, nearbyTickets);
	}

	private void noUnloadReward() {
		check(MasterVictories.progress(remote).equals(remoteProgress) && remote.rewards == remoteRewards
			&& remote.totalExperience == remoteExperience && Techniques.book(remote).learned().size() == remoteLessons,
			"Unloading/reloading cannot grant a clear, lesson, reward message or experience");
	}

	private void legitimateDeath(Challenger winner) {
		SwordMaster defeated = lobby(winner, stage);
		check(SwordMaster.ready(winner) == 1, "The legitimate-death comparison has a real ready roster");
		defeated.customServerAiStep(winner.level());
		defeated.setNoAi(false);
		check(defeated.hurtServer(winner.level(), winner.level().damageSources().playerAttack(winner), 100_000)
			&& !defeated.isAlive(), "A legitimate enrolled hit follows the native death path");
		check(MasterVictories.progress(winner).cleared(MastersRules.EMBER) && winner.rewards == 1,
			"Death awards the accepted roster before clearing it");
		cleaned(defeated);
		int lessons = Techniques.book(winner).learned().size();
		defeated.onRemoval(Entity.RemovalReason.KILLED);
		defeated.discard();
		cleaned(defeated);
		check(winner.rewards == 1 && Techniques.book(winner).learned().size() == lessons,
			"Repeated terminal cleanup cannot duplicate a legitimate first-clear reward");
	}

	private static SwordMaster lobby(ServerPlayer player, BlockPos center) {
		SwordMaster master = AuraWorld.SWORD_MASTER.create(player.level(), EntitySpawnReason.COMMAND);
		check(master != null, "The native lifecycle fixture constructs its registered master");
		master.snapTo(center.getX() + .5, center.getY() + 1, center.getZ() + 3.5, 180, 0);
		master.setNoAi(true);
		check(player.level().addFreshEntity(master), "The lifecycle fixture enters the real entity manager");
		master.mobInteract(player, InteractionHand.MAIN_HAND);
		master.mobInteract(player, InteractionHand.MAIN_HAND);
		check(master.challengers().contains(player.getUUID()), "The lifecycle fixture enrolls by actual consent");
		return master;
	}

	private static Challenger challenger(ServerLevel level, String name, BlockPos at) {
		Challenger player = new Challenger(level, name);
		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(at.getX() + .5, at.getY() + 1, at.getZ() + .5, 0, 0);
		level.addNewPlayer(player);
		return player;
	}

	private static void floor(ServerLevel level, BlockPos center) {
		for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++)
			level.setBlockAndUpdate(center.offset(x, 0, z), Blocks.STONE.defaultBlockState());
	}

	private static void cleaned(SwordMaster master) {
		check(!active().contains(master) && bar(master).getPlayers().isEmpty() && master.challengers().isEmpty()
			&& ((Map<?, ?>) field(master, "invitations")).isEmpty(), "Terminal cleanup releases registry, roster, invitations and bossbar viewers");
		check(master.getTarget() == null && master.getNavigation().isDone() && master.slashTarget == null && master.slashAim == null
			&& field(master, "home") == null && field(master, "challenger") == null && field(master, "waitingFor") == null,
			"Terminal cleanup releases target, navigation and arena references");
		check(master.attackAnimation() == 0 && master.attackAimPitch() == 0 && !master.state(AuraFighter.WINDUP)
			&& !master.state(AuraFighter.DASH) && !master.guarding() && !master.afterburnPending() && !master.pursuitPending()
			&& !master.reprisePending() && !master.fracturePending(), "Terminal cleanup cancels every combat controller and visual warning");
	}

	private static Set<?> active() { return (Set<?>) field(null, "ACTIVE"); }
	private static ServerBossEvent bar(SwordMaster master) { return (ServerBossEvent) field(master, "bar"); }
	private static Object field(SwordMaster master, String name) {
		return readField(master, SwordMaster.class, name);
	}
	private static Object readField(Object instance, Class<?> owner, String name) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(instance);
		} catch (ReflectiveOperationException e) { throw new AssertionError(e); }
	}
	private static void check(boolean result, String message) {
		if (!result) throw new AssertionError(message);
	}
}
