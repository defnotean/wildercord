package dev.wildercord.aura;

import com.mojang.authlib.GameProfile;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.CrimsonArts;
import dev.wildercord.aura.arts.MethodArts;
import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.aura.world.HailSkyTrialSupport;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Statuses;
import dev.wildercord.duel.DuelRules;
import dev.wildercord.duel.Duels;
import dev.wildercord.party.HailSkyPartySupport;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Registered Crimson Moon on an actual connected owner and a dedicated server which keeps ticking after disconnect.
 * This suite deliberately also runs before Moon acquires a Masters style profile: it uses the registered performer
 * through SwordStrings.perform, not an invented profile, a replacement performer, or a lifecycle event invoker.
 * Authoritative Final-string input and presentation belong to the separate timeline acceptance suite.
 *
 * <p>An already-admitted ArtKit.Hits strike may complete its common passive/stance/mark work. Moon must suppress its
 * own subsequent drink, new wound, later victims and later wound ticks. The mend bucket separates Moon's drink from
 * the common Crimson passive; the short common mark is allowed to finish and expire naturally.
 */
public final class CrimsonMoonReleasedOwnerTest implements FabricClientGameTest {
	private enum Change {
		CONTROL, WEAPON, METHOD, INTERRUPTION,
		DEATH, RESPAWN, DIMENSION, ROUND_TRIP, DISCONNECT,
		TARGET_DEATH, TARGET_REMOVAL, TARGET_DIMENSION, TEAM, PARTY, DUEL, TRIAL,
		LETHAL, NESTED_SOURCE;

		boolean retiresOwner() {
			return this == DEATH || this == RESPAWN || this == DIMENSION || this == ROUND_TRIP || this == DISCONNECT;
		}
		boolean blocksTarget() {
			return this == TARGET_DEATH || this == TARGET_REMOVAL || this == TARGET_DIMENSION
				|| this == TEAM || this == PARTY || this == TRIAL;
		}
	}
	private enum Boundary { BEFORE_DIRECT, BEFORE_WOUND, DIRECT_DAMAGE, WOUND_DAMAGE }
	private record Case(Change change, Boundary boundary) {
		String label() { return change + "/" + boundary; }
		boolean callback() { return boundary == Boundary.DIRECT_DAMAGE || boundary == Boundary.WOUND_DAMAGE; }
	}
	private record Hit(LivingEntity target, DamageSource source) {}
	private static Consumer<Hit> onDamage;
	private static boolean listening;
	private static final float HEALTH = 200;
	private static final Vec3 FEET = new Vec3(.5, 100, .5);
	private static final Vec3 VICTIM = FEET.add(0, 0, 5.1);
	private static final int DIRECT = 5;
	private static final int FIRST_WOUND = DIRECT + ArtRules.BLEED_PERIOD;
	private static final int FINISH = DIRECT + ArtRules.MOON_BLEEDS * ArtRules.BLEED_PERIOD + 14;
	private static final List<Integer> MARKS = List.of(SwordString.Token.FULL.bit(), SwordString.Token.FULL.bit(),
		SwordString.Token.FULL.bit(), SwordString.Token.LOW.bit());
	private Probe current;

