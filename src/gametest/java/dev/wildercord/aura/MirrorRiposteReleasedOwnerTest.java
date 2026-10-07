package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtFields;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.ArtWards;
import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.aura.arts.RimeArts;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Statuses;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.projectile.arrow.Arrow;
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
import java.util.function.Consumer;

/** Real guard/Attack/Punch admission, native connected bodies, delayed damage callbacks and actual ward collisions. */
public final class MirrorRiposteReleasedOwnerTest implements FabricClientGameTest {
	private enum Change {
		CONTROL(true), WEAPON(true), METHOD(true), INTERRUPTION(true),
		DEATH(false), RESPAWN(false), DIMENSION(false), ROUND_TRIP(false), DISCONNECT(false);
		final boolean survives;
		Change(boolean survives) { this.survives = survives; }
	}
	private static final Vec3 FEET = new Vec3(.5, 100, .5);
	private static final float HEALTH = 200;
	private static final List<Integer> MARKS = List.of(SwordString.Token.marks(SwordString.Token.COUNTER, SwordString.Token.LOW));
	private Probe active;
	private boolean spellControls;
	private final List<Entity> fixtures = new ArrayList<>();

	private record Hit(long tick, float amount) {}
	private final class Foe extends Husk {
		final List<Hit> hits = new ArrayList<>();
		int nestedHits;
		final List<Long> spellTicks = new ArrayList<>();
		Consumer<Foe> after, nested;
		Foe(ServerLevel level) { super(EntityTypes.HUSK, level); }
		@Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
			boolean took = super.hurtServer(level, source, amount);
			Probe p = active;
			if (p != null && p.accepted > 0 && source.getEntity() == p.owner) {
				if (source.is(Aura.DAMAGE)) {
					if (CounterHitCapture.direct(p.owner, p.art, this)) {
						p.action = CounterHitCapture.action(p.owner, p.art, this, p.action);
						hits.add(new Hit(level.getGameTime(), amount));
						if (after != null) { var callback = after; after = null; callback.accept(this); }
					}
				} else if (p.spells == null || took && CounterSpellCapture.matches(p.spells, this, source)) {
					spellTicks.add(level.getGameTime());
					if (level.getGameTime() == p.released + 1) nestedHits++;
					if (nested != null && ArtHitScope.boundary(p.owner) != null) { var callback = nested; nested = null; callback.accept(this); }
				}
			}
			return took;
		}
	}
	private final class Guest extends net.fabricmc.fabric.api.entity.FakePlayer {
		Consumer<Guest> after;
		int hits;
		Guest(ServerLevel level) { super(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "RiposteParty")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
		@Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
			boolean took = super.hurtServer(level, source, amount);
			if (source.is(Aura.DAMAGE) && active != null && source.getEntity() == active.owner
				&& CounterHitCapture.direct(active.owner, active.art, this)) {
				active.action = CounterHitCapture.action(active.owner, active.art, this, active.action);
				hits++; if (after != null) { var callback = after; after = null; callback.accept(this); }
			}
			return took;
		}
	}
	private final class Probe {
		final String art, scenario;
		final Change change;
		final ServerPlayer owner;
		final ServerLevel level;
		final List<Foe> line = new ArrayList<>();
		Foe foreign, shooter;
		final List<Foe> collateral = new ArrayList<>();
		Stance.State stance;
		Momentum.State momentum;
		BladeBond blade;
		Guest guest;
		final double[] damage = new double[6];
		ReleasedArtOwner lifetime;
		java.util.function.BooleanSupplier physical;
		long accepted, released;
		int releases, addonAfter;
		Object action;
		CounterSpellCapture.Session spells;
		Arrow reflected, lastReflection;
		Vec3 incoming;
		boolean finished, callback;
		float health;
		Throwable failure;
		Probe(String art, String scenario, Change change, ServerPlayer owner) {
			this.art = art; this.scenario = scenario; this.change = change; this.owner = owner; this.level = owner.level();
		}
	}

	@Override public void runTest(ClientGameTestContext context) {
		Properties properties = new Properties();
		properties.setProperty("server-ip", "127.0.0.1"); properties.setProperty("server-port", Integer.toString(localPort()));
		properties.setProperty("online-mode", "false"); properties.setProperty("enforce-secure-profile", "false");
		properties.setProperty("pause-when-empty-seconds", "-1"); properties.setProperty("view-distance", "3"); properties.setProperty("simulation-distance", "3");
		AuraApi.StringHook hook = this::released;
		AuraApi.StanceHook stanceHook = (player, target, amount, source) -> {
			Probe p = active;
			if (p != null && p.scenario.equals("rune_allied") && player == p.guest && target == p.collateral.getFirst()
				&& source == StanceRules.Source.ART && ArtHitScope.released(p.owner) != null && CounterSpellCapture.paidPair(p.spells, p.guest, target)) {
				p.callback = true; p.stance = Stance.state(target); p.line.getFirst().discard();
			}
			if (p != null && p.scenario.equals("stance_hook") && target == p.line.get(1) && ArtHitScope.released(player) != null) {
				p.callback = true; p.stance = Stance.state(target); p.line.getFirst().discard();
			}
			return amount;
		};
		AuraApi.MomentumHook momentumHook = (player, amount, source) -> {
			Probe p = active;
			if (p != null && p.scenario.equals("momentum_hook") && ArtHitScope.released(player) != null) {
				p.callback = true; p.momentum = Momentum.state(player); p.line.getFirst().discard();
			}
			return amount;
		};
		AuraApi.BladeHook bladeHook = new AuraApi.BladeHook() {
			@Override public double resonance(ServerPlayer player, double amount, String source) {
				Probe p = active;
				if (p != null && p.scenario.equals("blade_hook") && ArtHitScope.released(player) != null) {
					p.callback = true; BondedBlades.flush(player); p.blade = BondedBlades.bond(player.getMainHandItem()); p.line.getFirst().discard();
				}
				return amount;
			}
		};
		try (var server = context.worldBuilder().createServer(properties)) {
			TestDedicatedServerConnection connection = server.connect();
			try {
				connection.waitForChunksDownload(); connection.waitForClientboundPackets();
				server.runCommand("gamerule minecraft:spawn_mobs false");
				server.runCommand("gamerule minecraft:natural_health_regeneration false");
				server.runOnServer(s -> { arena(s.overworld()); arena(s.getLevel(Level.NETHER)); AuraApi.onString(hook); AuraApi.onStance(stanceHook); AuraApi.onMomentum(momentumHook); AuraApi.onBlade(bladeHook);
					dev.wildercord.cast.AddonRunes.reaction("wildercord:test_riposte_first", "arcane", (caster, target) -> {
						Probe p = active;
						if (p != null && p.scenario.equals("rune_addon") && caster == p.owner && target == p.line.get(1)
							&& ArtHitScope.released(p.owner) != null && CounterSpellCapture.inEtching(p.spells, target)) { p.callback = true; p.line.getFirst().discard(); }
						return 1;
					});
					dev.wildercord.cast.AddonRunes.reaction("wildercord:test_riposte_second", "arcane", (caster, target) -> {
						Probe p = active; if (p != null && p.scenario.equals("rune_addon") && caster == p.owner && target == p.line.get(1)) p.addonAfter++;
						return 1;
					});
				});
				for (String art : List.of("glacier_mirror", "static_riposte")) {
					for (Change change : Change.values()) {
						server.runOnServer(s -> begin(s, art, "owner", change));
						server.waitFor(s -> active.finished, 180);
						server.runOnServer(s -> verifyFinished());
						if (change == Change.DISCONNECT) {
							context.waitFor(mc -> mc.level == null, 200); context.setScreen(TitleScreen::new);
							connection = server.connect(); connection.waitForChunksDownload();
							server.runOnServer(s -> check(s.getPlayerList().getPlayer(active.owner.getUUID()) != active.owner
								&& !active.lifetime.valid(), "Reconnect cannot revive the original released owner"));
						}
						server.runOnServer(this::restore); context.runOnClient(mc -> mc.gui.setScreen(null)); context.waitTicks(5);
					}
				}
				for (String scenario : List.of("four_hops", "strict_range", "no_los", "dead_source", "removed_source", "replaced_source", "foreign_source",
					"source_team", "target_team", "target_range", "callback_source", "callback_target", "callback_owner", "rune_source", "resonance_source",
					"conduct_control", "conduct_source", "conduct_owner", "conduct_team", "party_callback", "party_source", "stance_hook", "momentum_hook", "blade_hook",
					"fire_source", "life_source", "wind_source", "time_source", "rune_shock_no_los", "rune_clock", "rune_addon", "rune_allied")) {
					server.runOnServer(s -> begin(s, "static_riposte", scenario, Change.CONTROL));
					server.waitFor(s -> active.finished, 180); server.runOnServer(s -> verifyFinished());
					server.runOnServer(this::restore); context.waitTicks(5);
				}
				for (String scenario : List.of("ward_eviction", "ward_replacement", "last_tick_transfer", "last_tick_owner", "last_tick_removed", "mirror_chill_cover")) {
					server.runOnServer(s -> begin(s, "glacier_mirror", scenario, Change.CONTROL));
					server.waitFor(s -> active.finished, 180); server.runOnServer(s -> verifyFinished());
					server.runOnServer(this::restore); context.waitTicks(5);
				}
			} finally {
				server.runOnServer(s -> { AuraApi.stringHooks().remove(hook); AuraApi.stanceHooks().remove(stanceHook); AuraApi.momentumHooks().remove(momentumHook); AuraApi.bladeHooks().remove(bladeHook);
					// The public API replaces registrations by ID; cleanup leaves two inert, non-element observers.
					dev.wildercord.cast.AddonRunes.reaction("wildercord:test_riposte_first", "wildercord_test_inactive", (caster, target) -> 1);
					dev.wildercord.cast.AddonRunes.reaction("wildercord:test_riposte_second", "wildercord_test_inactive", (caster, target) -> 1);
					if (active != null) CounterSpellCapture.close(active.spells);
					fixtures.forEach(Entity::discard); fixtures.clear(); });
				if (context.computeOnClient(mc -> mc.level != null)) connection.close();
				else context.setScreen(TitleScreen::new);
			}
		}
	}

	private void begin(MinecraftServer server, String art, String scenario, Change change) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		prepare(player, server.overworld(), art); arena(player.level());
		Probe p = new Probe(art, scenario, change, player); active = p;
		check(player.connection.getRemoteAddress() instanceof InetSocketAddress address && address.getAddress().isLoopbackAddress(), "Real loopback TCP owner");
		for (int i = 0; i < 6; i++) p.line.add(foe(player.level(), .5, 2.1 + i * 4));
		p.foreign = foe(server.getLevel(Level.NETHER), .5, 6.1);
		if (scenario.equals("mirror_chill_cover")) {
			p.line.get(1).teleportTo(2.3, 100, 2.3);
			for (int y = 100; y <= 103; y++) p.level.setBlockAndUpdate(new BlockPos(1, y, 1), Blocks.STONE.defaultBlockState());
			check(player.hasLineOfSight(p.line.getFirst()) && !player.hasLineOfSight(p.line.get(1)), "Primary stays visible while the original chill cone contains a covered secondary body");
		}
		for (int i = 1; i < p.line.size(); i++) p.damage[i] = ArtKit.weapon(player) * ArtKit.scale()
			* ArtRules.chain(ArtRules.RIPOSTE_FACTOR, ArtRules.RIPOSTE_KEEP, i)
			* dev.wildercord.cast.AuraElements.bonus(player, p.line.get(i), p.level.damageSources().source(Aura.DAMAGE, player, player), Aura.element(player));
		player.setShiftKeyDown(true); check(AuraGuard.raise(player) && AuraGuard.perfectNow(player), "Native perfect guard is truly raised");
		float health = player.getHealth(); p.line.getFirst().doHurtTarget(player.level(), player);
		check(player.getHealth() == health && AuraGuard.caught(player) != null, "Actual hostile melee earns the original counter receipt");
		p.line.getFirst().teleportTo(.5, 100, 2.1); p.line.getFirst().setDeltaMovement(Vec3.ZERO);
		player.connection.handleAttack(new ServerboundAttackPacket(p.line.getFirst().getId()));
		player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
		p.line.forEach(f -> { f.hits.clear(); Effects.readyToHurt(f); f.setDeltaMovement(Vec3.ZERO); });
		p.line.getFirst().teleportTo(.5, 100, 2.1);
		var ability = AuraApi.artOf(player, art).orElseThrow(); check(SwordStrings.saw(player, ability), "Authentic observed guard/low suffix");
		// An actual immune primary leaves the first successful art damage for the delayed rune test.
		if (scenario.startsWith("rune_")) p.line.getFirst().setPermanentlyInvulnerable(true);
		if (scenario.equals("blade_hook")) check(BondedBlades.bond(player, InteractionHand.MAIN_HAND, "native Riposte callback test"), "Native bonded-blade admission");
		p.accepted = player.level().getGameTime(); float aura = Aura.aura(player);
		SwordStrings.request(player, new SwordStrings.Perform(art, MARKS));
		check(Aura.aura(player) < aura && SwordStrings.readyAt(player, art) > p.accepted && MastersArts.committed(player), "Real request pays and admits its timeline");
	}

	private void released(ServerPlayer player, AuraApi.StringArt art, AuraApi.StringContext input) {
		Probe p = active; if (p == null || p.owner != player || !p.art.equals(art.id())) return;
		checked(p, () -> {
			p.releases++; p.released = p.level.getGameTime(); p.lifetime = ReleasedArtOwner.capture(player); p.physical = MastersArts.continuation(player);
			check(p.released == p.accepted + MastersStyleRules.of(p.art).windup() && p.line.getFirst().hits.size() == 1,
				"Exactly one native primary hit and completion at the accepted release tick");
			check(p.physical.getAsBoolean() && p.lifetime.valid(), "Physical recovery and released owner begin independently");
			if (p.scenario.equals("owner")) mutateOwner(p);
			else if (p.art.equals("static_riposte")) setChainScenario(p);
			else setWardScenario(p);
			if (p.art.equals("glacier_mirror") && p.change.survives) {
				p.shooter = foe(p.level, -8.5, .5);
				// Native teleport publishes the turn to the client too, so ordinary movement packets cannot reset it.
				player.teleportTo(p.level, FEET.x, FEET.y, FEET.z, Set.of(), 90, 0, false);
				p.reflected = arrow(p, player.getEyePosition().add(-2.6, -.3, -1.6), player.getEyePosition().add(0, -.3, 0));
				p.incoming = p.reflected.getDeltaMovement().normalize();
			}
			Scheduler.later(7, () -> checked(p, () -> firstEffects(p)));
			Scheduler.later(p.scenario.equals("rune_clock") ? 85 : 53, () -> checked(p, () -> finish(p)));
		});
	}

	private void mutateOwner(Probe p) {
		ServerPlayer player = p.owner; MinecraftServer server = p.level.getServer();
		switch (p.change) {
			case CONTROL -> { }
			case WEAPON -> player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			case METHOD -> player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, Aura.aura(player), 0));
			case INTERRUPTION -> {
				player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD)); player.startUsingItem(InteractionHand.OFF_HAND);
				check(Statuses.interrupt(player) && !p.physical.getAsBoolean(), "Native interruption cancels physical recovery after release");
			}
			case DEATH -> { player.kill(p.level); check(!player.isAlive(), "Native kill retires the released owner"); }
			case RESPAWN -> {
				player.kill(p.level); ServerPlayer replacement = HailfallReleasedOwnerTest.respawn(server, player); prepare(replacement, p.level, p.art);
				if (p.art.equals("glacier_mirror")) ArtWards.mirror(replacement, ArtRules.MIRROR_TICKS);
			}
			case DIMENSION, ROUND_TRIP -> {
				check(player.teleportTo(server.getLevel(Level.NETHER), FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "Native owner world departure");
				if (p.change == Change.ROUND_TRIP) check(player.teleportTo(p.level, FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "Same-callback return without polling old lifetime");
			}
			case DISCONNECT -> {
				Connection socket = server.getConnection().getConnections().stream().filter(c -> c.getPacketListener() == player.connection).findFirst().orElseThrow();
				check(socket.isConnected() && !socket.isMemoryConnection(), "Actual connected TCP socket");
				socket.disconnect(Component.literal("Mirror/Riposte released owner test")); socket.handleDisconnection();
				check(server.getPlayerList().getPlayer(player.getUUID()) == null && player.isRemoved(), "Normal disconnect removes original body");
			}
		}
	}

	private void setChainScenario(Probe p) {
		Foe primary = p.line.get(0), firstHop = p.line.get(1);
		if (p.scenario.startsWith("rune_") || p.scenario.equals("resonance_source")) {
			var rune = p.scenario.equals("resonance_source") ? null : p.scenario.equals("rune_clock") ? dev.wildercord.spell.Runes.STORMCLOCK
				: List.of("rune_shock_no_los", "rune_allied").contains(p.scenario) ? dev.wildercord.spell.Runes.SHOCK : dev.wildercord.spell.Runes.HARM;
			if (!spellControls) { CounterSpellCaptureChecks.run(p.owner, firstHop, p.action); spellControls = true; }
			p.spells = CounterSpellCapture.begin(p.owner, p.action, firstHop, p.released, rune);
		}
		switch (p.scenario) {
			case "four_hops", "stance_hook", "momentum_hook", "blade_hook" -> { }
			case "strict_range" -> firstHop.teleportTo(.5, 100, primary.getZ() + ArtRules.RIPOSTE_REACH);
			case "no_los" -> { for (int y = 100; y <= 104; y++) p.level.setBlockAndUpdate(new BlockPos(0, y, 4), Blocks.STONE.defaultBlockState());
				check(!primary.hasLineOfSight(firstHop), "Opaque native wall genuinely blocks the link's LOS"); }
			case "dead_source" -> { primary.kill(p.level); check(!primary.isAlive() && p.level.getEntity(primary.getUUID()) == primary, "Lethal original source is still loaded"); }
			case "removed_source" -> primary.discard();
			case "replaced_source" -> { var id = primary.getUUID(); primary.discard(); Foe replacement = new Foe(p.level); replacement.setUUID(id);
				replacement.snapTo(.5, 100, 2.1, 0, 0); check(p.level.addFreshEntity(replacement) && p.level.getEntity(id) == replacement, "Actual same-UUID replacement body"); fixtures.add(replacement); }
			case "foreign_source" -> check(primary.teleportTo(p.level.getServer().getLevel(Level.NETHER), .5, 100, 2.1, Set.of(), 0, 0, false), "Original source actually leaves its world");
			case "source_team" -> ally(p.owner, primary);
			case "target_team" -> firstHop.after = hit -> { p.callback = true; ally(p.owner, hit); };
			case "target_range" -> firstHop.after = hit -> { p.callback = true; hit.teleportTo(.5, 100, 30.1); };
			case "callback_source" -> firstHop.after = hit -> { p.callback = true; primary.discard(); };
			case "callback_target" -> firstHop.after = hit -> { p.callback = true; hit.discard(); };
			case "callback_owner" -> firstHop.after = hit -> { p.callback = true; p.owner.kill(p.level); };
			case "party_callback", "party_source" -> {
				firstHop.discard(); p.guest = new Guest(p.level); p.guest.setGameMode(GameType.SURVIVAL);
				p.guest.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); p.guest.setHealth(200);
				p.guest.snapTo(.5, 100, 6.1, 0, 0); p.level.addNewPlayer(p.guest); fixtures.add(p.guest);
				p.level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.PVP, true, p.level.getServer());
				if (p.scenario.equals("party_callback")) p.guest.after = hit -> { p.callback = true; dev.wildercord.party.HailSkyPartySupport.join(p.owner, hit); };
				else p.line.get(2).after = hit -> { p.callback = true; dev.wildercord.party.HailSkyPartySupport.join(p.owner, p.guest); };
			}
			case "conduct_control", "conduct_source", "conduct_owner", "conduct_team" -> {
				// Remove alternative ordinary collateral, keeping two native close bodies behind an opaque wall.
				for (int i = 2; i < p.line.size(); i++) p.line.get(i).teleportTo(.5, 100, 40 + 4 * i);
				primary.teleportTo(.5, 100, 2.1);
				p.collateral.add(foe(p.level, 2.0, 6.1)); p.collateral.add(foe(p.level, 2.7, 6.1));
				for (int y = 100; y <= 104; y++) p.level.setBlockAndUpdate(new BlockPos(1, y, 6), Blocks.STONE.defaultBlockState());
				check(!firstHop.hasLineOfSight(p.collateral.getFirst()), "Ordinary elemental collateral deliberately has no LOS");
				// A lethal but loaded original conductor remains valid, and cannot consume a living collateral slot.
				primary.kill(p.level);
				dev.wildercord.cast.Reactions.mark(firstHop, dev.wildercord.cast.Reactions.Mark.IONISED, 40);
				for (Foe collateral : p.collateral) collateral.nested = hit -> {
					if (p.callback) return; p.callback = true;
					switch (p.scenario) {
						case "conduct_source" -> primary.discard();
						case "conduct_owner" -> p.owner.kill(p.level);
						case "conduct_team" -> ally(p.owner, primary);
						default -> { }
					}
				};
			}
			case "fire_source", "life_source", "wind_source", "time_source" -> {
				String method = switch (p.scenario) { case "fire_source" -> "ember"; case "life_source" -> "verdant"; case "wind_source" -> "gale"; default -> "hourglass"; };
				p.owner.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(method, AuraRules.SOVEREIGN, 4500, Aura.aura(p.owner), 0));
				p.owner.setHealth(10); p.health = p.owner.getHealth();
				for (int i = 2; i < p.line.size(); i++) p.line.get(i).teleportTo(.5, 100, 40 + 4 * i);
				primary.kill(p.level);
				p.collateral.add(foe(p.level, 2.1, 6.1)); p.collateral.add(foe(p.level, 2.8, 6.1));
				Consumer<Foe> retire = hit -> { p.callback = true; primary.discard(); };
				if (p.scenario.equals("fire_source")) {
					dev.wildercord.cast.Reactions.mark(firstHop, dev.wildercord.cast.Reactions.Mark.WINDSWEPT, 40);
					p.collateral.forEach(f -> f.nested = retire);
				} else {
					firstHop.nested = retire;
					if (p.scenario.equals("time_source")) firstHop.igniteForSeconds(4);
					else dev.wildercord.cast.Reactions.mark(firstHop, p.scenario.equals("life_source")
						? dev.wildercord.cast.Reactions.Mark.SHADOWED : dev.wildercord.cast.Reactions.Mark.BLEEDING, 40);
				}
			}
			case "rune_shock_no_los", "rune_clock", "rune_allied" -> {
				primary.setPermanentlyInvulnerable(false);
				dev.wildercord.player.Spellbooks.setCord(p.owner, new ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));
				dev.wildercord.player.Spellbooks.setMana(p.owner, 100); p.owner.setAttached(RuneEtchings.READY, 0L);
				p.owner.getMainHandItem().set(RuneEtchings.RUNE, p.scenario.equals("rune_clock") ? dev.wildercord.spell.Runes.STORMCLOCK.id() : dev.wildercord.spell.Runes.SHOCK.id());
				if (!p.scenario.equals("rune_clock")) {
					p.collateral.add(foe(p.level, 2.2, 6.1));
					for (int y = 100; y <= 104; y++) p.level.setBlockAndUpdate(new BlockPos(1, y, 6), Blocks.STONE.defaultBlockState());
					check(!firstHop.hasLineOfSight(p.collateral.getFirst()), "Etched Shock collateral genuinely lies behind an opaque wall");
					if (p.scenario.equals("rune_allied")) {
						p.guest = new Guest(p.level); p.guest.setGameMode(GameType.SURVIVAL); p.guest.snapTo(-2, 100, .5, 0, 0);
						p.guest.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, 160, 0));
						p.guest.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD)); p.level.addNewPlayer(p.guest); fixtures.add(p.guest);
						dev.wildercord.party.HailSkyPartySupport.join(p.owner, p.guest);
						p.collateral.getFirst().nested = hit -> {
							Effects.readyToHurt(hit);
							check(AuraCombat.projected(p.guest, hit, 1, true) > 0, "An actual allied projected blade hit seeds the pending pair inside the spell callback");
						};
					}
				} else Scheduler.later(6, () -> checked(p, () -> {
					primary.discard(); p.owner.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
					p.owner.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD)); p.owner.startUsingItem(InteractionHand.OFF_HAND);
					check(Statuses.interrupt(p.owner), "Ordinary physical interruption occurs after all four chain hops");
				}));
			}
			case "rune_source", "resonance_source", "rune_addon" -> {
				primary.setPermanentlyInvulnerable(false);
				if (!p.scenario.equals("resonance_source")) {
					dev.wildercord.player.Spellbooks.setCord(p.owner, new ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));
					dev.wildercord.player.Spellbooks.setMana(p.owner, 100); p.owner.setAttached(RuneEtchings.READY, 0L);
					p.owner.getMainHandItem().set(RuneEtchings.RUNE, dev.wildercord.spell.Runes.HARM.id());
				} else {
					var plan = dev.wildercord.spell.SpellCompiler.compile(List.of(dev.wildercord.spell.Runes.TOUCH, dev.wildercord.spell.Runes.HARM));
					var seed = new dev.wildercord.cast.Cast(p.owner, 1, dev.wildercord.player.Heart.Bonuses.NONE, false, null,
						new dev.wildercord.cast.Cast.Info(plan.root(), 2, "arcane", List.of(dev.wildercord.spell.Runes.TOUCH, dev.wildercord.spell.Runes.HARM)));
					CounterSpellCapture.seed(p.spells, seed);
					Effects.apply(seed, plan.root().groups.getFirst().effects.getFirst(),
						new dev.wildercord.cast.Cast.Hit(List.of(firstHop), firstHop.position(), new Vec3(0, 0, 1), p.owner.getEyePosition(), null, null, false));
					Effects.readyToHurt(firstHop);
				}
				if (!p.scenario.equals("rune_addon")) firstHop.nested = hit -> { p.callback = true; primary.discard(); };
			}
			default -> throw new AssertionError(p.scenario);
		}
	}

	private void setWardScenario(Probe p) {
		if (p.scenario.equals("ward_eviction")) {
			for (int i = 0; i < ArtFields.PER_PLAYER; i++) ArtFields.open(p.owner, "mirror_eviction_fixture", ArtFields.disc(p.owner::position, 1, 1), 60, 60, (field, owner, age) -> {});
			check(ArtFields.count(p.owner, RimeArts.MIRROR) == 0 && ArtWards.mirrored(p.owner), "Cosmetic cap eviction preserves the independent ward");
		} else if (p.scenario.equals("ward_replacement")) {
			Scheduler.later(45, () -> checked(p, () -> ArtWards.mirror(p.owner, 50)));
		} else if (p.scenario.startsWith("last_tick_")) {
			Scheduler.later(ArtRules.MIRROR_TICKS, () -> checked(p, () -> {
				check(ArtWards.mirrored(p.owner), "The original inclusive last legitimate ward tick remains admitted");
				p.lastReflection = arrow(p, p.owner.getEyePosition().add(-1, 0, 0), p.owner.getEyePosition());
				var deflection = ArtWards.deflection(p.owner, p.lastReflection);
				check(deflection != null, "The exact last-tick reflection receives its real ward deflector");
				deflection.deflect(p.lastReflection, p.owner, p.level.getRandom(), Vec3.ZERO);
				check(Math.abs(p.lastReflection.getDeltaMovement().length() - 2) < .0001, "Original 1.6-speed projectile is reflected one quarter faster");
				if (p.scenario.equals("last_tick_owner")) p.owner.kill(p.level);
				if (p.scenario.equals("last_tick_removed")) p.lastReflection.discard();
			}));
		}
	}

	private void firstEffects(Probe p) {
		CounterHitCapture.assertIdle();
		if (p.spells != null) CounterSpellCapture.verify(p.spells, p.scenario.equals("resonance_source") || p.scenario.equals("rune_allied"),
			!p.scenario.equals("resonance_source") && !p.scenario.equals("rune_addon"));
		if (p.art.equals("glacier_mirror")) {
			if (p.scenario.equals("mirror_chill_cover")) check(p.line.get(1).hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)
				&& p.line.get(1).getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS).getAmplifier() == 1 && p.line.get(1).getTicksFrozen() > 0
				&& p.line.get(1).hits.isEmpty(), "Mirror preserves its original covered-collateral chill without adding a second primary hit");
			check(ArtWards.mirrored(p.owner) == p.change.survives, "Ward tracks only the original owner lifetime");
			if (p.change.survives) check(p.reflected.getOwner() == p.owner && p.reflected.getDeltaMovement().normalize().dot(p.incoming) < -.3,
				"A real colliding projectile follows live ninety-degree facing and transfers ownership");
			else check(ArtFields.count(p.owner, RimeArts.MIRROR) == 0, "Retired original-body cosmetic pane has ended");
			if (p.change == Change.RESPAWN) check(ArtWards.mirrored(p.level.getServer().getPlayerList().getPlayer(p.owner.getUUID())),
				"Old-body ward/field cleanup never removes the replacement body's new ward");
			return;
		}
		if (List.of("fire_source", "life_source", "wind_source", "time_source").contains(p.scenario)) {
			check(p.callback && p.line.get(1).hits.isEmpty(), "Actual changed-element nested damage retires its source before the outer delayed hit");
			if (p.scenario.equals("fire_source")) check(p.collateral.stream().filter(Entity::isOnFire).count() == 1
				&& p.collateral.stream().mapToInt(f -> f.nestedHits).sum() == 1, "No later Wildfire ignition or damage after retirement");
			else check(p.collateral.stream().noneMatch(f -> f.hasEffect(net.minecraft.world.effect.MobEffects.POISON)), "No later Blight poison after retirement");
			check(p.owner.getHealth() == p.health, "No later Blight/Rupture healing after retirement"); return;
		}
		if (p.scenario.equals("rune_allied")) {
			check(p.callback && p.collateral.getFirst().spellTicks.contains(p.released + 1), "A real scoped spell closes the allied blade pair and reaches its stance utility hook");
			check(java.util.Objects.equals(p.stance, Stance.state(p.collateral.getFirst())), "Allied utility rechecks the original spell consequence before committing stance");
			check(ArtHitScope.released(p.owner) == null && ArtHitScope.released(p.guest) == null, "Both nested actor scopes restore after utility");
			for (int i = 2; i < p.line.size(); i++) check(p.line.get(i).hits.isEmpty(), "Allied callback retirement suppresses later chain hops"); return;
		}
		if (p.scenario.equals("rune_shock_no_los")) {
			check(p.collateral.getFirst().spellTicks.contains(p.released + 1), "The actual delayed etched Shock keeps ordinary no-LOS collateral");
			check(p.owner.getAttachedOrElse(RuneEtchings.READY, 0L) > p.released, "The existing inscription actually paid its cooldown"); return;
		}
		if (p.scenario.equals("rune_clock")) {
			check(p.line.get(1).spellTicks.contains(p.released + 1) && p.line.getFirst().isRemoved(), "A real delayed first-success inscription released its clock before conductor retirement"); return;
		}
		boolean all = p.change.survives && List.of("owner", "four_hops", "no_los", "dead_source").contains(p.scenario);
		boolean callback = List.of("target_team", "target_range", "callback_source", "callback_target", "callback_owner", "rune_source", "resonance_source", "rune_addon", "stance_hook", "momentum_hook", "blade_hook").contains(p.scenario);
		if (p.scenario.startsWith("conduct_")) {
			int collateral = p.collateral.stream().mapToInt(f -> f.nestedHits).sum();
			check(p.callback && collateral == (p.scenario.equals("conduct_control") ? 2 : 1), "Real ionised collateral keeps ordinary two-body/no-LOS behavior and stops immediately after retirement");
			if (!p.scenario.equals("conduct_control")) check(p.line.get(1).hits.isEmpty(), "Nested Conduct retirement prevents even the pending primary hop damage");
			check(ArtHitScope.boundary(p.owner) == null, "Nested elemental callback restores the released scope"); return;
		}
		if (p.scenario.startsWith("party_")) {
			check(p.callback && p.guest.hits == 1, "Real player party consent occurs in the delayed chain callback");
			check(!dev.wildercord.cast.Reactions.has(p.guest, dev.wildercord.cast.Reactions.Mark.IONISED) || p.scenario.equals("party_source"), "Newly allied live target receives no post-callback shock");
			for (int i = p.scenario.equals("party_source") ? 3 : 2; i < p.line.size(); i++) check(p.line.get(i).hits.isEmpty(), "Current party protection retires later callbacks");
			return;
		}
		if (all) {
			for (int hop = 1; hop <= ArtRules.RIPOSTE_JUMPS; hop++) {
				Foe foe = p.line.get(hop); check(foe.hits.size() == 1 && foe.hits.getFirst().tick() == p.released + hop, "Exactly one nearest distinct hop at each original one-tick beat");
				if (p.change == Change.CONTROL && !p.scenario.equals("dead_source")) {
					check(Math.abs(foe.hits.getFirst().amount() - p.damage[hop]) < .02, "Original hop damage decay and modifiers");
				}
			}
		} else if (callback) {
			check(p.callback && p.line.get(1).hits.size() == 1, "The real delayed direct/nested callback actually ran");
			check(!dev.wildercord.cast.Reactions.has(p.line.get(1), dev.wildercord.cast.Reactions.Mark.IONISED),
				"An invalidated delayed hit cannot add its later shock");
			for (int hop = 2; hop < p.line.size(); hop++) check(p.line.get(hop).hits.isEmpty(), "No later hop after callback retirement");
			if (p.scenario.equals("rune_addon")) check(p.addonAfter == 0 && !p.line.get(1).spellTicks.contains(p.released + 1), "Retiring addon reaction stops later callbacks and pending spell damage");
			if (p.scenario.equals("stance_hook")) check(java.util.Objects.equals(p.stance, Stance.state(p.line.get(1))), "Stance hook retirement precedes committing its wear");
			if (p.scenario.equals("momentum_hook")) check(p.momentum.equals(Momentum.state(p.owner)), "Momentum hook retirement precedes committing its gain");
			if (p.scenario.equals("blade_hook")) { BondedBlades.flush(p.owner); check(p.blade.equals(BondedBlades.bond(p.owner.getMainHandItem())), "Blade hook retirement precedes pending resonance/history mutation"); }
		} else for (int hop = 1; hop < p.line.size(); hop++) check(p.line.get(hop).hits.isEmpty(), "Invalid original conductor/owner or exact strict-five boundary stops the chain");
		check(p.line.get(5).hits.isEmpty() && p.foreign.hits.isEmpty(), "Never a fifth hop or foreign-world retarget");
		check(ArtHitScope.boundary(p.owner) == null, "Released-hit authority is restored after every callback");
	}

	private void finish(Probe p) {
		CounterSpellCapture.assertIdle();
		if (p.scenario.equals("owner")) check(p.lifetime.valid() == p.change.survives, "Original owner lifetime never revives");
		if (p.scenario.equals("rune_clock")) check(p.line.get(1).spellTicks.contains(p.released + 41) && p.line.get(1).spellTicks.contains(p.released + 81),
			"Legitimately released Stormclock keeps both forty/eighty-tick strikes after chain expiry, conductor removal and ordinary gear/interruption");
		if (p.art.equals("glacier_mirror")) {
			check(ArtWards.mirrored(p.owner) == p.scenario.equals("ward_replacement"), "Expiry/stale old field cleanup cannot remove a newer ward");
			if (p.scenario.equals("last_tick_transfer")) check(p.lastReflection.getOwner() == p.owner, "Last legitimate reflection transfers ownership after ward expiry");
			if (p.scenario.equals("last_tick_owner") || p.scenario.equals("last_tick_removed")) check(p.lastReflection.getOwner() != p.owner,
				"Retired owner or removed projectile cannot receive the delayed ownership transfer");
		}
		check(p.foreign.hits.isEmpty(), "Foreign targets stay untouched through all original callbacks"); p.finished = true;
	}

	private void verifyFinished() {
		if (active.failure != null) throw new AssertionError(active.art + ": " + active.scenario + ": " + active.change, active.failure);
		check(active.releases == 1 && active.finished, "Exactly one actual registered release finishes");
		Wildercord.LOGGER.info("MIRROR_RIPOSTE_LIFETIME art={} scenario={} ownerChange={} accepted={} released={} hits={} callback={}",
			active.art, active.scenario, active.change, active.accepted, active.released, active.line.stream().map(f -> f.hits.size()).toList(), active.callback);
	}
	private void restore(MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayer(active.owner.getUUID());
		if (!player.isAlive()) player = HailfallReleasedOwnerTest.respawn(server, player);
		if (active.guest != null) { dev.wildercord.party.HailSkyPartySupport.leave(active.guest); dev.wildercord.party.HailSkyPartySupport.leave(player); }
		CounterSpellCapture.close(active.spells);
		prepare(player, server.overworld(), active.art); fixtures.forEach(Entity::discard); fixtures.clear(); active = null;
	}
	private static void prepare(ServerPlayer player, ServerLevel level, String art) {
		MastersArts.cancel(player); SwordStrings.forget(player.getUUID()); AuraGuard.forget(player.getUUID());
		HailfallReleasedOwnerTest.prepare(player, level);
		player.removeAttached(AuraAttachments.STATE); player.removeAttached(Momentum.MOMENTUM);
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(ArtRules.art(art).method(), AuraRules.SOVEREIGN, 4500, 160, 0));
		var board = level.getScoreboard(); var team = board.getPlayerTeam("released_counter_allies"); if (team != null) board.removePlayerTeam(team);
	}
	private Foe foe(ServerLevel level, double x, double z) {
		Foe target = new Foe(level); target.addTag("wildercord.rolled"); target.setNoAi(true); target.setNoGravity(true);
		target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); target.setHealth(HEALTH);
		target.snapTo(x, 100, z, 180, 0); check(level.addFreshEntity(target), "Native exact fixture body is loaded"); fixtures.add(target); return target;
	}
	private Arrow arrow(Probe p, Vec3 from, Vec3 toward) {
		Arrow arrow = new Arrow(EntityTypes.ARROW, p.level); arrow.setNoGravity(true); arrow.setOwner(p.shooter == null ? p.line.getFirst() : p.shooter);
		arrow.snapTo(from.x, from.y, from.z); arrow.setDeltaMovement(toward.subtract(from).normalize().scale(1.6));
		check(p.level.addFreshEntity(arrow), "Native projectile is loaded"); fixtures.add(arrow); return arrow;
	}
	private static void ally(ServerPlayer owner, LivingEntity target) {
		var board = owner.level().getScoreboard(); var team = board.getPlayerTeam("released_counter_allies"); if (team == null) team = board.addPlayerTeam("released_counter_allies");
		board.addPlayerToTeam(owner.getScoreboardName(), team); board.addPlayerToTeam(target.getScoreboardName(), team);
	}
	private static void arena(ServerLevel level) {
		for (int x = -1; x <= 0; x++) for (int z = -1; z <= 2; z++) level.setChunkForced(x, z, true);
		for (int x = -8; x <= 8; x++) for (int z = -8; z <= 26; z++) {
			level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 2);
			for (int y = 100; y <= 106; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
		}
	}
	private static void checked(Probe p, Runnable task) { if (p.failure == null) try { task.run(); } catch (RuntimeException | AssertionError e) { p.failure = e; p.finished = true; } }
	private static int localPort() { try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) { return socket.getLocalPort(); } catch (java.io.IOException e) { throw new AssertionError(e); } }
	private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
}
