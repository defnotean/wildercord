package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtFields;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.CrimsonArts;
import dev.wildercord.aura.arts.HollowArts;
import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Statuses;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Actual registered Collapse/Red Rain performers, admitted through SwordStrings and their eight-tick windups.
 * Targets enter only after the release burst. Real body/world transitions retire the released field while ordinary
 * physical cancellation leaves its delayed work alive. A dedicated server keeps ticking after its TCP owner leaves.
 * No lifecycle event or field callback is invoked directly; ArtFieldMutationSafetyTest retains its independent scope.
 */
public final class GroundFieldReleasedOwnerTest implements FabricClientGameTest {

	private enum Change {
		CONTROL(true), WEAPON(true), METHOD(true), INTERRUPTION(true),
		DEATH(false), RESPAWN(false), DIMENSION(false), ROUND_TRIP(false), DISCONNECT(false);

		final boolean survives;
		Change(boolean survives) { this.survives = survives; }
	}

	private record Art(String id, String method, String field, double ahead, int delay, int duration, Reactions.Mark mark) {}
	private static final Art COLLAPSE = new Art(HollowArts.COLLAPSE, HollowArts.METHOD, HollowArts.WELL,
		ArtRules.COLLAPSE_AHEAD, ArtRules.COLLAPSE_TICKS, ArtRules.COLLAPSE_TICKS, Reactions.Mark.SHADOWED);
	private static final Art RED_RAIN = new Art(CrimsonArts.RED_RAIN, CrimsonArts.METHOD, CrimsonArts.RAIN,
		ArtRules.RAIN_AHEAD, ArtRules.BLEED_PERIOD, ArtRules.RAIN_TICKS, Reactions.Mark.BLEEDING);
	private record Case(Art art, Change change, boolean duringDamage) {
		String label() { return art.id() + "/" + change + (duringDamage ? "/damage-callback" : ""); }
	}
	private record Hit(LivingEntity target, DamageSource source) {}
	private static Consumer<Hit> onDamage;
	private static boolean listening;
	private static final float HEALTH = 200;
	private static final Vec3 FEET = new Vec3(.5, 100, .5);
	private Probe current;

	private static final class Probe {
		final Case scenario;
		final ServerPlayer original;
		final ServerLevel level;
		Vec3 centre;
		LivingEntity target, destinationTarget, secondTarget, admitted;
		ReleasedArtOwner binding;
		BooleanSupplier physical;
		long accepted, released, armed, mutated;
		int releases, damageCallbacks;
		float firstHealth, ownerHealthAfterMutation;
		boolean firstMark, finished;
		Throwable failure;

