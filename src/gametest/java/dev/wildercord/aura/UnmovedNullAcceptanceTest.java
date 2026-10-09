package dev.wildercord.aura;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.client.EarnedCounterHelp;
import dev.wildercord.client.SwordStringsClient;
import dev.wildercord.client.WildercordKeys;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/** Isolated native Unmoved/Null proof, policy and lifecycle fixtures. Registration/execution remain separate review gates. */
public final class UnmovedNullAcceptanceTest implements FabricClientGameTest {
	private static final List<Integer> MARKS = List.of(SwordString.Token.marks(SwordString.Token.COUNTER, SwordString.Token.LOW));
	private static final Vec3 FEET = new Vec3(.5, 100, .5);
	private final List<Entity> fixtures = new ArrayList<>();
	private Probe active;
	private InputConstants.Key oldKey;

	private static final class Guest extends net.fabricmc.fabric.api.entity.FakePlayer {
		Guest(ServerLevel level) { super(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "CounterClash")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private record Hit(long tick, float amount) {}
	private final class Foe extends Husk {
		final List<Hit> hits = new ArrayList<>();
		Consumer<Foe> after, afterLinked;
		Foe(ServerLevel level) { super(EntityTypes.HUSK, level); }
		@Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
			boolean result = super.hurtServer(level, source, amount);
			if (source.is(Aura.DAMAGE) && active != null && source.getEntity() == active.owner
				&& CounterHitCapture.direct(active.owner, active.art, this)) {
				active.action = CounterHitCapture.action(active.owner, active.art, this, active.action);
				hits.add(new Hit(level.getGameTime(), amount)); active.order.add(this);
				if (after != null) { var callback = after; after = null; callback.accept(this); }
			}
			if (!source.is(Aura.DAMAGE) && afterLinked != null && source.getEntity() instanceof ServerPlayer owner
				&& MastersArts.earnedCounter(owner) != null) { var callback = afterLinked; afterLinked = null; callback.accept(this); }
			return result;
		}
	}
	private static final class Probe {
		final String art;
		ServerPlayer owner;
		Object action;
		Foe target, splash, replacement;
		long accepted, hookAt, ready;
		int effectiveRest;
		float caught, health;
		int spends, hooks;
		double expected;
		boolean beforeFrame, exposed, recovery, ended;
		Consumer<ServerPlayer> paymentCallback, beforeAdmission;
		final List<Foe> order = new ArrayList<>();
		EarnedCounters.Route route;
		Probe(String art) { this.art = art; }
	}