	private static final class Guest extends FakePlayer {
		Guest(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "MoonGuest")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private static final class Probe {
		final Case scenario;
		final ServerPlayer owner;
		final ServerLevel level;
		final List<LivingEntity> fixtures = new ArrayList<>();
		LivingEntity target, second, destination, movedTarget, carrier;
		Guest guest;
		SwordMaster trial;
		Cast outerCast;
		ReleasedArtOwner binding;
		BooleanSupplier physical;
		MastersStyleRules.Style profile;
		long accepted, released, mutated;
		int releases, hits, secondHits;
		float initialHealth, mutationHealth, finalHealth;
		double mutationMendRoom;
		boolean nestedEntered, mutatedOnce, finished;
		Throwable failure;
		Probe(Case scenario, ServerPlayer owner) {
			this.scenario = scenario; this.owner = owner; this.level = owner.level();
		}
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!listening) {
			listening = true;
			ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, damage, blocked) -> {
				Consumer<Hit> action = onDamage;
				if (action != null && damage > 0) action.accept(new Hit(target, source));
			});
			// Fabric's AFTER_DAMAGE is nonlethal; native lethal hits are observed through AFTER_DEATH instead.
			ServerLivingEntityEvents.AFTER_DEATH.register((target, source) -> {
				Consumer<Hit> action = onDamage;
				if (action != null) action.accept(new Hit(target, source));
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
		long[] nextInterruption = {-1};
		try (var server = context.worldBuilder().createServer(properties)) {
			TestDedicatedServerConnection connection = server.connect();
			try {
				connection.waitForChunksDownload();
				connection.waitForClientboundPackets();
				server.runCommand("gamerule minecraft:spawn_mobs false");
				server.runCommand("gamerule minecraft:natural_health_regeneration false");
				server.runOnServer(s -> {
					arena(s.overworld()); arena(s.getLevel(Level.NETHER)); AuraApi.onString(hook);
					s.overworld().getGameRules().set(GameRules.PVP, true, s);
				});
				for (Case scenario : cases()) {
					if (scenario.change() == Change.INTERRUPTION) {
						// Reusing the real connected owner must respect the production per-UUID immunity.
						// Wait on actual server time; never clear or edit Statuses history between cases.
						server.waitFor(s -> s.overworld().getGameTime() >= nextInterruption[0], Statuses.INTERRUPT_GAP + 100);
					}
					server.runOnServer(s -> begin(s, scenario));
					server.waitFor(s -> current.finished, 250);
					server.runOnServer(s -> {
						Probe p = current;
						if (p.failure != null) throw new AssertionError(scenario.label() + " failed", p.failure);
						check(p.releases == 1 && p.mutatedOnce, "Exactly one actual release and intended transition: " + scenario.label());
						if (scenario.change() == Change.INTERRUPTION) {
							check(p.mutated >= nextInterruption[0], "Repeated native interruption waits for the full real immunity expiry");
							nextInterruption[0] = p.mutated + Statuses.INTERRUPT_GAP + 1;
						}
						check(Effects.applying() == null && Effects.applyingCast() == null, "Moon restores the ambient source and cast after all scheduled work");
						Wildercord.LOGGER.info("CRIMSON_MOON_OWNER case={} accepted={} release={} mutation={} hits={} secondHits={} health={} ownerValid={} profiled={}",
							scenario.label(), p.accepted, p.released, p.mutated, p.hits, p.secondHits, p.finalHealth, p.binding.valid(), p.profile != null);
						if (scenario.change() != Change.DISCONNECT) restore(s, p);
					});
					if (scenario.change() == Change.DISCONNECT) {
						context.waitFor(mc -> mc.level == null, 200);
						context.setScreen(TitleScreen::new);
						connection = server.connect();
						connection.waitForChunksDownload();
						connection.waitForClientboundPackets();
						server.runOnServer(s -> {
							Probe p = current;
							ServerPlayer joined = s.getPlayerList().getPlayer(p.owner.getUUID());
							check(joined != null && joined != p.owner && joined.isAlive(), "A real reconnect creates a live replacement body with the same UUID");
							check(!p.binding.valid() && ReleasedArtOwner.capture(joined).valid(), "Reconnect cannot revive Moon's old original-body release");
							check(p.target.getHealth() == p.finalHealth, "Reconnect does not resume a retired wound");
							cleanup(p);
						});
					}
					context.runOnClient(mc -> mc.gui.setScreen(null));
					context.waitTicks(5);
				}
			} finally {
				onDamage = null;
				server.runOnServer(s -> { AuraApi.stringHooks().remove(hook); if (current != null) cleanup(current); });
				if (context.computeOnClient(mc -> mc.level != null)) connection.close();
				else context.setScreen(TitleScreen::new);
			}
		}
	}

	private static List<Case> cases() {
		List<Case> cases = new ArrayList<>();
		for (Change change : List.of(Change.CONTROL, Change.WEAPON, Change.METHOD, Change.INTERRUPTION,
			Change.DEATH, Change.RESPAWN, Change.DIMENSION, Change.ROUND_TRIP, Change.DISCONNECT,
			Change.TARGET_DEATH, Change.TARGET_REMOVAL, Change.TARGET_DIMENSION, Change.TEAM, Change.PARTY, Change.TRIAL, Change.LETHAL)) {
			cases.add(new Case(change, Boundary.BEFORE_DIRECT));
			cases.add(new Case(change, Boundary.BEFORE_WOUND));
		}
		for (Change change : List.of(Change.DEATH, Change.ROUND_TRIP, Change.TARGET_REMOVAL, Change.TARGET_DIMENSION, Change.TEAM)) {
			cases.add(new Case(change, Boundary.DIRECT_DAMAGE));
			cases.add(new Case(change, Boundary.WOUND_DAMAGE));
		}
		cases.add(new Case(Change.DUEL, Boundary.BEFORE_DIRECT));
		cases.add(new Case(Change.NESTED_SOURCE, Boundary.BEFORE_DIRECT));
		return cases;
	}

	private void begin(MinecraftServer server, Case scenario) {
		ServerPlayer owner = server.getPlayerList().getPlayers().getFirst();
		prepare(owner, server.overworld());
		Probe p = new Probe(scenario, owner); current = p;
		check(owner.connection.getRemoteAddress() instanceof InetSocketAddress address && address.getAddress().isLoopbackAddress(),
			"Moon's owner is a real loopback TCP player");
		if (scenario.change() == Change.PARTY || scenario.change() == Change.DUEL) {
			p.guest = guest(p, VICTIM); p.target = p.guest;
		} else if (scenario.change() == Change.TRIAL) {
			p.guest = guest(p, FEET.add(7, 0, 0));
			p.trial = HailSkyTrialSupport.start(owner, p.guest, VICTIM.x, VICTIM.z);
			p.fixtures.add(p.trial); p.target = p.trial;
		} else p.target = target(p, p.level, VICTIM);
		p.initialHealth = p.target.getHealth();
		if (scenario.callback() && scenario.change().retiresOwner()) p.second = target(p, p.level, VICTIM.add(.6, 0, 0));
		p.destination = target(p, server.getLevel(Level.NETHER), VICTIM);
		check(ArtKit.harmable(owner, p.target), "Every selected victim is initially live and legally harmable");
		onDamage = hit -> checked(p, () -> observedDamage(p, hit));
		// Admission uses the scheduler's clock so all relative boundaries are exact server ticks.
		Scheduler.later(2, () -> checked(p, () -> {
			if (scenario.change() == Change.NESTED_SOURCE) {
				p.guest = guest(p, FEET.add(-6, 0, -6));
				p.carrier = target(p, p.level, FEET.add(-6, 0, -4));
				p.outerCast = new Cast(p.guest);
				var harm = SpellCompiler.compile(List.of(Runes.TOUCH, Runes.HARM)).root().groups.getFirst().effects.getFirst();
				Effects.apply(p.outerCast, harm, new Cast.Hit(List.of(p.carrier), p.carrier.position(), new Vec3(0, 0, 1), p.guest.position(), null, null, false));
				check(p.nestedEntered, "An actual unrelated spell damage callback admits the original Moon");
				check(Effects.applying() == null && Effects.applyingCast() == null, "The surrounding actual spell restores its caller");
			} else perform(p);
		}));
	}

	private static void perform(Probe p) {
		AuraApi.StringArt art = AuraApi.artOf(p.owner, CrimsonArts.CRIMSON_MOON).orElseThrow();
		check(art.string().fits(MARKS.stream().mapToInt(Integer::intValue).toArray()), "The real registered Final art retains its full/full/full/low string");
		p.accepted = p.level.getGameTime();
		float aura = Aura.aura(p.owner);
		check(SwordStrings.perform(p.owner, art, MARKS), "The actual registered Moon is admitted through SwordStrings.perform");
		check(Aura.aura(p.owner) < aura && SwordStrings.readyAt(p.owner, art.id()) > p.accepted, "The admitted Moon pays and rests normally");
	}

	private void released(ServerPlayer owner, AuraApi.StringArt art, AuraApi.StringContext move) {
		Probe p = current;
		if (p == null || owner != p.owner || !CrimsonArts.CRIMSON_MOON.equals(art.id())) return;
		checked(p, () -> {
			p.releases++; p.released = p.level.getGameTime();
			p.profile = MastersStyleRules.of(art.id());
			check(p.released - p.accepted == (p.profile == null ? 0 : p.profile.windup()), "The real current profile determines release, including pre-timeline instant Moon");
			check(move.marks().equals(MARKS) && move.at() == p.accepted, "Completion preserves the admitted art's marks and time");
			p.binding = ReleasedArtOwner.capture(owner); p.physical = MastersArts.continuation(owner);
			check(p.binding.valid() && p.binding.level() == p.level, "The release binds the original live connected body and world");
			check(p.hits == 0 && p.target.getHealth() == p.initialHealth, "No selected-target hit precedes Moon's scheduled direct beat");
			check(1 + Math.min(4, (int) (p.target.distanceTo(owner) / 1.2)) == DIRECT, "The native fixture's authored direct delay is exactly five ticks");
			if (p.scenario.change() == Change.LETHAL && p.scenario.boundary() == Boundary.BEFORE_WOUND) {
				// The opening drink must have taken zero, leaving this same performance's drink cap for its lethal wound.
				owner.setHealth(owner.getMaxHealth());
			}
			if (!p.scenario.callback()) {
				int at = p.scenario.boundary() == Boundary.BEFORE_DIRECT ? 1 : DIRECT + 1;
				Scheduler.later(at, () -> checked(p, () -> mutate(p)));
			}
			Scheduler.later(DIRECT, () -> checked(p, () -> afterDirect(p)));
			// A common admitted strike's mark lasts 60 ticks; an incorrectly created Moon wound lasts 70.
			Scheduler.later(DIRECT + AuraRules.MARK_TICKS + 1, () -> checked(p, () -> {
				if (p.scenario.boundary() == Boundary.DIRECT_DAMAGE && (p.scenario.change().retiresOwner() || p.scenario.change() == Change.TEAM)) {
					check(!Reactions.has(p.target, Reactions.Mark.BLEEDING), "Only a common admitted-hit mark may finish; no newly forbidden Moon wound mark survives it");
				}
			}));
			Scheduler.later(FINISH, () -> checked(p, () -> finish(p)));
		});
	}

	private static void observedDamage(Probe p, Hit hit) {
		if (hit.target() == p.carrier && hit.source().getEntity() == p.guest) {
			check(!p.nestedEntered && Effects.applying() == p.guest && Effects.applyingCast() == p.outerCast, "A distinct real spell owns its native damage callback");
			p.nestedEntered = true; perform(p);
			check(Effects.applying() == p.guest && Effects.applyingCast() == p.outerCast, "Moon admission restores the exact enclosing spell actor and cast");
			return;
		}
		if (hit.target() != p.target && hit.target() != p.second) return;
		if (hit.source().getEntity() != p.owner || !hit.source().is(Aura.DAMAGE)) return;
		check(Effects.applying() == p.owner && Effects.applyingCast() == null, "Each actual direct hit and inherited wound retains Moon's actor and borrows no spell cast");
		if (hit.target() == p.second) { p.secondHits++; return; }
		p.hits++;
		long expected = p.released + DIRECT + (p.hits - 1L) * ArtRules.BLEED_PERIOD;
		check(p.level.getGameTime() == expected, "Direct and wound hits occur on Moon's native authored schedule");
		boolean boundary = p.scenario.boundary() == Boundary.DIRECT_DAMAGE && p.hits == 1
			|| p.scenario.boundary() == Boundary.WOUND_DAMAGE && p.hits == 2;
		if (boundary) mutate(p);
	}

	private static void mutate(Probe p) {
		check(!p.mutatedOnce, "The intended lifecycle or admission change happens once");
		p.mutatedOnce = true; p.mutated = p.level.getGameTime();
		int at = switch (p.scenario.boundary()) {
			case BEFORE_DIRECT -> 1; case BEFORE_WOUND -> DIRECT + 1;
			case DIRECT_DAMAGE -> DIRECT; case WOUND_DAMAGE -> FIRST_WOUND;
		};
		check(p.mutated == p.released + at, "The native change occurs exactly at the selected delayed-work boundary");
		ServerPlayer owner = p.owner; MinecraftServer server = p.level.getServer();
		switch (p.scenario.change()) {
			case CONTROL, NESTED_SOURCE -> { }
			case WEAPON -> {
				owner.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				if (p.profile != null) check(!p.physical.getAsBoolean(), "The actual weapon change cancels physical continuation");
			}
			case METHOD -> {
				owner.setAttached(AuraAttachments.AURA, Aura.data(owner).withMethod("stone"));
				if (p.profile != null) check(!p.physical.getAsBoolean(), "The actual method change cancels physical continuation");
			}
			case INTERRUPTION -> {
				owner.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
				owner.startUsingItem(InteractionHand.OFF_HAND);
				check(owner.isUsingItem() && Statuses.interrupt(owner) && !owner.isUsingItem(), "A real shared interruption cancels active native item use");
				if (p.profile != null) check(!p.physical.getAsBoolean(), "A successful shared interruption cancels the actual physical continuation");
			}
			case DEATH -> { owner.kill(p.level); check(!owner.isAlive(), "The original owner actually dies"); }
			case RESPAWN -> {
				owner.kill(p.level); ServerPlayer replacement = respawn(server, owner); prepare(replacement, p.level);
				check(ReleasedArtOwner.capture(replacement).valid(), "The real live replacement body can own a fresh release");
			}
			case DIMENSION, ROUND_TRIP -> {
				check(owner.teleportTo(server.getLevel(Level.NETHER), FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "Native owner teleport crosses dimensions");
				check(owner.level() != p.level, "The exact original body departed its release world");
				if (p.scenario.change() == Change.ROUND_TRIP) {
					// Do not inspect the old binding in the foreign world. Only the real event can remember departure.
					check(owner.teleportTo(p.level, FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "The same owner returns synchronously in the same call");
					check(owner.isAlive() && server.getPlayerList().getPlayer(owner.getUUID()) == owner, "The exact original connected body is already back before Moon resumes");
					check(ReleasedArtOwner.capture(owner).valid(), "A same-call return starts a separate usable fresh lifetime");
				}
			}
			case DISCONNECT -> {
				Connection socket = server.getConnection().getConnections().stream().filter(c -> c.getPacketListener() == owner.connection).findFirst().orElseThrow();
				check(socket.isConnected() && !socket.isMemoryConnection(), "Disconnect closes the actual owner's TCP connection");
				socket.disconnect(Component.literal("Crimson Moon released-owner lifecycle test"));
				check(!socket.isConnected(), "The real network channel is closed");
				socket.handleDisconnection();
				check(server.getPlayerList().getPlayer(owner.getUUID()) == null && owner.isRemoved(), "Native closed-connection cleanup removes the original owner body");
			}
			case TARGET_DEATH -> { p.target.kill(p.level); check(!p.target.isAlive(), "The native victim actually dies before its next delayed hit"); }
			case TARGET_REMOVAL -> { p.target.discard(); check(p.target.isRemoved(), "The selected live victim is actually removed"); }
			case TARGET_DIMENSION -> {
				ServerLevel destination = server.getLevel(Level.NETHER);
				UUID id = p.target.getUUID();
				check(p.target.teleportTo(destination, VICTIM.x, VICTIM.y, VICTIM.z, Set.of(), 180, 0, false), "The selected victim actually departs the released world");
				p.movedTarget = (LivingEntity) destination.getEntity(id);
				check(p.movedTarget != null && p.movedTarget.isAlive(), "Native target teleport creates or moves the real victim in its destination world");
				if (p.movedTarget != p.target) p.fixtures.add(p.movedTarget);
			}
			case TEAM -> ally(owner, p.target);
			case PARTY, DUEL -> {
				HailSkyPartySupport.join(owner, p.guest);
				check(!ArtKit.harmable(owner, p.target), "A real post-release party membership change protects the selected target");
				if (p.scenario.change() == Change.DUEL) {
					var duel = Duels.startBout(owner, p.guest, new DuelRules.Terms(40, 0, 600, 0, 1, false), FEET, null);
					duel.tick(p.level.getGameTime());
					check(ArtKit.harmable(owner, p.target), "A real explicitly fighting duel readmits the same selected party ally");
				}
			}
			case TRIAL -> {
				HailSkyTrialSupport.removeEnrollment(owner, p.trial);
				check(!ArtKit.harmable(owner, p.trial), "Real roster pruning protects the trial master from its former participant");
			}
			case LETHAL -> {
				p.target.setHealth(.05F);
				// Keep room for a genuine art drink, including the wound after the initial direct hit.
				owner.setHealth(4);
			}
		}
		p.mutationHealth = p.target.getHealth(); p.mutationMendRoom = ArtKit.mendRoom(owner);
		// valid() itself can retire: let the production callback discover invalid owners without a test-side poll.
		if (!p.scenario.change().retiresOwner()) check(p.binding.valid(), "Target changes and ordinary physical cancellation preserve released ownership");
	}

	private static void afterDirect(Probe p) {
		// Direct callbacks have now enqueued their inherited wounds. Append this observer after them,
		// since tasks sharing a due tick execute in registration order.
		Scheduler.later(ArtRules.BLEED_PERIOD, () -> checked(p, () -> afterFirstWound(p)));
		if (p.scenario.boundary() == Boundary.BEFORE_DIRECT && (p.scenario.change().retiresOwner() || p.scenario.change().blocksTarget())) {
			check(p.hits == 0, "No direct damage reaches a retired owner or a no-longer-admitted selected target");
			check(!Reactions.has(p.target, Reactions.Mark.BLEEDING), "Rejected direct work adds no bleeding mark or wound");
		} else {
			check(p.hits == 1, "The real selected target receives exactly one admitted direct hit");
			if (p.scenario.boundary() != Boundary.DIRECT_DAMAGE && p.scenario.change() != Change.LETHAL) {
				check(Reactions.has(p.target, Reactions.Mark.BLEEDING), "A live lawful direct hit creates the real Moon wound");
			}
		}
		if (p.scenario.boundary() == Boundary.DIRECT_DAMAGE) assertCallbackBoundary(p);
		if (p.scenario.change() == Change.LETHAL && p.scenario.boundary() == Boundary.BEFORE_DIRECT) assertLethalDrink(p);
		check(p.destination.getHealth() == HEALTH && !Reactions.has(p.destination, Reactions.Mark.BLEEDING), "Moon never retargets a destination-world bystander");
	}

	private static void afterFirstWound(Probe p) {
		if (p.scenario.boundary() == Boundary.WOUND_DAMAGE) assertCallbackBoundary(p);
		if (p.scenario.change() == Change.LETHAL && p.scenario.boundary() == Boundary.BEFORE_WOUND) assertLethalDrink(p);
		if (p.scenario.change().retiresOwner() || p.scenario.change().blocksTarget()) {
			int expected = switch (p.scenario.boundary()) {
				case BEFORE_DIRECT -> 0; case BEFORE_WOUND, DIRECT_DAMAGE -> 1; case WOUND_DAMAGE -> 2;
			};
			check(p.hits == expected, "Only hits admitted before the exact retirement/admission change complete");
		} else if (p.scenario.change() != Change.LETHAL && p.scenario.change() != Change.DUEL) {
			check(p.hits == 2, "The inherited first wound still lands after the ordinary post-release physical change");
		}
	}

	private static void assertCallbackBoundary(Probe p) {
		check(p.mutatedOnce, "Retirement or target change was observed inside a real native damage callback");
		if (p.scenario.change().retiresOwner() || p.scenario.change() == Change.TARGET_REMOVAL || p.scenario.change() == Change.TARGET_DIMENSION) {
			check(close(ArtKit.mendRoom(p.owner), p.mutationMendRoom), "No Moon drink consumes any art-mending room after the callback retires its owner or removes its target from the release world");
		}
		// A lawful hit may drink after its recipient joins a team; the next harm or new wound rechecks allegiance.
		if (p.second != null) {
			int expected = p.scenario.boundary() == Boundary.DIRECT_DAMAGE ? 0 : 1;
			check(p.secondHits == expected, "Retirement suppresses the next already-selected direct hit or wound in the same scheduler tick");
		}
		// Do not require the common already-admitted Hits strike to roll back its passive heal, stance or short mark.
	}

	private static void assertLethalDrink(Probe p) {
		check(!p.target.isAlive(), "The admitted real direct/wound hit is lethal");
		check(p.binding.valid() && p.owner.isAlive(), "A lawful lethal victim never retires its live owner");
		check(ArtKit.mendRoom(p.owner) < p.mutationMendRoom && p.owner.getHealth() > 4,
			"A valid lethal hit still supplies Moon's actual drink, measured independently of the common Crimson passive");
	}

	private static void finish(Probe p) {
		check(p.level.getGameTime() == p.released + FINISH, "The dedicated release world keeps ticking past every direct hit and all six wound beats");
		check(p.binding.valid() != p.scenario.change().retiresOwner(), "The old original-body binding remains permanently retired exactly for lifecycle changes");
		if (p.scenario.change().retiresOwner() || p.scenario.change().blocksTarget()) {
			int expected = switch (p.scenario.boundary()) {
				case BEFORE_DIRECT -> 0; case BEFORE_WOUND, DIRECT_DAMAGE -> 1; case WOUND_DAMAGE -> 2;
			};
			check(p.hits == expected, "Every later direct or wound task remains harmless after invalidation");
			check(p.target.getHealth() == p.mutationHealth, "No later Moon work changes the retired or protected target's health");
			if (p.movedTarget != null) check(p.movedTarget.getHealth() == p.mutationHealth, "A departed target takes no delayed damage in its new world");
		} else if (p.scenario.change() == Change.LETHAL) {
			check(p.hits == (p.scenario.boundary() == Boundary.BEFORE_DIRECT ? 1 : 2), "A dead victim receives no later wound callbacks");
		} else if (p.scenario.change() == Change.DUEL) {
			check(p.hits > 0 && p.target.getHealth() < p.initialHealth, "Dynamic duel readmission permits real damage under the ordinary PvP cap");
		} else {
			check(p.hits == 1 + ArtRules.MOON_BLEEDS, "All six actual wound ticks survive weapon changes, method changes, interruption and recovery expiry");
			if (p.profile != null) {
				check(!p.physical.getAsBoolean() && !MastersArts.committed(p.owner), "The physical sequence has ended before the released wound lifetime finishes");
				check(DIRECT + ArtRules.MOON_BLEEDS * ArtRules.BLEED_PERIOD > p.profile.recovery(), "A real wound beat outlives ordinary physical recovery");
			}
		}
		if (p.second != null) check(p.secondHits == (p.scenario.boundary() == Boundary.DIRECT_DAMAGE ? 0 : 1), "No later callback revives a second victim's retired wound");
		check(p.destination.getHealth() == HEALTH && !Reactions.has(p.destination, Reactions.Mark.BLEEDING), "Every foreign-world bystander stays untouched through the whole old release");
		p.finalHealth = p.target.getHealth(); p.finished = true; onDamage = null;
	}

	private static void restore(MinecraftServer server, Probe p) {
		ServerPlayer owner = server.getPlayerList().getPlayer(p.owner.getUUID());
		if (!owner.isAlive()) owner = respawn(server, owner);
		prepare(owner, p.level);
		if (p.scenario.change().retiresOwner()) check(!p.binding.valid() && ReleasedArtOwner.capture(owner).valid(), "Returning to an eligible live original-world body cannot revive an old Moon release");
		cleanup(p);
	}

	private static ServerPlayer respawn(MinecraftServer server, ServerPlayer dead) {
		dead.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
		ServerPlayer replacement = server.getPlayerList().getPlayer(dead.getUUID());
		check(replacement != null && replacement != dead && replacement.isAlive() && dead.isRemoved(), "Native respawn replaces the body while preserving its UUID");
		check(replacement.connection.player == replacement, "The real connection now owns the replacement body");
		return replacement;
	}

	private static void prepare(ServerPlayer owner, ServerLevel level) {
		Duels.callOff(owner); HailSkyPartySupport.leave(owner); MethodArts.forget(owner.getUUID());
		level.getScoreboard().removePlayerFromTeam(owner.getScoreboardName());
		owner.setGameMode(GameType.SURVIVAL);
		check(owner.teleportTo(level, FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "The actual player connection receives its arena teleport");
		owner.setNoGravity(true); owner.setDeltaMovement(Vec3.ZERO); owner.resetFallDistance();
		owner.setOnGround(true); owner.setShiftKeyDown(false); owner.setSprinting(false); owner.stopUsingItem();
		owner.removeAllEffects(); owner.clearFire(); owner.setHealth(12);
		owner.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("crimson", AuraRules.SOVEREIGN, 4500, 160, 0));
		owner.setAttached(SwordStrings.COOLDOWNS, SwordStrings.Cooldowns.NONE);
		owner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		owner.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		SwordStrings.forget(owner.getUUID()); Momentum.reset(owner);
	}

	private static LivingEntity target(Probe p, ServerLevel level, Vec3 at) {
		Mob target = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		check(target != null, "The native Moon victim exists");
		target.addTag("wildercord.rolled"); target.setNoAi(true); target.setNoGravity(true); target.setPersistenceRequired();
		target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
		target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
		var reinforcements = target.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
		if (reinforcements != null) reinforcements.setBaseValue(0);
		target.setHealth(HEALTH); target.snapTo(at.x, at.y, at.z, 180, 0);
		check(level.addFreshEntity(target), "The native victim is admitted to its world"); p.fixtures.add(target);
		check(target.getHealth() == HEALTH && target.getMaxHealth() == HEALTH && !target.hasAttached(WildercordAttachments.RUNEBOUND),
			"Native admission preserves a deterministic ordinary 200-health victim");
		return target;
	}

	private static Guest guest(Probe p, Vec3 at) {
		Guest guest = new Guest(p.level); guest.setGameMode(GameType.SURVIVAL); guest.setNoGravity(true);
		guest.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
		guest.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); guest.setHealth(HEALTH);
		guest.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("stone", AuraRules.GLOW, 100, 0, 0));
		guest.snapTo(at.x, at.y, at.z, 180, 0); p.level.addNewPlayer(guest); p.fixtures.add(guest);
		return guest;
	}

	private static void ally(ServerPlayer owner, LivingEntity target) {
		var board = owner.level().getScoreboard(); var team = board.getPlayerTeam("moon_owner_friends");
		if (team == null) team = board.addPlayerTeam("moon_owner_friends");
		team.setAllowFriendlyFire(false); board.addPlayerToTeam(owner.getScoreboardName(), team); board.addPlayerToTeam(target.getScoreboardName(), team);
		check(!ArtKit.harmable(owner, target), "The actual scoreboard team immediately protects this selected victim");
	}

	private static void cleanup(Probe p) {
		Duels.callOff(p.owner); HailSkyPartySupport.leave(p.owner);
		p.level.getScoreboard().removePlayerFromTeam(p.owner.getScoreboardName());
		for (LivingEntity entity : p.fixtures) {
			if (entity instanceof ServerPlayer player) HailSkyPartySupport.leave(player);
			p.level.getScoreboard().removePlayerFromTeam(entity.getScoreboardName()); entity.discard();
		}
	}

	private static void arena(ServerLevel level) {
		check(level != null, "Both real native dimensions exist");
		for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++) level.setChunkForced(x, z, true);
		for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
			level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 2);
			for (int y = 100; y <= 107; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
		}
	}
	private static void checked(Probe p, Runnable action) {
		if (p.failure != null) return;
		try { action.run(); } catch (RuntimeException | AssertionError failure) { p.failure = failure; p.finished = true; }
	}
	private static boolean close(double a, double b) { return Math.abs(a - b) < 1.0E-4; }
	private static int localPort() {
		try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) { return socket.getLocalPort(); }
		catch (java.io.IOException failure) { throw new AssertionError("Could not reserve native-test loopback port", failure); }
	}
	private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
}
