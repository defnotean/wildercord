package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.aura.world.MasterVictories;
import dev.wildercord.aura.world.MasterVictoryRules;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.cast.*;
import dev.wildercord.content.ScrollSpell;
import dev.wildercord.content.SpellScrollItem;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.RelayRules;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Combined production admissions; registered through WallTurnCommitmentTest, never a separate CI job. */
final class WallRelayChecks {
	private static boolean listening;
	private static ServerPlayer hooked;
	private static Predicate<ServerPlayer> before;
	private static Runnable spent;
	private static String spentReason = "master_form:wall_turn";
	private static Runnable after;
	private static Predicate<ServerPlayer> movementBlocker;
	private static Runnable afterServerTick;
	private long sequence = 100, nonce = 2_000_000;
	private static final class StaleBody extends net.fabricmc.fabric.api.entity.FakePlayer {
		StaleBody(ServerPlayer original) { super(original.level(), original.getGameProfile()); }
	}
	private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
	static void run(ClientGameTestContext c) { new WallRelayChecks().exercise(c); }
	private void exercise(ClientGameTestContext c) {
		listen();
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule fall_damage false");
			fresh(c, world);
			world.getServer().runOnServer(server -> {
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.WallTurnCommitmentTest#relay\",\"seed\":\"{}\"}", server.overworld().getSeed());
				var p = server.getPlayerList().getPlayers().getFirst();
				down(p); check(RelayCircles.pending(p), "A real paid Relay focus precedes the switch probes");
				float mana = Spellbooks.mana(p); long rest = relayRest(p), slotRest = Spellbooks.readyAt(p, 0);
				SpellCaster.cast(p, 99); Charging.request(p, 99, true);
				RelayCircles.input(p, RelayInputRules.DOWN, 99, ++nonce);
				check(RelayCircles.pending(p) && Spellbooks.mana(p) == mana && relayRest(p) == rest, "Malformed cast, charge and Relay packets leave paid focus and rest intact");
				rejectedScroll(p, List.of(), "empty");
				rejectedScroll(p, List.of(Runes.AMPLIFY.id()), "unattached_modifier");
				Spellbooks.setMana(p, 0); SpellCaster.cast(p, 1);
				check(RelayCircles.pending(p) && relayRest(p) == rest && Spellbooks.readyAt(p, 0) == slotRest, "Insufficient ordinary spell resources preserve the prior paid focus and both cooldowns");
				Spellbooks.setMana(p, mana);
				hooked = p; before = ignored -> { Spellbooks.setMana(p, 0); return true; };
				try { SpellCaster.cast(p, 1); } finally { before = null; hooked = null; }
				check(RelayCircles.pending(p) && relayRest(p) == rest && Spellbooks.readyAt(p, 1) == 0,
					"A validation callback that removes payment capacity cannot retire the focus or start an ordinary cooldown");
				Spellbooks.setMana(p, mana);
				p.setAttached(AuraAttachments.AURA, Aura.data(p).withAura(19));
				check(!press(p) && RelayCircles.pending(p) && Aura.aura(p) == 19, "Insufficient Aura refuses the switch without consuming the existing focus");
				p.setAttached(AuraAttachments.AURA, Aura.data(p).withAura(160));
				MasterForms.request(p, new MasterForms.Action(WallTurnRules.RELEASE, MasterForms.view(p).epoch(), ++sequence));
				RelayCircles.input(p, RelayInputRules.UP, 0, ++nonce);
			});
			c.waitTicks(1);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); airborne(p);
				long rest = relayRest(p), slotRest = Spellbooks.readyAt(p, 0); float mana = Spellbooks.mana(p);
				RelayCircles.input(p, RelayInputRules.DOWN, 1, ++nonce);
				check(RelayCircles.pending(p) && !RelayCircles.committed(p), "A wrong-slot request cannot erase the still-valid paid original focus");
				hooked = p; spent = () -> {
					check(ActionAdmission.busy(p) && RelayCircles.pending(p), "The old focus survives until the paid wall admission completes");
					RelayCircles.input(p, RelayInputRules.UP, 0, ++nonce); RelayCircles.input(p, RelayInputRules.DOWN, 0, ++nonce);
					SpellCaster.cast(p, 1); Charging.request(p, 1, true);
					check(!press(p) && !AuraStep.step(p) && !MastersArts.activate(p, 0), "Payment callbacks cannot re-enter a second movement or art admission");
				};
				long acceptedSequence = sequence + 1;
				try { check(press(p), "Accepted paid Wall Turn replaces an uncommitted focus"); } finally { spent = null; hooked = null; }
				long wallRest = MasterForms.data(p).readyAt();
				check(!RelayCircles.pending(p) && MasterForms.ownsMotion(p) && Aura.aura(p) == 140 && Spellbooks.mana(p) == mana,
					"The switch pays only 20 Aura once and never refunds or repays Relay");
				check(relayRest(p) == rest && Spellbooks.readyAt(p, 0) == slotRest && p.fallDistance >= 7, "Relay cooldowns and accumulated fall risk are unchanged by the accepted switch");
				check(!MasterForms.request(p, new MasterForms.Action(WallTurnRules.PRESS, MasterForms.view(p).epoch(), acceptedSequence)) && !press(p)
					&& MasterForms.data(p).readyAt() == wallRest && Aura.aura(p) == 140, "Same-tick replay and hold repeat cannot pay or switch twice");
			});

			fresh(c, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); float beforePlacement = Spellbooks.mana(p);
				down(p);
				check(RelayCircles.pending(p) && Spellbooks.mana(p) < beforePlacement, "Accepted SELF scroll fixture begins with a real paid Relay focus");
				float mana = Spellbooks.mana(p), aura = Aura.aura(p); long rest = relayRest(p), slotRest = Spellbooks.readyAt(p, 0);
				var cord = Spellbooks.cord(p); var mainHand = p.getMainHandItem(); var form = MasterForms.data(p);
				var definition = new ScrollSpell(List.of(Runes.SELF.id()), "Self without a payload", "Fixture");
				var compiled = SpellCompiler.compile(SpellScrollItem.runesOf(definition));
				check(!compiled.isEmpty() && compiled.root().groups.getFirst().effects.isEmpty(), "SELF without a payload is an admitted shape group, not an empty compilation");
				var scroll = new ItemStack(WildercordItems.SPELL_SCROLL); scroll.set(WildercordComponents.SCROLL, definition);
				scrollBoundary(p, scroll, "self", "before_offhand", compiled.isEmpty(), null);
				p.setItemInHand(InteractionHand.OFF_HAND, scroll);
				scrollBoundary(p, scroll, "self", "after_offhand", compiled.isEmpty(), null);
				check(RelayCircles.pending(p) && Spellbooks.cord(p) == cord && p.getMainHandItem() == mainHand,
					"Equipping the SELF scroll in the offhand preserves the original paid focus and held identities before use");
				var result = WildercordItems.SPELL_SCROLL.use(p.level(), p, InteractionHand.OFF_HAND);
				scrollBoundary(p, scroll, "self", "after_use", compiled.isEmpty(), result);
				check(result == InteractionResult.SUCCESS && !RelayCircles.pending(p) && scroll.getCount() == 0,
					"An actually admitted SELF-only scroll retires the uncommitted focus and consumes the scroll once");
				check(Spellbooks.mana(p) == mana && Aura.aura(p) == aura && relayRest(p) == rest && Spellbooks.readyAt(p, 0) == slotRest && MasterForms.data(p).equals(form),
					"Accepted scroll use preserves paid Relay cost/rest and does not alter the Master-form reservation");
			});

			fresh(c, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "A paid wall brace precedes rejected Relay requests");
				var wall = MasterForms.data(p); Spellbooks.setMana(p, 0); down(p);
				check(MasterForms.ownsMotion(p) && !RelayCircles.pending(p) && MasterForms.data(p).equals(wall), "An unfunded first Relay input preserves all paid Wall Turn state");
				RelayCircles.input(p, RelayInputRules.DOWN, -2, ++nonce);
				check(MasterForms.ownsMotion(p), "Malformed Relay slot cannot cancel physical motion");
				Spellbooks.setMana(p, 200); RelayCircles.input(p, RelayInputRules.UP, 0, ++nonce);
			});
			c.waitTicks(1);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); var wall = MasterForms.data(p); float aura = Aura.aura(p);
				hooked = p; before = ignored -> {
					check(!press(p), "Relay validation callback cannot re-enter Wall Turn");
					RelayCircles.input(p, RelayInputRules.DOWN, 0, ++nonce);
					check(MasterForms.ownsMotion(p), "Validation keeps the old physical owner until Relay acceptance"); return true;
				};
				after = () -> {
					float paidMana = Spellbooks.mana(p); long paidRest = relayRest(p);
					check(RelayCircles.pending(p) && !MasterForms.ownsMotion(p) && !press(p), "Paid Relay callback observes its receipt and cannot switch back to a wall");
					RelayCircles.input(p, RelayInputRules.UP, 0, ++nonce); RelayCircles.input(p, RelayInputRules.DOWN, 0, ++nonce);
					check(Spellbooks.mana(p) == paidMana && relayRest(p) == paidRest && !RelayCircles.committed(p), "Paid callbacks cannot replay payment or turn placement into release");
				};
				long acceptedNonce = nonce + 2;
				try { down(p); } finally { before = null; after = null; hooked = null; }
				check(RelayCircles.pending(p) && !MasterForms.ownsMotion(p) && MasterForms.data(p).equals(wall) && Aura.aura(p) == aura && p.fallDistance >= 7,
					"An accepted first Relay input retires only physical motion, preserving paid Aura rest, airborne use and fall risk");
				float mana = Spellbooks.mana(p); long rest = relayRest(p);
				RelayCircles.input(p, RelayInputRules.DOWN, 0, acceptedNonce); RelayCircles.input(p, RelayInputRules.DOWN, 0, ++nonce);
				check(Spellbooks.mana(p) == mana && relayRest(p) == rest && !RelayCircles.committed(p), "Repeated first input neither repays nor becomes a second input");
			});

			fresh(c, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "Veto fixture owns a paid brace");
				var wall = MasterForms.data(p); float mana = Spellbooks.mana(p);
				hooked = p; before = ignored -> false;
				try { down(p); } finally { before = null; hooked = null; }
				check(MasterForms.ownsMotion(p) && MasterForms.data(p).equals(wall) && Spellbooks.mana(p) == mana && relayRest(p) == 0,
					"BEFORE_CAST veto preserves motion and both resource domains");
			});

			fresh(c, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "Landing fixture braces"); MasterForms.cancel(p);
				p.teleportTo(.5, 150, .5); p.setOnGround(true); p.setDeltaMovement(Vec3.ZERO);
			});
			c.waitTicks(2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); check(MasterForms.data(p).recoveryUntil() > MasterForms.now(p), "Actual safe landing starts recovery before the spell input");
				long recovery = MasterForms.data(p).recoveryUntil(); down(p);
				check(RelayCircles.pending(p) && MasterForms.data(p).recoveryUntil() == recovery, "Ordinary Relay placement remains available during landing recovery");
				RelayCircles.input(p, RelayInputRules.UP, 0, ++nonce);
			});
			c.waitTicks(1);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); float mana = Spellbooks.mana(p); long rest = relayRest(p);
				RelayCircleTest.aim(p, new Vec3(.5, 151, 7)); down(p);
				check(RelayCircles.committed(p) && MasterForms.data(p).recoveryUntil() > MasterForms.now(p)
					&& Spellbooks.mana(p) == mana && relayRest(p) == rest, "The valid second fresh input commits during landing recovery without another payment");
			});

			fresh(c, world);
			world.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); down(p); RelayCircles.input(p, RelayInputRules.UP, 0, ++nonce); });
			c.waitTicks(1);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); RelayCircleTest.aim(p, new Vec3(.5, 151, 7)); down(p);
				airborne(p); check(RelayCircles.committed(p) && !MasterForms.able(p) && !press(p) && Aura.aura(p) == 160,
					"A committed Relay warning excludes an otherwise ready Wall Turn before payment");
				MasterForms.request(p, new MasterForms.Action(WallTurnRules.RELEASE, MasterForms.view(p).epoch(), ++sequence));
			});
			c.waitTicks(RelayRules.WARN_TICKS + 1);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); airborne(p);
				check(RelayCircles.recovering(p) && !press(p) && Aura.aura(p) == 160 && MasterForms.data(p).readyAt() == 0,
					"Relay recovery also excludes Wall Turn without creating or shortening a form cooldown");
			});

			fresh(c, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "Lifecycle callback fixture braces");
				long wallRest = MasterForms.data(p).readyAt(), epoch = MasterForms.view(p).epoch(); float mana = Spellbooks.mana(p); var origin = p.level();
				hooked = p; before = ignored -> {
					check(p.teleportTo(server.getLevel(Level.NETHER), .5, 152, .5, Set.of(), 0, 55, false), "Native callback dimension departure succeeds");
					check(p.teleportTo(origin, .5, 152, .5, Set.of(), 0, 55, false), "Native callback returns the same body immediately"); return true;
				};
				try { down(p); } finally { before = null; hooked = null; }
				check(!RelayCircles.pending(p) && !MasterForms.ownsMotion(p) && Spellbooks.mana(p) == mana && MasterForms.data(p).readyAt() == wallRest
					&& !MasterForms.request(p, new MasterForms.Action(WallTurnRules.PRESS, epoch, ++sequence)), "Lifecycle retirement prevents either stale admission from reviving; paid wall rest survives");
				var stale = new StaleBody(p);
				RelayCircles.input(stale, RelayInputRules.DOWN, 0, ++nonce);
				check(!RelayCircles.pending(stale) && !MasterForms.request(stale, new MasterForms.Action(WallTurnRules.PRESS, epoch, ++sequence))
					&& !ActionAdmission.busy(p), "An unregistered replacement body with the same UUID cannot admit either action or retain an admission lock");
			});

			fresh(c, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "Post-payment lifecycle fixture braces");
				long wallRest = MasterForms.data(p).readyAt(); float mana = Spellbooks.mana(p); var origin = p.level();
				long[] paidRest = {0}; float[] paidMana = {mana}; hooked = p;
				after = () -> {
					paidRest[0] = relayRest(p); paidMana[0] = Spellbooks.mana(p);
					check(paidRest[0] > RelayCircles.now(p) && paidMana[0] < mana, "The lifecycle callback runs only after real Relay payment and rest reservation");
					p.teleportTo(server.getLevel(Level.NETHER), .5, 152, .5, Set.of(), 0, 55, false);
					p.teleportTo(origin, .5, 152, .5, Set.of(), 0, 55, false);
				};
				try { down(p); } finally { after = null; hooked = null; }
				check(!RelayCircles.pending(p) && !MasterForms.ownsMotion(p) && relayRest(p) == paidRest[0] && Spellbooks.mana(p) == paidMana[0]
					&& MasterForms.data(p).readyAt() == wallRest && !ActionAdmission.busy(p), "Post-payment retirement preserves both costs/rests and cannot resurrect either owner or leak the admission guard");
			});
			fresh(c, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); float mana = Spellbooks.mana(p);
				hooked = p; spentReason = "step";
				spent = () -> { check(!press(p), "An existing Aura Step payment cannot admit a second wall owner"); down(p); };
				try { check(AuraStep.step(p), "The ordinary paid Aura Step still executes with its original admission"); }
				finally { hooked = null; spent = null; spentReason = "master_form:wall_turn"; }
				check(!MasterForms.ownsMotion(p) && MasterForms.data(p).readyAt() == 0 && !RelayCircles.pending(p) && Spellbooks.mana(p) == mana,
					"A legacy payment hook cannot create new form/Relay costs or movement ownership");
			});
			blockerCancellation(c, world);
		} finally { hooked = null; before = null; spent = null; after = null; movementBlocker = null; afterServerTick = null; spentReason = "master_form:wall_turn"; }
	}
	private static void rejectedScroll(ServerPlayer p, List<String> ids, String label) {
		float mana = Spellbooks.mana(p), aura = Aura.aura(p); long rest = relayRest(p), slotRest = Spellbooks.readyAt(p, 0);
		var cord = Spellbooks.cord(p); var mainHand = p.getMainHandItem(); var form = MasterForms.data(p);
		var definition = new ScrollSpell(ids, label, "Fixture");
		var compiled = SpellCompiler.compile(SpellScrollItem.runesOf(definition));
		check(compiled.isEmpty(), "The rejected scroll fixture must actually compile empty: " + label);
		var scroll = new ItemStack(WildercordItems.SPELL_SCROLL); scroll.set(WildercordComponents.SCROLL, definition);
		scrollBoundary(p, scroll, label, "before_offhand", compiled.isEmpty(), null);
		p.setItemInHand(InteractionHand.OFF_HAND, scroll);
		scrollBoundary(p, scroll, label, "after_offhand", compiled.isEmpty(), null);
		check(RelayCircles.pending(p) && Spellbooks.cord(p) == cord && p.getMainHandItem() == mainHand,
			"Offhand assignment preserves the original paid focus and held identities before rejected use: " + label);
		var result = WildercordItems.SPELL_SCROLL.use(p.level(), p, InteractionHand.OFF_HAND);
		scrollBoundary(p, scroll, label, "after_use", compiled.isEmpty(), result);
		check(result == InteractionResult.FAIL && RelayCircles.pending(p) && scroll.getCount() == 1,
			"A genuinely rejected scroll does not erase a paid focus or consume its item: " + label);
		check(Spellbooks.mana(p) == mana && Aura.aura(p) == aura && relayRest(p) == rest && Spellbooks.readyAt(p, 0) == slotRest && MasterForms.data(p).equals(form),
			"Rejected scroll use preserves both paid resource/commitment domains: " + label);
	}
	private static void scrollBoundary(ServerPlayer p, ItemStack scroll, String label, String phase, boolean empty, InteractionResult result) {
		String outcome = result == null ? "not_called" : result == InteractionResult.FAIL ? "fail" : result == InteractionResult.SUCCESS ? "success" : "other";
		dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_SCROLL {\"case\":\"{}\",\"phase\":\"{}\",\"compiledEmpty\":{},\"result\":\"{}\",\"relayPending\":{},\"scrollCount\":{},\"mana\":{},\"relayRest\":{},\"slotReady\":{}}",
			label, phase, empty, outcome, RelayCircles.pending(p), scroll.getCount(), Spellbooks.mana(p), relayRest(p), Spellbooks.readyAt(p, 0));
	}
	private void blockerCancellation(ClientGameTestContext c, TestSingleplayerContext world) {
		Vec3 untouched = new Vec3(.11, -.03, 0);
		fresh(c, world);
		world.getServer().runOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst(); down(p); float mana = Spellbooks.mana(p); long rest = relayRest(p);
			hooked = p; movementBlocker = owner -> { MasterFormMovement.begin(owner, 2); return false; };
			try { check(!press(p), "A registered predicate that installs another movement lease cannot admit Wall Turn afterward"); }
			finally { movementBlocker = null; hooked = null; }
			check(RelayCircles.pending(p) && Spellbooks.mana(p) == mana && relayRest(p) == rest && Aura.aura(p) == 160 && MasterForms.data(p).readyAt() == 0,
				"A competing movement owner established during preliminary admission refuses before cost and preserves the paid focus");
		});
		fresh(c, world);
		world.getServer().runOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst(); down(p);
			check(RelayCircles.pending(p), "Blocker admission fixture owns a real paid Relay focus");
			float mana = Spellbooks.mana(p); long rest = relayRest(p), slotRest = Spellbooks.readyAt(p, 0); int[] cancelled = {0};
			hooked = p; movementBlocker = owner -> {
				if (MasterForms.ownsMotion(owner)) { check(MasterForms.cancel(owner), "The registered predicate cancels the reserved paid brace"); cancelled[0]++; owner.setDeltaMovement(untouched); }
				return false;
			};
			try { check(!press(p), "Cancellation during the final validity predicate cannot report an accepted orphan brace"); }
			finally { movementBlocker = null; hooked = null; }
			check(cancelled[0] == 1 && !MasterForms.ownsMotion(p) && p.getDeltaMovement().equals(untouched), "No aborted brace zeros motion or regains ownership after the external predicate returns");
			check(RelayCircles.pending(p) && Spellbooks.mana(p) == mana && relayRest(p) == rest && Spellbooks.readyAt(p, 0) == slotRest,
				"Rejected wall admission preserves the prior paid focus and both Relay cooldowns");
			check(Aura.aura(p) == 140 && MasterForms.data(p).readyAt() == MasterForms.now(p) + WallTurnRules.REST_TICKS
				&& MasterForms.data(p).airborneUsed() && p.fallDistance >= 7, "The interrupted paid wall reservation keeps its one cost, rest, airborne use and fall risk");
		});
		c.waitTicks(1);
		world.getServer().runOnServer(server -> check(MasterForms.view(server.getPlayerList().getPlayers().getFirst()).phase() == WallTurnRules.ABORT,
			"The next server view publishes abort, never an orphan brace event"));

		fresh(c, world);
		world.getServer().runOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "Kick cancellation starts from a real paid brace");
			MasterForms.request(p, new MasterForms.Action(WallTurnRules.RELEASE, MasterForms.view(p).epoch(), ++sequence));
		});
		c.waitTicks(1);
		world.getServer().runOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst(); var paid = MasterForms.data(p); float aura = Aura.aura(p);
			hooked = p; movementBlocker = owner -> { MasterForms.cancel(owner); owner.setDeltaMovement(untouched); return false; };
			try { check(!press(p), "The fresh kick returns false after registered-blocker cancellation without dereferencing cleared active state"); }
			finally { movementBlocker = null; hooked = null; }
			check(!MasterForms.ownsMotion(p) && MasterForms.data(p).equals(paid) && Aura.aura(p) == aura && p.getDeltaMovement().equals(untouched)
				&& p.fallDistance >= 7, "Rejected kick neither moves the aborted object nor changes its paid reservation or fall risk");
		});

		fresh(c, world);
		Vec3[] afterTickMotion = {null};
		world.getServer().runOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "Tick cancellation starts from an accepted brace");
			hooked = p; movementBlocker = owner -> { if (MasterForms.ownsMotion(owner)) { MasterForms.cancel(owner); owner.setDeltaMovement(untouched); } return false; };
			afterServerTick = () -> { afterTickMotion[0] = p.getDeltaMovement(); afterServerTick = null; };
		});
		c.waitTicks(1);
		world.getServer().runOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst(); movementBlocker = null; hooked = null;
			check(!MasterForms.ownsMotion(p) && MasterForms.view(p).phase() == WallTurnRules.ABORT && untouched.equals(afterTickMotion[0])
				&& MasterForms.data(p).airborneUsed() && p.fallDistance >= 7, "The tick cannot advance, finish or publish a cancelled movement receipt");
		});
	}
	private void fresh(ClientGameTestContext c, TestSingleplayerContext world) {
		c.waitTicks(20);
		world.getServer().runOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst(); Charging.interrupt(p); p.removeAllEffects();
			RelayCircleTest.prepare(p, Runes.HARM);
			for (int y = 150; y <= 156; y++) for (int z = -3; z <= 3; z++) p.level().setBlockAndUpdate(new BlockPos(-1, y, z), Blocks.STONE.defaultBlockState());
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD)); p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, 160, 0));
			p.setAttached(MasterVictories.RECORD, MasterVictoryRules.Progress.NONE.withClear(MastersRules.GALE));
			p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(true, 1, 0, false, false));
			Spellbooks.set(p, Spellbooks.get(p).learn(Runes.SELF.id()).learn(Runes.HEAL.id()).withSpell(1, List.of(Runes.SELF.id(), Runes.HEAL.id())));
			airborne(p);
			MasterForms.request(p, new MasterForms.Action(WallTurnRules.RELEASE, MasterForms.view(p).epoch(), ++sequence));
			RelayCircles.input(p, RelayInputRules.UP, 0, ++nonce);
		});
		c.waitTicks(1);
		world.getServer().runOnServer(server -> airborne(server.getPlayerList().getPlayers().getFirst()));
	}
	private static void airborne(ServerPlayer p) { p.teleportTo(.5, 152, .5); p.setOnGround(false); p.setDeltaMovement(Vec3.ZERO); p.setXRot(55); p.setYRot(0); p.fallDistance = 7; }
	private boolean press(ServerPlayer p) { return MasterForms.request(p, new MasterForms.Action(WallTurnRules.PRESS, MasterForms.view(p).epoch(), ++sequence)); }
	private void down(ServerPlayer p) { RelayCircles.input(p, RelayInputRules.UP, 0, ++nonce); RelayCircles.input(p, RelayInputRules.DOWN, 0, ++nonce); }
	private static long relayRest(ServerPlayer p) { return p.getAttachedOrElse(RelayState.REST, 0L); }
	private static void listen() {
		if (listening) return; listening = true;
		WildercordEvents.BEFORE_CAST.register((p, slot, runes, cost) -> p != hooked || before == null || before.test(p));
		WildercordEvents.AFTER_CAST.register((p, slot, runes, cost) -> { if (p == hooked && after != null) after.run(); });
		AuraApi.onSpend((p, paid, reason, backlash) -> { if (p == hooked && spent != null && reason.equals(spentReason)) spent.run(); });
		MasterFormMovement.registerBlocker(p -> p == hooked && movementBlocker != null && movementBlocker.test(p));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (afterServerTick != null && hooked != null && hooked.level().getServer() == server) afterServerTick.run();
		});
	}
}
