package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.aura.arts.RimeArts;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Statuses;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Real Hailfall, admitted by two native Punch observations and SwordStrings.request. Each mutation runs on the
 * scheduler at release +3: the first ray has run at +2, but its separately scheduled stone is not due until +4.
 * A persistent dedicated server keeps those callbacks and their actual targets alive after its real TCP owner leaves.
 * No lifecycle event is invoked directly, no performer is replaced, and no callback is called by the test.
 */
public final class HailfallReleasedOwnerTest implements FabricClientGameTest {

	private enum Change {
		CONTROL(true), WEAPON(true), METHOD(true), INTERRUPTION(true),
		DEATH(false), RESPAWN(false), DIMENSION(false), ROUND_TRIP(false), DISCONNECT(false);

		final boolean survives;
		Change(boolean survives) { this.survives = survives; }
	}

	private static final float HEALTH = 200;
	private static final Vec3 FEET = new Vec3(.5, 100, .5);
	private static final int FINISH = ArtRules.HAIL_TICKS + 8;
	private Probe current;

	private static final class Probe {
		final Change change;
		final ServerPlayer original;
		final ServerLevel level;
		final LivingEntity target;
		final LivingEntity destinationTarget;
		ReleasedArtOwner binding;
		java.util.function.BooleanSupplier physical;
		long accepted, released, mutated;
		int releases;
		float firstHealth;
		boolean profiled, firstChill, firstFrozen, finished;
		Throwable failure;

		Probe(Change change, ServerPlayer player, LivingEntity target, LivingEntity destinationTarget) {
			this.change = change;
			this.original = player;
			this.level = player.level();
			this.target = target;
			this.destinationTarget = destinationTarget;
		}
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		Properties properties = new Properties();
		properties.setProperty("server-ip", "127.0.0.1");
		properties.setProperty("server-port", Integer.toString(localPort()));
		properties.setProperty("online-mode", "false");
		properties.setProperty("enforce-secure-profile", "false");
		properties.setProperty("pause-when-empty-seconds", "-1");
		properties.setProperty("view-distance", "3");
		properties.setProperty("simulation-distance", "3");
		AuraApi.StringHook hook = this::released;
		try (var server = context.worldBuilder().createServer(properties)) {
			TestDedicatedServerConnection connection = server.connect();
			try {
				connection.waitForChunksDownload();
				connection.waitForClientboundPackets();
				server.runCommand("gamerule minecraft:spawn_mobs false");
				server.runCommand("gamerule minecraft:natural_health_regeneration false");
				server.runOnServer(s -> {
					arena(s.overworld());
					arena(s.getLevel(Level.NETHER));
					AuraApi.onString(hook);
				});
				for (Change change : Change.values()) {
					server.runOnServer(s -> begin(s, change));
					server.waitFor(s -> current.finished, 300);
					server.runOnServer(s -> {
						Probe probe = current;
						if (probe.failure != null) throw new AssertionError(change + " failed", probe.failure);
						check(probe.releases == 1, change + ": the actual registered performer completed exactly once");
						check(probe.mutated - probe.released == 3, change + ": mutation occurred between ray and nested stone");
						Wildercord.LOGGER.info("HAILFALL_OWNER case={} accepted={} release={} mutation={} firstHealth={} chill={} frozen={} finalHealth={} binding={}",
							change, probe.accepted, probe.released, probe.mutated, probe.firstHealth, probe.firstChill,
							probe.firstFrozen, probe.target.getHealth(), probe.binding.valid());
						if (change != Change.DISCONNECT) restore(s, probe);
					});
					if (change != Change.DISCONNECT) {
						context.runOnClient(mc -> mc.gui.setScreen(null));
						context.waitTicks(5);
					}
				}
				// Fabric's TestDedicatedServerConnection.close() rejects an already-disconnected client. Reconnect first,
				// proving that a new real connection with the same UUID cannot revive the original release binding.
				context.waitFor(mc -> mc.level == null, 200);
				context.setScreen(TitleScreen::new);
				connection = server.connect();
				connection.waitForChunksDownload();
				server.runOnServer(s -> {
					ServerPlayer rejoined = s.getPlayerList().getPlayer(current.original.getUUID());
					check(rejoined != null && rejoined != current.original && rejoined.isAlive(), "Reconnect creates a live, different original-UUID body");
					check(!current.binding.valid(), "A real reconnect never revives the old release");
					check(ReleasedArtOwner.capture(rejoined).valid(), "A new release can bind to the newly connected body");
					untouched(current.target, "Reconnect leaves the retired cloud's target untouched");
					current.target.discard();
					current.destinationTarget.discard();
				});
			} finally {
				server.runOnServer(s -> AuraApi.stringHooks().remove(hook));
				if (context.computeOnClient(mc -> mc.level != null)) connection.close();
				else context.setScreen(TitleScreen::new);
			}
		}
	}