		Probe(Case scenario, ServerPlayer player) {
			this.scenario = scenario;
			this.original = player;
			this.level = player.level();
		}
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!listening) {
			listening = true;
			// The native listener retains no test/world/player once each case releases its callback.
			ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, damage, blocked) -> {
				Consumer<Hit> action = onDamage;
				if (action != null && damage > 0) action.accept(new Hit(target, source));
			});
		}
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
				List<Case> cases = new ArrayList<>();
				for (Art art : List.of(COLLAPSE, RED_RAIN)) {
					for (Change change : Change.values()) cases.add(new Case(art, change, false));
				}
				// These narrow callback cases complement the existing mutation-safety suite: retirement happens inside
				// an actual crush/bleed hit, so a selected next victim and post-hit mark/drink must stop synchronously.
				for (Art art : List.of(COLLAPSE, RED_RAIN)) {
					cases.add(new Case(art, Change.DEATH, true));
					cases.add(new Case(art, Change.ROUND_TRIP, true));
				}
				for (Case scenario : cases) {
					server.runOnServer(s -> begin(s, scenario));
					server.waitFor(s -> current.finished, 250);
					server.runOnServer(s -> {
						Probe probe = current;
						if (probe.failure != null) throw new AssertionError(scenario.label() + " failed", probe.failure);
						check(probe.releases == 1, scenario.label() + ": registered performer completes exactly once");
						check(probe.armed - probe.released == 3, scenario.label() + ": change is made or armed at release +3");
						check(probe.mutated - probe.released == (scenario.duringDamage() ? scenario.art().delay() : 3),
							scenario.label() + ": native transition runs at the intended delayed-field boundary");
						Wildercord.LOGGER.info("GROUND_FIELD_OWNER case={} accepted={} release={} mutation={} firstHealth={} mark={} finalHealth={} callbacks={} binding={}",
							scenario.label(), probe.accepted, probe.released, probe.mutated, probe.firstHealth, probe.firstMark,
							probe.target.getHealth(), probe.damageCallbacks, probe.binding.valid());
						if (scenario.change() != Change.DISCONNECT) restore(s, probe);
					});
					if (scenario.change() == Change.DISCONNECT) {
						// Fabric rejects close() on an already-disconnected client. A real reconnect also proves that the
						// same UUID's new live body cannot revive either art's old original-body binding.
						context.waitFor(mc -> mc.level == null, 200);
						context.setScreen(TitleScreen::new);
						connection = server.connect();
						connection.waitForChunksDownload();
						connection.waitForClientboundPackets();
						server.runOnServer(s -> {
							Probe probe = current;
							ServerPlayer rejoined = s.getPlayerList().getPlayer(probe.original.getUUID());
							check(rejoined != null && rejoined != probe.original && rejoined.isAlive(), "Reconnect creates a new live original-UUID body");
							check(!probe.binding.valid(), "A new real connection never revives the retired ground-field binding");
							check(ReleasedArtOwner.capture(rejoined).valid(), "The reconnected body can own a fresh release");
							untouched(probe.target, "Reconnect cannot revive delayed ground-field damage or marks");
							discard(probe);
						});
					}
					context.runOnClient(mc -> mc.gui.setScreen(null));
					context.waitTicks(5);
				}
			} finally {
				onDamage = null;
				server.runOnServer(s -> AuraApi.stringHooks().remove(hook));
				if (context.computeOnClient(mc -> mc.level != null)) connection.close();
				else context.setScreen(TitleScreen::new);
			}
		}
	}

	private void begin(MinecraftServer server, Case scenario) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		prepare(player, server.overworld(), scenario.art().method());
		Probe probe = new Probe(scenario, player);
		current = probe;
		check(player.connection.getRemoteAddress() instanceof InetSocketAddress address && address.getAddress().isLoopbackAddress(),
			"The ground-field owner is an actual loopback TCP client");
		// Accept on the scheduler's own clock, so the asserted eight-tick release cannot depend on whether
		// runOnServer happened before or after this server tick's END callback.
		Scheduler.later(2, () -> checked(probe, () -> {
			MastersStyleRules.Style profile = MastersStyleRules.of(scenario.art().id());
			check(profile != null && profile.windup() == 8 && profile.targets() == MastersStyleRules.TargetPolicy.GROUND_AHEAD,
				"Each actual ground-field art has the new eight-tick fixed-release profile");
			check(scenario.art().delay() == (scenario.art() == COLLAPSE ? 16 : 10),
				"The authored damage remains release +16 for Collapse and release +10 for Red Rain");
			AuraApi.StringArt art = AuraApi.artOf(player, scenario.art().id()).orElseThrow();
			probe.accepted = player.level().getGameTime();
			float before = Aura.aura(player);
			check(SwordStrings.perform(player, art, List.of(SwordString.Token.LEAP.bit(), SwordString.Token.LOW.bit())),
				"The registered performer is admitted through SwordStrings, not called directly");
			check(Aura.aura(player) < before && SwordStrings.readyAt(player, art.id()) > probe.accepted,
				"The accepted ground-field windup actually pays and starts its cooldown");
			check(probe.releases == 0 && ArtFields.count(player, scenario.art().field()) == 0,
				"Acceptance has neither run the performer nor created its field");
		}));
	}

	private void released(ServerPlayer player, AuraApi.StringArt art, AuraApi.StringContext move) {
		Probe probe = current;
		if (probe == null || player != probe.original || !probe.scenario.art().id().equals(art.id())) return;
		checked(probe, () -> {
			probe.releases++;
			probe.released = player.level().getGameTime();
			check(probe.released - probe.accepted == 8, "The actual registered performer releases exactly eight ticks after acceptance");
			check(ArtFields.count(player, probe.scenario.art().field()) == 1, "The release created exactly one actual field");
			probe.binding = ReleasedArtOwner.capture(player);
			probe.physical = MastersArts.continuation(player);
			check(probe.binding.valid() && probe.binding.level() == probe.level && probe.physical.getAsBoolean(),
				"Released field and physical recovery begin on the original connected body/world");
			Vec3 ahead = player.position().add(ArtKit.flat(player).scale(probe.scenario.art().ahead()));
			Vec3 centre = ArtKit.floor(probe.level, ahead.add(0, 1, 0), 1.5, 3);
			check(centre != null, "The released field has an actual solid arena floor");
			probe.centre = centre;
			// Completion hooks run after the performer, including Red Rain's immediate burst. Neither victim exists
			// until now, so subsequent damage and marks can only be evidence of this field's delayed native pulses.
			probe.target = target(probe.level, centre);
			probe.destinationTarget = target(probe.level.getServer().getLevel(Level.NETHER), centre);
			check(ArtFields.inside(player, probe.scenario.art().field(), probe.target), "The new victim is at the actual field centre");
			untouched(probe.target, "The post-release victim has no immediate-burst damage or marks");
			if (probe.scenario.duringDamage()) {
				probe.secondTarget = target(probe.level, centre.add(.8, 0, 0));
				check(ArtFields.inside(player, probe.scenario.art().field(), probe.secondTarget), "Both callback victims are inside the same actual ground field");
				player.setHealth(12);
			}
			Scheduler.later(3, () -> checked(probe, () -> armOrMutate(probe)));
			Scheduler.later(probe.scenario.art().delay() - 1, () -> checked(probe, () -> {
				untouched(probe.target, "No delayed damage or marks occur before the field's authored first damaging beat");
				if (probe.secondTarget != null) untouched(probe.secondTarget, "The second victim is untouched before that beat");
			}));
			Scheduler.later(probe.scenario.art().delay() + 1, () -> checked(probe, () -> firstPulse(probe)));
			Scheduler.later(probe.scenario.art().duration() + 8, () -> checked(probe, () -> finish(probe)));
		});
	}

	private static void armOrMutate(Probe probe) {
		probe.armed = probe.level.getGameTime();
		check(probe.armed == probe.released + 3, "The post-release change precedes Red Rain's +10 bleed and Collapse's +16 crush");
		untouched(probe.target, "The field has not damaged or marked its victim at release +3");
		if (!probe.scenario.duringDamage()) {
			mutate(probe);
			return;
		}
		onDamage = hit -> {
			if (hit.source().getEntity() != probe.original || (hit.target() != probe.target && hit.target() != probe.secondTarget)) return;
			checked(probe, () -> {
				probe.damageCallbacks++;
				check(probe.damageCallbacks == 1, "A retired pulse must not damage the other already-selected victim");
				probe.admitted = hit.target();
				double radius = probe.scenario.art() == COLLAPSE ? ArtRules.COLLAPSE_RADIUS : ArtRules.RAIN_RADIUS;
				check(ArtKit.around(probe.original, probe.centre, radius, 1.5, 3, 8)
					.containsAll(List.of(probe.target, probe.secondTarget)),
					"Both live hostile victims are still in the actual damaging footprint when the first hit retires its owner");
				mutate(probe);
				probe.ownerHealthAfterMutation = probe.original.getHealth();
			});
		};
	}

	private static void mutate(Probe probe) {
		ServerPlayer player = probe.original;
		MinecraftServer server = probe.level.getServer();
		probe.mutated = probe.level.getGameTime();
		check(probe.mutated == probe.released + (probe.scenario.duringDamage() ? probe.scenario.art().delay() : 3),
			"The native owner change occurs on the exact scheduled boundary");
		switch (probe.scenario.change()) {
			case CONTROL -> check(probe.physical.getAsBoolean(), "The unchanged owner is still in physical recovery");
			case WEAPON -> {
				player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				check(!probe.physical.getAsBoolean(), "Removing the weapon really cancels physical continuation");
			}
			case METHOD -> {
				player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, Aura.aura(player), 0));
				check(!probe.physical.getAsBoolean(), "Changing method really cancels physical continuation");
			}
			case INTERRUPTION -> {
				check(probe.physical.getAsBoolean(), "Actual physical recovery exists before the shared interruption");
				check(Statuses.interrupt(player), "A real shared interruption succeeds after release");
				check(!probe.physical.getAsBoolean(), "The interruption cancels actual physical continuation");
			}
			case DEATH -> {
				player.kill(probe.level);
				check(!player.isAlive(), "Native kill really kills the released owner");
			}
			case RESPAWN -> {
				player.kill(probe.level);
				check(!player.isAlive(), "Body replacement starts with actual native death");
				ServerPlayer replacement = respawn(server, player);
				prepare(replacement, probe.level, probe.scenario.art().method());
				check(ReleasedArtOwner.capture(replacement).valid(), "A live connected replacement may own a fresh release");
			}
			case DIMENSION, ROUND_TRIP -> {
				check(player.teleportTo(server.getLevel(Level.NETHER), FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false),
					"Native teleport crosses to another ServerLevel");
				check(player.level() != probe.level, "The actual original body departed its release world");
				if (probe.scenario.change() == Change.ROUND_TRIP) {
					// Never poll the old binding in the foreign world: the native change-level event must remember
					// this same-call departure after the exact live connected body returns before any field callback.
					check(player.teleportTo(probe.level, FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "Native return completes in the same callback");
					check(player.isAlive() && player.level() == probe.level && server.getPlayerList().getPlayer(player.getUUID()) == player,
						"The same original live connected body is back before the old field resumes");
					check(ReleasedArtOwner.capture(player).valid(), "The returned body can start a separate fresh release lifetime");
				}
			}
			case DISCONNECT -> {
				Connection socket = server.getConnection().getConnections().stream()
					.filter(candidate -> candidate.getPacketListener() == player.connection).findFirst().orElseThrow();
				check(socket.isConnected() && !socket.isMemoryConnection(), "Disconnect uses the owner's actual connected TCP socket");
				socket.disconnect(Component.literal("Ground-field released-owner lifecycle test"));
				check(!socket.isConnected(), "The owner's real network channel has closed");
				// Drain native closed-connection cleanup as ServerConnectionListener.tick does; no event invoker.
				socket.handleDisconnection();
				check(server.getPlayerList().getPlayer(player.getUUID()) == null && player.isRemoved(),
					"Native disconnect removes the original owner body from the dedicated server");
			}
		}
		// valid() can itself retire a binding. The retiring cases deliberately leave that discovery to the actual
		// field callback, including when its owner returned synchronously during the first damage callback.
		if (probe.scenario.change().survives) check(probe.binding.valid(), "Ordinary physical cancellation preserves released ownership");
	}

	private static void firstPulse(Probe probe) {
		check(probe.level.getGameTime() == probe.released + probe.scenario.art().delay() + 1,
			"Evidence is recorded immediately after the actual first damaging beat");
		probe.firstHealth = probe.target.getHealth();
		probe.firstMark = Reactions.has(probe.target, probe.scenario.art().mark());
		if (probe.scenario.duringDamage()) {
			check(probe.damageCallbacks == 1 && probe.admitted != null && probe.admitted.getHealth() < HEALTH,
				"A real delayed damage callback admitted exactly one hit before native owner retirement");
			LivingEntity next = probe.admitted == probe.target ? probe.secondTarget : probe.target;
			untouched(next, "The same retired pulse cannot damage or mark its next already-selected target");
			check(!Reactions.has(probe.admitted, probe.scenario.art().mark()), "The field cannot add its post-hit mark after owner retirement");
			if (probe.scenario.art() == RED_RAIN) {
				check(probe.original.getHealth() == probe.ownerHealthAfterMutation, "Rain cannot drink-heal its retired owner after the admitted hit");
			}
		} else if (probe.scenario.change().survives) {
			check(probe.firstHealth < HEALTH && probe.firstMark,
				probe.scenario.label() + ": the already-released field really damages and marks its post-burst victim");
		} else untouched(probe.target, probe.scenario.label() + ": the retired field's delayed beat causes no damage or mark");
		untouched(probe.destinationTarget, "The field never retargets a victim in its owner's destination world");
	}

	private static void finish(Probe probe) {
		check(probe.level.getGameTime() >= probe.released + probe.scenario.art().duration() + 8,
			"The original world has ticked beyond the whole released field lifetime");
		check(probe.binding.valid() == probe.scenario.change().survives, "The original field ownership never revives after retirement");
		check(ArtFields.count(probe.original, probe.scenario.art().field()) == 0, "No expired or retired ground field remains active");
		if (probe.scenario.duringDamage()) {
			check(probe.damageCallbacks == 1, "No later field pulse damages either target after callback retirement");
			untouched(probe.admitted == probe.target ? probe.secondTarget : probe.target, "The selected next victim stays untouched for the whole old field lifetime");
			check(!Reactions.has(probe.admitted, probe.scenario.art().mark()), "The first admitted victim gains no later old-field mark");
		} else if (!probe.scenario.change().survives) {
			untouched(probe.target, "Every later delayed field beat stays harmless after owner retirement");
		}
		untouched(probe.destinationTarget, "No destination-world victim is touched during the whole old field lifetime");
		onDamage = null;
		probe.finished = true;
	}

	private static void restore(MinecraftServer server, Probe probe) {
		ServerPlayer player = server.getPlayerList().getPlayer(probe.original.getUUID());
		if (!player.isAlive()) player = respawn(server, player);
		prepare(player, probe.level, probe.scenario.art().method());
		if (!probe.scenario.change().survives) {
			check(!probe.binding.valid(), "Restoring a live connected original-world player never restores the old release");
			check(ReleasedArtOwner.capture(player).valid(), "A fresh binding works after the native lifecycle transition");
		}
		discard(probe);
	}

	private static void discard(Probe probe) {
		probe.target.discard();
		probe.destinationTarget.discard();
		if (probe.secondTarget != null) probe.secondTarget.discard();
	}

	private static ServerPlayer respawn(MinecraftServer server, ServerPlayer dead) {
		dead.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
		ServerPlayer replacement = server.getPlayerList().getPlayer(dead.getUUID());
		check(replacement != null && replacement != dead && replacement.isAlive() && dead.isRemoved(),
			"The native respawn request replaces the old body while preserving its UUID");
		check(replacement.connection.player == replacement, "The actual connection now owns the replacement body");
		return replacement;
	}

	private static void prepare(ServerPlayer player, ServerLevel level, String method) {
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
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(method, AuraRules.SOVEREIGN, 4500, 160, 0));
		player.setAttached(SwordStrings.COOLDOWNS, SwordStrings.Cooldowns.NONE);
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		SwordStrings.forget(player.getUUID());
	}

	private static LivingEntity target(ServerLevel level, Vec3 centre) {
		var target = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		check(target != null, "A native ground-field target exists");
		target.setNoAi(true);
		target.setNoGravity(true);
		target.setPersistenceRequired();
		target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
		target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
		target.setHealth(HEALTH);
		target.snapTo(centre.x, centre.y, centre.z, 180, 0);
		level.addFreshEntity(target);
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
		check(target.isAlive() && target.getHealth() == HEALTH && !Reactions.has(target, Reactions.Mark.SHADOWED)
			&& !Reactions.has(target, Reactions.Mark.BLEEDING), reason);
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
