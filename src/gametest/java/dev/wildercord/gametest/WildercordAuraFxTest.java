package dev.wildercord.gametest;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.PlaceholderArts;
import dev.wildercord.aura.StringRules;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.client.AuraBanners;
import dev.wildercord.client.AuraFxClient;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.client.fx.HitStop;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.client.fx.ScreenEffects;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.player.Spellbooks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Aura's feel (the aura overhaul's step 2), in a real world on a stone platform in the sky, played with the real keys where a
 * player would use them:
 * <ul>
 *   <li>the body's aura at every stage, at rest and in a fight, by day and by night, in third person; and in first person nothing of
 *       it but a faint glow at the bottom edge while it flares (no motes or wisps let off round your own eyes);</li>
 *   <li>a coated swing draws your own trail at once, cut the way the swing was (a cut, a low sweep, a leaping fall), and lands with a
 *       flash and a hit-stop; a half swing lands light, without one;</li>
 *   <li>Flow's sweep is seen by the sweeper too (a sweep trail of their own), a perfect guard is a whisper of gold low in your own
 *       first-person view and a full burst for everyone else, the slash draws its blade, the step and Dominion flare the body's aura,
 *       Dominion and every art name themselves in a banner, and each art cuts its own trail and lands heavily;</li>
 *   <li>what others see of a swordsman: a technique's full trail and its banner over their head;</li>
 *   <li>the settings: trails, body aura, impact and banners each switched off leave out what they say, camera motion off takes the
 *       nudge away, and the performance profile softens them all;</li>
 *   <li>every method has its own three sounds, registered.</li>
 * </ul>
 * Screenshots ({@code aurafx_*}): the body by stage ({@code aurafx_body_<stage>_<method>_{rest,fight,night}}), first-person views
 * of each feature, third-person views of trails, the sweep, the perfect guard, the slash, the step, Dominion, the arts, an impact,
 * and banners (yours and over another's head).
 *
 * <p>Runs in the full suite; {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it.</p>
 */
public class WildercordAuraFxTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.aurafx_test";
	/** A sword's full swing comes back in 11 ticks. */
	private static final int FULL = 13;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		Settings saved = context.computeOnClient(mc -> Settings.now());
		context.runOnClient(mc -> {
			mc.getWindow().setWindowed(1920, 1080);
			mc.options.guiScale().set(2);
			mc.resizeGui();
			mc.options.toggleCrouch().set(false);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			MagicQuality.preset("cinematic");
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
			run(failures, "every method's sounds", () -> sounds(context));
			run(failures, "the body's aura by stage", () -> body(context, world));
			reset(context, world);
			run(failures, "your own swings' trails and impacts", () -> swings(context, world));
			reset(context, world);
			run(failures, "Flow's sweep", () -> sweep(context, world));
			reset(context, world);
			run(failures, "the perfect guard", () -> guard(context, world));
			reset(context, world);
			run(failures, "the slash, the step and Dominion", () -> techniques(context, world));
			reset(context, world);
			run(failures, "the arts", () -> arts(context, world));
			reset(context, world);
			run(failures, "what others see", () -> others(context, world));
			reset(context, world);
			run(failures, "the settings", () -> settings(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("Aura's feel went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			context.runOnClient(mc -> {
				saved.restore();
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.getInput().releaseKey(o -> o.keyShift);
		}
	}

	/** The player's visual settings, put back as they were when the test is done. */
	private record Settings(MagicQuality.Level own, MagicQuality.Level others, boolean flash, boolean shake, boolean titles,
			MagicQuality.StringIndicator indicator, MagicQuality.Trails trails, MagicQuality.BodyAura body, MagicQuality.Impact impact,
			MagicQuality.Banners banners) {
		static Settings now() {
			return new Settings(MagicQuality.own, MagicQuality.others, MagicQuality.reducedFlash, MagicQuality.cameraShake, MagicQuality.spellTitles,
				MagicQuality.stringIndicator, MagicQuality.bladeTrails, MagicQuality.bodyAura, MagicQuality.impact, MagicQuality.banners);
		}

		void restore() {
			MagicQuality.own = own;
			MagicQuality.others = others;
			MagicQuality.reducedFlash = flash;
			MagicQuality.cameraShake = shake;
			MagicQuality.spellTitles = titles;
			MagicQuality.stringIndicator = indicator;
			MagicQuality.bladeTrails = trails;
			MagicQuality.bodyAura = body;
			MagicQuality.impact = impact;
			MagicQuality.banners = banners;
			MagicQuality.save();
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

	// ------------------------------------------------------------------ sounds

	private static void sounds(ClientGameTestContext context) {
		String missing = context.computeOnClient(mc -> {
			for (var entry : AuraFx.families().entrySet()) {
				for (AuraFx.Sound sound : AuraFx.Sound.values()) {
					String name = entry.getValue().of(sound);
					if (WildercordSounds.kit(name) == null) {
						return entry.getKey() + "'s " + name;
					}
				}
			}
			for (AuraFx.Sound sound : AuraFx.Sound.values()) {
				if (WildercordSounds.kit(AuraFx.STEEL.of(sound)) == null) {
					return AuraFx.STEEL.of(sound);
				}
			}
			return null;
		});
		check(missing == null, "every method's sounds should be registered (missing " + missing + ")");
	}

	// ------------------------------------------------------------------ the body's aura

	/** Each stage in its own method's colour, at rest and in a fight, by day; and by night in a fight. */
	private static void body(ClientGameTestContext context, TestSingleplayerContext world) {
		String[] methods = {"ember", "rime", "thunder", "verdant", "hollow"};
		String[] stages = {"glow", "flow", "edge", "form", "sovereign"};
		for (int stage = AuraRules.GLOW; stage <= AuraRules.SOVEREIGN; stage++) {
			String method = methods[stage - 1];
			int s = stage;
			on(world, player -> {
				stand(player);
				setAura(player, method, s, AuraRules.capacity(s));
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
				player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(2.9);
				// Facing the camera (third person, in front), a little from above.
				player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 200.0F, 12.0F, false);
				player.setAttached(AuraPresence.LOOK, AuraPresence.look(player).fightUntil(0));
				return null;
			});
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			world.getServer().runCommand("time set 3000");
			context.waitTicks(30);
			float rest = context.computeOnClient(mc -> AuraFxClient.bodyIntensity(mc.player, mc.level.getGameTime()));
			check(Math.abs(rest - AuraFxRules.IDLE) < 0.01F, stages[s - 1] + ": at rest the body's aura should be calm (" + rest + ")");
			shot(context, "aurafx_body_" + stages[s - 1] + "_" + method + "_rest");
			int motes = context.computeOnClient(mc -> AuraFxClient.counts()[5]);
			fight(world, 400);
			context.waitTicks(30);
			float fighting = context.computeOnClient(mc -> AuraFxClient.bodyIntensity(mc.player, mc.level.getGameTime()));
			check(Math.abs(fighting - AuraFxRules.FIGHTING) < 0.01F, stages[s - 1] + ": in a fight it should flare (" + fighting + ")");
			shot(context, "aurafx_body_" + stages[s - 1] + "_" + method + "_fight");
			int after = context.computeOnClient(mc -> AuraFxClient.counts()[5]);
			check(after > motes, stages[s - 1] + ": in third person the body should let off motes and wisps (" + (after - motes) + ")");
			world.getServer().runCommand("time set 18000");
			context.waitTicks(20);
			shot(context, "aurafx_body_" + stages[s - 1] + "_" + method + "_night");
			if (s == AuraRules.SOVEREIGN || s == AuraRules.FORM) {
				// Seen from behind, the mantle and the corona stream up off the back.
				context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
				context.waitTicks(10);
				shot(context, "aurafx_body_" + stages[s - 1] + "_" + method + "_back");
			}
		}
		// In first person, at Sovereign in a fight: none of it round your own eyes, only the whisper at the bottom of the screen.
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		on(world, player -> {
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 6.0F, false);
			return null;
		});
		fight(world, 400);
		context.waitTicks(10);
		int before = context.computeOnClient(mc -> AuraFxClient.counts()[5]);
		context.waitTicks(40);
		int fp = context.computeOnClient(mc -> AuraFxClient.counts()[5]);
		check(fp == before, "in first person no motes or wisps should rise round your own eyes (" + (fp - before) + ")");
		shot(context, "aurafx_body_sovereign_fp_night");
		world.getServer().runCommand("time set 3000");
		context.waitTicks(5);
		shot(context, "aurafx_body_sovereign_fp_day");
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			return null;
		});
	}

	/** The player is in a fight for {@code ticks}: their body's aura flares (as a blow given or taken would make it). */
	private static void fight(TestSingleplayerContext world, int ticks) {
		on(world, player -> {
			player.setAttached(AuraPresence.LOOK, AuraPresence.look(player).fightUntil(player.level().getGameTime() + ticks));
			return null;
		});
	}

	// ------------------------------------------------------------------ your own swings

	/** At Glow (from Flow a full swing at a creature sweeps, which the sweep's own section films). */
	private static void swings(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "ember", AuraRules.GLOW, AuraRules.capacity(AuraRules.GLOW));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 400).addTag("wildercord.aurafx_target");
			return null;
		});
		context.waitTicks(20);
		int[] before = context.computeOnClient(mc -> AuraFxClient.counts());
		int stops = context.computeOnClient(mc -> HitStop.stops());
		swing(context);
		context.waitTicks(1);
		shot(context, "aurafx_swing_cut_fp");
		context.waitTicks(3);
		int[] after = context.computeOnClient(mc -> AuraFxClient.counts());
		String stroke = context.computeOnClient(mc -> AuraFxClient.lastOwnStroke());
		check(after[1] == before[1] + 1 && stroke.equals("cut"), "a full coated swing should draw your own cut at once (" + (after[1] - before[1])
			+ " trails, " + stroke + ")");
		check(after[2] > before[2], "the coated blow should land as an impact on this client");
		int stopped = context.computeOnClient(mc -> HitStop.stops());
		check(stopped > stops, "a full coated blow should hold the moment (a hit-stop)");
		// A half swing: light, no hit-stop (and a fresh string so nothing plays).
		context.waitTicks(4);
		swing(context);
		context.waitTicks(4);
		int light = context.computeOnClient(mc -> HitStop.stops());
		check(light == stopped, "a half swing lands light, without a hit-stop");
		context.waitTicks(FULL + 20);
		// A low swing cuts low across the legs; filmed from behind.
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		lowSwing(context);
		context.waitTicks(2);
		shot(context, "aurafx_swing_low_tp");
		context.getInput().releaseKey(o -> o.keyShift);
		String low = context.computeOnClient(mc -> AuraFxClient.lastOwnStroke());
		check(low.equals("low"), "a low swing should cut low (" + low + ")");
		context.waitTicks(FULL + 20);
		// A full cut and an impact, from behind.
		swing(context);
		context.waitTicks(2);
		shot(context, "aurafx_swing_cut_tp");
		context.waitTicks(FULL);
		// A leaping swing comes down from above.
		context.runOnClient(mc -> {
			mc.player.setXRot(25);
			mc.player.xRotO = 25;
		});
		context.getInput().holdKeyFor(o -> o.keyJump, 1);
		context.waitTicks(4);
		swing(context);
		context.waitTicks(2);
		shot(context, "aurafx_swing_leap_tp");
		String leap = context.computeOnClient(mc -> AuraFxClient.lastOwnStroke());
		check(leap.equals("falling"), "a leaping swing should come down from above (" + leap + ")");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		context.waitTicks(20);
		// Your own leaping cut, in first person: it comes down past the bottom of the view, not through its middle.
		context.getInput().holdKeyFor(o -> o.keyJump, 1);
		context.waitTicks(4);
		swing(context);
		context.waitTicks(2);
		shot(context, "aurafx_swing_leap_fp");
		String leapOwn = context.computeOnClient(mc -> AuraFxClient.lastOwnStroke());
		check(leapOwn.equals("falling"), "a leaping swing in first person should come down from above (" + leapOwn + ")");
		context.waitTicks(FULL + 20);
		// Looking well down at a foe: your own cut tips with the look, still low and clear of the middle of the view.
		context.runOnClient(mc -> {
			mc.player.setXRot(30);
			mc.player.xRotO = 30;
		});
		context.waitTicks(10);
		int[] down = context.computeOnClient(mc -> AuraFxClient.counts());
		swing(context);
		context.waitTicks(1);
		shot(context, "aurafx_swing_down_fp");
		int[] downAfter = context.computeOnClient(mc -> AuraFxClient.counts());
		check(downAfter[1] == down[1] + 1, "a cut looking down at a foe should draw your own trail (" + (downAfter[1] - down[1]) + ")");
		context.waitTicks(FULL + 20);
		// A run of cuts at night in first person: each cuts back the other way.
		world.getServer().runCommand("time set 18000");
		context.runOnClient(mc -> {
			mc.player.setXRot(8);
			mc.player.xRotO = 8;
		});
		context.waitTicks(20);
		swing(context);
		context.waitTicks(FULL);
		swing(context);
		context.waitTicks(1);
		shot(context, "aurafx_swing_run_fp_night");
		world.getServer().runCommand("time set 3000");
	}

	// ------------------------------------------------------------------ Flow's sweep

	private static void sweep(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "gale", AuraRules.FLOW, AuraRules.capacity(AuraRules.FLOW));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.0), 400).addTag("wildercord.aurafx_target");
			spawn(player.level(), EntityTypes.HUSK, at(-1.6, 2.3), 400);
			spawn(player.level(), EntityTypes.HUSK, at(1.6, 2.3), 400);
			return null;
		});
		context.waitTicks(30);
		String before = context.computeOnClient(mc -> AuraFxClient.lastOwnStroke());
		int[] counts = context.computeOnClient(mc -> AuraFxClient.counts());
		swing(context);
		context.waitTicks(2);
		shot(context, "aurafx_sweep_fp");
		context.waitTicks(2);
		String stroke = context.computeOnClient(mc -> AuraFxClient.lastOwnStroke());
		int[] after = context.computeOnClient(mc -> AuraFxClient.counts());
		check(stroke.equals("sweep") && after[1] == counts[1] + 1, "the sweeper should see their own sweep, once (" + before + " -> " + stroke
			+ ", " + (after[1] - counts[1]) + " own trails)");
		context.waitTicks(30);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(5.0);
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 30.0F, false);
			return null;
		});
		context.waitTicks(10);
		swing(context);
		context.waitTicks(2);
		shot(context, "aurafx_sweep_tp");
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			return null;
		});
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
	}

	// ------------------------------------------------------------------ the perfect guard

	private static void guard(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "thunder", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 1.6), 400).addTag("wildercord.aurafx_target");
			return null;
		});
		context.waitTicks(20);
		for (int view = 0; view < 2; view++) {
			boolean firstPerson = view == 0;
			context.runOnClient(mc -> mc.options.setCameraType(firstPerson ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK));
			context.waitTicks(5);
			int[] before = context.computeOnClient(mc -> AuraFxClient.counts());
			context.getInput().holdKey(o -> o.keyShift);
			context.waitTicks(2);
			context.getInput().pressKey(WildercordKeys.auraMapping());
			for (int t = 0; t < 4 && !on(world, player -> AuraGuard.perfectNow(player)); t++) {
				context.waitTicks(1);
			}
			float taken = on(world, player -> {
				Mob husk = tagged(player, "wildercord.aurafx_target");
				float health = player.getHealth();
				husk.doHurtTarget(player.level(), player);
				return health - player.getHealth();
			});
			check(taken <= 0, "the guard should have been perfect (took " + taken + ")");
			context.waitTicks(2);
			shot(context, firstPerson ? "aurafx_guard_fp" : "aurafx_guard_tp");
			int[] after = context.computeOnClient(mc -> AuraFxClient.counts());
			check(after[3] >= before[3] + 2, "a perfect guard should burst (its gold, and the aura's own ring)");
			check(after[4] > before[4], "a perfect guard should flare the body's aura");
			if (firstPerson) {
				check(after[6] >= before[6] + 2, "in your own first person the guard's bursts should be drawn as a whisper, low (" + (after[6] - before[6]) + ")");
			} else {
				check(after[6] == before[6], "in third person the guard's bursts should be drawn whole");
			}
			context.getInput().releaseKey(o -> o.keyShift);
			on(world, player -> {
				player.removeAttached(AuraAttachments.STATE);
				return null;
			});
			context.waitTicks(30);
		}
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
	}

	// ------------------------------------------------------------------ the slash, the step and Dominion

	private static void techniques(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "hollow", AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			for (int i = 0; i < 3; i++) {
				spawn(player.level(), EntityTypes.HUSK, at(-2 + 2 * i, 7), 400);
			}
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.5);
			return null;
		});
		world.getServer().runCommand("time set 18000");
		context.waitTicks(20);
		// The slash, from behind (a tap of the Aura key: Sovereign waits a moment in case it's a double tap).
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(5);
		int[] before = context.computeOnClient(mc -> AuraFxClient.counts());
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(12);
		shot(context, "aurafx_slash_tp");
		String slash = context.computeOnClient(mc -> AuraFxClient.lastOwnStroke());
		int[] after = context.computeOnClient(mc -> AuraFxClient.counts());
		check(slash.equals("draw") && after[1] > before[1], "the slash should draw its blade, for the swordsman too (" + slash + ")");
		context.waitTicks(45);
		// The slash in first person: its draw low and thin.
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		context.waitTicks(5);
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(11);
		shot(context, "aurafx_slash_fp");
		context.waitTicks(45);
		// The step: a double tap; the body's aura surges.
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		on(world, player -> {
			stand(player);
			return null;
		});
		context.waitTicks(10);
		int flares = context.computeOnClient(mc -> AuraFxClient.counts()[4]);
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(2);
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(3);
		shot(context, "aurafx_step_tp");
		int stepped = context.computeOnClient(mc -> AuraFxClient.counts()[4]);
		check(stepped > flares, "the step should surge the body's aura");
		context.waitTicks(40);
		// Dominion: held; its banner, grand.
		on(world, player -> {
			stand(player);
			setAura(player, "hollow", AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN));
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(6.0);
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 35.0F, false);
			return null;
		});
		context.waitTicks(10);
		context.getInput().holdKey(WildercordKeys.auraMapping());
		context.waitTicks(8);
		context.getInput().releaseKey(WildercordKeys.auraMapping());
		context.waitTicks(2);
		shot(context, "aurafx_dominion_tp");
		check(context.computeOnClient(mc -> dev.wildercord.client.fx.AuraGroundScar.showing() > 0),
			"Dominion must leave physical ground scars");
		context.runOnClient(mc -> {
			int scarsBefore = dev.wildercord.client.fx.AuraGroundScar.showing();
			dev.wildercord.client.fx.AuraGroundScar.receive(new AuraFx.GroundScar(new Vec3(100, 250, 100), 3, 20, 1));
			check(dev.wildercord.client.fx.AuraGroundScar.showing() == scarsBefore, "A ground scar must not float over open air");
		});
		String banner = context.computeOnClient(mc -> AuraBanners.ownShowing());
		String expected = context.computeOnClient(mc -> Component.translatable("aura.wildercord.technique.dominion").getString());
		check(banner.equals(expected), "Dominion should name itself in a banner (" + banner + ")");
		boolean surging = context.computeOnClient(mc -> AuraFxClient.surging(mc.player.getId()));
		check(surging, "Dominion should surge the body's aura");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		on(world, player -> {
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 10.0F, false);
			return null;
		});
		context.waitTicks(4);
		shot(context, "aurafx_dominion_fp");
		world.getServer().runCommand("time set 3000");
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			return null;
		});
		// The circle's light runs its whole time on each client: let it go before the next pictures.
		context.waitTicks(170);
	}

	// ------------------------------------------------------------------ the arts

	/**
	 * Each common art performed (its keys are tested in WildercordSwordStringsTest): its trail, banner and impacts. Played by a
	 * method the test makes without arts of its own (every built-in method has its own: WildercordArtsTest films those).
	 */
	private static void arts(ClientGameTestContext context, TestSingleplayerContext world) {
		String plain = TestMethods.plain();
		String[] methods = {plain, plain, plain, plain, plain};
		String[] strokes = {"cut", "rising", "cross", "thrust", "spin"};
		for (int i = 0; i < PlaceholderArts.IDS.size(); i++) {
			String id = PlaceholderArts.IDS.get(i);
			String method = methods[i];
			boolean night = i % 2 == 1;
			world.getServer().runCommand(night ? "time set 18000" : "time set 3000");
			on(world, player -> {
				kill(player, TAG);
				stand(player);
				setAura(player, method, AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN));
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
				player.removeAttached(SwordStrings.COOLDOWNS);
				spawn(player.level(), EntityTypes.HUSK, at(-1.6, 2.4), 400);
				spawn(player.level(), EntityTypes.HUSK, at(1.6, 2.4), 400);
				spawn(player.level(), EntityTypes.HUSK, at(0, 3.0), 400);
				player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(5.0);
				player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 30.0F, false);
				return null;
			});
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(12);
			int[] before = context.computeOnClient(mc -> AuraFxClient.counts());
			String done = on(world, player -> {
				AuraApi.StringArt art = AuraApi.string(id).orElseThrow();
				List<Integer> marks = art.string().tokens().stream().map(t -> SwordString.Token.marks(t)).toList();
				return SwordStrings.perform(player, art, marks) ? null : id + " didn't go off";
			});
			check(done == null, done);
			context.waitTicks(3 + styleWindup(id));
			shot(context, "aurafx_art_" + (i + 1) + (night ? "_night" : "_day"));
			int[] after = context.computeOnClient(mc -> AuraFxClient.counts());
			String stroke = context.computeOnClient(mc -> AuraFxClient.lastOwnStroke());
			String banner = context.computeOnClient(mc -> AuraBanners.ownShowing());
			String name = context.computeOnClient(mc -> Component.translatable("aura.wildercord.art." + id).getString());
			check(stroke.equals(strokes[i]), id + " should cut a " + strokes[i] + " (" + stroke + ")");
			check(banner.equals(name), id + " should name itself in a banner (" + banner + ")");
			check(after[2] > before[2], id + " should land on the husks as impacts");
			check(after[4] > before[4], id + " should surge the body's aura");
			if (i == 0 || i == 4) {
				// And from inside it: the trail thin and low, the banner by the left edge.
				context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
				on(world, player -> {
					player.removeAttached(SwordStrings.COOLDOWNS);
					setAura(player, method, AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN));
					player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 8.0F, false);
					return null;
				});
				context.waitTicks(10);
				on(world, player -> {
					AuraApi.StringArt art = AuraApi.string(id).orElseThrow();
					SwordStrings.perform(player, art, art.string().tokens().stream().map(t -> SwordString.Token.marks(t)).toList());
					return null;
				});
				context.waitTicks(3 + styleWindup(id));
				shot(context, "aurafx_art_" + (i + 1) + "_fp");
			}
		}
		world.getServer().runCommand("time set 3000");
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			return null;
		});
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
	}

	// ------------------------------------------------------------------ what others see

	/**
	 * What another swordsman looks like from here: a husk with a sword stands in for them (only one real player is in this world),
	 * a technique's trail laid round it whole, and its banner over its head.
	 */
	private static void others(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "stone", AuraRules.GLOW, AuraRules.capacity(AuraRules.GLOW));
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0.6, 3.2), 400);
			husk.addTag("wildercord.aurafx_other");
			husk.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));
			husk.snapTo(husk.getX(), husk.getY(), husk.getZ(), 150, 0);
			husk.setYHeadRot(150);
			husk.yBodyRot = 150;
			return null;
		});
		world.getServer().runCommand("time set 18000");
		context.waitTicks(20);
		int[] before = context.computeOnClient(mc -> AuraFxClient.counts());
		on(world, player -> {
			Mob husk = tagged(player, "wildercord.aurafx_other");
			AuraFx.banner(husk, Component.translatable("aura.wildercord.art.second_art"), Component.literal("Crimson Breath"), 0xD2283C,
				AuraFxRules.BannerKind.ART);
			AuraFx.trail(husk, AuraFxRules.Stroke.CUT, false, 0xD2283C, AuraRules.SOVEREIGN, 1.3F);
			return null;
		});
		context.waitTicks(3);
		shot(context, "aurafx_others_trail_banner");
		int[] after = context.computeOnClient(mc -> AuraFxClient.counts());
		int showing = context.computeOnClient(mc -> AuraBanners.othersShowing());
		check(after[0] > before[0] && after[1] == before[1], "another's technique trail should be drawn, and not as your own");
		check(showing == 1, "another's banner should float over them (" + showing + ")");
		world.getServer().runCommand("time set 3000");
	}

	// ------------------------------------------------------------------ the settings

	private static void settings(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "verdant", AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 400).addTag("wildercord.aurafx_target");
			return null;
		});
		context.waitTicks(20);
		// Trails off: no trail for your swing (the swing still lands).
		context.runOnClient(mc -> MagicQuality.bladeTrails = MagicQuality.Trails.OFF);
		int own = context.computeOnClient(mc -> AuraFxClient.counts()[1]);
		swing(context);
		context.waitTicks(FULL + 4);
		int trails = context.computeOnClient(mc -> AuraFxClient.counts()[1]);
		check(trails == own, "with trails off, no trail should be drawn");
		context.runOnClient(mc -> MagicQuality.bladeTrails = MagicQuality.Trails.FULL);
		// Impact off: no hit-stop. Camera motion off: no nudge.
		context.runOnClient(mc -> MagicQuality.impact = MagicQuality.Impact.OFF);
		int stops = context.computeOnClient(mc -> HitStop.stops());
		swing(context);
		context.waitTicks(FULL + 4);
		check(context.computeOnClient(mc -> HitStop.stops()) == stops, "with impact off, no hit-stop");
		context.runOnClient(mc -> {
			MagicQuality.impact = MagicQuality.Impact.FULL;
			MagicQuality.cameraShake = false;
		});
		swing(context);
		context.waitTicks(1);
		boolean nudged = context.computeOnClient(mc -> ScreenEffects.nudging());
		check(!nudged, "with camera motion off, no nudge");
		context.waitTicks(FULL + 4);
		context.runOnClient(mc -> MagicQuality.cameraShake = true);
		swing(context);
		boolean nudge = false;
		for (int t = 0; t < 3 && !nudge; t++) {
			context.waitTicks(1);
			nudge = context.computeOnClient(mc -> ScreenEffects.nudging());
		}
		check(nudge, "with camera motion on, a full blow nudges the view");
		context.waitTicks(FULL + 4);
		// Body aura off: nothing, not even its intensity.
		context.runOnClient(mc -> MagicQuality.bodyAura = MagicQuality.BodyAura.OFF);
		float off = context.computeOnClient(mc -> AuraFxClient.bodyIntensity(mc.player, mc.level.getGameTime()));
		check(off == 0, "with the body's aura off it shouldn't show (" + off + ")");
		context.runOnClient(mc -> MagicQuality.bodyAura = MagicQuality.BodyAura.CALM);
		fight(world, 200);
		context.waitTicks(3);
		float calm = context.computeOnClient(mc -> AuraFxClient.bodyIntensity(mc.player, mc.level.getGameTime()));
		check(Math.abs(calm - AuraFxRules.IDLE) < 0.01F, "calm, it shouldn't flare even in a fight (" + calm + ")");
		context.runOnClient(mc -> MagicQuality.bodyAura = MagicQuality.BodyAura.FULL);
		// Banners off: an art names nothing.
		context.runOnClient(mc -> MagicQuality.banners = MagicQuality.Banners.OFF);
		int shown = context.computeOnClient(mc -> AuraBanners.shown());
		on(world, player -> {
			player.removeAttached(SwordStrings.COOLDOWNS);
			AuraApi.StringArt art = AuraApi.string(PlaceholderArts.FIRST).orElseThrow();
			SwordStrings.perform(player, art, art.string().tokens().stream().map(t -> SwordString.Token.marks(t)).toList());
			return null;
		});
		context.waitTicks(3);
		check(context.computeOnClient(mc -> AuraBanners.shown()) == shown, "with banners off, no banner");
		// The performance profile softens it all.
		context.runOnClient(mc -> MagicQuality.preset("performance"));
		String profile = context.computeOnClient(mc -> MagicQuality.bladeTrails + " " + MagicQuality.bodyAura + " " + MagicQuality.impact + " "
			+ MagicQuality.banners);
		check(profile.equals("SUBTLE CALM SOFT OWN"), "the performance profile should soften aura's feel (" + profile + ")");
		context.runOnClient(mc -> MagicQuality.preset("cinematic"));
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

	// ------------------------------------------------------------------ the stage

	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 12) + " " + (y - 1) + " " + (z - 8) + " " + (x + 12) + " " + (y - 1) + " " + (z + 26) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 12) + " " + y + " " + (z - 8) + " " + (x + 12) + " " + (y + 6) + " " + (z + 26) + " minecraft:air");
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
	}

	private static void reset(ClientGameTestContext context, TestSingleplayerContext world) {
		context.getInput().releaseKey(o -> o.keyShift);
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			player.removeAttached(AuraAttachments.STATE);
			player.removeAttached(SwordStrings.COOLDOWNS);
			player.setAttached(AuraPresence.TIMERS, AuraPresence.Timers.NONE);
			player.removeAttached(AuraPresence.LOOK);
			dev.wildercord.aura.AuraDominion.end(player);
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(StringRules.ENGAGED_TICKS / 2);
	}

	private static void setAura(ServerPlayer player, String method, int stage, float aura) {
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(method, stage, AuraRules.threshold(stage), aura, 0));
		player.removeAttached(AuraAttachments.STATE);
	}

	private static Vec3 at(double side, double ahead) {
		return new Vec3(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead);
	}

	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, Vec3 at, double health) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		if (mob == null) {
			throw new AssertionError("couldn't make a " + type);
		}
		mob.snapTo(at.x, at.y, at.z, 180, 0);
		mob.setNoAi(true);
		mob.addTag(TAG);
		mob.addTag("wildercord.rolled");
		AttributeInstance max = mob.getAttribute(Attributes.MAX_HEALTH);
		if (max != null) {
			max.setBaseValue(health);
		}
		mob.setHealth((float) health);
		level.addFreshEntity(mob);
		return mob;
	}

	private static Mob tagged(ServerPlayer player, String tag) {
		List<Mob> found = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48), m -> m.entityTags().contains(tag) && m.isAlive());
		if (found.isEmpty()) {
			throw new AssertionError("the mob tagged " + tag + " is gone");
		}
		return found.getFirst();
	}

	private static void kill(ServerPlayer player, String tag) {
		for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64), e -> e.entityTags().contains(tag))) {
			e.discard();
		}
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
	private static int styleWindup(String id) {
		var style = dev.wildercord.aura.MastersStyleRules.of(id);
		return style == null ? 0 : style.windup();
	}

}
