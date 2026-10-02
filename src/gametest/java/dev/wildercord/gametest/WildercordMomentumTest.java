package dev.wildercord.gametest;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.Momentum;
import dev.wildercord.aura.MomentumRules;
import dev.wildercord.aura.Stance;
import dev.wildercord.aura.StanceRules;
import dev.wildercord.aura.StringRules;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.EmberArts;
import dev.wildercord.aura.arts.Finishers;
import dev.wildercord.cast.TrainingDummy;
import dev.wildercord.cast.WildercordEntities;
import dev.wildercord.client.AuraBanners;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.client.StanceHud;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Momentum and openings, step 5 of the aura overhaul, played with the real keys on a stone platform in the sky:
 * <ul>
 *   <li><b>Momentum</b>: clean full swings build it (and a weak one doesn't), on the server and the swordsman's own client alike;
 *       a hit taken knocks a share off; out of a fight it ebbs; each tier makes arts cheaper and stronger; the Final Art waits on the
 *       peak (below it the same swings fall through to the First Art) and its release spends some; it can't be farmed on a dummy (a
 *       tier short of the peak), a foe with no mind of its own, a foe in a boat, or one foe struck forever (its budget);</li>
 *   <li><b>Stance</b>: blows wear a foe's stance (Stone's more), an art more; each client draws its bar; broken, the foe is opened
 *       (held, sealed, the crosshair cue showing), and the next full swing is a finisher: a share of what it lost, aura and momentum
 *       back, its banner, the foe steady after;</li>
 *   <li><b>Every method's finisher</b>, named and filmed from inside it and from behind;</li>
 *   <li><b>A boss</b>: much more stance, only slowed when opened, a small finisher, steady long after and steadier each break;</li>
 *   <li><b>A duel</b> (a fake second player): a blow wears at most a third of a player's stance, three or more open them (slowed,
 *       shield and guard broken, never held), a finisher on them is capped and a totem still saves them, a teammate is never touched,
 *       and the player's own stance shows on their aura strip;</li>
 *   <li>the Aura page's momentum and finisher lines, and the momentum line on the HUD at each tier.</li>
 * </ul>
 * Screenshots: {@code momentum_*}, {@code stance_*}, {@code opened_*}, {@code finisher_<id>_fp} and {@code _tp}.
 *
 * <p>{@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it.</p>
 */
public class WildercordMomentumTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 190, 0);
	private static final String TAG = "wildercord.momentum_test";
	private static final List<String> PERFORMED = Collections.synchronizedList(new ArrayList<>());
	private static boolean hooked;
	/** A sword's full swing comes back in 11 ticks: a swing as soon as it's full again. */
	private static final int FULL = 13;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		TestMethods.plain();
		if (!hooked) {
			hooked = true;
			AuraApi.onString((player, art, ctx) -> PERFORMED.add(art.id()));
		}
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
			run(failures, "momentum builds from clean hits", () -> builds(context, world));
			run(failures, "momentum falls and ebbs", () -> falls(context, world));
			run(failures, "the tiers make arts cheaper and stronger", () -> tiers(context, world));
			run(failures, "the Final Art waits on the peak", () -> gate(context, world));
			run(failures, "momentum can't be farmed", () -> honest(context, world));
			run(failures, "stance wears", () -> wears(context, world));
			run(failures, "an opened foe and its finisher", () -> opened(context, world));
			for (String method : List.of("ember", "rime", "thunder", "gale", "stone", "verdant", "hollow", "starlit", "hourglass", "crimson", TestMethods.PLAIN)) {
				run(failures, method + "'s finisher", () -> finisher(context, world, method));
			}
			run(failures, "a boss", () -> boss(context, world));
			run(failures, "a duel", () -> duel(context, world));
			run(failures, "the HUD and the Aura page", () -> pages(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("Momentum and openings went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				AuraScreen.listArts(false);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.getInput().releaseKey(o -> o.keyShift);
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

	// ------------------------------------------------------------------ momentum

	private static void builds(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200);
			return null;
		});
		context.waitTicks(20);
		double before = on(world, Momentum::value);
		check(before == 0, "momentum should start empty (" + before + ")");
		swing(context);
		regroup(context, world);
		context.waitTicks(4);
		double one = on(world, Momentum::value);
		check(one > 2.5 && one < 8, "a clean full swing should build a few points (" + one + ")");
		double client = context.computeOnClient(mc -> Momentum.value(mc.player));
		check(Math.abs(client - one) < 0.6, "the swordsman's own client should read the same (" + client + " against " + one + ")");
		for (int i = 0; i < 2; i++) {
			context.waitTicks(FULL - 6);
			swing(context);
			regroup(context, world);
			context.waitTicks(4);
		}
		double three = on(world, Momentum::value);
		check(three > one * 2.4, "three clean swings should build about three times one (" + three + " after " + one + ")");
		// A swing short of full (straight after another) builds nothing.
		context.waitTicks(FULL);
		swing(context);
		context.waitTicks(3);
		double afterFull = on(world, Momentum::value);
		swing(context);
		context.waitTicks(3);
		double afterWeak = on(world, Momentum::value);
		check(Math.abs(afterWeak - afterFull) < 0.01, "a weak swing shouldn't build momentum (" + afterFull + " to " + afterWeak + ")");
		// The line under the aura bar, at the second tier and at the peak.
		setMomentum(world, 62);
		context.waitTicks(8);
		shot(context, "momentum_hud_tier2");
		setMomentum(world, 100);
		context.waitTicks(16);
		shot(context, "momentum_hud_peak");
		int glow = on(world, player -> AuraPresence.look(player).momentum());
		check(glow == MomentumRules.PEAK_TIER, "everyone should see the swordsman's aura burn at the peak (" + glow + ")");
	}

	private static void falls(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			spawn(player.level(), EntityTypes.HUSK, at(0, 1.6), 200);
			long now = player.level().getGameTime();
			player.setAttached(Momentum.MOMENTUM, new Momentum.State(80, now + 40, (float) (MomentumRules.EBB / 20.0 * 1.4), 0, 0));
			return null;
		});
		context.waitTicks(4);
		String struck = on(world, player -> {
			double before = Momentum.value(player);
			Mob husk = foes(player).getFirst();
			husk.doHurtTarget(player.level(), player);
			double after = Momentum.value(player);
			double lost = before - after;
			return lost > 10 && lost < 35 ? null : "a husk's blow should knock off a share (" + before + " to " + after + ")";
		});
		check(struck == null, struck);
		kill(world);
		double held = on(world, Momentum::value);
		context.waitTicks(60 + 40);
		double ebbed = on(world, Momentum::value);
		// Ember's grace is three seconds, then it ebbs eight a second, half again as fast: about eleven in the second after.
		check(ebbed < held - 8, "out of a fight it should ebb (" + held + " to " + ebbed + ")");
		context.waitTicks(220);
		check(on(world, Momentum::value) == 0, "and in the end be gone");
	}

	private static void tiers(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		String prices = on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			AuraApi.StringArt art = AuraApi.string(EmberArts.KINDLING_DRAW).orElseThrow();
			double base = SwordStrings.price(player, art);
			setMomentumNow(player, 80);
			double tier3 = SwordStrings.price(player, art);
			setMomentumNow(player, 100);
			double peak = SwordStrings.price(player, art);
			if (Math.abs(base - art.cost()) > 1.0E-6 || Math.abs(tier3 - art.cost() * 0.8) > 1.0E-6 || Math.abs(peak - art.cost() * 0.75) > 1.0E-6) {
				return "the price should fall a fifth at the third tier and a quarter at the peak (" + base + ", " + tier3 + ", " + peak + ")";
			}
			return null;
		});
		check(prices == null, prices);
		// Stronger: the same art on two like husks, one at no momentum and one at the peak.
		float[] taken = new float[2];
		for (int i = 0; i < 2; i++) {
			int pass = i;
			reset(context, world);
			on(world, player -> {
				setAura(player, "ember", AuraRules.SOVEREIGN);
				Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.0), 300);
				husk.getAttribute(Attributes.ARMOR).setBaseValue(0);
				setMomentumNow(player, pass == 0 ? 0 : 100);
				return null;
			});
			context.waitTicks(5);
			taken[i] = on(world, player -> {
				Mob husk = foes(player).getFirst();
				float hp = husk.getHealth();
				AuraApi.StringArt art = AuraApi.string(EmberArts.KINDLING_DRAW).orElseThrow();
				SwordStrings.perform(player, art, marks(art));
				return hp - husk.getHealth();
			});
		}
		double ratio = taken[1] / Math.max(0.01, taken[0]);
		check(ratio > 1.14 && ratio < 1.26, "an art at the peak should strike a fifth harder (" + taken[0] + " then " + taken[1] + ")");
	}

	private static void gate(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			steady(spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 400));
			setMomentumNow(player, 50);
			return null;
		});
		context.waitTicks(20);
		PERFORMED.clear();
		finalSwings(context, world);
		context.waitTicks(6);
		context.getInput().releaseKey(o -> o.keyShift);
		check(PERFORMED.equals(List.of(EmberArts.KINDLING_DRAW)), "below the peak the Final Art's swings should fall through to the First Art (" + PERFORMED + ")");
		String hint = context.computeOnClient(mc -> {
			AuraApi.StringArt art = AuraApi.string(EmberArts.SUNFALL).orElseThrow();
			return art.condition().met(mc.player) ? "the client should know the Final Art waits" : art.condition().hintKey(mc.player);
		});
		check("message.wildercord.aura.art.peak".equals(hint), "it should say it waits on the peak (" + hint + ")");
		on(world, player -> {
			player.removeAttached(SwordStrings.COOLDOWNS);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(AuraRules.capacity(AuraRules.SOVEREIGN)));
			player.setAttached(Momentum.MOMENTUM, new Momentum.State(100, player.level().getGameTime() + 100000, 0, 0, 0));
			return null;
		});
		context.waitTicks(30);
		PERFORMED.clear();
		finalSwings(context, world);
		context.waitTicks(6);
		context.getInput().releaseKey(o -> o.keyShift);
		check(PERFORMED.equals(List.of(EmberArts.SUNFALL)), "at the peak they should play the Final Art (" + PERFORMED + ")");
		double after = on(world, Momentum::value);
		check(after <= MomentumRules.MAX - MomentumRules.FINAL_SPEND + 1.0E-6, "its release should spend some (" + after + ")");
		check(!on(world, Momentum::peak), "and leave it short of the peak");
		context.waitTicks(40);
	}

	private static void honest(ClientGameTestContext context, TestSingleplayerContext world) {
		// A training dummy: it builds, but never past the practice ceiling.
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			TrainingDummy dummy = WildercordEntities.TRAINING_DUMMY.create(player.level(), EntitySpawnReason.COMMAND);
			Vec3 p = at(0, 2.2);
			dummy.snapTo(p.x, p.y, p.z, 180, 0);
			dummy.addTag(TAG);
			player.level().addFreshEntity(dummy);
			player.setAttached(Momentum.MOMENTUM, new Momentum.State(70, player.level().getGameTime() + 100000, 0, 0, 0));
			return null;
		});
		context.waitTicks(20);
		for (int i = 0; i < 3; i++) {
			swing(context);
			context.waitTicks(FULL);
		}
		double dummy = on(world, Momentum::value);
		check(dummy > 70 && dummy <= MomentumRules.PRACTICE_CEILING + 1.0E-4, "a dummy should build only to the practice ceiling (" + dummy + ")");
		// What's built in the practice arena stays there: changing world empties it.
		double goingIn = on(world, player -> {
			setMomentumNow(player, 90);
			check(dev.wildercord.cast.PracticeRoom.enter(player) == 1, "the practice arena should open");
			return Momentum.value(player);
		});
		check(goingIn == 0, "going into the practice arena should empty momentum (" + goingIn + ")");
		context.waitTicks(30);
		double comingOut = on(world, player -> {
			setMomentumNow(player, 90);
			check(dev.wildercord.cast.PracticeRoom.leave(player) == 1, "the practice arena should let the player go home");
			return Momentum.value(player);
		});
		check(comingOut == 0, "coming out of the practice arena should empty momentum (" + comingOut + ")");
		context.waitTicks(30);
		try {
			world.getConnection().waitForChunksRender();
		} catch (RuntimeException e) {
			// Slow chunks only make the next scene's first frames bare; the checks don't need them.
		}
		// A husk with no mind of its own, and one in a boat: nothing.
		for (String which : List.of("mindless", "boat")) {
			reset(context, world);
			on(world, player -> {
				setAura(player, "ember", AuraRules.SOVEREIGN);
				Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200);
				if (which.equals("mindless")) {
					husk.setNoAi(true);
				} else {
					Entity boat = EntityTypes.OAK_BOAT.create(player.level(), EntitySpawnReason.COMMAND);
					Vec3 p = at(0, 2.4);
					boat.snapTo(p.x, p.y, p.z, 180, 0);
					boat.addTag(TAG);
					player.level().addFreshEntity(boat);
					husk.startRiding(boat);
				}
				return null;
			});
			context.waitTicks(20);
			if (which.equals("boat")) {
				context.runOnClient(mc -> {
					mc.player.setXRot(18);
					mc.player.xRotO = 18;
				});
			}
			for (int i = 0; i < 2; i++) {
				swing(context);
				context.waitTicks(FULL);
			}
			String got = on(world, player -> {
				Mob husk = foes(player).getFirst();
				if (husk.getHealth() >= husk.getMaxHealth()) {
					return "the " + which + " husk should have been struck";
				}
				return Momentum.value(player) == 0 ? null : "a " + which + " husk shouldn't build momentum (" + Momentum.value(player) + ")";
			});
			check(got == null, got);
		}
		// One foe struck again and again: its budget runs out.
		reset(context, world);
		on(world, player -> {
			setAura(player, TestMethods.PLAIN, AuraRules.SOVEREIGN);
			steady(spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 600));
			return null;
		});
		context.waitTicks(20);
		for (int i = 0; i < 15; i++) {
			swing(context);
			regroup(context, world);
			context.waitTicks(FULL - 2);
		}
		double budget = on(world, Momentum::value);
		double most = MomentumRules.budget(600, false, false, false);
		check(budget > most - 4 && budget <= most + 1.0E-3, "one foe's blows should build only its budget, " + most + " (" + budget + ")");
	}

	// ------------------------------------------------------------------ stance

	private static void wears(ClientGameTestContext context, TestSingleplayerContext world) {
		double[] worn = new double[2];
		String[] methods = {"ember", "stone"};
		for (int i = 0; i < 2; i++) {
			int pass = i;
			reset(context, world);
			on(world, player -> {
				setAura(player, methods[pass], AuraRules.SOVEREIGN);
				spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 100);
				return null;
			});
			context.waitTicks(20);
			swing(context);
			regroup(context, world);
			context.waitTicks(FULL - 2);
			swing(context);
			regroup(context, world);
			context.waitTicks(3);
			worn[i] = on(world, player -> {
				Stance.State s = Stance.state(foes(player).getFirst());
				return s == null ? 0 : s.wornAt(player.level().getGameTime()) / s.pool();
			});
			if (i == 0) {
				int[] drawn = context.computeOnClient(mc -> StanceHud.drawn());
				check(drawn[0] >= 1, "the swordsman's client should draw the worn husk's stance bar (" + drawn[0] + ")");
				// For the pictures: the husk a step back and still, its flames out, so its bar reads against the sky.
				on(world, player -> {
					Mob husk = foes(player).getFirst();
					Vec3 p = at(0, 3.4);
					husk.setNoAi(true);
					husk.teleportTo(p.x, p.y, p.z);
					husk.setDeltaMovement(Vec3.ZERO);
					husk.clearFire();
					return null;
				});
				context.waitTicks(10);
				shot(context, "stance_bar_fp");
				thirdPerson(context, world, -55, 24, true);
				context.waitTicks(3);
				shot(context, "stance_bar_tp");
				firstPerson(context, world);
			}
		}
		check(worn[0] > 0.1 && worn[0] < 0.4, "two blows should wear a strong husk's stance part way (" + worn[0] + ")");
		check(worn[1] > worn[0] * 1.15, "Stone's blows should wear it more (" + worn[1] + " against " + worn[0] + ")");
		// An art wears more than a blow.
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.0), 100);
			return null;
		});
		context.waitTicks(10);
		double[] art = on(world, player -> {
			Mob husk = foes(player).getFirst();
			float hp = husk.getHealth();
			AuraApi.StringArt kindling = AuraApi.string(EmberArts.KINDLING_DRAW).orElseThrow();
			SwordStrings.perform(player, kindling, marks(kindling));
			Stance.State s = Stance.state(husk);
			return new double[] {s == null ? 0 : s.wornAt(player.level().getGameTime()), hp - husk.getHealth()};
		});
		// For what it deals, an art wears twice what a blow does (a blow wears what it deals).
		check(art[1] > 0 && art[0] / art[1] > 1.6, "an art should wear more than a blow for what it deals (" + art[0] + " worn by " + art[1] + " dealt)");
	}

	private static void opened(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			// Half a pool, so the aura a finisher gives back has room to land.
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(60));
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 60);
			husk.setHealth(30);
			// Worn almost through: the next blow breaks it.
			Stance.wear(player, husk, StanceRules.pool(StanceRules.Kind.CREATURE, 60, 0) - 2, StanceRules.Source.BLOW);
			return null;
		});
		context.waitTicks(10);
		double momentum = on(world, Momentum::value);
		swing(context);
		context.waitTicks(4);
		String open = on(world, player -> {
			Mob husk = foes(player).getFirst();
			if (!Stance.opened(husk)) {
				return "the blow should have broken its stance";
			}
			if (!husk.isNoAi() || !husk.hasAttached(WildercordAttachments.FROZEN_UNTIL)) {
				return "an opened creature should be held";
			}
			return Momentum.value(player) > momentum + MomentumRules.BREAK * 0.9 ? null : "breaking it should build momentum";
		});
		check(open == null, open);
		int[] drawn = context.computeOnClient(mc -> StanceHud.drawn());
		check(drawn[1] >= 1, "the client should draw its opened seal (" + drawn[1] + ")");
		check(drawn[2] == 1, "the crosshair cue should show over an opened foe");
		context.waitTicks(4);
		shot(context, "opened_fp");
		thirdPerson(context, world, -55, 24, true);
		context.waitTicks(3);
		shot(context, "opened_tp");
		firstPerson(context, world);
		context.waitTicks(FULL);
		Object[] before = on(world, player -> new Object[] {foes(player).getFirst().getHealth(), ArtKit.givenBack(), Stance.finishers(), Momentum.value(player)});
		swing(context);
		context.waitTicks(3);
		shot(context, "finisher_first_fp");
		context.waitTicks(3);
		String finished = on(world, player -> {
			Mob husk = foes(player).getFirst();
			if (Stance.finishers() != (int) before[2] + 1 || !Finishers.PYREBRAND.equals(Stance.lastFinisher())) {
				return "the full swing should have been Ember's finisher (" + Stance.lastFinisher() + ")";
			}
			float lost = (float) before[0] - husk.getHealth();
			if (husk.isAlive() && lost < 30 * StanceRules.SHARE) {
				return "it should deal a share of what the husk had lost besides the blow (" + lost + ")";
			}
			if (ArtKit.givenBack() - (double) before[1] < StanceRules.finisherAura(AuraRules.SOVEREIGN, false) - 0.01) {
				return "it should give aura back (" + (ArtKit.givenBack() - (double) before[1]) + ")";
			}
			if (Momentum.value(player) < (double) before[3] + MomentumRules.FINISHER * 0.9) {
				return "it should build momentum";
			}
			Stance.State s = Stance.state(husk);
			if (husk.isAlive() && (s == null || s.opened(player.level().getGameTime()) || !s.steady(player.level().getGameTime()))) {
				return "after it the husk should stand steady, no longer opened";
			}
			return null;
		});
		check(finished == null, finished);
		String banner = context.computeOnClient(mc -> AuraBanners.ownShowing());
		check(banner.equals("Pyrebrand"), "the finisher's banner should show its name (" + banner + ")");
	}

	/** One method's finisher, landed with the keys on an opened husk: from inside it, then from behind and above. */
	private static void finisher(ClientGameTestContext context, TestSingleplayerContext world, String method) {
		String expected = AuraApi.finisher(method).id();
		for (int view = 0; view < 2; view++) {
			boolean tp = view == 1;
			reset(context, world);
			on(world, player -> {
				setAura(player, method, AuraRules.SOVEREIGN);
				Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.4), 80);
				husk.setHealth(40);
				// Its stance broken outright (by the swordsman: it's theirs to finish).
				Stance.wear(player, husk, StanceRules.pool(StanceRules.Kind.CREATURE, 80, 0), StanceRules.Source.BLOW);
				return null;
			});
			if (tp) {
				thirdPerson(context, world, 26, false);
			}
			context.waitTicks(16);
			check(on(world, player -> Stance.opened(foes(player).getFirst())), "the husk should stand opened");
			int count = on(world, player -> Stance.finishers());
			swing(context);
			if (tp) {
				// Struck looking at the foe; then the view swings round to the side, so the foe isn't hidden behind the swordsman.
				context.waitTicks(1);
				on(world, player -> {
					player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), -58.0F, 28.0F, false);
					return null;
				});
			}
			context.waitTicks(tp ? 4 : 3);
			shot(context, "finisher_" + expected + (tp ? "_tp" : "_fp"));
			String done = on(world, player -> Stance.finishers() == count + 1 && expected.equals(Stance.lastFinisher()) ? null
				: "the swing should have been " + expected + " (" + Stance.lastFinisher() + ")");
			check(done == null, done);
			context.waitTicks(30);
			if (tp) {
				firstPerson(context, world);
			}
		}
	}

	// ------------------------------------------------------------------ a boss

	private static void boss(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "stone", AuraRules.SOVEREIGN);
			Mob guardian = spawn(player.level(), EntityTypes.ELDER_GUARDIAN, at(0, 3.2), 80);
			guardian.setNoAi(true);
			guardian.setHealth(40);
			return null;
		});
		context.waitTicks(10);
		String boss = on(world, player -> {
			Mob guardian = foes(player, EntityTypes.ELDER_GUARDIAN).getFirst();
			if (Stance.kind(guardian) != StanceRules.Kind.BOSS) {
				return "an elder guardian should stand as a boss";
			}
			double pool = StanceRules.pool(StanceRules.Kind.BOSS, 80, 0);
			if (pool / StanceRules.BOSS_TOUGHNESS <= StanceRules.pool(StanceRules.Kind.CREATURE, 80, 0)) {
				return "a boss should take more to break than a creature of its health";
			}
			AuraApi.wearStance(player, guardian, pool);
			if (!Stance.opened(guardian)) {
				return "worn through, a boss opens too";
			}
			if (guardian.hasAttached(WildercordAttachments.FROZEN_UNTIL) || guardian.getEffect(MobEffects.SLOWNESS) == null) {
				return "an opened boss should be slowed, never held";
			}
			return null;
		});
		check(boss == null, boss);
		context.runOnClient(mc -> {
			mc.player.setXRot(4);
			mc.player.xRotO = 4;
		});
		context.waitTicks(4);
		shot(context, "opened_boss_fp");
		Object[] before = on(world, player -> new Object[] {foes(player, EntityTypes.ELDER_GUARDIAN).getFirst().getHealth(), Stance.finishers()});
		swing(context);
		context.waitTicks(4);
		String finished = on(world, player -> {
			Mob guardian = foes(player, EntityTypes.ELDER_GUARDIAN).getFirst();
			if (Stance.finishers() != (int) before[1] + 1) {
				return "the swing should have been a finisher";
			}
			float lost = (float) before[0] - guardian.getHealth();
			double blade = ArtKit.weapon(player);
			if (lost > blade * 2.2 + StanceRules.BOSS_CAP * blade) {
				return "a boss should take only a small finisher (" + lost + ")";
			}
			Stance.State s = Stance.state(guardian);
			long now = player.level().getGameTime();
			if (s == null || s.steadyUntil() - now < StanceRules.steadyTicks(StanceRules.Kind.BOSS) - 10) {
				return "a boss should stand steady long after";
			}
			if (s.pool() <= StanceRules.pool(StanceRules.Kind.BOSS, 80, 0) + 1.0E-3) {
				return "and grow steadier (" + s.pool() + ")";
			}
			return null;
		});
		check(finished == null, finished);
	}

	// ------------------------------------------------------------------ a duel

	private static void duel(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "crimson", AuraRules.SOVEREIGN);
			return null;
		});
		context.waitTicks(10);
		// Blows on another player: each wears at most a third of their stance; three or more open them.
		int blows = 0;
		boolean open = false;
		double most = 0;
		while (blows < 8 && !open) {
			context.waitTicks(FULL);
			blows++;
			double[] after = on(world, player -> {
				Rival rival = rival(player);
				Stance.State s = Stance.state(rival);
				double before = s == null ? 0 : s.opened(player.level().getGameTime()) ? s.pool() : s.wornAt(player.level().getGameTime());
				rival.setHealth(rival.getMaxHealth());
				// The fake player isn't ticked by the world: its moment of invulnerability after a blow is let go by hand.
				dev.wildercord.cast.Effects.readyToHurt(rival);
				player.attack(rival);
				Stance.State t = Stance.state(rival);
				long now = player.level().getGameTime();
				return new double[] {t == null ? 0 : t.opened(now) ? StanceRules.PLAYER_POOL : t.wornAt(now), before, t != null && t.opened(now) ? 1 : 0};
			});
			if (after[2] == 0) {
				most = Math.max(most, after[0] - after[1]);
			}
			open = after[2] == 1;
		}
		check(open, "steady blows should open another player in the end (" + blows + " blows)");
		check(blows >= 3, "it should take three blows or more to open a player (" + blows + ")");
		check(most <= StanceRules.PLAYER_POOL * StanceRules.PVP_BLOW_CAP + 1.0E-3, "no blow should wear more than a third of it (" + most + ")");
		String staggered = on(world, player -> {
			Rival rival = rival(player);
			if (rival.getEffect(MobEffects.SLOWNESS) == null) {
				return "an opened player should be slowed";
			}
			if (!rival.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD))) {
				return "and their shield broken for the moment";
			}
			return rival.hasAttached(WildercordAttachments.FROZEN_UNTIL) ? "but never held" : null;
		});
		check(staggered == null, staggered);
		// The finisher on a player: a capped share, through their armour, never a one-shot.
		context.waitTicks(FULL);
		String capped = on(world, player -> {
			Rival rival = rival(player);
			rival.setHealth(14);
			dev.wildercord.cast.Effects.readyToHurt(rival);
			int count = Stance.finishers();
			float hp = rival.getHealth();
			player.attack(rival);
			float lost = hp - rival.getHealth();
			if (Stance.finishers() != count + 1) {
				return "the full swing should have been a finisher";
			}
			double blow = ArtKit.weapon(player) * 1.6;
			double cap = Math.min(6 * StanceRules.PLAYER_SHARE, StanceRules.PVP_FINISHER * dev.wildercord.config.Config.get().aura().pvpScale());
			return lost <= blow + cap + 0.01 ? null : "a finisher on a player should add at most " + cap + " (" + lost + ")";
		});
		check(capped == null, capped);
		// A totem still saves them.
		context.waitTicks(FULL);
		String totem = on(world, player -> {
			Rival rival = rival(player);
			rival.removeAttached(Stance.STANCE);
			AuraApi.wearStance(player, rival, StanceRules.PLAYER_POOL);
			if (!Stance.opened(rival)) {
				return "worn through, they should open";
			}
			rival.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
			rival.setHealth(1.5F);
			dev.wildercord.cast.Effects.readyToHurt(rival);
			player.attack(rival);
			boolean saved = rival.isAlive() && rival.getHealth() > 0 && rival.getOffhandItem().isEmpty();
			rival.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
			rival.removeAllEffects();
			rival.setHealth(rival.getMaxHealth());
			return saved ? null : "a totem should still save a player from a finisher";
		});
		check(totem == null, totem);
		// A teammate is never touched.
		String team = on(world, player -> {
			Rival rival = rival(player);
			rival.removeAttached(Stance.STANCE);
			Scoreboard board = player.level().getScoreboard();
			PlayerTeam allies = board.addPlayerTeam("wildercord_allies");
			allies.setAllowFriendlyFire(false);
			board.addPlayerToTeam(player.getScoreboardName(), allies);
			board.addPlayerToTeam(rival.getScoreboardName(), allies);
			double wore = AuraApi.wearStance(player, rival, 10);
			board.removePlayerTeam(allies);
			return wore == 0 ? null : "a teammate's stance should never be worn (" + wore + ")";
		});
		check(team == null, team);
		// The rival's own stance bar over their head as their opponent sees it, then the seal once it breaks: a step further off,
		// as a duel is fought, once the finisher's banner and the totem's sparks have gone.
		on(world, player -> {
			Rival rival = rival(player);
			rival.removeAttached(Stance.STANCE);
			rival.removeAllEffects();
			Vec3 p = at(0, 3.0);
			rival.snapTo(p.x, p.y, p.z, 180, 0);
			rival.setYHeadRot(180);
			return null;
		});
		context.waitTicks(50);
		on(world, player -> {
			AuraApi.wearStance(player, rival(player), StanceRules.PLAYER_POOL * 0.7);
			return null;
		});
		context.waitTicks(6);
		boolean seen = context.computeOnClient(mc -> mc.level.players().stream().anyMatch(p -> p.getName().getString().equals("Rival")));
		if (seen) {
			int[] drawn = context.computeOnClient(mc -> StanceHud.drawn());
			check(drawn[0] >= 1, "the client should draw the other player's stance bar (" + drawn[0] + ")");
			shot(context, "stance_duel_fp");
			on(world, player -> {
				AuraApi.wearStance(player, rival(player), StanceRules.PLAYER_POOL);
				return null;
			});
			context.waitTicks(6);
			shot(context, "opened_duel_fp");
			thirdPerson(context, world, -55, 22, true);
			context.waitTicks(3);
			shot(context, "opened_duel_tp");
			firstPerson(context, world);
		}
		// Your own stance, worn in a duel, along the top of your aura strip.
		on(world, player -> {
			Rival rival = rival(player);
			rival.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("thunder", AuraRules.FORM, AuraRules.threshold(AuraRules.FORM), 60, 0));
			AuraApi.wearStance(rival, player, StanceRules.PLAYER_POOL * 0.6);
			setMomentumNow(player, 40);
			return null;
		});
		context.waitTicks(6);
		float own = context.computeOnClient(mc -> StanceHud.ownShown());
		check(own > 0.4, "the swordsman's own worn stance should show on their HUD (" + own + ")");
		shot(context, "stance_own_hud");
		on(world, player -> {
			player.removeAttached(Stance.STANCE);
			Rival rival = rival(player);
			rival.discard();
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(List.of(rival.getUUID())));
			return null;
		});
		rival = null;
	}

	// ------------------------------------------------------------------ the pages

	private static void pages(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			setMomentumNow(player, 80);
			return null;
		});
		context.waitTicks(5);
		shot(context, "momentum_hud_tier3");
		context.runOnClient(mc -> {
			AuraScreen.listArts(true);
			AuraScreen.browse(null);
		});
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		shot(context, "momentum_page_arts");
		context.runOnClient(mc -> AuraScreen.listArts(false));
		context.waitTicks(2);
		shot(context, "momentum_page_techniques");
		context.setScreen(() -> null);
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

	// ------------------------------------------------------------------ views

	private static void thirdPerson(ClientGameTestContext context, TestSingleplayerContext world, float pitch, boolean hud) {
		thirdPerson(context, world, 0.0F, pitch, hud);
	}

	/** Third person, the view turned {@code yaw} (a side view, so the foe isn't hidden behind the swordsman) and looking {@code pitch} down. */
	private static void thirdPerson(ClientGameTestContext context, TestSingleplayerContext world, float yaw, float pitch, boolean hud) {
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(5.0);
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
		world.getServer().runCommand("fill " + (x - 12) + " " + (y - 1) + " " + (z - 8) + " " + (x + 12) + " " + (y - 1) + " " + (z + 24) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 12) + " " + y + " " + (z - 8) + " " + (x + 12) + " " + (y + 10) + " " + (z + 24) + " minecraft:air");
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
		kill(world);
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runCommand("kill @e[type=block_display]");
		world.getServer().runCommand("fill " + (STAGE.getX() - 12) + " " + STAGE.getY() + " " + (STAGE.getZ() - 8) + " " + (STAGE.getX() + 12) + " "
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
		context.runOnClient(mc -> {
			mc.player.setXRot(8);
			mc.player.xRotO = 8;
		});
		// Long enough for any string to run out and the last blow to be forgotten.
		context.waitTicks(StringRules.ENGAGED_TICKS / 2);
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
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
	}

	/** Sets the swordsman's momentum to {@code value}, held (no ebb) for the rest of the scene. */
	private static void setMomentumNow(ServerPlayer player, double value) {
		player.setAttached(Momentum.MOMENTUM, new Momentum.State((float) value, player.level().getGameTime() + 100000, 0, 0, 0));
	}

	private static void setMomentum(TestSingleplayerContext world, double value) {
		on(world, player -> {
			setMomentumNow(player, value);
			return null;
		});
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
		return foes(player, EntityTypes.HUSK);
	}

	private static List<Mob> foes(ServerPlayer player, EntityType<?> type) {
		List<Mob> found = new ArrayList<>(player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48),
			m -> m.entityTags().contains(TAG) && m.getType() == type));
		found.sort(java.util.Comparator.comparingInt(Mob::getId));
		if (found.isEmpty()) {
			throw new AssertionError("the foes are gone");
		}
		return found;
	}

	/** Where each foe was stood, to put it back after a swing's knockback. */
	private static final Map<java.util.UUID, Vec3> SPOTS = new java.util.concurrent.ConcurrentHashMap<>();

	/** Puts the foes back where they stood after a swing's knockback (as a real foe presses in again), two ticks after it landed. */
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
			super(level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("wildercord-rival".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
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

	/** The duel's other player, in front of the swordsman. */
	private static Rival rival(ServerPlayer player) {
		if (rival == null || rival.isRemoved() || rival.level() != player.level()) {
			rival = new Rival(player.level());
			Vec3 p = at(0, 2.0);
			rival.snapTo(p.x, p.y, p.z, 180, 0);
			rival.setYHeadRot(180);
			// The swordsman's client is told who they are first, or it won't show a player it has never heard of.
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

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