	private void begin(MinecraftServer server, Change change) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		prepare(player, server.overworld());
		LivingEntity target = target(server.overworld());
		LivingEntity destination = target(server.getLevel(Level.NETHER));
		Probe probe = new Probe(change, player, target, destination);
		current = probe;
		check(player.connection.getRemoteAddress() instanceof InetSocketAddress address && address.getAddress().isLoopbackAddress(),
			"The original Hailfall owner is a real loopback TCP client");
		SwordStrings.forget(player.getUUID());
		// Native attack handlers record the server-observed marks. The test never calls swung(), performs an art
		// directly, fabricates a ledger entry, or supplies an invented last-hit target.
		player.jumpFromGround();
		player.setOnGround(false);
		player.setDeltaMovement(Vec3.ZERO);
		check(SwordString.Token.LEAP.fits(SwordStrings.observedMarks(player)), "The first native Punch observes a leap");
		player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
		// Two scheduler ticks guarantee distinct server ticks even when runOnServer runs before the tick's END event.
		Scheduler.later(2, () -> checked(probe, () -> {
			player.setOnGround(true);
			player.setShiftKeyDown(true);
			player.setDeltaMovement(Vec3.ZERO);
			check(SwordString.Token.LOW.fits(SwordStrings.observedMarks(player)), "The second native Punch observes a low stroke");
			player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
			AuraApi.StringArt art = AuraApi.artOf(player, RimeArts.HAILFALL).orElseThrow();
			check(SwordStrings.saw(player, art), "The real leap/low suffix is admissible before requesting Hailfall");
			probe.accepted = player.level().getGameTime();
			float before = Aura.aura(player);
			SwordStrings.request(player, new SwordStrings.Perform(RimeArts.HAILFALL,
				List.of(SwordString.Token.LEAP.bit(), SwordString.Token.LOW.bit())));
			check(Aura.aura(player) < before && SwordStrings.readyAt(player, RimeArts.HAILFALL) > probe.accepted,
				"The observed request actually pays and starts Hailfall's cooldown");
			check(!SwordStrings.saw(player, art), "The accepted request consumes its authentic observed strokes");
		}));
	}

	private void released(ServerPlayer player, AuraApi.StringArt art, AuraApi.StringContext move) {
		Probe probe = current;
		if (probe == null || player != probe.original || !RimeArts.HAILFALL.equals(art.id())) return;
		checked(probe, () -> {
			probe.releases++;
			probe.released = player.level().getGameTime();
			MastersStyleRules.Style profile = MastersStyleRules.of(art.id());
			probe.profiled = profile != null;
			probe.physical = MastersArts.continuation(player);
			check(probe.released - probe.accepted == (profile == null ? 0 : profile.windup()),
				"The actual Hailfall release follows its current profile, including the pre-timeline instant version");
			check(move.marks().size() == 2 && SwordString.Token.LEAP.fits(move.marks().getFirst())
				&& SwordString.Token.LOW.fits(move.marks().getLast()), "The actual performer received the authoritative leap/low marks");
			probe.binding = ReleasedArtOwner.capture(player);
			check(probe.binding.valid() && probe.binding.level() == probe.level, "Released ownership starts on the exact original body/world");
			untouched(probe.target, "The cloud target is beyond the initial cut and has no pre-existing chill");
			Scheduler.later(3, () -> checked(probe, () -> mutate(probe)));
			Scheduler.later(5, () -> checked(probe, () -> firstStone(probe)));
			Scheduler.later(FINISH, () -> checked(probe, () -> finish(probe)));
		});
	}

	private static void mutate(Probe probe) {
		ServerPlayer player = probe.original;
		MinecraftServer server = probe.level.getServer();
		probe.mutated = probe.level.getGameTime();
		check(probe.mutated == probe.released + 3, "Mutation is precisely one tick after the first ray and one before its stone");
		untouched(probe.target, "At the ray/stone gap the real target has not been damaged or chilled");
		switch (probe.change) {
			case CONTROL -> { }
			case WEAPON -> player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			case METHOD -> player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, Aura.aura(player), 0));
			case INTERRUPTION -> {
				// The native item-use interruption makes this an actual successful interrupt even before Hailfall
				// gains a physical recovery profile. Once profiled, the same call also cancels that recovery.
				player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
				player.startUsingItem(InteractionHand.OFF_HAND);
				check(player.isUsingItem(), "A native interruptible use is active after release");
				if (probe.profiled) check(probe.physical.getAsBoolean(), "The profiled form still has an actual physical continuation before interruption");
				check(Statuses.interrupt(player) && !player.isUsingItem(), "A real shared interruption succeeds after release");
				if (probe.profiled) check(!probe.physical.getAsBoolean(), "The shared interruption really cancels the form's physical continuation");
			}
			case DEATH -> {
				player.kill(probe.level);
				check(!player.isAlive(), "Native kill really kills the released owner before the nested stone");
			}
			case RESPAWN -> {
				player.kill(probe.level);
				check(!player.isAlive(), "Respawn starts from an actual native death");
				ServerPlayer replacement = respawn(server, player);
				prepare(replacement, probe.level);
				check(ReleasedArtOwner.capture(replacement).valid(), "The connected replacement body can own a new release");
			}
			case DIMENSION, ROUND_TRIP -> {
				check(player.teleportTo(server.getLevel(Level.NETHER), FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false),
					"Native teleport crosses to a different ServerLevel");
				check(player.level() != probe.level, "The original body actually departed its release world");
				if (probe.change == Change.ROUND_TRIP) {
					// Deliberately do not poll binding.valid() in the foreign world. Only the real level-change event
					// can remember this same-call departure once the exact body returns before any scheduled callback.
					check(player.teleportTo(probe.level, FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "Native return completes in the same callback");
					check(player.isAlive() && player.level() == probe.level && server.getPlayerList().getPlayer(player.getUUID()) == player,
						"The exact live connected original body is back before the old binding is inspected");
					check(ReleasedArtOwner.capture(player).valid(), "The returned body can start a fresh release lifetime");
				}
			}
			case DISCONNECT -> {
				Connection socket = server.getConnection().getConnections().stream()
					.filter(candidate -> candidate.getPacketListener() == player.connection).findFirst().orElseThrow();
				check(socket.isConnected() && !socket.isMemoryConnection(), "Disconnect acts on the owner's actual connected TCP socket");
				socket.disconnect(Component.literal("Hailfall released-owner lifecycle test"));
				check(!socket.isConnected(), "The actual network channel is closed before the nested stone");
				// Drain the native closed-connection lifecycle synchronously, as ServerConnectionListener.tick does.
				// This calls normal player removal and Fabric notifications; it does not invoke an event or hook directly.
				socket.handleDisconnection();
				check(server.getPlayerList().getPlayer(player.getUUID()) == null && player.isRemoved(),
					"The real disconnect removes the original body from the dedicated server");
			}
		}
		// Do not poll the old binding after a retiring transition: valid() itself can retire a lifetime. The real
		// production ray/stone callbacks must discover the invalid owner without help from a test-side poll.
		if (probe.change.survives) check(probe.binding.valid(), "An ordinary post-release change keeps its live owner");
	}

	private static void firstStone(Probe probe) {
		check(probe.level.getGameTime() == probe.released + 5, "First-stone evidence is taken after +4 and before the second stone at +6");
		probe.firstHealth = probe.target.getHealth();
		probe.firstChill = probe.target.hasEffect(MobEffects.SLOWNESS);
		probe.firstFrozen = probe.target.getTicksFrozen() > 0;
		if (probe.change.survives) {
			check(probe.firstHealth < HEALTH && probe.firstChill && probe.firstFrozen,
				probe.change + ": the already-released nested stone actually damages and chills its target");
		} else untouched(probe.target, probe.change + ": the pending nested stone causes no damage or chill");
		untouched(probe.destinationTarget, probe.change + ": the old cloud cannot retarget a foreign-world victim");
	}

	private static void finish(Probe probe) {
		check(probe.level.getGameTime() >= probe.released + FINISH, "The original world kept ticking past every scheduled ray and stone");
		check(probe.binding.valid() == probe.change.survives, "The original binding never revives during the remaining cloud lifetime");
		if (!probe.change.survives) untouched(probe.target, probe.change + ": every later ray/stone remains harmless");
		untouched(probe.destinationTarget, "Every destination-world target remains untouched through the whole cloud");
		probe.finished = true;
	}

	private static void restore(MinecraftServer server, Probe probe) {
		ServerPlayer player = server.getPlayerList().getPlayer(probe.original.getUUID());
		if (!player.isAlive()) player = respawn(server, player);
		prepare(player, probe.level);
		if (!probe.change.survives) {
			check(!probe.binding.valid(), "Restoring a live connected original-world player cannot restore the old release");
			check(ReleasedArtOwner.capture(player).valid(), "A fresh owner binding is usable after the native lifecycle transition");
		}
		probe.target.discard();
		probe.destinationTarget.discard();
	}

	private static ServerPlayer respawn(MinecraftServer server, ServerPlayer dead) {
		dead.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
		ServerPlayer replacement = server.getPlayerList().getPlayer(dead.getUUID());
		check(replacement != null && replacement != dead && replacement.isAlive() && dead.isRemoved(),
			"The native respawn request replaces the original body while preserving its UUID");
		check(replacement.connection.player == replacement, "The actual connection now owns the replacement body");
		return replacement;
	}

	private static void prepare(ServerPlayer player, ServerLevel level) {
		player.setGameMode(GameType.SURVIVAL);
		player.teleportTo(level, FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false);
		player.setNoGravity(true);
		player.setDeltaMovement(Vec3.ZERO);
		player.setOnGround(true);
		player.setShiftKeyDown(false);
		player.setSprinting(false);
		player.stopUsingItem();
		player.removeAllEffects();
		player.clearFire();
		player.setHealth(player.getMaxHealth());
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("rime", AuraRules.SOVEREIGN, 4500, 160, 0));
		player.setAttached(SwordStrings.COOLDOWNS, SwordStrings.Cooldowns.NONE);
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
	}

	private static LivingEntity target(ServerLevel level) {
		var target = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		check(target != null, "A native Hailfall target exists");
		target.setNoAi(true);
		target.setNoGravity(true);
		target.setPersistenceRequired();
		target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
		target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
		target.setHealth(HEALTH);
		target.snapTo(FEET.x, FEET.y, FEET.z + ArtRules.HAIL_AHEAD, 180, 0);
		level.addFreshEntity(target);
		// The first stone's maximum random offset is only HAIL_RADIUS * .2, so the central target is guaranteed
		// inside HAIL_STONE_REACH without seeding world RNG or making later random stones part of the assertion.
		check(ArtRules.HAIL_RADIUS * .2 < ArtRules.HAIL_STONE_REACH, "The first native stone necessarily reaches the target");
		check(ArtRules.HAIL_AHEAD > ArtRules.HAIL_CUT_REACH + target.getBbWidth() / 2,
			"The actual target starts outside the opening cut");
		return target;
	}

	private static void arena(ServerLevel level) {
		check(level != null, "The native test dimension exists");
		for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++) level.setChunkForced(x, z, true);
		for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
			level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 2);
			for (int y = 100; y <= 107; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
		}
	}

	private static void untouched(LivingEntity target, String reason) {
		check(target.isAlive() && target.getHealth() == HEALTH && !target.hasEffect(MobEffects.SLOWNESS)
			&& target.getTicksFrozen() == 0, reason);
	}

	private static void checked(Probe probe, Runnable action) {
		if (probe.failure != null) return;
		try { action.run(); }
		catch (RuntimeException | AssertionError failure) {
			probe.failure = failure;
			probe.finished = true;
		}
	}

	private static int localPort() {
		try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) { return socket.getLocalPort(); }
		catch (java.io.IOException failure) { throw new AssertionError("Could not reserve local native-test TCP port", failure); }
	}

	private static void check(boolean value, String reason) {
		if (!value) throw new AssertionError(reason);
	}
}
