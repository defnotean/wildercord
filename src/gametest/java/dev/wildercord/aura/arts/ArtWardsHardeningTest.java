package dev.wildercord.aura.arts;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

/** Native ward adapter checks on the actual connected body; admission/payment is covered separately. */
public final class ArtWardsHardeningTest implements FabricClientGameTest {
	private static final Identifier MODIFIER = Wildercord.id("art_unmoved");
	private Plan active;
	private static SetterProbe setterProbe;
	private static final class SetterProbe {
		final ServerPlayer player;
		final Runnable head, tail;
		Throwable failure;
		int heads, tails;
		SetterProbe(ServerPlayer player, Runnable head, Runnable tail) { this.player = player; this.head = head; this.tail = tail; }
	}
	/** The test-only native setter observer never changes an invocation, its arguments, return, or exception. */
	public static void observeSetter(ServerPlayer player, boolean head) {
		SetterProbe probe = setterProbe;
		if (probe == null || probe.player != player) return;
		try {
			if (head) { probe.heads++; probe.head.run(); }
			else { probe.tails++; probe.tail.run(); }
		} catch (Throwable failure) { if (probe.failure == null) probe.failure = failure; }
	}
	private record Transition(ServerPlayer player, ServerLevel origin, ServerLevel destination) {}
	private Consumer<Transition> earlyTransition, lateTransition;
	private record Respawn(ServerPlayer oldBody, ServerPlayer newBody) {}
	private Consumer<Respawn> earlyRespawn, lateRespawn;

	private static final class Plan {
		final String name;
		final ServerPlayer player;
		final ServerLevel level;
		final long start;
		final TreeMap<Long, Runnable> checks = new TreeMap<>();
		Throwable failure;
		boolean done;

		Plan(String name, ServerPlayer player) {
			this.name = name; this.player = player; this.level = player.level(); this.start = level.getGameTime();
		}

		void at(int age, Runnable check) {
			if (checks.put(start + age, check) != null) throw new AssertionError("Duplicate native check age " + age);
		}

		void tick() {
			if (done) return;
			try {
				if (!checks.isEmpty() && level.getGameTime() >= checks.firstKey()) {
					var next = checks.pollFirstEntry();
					check(level.getGameTime() == next.getKey(), name + ": the native clock visits the exact assertion tick");
					next.getValue().run();
				}
				done = checks.isEmpty();
			} catch (Throwable thrown) { failure = thrown; done = true; }
		}
	}

