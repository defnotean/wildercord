package dev.wildercord.gametest;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraDominion;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.Awakening;
import dev.wildercord.aura.AwakeningRules;
import dev.wildercord.aura.Momentum;
import dev.wildercord.aura.MomentumRules;
import dev.wildercord.aura.Stance;
import dev.wildercord.aura.StanceRules;
import dev.wildercord.aura.StringRules;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.ArtWards;
import dev.wildercord.aura.arts.EmberArts;
import dev.wildercord.aura.arts.RimeArts;
import dev.wildercord.cast.Reactions;
import dev.wildercord.client.AuraBanners;
import dev.wildercord.client.AuraFxClient;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.client.AwakeningHud;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Awakening, step 6 of the aura overhaul, played with the real keys on a stone platform in the sky:
 * <ul>
 *   <li><b>The key</b>: the Aura key tapped, then held, awakens a swordsman with a full pool and momentum enough (its charge filling
 *       round the crosshair as it's held); without them it's refused with why (and the lone tap went as the slash it was); a quick
 *       second press is still the step's double tap; a held second press let go early sends nothing; a lone hold is still Dominion;</li>
 *   <li><b>What it gives</b>: coated blows a little harder (a creature's), arts free on both sides, momentum held at its peak through a
 *       hit, faster on foot and with the blade, the Final Art open and free (and still resting its half a minute);</li>
 *   <li><b>The moment</b>: filmed gathering, bursting, risen and burning, from behind, the side and the front, by night and by day;
 *       through the swordsman's own eyes only the edges glow, once;</li>
 *   <li>each stage's awakened form, and each method's flourish;</li>
 *   <li><b>Finishers feed it</b> a second each; <b>it ends</b>: what aura is left burns away, the swordsman is spent (slowed, gathering
 *       nothing, momentum empty, the slow coming back after milk), recovers, and rests until the next; a death ends it (the new
 *       body not spent, the rest still running);</li>
 *   <li><b>A duel</b>: half the damage bonus against a player, an awakened rival drawn on the swordsman's own client;</li>
 *   <li><b>The Sovereign's awakened Dominion</b>, each method's: wider, named, and doing its method's thing to the husks inside;</li>
 *   <li>the HUD's mark at each phase and the Aura page.</li>
 * </ul>
 * Screenshots: {@code awakening_*} and {@code dominion_*}.
 *
 * <p>{@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it; {@code WILDERCORD_AWAKENING=a,b}
 * plays only the scenes named (input, gives, moment, forms, methods, fed, final, spent, death, duel, dominions, pages).</p>
 */
public class WildercordAwakeningTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 190, 0);
	private static final String TAG = "wildercord.awakening_test";
	private static final List<String> AWAKENED = Collections.synchronizedList(new ArrayList<>());
	private static final List<String> ENDED = Collections.synchronizedList(new ArrayList<>());
	private static final List<String> PERFORMED = Collections.synchronizedList(new ArrayList<>());
	private static boolean hooked;
	/** A sword's full swing comes back in 11 ticks (a little sooner awakened). */
	private static final int FULL = 13;
	private static final List<String> METHODS = List.of("ember", "rime", "thunder", "gale", "stone", "verdant", "hollow", "starlit", "hourglass",
		"crimson");

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		TestMethods.plain();
		if (!hooked) {
			hooked = true;
			AuraApi.onAwakening(new AuraApi.AwakeningHook() {
				@Override
				public void awakened(ServerPlayer player, int ticks) {
					AWAKENED.add(Aura.data(player).method() + ":" + ticks);
				}

				@Override
				public void ended(ServerPlayer player) {
					ENDED.add(Aura.data(player).method());
				}
			});
			AuraApi.onString((player, art, ctx) -> PERFORMED.add(art.id()));
		}
		String only = System.getenv("WILDERCORD_AWAKENING");
		Set<String> scenes = only == null || only.isBlank() ? null : Set.of(only.split(","));
		context.runOnClient(mc -> {
			mc.getWindow().setWindowed(1920, 1080);
			mc.options.guiScale().set(2);
			mc.resizeGui();
			mc.options.toggleCrouch().set(false);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			MagicQuality.stringIndicator = MagicQuality.StringIndicator.CROSSHAIR;
		});
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule natural_regeneration false");
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("time set 3000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			scene(scenes, "input", failures, "the key awakens and refuses", () -> input(context, world));
			scene(scenes, "gives", failures, "what it gives", () -> gives(context, world));
			scene(scenes, "moment", failures, "the moment", () -> moment(context, world));
			scene(scenes, "forms", failures, "each stage's awakened form", () -> forms(context, world));
			if (scenes == null || scenes.contains("methods")) {
				for (String method : METHODS) {
					run(failures, method + "'s awakening", () -> method(context, world, method));
				}
				run(failures, "a plain method's awakening", () -> method(context, world, TestMethods.PLAIN));
			}
			scene(scenes, "fed", failures, "finishers feed it", () -> fed(context, world));
			scene(scenes, "final", failures, "the Final Art while awakened", () -> finalArt(context, world));
			scene(scenes, "spent", failures, "it ends, the swordsman is spent, recovers and rests", () -> spent(context, world));
			scene(scenes, "death", failures, "a death ends it", () -> death(context, world));
			scene(scenes, "duel", failures, "a duel", () -> duel(context, world));
			if (scenes == null || scenes.contains("dominions")) {
				for (String method : METHODS) {
					run(failures, method + "'s awakened Dominion", () -> dominion(context, world, method));
				}
				run(failures, "a plain method's awakened Dominion", () -> dominion(context, world, TestMethods.PLAIN));
			}
			scene(scenes, "pages", failures, "the HUD and the Aura page", () -> pages(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("Awakening went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.getInput().releaseKey(o -> o.keyShift);
			context.getInput().releaseKey(WildercordKeys.auraMapping());
		}
	}

	private static void scene(Set<String> scenes, String id, List<String> failures, String what, Runnable test) {
		if (scenes == null || scenes.contains(id)) {
			run(failures, what, test);
		}
	}

	private static void run(List<String> failures, String what, Runnable test) {
		try {
			test.run();
		} catch (AssertionError e) {
			failures.add(what + ": " + e.getMessage());
		}
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	// ------------------------------------------------------------------ the key

	private static void input(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE);
			setMomentumNow(player, 0);
			steady(spawn(player.level(), EntityTypes.HUSK, at(0, 4), 200));
			return null;
		});
		context.waitTicks(20);
		// Not ready (no momentum): the lone tap goes at once as the slash, and the held press is refused with why.
		String why = context.computeOnClient(mc -> String.valueOf(Awakening.refusal(mc.player)));
		check(why.equals("MOMENTUM"), "the swordsman's own client should know it waits on momentum (" + why + ")");
		long before = on(world, player -> player.level().getGameTime());
		tapHold(context, AwakeningRules.HOLD_TICKS + 3);
		context.waitTicks(4);
		String refused = on(world, player -> {
			if (Awakening.awakened(player)) {
				return "without momentum it shouldn't awaken";
			}
			return Aura.state(player).slashReadyAt() > before ? null : "not ready, the lone tap should have gone at once (the slash)";
		});
		check(refused == null, refused);
		// Ready: a full pool and momentum enough.
		on(world, player -> {
			player.removeAttached(AuraAttachments.STATE);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(AuraRules.capacity(AuraRules.EDGE)));
			setMomentumNow(player, 60);
			return null;
		});
		context.waitTicks(10);
		String ready = context.computeOnClient(mc -> String.valueOf(Awakening.refusal(mc.player)));
		check(ready.equals("null"), "with a full pool and momentum it should be ready on the client (" + ready + ")");
		shot(context, "awakening_hud_ready");
		long slashBefore = on(world, player -> Aura.state(player).slashReadyAt());
		int charges = context.computeOnClient(mc -> AwakeningHud.counts()[0]);
		// The tap, then the press held: its charge fills round the crosshair...
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(2);
		context.getInput().holdKey(WildercordKeys.auraMapping());
		context.waitTicks(10);
		shot(context, "awakening_charge_fp");
		check(context.computeOnClient(mc -> AwakeningHud.counts()[0]) > charges, "the charge should show round the crosshair while it's held");
		check(!on(world, Awakening::awakened), "not yet: the hold hasn't completed");
		// ...and completing, it awakens.
		context.waitTicks(AwakeningRules.HOLD_TICKS - 8);
		context.getInput().releaseKey(WildercordKeys.auraMapping());
		context.waitTicks(3);
		String woke = on(world, player -> {
			if (!Awakening.awakened(player)) {
				return "a tap then a held press should awaken (" + Awakening.refusal(player) + ")";
			}
			if (Aura.state(player).slashReadyAt() != slashBefore) {
				return "the waiting tap should go with the awakening, never as a slash first";
			}
			long left = Awakening.left(player);
			return left > AwakeningRules.ticks(AuraRules.EDGE, 1) - 10 && left <= AwakeningRules.ticks(AuraRules.EDGE, 1) ? null
				: "an Edge awakening should last twelve seconds (" + left + " left)";
		});
		check(woke == null, woke);
		check(context.computeOnClient(mc -> Awakening.awakened(mc.player)), "the swordsman's own client should know they're awakened");
		check(AWAKENED.stream().anyMatch(s -> s.startsWith("ember:")), "the awakening hooks should hear of it (" + AWAKENED + ")");
		String grimoire = on(world, player -> dev.wildercord.player.Heart.grimoire(player).contains("aura:awakening") ? null
			: "the first awakening should go into the Grimoire");
		check(grimoire == null, grimoire);
		calm(context, world);

		// Form, ready: a quick double tap is still the step.
		reset(context, world);
		on(world, player -> {
			setAura(player, "gale", AuraRules.FORM);
			setMomentumNow(player, 60);
			return null;
		});
		context.waitTicks(10);
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(3);
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(3);
		String stepped = on(world, player -> {
			if (Awakening.awakened(player)) {
				return "a quick double tap shouldn't awaken";
			}
			return AuraPresence.timers(player).stepReadyAt() > player.level().getGameTime() ? null : "a quick double tap should still step";
		});
		check(stepped == null, stepped);
		// A held second press let go before it completes: nothing at all (no slash, no step, no awakening).
		on(world, player -> {
			stand(player);
			player.setAttached(AuraPresence.TIMERS, AuraPresence.Timers.NONE);
			player.removeAttached(AuraAttachments.STATE);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(AuraRules.capacity(AuraRules.FORM)));
			return null;
		});
		context.waitTicks(20);
		tapHold(context, 9);
		context.waitTicks(15);
		String nothing = on(world, player -> {
			if (Awakening.awakened(player)) {
				return "let go early, it shouldn't awaken";
			}
			if (AuraPresence.timers(player).stepReadyAt() > 0) {
				return "let go early, it shouldn't step";
			}
			return Aura.state(player).slashReadyAt() <= 0 ? null : "let go early, the tap shouldn't go as a slash";
		});
		check(nothing == null, nothing);
		// Sovereign: a lone hold is still Dominion (an ordinary one, not awakened).
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			setMomentumNow(player, 60);
			return null;
		});
		context.waitTicks(20);
		context.getInput().holdKey(WildercordKeys.auraMapping());
		context.waitTicks(8);
		context.getInput().releaseKey(WildercordKeys.auraMapping());
		context.waitTicks(3);
		String dominion = on(world, player -> {
			if (Awakening.awakened(player)) {
				return "a lone hold shouldn't awaken";
			}
			if (!AuraDominion.active(player)) {
				return "a lone hold should still raise a Dominion";
			}
			return !AuraDominion.sovereign(player) && Math.abs(AuraDominion.radius(player) - AuraRules.DOMINION_RADIUS) < 1.0E-6 ? null
				: "raised not awakened, it's an ordinary Dominion (" + AuraDominion.radius(player) + ")";
		});
		check(dominion == null, dominion);
		on(world, player -> {
			AuraDominion.end(player);
			return null;
		});
		context.waitTicks(20);
	}

	// ------------------------------------------------------------------ what it gives

	private static void gives(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE);
			setMomentumNow(player, 60);
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.4), 400);
			husk.getAttribute(Attributes.ARMOR).setBaseValue(0);
			steady(husk);
			return null;
		});
		context.waitTicks(FULL + 5);
		double[] speeds = on(world, player -> new double[] {player.getAttributeValue(Attributes.MOVEMENT_SPEED), player.getAttributeValue(Attributes.ATTACK_SPEED)});
		float plain = blow(world);
		on(world, player -> {
			check(AuraApi.awaken(player), "it should awaken (" + Awakening.refusal(player) + ")");
			return null;
		});
		context.waitTicks(AwakeningRules.BURST_AT + 4);
		regroup(context, world);
		context.waitTicks(FULL);
		float awake = blow(world);
		double ratio = awake / Math.max(0.01, plain);
		check(ratio > 1.11 && ratio < 1.19, "a coated blow should land about 15% harder awakened (" + plain + " then " + awake + ")");
		// Faster, on foot and with the blade.
		double[] faster = on(world, player -> new double[] {player.getAttributeValue(Attributes.MOVEMENT_SPEED), player.getAttributeValue(Attributes.ATTACK_SPEED)});
		check(Math.abs(faster[0] / speeds[0] - 1.1) < 0.02, "a tenth faster on foot (" + speeds[0] + " to " + faster[0] + ")");
		check(Math.abs(faster[1] / speeds[1] - 1.1) < 0.02, "a tenth faster with the blade (" + speeds[1] + " to " + faster[1] + ")");
		// Arts cost nothing, on both sides alike.
		double serverPrice = on(world, player -> SwordStrings.price(player, AuraApi.string(EmberArts.KINDLING_DRAW).orElseThrow()));
		double clientPrice = context.computeOnClient(mc -> SwordStrings.price(mc.player, AuraApi.string(EmberArts.KINDLING_DRAW).orElseThrow()));
		check(serverPrice == 0 && clientPrice == 0, "arts should cost nothing awakened (" + serverPrice + " on the server, " + clientPrice + " on the client)");
		String paid = on(world, player -> {
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(30));
			AuraApi.StringArt art = AuraApi.string(EmberArts.KINDLING_DRAW).orElseThrow();
			boolean went = SwordStrings.perform(player, art, marks(art));
			float left = Aura.aura(player);
			return went && Math.abs(left - 30) < 1.0E-3 ? null : "an art played awakened should spend nothing (" + went + ", " + left + " left of 30)";
		});
		check(paid == null, paid);
		var firstTiming = dev.wildercord.aura.MastersStyleRules.of(EmberArts.KINDLING_DRAW);
		context.waitTicks(firstTiming.windup() + firstTiming.recovery());
		// Momentum holds at its peak, even through a hit taken.
		String held = on(world, player -> {
			double before = Momentum.value(player);
			Mob husk = foes(player).getFirst();
			husk.doHurtTarget(player.level(), player);
			double after = Momentum.value(player);
			player.setHealth(player.getMaxHealth());
			return MomentumRules.peak(before) && MomentumRules.peak(after) ? null : "momentum should hold at the peak, through a hit too (" + before + ", " + after + ")";
		});
		check(held == null, held);
		check(context.computeOnClient(mc -> MomentumRules.peak(Momentum.value(mc.player))), "the swordsman's own client should read the peak too");
		calm(context, world);
		double[] back = on(world, player -> new double[] {player.getAttributeValue(Attributes.MOVEMENT_SPEED), player.getAttributeValue(Attributes.ATTACK_SPEED)});
		check(Math.abs(back[0] - speeds[0]) < 1.0E-6 && Math.abs(back[1] - speeds[1]) < 1.0E-6, "the speed goes when it ends");
	}

	/** A full swing of the swordsman's on the scene's husk, landed on the server: what it took. */
	private static float blow(TestSingleplayerContext world) {
		return on(world, player -> {
			Mob husk = foes(player).getFirst();
			husk.setHealth(husk.getMaxHealth());
			dev.wildercord.cast.Effects.readyToHurt(husk);
			float hp = husk.getHealth();
			player.attack(husk);
			return hp - husk.getHealth();
		});
	}

	// ------------------------------------------------------------------ the moment

	private static void moment(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.FORM);
			setMomentumNow(player, 60);
			spawn(player.level(), EntityTypes.HUSK, at(2.2, 1.6), 200);
			spawn(player.level(), EntityTypes.HUSK, at(-2.0, 1.2), 200);
			return null;
		});
		thirdPerson(context, world, -40, 14, 6.0, false);
		context.waitTicks(10);
		int[] before = context.computeOnClient(mc -> AuraFxClient.counts());
		on(world, player -> {
			check(AuraApi.awaken(player), "it should awaken");
			return null;
		});
		context.waitTicks(3);
		shot(context, "awakening_gather_tp");
		context.waitTicks(AwakeningRules.BURST_AT - 2);
		shot(context, "awakening_burst_tp");
		String pushed = on(world, player -> {
			for (Mob husk : foes(player)) {
				if (husk.distanceTo(player) < 2.4) {
					return "foes close by should be thrown back a step (" + husk.distanceTo(player) + ")";
				}
			}
			return null;
		});
		check(pushed == null, pushed);
		context.waitTicks(8);
		shot(context, "awakening_rise_tp");
		context.waitTicks(25);
		shot(context, "awakening_burning_tp");
		int[] after = context.computeOnClient(mc -> AuraFxClient.counts());
		check(after[3] > before[3] && after[4] > before[4], "the burst and the flare should reach the client");
		float form = context.computeOnClient(mc -> AuraFxClient.awakenedForm(mc.player, mc.level.getGameTime()));
		check(form > 0.9F, "the body's aura should burn in its awakened form (" + form + ")");
		String banner = context.computeOnClient(mc -> AuraBanners.ownShowing());
		check(banner.isEmpty() || banner.equals("Awakening"), "its banner should name it (" + banner + ")");
		// From the front: the eyes.
		front(context, world, 4.0);
		context.waitTicks(3);
		shot(context, "awakening_face_tp");
		calm(context, world);
		context.waitTicks(40);
		// Through the swordsman's own eyes: only the edges glow, once; the burst is a whisper low in the view.
		firstPerson(context, world);
		context.waitTicks(10);
		int glowed = context.computeOnClient(mc -> AwakeningHud.counts()[1]);
		int whispers = context.computeOnClient(mc -> AuraFxClient.counts()[6]);
		on(world, player -> {
			setMomentumNow(player, 60);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(AuraRules.capacity(AuraRules.FORM)));
			check(AuraApi.awaken(player), "it should awaken again (" + Awakening.refusal(player) + ")");
			return null;
		});
		context.waitTicks(AwakeningRules.BURST_AT + 2);
		shot(context, "awakening_burst_fp");
		check(context.computeOnClient(mc -> AwakeningHud.counts()[1]) > glowed, "in first person the edges should glow as it bursts");
		check(context.computeOnClient(mc -> AuraFxClient.counts()[6]) > whispers, "its burst should be a whisper through the swordsman's own eyes");
		context.waitTicks(40);
		int settled = context.computeOnClient(mc -> AwakeningHud.counts()[1]);
		context.waitTicks(10);
		check(context.computeOnClient(mc -> AwakeningHud.counts()[1]) == settled, "the edge glow should be gone within two seconds");
		shot(context, "awakening_burning_fp");
		calm(context, world);
		// By day, from the side.
		world.getServer().runCommand("time set 6000");
		context.waitTicks(40);
		on(world, player -> {
			setMomentumNow(player, 60);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(AuraRules.capacity(AuraRules.FORM)));
			player.setAttached(Awakening.AWAKENING, Awakening.State.NONE);
			return null;
		});
		thirdPerson(context, world, -60, 12, 6.0, false);
		context.waitTicks(5);
		on(world, player -> {
			check(AuraApi.awaken(player), "it should awaken by day");
			return null;
		});
		context.waitTicks(AwakeningRules.BURST_AT + 2);
		shot(context, "awakening_burst_day_tp");
		context.waitTicks(30);
		shot(context, "awakening_burning_day_tp");
		calm(context, world);
		world.getServer().runCommand("time set 3000");
	}

	// ------------------------------------------------------------------ each stage's form, each method's flourish

	private static void forms(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		int[] stages = {AuraRules.EDGE, AuraRules.FORM, AuraRules.SOVEREIGN};
		for (int stage : stages) {
			reset(context, world);
			on(world, player -> {
				setAura(player, "rime", stage);
				setMomentumNow(player, 60);
				return null;
			});
			front(context, world, 3.6);
			context.waitTicks(8);
			if (stage == AuraRules.EDGE) {
				// For comparison: Edge in a fight, not awakened.
				on(world, player -> {
					AuraPresence.Look look = AuraPresence.look(player);
					player.setAttached(AuraPresence.LOOK, look.fightUntil(player.level().getGameTime() + 400));
					return null;
				});
				context.waitTicks(4);
				shot(context, "awakening_form_edge_before");
			}
			on(world, player -> {
				check(AuraApi.awaken(player), "it should awaken at " + stage);
				return null;
			});
			context.waitTicks(AwakeningRules.RISE + 20);
			shot(context, "awakening_form_" + dev.wildercord.aura.AuraStages.id(stage));
			calm(context, world);
			context.waitTicks(30);
		}
		world.getServer().runCommand("time set 3000");
	}

	private static void method(ClientGameTestContext context, TestSingleplayerContext world, String method) {
		world.getServer().runCommand("time set 18000");
		reset(context, world);
		on(world, player -> {
			setAura(player, method, AuraRules.FORM);
			setMomentumNow(player, 60);
			return null;
		});
		thirdPerson(context, world, -35, 18, 6.5, false);
		context.waitTicks(8);
		on(world, player -> {
			check(AuraApi.awaken(player), method + " should awaken (" + Awakening.refusal(player) + ")");
			return null;
		});
		context.waitTicks(AwakeningRules.BURST_AT + 7);
		String name = method.contains(":") ? "plain" : method;
		shot(context, "awakening_" + name + "_tp");
		context.waitTicks(30);
		check(on(world, Awakening::awakened), method + " should still be awakened");
		calm(context, world);
		context.waitTicks(40);
		world.getServer().runCommand("time set 3000");
	}

	// ------------------------------------------------------------------ finishers feed it

	private static void fed(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.FORM);
			setMomentumNow(player, 60);
			check(AuraApi.awaken(player), "it should awaken");
			return null;
		});
		context.waitTicks(AwakeningRules.RISE);
		long[] before = on(world, player -> {
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.4), 80);
			husk.setHealth(60);
			Stance.wear(player, husk, StanceRules.pool(StanceRules.Kind.CREATURE, 80, 0), StanceRules.Source.BLOW);
			Awakening.State s = Awakening.state(player);
			return new long[] {s.until(), s.extended(), Stance.finishers()};
		});
		context.waitTicks(FULL);
		swing(context);
		context.waitTicks(4);
		String fed = on(world, player -> {
			Awakening.State s = Awakening.state(player);
			if (Stance.finishers() != before[2] + 1) {
				return "the full swing should have been a finisher";
			}
			return s.until() == before[0] + AwakeningRules.FINISHER_EXTEND && s.extended() == before[1] + AwakeningRules.FINISHER_EXTEND ? null
				: "a finisher should feed it a second (" + (s.until() - before[0]) + ")";
		});
		check(fed == null, fed);
		// Fed to its limit, it takes no more.
		String limit = on(world, player -> {
			Awakening.State s = Awakening.state(player);
			player.setAttached(Awakening.AWAKENING, new Awakening.State(s.phase(), s.since(), s.until(), s.spentUntil(), s.readyAt(), s.price(),
				AwakeningRules.MAX_EXTEND));
			Mob husk = foes(player).getFirst();
			husk.setHealth(60);
			husk.removeAttached(Stance.STANCE);
			Stance.wear(player, husk, StanceRules.pool(StanceRules.Kind.CREATURE, 80, 0), StanceRules.Source.BLOW);
			return null;
		});
		context.waitTicks(FULL);
		long until = on(world, player -> Awakening.state(player).until());
		swing(context);
		context.waitTicks(4);
		check(on(world, player -> Awakening.state(player).until()) == until, "fed to its limit, a finisher adds nothing more");
		calm(context, world);
	}

	// ------------------------------------------------------------------ the Final Art

	private static void finalArt(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			setMomentumNow(player, 60);
			steady(spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 400));
			return null;
		});
		context.waitTicks(20);
		// Below the peak (60) the Final Art waits; awakened, it opens.
		check(!context.computeOnClient(mc -> AuraApi.FINAL_GATE.met(mc.player)), "at 60 momentum the Final Art should wait");
		on(world, player -> {
			check(AuraApi.awaken(player), "it should awaken");
			return null;
		});
		context.waitTicks(AwakeningRules.BURST_AT + 4);
		regroup(context, world);
		context.waitTicks(FULL);
		check(context.computeOnClient(mc -> AuraApi.FINAL_GATE.met(mc.player)), "awakened, the Final Art should open on the client");
		double price = on(world, player -> SwordStrings.price(player, AuraApi.string(EmberArts.SUNFALL).orElseThrow()));
		check(price == 0, "awakened, the Final Art should cost nothing (" + price + ")");
		PERFORMED.clear();
		finalSwings(context, world);
		context.waitTicks(6);
		context.getInput().releaseKey(o -> o.keyShift);
		check(PERFORMED.contains(EmberArts.SUNFALL), "the Final Art's string should play the Final Art awakened (" + PERFORMED + ")");
		String after = on(world, player -> {
			double momentum = Momentum.value(player);
			if (!MomentumRules.peak(momentum)) {
				return "its release spends some, but awakened momentum should hold at the peak (" + momentum + ")";
			}
			long rest = SwordStrings.readyAt(player, EmberArts.SUNFALL) - player.level().getGameTime();
			return rest > Awakening.left(player) ? null : "it should still rest longer than the awakening has left (" + rest + ")";
		});
		check(after == null, after);
		context.waitTicks(30);
		calm(context, world);
	}

	// ------------------------------------------------------------------ the end, spent, recovered, resting

	private static void spent(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE);
			setMomentumNow(player, 60);
			check(AuraApi.awaken(player), "it should awaken");
			return null;
		});
		context.waitTicks(AwakeningRules.RISE);
		// Its end, brought near: the last two seconds gutter.
		on(world, player -> {
			Awakening.State s = Awakening.state(player);
			long now = player.level().getGameTime();
			player.setAttached(Awakening.AWAKENING, new Awakening.State(s.phase(), s.since(), now + 30, s.spentUntil(), now + 30 + 3600, s.price(), 0));
			return null;
		});
		front(context, world, 3.6);
		context.waitTicks(12);
		shot(context, "awakening_gutter_tp");
		int ended = ENDED.size();
		context.waitTicks(22);
		String spent = on(world, player -> {
			if (!Awakening.spent(player)) {
				return "it should end in the spent state (" + Awakening.state(player) + ")";
			}
			if (Aura.aura(player) > 0) {
				return "what aura was left should burn away (" + Aura.aura(player) + ")";
			}
			if (!player.hasEffect(MobEffects.SLOWNESS)) {
				return "spent, the swordsman should be slowed";
			}
			if (Momentum.value(player) > 0) {
				return "spent, momentum should be empty";
			}
			if (AuraApi.gain(player, 10, "test") > 0) {
				return "spent, no aura should come in";
			}
			if (Aura.giveBack(player, 10) > 0) {
				return "spent, not even aura given back";
			}
			return null;
		});
		check(spent == null, spent);
		check(ENDED.size() > ended, "the awakening hooks should hear it end");
		check(context.computeOnClient(mc -> Awakening.spent(mc.player)), "the swordsman's own client should know they're spent");
		check("SPENT".equals(context.computeOnClient(mc -> String.valueOf(Awakening.refusal(mc.player)))), "spent, it should refuse to awaken");
		// A coated blow gives nothing back either.
		String blow = on(world, player -> {
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.4), 200);
			steady(husk);
			dev.wildercord.cast.Effects.readyToHurt(husk);
			player.attack(husk);
			return Aura.aura(player) <= 0 ? null : "spent, a blow should gather no aura (" + Aura.aura(player) + ")";
		});
		check(blow == null, blow);
		context.waitTicks(10);
		shot(context, "awakening_spent_tp");
		firstPerson(context, world);
		context.waitTicks(3);
		shot(context, "awakening_spent_hud");
		// Milk doesn't wash a spent body clean: the slow comes back.
		on(world, player -> {
			player.removeEffect(MobEffects.SLOWNESS);
			return null;
		});
		context.waitTicks(25);
		check(on(world, player -> player.hasEffect(MobEffects.SLOWNESS)), "the slow should come back while spent");
		// Recovered: aura comes again; resting until the next.
		on(world, player -> {
			Awakening.State s = Awakening.state(player);
			long now = player.level().getGameTime();
			player.setAttached(Awakening.AWAKENING, new Awakening.State(s.phase(), s.since(), s.until(), now + 2, s.readyAt(), s.price(), s.extended()));
			return null;
		});
		context.waitTicks(6);
		String recovered = on(world, player -> {
			if (Awakening.spent(player)) {
				return "it should let go in time";
			}
			if (Awakening.state(player).stage() != AwakeningRules.Phase.RESTING) {
				return "after it, it should rest (" + Awakening.state(player).stage() + ")";
			}
			if (AuraApi.gain(player, 5, "test") <= 0) {
				return "recovered, aura should come in again";
			}
			return Awakening.refusal(player) == AwakeningRules.Refusal.RESTING ? null : "resting, it should refuse (" + Awakening.refusal(player) + ")";
		});
		check(recovered == null, recovered);
		player(world, p -> p.removeEffect(MobEffects.SLOWNESS));
		context.waitTicks(3);
		shot(context, "awakening_resting_hud");
		calm(context, world);
		world.getServer().runCommand("time set 3000");
	}

	// ------------------------------------------------------------------ a death ends it

	private static void death(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		long[] before = on(world, player -> {
			setAura(player, "ember", AuraRules.FORM);
			setMomentumNow(player, 60);
			check(AuraApi.awaken(player), "it should awaken");
			return new long[] {player.level().getGameTime(), Awakening.state(player).readyAt()};
		});
		context.waitTicks(AwakeningRules.RISE);
		world.getServer().runOnServer(server -> player(server).kill(player(server).level()));
		context.waitTicks(5);
		context.runOnClient(mc -> {
			mc.player.respawn();
			mc.gui.setScreen(null);
		});
		context.waitTicks(15);
		String after = on(world, player -> {
			Awakening.State s = Awakening.state(player);
			long now = player.level().getGameTime();
			if (Awakening.awakened(player) || Awakening.spent(player)) {
				return "a death should end it, and the new body shouldn't be spent (" + s + ")";
			}
			if (s.stage() != AwakeningRules.Phase.RESTING) {
				return "after a death it should rest (" + s.stage() + ")";
			}
			if (!s.resting(now) || s.readyAt() > before[1]) {
				return "its rest should still run, from the death at the latest (" + s.readyAt() + " against " + before[1] + ")";
			}
			if (player.hasEffect(MobEffects.SLOWNESS)) {
				return "the new body shouldn't be slowed";
			}
			if (Aura.data(player).aura() > 0) {
				return "a new body starts with its aura empty (" + Aura.data(player).aura() + ")";
			}
			return AuraApi.gain(player, 5, "test") > 0 ? null : "the new body should gather aura";
		});
		check(after == null, after);
		check(!context.computeOnClient(mc -> Awakening.awakened(mc.player)), "the swordsman's own client should know it ended");
		calm(context, world);
	}

	// ------------------------------------------------------------------ a duel

	private static void duel(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.FORM);
			setMomentumNow(player, 60);
			Rival rival = rival(player);
			rival.getAttribute(Attributes.ARMOR).setBaseValue(0);
			rival.removeAllEffects();
			return null;
		});
		context.waitTicks(FULL + 5);
		float plain = on(world, player -> rivalBlow(player));
		on(world, player -> {
			check(AuraApi.awaken(player), "it should awaken");
			return null;
		});
		context.waitTicks(AwakeningRules.BURST_AT + 4);
		on(world, player -> {
			Rival rival = rival(player);
			Vec3 p = at(0, 2.0);
			rival.snapTo(p.x, p.y, p.z, 180, 0);
			rival.setDeltaMovement(Vec3.ZERO);
			return null;
		});
		context.waitTicks(FULL);
		float awake = on(world, player -> rivalBlow(player));
		double factor = on(world, player -> Awakening.damage(player, rival(player)));
		check(Math.abs(factor - AwakeningRules.damage(AwakeningRules.DAMAGE, true)) < 1.0E-9, "against a player the bonus should be half (" + factor + ")");
		// Under the cap and the PvP scale: a few percent in the end (a creature's would be 15%).
		double ratio = awake / Math.max(0.01, plain);
		check(ratio > 1.0 && ratio < 1.07, "against a player a coated blow should land only a little harder (" + plain + " then " + awake + ")");
		calm(context, world);
		// An awakened rival, as the swordsman's own client draws them.
		on(world, player -> {
			Rival rival = rival(player);
			rival.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("crimson", AuraRules.SOVEREIGN, AuraRules.threshold(AuraRules.SOVEREIGN),
				AuraRules.capacity(AuraRules.SOVEREIGN), 0));
			rival.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));
			rival.setAttached(AuraAttachments.LOOK, new AuraAttachments.Look(dev.wildercord.aura.BreathingMethods.CRIMSON.color(AuraRules.SOVEREIGN),
				AuraRules.SOVEREIGN, true, false, false));
			long now = player.level().getGameTime();
			rival.setAttached(Awakening.AWAKENING, new Awakening.State(AwakeningRules.Phase.AWAKENED.ordinal(), now - 40, now + 2000, 0, now + 6000, 0, 0));
			Vec3 p = at(0, 3.2);
			rival.snapTo(p.x, p.y, p.z, 180, 0);
			rival.setYHeadRot(180);
			return null;
		});
		context.waitTicks(20);
		boolean seen = context.computeOnClient(mc -> mc.level.players().stream().anyMatch(p -> p.getName().getString().equals("Rival") && Awakening.awakened(p)));
		check(seen, "the swordsman's client should know the rival is awakened");
		shot(context, "awakening_rival_fp");
		on(world, player -> {
			Rival rival = rival(player);
			rival.discard();
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(List.of(rival.getUUID())));
			return null;
		});
		rival = null;
		world.getServer().runCommand("time set 3000");
	}

	private static float rivalBlow(ServerPlayer player) {
		Rival rival = rival(player);
		rival.setHealth(rival.getMaxHealth());
		rival.removeAttached(Stance.STANCE);
		dev.wildercord.cast.Effects.readyToHurt(rival);
		float hp = rival.getHealth();
		player.attack(rival);
		float lost = hp - rival.getHealth();
		rival.setHealth(rival.getMaxHealth());
		return lost;
	}

	// ------------------------------------------------------------------ the Sovereign's awakened Dominion

	private static void dominion(ClientGameTestContext context, TestSingleplayerContext world, String method) {
		world.getServer().runCommand("time set 18000");
		reset(context, world);
		AwakeningRules.Flavour flavour = AwakeningRules.Flavour.of(method);
		on(world, player -> {
			setAura(player, method, AuraRules.SOVEREIGN);
			setMomentumNow(player, 60);
			for (int i = 0; i < 3; i++) {
				double a = Math.PI * 0.35 + Math.PI * 0.3 * i;
				Mob husk = spawn(player.level(), EntityTypes.HUSK, at(Math.cos(a) * 2.8, Math.sin(a) * 2.8), 120);
				if (flavour != AwakeningRules.Flavour.STONE) {
					steady(husk);
				}
			}
			check(AuraApi.awaken(player), method + " should awaken (" + Awakening.refusal(player) + ")");
			return null;
		});
		context.waitTicks(AwakeningRules.BURST_AT + 4);
		regroup(context, world);
		on(world, player -> {
			player.setHealth(10);
			return null;
		});
		thirdPerson(context, world, 0, 62, 9.0, false);
		context.waitTicks(4);
		context.getInput().holdKey(WildercordKeys.auraMapping());
		context.waitTicks(8);
		context.getInput().releaseKey(WildercordKeys.auraMapping());
		context.waitTicks(2);
		String raised = on(world, player -> {
			if (!AuraDominion.active(player)) {
				return "a hold should raise a Dominion";
			}
			if (!AuraDominion.sovereign(player)) {
				return "raised awakened, it should be the Sovereign's own";
			}
			double r = AuraDominion.radius(player);
			return Math.abs(r - AuraRules.DOMINION_RADIUS * AwakeningRules.Sovereign.RADIUS) < 1.0E-6 ? null : "it should be half again as wide (" + r + ")";
		});
		check(raised == null, raised);
		String banner = context.computeOnClient(mc -> AuraBanners.ownShowing());
		String expected = context.computeOnClient(mc -> Component.translatable(flavour.nameKey()).getString());
		check(banner.equals(expected), "its banner should name it " + expected + " (" + banner + ")");
		String grimoire = on(world, player -> dev.wildercord.player.Heart.grimoire(player).contains("aura:sovereign_dominion") ? null
			: "the first awakened Dominion should go into the Grimoire");
		check(grimoire == null, grimoire);
		// What it does at once.
		String atOnce = on(world, player -> switch (flavour) {
			case RIME -> foes(player).stream().allMatch(RimeArts::frozen) ? null : "the husks inside should freeze as it's raised";
			case VERDANT -> foes(player).stream().allMatch(ArtKit::rooted) ? null : "the husks inside should be rooted as it's raised";
			case HOLLOW -> foes(player).stream().allMatch(ArtWards::silenced) ? null : "the husks inside should be silenced as it's raised";
			case HOURGLASS -> foes(player).stream().allMatch(Mob::isNoAi) ? null : "the husks inside should be held still as it's raised";
			default -> null;
		});
		check(atOnce == null, atOnce);
		context.waitTicks(10);
		String name = method.contains(":") ? "plain" : method;
		shot(context, "dominion_" + name + "_tp");
		float[] health = on(world, player -> {
			List<Mob> foes = foes(player);
			float[] h = new float[foes.size()];
			for (int i = 0; i < h.length; i++) {
				h[i] = foes.get(i).getHealth();
			}
			return h;
		});
		boolean[] lifted = {false};
		for (int t = 0; t < 45; t++) {
			context.waitTicks(1);
			if (flavour == AwakeningRules.Flavour.GALE && on(world, player -> foes(player).stream().anyMatch(m -> !m.onGround()))) {
				lifted[0] = true;
			}
		}
		String beat = on(world, player -> switch (flavour) {
			case EMBER -> foes(player).stream().anyMatch(m -> m.getRemainingFireTicks() > 0) ? null : "the husks inside should burn";
			case THUNDER -> {
				List<Mob> foes = foes(player);
				boolean hurt = false;
				for (int i = 0; i < foes.size() && i < health.length; i++) {
					hurt |= foes.get(i).getHealth() < health[i];
				}
				yield hurt ? null : "a bolt should have fallen on a husk inside";
			}
			case GALE -> lifted[0] ? null : "an updraft should have lifted the husks inside";
			case STONE -> {
				if (!ArtWards.hardened(player)) {
					yield "standing in it, the swordsman should be hardened";
				}
				Stance.State s = Stance.state(foes(player).getFirst());
				yield s != null && s.wornAt(player.level().getGameTime()) > 0 ? null : "it should wear the husks' stance";
			}
			case VERDANT -> player.getHealth() > 10 ? null : "it should mend the swordsman standing in it (" + player.getHealth() + ")";
			case STARLIT -> foes(player).stream().anyMatch(m -> ArtWards.starred(player, m)) ? null : "a star should have fallen on a husk inside";
			case HOURGLASS -> foes(player).stream().allMatch(m -> m.hasEffect(MobEffects.SLOWNESS)) ? null : "time should drag for the husks inside";
			case CRIMSON -> foes(player).stream().anyMatch(m -> Reactions.has(m, Reactions.Mark.BLEEDING)) ? null : "the husks inside should bleed";
			case RIME -> foes(player).stream().allMatch(m -> m.hasEffect(MobEffects.SLOWNESS) || RimeArts.frozen(m)) ? null : "the husks inside should be chilled";
			case HOLLOW, PLAIN -> null;
		});
		check(beat == null, beat);
		on(world, player -> {
			AuraDominion.end(player);
			player.setHealth(player.getMaxHealth());
			return null;
		});
		calm(context, world);
		context.waitTicks(30);
		world.getServer().runCommand("time set 3000");
	}

	// ------------------------------------------------------------------ the HUD and the Aura page

	private static void pages(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "starlit", AuraRules.SOVEREIGN);
			setMomentumNow(player, 60);
			return null;
		});
		context.waitTicks(10);
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		shot(context, "awakening_page_ready");
		context.setScreen(() -> null);
		on(world, player -> {
			check(AuraApi.awaken(player), "it should awaken");
			return null;
		});
		context.waitTicks(AwakeningRules.RISE);
		shot(context, "awakening_hud_awakened");
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		shot(context, "awakening_page_awakened");
		context.setScreen(() -> null);
		calm(context, world);
	}

	// ------------------------------------------------------------------ the keys

	private static void swing(ClientGameTestContext context) {
		context.getInput().pressKey(o -> o.keyAttack);
	}

	private static void lowSwing(ClientGameTestContext context) {
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(2);
		swing(context);
	}

	/** Three full swings and a low one: the Final Art's string. */
	private static void finalSwings(ClientGameTestContext context, TestSingleplayerContext world) {
		for (int i = 0; i < 3; i++) {
			swing(context);
			regroup(context, world);
			context.waitTicks(FULL - 2);
		}
		lowSwing(context);
	}

	/** A tap of the real Aura key, then pressed again at once and held {@code hold} ticks. */
	private static void tapHold(ClientGameTestContext context, int hold) {
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(2);
		context.getInput().holdKey(WildercordKeys.auraMapping());
		context.waitTicks(hold);
		context.getInput().releaseKey(WildercordKeys.auraMapping());
	}

	// ------------------------------------------------------------------ views

	/** Third person from behind, the view turned {@code yaw} and looking {@code pitch} down, {@code distance} back. */
	private static void thirdPerson(ClientGameTestContext context, TestSingleplayerContext world, float yaw, float pitch, double distance, boolean hud) {
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(distance);
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), yaw, pitch, false);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			if (mc.gui.hud.isHidden() == hud) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(2);
	}

	/** Third person from the front (the face and the eyes), {@code distance} off, the HUD hidden. */
	private static void front(ClientGameTestContext context, TestSingleplayerContext world, double distance) {
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(distance);
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 160.0F, 6.0F, false);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(2);
	}

	private static void firstPerson(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 8.0F, false);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
	}

	// ------------------------------------------------------------------ the stage

	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 14) + " " + (y - 1) + " " + (z - 14) + " " + (x + 14) + " " + (y - 1) + " " + (z + 24) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 14) + " " + y + " " + (z - 14) + " " + (x + 14) + " " + (y + 10) + " " + (z + 24) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			stand(player);
		});
	}

	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.resetFallDistance();
		player.clearFire();
	}

	private static void reset(ClientGameTestContext context, TestSingleplayerContext world) {
		context.getInput().releaseKey(o -> o.keyShift);
		calm(context, world);
		kill(world);
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runCommand("kill @e[type=block_display]");
		world.getServer().runCommand("fill " + (STAGE.getX() - 14) + " " + STAGE.getY() + " " + (STAGE.getZ() - 14) + " " + (STAGE.getX() + 14) + " "
			+ (STAGE.getY() + 10) + " " + (STAGE.getZ() + 24) + " minecraft:air");
		context.waitTicks(6);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			player.removeAttached(AuraAttachments.STATE);
			player.removeAttached(SwordStrings.COOLDOWNS);
			player.removeAttached(Momentum.MOMENTUM);
			player.removeAttached(Stance.STANCE);
			player.setAttached(AuraPresence.TIMERS, AuraPresence.Timers.NONE);
			dev.wildercord.aura.arts.MethodArts.forget(player.getUUID());
		});
		firstPerson(context, world);
		context.runOnClient(mc -> {
			mc.player.setXRot(8);
			mc.player.xRotO = 8;
		});
		// Long enough for any string to run out and the last blow to be forgotten.
		context.waitTicks(StringRules.ENGAGED_TICKS / 2);
	}

	/** Ends any awakening (spent after it, as it would be) and then clears it all away, the pool full again, for the next scene. */
	private static void calm(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			AuraApi.endAwakening(player);
			return null;
		});
		context.waitTicks(2);
		on(world, player -> {
			player.removeAttached(Awakening.AWAKENING);
			player.removeAllEffects();
			if (Aura.stage(player) > AuraRules.NONE) {
				player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(Aura.capacity(player)));
			}
			player.removeAttached(Momentum.MOMENTUM);
			return null;
		});
		context.waitTicks(2);
	}

	private static void kill(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			for (Entity e : player.level().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(64), e -> e.entityTags().contains(TAG))) {
				e.discard();
			}
		});
	}

	private static void setAura(ServerPlayer player, String method, int stage) {
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(method, stage, AuraRules.threshold(stage), AuraRules.capacity(stage), 0));
		player.removeAttached(AuraAttachments.STATE);
		player.removeAttached(Awakening.AWAKENING);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
	}

	/** Sets the swordsman's momentum to {@code value}, held (no ebb) for the rest of the scene. */
	private static void setMomentumNow(ServerPlayer player, double value) {
		player.setAttached(Momentum.MOMENTUM, new Momentum.State((float) value, player.level().getGameTime() + 100000, 0, 0, 0));
	}

	private static List<Integer> marks(AuraApi.StringArt art) {
		return art.string().tokens().stream().map(t -> SwordString.Token.marks(t)).toList();
	}

	private static Vec3 at(double side, double ahead) {
		return new Vec3(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead);
	}

	/** A foe that stands its ground: no speed, no sight, its AI left on (so a lift or a throw carries it, and it can fight back). */
	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, Vec3 at, double health) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		if (mob == null) {
			throw new AssertionError("couldn't make a " + type);
		}
		mob.snapTo(at.x, at.y, at.z, 180, 0);
		mob.addTag(TAG);
		mob.addTag("wildercord.rolled");
		mob.setPersistenceRequired();
		AttributeInstance max = mob.getAttribute(Attributes.MAX_HEALTH);
		if (max != null) {
			max.setBaseValue(health);
		}
		AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(0.0);
		}
		AttributeInstance sight = mob.getAttribute(Attributes.FOLLOW_RANGE);
		if (sight != null) {
			sight.setBaseValue(0.0);
		}
		mob.setHealth((float) health);
		level.addFreshEntity(mob);
		SPOTS.put(mob.getUUID(), at);
		return mob;
	}

	/** The scene's husks, in the order they were spawned. */
	private static List<Mob> foes(ServerPlayer player) {
		List<Mob> found = new ArrayList<>(player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48),
			m -> m.entityTags().contains(TAG) && m.getType() == EntityTypes.HUSK));
		found.sort(java.util.Comparator.comparingInt(Mob::getId));
		if (found.isEmpty()) {
			throw new AssertionError("the foes are gone");
		}
		return found;
	}

	/** Where each foe was stood, to put it back after a swing's knockback or the burst's push. */
	private static final Map<java.util.UUID, Vec3> SPOTS = new java.util.concurrent.ConcurrentHashMap<>();

	private static void regroup(ClientGameTestContext context, TestSingleplayerContext world) {
		context.waitTicks(2);
		on(world, player -> {
			for (Mob m : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32), m -> m.entityTags().contains(TAG) && !m.isPassenger())) {
				Vec3 p = SPOTS.get(m.getUUID());
				if (p != null) {
					m.teleportTo(p.x, p.y, p.z);
					m.setDeltaMovement(Vec3.ZERO);
				}
			}
			return null;
		});
	}

	/** A foe that stands steady for the whole scene: nothing wears its stance (a scene about something else). */
	private static void steady(Mob mob) {
		long now = mob.level().getGameTime();
		mob.setAttached(Stance.STANCE, new Stance.State(0, now, (float) StanceRules.pool(Stance.kind(mob), mob.getMaxHealth(), 0), Stance.kind(mob).ordinal(),
			-1, now + 100000, 0));
	}

	/**
	 * A second player for a duel: a fake one (Fabric's own can't be hurt and has no team, so this one can and does), in the world in
	 * front of the swordsman, so the client sees them too.
	 */
	private static final class Rival extends FakePlayer {
		Rival(ServerLevel level) {
			super(level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("wildercord-awakening-rival".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
				"Rival"));
		}

		@Override
		public boolean isInvulnerableTo(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
			return false;
		}

		@Override
		public PlayerTeam getTeam() {
			return level().getScoreboard().getPlayersTeam(getScoreboardName());
		}
	}

	private static Rival rival;

	private static Rival rival(ServerPlayer player) {
		if (rival == null || rival.isRemoved() || rival.level() != player.level()) {
			rival = new Rival(player.level());
			Vec3 p = at(0, 2.0);
			rival.snapTo(p.x, p.y, p.z, 180, 0);
			rival.setYHeadRot(180);
			player.connection.send(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(rival)));
			player.level().addNewPlayer(rival);
		}
		return rival;
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	private static void player(TestSingleplayerContext world, java.util.function.Consumer<ServerPlayer> step) {
		world.getServer().runOnServer(server -> step.accept(player(server)));
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