	@Override public void runTest(ClientGameTestContext context) {
		AuraApi.SpendHook spend = (player, amount, reason, backlash) -> {
			var p = active;
			if (p == null || !reason.equals("art:" + p.art)) return;
			p.spends++; p.accepted = player.level().getGameTime(); p.health = player.getHealth();
			int wind = MastersStyleRules.of(p.art).windup(), recovery = MastersStyleRules.of(p.art).recovery();
			Scheduler.later(1, () -> p.exposed = !AuraGuard.guarding(player) && !AuraGuard.raise(player));
			Scheduler.later(wind - 1, () -> p.beforeFrame = p.target.hits.isEmpty() && p.splash.hits.isEmpty() && p.hooks == 0);
			Scheduler.later(wind + recovery - 1, () -> p.recovery = MastersArts.committed(player) && !AuraGuard.raise(player));
			Scheduler.later(wind + recovery, () -> p.ended = !MastersArts.committed(player));
			if (p.paymentCallback != null) p.paymentCallback.accept(player);
		};
		AuraApi.StringHook complete = (player, art, input) -> {
			var p = active;
			if (p == null || !art.id().equals(p.art)) return;
			p.hooks++; p.hookAt = player.level().getGameTime();
			var release = MastersArts.earnedCounter(player);
			check(release != null && release.caughtDamage() == p.caught, "The actual performer keeps the exact earned catch");
			check(input.marks().size() == 1 && SwordString.Token.COUNTER.fits(input.marks().getFirst()), "The real one-stroke counter proof reaches hooks");
			p.route = release.route();
		};
		AuraApi.onSpend(spend); AuraApi.onString(complete);
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("fill -12 99 -12 12 99 12 minecraft:stone");
			world.getServer().runCommand("fill -12 100 -12 12 108 12 minecraft:air");
			world.getServer().runCommand("teleport @a .5 100 .5");
			context.waitTicks(25);
			context.runOnClient(mc -> {
				mc.options.toggleCrouch().set(false);
				oldKey = KeyMappingHelper.getBoundKeyOf(WildercordKeys.auraMapping());
				WildercordKeys.auraMapping().setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_I));
				KeyMapping.resetMapping();
				check(EarnedCounterHelp.controls("unmoved").getString().contains(WildercordKeys.auraMapping().getTranslatedKeyMessage().getString()),
					"Counter help displays the real rebound Aura key");
			});
			for (String art : List.of("unmoved", "null_parry")) {
				realKeys(context, world, art);
				routes(context, world, art);
				for (String cause : List.of("removed", "uuid", "replacement", "target_world", "owner_world", "team", "weapon", "interrupt")) loss(context, world, art, cause);
				for (String cause : List.of("no_aura", "unearned", "stale")) refusal(context, world, art, cause);
				empty(context, world, art);
				callback(context, world, art, false);
				callback(context, world, art, true);
				clash(context, world, art, true, false);
				clash(context, world, art, true, true);
				clash(context, world, art, false, false);
				party(context, world, art);
				linkedCallback(context, world, art, true);
				linkedCallback(context, world, art, false);
				paidCancellation(context, world, art);
				lateEligibilityCancellation(context, world, art);
				zeroPrice(context, world, art);
				if (art.equals("unmoved")) for (String cause : List.of("released", "owner_world", "target_replacement", "team")) landing(context, world, cause);
				if (art.equals("null_parry")) pulseCap(context, world);
				if (art.equals("unmoved")) paidWardDeadline(context, world);
			}
		} finally {
			active = null; AuraApi.spendHooks().remove(spend); AuraApi.stringHooks().remove(complete);
			context.getInput().releaseKey(o -> o.keyShift);
			if (oldKey != null) context.runOnClient(mc -> { WildercordKeys.auraMapping().setKey(oldKey); KeyMapping.resetMapping(); });
		}
	}

	private void realKeys(ClientGameTestContext context, TestSingleplayerContext world, String art) {
		context.getInput().releaseKey(o -> o.keyShift); context.waitTicks(85);
		Probe p = on(world, player -> prepare(player, art));
		context.waitTicks(5);
		context.getInput().holdKey(o -> o.keyShift); context.waitTicks(2);
		context.getInput().pressKey(WildercordKeys.auraMapping());
		for (int t = 0; t < 4 && !on(world, AuraGuard::perfectNow); t++) context.waitTicks(1);
		on(world, player -> { catchBlow(player, p); return null; });
		context.waitTicks(2);
		context.getInput().pressKey(o -> o.keyAttack);
		context.waitTicks(2);
		check(context.computeOnClient(mc -> art.equals(SwordStringsClient.lastAsked())), "Actual attack input emits the registered counter Perform payload");
		on(world, player -> {
			check(p.spends == 1 && MastersArts.committed(player) && !AuraGuard.guarding(player), "Still-sneaking real input pays once and drops guard");
			p.ready = SwordStrings.readyAt(player, art);
			return null;
		});
		context.waitTicks(25); context.getInput().releaseKey(o -> o.keyShift);
		on(world, player -> {
			verify(p, player, true);
			check(p.route == EarnedCounters.Route.STRUCK || p.route == EarnedCounters.Route.CAUGHT, "The actual attacked/caught body stays selected");
			check(p.target.hits.size() == 1 && p.target.hits.getFirst().tick() == p.accepted + MastersStyleRules.of(art).windup(), "Exactly one primary hit lands on the fixed server active tick");
			check(Math.abs(p.target.hits.getFirst().amount() - p.expected) < .02, "Active damage retains its original weapon factor");
			if (art.equals("null_parry")) check(p.order.indexOf(p.splash) < p.order.indexOf(p.target), "The radial pulse precedes the primary strike");
			return null;
		});
	}

	private Probe request(ClientGameTestContext context, TestSingleplayerContext world, String art, boolean target,
			Consumer<ServerPlayer> before, Consumer<ServerPlayer> after) {
		context.waitTicks(85);
		return on(world, player -> {
			var p = prepare(player, art); player.setShiftKeyDown(true);
			check(AuraGuard.raise(player) && AuraGuard.perfectNow(player), "Native guard begins in its true perfect window");
			catchBlow(player, p);
			before.accept(player);
			if (target) player.connection.handleAttack(new ServerboundAttackPacket(p.target.getId()));
			player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
			p.target.hits.clear(); p.splash.hits.clear();
			Effects.readyToHurt(p.target); Effects.readyToHurt(p.splash);
			var ability = AuraApi.artOf(player, art).orElseThrow();
			check(SwordStrings.saw(player, ability), "Native Attack/Punch owns an authentic guard-earned stroke");
			float aura = Aura.aura(player); double price = SwordStrings.price(player, ability); p.effectiveRest = SwordStrings.rest(player, ability);
			if (p.beforeAdmission != null) p.beforeAdmission.accept(player);
			SwordStrings.request(player, new SwordStrings.Perform(art, MARKS));
			check(p.spends == 1 && Math.abs(Aura.aura(player) - (aura - price)) < .001 && !AuraGuard.guarding(player), "Paid request lowers its original guard once");
			p.ready = SwordStrings.readyAt(player, art);
			check(p.ready == p.accepted + p.effectiveRest, "Paid admission anchors the exact effective individual rest once");
			if (art.equals("unmoved") && p.paymentCallback == null) check(dev.wildercord.aura.arts.ArtWards.hardened(player), "Unmoved defense is finalized on the accepted tick, before offensive windup");
			float paid = Aura.aura(player);
			SwordStrings.request(player, new SwordStrings.Perform(art, MARKS));
			check(p.spends == 1 && Aura.aura(player) == paid && !SwordStrings.saw(player, ability), "A duplicate cannot repay or reuse the consumed ledger stroke");
			after.accept(player);
			return p;
		});
	}

	private void routes(ClientGameTestContext context, TestSingleplayerContext world, String art) {
		for (String route : List.of("struck", "distant_struck", "caught", "nearest", "bound", "outside", "covered")) {
			Probe p = request(context, world, art, route.equals("struck") || route.equals("distant_struck"), player -> {
				if (route.equals("struck")) active.target = foe(player.level(), -.5, 2.5);
				if (route.equals("distant_struck")) active.beforeAdmission = owner -> active.target.teleportTo(.5, 100, 20.5);
				if (route.equals("nearest")) active.target.teleportTo(20.5, 100, .5);
				if (route.equals("bound") || route.equals("outside")) {
					active.target.teleportTo(.5, 100, 6.5 + (route.equals("outside") ? .01 : 0));
					active.splash.teleportTo(20.5, 100, 3.5);
				}
				if (route.equals("covered")) for (int y = 100; y <= 103; y++)
					player.level().setBlockAndUpdate(new BlockPos(0, y, 1), Blocks.STONE.defaultBlockState());
			}, player -> {});
			context.waitTicks(25);
			on(world, player -> {
				verify(p, player, true);
				var expected = switch (route) {
					case "struck", "distant_struck" -> EarnedCounters.Route.STRUCK;
					case "caught", "bound", "covered" -> EarnedCounters.Route.CAUGHT;
					case "nearest" -> EarnedCounters.Route.NEAREST;
					default -> EarnedCounters.Route.EMPTY;
				};
				check(p.route == expected, "Original route priority: " + art + "/" + route);
				if (route.equals("nearest") || route.equals("outside")) check(p.target.hits.isEmpty(), "No fallback revives an out-of-range caught body");
				else check(p.target.hits.size() == 1, "Original primary admits covered and deliberately distant struck bodies");
				if (route.equals("covered") && art.equals("null_parry")) check(p.splash.hits.size() == 1, "Covered radial collateral retains its original no-LOS policy");
				return null;
			});
		}
	}

	private void loss(ClientGameTestContext context, TestSingleplayerContext world, String art, String cause) {
		Probe p = request(context, world, art, true, player -> {}, player -> {
			var q = active;
			switch (cause) {
				case "removed" -> q.target.discard();
				case "uuid" -> q.target.setUUID(java.util.UUID.randomUUID());
				case "replacement" -> {
					var uuid = q.target.getUUID(); q.target.discard(); var replacement = new Foe(player.level());
					replacement.setUUID(uuid); replacement.setNoAi(true); replacement.snapTo(.5, 100, 2.1, 180, 0);
					check(player.level().addFreshEntity(replacement) && player.level().getEntity(uuid) == replacement,
						"Distinct same-UUID body is actually loaded at the original target"); fixtures.add(replacement); q.replacement = replacement;
				}
				case "target_world" -> check(q.target.teleportTo(player.level().getServer().getLevel(net.minecraft.world.level.Level.NETHER),
					.5, 120, 2.1, Set.of(), 0, 0, false), "Selected body actually changes world");
				case "owner_world" -> {
					var level = player.level();
					check(player.teleportTo(level.getServer().getLevel(net.minecraft.world.level.Level.NETHER), .5, 120, .5, Set.of(), 0, 0, false), "Owner actually leaves the original world");
					check(player.teleportTo(level, FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "Same owner body returns in the same callback");
				}
				case "range" -> q.target.teleportTo(.5, 100, 10.5);
				case "cover" -> { for (int y = 100; y <= 103; y++) player.level().setBlockAndUpdate(new BlockPos(0, y, 1), Blocks.STONE.defaultBlockState()); }
				case "team" -> {
					var board = player.level().getScoreboard(); var team = board.getPlayerTeam("earned_counter_allies");
					if (team == null) team = board.addPlayerTeam("earned_counter_allies");
					board.addPlayerToTeam(player.getScoreboardName(), team); board.addPlayerToTeam(q.target.getScoreboardName(), team);
				}
				case "weapon" -> { int slot = player.getInventory().getSelectedSlot(); player.getInventory().setSelectedSlot((slot + 1) % 9); player.getInventory().setSelectedSlot(slot); }
				case "interrupt" -> dev.wildercord.cast.Charging.interrupt(player);
			}
		});
		context.waitTicks(25);
		on(world, player -> {
			boolean pulse = art.equals("null_parry") && !List.of("owner_world", "weapon", "interrupt").contains(cause);
			verify(p, player, pulse);
			check(p.target.hits.isEmpty() && (pulse ? p.splash.hits.size() == 1 : p.splash.hits.isEmpty()),
				"Lost primary cannot retarget; only Null's independent valid-owner radial pulse survives: " + cause);
			if (p.replacement != null) {
				check(p.replacement.hits.size() == (pulse ? 1 : 0), "Replacement is eligible only for Null's fresh radial query, never the captured primary");
				if (pulse) check(Math.abs(p.replacement.hits.getFirst().amount() - p.expected * ArtRules.NULL_PULSE_FACTOR) < .02,
					"Same-UUID replacement takes genuine quarter-weapon radial damage, never one-weapon primary damage");
				check(!dev.wildercord.aura.arts.ArtWards.silenced(p.replacement)
					&& !p.replacement.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "Replacement cannot inherit primary hold or silence");
			}
			if (art.equals("unmoved") && !cause.equals("owner_world")) check(dev.wildercord.aura.arts.ArtWards.hardened(player),
				"Later offensive cancellation keeps already-paid Unmoved defense");
			return null;
		});
	}

	private void refusal(ClientGameTestContext context, TestSingleplayerContext world, String art, String cause) {
		context.getInput().holdKey(o -> o.keyShift); context.waitTicks(85);
		Probe p = on(world, player -> {
			var q = prepare(player, art); player.setShiftKeyDown(true); check(AuraGuard.raise(player), "Refusal starts with a real guard");
			if (!cause.equals("unearned")) catchBlow(player, q);
			return q;
		});
		if (cause.equals("stale")) context.waitTicks(17);
		if (cause.equals("unearned")) {
			context.waitTicks(on(world, WayEffects::perfectWindow) + 1);
			on(world, player -> { player.setShiftKeyDown(true); p.target.doHurtTarget(player.level(), player);
				check(AuraGuard.caught(player) == null, "An ordinary held-guard hit creates no perfect-catch receipt"); return null; });
		}
		on(world, player -> {
			player.setShiftKeyDown(true);
			if (cause.equals("no_aura")) player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(1));
			player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
			float aura = Aura.aura(player); var state = Aura.state(player);
			SwordStrings.request(player, new SwordStrings.Perform(art, MARKS));
			check(p.spends == 0 && !MastersArts.committed(player) && Aura.aura(player) == aura && Aura.state(player).equals(state), "Refusal " + cause + " cannot lower guard or alter payment/rest");
			check(SwordStrings.readyAt(player, art) == 0, "Refusal starts no art rest");
			return null;
		});
	}

	private void empty(ClientGameTestContext context, TestSingleplayerContext world, String art) {
		Probe p = request(context, world, art, false, player -> {
			active.target.teleportTo(10.5, 100, .5); active.splash.teleportTo(10.5, 100, 3.5);
		}, player -> active.splash.teleportTo(.8, 100, 2.5));
		context.waitTicks(25);
		on(world, player -> {
			verify(p, player, true);
			check(p.route == EarnedCounters.Route.EMPTY && p.target.hits.isEmpty(), "An explicit empty route never selects a new primary");
			if (art.equals("null_parry")) check(p.splash.hits.size() == 1, "EMPTY Null still releases its quarter-weapon radial pulse");
			else check(p.splash.hits.isEmpty() && dev.wildercord.aura.arts.ArtWards.hardened(player), "EMPTY Unmoved grants defense without a new primary");
			return null;
		});
	}

	private void callback(ClientGameTestContext context, TestSingleplayerContext world, String art, boolean retire) {
		Probe p = request(context, world, art, true, player -> {}, player -> {
			Foe first = art.equals("null_parry") ? active.splash : active.target;
			first.after = foe -> {
				if (retire) active.target.discard();
				else { player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); dev.wildercord.cast.Charging.interrupt(player); }
			};
		});
		context.waitTicks(25);
		on(world, player -> {
			check(p.spends == 1 && p.hooks == (retire && art.equals("unmoved") ? 0 : 1), "Generic Null pulse completion is independent of primary loss");
			if (retire && art.equals("null_parry")) check(p.target.hits.isEmpty() && p.splash.hits.size() == 1, "Pulse callback cannot replace the removed original primary");
			if (!retire) check(p.target.hits.size() == 1, "Released callbacks survive ordinary gear/physical interruption");
			return null;
		});
	}

	private void clash(ClientGameTestContext context, TestSingleplayerContext world, String art, boolean wins, boolean lostTarget) {
		context.waitTicks(85);
		Probe p = on(world, player -> {
			var q = prepare(player, art); player.setShiftKeyDown(true);
			check(AuraGuard.raise(player), "Clash begins with the same real earned guard"); catchBlow(player, q);
			player.connection.handleAttack(new ServerboundAttackPacket(q.target.getId()));
			player.connection.handlePunch(ServerboundPunchPacket.INSTANCE); q.target.setDeltaMovement(Vec3.ZERO);
			var guest = new Guest(player.level()); guest.setGameMode(GameType.SURVIVAL); guest.snapTo(.5, 100, 4.5, 180, 0);
			player.level().addNewPlayer(guest); fixtures.add(guest);
			player.level().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.PVP, true, player.level().getServer());
			Clashes.forgetRest();
			Crescents.launch(guest, player.getEyePosition().add(0, 0, 2), new Vec3(0, 0, -1), 0x88CCFF,
				6, 0, .7, 12, 2, 6, false, entity -> false, (flight, target) -> 0);
			float aura = Aura.aura(player);
			SwordStrings.request(player, new SwordStrings.Perform(art, MARKS));
			check(Clashes.holding(player) && q.spends == 0 && Aura.aura(player) == aura && AuraGuard.guarding(player),
				"Actual oncoming crescent holds the already consumed counter unpaid without dropping guard");
			SwordStrings.request(player, new SwordStrings.Perform(art, MARKS));
			check(q.spends == 0, "A duplicate cannot create another held art");
			for (int beat = 0; beat < ClashRules.BEATS; beat++) Scheduler.later(ClashRules.beat(beat), () -> Clashes.pressFor(wins ? player : guest));
			if (lostTarget) Scheduler.later(20, q.target::discard);
			Scheduler.later(ClashRules.serverLength() - 1, () -> check(Clashes.holding(player) && q.spends == 0,
				"Reservation lasts only the original finite clash, with no early payment"));
			return q;
		});
		context.waitTicks(ClashRules.serverLength() + 25);
		on(world, player -> {
			check(!Clashes.holding(player) && p.spends == 1, "Clash always resolves and pays exactly once");
			check(AuraGuard.caught(player) == null, "The old mutable catch cache has expired before checking the preserved receipt");
			if (wins && !lostTarget) {
				check(p.hooks == 1 && p.route == EarnedCounters.Route.STRUCK && p.target.hits.size() == 1,
					"Winning finite clash carries the original selected body and original caught bonus through expiry");
				check(p.target.hits.getFirst().tick() == p.accepted + MastersStyleRules.of(art).windup(), "Clash winner begins its paid timeline at resolution");
			} else {
				boolean pulse = wins && lostTarget && art.equals("null_parry");
				check(p.hooks == (pulse ? 1 : 0) && p.target.hits.isEmpty() && (pulse ? p.splash.hits.size() == 1 : p.splash.hits.isEmpty()),
					"Lost clash releases nothing; a valid Null win with a lost primary retains only the independent pulse");
			}
			if (art.equals("unmoved")) check(dev.wildercord.aura.arts.ArtWards.hardened(player) == wins, "Only finalized paid winning admission grants the ward");
			return null;
		});
	}

	private void party(ClientGameTestContext context, TestSingleplayerContext world, String art) {
		final Guest[] guest = {null};
		Probe p = request(context, world, art, false, player -> {
			active.target.teleportTo(10.5, 100, .5); active.splash.teleportTo(10.5, 100, 3.5);
			guest[0] = new Guest(player.level()); guest[0].setGameMode(GameType.SURVIVAL); guest[0].snapTo(.5, 100, 2.2, 180, 0);
			player.level().addNewPlayer(guest[0]); fixtures.add(guest[0]);
			player.level().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.PVP, true, player.level().getServer());
			check(ArtKit.harmable(player, guest[0]), "A real native player body is initially hostile");
		}, player -> dev.wildercord.party.HailSkyPartySupport.join(player, guest[0]));
		context.waitTicks(25);
		on(world, player -> {
			verify(p, player, art.equals("null_parry"));
			check(guest[0].getHealth() == guest[0].getMaxHealth() && player.getHealth() == p.health,
				"Actual party consent during windup suppresses selected-target damage, status and self-heal");
			dev.wildercord.party.HailSkyPartySupport.leave(guest[0]); dev.wildercord.party.HailSkyPartySupport.leave(player);
			return null;
		});
	}

	private void linkedCallback(ClientGameTestContext context, TestSingleplayerContext world, String art, boolean etched) {
		boolean[] nested = {false};
		Probe p = request(context, world, art, etched, player -> {}, player -> {
			Foe first = art.equals("null_parry") ? active.splash : active.target;
			first.afterLinked = foe -> { nested[0] = true; active.health = player.getHealth(); active.target.discard(); };
			if (etched) {
				dev.wildercord.player.Spellbooks.setCord(player, new ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));
				dev.wildercord.player.Spellbooks.setMana(player, 100); player.setAttached(RuneEtchings.READY, 0L);
				player.getMainHandItem().set(RuneEtchings.RUNE, dev.wildercord.spell.Runes.HARM.id());
			} else Scheduler.later(MastersStyleRules.of(art).windup() - 1, () -> {
				var plan = dev.wildercord.spell.SpellCompiler.compile(List.of(dev.wildercord.spell.Runes.TOUCH, dev.wildercord.spell.Runes.HARM));
				float before = first.getHealth();
				Effects.apply(new dev.wildercord.cast.Cast(player), plan.root().groups.getFirst().effects.getFirst(),
					new dev.wildercord.cast.Cast.Hit(List.of(first), first.position(), new Vec3(0, 0, 1), player.getEyePosition(), null, null, false));
				check(first.getHealth() < before, "An actual ordinary spell hit primes the resonance before the counter release");
			});
		});
		context.waitTicks(25);
		on(world, player -> {
			check(nested[0], "The real counter reaches its nested " + (etched ? "etched rune" : "resonant") + " damage callback");
			check(p.spends == 1 && p.hooks == (art.equals("null_parry") ? 1 : 0) && player.getHealth() == p.health,
				"Nested damage losing the primary stops primary consequences while Null pulse completion remains independent");
			check((art.equals("null_parry") ? p.target : p.splash).hits.isEmpty(), "No later counter recipient mutates after nested callback invalidation");
			return null;
		});
	}

	private void paidCancellation(ClientGameTestContext context, TestSingleplayerContext world, String art) {
		Probe p = request(context, world, art, false,
			player -> active.paymentCallback = owner -> {
				check(MastersArts.committed(owner), "Shared recovery is installed before the payment callback");
				check(!dev.wildercord.aura.arts.ArtWards.hardened(owner), "Unmoved cannot grant defense before payment finalizes");
				MastersArts.cancel(owner);
			}, player -> check(!dev.wildercord.aura.arts.ArtWards.hardened(player), "Payment callback cancellation cannot grant a free ward"));
		context.waitTicks(25);
		on(world, player -> { verify(p, player, false); check(p.target.hits.isEmpty() && p.splash.hits.isEmpty(), "Paid cancellation cannot revive offense"); return null; });
	}

	/** Focused admission harness uses a genuine caught blow; packet authenticity is covered separately by realKeys/request. */
	private void lateEligibilityCancellation(ClientGameTestContext context, TestSingleplayerContext world, String art) {
		context.waitTicks(85);
		on(world, player -> {
			Probe p = prepare(player, art); player.setShiftKeyDown(true);
			check(AuraGuard.raise(player), "Focused transaction starts with a genuine guard"); catchBlow(player, p);
			var original = AuraApi.artOf(player, art).orElseThrow();
			var wrapped = new AuraApi.StringArt(original.id(), original.string(), original.stage(), original.cost(), original.cooldownTicks(), owner -> {
				if (p.spends > 0) MastersArts.cancel((ServerPlayer) owner);
				return original.available().test(owner);
			}, original.condition(), original.performer());
			var accepted = EarnedCounters.accept(player, new AuraApi.StringContext(wrapped, MARKS, p.target, player.level().getGameTime()),
				EarnedCounters.Guard.capture(player, AuraGuard.caught(player)), () -> true);
			check(accepted != null, "Genuine original guard can enter focused transaction harness");
			Object clash = new Object(); long tick = player.level().getServer().getTickCount(); accepted.hold(clash, tick);
			check(SwordStrings.release(player, wrapped, MARKS, accepted, clash), "A paid callback-cancelled winning admission is terminally accepted");
			check(p.spends == 1 && !dev.wildercord.aura.arts.ArtWards.hardened(player), "Late eligibility callback cannot revive token or grant ward");
			check(!SwordStrings.release(player, wrapped, MARKS, accepted, clash) && p.spends == 1, "Duplicate winning resume cannot forfeit and repay");
			return null;
		});
		context.waitTicks(25);
		on(world, player -> { check(active.hooks == 0 && active.target.hits.isEmpty() && active.splash.hits.isEmpty(), "Cancelled admission stays cancelled"); return null; });
	}

	private void zeroPrice(ClientGameTestContext context, TestSingleplayerContext world, String art) {
		context.waitTicks(85);
		Probe p = on(world, player -> {
			Probe q = prepare(player, art); player.setShiftKeyDown(true);
			check(AuraGuard.raise(player), "Zero-price path still requires real perfect guard"); catchBlow(player, q);
			long now = player.level().getGameTime();
			player.setAttached(Awakening.AWAKENING, new Awakening.State(AwakeningRules.Phase.AWAKENED.ordinal(), now, now + 200, now + 800, now + 4400, 0, 0));
			player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
			var ability = AuraApi.artOf(player, art).orElseThrow();
			check(SwordStrings.price(player, ability) == 0 && SwordStrings.saw(player, ability), "Real awakening modifier makes the authentic counter free");
			float before = Aura.aura(player); q.accepted = now; q.effectiveRest = SwordStrings.rest(player, ability);
			SwordStrings.request(player, new SwordStrings.Perform(art, MARKS));
			q.ready = SwordStrings.readyAt(player, art);
			check(q.ready == now + q.effectiveRest, "Zero-price admission retains the exact ordinary effective rest");
			check(q.spends == 0 && Aura.aura(player) == before && MastersArts.committed(player) && q.ready > now,
				"Zero positive spends still commit one logical admission and original rest");
			check(dev.wildercord.aura.arts.ArtWards.hardened(player) == art.equals("unmoved"), "Only the paid-logical Unmoved grants immediate defense");
			SwordStrings.request(player, new SwordStrings.Perform(art, MARKS));
			check(q.spends == 0 && SwordStrings.readyAt(player, art) == q.ready, "A duplicate free request cannot refresh admission or rest");
			return q;
		});
		context.waitTicks(25);
		on(world, player -> { check(p.hooks == 1 && p.target.hits.size() == 1 && p.hookAt == p.accepted + MastersStyleRules.of(art).windup(),
			"Zero-price modifier preserves one offensive release and completion"); player.removeAttached(Awakening.AWAKENING); return null; });
	}

	private void landing(ClientGameTestContext context, TestSingleplayerContext world, String cause) {
		boolean[] before = {false}, after = {false}; Foe[] replacement = {null};
		Probe p = request(context, world, "unmoved", true, player -> {}, player -> {
			Probe q = active;
			Scheduler.later(7, () -> {
				q.target.setNoGravity(true); q.target.teleportTo(20.5, 115, .5); q.target.setDeltaMovement(Vec3.ZERO); q.target.setOnGround(false);
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player.setAttached(AuraAttachments.AURA, Aura.data(player).withMethod("hollow"));
				dev.wildercord.cast.Charging.interrupt(player);
				if (cause.equals("owner_world")) {
					var level = player.level();
					check(player.teleportTo(level.getServer().getLevel(net.minecraft.world.level.Level.NETHER), .5, 120, .5, Set.of(), 0, 0, false), "Released owner leaves world");
					check(player.teleportTo(level, FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "Owner round trip cannot revive released hold");
				} else if (cause.equals("target_replacement")) {
					var uuid = q.target.getUUID(); q.target.discard();
					var other = new Foe(player.level()); other.setUUID(uuid); other.setNoAi(true); other.snapTo(20.5, 115, .5, 0, 0);
					other.setNoGravity(true); check(player.level().addFreshEntity(other), "Replacement target is loaded"); fixtures.add(other); replacement[0] = other;
				} else if (cause.equals("team")) {
					var board = player.level().getScoreboard(); var team = board.getPlayerTeam("earned_counter_allies");
					if (team == null) team = board.addPlayerTeam("earned_counter_allies");
					board.addPlayerToTeam(player.getScoreboardName(), team); board.addPlayerToTeam(q.target.getScoreboardName(), team);
				}
			});
			Scheduler.later(45, () -> before[0] = !q.target.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS));
			Scheduler.later(47, () -> after[0] = q.target.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS));
		});
		context.waitTicks(48);
		on(world, player -> {
			check(p.target.hits.size() == 1 && before[0], "Unmoved does not hold an airborne thrown target before its 40-tick maximum");
			check(after[0] == cause.equals("released"), "Landing permission survives equipment/method/recovery, but cannot revive retired owner, replaced target or current ally");
			if (replacement[0] != null) check(!replacement[0].hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "Landing watcher never rebinds replacement UUID");
			return null;
		});
	}

	private void pulseCap(ClientGameTestContext context, TestSingleplayerContext world) {
		List<Foe> radial = new ArrayList<>();
		Probe p = request(context, world, "null_parry", true, player -> {
			active.splash.teleportTo(20.5, 100, .5);
			for (int i = 0; i < 6; i++) radial.add(foe(player.level(), 1.1 + i * .08, 2.2 + i * .05));
		}, player -> {});
		context.waitTicks(25);
		on(world, player -> {
			check(p.target.hits.size() == 1 && radial.stream().filter(foe -> !foe.hits.isEmpty()).count() == 5,
				"Null queries cap6 first, then excludes the primary without refilling its slot");
			check(radial.getLast().hits.isEmpty(), "The seventh-nearest body cannot fill the excluded primary's slot");
			check(p.order.getLast() == p.target, "All radial strikes precede the original primary");
			check(dev.wildercord.cast.Reactions.has(p.target, dev.wildercord.cast.Reactions.Mark.SHADOWED)
				&& dev.wildercord.aura.arts.ArtWards.silenced(p.target), "The live primary keeps shadow80 and ordinary silence60");
			return null;
		});
	}

	private void paidWardDeadline(ClientGameTestContext context, TestSingleplayerContext world) {
		boolean[] at80 = {false}, at81 = {true};
		Probe p = request(context, world, "unmoved", true, player -> {}, player -> {
			long accepted = active.accepted;
			Scheduler.later(80, () -> { check(player.level().getGameTime() == accepted + 80, "Exact paid-admission endpoint tick"); at80[0] = dev.wildercord.aura.arts.ArtWards.hardened(player); });
			Scheduler.later(81, () -> { check(player.level().getGameTime() == accepted + 81, "Exact first expired tick"); at81[0] = dev.wildercord.aura.arts.ArtWards.hardened(player); });
		});
		context.waitTicks(82);
		on(world, player -> {
			check(p.spends == 1 && p.hooks == 1 && p.target.hits.size() == 1, "One paid Unmoved admission reaches one offensive release");
			check(at80[0] && !at81[0], "The +6 offensive release cannot regrant or refresh the original inclusive +80 ward deadline");
			check(SwordStrings.readyAt(player, p.art) == p.accepted + p.effectiveRest, "The long-lived ward never changes individual rest");
			return null;
		});
	}

	private void catchBlow(ServerPlayer player, Probe p) {
		check(AuraGuard.perfectNow(player), "The real hit occurs inside the actual perfect guard window");
		float health = player.getHealth();
		p.target.doHurtTarget(player.level(), player);
		check(player.getHealth() == health && AuraGuard.caught(player) != null, "Actual hostile hit is perfectly caught");
		p.caught = AuraGuard.caught(player).damage();
		p.target.teleportTo(.5, 100, 2.1); p.target.setDeltaMovement(Vec3.ZERO);
		p.expected = ArtKit.weapon(player) * ArtKit.scale()
			* dev.wildercord.cast.AuraElements.bonus(player, p.target,
				player.level().damageSources().source(Aura.DAMAGE, player, player), Aura.element(player));
	}
	private Probe prepare(ServerPlayer player, String art) {
		active = null; MastersArts.cancel(player); SwordStrings.forget(player.getUUID()); AuraGuard.forget(player.getUUID());
		fixtures.forEach(Entity::discard); fixtures.clear();
		for (int y = 100; y <= 103; y++) player.level().setBlockAndUpdate(new BlockPos(0, y, 1), Blocks.AIR.defaultBlockState());
		var board = player.level().getScoreboard(); var team = board.getPlayerTeam("earned_counter_allies"); if (team != null) board.removePlayerTeam(team);
		player.setGameMode(GameType.SURVIVAL);
		player.teleportTo(player.level(), FEET.x, FEET.y, FEET.z, Set.<Relative>of(), 0, 8, false);
		player.setDeltaMovement(Vec3.ZERO); player.removeAllEffects(); Effects.readyToHurt(player); player.setHealth(10); player.getFoodData().setFoodLevel(20);
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(ArtRules.art(art).method(), AuraRules.EDGE, AuraRules.threshold(AuraRules.EDGE), AuraRules.capacity(AuraRules.EDGE), 0));
		player.removeAttached(AuraAttachments.STATE); player.removeAttached(SwordStrings.COOLDOWNS);
		player.removeAttached(Momentum.MOMENTUM); player.removeAttached(Awakening.AWAKENING);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD)); player.inventoryMenu.broadcastChanges();
		var p = new Probe(art); p.owner = player; p.target = foe(player.level(), .5, 2.1); p.splash = foe(player.level(), 1.1, 2.7); p.health = player.getHealth();
		active = p; return p;
	}
	private Foe foe(ServerLevel level, double x, double z) {
		var foe = new Foe(level); foe.setNoAi(true); foe.addTag("wildercord.rolled");
		foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); foe.setHealth(100); foe.snapTo(x, 100, z, 180, 0);
		level.addFreshEntity(foe); fixtures.add(foe); return foe;
	}
	private void verify(Probe p, ServerPlayer player, boolean hits) {
		CounterHitCapture.assertIdle();
		check(p.spends == 1 && p.hooks == (hits ? 1 : 0), "Exactly one payment and only a valid release completes");
		check(p.beforeFrame && p.exposed && p.recovery && p.ended, "Windup has no early harm and the exact fixed exposed recovery ends on time");
		check(SwordStrings.readyAt(player, p.art) == p.ready, "No delayed frame changes the single accepted art rest");
		if (hits) check(p.hookAt == p.accepted + MastersStyleRules.of(p.art).windup(), "Completion is at the exact server active frame");
	}
	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> action) {
		return world.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