	@Override public void runTest(ClientGameTestContext context) {
		Identifier early = Wildercord.id("hardening_test_early"), late = Wildercord.id("hardening_test_late");
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.addPhaseOrdering(early, Event.DEFAULT_PHASE);
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.addPhaseOrdering(Event.DEFAULT_PHASE, late);
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(early, (player, origin, destination) -> {
			if (active != null && active.player == player && earlyTransition != null) earlyTransition.accept(new Transition(player, origin, destination));
		});
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(late, (player, origin, destination) -> {
			if (active != null && active.player == player && lateTransition != null) lateTransition.accept(new Transition(player, origin, destination));
		});
		ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(early, Event.DEFAULT_PHASE);
		ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, late);
		ServerPlayerEvents.AFTER_RESPAWN.register(early, (oldPlayer, newPlayer, alive) -> {
			if (active != null && active.player == oldPlayer && earlyRespawn != null) earlyRespawn.accept(new Respawn(oldPlayer, newPlayer));
		});
		ServerPlayerEvents.AFTER_RESPAWN.register(late, (oldPlayer, newPlayer, alive) -> {
			if (active != null && active.player == oldPlayer && lateRespawn != null) lateRespawn.accept(new Respawn(oldPlayer, newPlayer));
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Plan plan = active;
			if (plan != null && plan.level.getServer() == server) plan.tick();
		});
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(30);
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			try {
				setterCaptureBarrier(world);
				scenario(world, "same_level_setter_keeps_generation", plan -> {
					ReleasedArtOwner original = ReleasedArtOwner.capture(plan.player); ArtWards.Hardened originalWard = grant(plan.player);
					plan.at(1, () -> { plan.player.setServerLevel(plan.level); check(original.valid() && originalWard.active(), "Same-level native setter preserves original generation and deadline"); ward(plan.player, true); });
				});
				destinationGrant(world);
				nestedRoundTrip(world, false);
				nestedRoundTrip(world, true);
				scenario(world, "disconnect_event_retires_original_generation", plan -> {
					ReleasedArtOwner owner = ReleasedArtOwner.capture(plan.player);
					ArtWards.Hardened ward = grant(plan.player);
					plan.at(1, () -> {
						// Explicit Fabric callback-dispatch fixture, not a claim that the client transport disconnected.
						ServerPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(plan.player.connection, plan.level.getServer());
						check(!owner.valid() && !ward.active() && !ward.grant(), "Disconnect event permanently retires original released effects and hardening");
						ward(plan.player, false);
					});
				});
				scenario(world, "mountain_then_unmoved", plan -> {
					ServerPlayer player = plan.player;
					ArtWards.harden(player, 25); ward(player, true);
					ArtWards.Hardened[] unmoved = new ArtWards.Hardened[1];
					plan.at(10, () -> { unmoved[0] = grant(player); ward(player, true); });
					plan.at(25, () -> { check(unmoved[0].active(), "Both sources overlap at Mountain's inclusive endpoint"); ward(player, true); });
					plan.at(26, () -> { check(unmoved[0].active(), "Mountain expiry cannot retire Unmoved"); ward(player, true); });
					plan.at(90, () -> { check(unmoved[0].active(), "Unmoved retains acceptedAt + 80 inclusive"); ward(player, true); });
					plan.at(91, () -> { ward(player, false); check(!unmoved[0].active(), "Unmoved expires on its own original clock"); });
				});
				scenario(world, "unmoved_then_mountain", plan -> {
					ServerPlayer player = plan.player;
					ArtWards.Hardened unmoved = grant(player);
					plan.at(10, () -> { ArtWards.harden(player, 25); ward(player, true); });
					plan.at(36, () -> { check(unmoved.active(), "A later shorter Mountain cannot shorten Unmoved"); ward(player, true); });
					plan.at(70, () -> { ArtWards.harden(player, 25); ward(player, true); });
					plan.at(80, () -> { check(unmoved.active(), "Mountain refresh does not alter Unmoved's inclusive endpoint"); ward(player, true); });
					plan.at(81, () -> { check(!unmoved.active(), "Unmoved expiry cannot remove refreshed Mountain"); ward(player, true); });
					plan.at(95, () -> ward(player, true));
					plan.at(96, () -> ward(player, false));
				});
				scenario(world, "retire_unmoved_preserves_mountain_and_resistance", plan -> {
					ServerPlayer player = plan.player;
					ArtWards.Hardened unmoved = grant(player);
					ArtWards.harden(player, 25);
					plan.at(10, () -> {
						MobEffectInstance resistance = player.getEffect(MobEffects.RESISTANCE);
						check(resistance != null, "A real native Resistance instance exists before retirement");
						int duration = resistance.getDuration();
						unmoved.retire();
						check(!unmoved.active(), "The exact Unmoved token retires"); ward(player, true);
						check(player.getEffect(MobEffects.RESISTANCE) == resistance && resistance.getDuration() == duration,
							"Lease retirement never removes, replaces or refreshes native Resistance");
					});
					plan.at(25, () -> ward(player, true));
					plan.at(26, () -> {
						ward(player, false);
						check(player.hasEffect(MobEffects.RESISTANCE), "Vanilla Resistance may naturally outlive both knockback leases");
					});
				});
				scenario(world, "replay_never_refreshes", plan -> {
					ServerPlayer player = plan.player;
					ArtWards.Hardened unmoved = grant(player);
					plan.at(10, () -> {
						MobEffectInstance resistance = player.getEffect(MobEffects.RESISTANCE);
						check(resistance != null && resistance.getDuration() < 80, "Native Resistance genuinely counted down");
						int duration = resistance.getDuration();
						check(!unmoved.grant(), "The same receipt cannot be granted twice");
						check(player.getEffect(MobEffects.RESISTANCE) == resistance && resistance.getDuration() == duration,
							"Rejected replay cannot replace or refresh the native effect"); ward(player, true);
					});
					plan.at(81, () -> {
						ward(player, false);
						check(!unmoved.grant() && !player.hasEffect(MobEffects.RESISTANCE), "Expired replay never rearms either effect");
					});
				});
				scenario(world, "stronger_longer_resistance_survives", plan -> {
					ServerPlayer player = plan.player;
					player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 240, 2));
					MobEffectInstance stronger = player.getEffect(MobEffects.RESISTANCE);
					ArtWards.Hardened unmoved = grant(player);
					ArtWards.harden(player, 25);
					check(player.getEffect(MobEffects.RESISTANCE) == stronger && stronger.getAmplifier() == 2 && stronger.getDuration() == 240,
						"Both native addEffect calls preserve an existing stronger, longer effect");
					plan.at(10, () -> {
						int duration = stronger.getDuration(); unmoved.retire();
						check(player.getEffect(MobEffects.RESISTANCE) == stronger && stronger.getAmplifier() == 2 && stronger.getDuration() == duration,
							"Retiring Unmoved never strips an unrelated stronger Resistance"); ward(player, true);
					});
					plan.at(26, () -> { ward(player, false); check(player.getEffect(MobEffects.RESISTANCE) == stronger, "Mountain expiry also leaves stronger Resistance intact"); });
				});
				scenario(world, "original_world_round_trip", plan -> {
					ServerPlayer player = plan.player;
					ArtWards.Hardened unmoved = grant(player);
					ArtWards.harden(player, 25);
					plan.at(10, () -> {
						MobEffectInstance resistance = player.getEffect(MobEffects.RESISTANCE);
						int duration = resistance.getDuration();
						check(player.teleportTo(plan.level.getServer().getLevel(Level.NETHER), .5, 120, .5, Set.of(), 0, 0, false), "The actual original body leaves its world");
						check(player.teleportTo(plan.level, .5, 120, .5, Set.of(), 0, 0, false), "The actual body returns in the same callback without polling the old lease");
						ward(player, false);
						check(!unmoved.active() && !unmoved.grant(), "A round trip permanently retires the old Unmoved receipt");
						check(player.getEffect(MobEffects.RESISTANCE) == resistance && resistance.getDuration() == duration,
							"World retirement never blanket-removes native Resistance");
						ArtWards.harden(player, 25); ward(player, true);
						unmoved.retire(); ward(player, true);
					});
					plan.at(36, () -> ward(player, false));
				});
				scenario(world, "real_death_and_same_uuid_respawn", plan -> {
					ServerPlayer oldPlayer = plan.player;
					ArtWards.Hardened oldUnmoved = grant(oldPlayer);
					ReleasedArtOwner oldRelease = ReleasedArtOwner.capture(oldPlayer);
					ReleasedArtOwner[] earlyRelease = {null}; ArtWards.Hardened[] earlyWard = {null}; int[] callbacks = {0};
					earlyRespawn = event -> {
						check(event.oldBody() != event.newBody() && event.oldBody().getId() == event.newBody().getId()
							&& event.oldBody().equals(event.newBody()) && event.oldBody().hashCode() == event.newBody().hashCode(),
							"Native respawn uses equal entity IDs and equality/hash despite distinct actual bodies");
						earlyRelease[0] = ReleasedArtOwner.capture(event.newBody()); earlyWard[0] = grant(event.newBody()); callbacks[0]++;
					};
					lateRespawn = event -> {
						check(earlyRelease[0].valid() && earlyWard[0].active(), "Default old-body respawn retirement cannot revoke an early replacement grant"); callbacks[0]++;
					};
					ArtWards.harden(oldPlayer, 25);
					plan.at(10, () -> {
						oldPlayer.kill(plan.level);
						check(!oldPlayer.isAlive(), "The actual connected body died");
						ward(oldPlayer, false);
						oldPlayer.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
						ServerPlayer replacement = plan.level.getServer().getPlayerList().getPlayer(oldPlayer.getUUID());
						check(replacement != null && replacement != oldPlayer && replacement.isAlive() && oldPlayer.isRemoved()
							&& replacement.connection.player == replacement, "A legitimate native respawn installs a different current body with the same UUID");
						check(callbacks[0] == 2 && earlyRelease[0].valid() && earlyWard[0].active(), "Both native early/default/late respawn phases completed with independent generations");
						ward(replacement, true);
						earlyWard[0].retire(); ward(replacement, false);
						earlyRespawn = null; lateRespawn = null;
						check(!oldRelease.valid() && !oldUnmoved.active() && !oldUnmoved.grant(), "Old releases and paid receipts cannot bind to the replacement body");
						ArtWards.Hardened fresh = grant(replacement);
						oldUnmoved.retire();
						// A late legacy UUID-only cleanup is a direct cleanup probe, not an extra native disconnect.
						ArtWards.forget(oldPlayer.getUUID());
						check(fresh.active(), "Old-body and UUID-only cleanup cannot remove a replacement body's fresh grant"); ward(replacement, true);
						fresh.retire(); ward(replacement, false);
					});
				});
			} finally {
				world.getServer().runOnServer(server -> { active = null; ArtWards.clear(); });
			}
		} finally { active = null; earlyTransition = null; lateTransition = null; earlyRespawn = null; lateRespawn = null; setterProbe = null; }
	}

	private void setterCaptureBarrier(TestSingleplayerContext world) {
		scenario(world, "capture_inside_native_setter_scope", plan -> {
			ReleasedArtOwner old = ReleasedArtOwner.capture(plan.player); ArtWards.Hardened oldWard = grant(plan.player);
			ReleasedArtOwner[] outgoing = {null}, incoming = {null}; ArtWards.Hardened[] freshWard = {null};
			ServerLevel next = plan.level.getServer().getLevel(Level.NETHER);
			plan.at(1, () -> {
				SetterProbe probe = new SetterProbe(plan.player, () -> {
					check(plan.player.level() == plan.level, "HEAD observer runs before the native level assignment");
					outgoing[0] = ReleasedArtOwner.capture(plan.player);
					check(!outgoing[0].valid(), "A fresh outgoing-world capture is permanently retired inside the transition scope");
					check(!ArtWards.unmoved(plan.player, outgoing[0], plan.level.getGameTime(), 80).grant(), "An outgoing callback cannot grant defense after the departure barrier");
				}, () -> {
					check(plan.player.level() == next, "TAIL observer sees the actual destination assignment");
					incoming[0] = ReleasedArtOwner.capture(plan.player); freshWard[0] = grant(plan.player);
					check(incoming[0].valid() && freshWard[0].active(), "A destination receipt inside the setter remains eligible");
				});
				setterProbe = probe;
				try { check(plan.player.teleportTo(next, .5, 120, .5, Set.of(), 0, 0, false), "The native transition completes normally through both passive observers"); }
				finally { setterProbe = null; }
				if (probe.failure != null) throw new AssertionError("Native setter capture probe", probe.failure);
				check(probe.heads == 1 && probe.tails == 1, "Exactly one original setter invocation was observed");
				check(!old.valid() && !oldWard.active() && !outgoing[0].valid() && incoming[0].valid() && freshWard[0].active(),
					"Completion retains only the actual incoming generation");
				check(plan.player.teleportTo(plan.level, .5, 120, .5, Set.of(), 0, 0, false), "The actual body returns to its original world");
				check(!outgoing[0].valid() && !incoming[0].valid() && !freshWard[0].active(), "Neither old outgoing nor intermediate incoming receipts revive on return");
			});
		});
	}

	private void destinationGrant(TestSingleplayerContext world) {
		scenario(world, "early_destination_grant_survives_late_origin_event", plan -> {
			ReleasedArtOwner original = ReleasedArtOwner.capture(plan.player);
			ArtWards.Hardened oldWard = grant(plan.player);
			ReleasedArtOwner[] destination = {null}; ArtWards.Hardened[] freshWard = {null};
			ServerLevel next = plan.level.getServer().getLevel(Level.NETHER);
			earlyTransition = transition -> {
				check(transition.origin() == plan.level && transition.destination() == next, "Native destination callback carries original transition worlds");
				destination[0] = ReleasedArtOwner.capture(plan.player); freshWard[0] = grant(plan.player);
				check(!original.valid() && !oldWard.active(), "Actual departure retires old releases before the first AFTER listener");
			};
			lateTransition = transition -> check(destination[0].valid() && freshWard[0].active(), "Default and late origin listeners cannot retire a fresh destination generation");
			plan.at(1, () -> {
				check(plan.player.teleportTo(next, .5, 120, .5, Set.of(), 0, 0, false), "Real connected body crosses worlds");
				check(destination[0] != null && destination[0].valid() && freshWard[0].active(), "New generation remains valid after all AFTER listeners complete");
				earlyTransition = null; lateTransition = null;
				check(plan.player.teleportTo(plan.level, .5, 120, .5, Set.of(), 0, 0, false), "Body returns after destination-grant check");
				check(!destination[0].valid() && !freshWard[0].active(), "The next real departure retires the destination generation");
			});
		});
	}

	private void nestedRoundTrip(TestSingleplayerContext world, boolean captureIntermediate) {
		scenario(world, "nested_round_trip_intermediate_capture_" + captureIntermediate, plan -> {
			ReleasedArtOwner oldA = ReleasedArtOwner.capture(plan.player);
			ArtWards.Hardened oldWard = grant(plan.player);
			ArtWards.Mirror oldMirror = ArtWards.mirror(plan.player, oldA, 80);
			ReleasedArtOwner[] middleB = {null}, freshA = {null}, followB = {null}; ArtWards.Hardened[] freshWard = {null};
			ArtWards.Mirror[] freshMirror = {null}; int[] early = {0}, late = {0};
			ServerLevel next = plan.level.getServer().getLevel(Level.NETHER);
			earlyTransition = transition -> {
				early[0]++;
				if (early[0] == 1) {
					check(transition.origin() == plan.level && transition.destination() == next, "Outer native transition is A to B");
					if (captureIntermediate) middleB[0] = ReleasedArtOwner.capture(plan.player);
					// The false case intentionally never captures or polls ANY receipt in B before returning to A.
					check(plan.player.teleportTo(plan.level, .5, 120, .5, Set.of(), 0, 0, false), "Early outer listener performs actual nested B to A transition");
				} else if (early[0] == 2) {
					check(transition.origin() == next && transition.destination() == plan.level, "Inner native transition returns B to A");
					freshA[0] = ReleasedArtOwner.capture(plan.player); freshWard[0] = grant(plan.player);
					freshMirror[0] = ArtWards.mirror(plan.player, freshA[0], 80);
				} else {
					// Vanilla then moves the outer teleport's spectators: it walks A's players for any whose camera is the
					// mover, and the returned body is back in A watching itself, so it is carried to B a second time.
					check(early[0] == 3 && transition.origin() == plan.level && transition.destination() == next, "Vanilla's spectator follow is one more real A to B transition");
					followB[0] = ReleasedArtOwner.capture(plan.player);
				}
			};
			lateTransition = transition -> {
				late[0]++;
				check(!oldA.valid() && !oldWard.active() && !oldMirror.active(), "Old A releases never revive after an unobserved B round trip");
				if (late[0] <= 2) {
					check(freshA[0] != null && freshA[0].valid() && freshWard[0].active() && freshMirror[0].active(),
						"A fresh inner-A generation survives both default and stale outer-A AFTER listeners");
				} else {
					check(!freshA[0].valid() && !freshWard[0].active() && !freshMirror[0].active() && followB[0].valid(),
						"The follow's real departure retires the inner-A generation and keeps its own");
				}
			};
			plan.at(1, () -> {
				check(plan.player.teleportTo(next, .5, 120, .5, Set.of(), 0, 0, false), "Native outer teleport dispatches the adversarial listener order");
				check(plan.player.level() == next && early[0] == 3 && late[0] == 3, "Nested, stale outer and follow callbacks all actually ran");
				check(!oldA.valid() && !freshA[0].valid() && followB[0].valid(), "Actual assignment generation, not world equality, owns the surviving release");
				if (captureIntermediate) check(!middleB[0].valid(), "The captured intermediate B release is retired too");
				earlyTransition = null; lateTransition = null;
			});
			plan.at(2, () -> {
				check(!oldA.valid() && !freshA[0].valid() && !freshMirror[0].active() && followB[0].valid(), "Already-released Mirror and delayed-owner validity keep the correct generation on the next native tick");
				check(plan.player.teleportTo(plan.level, .5, 120, .5, Set.of(), 0, 0, false), "Body returns to A");
				check(!followB[0].valid(), "The return retires the follow's generation");
			});
		});
	}

	private void scenario(TestSingleplayerContext world, String name, Consumer<Plan> body) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			check(player.connection != null && player.connection.player == player
				&& server.getPlayerList().getPlayer(player.getUUID()) == player, "The ward fixture uses the actual connected original body");
			earlyTransition = null; lateTransition = null; earlyRespawn = null; lateRespawn = null; setterProbe = null; ArtWards.clear();
			player.setGameMode(GameType.SURVIVAL); player.removeAllEffects(); player.getInventory().clearContent();
			player.setHealth(player.getMaxHealth()); player.setNoGravity(true); player.setDeltaMovement(Vec3.ZERO);
			check(player.teleportTo(server.overworld(), .5, 120, .5, Set.of(), 0, 0, false), "The connected fixture is in the original world");
			player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
			ward(player, false);
			active = new Plan(name, player); body.accept(active);
		});
		world.getServer().waitFor(server -> active.done, 150);
		world.getServer().runOnServer(server -> {
			if (active.failure != null) throw new AssertionError("Native hardening scenario failed: " + name, active.failure);
			check(active.checks.isEmpty(), "All native hardening checks ran: " + name);
			System.out.println("ART_WARDS_HARDENING scenario=" + name + " connectedBody=true nativeTicks=true pass=true");
			active = null;
		});
	}

	private static ArtWards.Hardened grant(ServerPlayer player) {
		ArtWards.Hardened receipt = ArtWards.unmoved(player, ReleasedArtOwner.capture(player), player.level().getGameTime(), 80);
		check(receipt.grant(), "A fresh receipt grants exactly once on its original body");
		return receipt;
	}

	private static void ward(ServerPlayer player, boolean expected) {
		var resistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		check(resistance != null, "The connected player has a native knockback attribute");
		var modifier = resistance.getModifier(MODIFIER);
		// Inspect before hardened() can reconcile anything: ordinary tick/lifecycle wiring must already have updated the modifier.
		check((modifier != null) == expected, "The native modifier agrees with the independent live leases");
		check(ArtWards.hardened(player) == expected, "The ward query agrees with the native modifier");
		if (expected) {
			check(modifier.amount() == 1 && modifier.operation() == AttributeModifier.Operation.ADD_VALUE,
				"Hardening has exactly one +1 ADD_VALUE modifier");
			check(resistance.getModifiers().size() == 1 && Math.abs(resistance.getValue() - 1) < 1.0E-9,
				"Overlapping sources retain one effective native +1 knockback modifier");
		} else check(Math.abs(resistance.getValue()) < 1.0E-9, "No retired hardening remains on this exact body");
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
