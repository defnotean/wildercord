package dev.wildercord.gametest;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.PlaceholderArts;
import dev.wildercord.aura.StringRules;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.client.StringHud;
import dev.wildercord.client.SwordStringsClient;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Sword strings, played with the real keys as a player would (the attack key, sneak, jump, and the Aura key for the guard and
 * the step), on a stone platform in the sky, against husks that stand still:
 * <ul>
 *   <li>the five arts are on both sides, on their strings;</li>
 *   <li>swing, swing, low swing plays the First Art: the client reads it and asks, the server performs it (the hook hears it),
 *       spends its price, rests it, and writes the first string in the Grimoire; the indicator shows the marks and lights;</li>
 *   <li>played again at once, it's resting: the client refuses it itself, with the reason;</li>
 *   <li>a pause breaks a string, and the low swing that comes too late is a fumble; swings at nothing in peace, at a block, or
 *       as a spectator count for nothing;</li>
 *   <li>Flow's leaping swing then a low one plays the Second Art; Edge's counter (a swing straight after a perfect guard) the
 *       Third; Form's step cut (a swing straight after an Aura Step) the Fourth; Sovereign's three full swings and a low one, with
 *       a full pool, the Final Art, and without one falls through to the First;</li>
 *   <li>the server refuses what the client can't show it did: a string sent with no swings behind it, an art above the stage, and
 *       one with no blade in hand; with strings switched off nothing is read;</li>
 *   <li>the indicator by the hotbar, and the Aura page listing the arts with their strings.</li>
 * </ul>
 * Screenshots ({@code string_*}): the marks mid-string, completed, fumbled and refused in first person, a counter's waiting mark,
 * each art in third person, the indicator by the hotbar, and the Aura page.
 *
 * <p>Runs in the full suite; {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it.</p>
 */
public class WildercordSwordStringsTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.string_test";
	/** The arts the server performed, in order (an {@link AuraApi#onString} hook's record). */
	private static final List<String> PERFORMED = Collections.synchronizedList(new ArrayList<>());
	/** The creatures aura off a blade landed on (an art's burst: nothing else here projects aura), by id. */
	private static final List<java.util.UUID> ART_HITS = Collections.synchronizedList(new ArrayList<>());
	private static boolean hooked;
	/** A sword's full swing comes back in 11 ticks: this is a swing as soon as it's full again. */
	private static final int FULL = 13;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		if (!hooked) {
			hooked = true;
			AuraApi.onString((player, art, ctx) -> PERFORMED.add(art.id()));
			net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
				if (source.is(Aura.DAMAGE) && taken > 0 && entity.entityTags().contains(TAG)) {
					ART_HITS.add(entity.getUUID());
				}
			});
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
			run(failures, "the arts and their strings", () -> registry(context));
			run(failures, "the First Art", () -> first(context, world));
			reset(context, world);
			run(failures, "pauses, fumbles and swings that count for nothing", () -> fumbles(context, world));
			reset(context, world);
			run(failures, "the Second Art", () -> second(context, world));
			reset(context, world);
			run(failures, "the Third Art", () -> third(context, world));
			reset(context, world);
			run(failures, "the Fourth Art", () -> fourth(context, world));
			reset(context, world);
			run(failures, "the Final Art", () -> last(context, world));
			reset(context, world);
			run(failures, "the server's checks", () -> server(context, world));
			reset(context, world);
			run(failures, "the indicator by the hotbar and the Aura page", () -> views(context, world));
			reset(context, world);
			run(failures, "how the arts look", () -> looks(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("Sword strings went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				MagicQuality.stringIndicator = MagicQuality.StringIndicator.CROSSHAIR;
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

	// ------------------------------------------------------------------ the registry

	private static void registry(ClientGameTestContext context) {
		String problem = context.computeOnClient(mc -> {
			List<String> want = List.of("swing swing low", "leap low", "counter", "step", "full full full low");
			for (int i = 0; i < PlaceholderArts.IDS.size(); i++) {
				AuraApi.StringArt art = AuraApi.string(PlaceholderArts.IDS.get(i)).orElse(null);
				if (art == null) {
					return PlaceholderArts.IDS.get(i) + " isn't registered on the client";
				}
				if (!art.string().text().equals(want.get(i)) || art.stage() != i + 1) {
					return art.id() + " should be '" + want.get(i) + "' at stage " + (i + 1) + " (it's '" + art.string() + "' at " + art.stage() + ")";
				}
			}
			return null;
		});
		check(problem == null, problem);
		String server = PlaceholderArts.IDS.stream().allMatch(id -> AuraApi.string(id).isPresent()) ? null : "the arts aren't registered on the server";
		check(server == null, server);
	}

	// ------------------------------------------------------------------ the First Art

	private static void first(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "ember", AuraRules.GLOW, AuraRules.capacity(AuraRules.GLOW));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200).addTag("wildercord.string_target");
			return null;
		});
		context.waitTicks(20);
		int[] before = context.computeOnClient(mc -> SwordStringsClient.counts());
		PERFORMED.clear();
		swing(context);
		context.waitTicks(FULL);
		swing(context);
		context.waitTicks(3);
		int chain = context.computeOnClient(mc -> SwordStringsClient.chain().size());
		check(chain == 2, "two swings at the husk should be on the string (" + chain + ")");
		String live = context.computeOnClient(mc -> StringHud.phase());
		check(live.equals("live"), "the indicator should show the string being played (" + live + ")");
		shot(context, "string_live_fp");
		context.waitTicks(FULL - 5);
		float aura = on(world, player -> Aura.aura(player));
		ART_HITS.clear();
		lowSwing(context);
		context.waitTicks(1);
		shot(context, "string_complete_fp");
		context.waitTicks(3);
		String asked = context.computeOnClient(mc -> SwordStringsClient.lastAsked());
		int[] after = context.computeOnClient(mc -> SwordStringsClient.counts());
		check(after[0] == before[0] + 1 && asked.equals(PlaceholderArts.FIRST), "the client should have asked for the First Art (asked " + asked
			+ ", " + (after[0] - before[0]) + " times)");
		check(PERFORMED.equals(List.of(PlaceholderArts.FIRST)), "the server should have performed the First Art, once (" + PERFORMED + ")");
		String done = on(world, player -> {
			long now = player.level().getGameTime();
			long ready = SwordStrings.readyAt(player, PlaceholderArts.FIRST);
			if (ready <= now || ready > now + StringRules.FIRST_COOLDOWN) {
				return "the First Art should rest " + StringRules.FIRST_COOLDOWN + " ticks (ready at " + ready + ", now " + now + ")";
			}
			// Its price, and the low swing's own coat if the swing gave nothing back.
			float spent = aura - Aura.aura(player);
			if (spent < StringRules.FIRST_COST - 0.05 || spent > StringRules.FIRST_COST + AuraRules.COAT_COST + 0.05) {
				return "the First Art should cost " + StringRules.FIRST_COST + " aura (spent " + spent + ")";
			}
			if (!ART_HITS.contains(tagged(player, "wildercord.string_target").getUUID())) {
				return "the art's arc should cut the husk in front (aura landed on " + ART_HITS + ")";
			}
			return Heart.grimoire(player).contains("aura:sword_string") ? null : "the first string should go into the Grimoire";
		});
		check(done == null, done);
		boolean resting = context.computeOnClient(mc -> SwordStrings.readyAt(mc.player, PlaceholderArts.FIRST) > mc.level.getGameTime());
		check(resting, "the client should know the First Art rests");
		String lit = context.computeOnClient(mc -> StringHud.phase());
		check(lit.equals("done"), "the indicator should light the completed string (" + lit + ")");

		// Played again at once: it's resting, so the client refuses it itself (with the reason) and asks nothing.
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(10);
		int refusals = context.computeOnClient(mc -> SwordStringsClient.counts()[2]);
		swing(context);
		context.waitTicks(FULL);
		swing(context);
		context.waitTicks(FULL - 5);
		lowSwing(context);
		context.waitTicks(1);
		shot(context, "string_refused_fp");
		int[] again = context.computeOnClient(mc -> SwordStringsClient.counts());
		String refused = context.computeOnClient(mc -> SwordStringsClient.lastRefused());
		check(again[2] == refusals + 1 && refused.equals(PlaceholderArts.FIRST), "a resting First Art should be refused on the client (" + refused + ")");
		check(again[0] == after[0], "a refused string shouldn't be asked for");
		check(PERFORMED.size() == 1, "nothing more should have been performed (" + PERFORMED + ")");
	}

	// ------------------------------------------------------------------ fumbles and swings that don't count

	private static void fumbles(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "rime", AuraRules.GLOW, AuraRules.capacity(AuraRules.GLOW));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200).addTag("wildercord.string_target");
			return null;
		});
		context.waitTicks(20);
		PERFORMED.clear();
		int[] before = context.computeOnClient(mc -> SwordStringsClient.counts());
		swing(context);
		context.waitTicks(FULL);
		swing(context);
		// A breath too long: the string runs out, and the low swing comes late.
		int window = StringRules.window(StringRules.WINDOW, StringRules.recover(12.5));
		context.waitTicks(window + 2);
		String lapsed = context.computeOnClient(mc -> SwordStringsClient.chain().isEmpty() ? null : "the string should have run out");
		check(lapsed == null, lapsed);
		lowSwing(context);
		context.waitTicks(2);
		shot(context, "string_fumble_fp");
		int[] after = context.computeOnClient(mc -> SwordStringsClient.counts());
		String fumbled = context.computeOnClient(mc -> SwordStringsClient.lastFumbled());
		check(after[1] == before[1] + 1 && fumbled.equals(PlaceholderArts.FIRST), "the late low swing should be a fumble of the First Art (" + fumbled + ")");
		check(after[0] == before[0] && PERFORMED.isEmpty(), "a fumble asks for nothing (" + PERFORMED + ")");
		String phase = context.computeOnClient(mc -> StringHud.phase());
		check(phase.equals("fumble"), "the indicator should show the fumble (" + phase + ")");
		context.getInput().releaseKey(o -> o.keyShift);

		// At nothing, in peace: no husk, long after the last blow. Swings at the open sky count for nothing.
		on(world, player -> {
			kill(player, TAG);
			return null;
		});
		context.waitTicks(StringRules.ENGAGED_TICKS + 5);
		int strokes = context.computeOnClient(mc -> SwordStringsClient.counts()[3]);
		for (int i = 0; i < 3; i++) {
			swing(context);
			context.waitTicks(4);
		}
		int idle = context.computeOnClient(mc -> SwordStringsClient.counts()[3]);
		check(idle == strokes, "swings at nothing in peace shouldn't count (" + (idle - strokes) + " did)");

		// At a block: digging, not fighting.
		world.getServer().runCommand("setblock " + STAGE.getX() + " " + STAGE.getY() + " " + (STAGE.getZ() + 2) + " minecraft:oak_planks");
		context.waitTicks(3);
		context.runOnClient(mc -> {
			mc.player.setXRot(20);
			mc.player.xRotO = 20;
		});
		context.waitTicks(2);
		boolean onBlock = context.computeOnClient(mc -> mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK);
		check(onBlock, "the crosshair should be on the planks");
		swing(context);
		context.waitTicks(3);
		int dug = context.computeOnClient(mc -> SwordStringsClient.counts()[3]);
		check(dug == strokes, "a swing at a block shouldn't count");
		world.getServer().runCommand("setblock " + STAGE.getX() + " " + STAGE.getY() + " " + (STAGE.getZ() + 2) + " minecraft:air");
		context.runOnClient(mc -> {
			mc.player.setXRot(8);
			mc.player.xRotO = 8;
		});

		// In a fight, a whiff counts: strike a husk, then swing past it.
		on(world, player -> {
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200).addTag("wildercord.string_target");
			return null;
		});
		context.waitTicks(10);
		swing(context);
		on(world, player -> {
			kill(player, TAG);
			return null;
		});
		context.waitTicks(6);
		swing(context);
		context.waitTicks(2);
		int fight = context.computeOnClient(mc -> SwordStringsClient.counts()[3]);
		check(fight == strokes + 2, "in a fight a swing at nothing should count (" + (fight - strokes) + " of 2 did)");

		// As a spectator nothing is read.
		on(world, player -> {
			player.setGameMode(GameType.SPECTATOR);
			return null;
		});
		context.waitTicks(5);
		int watching = context.computeOnClient(mc -> SwordStringsClient.counts()[3]);
		swing(context);
		context.waitTicks(3);
		int watched = context.computeOnClient(mc -> SwordStringsClient.counts()[3]);
		on(world, player -> {
			player.setGameMode(GameType.SURVIVAL);
			return null;
		});
		context.waitTicks(5);
		check(watched == watching, "a spectator's swings shouldn't count");
	}

	// ------------------------------------------------------------------ the Second Art

	private static void second(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "gale", AuraRules.FLOW, AuraRules.capacity(AuraRules.FLOW));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200).addTag("wildercord.string_target");
			return null;
		});
		// Looking down a little more, so the leaping swing still finds the husk from the top of the jump.
		context.runOnClient(mc -> {
			mc.player.setXRot(25);
			mc.player.xRotO = 25;
		});
		context.waitTicks(20);
		PERFORMED.clear();
		ART_HITS.clear();
		context.getInput().holdKeyFor(o -> o.keyJump, 1);
		context.waitTicks(4);
		boolean airborne = context.computeOnClient(mc -> !mc.player.onGround());
		check(airborne, "the jump should have left the ground");
		swing(context);
		// Down again, then low.
		for (int t = 0; t < 14 && !context.computeOnClient(mc -> mc.player.onGround()); t++) {
			context.waitTicks(1);
		}
		lowSwing(context);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(1);
		shot(context, "string_second_tp");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		context.waitTicks(3);
		check(PERFORMED.equals(List.of(PlaceholderArts.SECOND)), "a leaping swing then a low one should play the Second Art (" + PERFORMED + ")");
		boolean cut = on(world, player -> ART_HITS.contains(tagged(player, "wildercord.string_target").getUUID()));
		check(cut, "the rising arc should cut the husk");
		context.getInput().releaseKey(o -> o.keyShift);
	}

	// ------------------------------------------------------------------ the Third Art

	private static void third(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "thunder", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 1.6), 200).addTag("wildercord.string_target");
			return null;
		});
		context.waitTicks(20);
		PERFORMED.clear();
		ART_HITS.clear();
		// The guard, by the real keys: sneak and the Aura key; the husk strikes in its perfect moment.
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(2);
		context.getInput().pressKey(WildercordKeys.auraMapping());
		for (int t = 0; t < 4 && !on(world, player -> AuraGuard.perfectNow(player)); t++) {
			context.waitTicks(1);
		}
		float taken = on(world, player -> {
			Mob husk = tagged(player, "wildercord.string_target");
			float before = player.getHealth();
			husk.doHurtTarget(player.level(), player);
			return before - player.getHealth();
		});
		check(taken <= 0, "the guard should have been perfect (took " + taken + ")");
		context.waitTicks(2);
		shot(context, "string_counter_cue_fp");
		// The counter: a swing straight after (still sneaking, as the guard held).
		swing(context);
		context.waitTicks(1);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(1);
		shot(context, "string_third_tp");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		context.waitTicks(3);
		context.getInput().releaseKey(o -> o.keyShift);
		check(PERFORMED.equals(List.of(PlaceholderArts.THIRD)), "a swing straight after a perfect guard should play the Third Art, not the First ("
			+ PERFORMED + ")");
		String staggered = on(world, player -> {
			Mob husk = tagged(player, "wildercord.string_target");
			MobEffectInstance slow = husk.getEffect(MobEffects.SLOWNESS);
			if (!ART_HITS.contains(husk.getUUID())) {
				return "the counter should cut the husk that struck";
			}
			return slow != null && slow.getAmplifier() >= 2 ? null : "the counter should leave the husk staggered (" + slow + ")";
		});
		check(staggered == null, staggered);
	}

	// ------------------------------------------------------------------ the Fourth Art

	private static void fourth(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "verdant", AuraRules.FORM, AuraRules.capacity(AuraRules.FORM));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			// Past where the step lands (six blocks on), a line of husks for the cut ahead.
			for (int i = 0; i < 3; i++) {
				spawn(player.level(), EntityTypes.HUSK, at(0, 8 + 1.5 * i), 200).addTag("wildercord.string_line");
			}
			return null;
		});
		context.waitTicks(20);
		PERFORMED.clear();
		double z0 = on(world, player -> player.getZ());
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(2);
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(5);
		double z1 = on(world, player -> player.getZ());
		check(z1 - z0 > 4, "the double tap should have stepped ahead (" + (z1 - z0) + " blocks)");
		swing(context);
		context.waitTicks(1);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(1);
		shot(context, "string_fourth_tp");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		context.waitTicks(3);
		check(PERFORMED.equals(List.of(PlaceholderArts.FOURTH)), "a swing straight after an Aura Step should play the Fourth Art (" + PERFORMED + ")");
		String cut = on(world, player -> {
			int hurt = 0;
			for (Mob husk : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16), m -> m.entityTags().contains("wildercord.string_line"))) {
				if (husk.getHealth() < husk.getMaxHealth()) {
					hurt++;
				}
			}
			return hurt >= 2 ? null : "the line ahead should cut the husks in it (" + hurt + " of 3)";
		});
		check(cut == null, cut);
	}

	// ------------------------------------------------------------------ the Final Art

	private static void last(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 400).addTag("wildercord.string_target");
			spawn(player.level(), EntityTypes.HUSK, at(-2.5, 0.5), 200).addTag("wildercord.string_ring");
			spawn(player.level(), EntityTypes.HUSK, at(2.5, -1.5), 200).addTag("wildercord.string_ring");
			return null;
		});
		// Filmed at night, where its light reads.
		world.getServer().runCommand("time set 18000");
		context.waitTicks(20);
		PERFORMED.clear();
		for (int i = 0; i < 3; i++) {
			swing(context);
			context.waitTicks(FULL);
		}
		float aura = on(world, player -> Aura.aura(player));
		lowSwing(context);
		context.waitTicks(1);
		shot(context, "string_final_fp");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(1);
		shot(context, "string_final_tp");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		context.waitTicks(3);
		context.getInput().releaseKey(o -> o.keyShift);
		world.getServer().runCommand("time set 3000");
		check(PERFORMED.equals(List.of(PlaceholderArts.FINAL)), "three full swings and a low one, with a full pool, should play the Final Art ("
			+ PERFORMED + ")");
		String ring = on(world, player -> {
			float spent = aura - Aura.aura(player);
			if (spent < StringRules.FINAL_COST - 0.05) {
				return "the Final Art should cost " + StringRules.FINAL_COST + " (spent " + spent + ")";
			}
			int hurt = 0;
			for (Mob husk : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(8), m -> m.entityTags().contains("wildercord.string_ring"))) {
				if (husk.getHealth() < husk.getMaxHealth()) {
					hurt++;
				}
			}
			return hurt == 2 ? null : "the ring should cut the husks round the player (" + hurt + " of 2)";
		});
		check(ring == null, ring);

		// Without a full pool the same swings fall through to the First Art.
		on(world, player -> {
			player.removeAttached(SwordStrings.COOLDOWNS);
			setAura(player, "ember", AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN) * 0.5F);
			return null;
		});
		context.waitTicks(20);
		PERFORMED.clear();
		for (int i = 0; i < 3; i++) {
			swing(context);
			context.waitTicks(FULL);
		}
		lowSwing(context);
		context.waitTicks(4);
		context.getInput().releaseKey(o -> o.keyShift);
		check(PERFORMED.equals(List.of(PlaceholderArts.FIRST)), "with half a pool the Final Art's swings should fall through to the First Art ("
			+ PERFORMED + ")");
	}

	// ------------------------------------------------------------------ the server's checks

	private static void server(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "starlit", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			return null;
		});
		context.waitTicks(40);
		PERFORMED.clear();
		int marks = SwordString.Token.marks(SwordString.Token.LOW);
		int plain = SwordString.Token.marks();
		// A First Art with no swings behind it: the server saw none, so it refuses.
		context.runOnClient(mc -> ClientPlayNetworking.send(new SwordStrings.Perform(PlaceholderArts.FIRST, List.of(plain, plain, marks))));
		context.waitTicks(3);
		check(PERFORMED.isEmpty(), "a string sent with no swings behind it should be refused (" + PERFORMED + ")");
		// An art above the stage, and one whose swings aren't its string.
		context.runOnClient(mc -> ClientPlayNetworking.send(new SwordStrings.Perform(PlaceholderArts.FINAL, List.of(plain, plain, plain, marks))));
		context.runOnClient(mc -> ClientPlayNetworking.send(new SwordStrings.Perform(PlaceholderArts.FIRST, List.of(plain, plain, plain))));
		context.waitTicks(3);
		check(PERFORMED.isEmpty(), "an art above the stage, or swings that aren't its string, should be refused (" + PERFORMED + ")");
		String why = on(world, player -> {
			AuraApi.StringArt art = AuraApi.string(PlaceholderArts.FIRST).orElseThrow();
			String a = SwordStrings.check(player, art, List.of(plain, plain, marks)).map(Enum::name).orElse("none");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			String b = SwordStrings.check(player, art, List.of(plain, plain, marks)).map(Enum::name).orElse("none");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			setAura(player, "starlit", AuraRules.EDGE, 2);
			String c = SwordStrings.check(player, art, List.of(plain, plain, marks)).map(Enum::name).orElse("none");
			return a.equals("UNSEEN") && b.equals("NO_WEAPON") && c.equals("NO_AURA") ? null
				: "the server should refuse unseen swings, an empty hand and too little aura (" + a + ", " + b + ", " + c + ")";
		});
		check(why == null, why);

		// Strings switched off: nothing is read on the client. (Written into the live settings and told to the client as a reload would.)
		on(world, player -> {
			setAura(player, "starlit", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE));
			return null;
		});
		swapStrings(world, false);
		context.waitTicks(5);
		boolean off = context.computeOnClient(mc -> !dev.wildercord.config.Config.strings(mc.player));
		check(off, "the client should learn strings are off");
		on(world, player -> {
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200).addTag("wildercord.string_target");
			return null;
		});
		context.waitTicks(10);
		int strokes = context.computeOnClient(mc -> SwordStringsClient.counts()[3]);
		swing(context);
		context.waitTicks(3);
		int after = context.computeOnClient(mc -> SwordStringsClient.counts()[3]);
		swapStrings(world, true);
		context.waitTicks(5);
		check(after == strokes, "with strings off, swings shouldn't be read");
	}

	/** Switches strings in the live settings (as an edited file and a reload would) and tells the client. */
	private static void swapStrings(TestSingleplayerContext world, boolean on) {
		world.getServer().runOnServer(server -> {
			try {
				java.nio.file.Path path = dev.wildercord.config.Config.path();
				String text = java.nio.file.Files.readString(path);
				text = text.replaceAll("\"strings\": (true|false)", "\"strings\": " + on);
				java.nio.file.Files.writeString(path, text);
				dev.wildercord.config.Config.reload(server);
			} catch (java.io.IOException e) {
				throw new AssertionError("couldn't switch strings: " + e);
			}
		});
	}

	// ------------------------------------------------------------------ the indicator by the hotbar, and the Aura page

	private static void views(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "hollow", AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN) * 0.6F);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200).addTag("wildercord.string_target");
			return null;
		});
		context.runOnClient(mc -> MagicQuality.stringIndicator = MagicQuality.StringIndicator.HOTBAR);
		context.waitTicks(20);
		swing(context);
		context.waitTicks(6);
		swing(context);
		context.waitTicks(3);
		shot(context, "string_hotbar");
		String hotbar = context.computeOnClient(mc -> StringHud.phase().equals("live") && StringHud.shown() == 2 ? null
			: "the indicator by the hotbar should still follow the string (" + StringHud.phase() + ", " + StringHud.shown() + ")");
		check(hotbar == null, hotbar);
		// Without a Cord the aura bar stands alone right of the hotbar, and the marks sit above it there too.
		ItemStack cord = on(world, player -> {
			ItemStack worn = Spellbooks.cord(player).copy();
			Spellbooks.setCord(player, ItemStack.EMPTY);
			return worn;
		});
		context.waitTicks(3);
		swing(context);
		context.waitTicks(3);
		shot(context, "string_hotbar_alone");
		on(world, player -> {
			Spellbooks.setCord(player, cord);
			return null;
		});
		context.runOnClient(mc -> MagicQuality.stringIndicator = MagicQuality.StringIndicator.CROSSHAIR);
		context.waitTicks(30);

		// The Aura page: its Sword strings tab (clicked as a player would) lists the arts, with their strings drawn.
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		double[] tab = context.computeOnClient(mc -> ((AuraScreen) mc.gui.screen()).artsTabPoint());
		check(tab != null, "the Aura page should draw its Sword strings tab");
		double guiScale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(tab[0] * guiScale, tab[1] * guiScale);
		context.waitTicks(1);
		context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(3);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "string_page");
		context.setScreen(() -> null);
		on(world, player -> {
			setAura(player, "rime", AuraRules.FLOW, 20);
			return null;
		});
		context.waitTicks(3);
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		shot(context, "string_page_flow");
		context.setScreen(() -> null);
		// The page opens on its techniques again for whatever comes next.
		context.runOnClient(mc -> AuraScreen.listArts(false));
	}

	// ------------------------------------------------------------------ how the arts look

	/**
	 * Each art in a different method's colour, from above and behind, by day (where its dark rim has to carry it against the
	 * sky) and by night, then in first person by day (how much of the view it takes). Performed on the server directly: the
	 * keys that play them are tested above.
	 */
	private static void looks(ClientGameTestContext context, TestSingleplayerContext world) {
		String[] methods = {"ember", "rime", "thunder", "verdant", "hollow"};
		for (int i = 0; i < PlaceholderArts.IDS.size(); i++) {
			String id = PlaceholderArts.IDS.get(i);
			String method = methods[i];
			boolean line = id.equals(PlaceholderArts.FOURTH);
			for (int view = 0; view < 3; view++) {
				boolean night = view == 1;
				boolean firstPerson = view == 2;
				world.getServer().runCommand(night ? "time set 18000" : "time set 3000");
				on(world, player -> {
					kill(player, TAG);
					stand(player);
					setAura(player, method, AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN));
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
					player.removeAttached(SwordStrings.COOLDOWNS);
					if (line) {
						spawn(player.level(), EntityTypes.HUSK, at(0.3, 2.6), 400);
						spawn(player.level(), EntityTypes.HUSK, at(-0.3, 4.4), 400);
					} else {
						spawn(player.level(), EntityTypes.HUSK, at(-1.7, 2.3), 400);
						spawn(player.level(), EntityTypes.HUSK, at(1.7, 2.3), 400);
						spawn(player.level(), EntityTypes.HUSK, at(0, 3.0), 400);
					}
					player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(5.0);
					// Looking down on the platform from behind (the camera follows the view); in first person, ahead as in a fight.
					player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, firstPerson ? 8.0F : 38.0F,
						false);
					return null;
				});
				context.runOnClient(mc -> {
					mc.options.setCameraType(firstPerson ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
					if (mc.gui.hud.isHidden() != !firstPerson) {
						mc.gui.hud.toggle();
					}
				});
				context.waitTicks(12);
				String done = on(world, player -> {
					AuraApi.StringArt art = AuraApi.string(id).orElseThrow();
					List<Integer> marks = art.string().tokens().stream().map(t -> SwordString.Token.marks(t)).toList();
					return SwordStrings.perform(player, art, marks) ? null : id + " didn't go off";
				});
				check(done == null, done);
				// Its light has swept across by now (a tick on the way to this client, a tick to sweep).
				context.waitTicks(3);
				shot(context, "string_art_" + (i + 1) + (firstPerson ? "_fp" : night ? "_night" : "_day"));
			}
		}
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			return null;
		});
		world.getServer().runCommand("time set 3000");
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
	}

	// ------------------------------------------------------------------ the keys

	/** The attack key, pressed (a swing), as a player would. */
	private static void swing(ClientGameTestContext context) {
		context.getInput().pressKey(o -> o.keyAttack);
	}

	/** Sneak held, then the attack key: a low swing. Sneak stays held (let it go when done). */
	private static void lowSwing(ClientGameTestContext context) {
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(2);
		swing(context);
	}

	// ------------------------------------------------------------------ the stage

	/** A stone platform in the sky; the player in survival with an Echo Cord, facing down it (+z). */
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

	/** The player back on the spot, facing +z, healthy, nothing lingering. */
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
		});
		// Long enough for any string to run out and the last blow to be forgotten.
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
}
