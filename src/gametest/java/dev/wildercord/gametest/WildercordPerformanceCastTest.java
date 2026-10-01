package dev.wildercord.gametest;

import com.mojang.authlib.GameProfile;
import dev.wildercord.cast.CastLock;
import dev.wildercord.cast.Charging;
import dev.wildercord.cast.WildSurge;
import dev.wildercord.client.CastingOptions;
import dev.wildercord.client.SigilTrace;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.client.fx.Incantations;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Incantation;
import dev.wildercord.spell.Overchannel;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.TraceGlyph;
import dev.wildercord.spell.WildMagic;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Casting as a performance, played for real on a platform in the sky at night:
 * <ul>
 *   <li>a charge held past full climbs overchannel stages, and each lands the beam measurably harder on a husk;</li>
 *   <li>a release on the beat (as the charge fills, or a stage lands) adds its bonus;</li>
 *   <li>a forged trace accuracy is clamped by the server to what the steadying allows;</li>
 *   <li>a glyph traced with the test's own mouse (sneak held, the camera holding still) scores high and steadies the
 *       release, and an untraced release scores neutral;</li>
 *   <li>a rival's incantation rises over their shoulders for this client to read, and the option hides it;</li>
 *   <li>a channel held too long tears loose without killing a caster on one heart.</li>
 * </ul>
 * Screenshots: {@code performance_overchannel_stage_1..3} (third person), {@code performance_trace_glyph},
 * {@code performance_incantation_rival} and {@code performance_incantation_third_person}.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY}
 * and {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordPerformanceCastTest implements FabricClientGameTest {
	private static final Overchannel.Tuning T = Overchannel.Tuning.DEFAULTS;
	/** The platform's floor. */
	private static BlockPos stage;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		Path config = Config.path();
		String original = read(config);
		boolean tracing = context.computeOnClient(mc -> {
			CastingOptions.ensureLoaded();
			return CastingOptions.tracing;
		});
		TraceGlyph.Assist assist = context.computeOnClient(mc -> CastingOptions.assist);
		CastingOptions.Incantations incantations = context.computeOnClient(mc -> CastingOptions.incantations);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("time set 18000", "gamerule advance_time false", "gamerule spawn_mobs false", "weather clear",
					"gamerule advance_weather false", "difficulty normal", "gamerule natural_health_regeneration false")) {
				world.getServer().runCommand(command);
			}
			world.getServer().runOnServer(server -> setUp(player(server)));
			context.runOnClient(mc -> {
				CastingOptions.tracing = true;
				CastingOptions.assist = TraceGlyph.Assist.NONE;
				CastingOptions.incantations = CastingOptions.Incantations.ALL;
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			context.waitTicks(20);
			// Wild magic off while the spell's power is measured (a surge would twist the very spell being measured), and no
			// mana coming back, so what the channel drains can be counted.
			configure(context, world, config, WildercordConfig.DEFAULTS.toJson().replace("\"wild_magic\": true", "\"wild_magic\": false")
				.replace("\"regen_multiplier\": 1.0", "\"regen_multiplier\": 0.0"));

			stagesAddPower(context, world);
			theBeatAddsItsBonus(context, world);
			aForgedTraceIsClamped(context, world);
			aTracedGlyphSteadies(context, world);
			incantationsForASecondViewer(context, world);
			screenshots(context, world);

			// Wild magic back on: a torn channel surges, harmlessly.
			configure(context, world, config, WildercordConfig.DEFAULTS.toJson());
			overholdingBackfiresSafely(context, world);
		} finally {
			if (original == null) {
				try {
					Files.deleteIfExists(config);
				} catch (IOException e) {
					throw new AssertionError("couldn't remove " + config + ": " + e);
				}
			} else {
				write(config, original);
			}
			context.runOnClient(mc -> {
				CastingOptions.tracing = tracing;
				CastingOptions.assist = assist;
				CastingOptions.incantations = incantations;
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
		}
	}

	// ------------------------------------------------------------------ set up

	private static void setUp(ServerPlayer player) {
		ServerLevel level = player.level();
		stage = new BlockPos(player.getBlockX(), 180, player.getBlockZ());
		for (int x = -7; x <= 7; x++) {
			for (int z = -7; z <= 9; z++) {
				level.setBlockAndUpdate(stage.offset(x, 0, z), (x + z) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
			}
		}
		player.setGameMode(GameType.SURVIVAL);
		// Empty hands: a Blank Rune held while sneaking still would try to attune, and say so over the pictures.
		player.getInventory().clearContent();
		Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
		Spellbook book = Spellbooks.get(player).withStarterGiven();
		for (RuneDef rune : Runes.all()) {
			book = book.learn(rune.id());
		}
		Spellbooks.set(player, book.withSpell(0, ids(Runes.BEAM, Runes.HARM)).withSpell(1, ids(Runes.BOLT, Runes.FIRE, Runes.SPLIT_MOD)).withSelected(0));
		// Four working Heart Circles: a heart that holds all three stages.
		player.setAttached(WildercordAttachments.CIRCLES, 4);
		face(player);
		Spellbooks.setMana(player, Mana.max(player));
	}

	/** On the platform, looking straight along +z at nothing but the husk's spot. */
	private static void face(ServerPlayer player) {
		player.teleportTo(player.level(), stage.getX() + 0.5, stage.getY() + 1, stage.getZ() + 0.5, Set.<Relative>of(), 0, 0, false);
	}

	/** A fresh charge can start: no cooldown, no rhythm, no charge in hand, full mana and health. */
	private static void ready(ServerPlayer player) {
		for (int spell = 0; spell < 4; spell++) {
			Spellbooks.setReadyAt(player, spell, 0);
		}
		player.setAttached(WildercordAttachments.RHYTHM, WildercordAttachments.Rhythm.NONE);
		Spellbooks.setMana(player, Mana.max(player));
		player.setHealth(player.getMaxHealth());
		// Empty hands: a Blank Rune held while sneaking still would try to attune, and say so over the pictures.
		player.getInventory().clearContent();
	}

	/** Takes away any husk left from an earlier measurement. */
	private static void clearTargets(ServerPlayer player) {
		for (Entity e : player.level().getAllEntities()) {
			if (e.entityTags().contains("wildercord.performance_target")) {
				e.discard();
			}
		}
	}

	/** A husk five blocks along the beam, standing still with plenty of health; the last one goes. */
	private static Mob target(ServerPlayer player) {
		ServerLevel level = player.level();
		clearTargets(player);
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		husk.snapTo(stage.getX() + 0.5, stage.getY() + 1, stage.getZ() + 5.5, 180, 0);
		husk.setNoAi(true);
		husk.setPersistenceRequired();
		husk.addTag("wildercord.rolled");
		husk.addTag("wildercord.performance_target");
		husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
		husk.setHealth(1000);
		level.addFreshEntity(husk);
		return husk;
	}

	private static Mob husk(ServerPlayer player) {
		for (Entity e : player.level().getAllEntities()) {
			if (e instanceof Mob mob && mob.entityTags().contains("wildercord.performance_target")) {
				return mob;
			}
		}
		throw new AssertionError("the husk is gone");
	}

	private static void configure(ClientGameTestContext context, TestSingleplayerContext world, Path path, String json) {
		write(path, json);
		world.getServer().runCommand("wildercord reload");
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------ stages and power

	/** What a beam released some ticks into its charge did: how it was performed, and the damage the husk took. */
	private record Release(Charging.Performance performance, float damage, float manaUsed, int stageSeenByClient) {}

	/**
	 * Charges spell 0 (Beam, Harm) from the server, lets it go {@code ticks} after it began, and measures. {@code look}
	 * ticks in, the client is asked what stage it sees.
	 */
	private static Release release(ClientGameTestContext context, TestSingleplayerContext world, int ticks, int look) {
		float before = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			face(player);
			target(player);
			Charging.request(player, 0, true);
			return Spellbooks.mana(player);
		});
		int seen = -1;
		if (look > 0 && look < ticks) {
			context.waitTicks(look);
			seen = context.computeOnClient(mc -> {
				WildercordAttachments.Charge charge = mc.player.getAttached(WildercordAttachments.CHARGE);
				return charge == null ? -1 : charge.stage();
			});
			context.waitTicks(ticks - look);
		} else {
			context.waitTicks(ticks);
		}
		world.getServer().runOnServer(server -> Charging.request(player(server), 0, false));
		context.waitTicks(8);
		int stageSeen = seen;
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Mob husk = husk(player);
			return new Release(Charging.last(player), husk.getMaxHealth() - husk.getHealth(), before - Spellbooks.mana(player), stageSeen);
		});
	}

	private static void stagesAddPower(ClientGameTestContext context, TestSingleplayerContext world) {
		int full = Charging.FULL;
		// Each released ten ticks after its stage lands: well clear of the beat, well short of the next stage.
		Release plain = release(context, world, full + 10, 0);
		check(plain.performance().stage() == 0 && !plain.performance().onBeat(), "a release just after full should be plain: " + plain);
		check(plain.damage() > 1, "the beam should have hurt the husk: " + plain);
		float[] damage = new float[4];
		damage[0] = plain.damage();
		for (int s = 1; s <= 3; s++) {
			int ticks = full + s * Overchannel.STAGE_TICKS + 10;
			Release r = release(context, world, ticks, ticks - 4);
			check(r.performance().stage() == s, "held " + ticks + " ticks, the charge should be at stage " + s + ": " + r);
			check(r.stageSeenByClient() == s, "the client should see stage " + s + " too (it saw " + r.stageSeenByClient() + ")");
			check(!r.performance().onBeat(), "ten ticks after a stage is off the beat: " + r);
			check(Math.abs(r.performance().power() - Overchannel.stagePower(s, T.powerPerStage())) < 1e-6, "stage " + s + "'s power: " + r);
			damage[s] = r.damage();
			double ratio = damage[s] / damage[0];
			double expected = Overchannel.stagePower(s, T.powerPerStage());
			check(Math.abs(ratio - expected) < 0.06 * expected, "stage " + s + " should land " + expected + "x as hard as a plain release (it was " + ratio + ")");
			if (s == 3) {
				// The channel drained spare mana on top of the spell's own price: about a second and a half of draining, at 15% of the price a second.
				float price = plain.manaUsed();
				float drained = r.manaUsed() - price;
				check(drained > price * 0.15F, "overchannelling should drain mana beyond the price (" + r.manaUsed() + " against " + price + ")");
			}
		}
		// Short of spare mana, the channel stops climbing and never touches the spell's own price: it still goes off at full price.
		String starved = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			face(player);
			target(player);
			Spellbooks.setMana(player, plain.manaUsed() + 0.4F);
			Charging.request(player, 0, true);
			return null;
		});
		check(starved == null, starved);
		context.waitTicks(full + 3 * Overchannel.STAGE_TICKS + 10);
		String gutters = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
			if (charge == null) {
				return "a starved channel should neither tear loose nor end";
			}
			if (charge.stage() > 1) {
				return "with almost no spare mana the channel shouldn't climb past stage I (it reached " + charge.stage() + ")";
			}
			if (Spellbooks.mana(player) < plain.manaUsed() - 1e-3F) {
				return "the channel should never drain the spell's own price (" + Spellbooks.mana(player) + " left of " + plain.manaUsed() + ")";
			}
			long readyAt = Spellbooks.readyAt(player, 0);
			Charging.request(player, 0, false);
			return Spellbooks.readyAt(player, 0) != readyAt ? null : "the starved charge should still go off when let go";
		});
		check(gutters == null, gutters);
		context.waitTicks(8);
	}

	private static void theBeatAddsItsBonus(ClientGameTestContext context, TestSingleplayerContext world) {
		int full = Charging.FULL;
		Release plain = release(context, world, full + 10, 0);
		Release onFull = release(context, world, full + 2, 0);
		check(onFull.performance().onBeat() && onFull.performance().stage() == 0, "let go just as it fills, the release is on the beat: " + onFull);
		check(Math.abs(onFull.performance().power() - (1 + T.beatBonus())) < 1e-6, "the beat's bonus: " + onFull);
		double ratio = onFull.damage() / plain.damage();
		check(Math.abs(ratio - (1 + T.beatBonus())) < 0.05, "on the beat should land " + (1 + T.beatBonus()) + "x as hard (it was " + ratio + ")");
		Release onStage = release(context, world, full + Overchannel.STAGE_TICKS + 2, 0);
		check(onStage.performance().onBeat() && onStage.performance().stage() == 1, "let go just as stage I lands: on the beat: " + onStage);
		check(Math.abs(onStage.performance().power() - Overchannel.power(1, true, 0, T)) < 1e-6, "stage I on the beat: " + onStage);
	}

	// ------------------------------------------------------------------ tracing

	private static void aForgedTraceIsClamped(ClientGameTestContext context, TestSingleplayerContext world) {
		// Claimed perfect (and impossibly more), never steadied: nothing of it counts.
		long start = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			Charging.request(player, 0, true);
			long begun = player.getAttached(WildercordAttachments.CHARGE).start();
			Charging.trace(player, begun, 5.0F);
			return begun;
		});
		context.waitTicks(Charging.FULL + 10);
		double unsteadied = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			return Charging.performance(player, player.getAttached(WildercordAttachments.CHARGE), player.level().getGameTime()).trace();
		});
		check(unsteadied == 0, "a trace claimed without steadying should count for nothing (it counted " + unsteadied + ")");
		// Steadied for twelve ticks only, then a forged packet from the client claiming 9.0: held to what twelve ticks allow.
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(12);
		context.getInput().releaseKey(options -> options.keyShift);
		context.waitTicks(2);
		context.runOnClient(mc -> ClientPlayNetworking.send(new WildercordNetworking.TraceSpell(start, 9.0F)));
		context.waitTicks(3);
		world.getServer().runOnServer(server -> Charging.request(player(server), 0, false));
		context.waitTicks(4);
		double clamped = world.getServer().computeOnServer(server -> Charging.last(player(server)).trace());
		double allowed = Math.min(1.0, 14.0 / Overchannel.FULL_STEADY);
		check(clamped > 0 && clamped <= allowed, "a forged 9.0 after twelve ticks of steadying should be clamped to at most " + allowed + " (it was " + clamped + ")");
	}

	private static void aTracedGlyphSteadies(ClientGameTestContext context, TestSingleplayerContext world) {
		// Traced: hold the cast key as a player would, steady with sneak, and draw the glyph with the mouse.
		world.getServer().runOnServer(server -> {
			ready(player(server));
			face(player(server));
			clearTargets(player(server));
		});
		context.waitTicks(5);
		context.getInput().holdKey(WildercordKeys.castMapping());
		context.waitTicks(10);
		boolean charging = context.computeOnClient(mc -> mc.player.hasAttached(WildercordAttachments.CHARGE));
		check(charging, "holding the cast key should start a charge");
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(3);
		// The window's own mouse, held as in play (a fresh grab ignores its first movement: spend it on nothing).
		context.runOnClient(mc -> mc.mouseHandler.grabMouse());
		context.getInput().moveCursor(0, 0);
		context.waitTicks(1);
		float yaw = context.computeOnClient(mc -> mc.player.getYRot());
		List<double[]> glyph = context.computeOnClient(mc -> SigilTrace.glyph());
		check(glyph.size() >= 4, "a two-rune spell's glyph should have three strokes or more: " + glyph.size());
		double rate = context.computeOnClient(mc -> {
			double s = mc.options.sensitivity().get() * 0.6 + 0.2;
			return Math.max(0.04, Math.min(0.3, s * s * s * 8.0 * 0.15));
		});
		double countsPerUnit = 20.0 / rate;
		for (int i = 1; i < glyph.size(); i++) {
			double[] a = glyph.get(i - 1);
			double[] b = glyph.get(i);
			int steps = 8;
			for (int k = 0; k < steps; k++) {
				double dx = (b[0] - a[0]) / steps * countsPerUnit;
				// The window's y runs down; the glyph's up.
				double dy = -(b[1] - a[1]) / steps * countsPerUnit;
				context.getInput().moveCursor(dx, dy);
				context.waitTicks(1);
			}
		}
		context.waitTicks(2);
		int points = context.computeOnClient(mc -> SigilTrace.pathSize());
		double accuracy = context.computeOnClient(mc -> SigilTrace.accuracy());
		float yawAfter = context.computeOnClient(mc -> mc.player.getYRot());
		check(points > 10, "the mouse should have moved the tracing point (" + points + " points)");
		check(accuracy > 0.85, "a glyph traced along its lines should score high (it scored " + accuracy + ")");
		check(Math.abs(yawAfter - yaw) < 0.01, "steadied, the camera should hold still (yaw " + yaw + " -> " + yawAfter + ")");
		shot(context, "performance_trace_glyph");
		// Let go once the channel is past stage I, so the steadying shows in its surge chance.
		waitForStage(context, 1);
		context.getInput().releaseKey(WildercordKeys.castMapping());
		context.waitTicks(6);
		context.getInput().releaseKey(options -> options.keyShift);
		context.waitTicks(2);
		Charging.Performance traced = world.getServer().computeOnServer(server -> Charging.last(player(server)));
		check(traced.trace() > 0.8, "the server should believe a well traced glyph: " + traced);
		check(traced.stage() >= 1, "it was let go overchannelled: " + traced);
		double raw = Overchannel.surgeChance(traced.stage(), T.surgePerStage(), 0);
		check(traced.surgeChance() < raw - 1e-6, "a traced glyph steadies the channel: " + traced.surgeChance() + " against " + raw);
		check(Math.abs(traced.power() - Overchannel.power(traced.stage(), traced.onBeat(), traced.trace(), T)) < 1e-6
			&& traced.power() > Overchannel.power(traced.stage(), traced.onBeat(), 0, T), "and adds its small bonus: " + traced);

		// Untraced: the same, without steadying. Nothing gained, nothing lost.
		world.getServer().runOnServer(server -> ready(player(server)));
		context.waitTicks(5);
		context.getInput().holdKey(WildercordKeys.castMapping());
		context.waitTicks(10);
		waitForStage(context, 1);
		context.getInput().releaseKey(WildercordKeys.castMapping());
		context.waitTicks(6);
		Charging.Performance untraced = world.getServer().computeOnServer(server -> Charging.last(player(server)));
		check(untraced.trace() == 0, "an untraced release should score nothing: " + untraced);
		check(untraced.stage() >= 1, "it was let go overchannelled: " + untraced);
		check(Math.abs(untraced.surgeChance() - Overchannel.surgeChance(untraced.stage(), T.surgePerStage(), 0)) < 1e-9,
			"untraced, the surge chance is the stage's own: " + untraced);
		check(Math.abs(untraced.power() - Overchannel.power(untraced.stage(), untraced.onBeat(), 0, T)) < 1e-9, "and the power the stage's: " + untraced);
	}

	/** Waits (at most a few seconds) until the local player's charge reaches {@code stage}. */
	private static void waitForStage(ClientGameTestContext context, int stage) {
		for (int i = 0; i < 120; i++) {
			int now = context.computeOnClient(mc -> {
				WildercordAttachments.Charge charge = mc.player.getAttached(WildercordAttachments.CHARGE);
				return charge == null ? -1 : charge.stage();
			});
			if (now >= stage) {
				return;
			}
			context.waitTicks(1);
		}
		throw new AssertionError("the charge never reached stage " + stage);
	}

	// ------------------------------------------------------------------ incantations

	private static void incantationsForASecondViewer(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> runes = ids(Runes.BOLT, Runes.FIRE, Runes.SPLIT_MOD);
		// Another caster, as this client sees one: a player three and a half blocks ahead, facing us, charging.
		int rival = context.computeOnClient(mc -> {
			RemotePlayer other = new RemotePlayer(mc.level, new GameProfile(UUID.nameUUIDFromBytes("wildercord-rival".getBytes(StandardCharsets.UTF_8)), "Rival"));
			// An id the server will never hand out.
			other.setId(-4242);
			other.snapTo(stage.getX() + 0.5, stage.getY() + 1, stage.getZ() + 4.0, 180, 0);
			other.setYHeadRot(180);
			other.setYBodyRot(180);
			mc.level.addEntity(other);
			long now = mc.level.getGameTime();
			other.setAttached(WildercordAttachments.CHARGE, new WildercordAttachments.Charge(1, now, runes, Charging.FULL, 3, 0, now + Charging.FULL, true));
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
			return other.getId();
		});
		context.waitTicks(Charging.FULL + 6);
		int spoken = context.computeOnClient(mc -> Incantations.spoken(rival));
		String line = context.computeOnClient(mc -> Incantations.line(rival));
		check(spoken == runes.size(), "every rune's syllable should have risen by the time the charge fills (" + spoken + " of " + runes.size() + ")");
		check(line.equals(Incantation.line(runes)), "the incantation should read \"" + Incantation.line(runes) + "\" (it reads \"" + line + "\")");
		shot(context, "performance_incantation_rival");
		context.runOnClient(mc -> CastingOptions.incantations = CastingOptions.Incantations.HIDE_OTHERS);
		context.waitTicks(10);
		check(context.computeOnClient(mc -> Incantations.spoken(rival)) == 0, "hiding others' incantations should hide the rival's");
		context.runOnClient(mc -> CastingOptions.incantations = CastingOptions.Incantations.NONE);
		context.waitTicks(4);
		check(context.computeOnClient(mc -> Incantations.spoken(rival)) == 0, "hiding all incantations should hide the rival's");
		context.runOnClient(mc -> CastingOptions.incantations = CastingOptions.Incantations.HIDE_MINE);
		context.waitTicks(4);
		check(context.computeOnClient(mc -> Incantations.spoken(rival)) == runes.size(), "hiding only your own should show the rival's again");
		context.runOnClient(mc -> {
			CastingOptions.incantations = CastingOptions.Incantations.ALL;
			mc.level.removeEntity(rival, Entity.RemovalReason.DISCARDED);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(10);
		check(context.computeOnClient(mc -> Incantations.spoken(rival)) == 0, "a caster who is gone says nothing more");
	}

	// ------------------------------------------------------------------ screenshots

	private static void screenshots(ClientGameTestContext context, TestSingleplayerContext world) {
		// The overchannel's three stages from behind the caster, the fire bolt's circle cracking further at each.
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			face(player);
			clearTargets(player);
			Charging.request(player, 1, true);
		});
		context.waitTicks(Charging.FULL + Overchannel.STAGE_TICKS + 5);
		shot(context, "performance_overchannel_stage_1");
		context.waitTicks(Overchannel.STAGE_TICKS - 2);
		shot(context, "performance_overchannel_stage_2");
		context.waitTicks(Overchannel.STAGE_TICKS - 2);
		shot(context, "performance_overchannel_stage_3");
		int seen = context.computeOnClient(mc -> {
			WildercordAttachments.Charge charge = mc.player.getAttached(WildercordAttachments.CHARGE);
			return charge == null ? -1 : charge.stage();
		});
		check(seen == 3, "the stage 3 picture should show stage 3 (it showed " + seen + ")");
		world.getServer().runOnServer(server -> Charging.request(player(server), 1, false));
		context.waitTicks(30);
		// The caster's own incantation from the front, mid-charge, rising over their shoulders.
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		world.getServer().runOnServer(server -> {
			ready(player(server));
			Charging.request(player(server), 1, true);
		});
		context.waitTicks(Charging.FULL + 4);
		int own = context.computeOnClient(mc -> Incantations.spoken(mc.player.getId()));
		check(own == 3, "the caster's own incantation should show in third person (" + own + " syllables)");
		shot(context, "performance_incantation_third_person");
		world.getServer().runOnServer(server -> Charging.request(player(server), 1, false));
		context.waitTicks(20);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
	}

	// ------------------------------------------------------------------ tearing loose

	private static void overholdingBackfiresSafely(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] readyBefore = new int[1];
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			face(player);
			// On one heart, and the surge forced to the one that lifts everyone off their feet.
			player.setHealth(2.0F);
			WildSurge.force(player, WildMagic.Surge.LEVITATE);
			Charging.request(player, 0, true);
		});
		context.waitTicks(Charging.FULL + 3 * Overchannel.STAGE_TICKS + Overchannel.GRACE + 4);
		String torn = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			if (player.hasAttached(WildercordAttachments.CHARGE)) {
				return "held past the last stage, the channel should have torn loose";
			}
			if (!player.isAlive() || player.getHealth() < 2.0F - 1e-3F) {
				return "tearing loose must never harm the caster (health " + player.getHealth() + ")";
			}
			float max = Mana.max(player);
			// Full before it tore (mana comes back as fast as the channel drank here), a share burnt, and a few ticks' regeneration since.
			if (Spellbooks.mana(player) > max * (1 - T.manaBurn()) + Mana.of(player).regen() * 0.5F + 1) {
				return "tearing loose should burn a share of mana (" + Spellbooks.mana(player) + " of " + max + ")";
			}
			if (!CastLock.locked(player)) {
				return "tearing loose should daze the caster for a moment";
			}
			long readyAt = Spellbooks.readyAt(player, 0);
			Charging.request(player, 0, false);
			if (Spellbooks.readyAt(player, 0) != readyAt) {
				return "letting go of a torn channel should cast nothing";
			}
			return null;
		});
		check(torn == null, torn);
		// The lift, the slow fall and the landing: still alive, still on one heart or more.
		context.waitTicks(140);
		String landed = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			WildSurge.force(player, null);
			if (!player.isAlive() || player.getHealth() < 2.0F - 1e-3F) {
				return "the torn channel's surge must not kill or hurt the caster (health " + player.getHealth() + ")";
			}
			player.setHealth(player.getMaxHealth());
			return null;
		});
		check(landed == null, landed);
	}

	// ------------------------------------------------------------------ helpers

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.waitTicks(1);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	private static List<String> ids(RuneDef... runes) {
		return Arrays.stream(runes).map(RuneDef::id).toList();
	}

	private static String read(Path path) {
		try {
			return Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : null;
		} catch (IOException e) {
			throw new AssertionError("couldn't read " + path + ": " + e);
		}
	}

	private static void write(Path path, String text) {
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, text, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AssertionError("couldn't write " + path + ": " + e);
		}
	}
}
