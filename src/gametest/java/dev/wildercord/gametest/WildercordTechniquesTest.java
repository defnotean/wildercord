package dev.wildercord.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraDominion;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.Awakening;
import dev.wildercord.aura.Momentum;
import dev.wildercord.aura.Stance;
import dev.wildercord.aura.StanceRules;
import dev.wildercord.aura.StringRules;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.aura.TechniqueRules;
import dev.wildercord.aura.Techniques;
import dev.wildercord.aura.WayBanner;
import dev.wildercord.aura.WayRules;
import dev.wildercord.aura.Ways;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.TechniqueArts;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.TechniqueScrollItem;
import dev.wildercord.client.AuraClient;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.client.TechniquePage;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Techniques of one's own, step 8 of the aura overhaul, written on the Aura page's writing page with the real mouse and keyboard and played
 * with the real keys on a stone platform in the sky:
 * <ul>
 *   <li><b>The page</b>: the tab clicked open, a slot picked, each seal opened and its part clicked, the name typed, the string composed
 *       from the swings, a string that meets an art refused, the technique written (the server's book holds it, its name made safe);</li>
 *   <li><b>Played</b>: its string played with the keys on husks, its banner under its writer's name, its price and rest paid, the wave
 *       reaching foes far past the blade, the sunder wearing their stance hard;</li>
 *   <li><b>Releases</b>: on the blade, a burst all round, an afterimage striking again from where it was struck, each filmed from behind
 *       and through the swordsman's own eyes;</li>
 *   <li><b>Intents</b>: bind rooting, echo striking twice, ward and rally steadying (a Way lending each), infuse burning longer;</li>
 *   <li><b>Ranks</b>: a use carrying a technique into Honed, the temper chosen on the page, Peerless and a part set down on a scroll;</li>
 *   <li><b>Scrolls and duelists</b>: a scroll read with the use key, read again and kept, a duelist's lesson;</li>
 *   <li><b>Slots, awakening and fairness</b>: the slots by stage, a technique free while awakened, a rival player held to the caps, a Way's
 *       part going quiet when the Way is left.</li>
 * </ul>
 * Screenshots: {@code technique_*}.
 *
 * <p>{@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it; {@code WILDERCORD_TECHNIQUES=a,b}
 * plays only the scenes named (page, played, releases, intents, elements, ranks, scrolls, slots, awakened, fair, lent).</p>
 */
public class WildercordTechniquesTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 190, 0);
	private static final String TAG = "wildercord.techniques_test";
	private static final List<String> PERFORMED = Collections.synchronizedList(new ArrayList<>());
	private static final Map<UUID, Integer> HITS = Collections.synchronizedMap(new HashMap<>());
	private static final List<String> HOOKED = Collections.synchronizedList(new ArrayList<>());
	private static boolean hooked;
	/** A sword's full swing comes back in a little under 13 ticks. */
	private static final int FULL = 14;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		if (!hooked) {
			hooked = true;
			AuraApi.onString((player, art, ctx) -> PERFORMED.add(art.id()));
			ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
				if (source.is(Aura.DAMAGE) && taken > 0) {
					HITS.merge(entity.getUUID(), 1, Integer::sum);
				}
			});
			AuraApi.onTechnique(new AuraApi.TechniqueHook() {
				@Override
				public void written(ServerPlayer player, int slot, Techniques.Written technique) {
					HOOKED.add("written:" + slot + ":" + technique.name());
				}

				@Override
				public void ranked(ServerPlayer player, int slot, Techniques.Written technique, int rank) {
					HOOKED.add("ranked:" + slot + ":" + rank);
				}

				@Override
				public void learned(ServerPlayer player, String part, String source) {
					HOOKED.add("learned:" + part + ":" + source);
				}
			});
		}
		String only = System.getenv("WILDERCORD_TECHNIQUES");
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
			scene(scenes, "page", failures, "writing on the page", () -> page(context, world));
			scene(scenes, "played", failures, "a technique played", () -> played(context, world));
			scene(scenes, "releases", failures, "the releases", () -> releases(context, world));
			scene(scenes, "intents", failures, "the intents", () -> intents(context, world));
			scene(scenes, "elements", failures, "the methods' elements", () -> elements(context, world));
			scene(scenes, "ranks", failures, "ranks", () -> ranks(context, world));
			scene(scenes, "scrolls", failures, "scrolls and duelists", () -> scrolls(context, world));
			scene(scenes, "slots", failures, "slots by stage", () -> slots(context, world));
			scene(scenes, "awakened", failures, "awakened", () -> awakened(context, world));
			scene(scenes, "fair", failures, "fair against a player", () -> fair(context, world));
			scene(scenes, "lent", failures, "a Way's lent part", () -> lent(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("Techniques went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.getInput().releaseKey(o -> o.keyShift);
			context.getInput().releaseKey(o -> o.keyUse);
			context.getInput().releaseKey(WildercordKeys.auraMapping());
			AuraScreen.listArts(false);
		}
	}

	private static void scene(Set<String> scenes, String id, List<String> failures, String what, Runnable test) {
		if (scenes == null || scenes.contains(id)) {
			try {
				test.run();
			} catch (AssertionError e) {
				failures.add(what + ": " + e.getMessage());
			}
		}
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	// ------------------------------------------------------------------ the page

	private static void page(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE);
			Techniques.clear(player);
			Techniques.teach(player, TechniqueRules.THRUST, "test");
			Techniques.teach(player, TechniqueRules.WAVE, "test");
			Techniques.teach(player, TechniqueRules.SUNDER, "test");
			return null;
		});
		context.waitTicks(4);
		// The tab, clicked from the techniques.
		context.runOnClient(mc -> AuraScreen.listArts(false));
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		click(context, s -> s.writingTabPoint());
		context.waitTicks(3);
		check(context.computeOnClient(mc -> AuraScreen.showingWriting()), "clicking the tab should open the writing page");
		shot(context, "technique_page_empty");
		// Slot I, then each seal's part.
		clickWriting(context, "slot:0");
		clickWriting(context, "seal:stroke");
		check(context.computeOnClient(mc -> mc.gui.screen() instanceof AuraScreen s && s.writingPoint("part:thrust") != null), "the stroke seal opens its parts");
		clickWriting(context, "part:thrust");
		// Picking a stroke moves on to the release, and a release to the intent.
		clickWriting(context, "part:wave");
		clickWriting(context, "part:sunder");
		// The name, typed (an invisible mark in it never gets in: the page takes only what can be shown).
		clickWriting(context, "name");
		check(context.computeOnClient(mc -> TechniquePage.typing()), "clicking the name starts typing it");
		context.getInput().typeChars("Ember Fang​");
		context.waitTicks(2);
		context.getInput().pressKey(InputConstants.KEY_RETURN);
		context.waitTicks(2);
		// A string that is the Second Art's own: refused on the page.
		for (String token : List.of("leap", "low")) {
			clickWriting(context, "token:" + token);
		}
		check(context.computeOnClient(mc -> TechniquePage.draftString()).equals("leap low"), "the swings compose its string ("
			+ context.computeOnClient(mc -> TechniquePage.draftString()) + ")");
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "technique_page_clash");
		int before = Techniques.writtenCount();
		clickWriting(context, "write");
		context.waitTicks(3);
		check(Techniques.writtenCount() == before, "a string that meets an art can't be written");
		// Taken back, and written in full swings and a low one instead.
		for (int i = 0; i < 2; i++) {
			clickWriting(context, "back");
		}
		for (String token : List.of("full", "full", "low")) {
			clickWriting(context, "token:" + token);
		}
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "technique_page_composed");
		clickWriting(context, "write");
		context.waitTicks(6);
		String written = on(world, player -> {
			Techniques.Written w = Techniques.book(player).slot(0);
			if (w.empty()) {
				return "the page should write it into slot I";
			}
			if (!w.name().equals("Ember Fang")) {
				return "its name is kept safe: no formatting, nothing invisible (" + w.name() + ")";
			}
			if (!w.stroke().equals(TechniqueRules.THRUST) || !w.release().equals(TechniqueRules.WAVE) || !w.intent().equals(TechniqueRules.SUNDER)) {
				return "its parts as clicked (" + w + ")";
			}
			if (!w.string().equals("full full low")) {
				return "its string as composed (" + w.string() + ")";
			}
			return dev.wildercord.player.Heart.grimoire(player).contains("aura:technique") ? null : "the first technique goes into the Grimoire";
		});
		check(written == null, written);
		check(HOOKED.stream().anyMatch(h -> h.equals("written:0:Ember Fang")), "the technique hooks hear of it (" + HOOKED + ")");
		check(context.computeOnClient(mc -> Techniques.book(mc.player).slot(0).name()).equals("Ember Fang"), "the client's book follows");
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(30);
		shot(context, "technique_page_written");
		// Hovering a part that isn't known says where to find it.
		clickWriting(context, "seal:release");
		hover(context, "part:burst");
		context.waitTicks(2);
		shot(context, "technique_page_unknown_part");
		context.setScreen(() -> null);
		context.runOnClient(mc -> AuraScreen.listArts(false));
	}

	// ------------------------------------------------------------------ played

	private static void played(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE);
			Techniques.clear(player);
			Techniques.teach(player, TechniqueRules.THRUST, "test");
			Techniques.teach(player, TechniqueRules.WAVE, "test");
			Techniques.teach(player, TechniqueRules.SUNDER, "test");
			check(Techniques.set(player, 0, "Ember Fang", TechniqueRules.THRUST, TechniqueRules.WAVE, TechniqueRules.SUNDER, "full full low"),
				"the technique should be written");
			ServerLevel level = player.level();
			spawn(level, EntityTypes.HUSK, at(0, 2.4), 60, 180);
			spawn(level, EntityTypes.HUSK, at(0.3, 7.0), 60, 180);
			spawn(level, EntityTypes.HUSK, at(-0.3, 10.5), 60, 180);
			return null;
		});
		context.waitTicks(10);
		PERFORMED.clear();
		HITS.clear();
		float auraBefore = on(world, Aura::aura);
		playFullFullLow(context);
		context.waitTicks(4);
		shot(context, "technique_wave_fp");
		context.waitTicks(10);
		check(PERFORMED.contains(TechniqueRules.artId(0)), "its string should play it (" + PERFORMED + ")");
		String verified = on(world, player -> {
			List<Mob> foes = foes(player);
			if (foes.size() < 3) {
				return "every husk should still stand";
			}
			Mob far = foes.get(2);
			if (HITS.getOrDefault(far.getUUID(), 0) < 1) {
				return "the wave should reach the husk ten blocks out (" + HITS + ")";
			}
			Mob near = foes.getFirst();
			Stance.State s = Stance.state(near);
			if (s == null || s.worn() < 6 && s.openUntil() < player.level().getGameTime()) {
				return "a sunder wears a stance hard (" + (s == null ? "untouched" : s.worn()) + ")";
			}
			long ready = SwordStrings.readyAt(player, TechniqueRules.artId(0));
			if (ready <= player.level().getGameTime()) {
				return "it should rest after";
			}
			return null;
		});
		check(verified == null, verified);
		float auraAfter = on(world, Aura::aura);
		check(auraAfter < auraBefore - 3, "its price should be paid (" + auraBefore + " to " + auraAfter + ")");
		check(context.computeOnClient(mc -> dev.wildercord.client.AuraBanners.lastName()).equals("Ember Fang"), "its banner should show its writer's name ("
			+ context.computeOnClient(mc -> dev.wildercord.client.AuraBanners.lastName()) + ")");
		// From behind and above.
		context.waitTicks(80);
		reset(context, world);
		on(world, player -> {
			ServerLevel level = player.level();
			spawn(level, EntityTypes.HUSK, at(0, 2.4), 60, 180);
			spawn(level, EntityTypes.HUSK, at(0.3, 7.0), 60, 180);
			spawn(level, EntityTypes.HUSK, at(-0.3, 10.5), 60, 180);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(Aura.capacity(player)));
			return null;
		});
		thirdPerson(context, world, 0, 12, 5.5, false);
		PERFORMED.clear();
		playFullFullLow(context);
		context.waitTicks(3);
		shot(context, "technique_wave_tp");
		check(PERFORMED.contains(TechniqueRules.artId(0)), "played again from behind (" + PERFORMED + ")");
		context.waitTicks(3);
		shot(context, "technique_wave_tp_flight");
		firstPerson(context, world);
	}

	// ------------------------------------------------------------------ releases

	private record Play(String name, String stroke, String release, String intent, String method, boolean night) {}

	private static void releases(ClientGameTestContext context, TestSingleplayerContext world) {
		for (Play play : List.of(
			new Play("Cinder Sweep", TechniqueRules.SWEEP, TechniqueRules.ON_BLADE, TechniqueRules.BIND, "ember", false),
			new Play("Frost Wheel", TechniqueRules.SPIN, TechniqueRules.BURST, TechniqueRules.ECHO, "rime", false),
			new Play("Falling Shade", TechniqueRules.FALLING, TechniqueRules.AFTERIMAGE, TechniqueRules.PIERCE, "hollow", true),
			new Play("Rising Gale", TechniqueRules.RISING, TechniqueRules.WAVE, TechniqueRules.INFUSE, "gale", false))) {
			for (int view = 0; view < 2; view++) {
				boolean fp = view == 0;
				reset(context, world);
				world.getServer().runCommand(play.night() && !fp ? "time set 18000" : "time set 3000");
				on(world, player -> {
					setAura(player, play.method(), AuraRules.EDGE);
					Techniques.clear(player);
					learnAll(player);
					check(Techniques.set(player, 0, play.name(), play.stroke(), play.release(), play.intent(), "full full low"), play.name() + " should be written");
					ServerLevel level = player.level();
					boolean round = play.release().equals(TechniqueRules.BURST) || play.stroke().equals(TechniqueRules.SPIN);
					spawn(level, EntityTypes.HUSK, at(0, 2.3), 60, 180);
					spawn(level, EntityTypes.HUSK, at(1.6, 2.6), 60, 200);
					spawn(level, EntityTypes.HUSK, at(-1.7, 2.2), 60, 160);
					if (round) {
						spawn(level, EntityTypes.HUSK, at(0.4, -2.4), 60, 0);
					}
					if (play.release().equals(TechniqueRules.WAVE)) {
						spawn(level, EntityTypes.HUSK, at(0, 8.5), 60, 180);
					}
					return null;
				});
				context.waitTicks(10);
				if (!fp) {
					thirdPerson(context, world, 0, 12, 5.5, false);
				}
				PERFORMED.clear();
				HITS.clear();
				context.runOnClient(mc -> AuraClient.afterimageCount());
				playFullFullLow(context);
				String id = play.name().toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
				context.waitTicks(play.release().equals(TechniqueRules.AFTERIMAGE) ? 4 : 3);
				shot(context, "technique_" + play.release() + "_" + id + (fp ? "_fp" : "_tp"));
				if (play.release().equals(TechniqueRules.AFTERIMAGE)) {
					check(context.computeOnClient(mc -> AuraClient.afterimageCount()) > 0, "an afterimage should stand where it was struck");
					// Step back and aside from it, as a swordsman would: the afterimage is left standing ahead (in view past the shoulder),
					// and strikes from there.
					on(world, player -> {
						player.teleportTo(player.level(), player.getX() - 1.5, player.getY(), player.getZ() - 2.4, Set.<Relative>of(), player.getYRot(),
							player.getXRot(), false);
						return null;
					});
					context.waitTicks(TechniqueRules.AFTERIMAGE_RELEASE.delay() - 2);
					shot(context, "technique_afterimage_strike" + (fp ? "_fp" : "_tp"));
				}
				context.waitTicks(16);
				check(PERFORMED.contains(TechniqueRules.artId(0)), play.name() + " should play (" + PERFORMED + ")");
				String verified = on(world, player -> {
					List<Mob> foes = foes(player);
					Mob first = foes.getFirst();
					switch (play.intent()) {
						case TechniqueRules.BIND -> {
							int rooted = 0;
							for (Mob m : foes) {
								rooted += ArtKit.rooted(m) ? 1 : 0;
							}
							if (rooted < 2) {
								return "a bind roots the foes it strikes (" + rooted + ")";
							}
						}
						case TechniqueRules.ECHO -> {
							if (HITS.getOrDefault(first.getUUID(), 0) < 2) {
								return "an echo strikes again (" + HITS.getOrDefault(first.getUUID(), 0) + ")";
							}
						}
						case TechniqueRules.PIERCE -> {
							if (HITS.getOrDefault(first.getUUID(), 0) < 2) {
								return "an afterimage strikes again from where it was struck (" + HITS.getOrDefault(first.getUUID(), 0) + ")";
							}
						}
						default -> {
						}
					}
					if (play.release().equals(TechniqueRules.BURST)) {
						Mob behind = foes.stream().filter(m -> m.getZ() < STAGE.getZ()).findFirst().orElse(null);
						if (behind == null || HITS.getOrDefault(behind.getUUID(), 0) < 1) {
							return "a burst strikes the foe behind too";
						}
					}
					if (play.release().equals(TechniqueRules.WAVE)) {
						Mob far = foes.stream().filter(m -> m.getZ() > STAGE.getZ() + 6).findFirst().orElse(null);
						if (far == null || HITS.getOrDefault(far.getUUID(), 0) < 1) {
							return "a wave reaches the foe far out";
						}
					}
					if (play.method().equals("ember") && first.getRemainingFireTicks() <= 0) {
						return "an Ember technique sets its foes alight";
					}
					if (play.method().equals("rime") && !first.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)) {
						return "a Rime technique chills its foes";
					}
					return null;
				});
				check(verified == null, play.name() + ": " + verified);
				context.waitTicks(20);
			}
			firstPerson(context, world);
		}
		world.getServer().runCommand("time set 3000");
	}

	// ------------------------------------------------------------------ intents

	private static void intents(ClientGameTestContext context, TestSingleplayerContext world) {
		// Ward, lent by the Bulwark: the swordsman steadied.
		reset(context, world);
		on(world, player -> {
			setAura(player, "stone", AuraRules.EDGE);
			Techniques.clear(player);
			Ways.set(player, WayRules.BULWARK);
			check(Techniques.knows(player, TechniqueRules.WARD), "the Bulwark lends its ward");
			check(!Techniques.learned(player, TechniqueRules.WARD), "lent, not learned");
			check(Techniques.set(player, 0, "Mountain Shell", TechniqueRules.DRAW, TechniqueRules.ON_BLADE, TechniqueRules.WARD, "full full low"), "written");
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.3), 60, 180);
			return null;
		});
		context.waitTicks(10);
		thirdPerson(context, world, 0, 12, 4.5, false);
		playFullFullLow(context);
		context.waitTicks(4);
		shot(context, "technique_ward_tp");
		check(on(world, player -> WayBanner.steadiness(player)) >= 0.19, "a ward steadies its swordsman (" + on(world, WayBanner::steadiness) + ")");
		firstPerson(context, world);
		// Rally, lent by the Banner: an ally steadied too.
		reset(context, world);
		on(world, player -> {
			setAura(player, "verdant", AuraRules.EDGE);
			Techniques.clear(player);
			learnAll(player);
			Ways.set(player, WayRules.BANNER);
			check(Techniques.set(player, 0, "Grove Call", TechniqueRules.SWEEP, TechniqueRules.ON_BLADE, TechniqueRules.RALLY, "full full low"), "written");
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.3), 60, 180);
			Ally a = ally(player);
			Scoreboard board = player.level().getScoreboard();
			PlayerTeam team = board.getPlayerTeam("wc_tech") == null ? board.addPlayerTeam("wc_tech") : board.getPlayerTeam("wc_tech");
			board.addPlayerToTeam(player.getScoreboardName(), team);
			board.addPlayerToTeam(a.getScoreboardName(), team);
			return null;
		});
		context.waitTicks(10);
		thirdPerson(context, world, 0, 12, 5.0, false);
		PERFORMED.clear();
		playFullFullLow(context);
		context.waitTicks(4);
		shot(context, "technique_rally_tp");
		check(PERFORMED.contains(TechniqueRules.artId(0)), "the rally should play (" + PERFORMED + ")");
		check(on(world, player -> WayBanner.steadiness(ally) >= 0.09), "a rally steadies an ally near (" + on(world, p -> WayBanner.steadiness(ally)) + ")");
		dropAlly(world);
		firstPerson(context, world);
		// Infuse: an Ember technique's fire burns twice as long.
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE);
			Techniques.clear(player);
			check(Techniques.set(player, 0, "Blazing Draw", TechniqueRules.DRAW, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE, "full full low"), "written");
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.3), 60, 180);
			return null;
		});
		context.waitTicks(10);
		playFullFullLow(context);
		context.waitTicks(4);
		int fire = on(world, player -> foes(player).getFirst().getRemainingFireTicks());
		check(fire >= TechniqueRules.flavour(dev.wildercord.aura.BreathingMethod.Flavour.IGNITE).ignite() * 2 - 12,
			"an infused Ember technique burns about twice as long (" + fire + ")");
		shot(context, "technique_infuse_fp");
	}

	// ------------------------------------------------------------------ the methods' elements

	private static void elements(ClientGameTestContext context, TestSingleplayerContext world) {
		// Verdant mends its swordsman, Starlit gives aura back, Thunder throws a spark on: one strike each, the same technique.
		for (String method : List.of("verdant", "starlit", "thunder", "crimson")) {
			reset(context, world);
			on(world, player -> {
				setAura(player, method, AuraRules.EDGE);
				Techniques.clear(player);
				learnAll(player);
				check(Techniques.set(player, 0, "Test Sweep", TechniqueRules.SWEEP, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE, "full full low"), "written");
				spawn(player.level(), EntityTypes.HUSK, at(0, 2.3), 60, 180);
				spawn(player.level(), EntityTypes.HUSK, at(1.4, 2.6), 60, 180);
				spawn(player.level(), EntityTypes.HUSK, at(2.6, 4.2), 60, 180);
				player.setHealth(10);
				return null;
			});
			context.waitTicks(10);
			float health = on(world, LivingEntity::getHealth);
			HITS.clear();
			playFullFullLow(context);
			context.waitTicks(12);
			String verified = on(world, player -> switch (method) {
				case "verdant" -> player.getHealth() > health + 0.9 ? null : "Verdant mends its swordsman (" + health + " to " + player.getHealth() + ")";
				case "starlit" -> null;
				case "thunder" -> foes(player).stream().filter(m -> HITS.getOrDefault(m.getUUID(), 0) > 0).count() >= 3 ? null
					: "Thunder's spark leaps to a foe the sweep missed (" + HITS + ")";
				case "crimson" -> dev.wildercord.cast.Reactions.has(foes(player).getFirst(), dev.wildercord.cast.Reactions.Mark.BLEEDING) ? null
					: "Crimson opens a wound that bleeds";
				default -> null;
			});
			check(verified == null, method + ": " + verified);
			shot(context, "technique_element_" + method + "_fp");
		}
	}

	// ------------------------------------------------------------------ ranks

	private static void ranks(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "starlit", AuraRules.FORM);
			Techniques.clear(player);
			check(Techniques.set(player, 0, "Star Draw", TechniqueRules.DRAW, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE, "full full low"), "written");
			Techniques.setXp(player, 0, TechniqueRules.threshold(TechniqueRules.HONED) - 0.5);
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.3), 60, 180);
			return null;
		});
		context.waitTicks(10);
		HOOKED.clear();
		PERFORMED.clear();
		String view = clientView(context);
		playFullFullLow(context);
		context.waitTicks(8);
		check(PERFORMED.contains(TechniqueRules.artId(0)), "it should play (" + PERFORMED + "; " + view + ")");
		check(on(world, p -> Techniques.rank(p, Techniques.book(p).slot(0))) == TechniqueRules.HONED, "a use on a real foe carries it into Honed ("
			+ on(world, p -> Techniques.book(p).honing(Techniques.book(p).slot(0).key()).xp()) + ")");
		check(HOOKED.contains("ranked:0:" + TechniqueRules.HONED), "the hooks hear the rank (" + HOOKED + ")");
		shot(context, "technique_rank_up_fp");
		// Its temper, chosen on the page.
		context.runOnClient(mc -> AuraScreen.showWriting(true, 0));
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		clickWriting(context, "temper:swift");
		context.waitTicks(4);
		check(on(world, p -> Techniques.book(p).honing(Techniques.book(p).slot(0).key()).temper()).equals(TechniqueRules.SWIFT),
			"clicking Swift tempers it");
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "technique_page_honed");
		// The edge waits for Keen.
		clickWriting(context, "edge:long");
		context.waitTicks(4);
		check(on(world, p -> Techniques.book(p).honing(Techniques.book(p).slot(0).key()).edge()).isEmpty(), "its edge waits for Keen");
		context.setScreen(() -> null);
		// Peerless: its parts set down on scrolls, its banner grand.
		on(world, player -> {
			Techniques.setXp(player, 0, TechniqueRules.threshold(TechniqueRules.PEERLESS) + 10);
			check(Techniques.teach(player, TechniqueRules.SWEEP, "test"), "a part to inscribe");
			check(Techniques.set(player, 0, "Star Draw", TechniqueRules.SWEEP, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE, "full full low"), "rewritten");
			Techniques.setXp(player, 0, TechniqueRules.threshold(TechniqueRules.PEERLESS) + 10);
			player.getInventory().add(new ItemStack(Items.PAPER, 2));
			player.getInventory().add(new ItemStack(AuraWorld.AURA_SHARD, 1));
			return null;
		});
		context.waitTicks(4);
		context.runOnClient(mc -> AuraScreen.showWriting(true, 0));
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "technique_page_peerless");
		clickWriting(context, "inscribe:" + TechniqueRules.SWEEP);
		context.waitTicks(4);
		check(on(world, p -> p.getInventory().contains(s -> s.is(TechniqueScrollItem.SCROLL)
			&& TechniqueRules.SWEEP.equals(s.get(TechniqueScrollItem.PART)))), "a Peerless technique sets a part down on a scroll");
		check(on(world, p -> !p.getInventory().contains(new ItemStack(AuraWorld.AURA_SHARD))), "it takes the Aura Shard");
		context.setScreen(() -> null);
		on(world, player -> {
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.3), 60, 180);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(Aura.capacity(player)));
			player.removeAttached(SwordStrings.COOLDOWNS);
			return null;
		});
		context.waitTicks(6);
		thirdPerson(context, world, 0, 12, 5.0, false);
		playFullFullLow(context);
		context.waitTicks(4);
		shot(context, "technique_peerless_tp");
		firstPerson(context, world);
	}

	// ------------------------------------------------------------------ scrolls and duelists

	private static void scrolls(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		HOOKED.clear();
		on(world, player -> {
			setAura(player, "rime", AuraRules.FLOW);
			Techniques.clear(player);
			player.setItemInHand(InteractionHand.MAIN_HAND, TechniqueScrollItem.of(TechniqueRules.BIND));
			player.getInventory().add(TechniqueScrollItem.of(TechniqueRules.BIND));
			return null;
		});
		context.waitTicks(4);
		shot(context, "technique_scroll_hand_fp");
		context.getInput().pressKey(o -> o.keyUse);
		context.waitTicks(4);
		check(on(world, p -> Techniques.learned(p, TechniqueRules.BIND)), "reading a scroll teaches its part (even before Edge: it waits)");
		check(HOOKED.contains("learned:" + TechniqueRules.BIND + ":scroll"), "the hooks hear of it (" + HOOKED + ")");
		check(on(world, p -> dev.wildercord.player.Heart.grimoire(p).contains("aura:technique_part")), "the first part goes into the Grimoire");
		on(world, player -> {
			player.setItemInHand(InteractionHand.MAIN_HAND, TechniqueScrollItem.of(TechniqueRules.BIND));
			return null;
		});
		context.waitTicks(2);
		context.getInput().pressKey(o -> o.keyUse);
		context.waitTicks(4);
		check(on(world, p -> p.getMainHandItem().is(TechniqueScrollItem.SCROLL)), "a scroll already known is kept, to give away");
		// A duelist's lesson: a part not known yet.
		String shown = on(world, player -> {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			return Techniques.duelistLesson(player, dev.wildercord.aura.BreathingMethods.THUNDER, net.minecraft.network.chat.Component.literal("Duelist"))
				.orElse("");
		});
		check(!shown.isEmpty() && !shown.equals(TechniqueRules.BIND), "a duelist shows a part not known yet (" + shown + ")");
		check(on(world, p -> Techniques.learned(p, shown)), "and it's learned");
		// Every scroll part known: a duelist has nothing left to show.
		on(world, player -> {
			TechniqueRules.scrollParts().forEach(p -> Techniques.teach(player, p, "test"));
			return null;
		});
		check(on(world, p -> Techniques.duelistLesson(p, dev.wildercord.aura.BreathingMethods.EMBER, net.minecraft.network.chat.Component.literal("Duelist"))
			.isEmpty()), "nothing left to show");
		// The page before Edge: every part found waits, the slots locked.
		context.runOnClient(mc -> AuraScreen.showWriting(true, 0));
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		clickWriting(context, "seal:intent");
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "technique_page_before_edge");
		context.setScreen(() -> null);
	}

	// ------------------------------------------------------------------ slots by stage

	private static void slots(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "thunder", AuraRules.EDGE);
			Techniques.clear(player);
			return null;
		});
		context.waitTicks(3);
		check(on(world, Techniques::slots) == 1, "one slot at Edge");
		String refused = on(world, player -> {
			boolean wrote = Techniques.write(player, new Techniques.Write(1, "Forged", TechniqueRules.DRAW, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE,
				"full full low"));
			return wrote ? "the second slot is closed at Edge" : null;
		});
		check(refused == null, refused);
		String clash = on(world, player -> {
			boolean wrote = Techniques.write(player, new Techniques.Write(0, "Clash", TechniqueRules.DRAW, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE,
				"leap low"));
			return wrote ? "a string that is an art's own can't be written, even asked for straight" : null;
		});
		check(clash == null, clash);
		String forged = on(world, player -> {
			boolean wrote = Techniques.write(player, new Techniques.Write(0, "§cRed §lFang‮", TechniqueRules.DRAW, TechniqueRules.ON_BLADE,
				TechniqueRules.INFUSE, "full full low"));
			String name = Techniques.book(player).slot(0).name();
			return wrote && name.equals("Red Fang") ? null : "a name asked for straight is made safe (" + name + ")";
		});
		check(forged == null, forged);
		on(world, player -> {
			Techniques.clear(player);
			return null;
		});
		String plain = on(world, player -> {
			boolean wrote = Techniques.write(player, new Techniques.Write(0, "Plain", TechniqueRules.DRAW, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE,
				"full full"));
			return wrote ? "full swings alone would go off in any fight: refused" : null;
		});
		check(plain == null, plain);
		String unknown = on(world, player -> {
			boolean wrote = Techniques.write(player, new Techniques.Write(0, "Unknown", TechniqueRules.SPIN, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE,
				"full full low"));
			return wrote ? "a part not known can't be written" : null;
		});
		check(unknown == null, unknown);
		on(world, player -> {
			setAura(player, "thunder", AuraRules.SOVEREIGN);
			return null;
		});
		context.waitTicks(3);
		check(on(world, Techniques::slots) == 3, "three slots at Sovereign");
		on(world, player -> {
			check(Techniques.set(player, 0, "Storm Lance", TechniqueRules.DRAW, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE, "full full low"), "I");
			check(Techniques.write(player, new Techniques.Write(1, "Thunder Draw", TechniqueRules.DRAW, TechniqueRules.ON_BLADE,
				TechniqueRules.INFUSE, "run low")), "the second slot opens at Form");
			return null;
		});
		check(on(world, player -> !Techniques.write(player, new Techniques.Write(2, "Echo of II", TechniqueRules.DRAW, TechniqueRules.ON_BLADE,
			TechniqueRules.INFUSE, "run low"))), "two techniques can't share a string");
		context.runOnClient(mc -> AuraScreen.showWriting(true, 2));
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "technique_page_sovereign");
		context.setScreen(() -> null);
	}

	// ------------------------------------------------------------------ awakened

	private static void awakened(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "crimson", AuraRules.EDGE);
			Techniques.clear(player);
			check(Techniques.set(player, 0, "Red Draw", TechniqueRules.DRAW, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE, "full full low"), "written");
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(Aura.capacity(player)));
			player.setAttached(Momentum.MOMENTUM, new Momentum.State(80, player.level().getGameTime() + 100000, 0, 0, 0));
			check(AuraApi.awaken(player), "the swordsman should awaken");
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.3), 60, 180);
			return null;
		});
		context.waitTicks(10);
		float before = on(world, Aura::aura);
		// What it would cost unawakened: the three coated swings spend less than that between them, so a technique charged would show.
		double price = on(world, player -> {
			Techniques.Written w = Techniques.book(player).slot(0);
			return TechniqueRules.cost(Techniques.pricedWorth(player, w), w.sword().orElseThrow());
		});
		PERFORMED.clear();
		playFullFullLow(context);
		context.waitTicks(6);
		check(PERFORMED.contains(TechniqueRules.artId(0)), "it plays while awakened (" + PERFORMED + ")");
		float after = on(world, Aura::aura);
		check(before - after < price, "free while awakened, as the arts are (" + before + " to " + after + ", its price " + price
			+ ": only the coated swings spend)");
		calm(context, world);
	}

	// ------------------------------------------------------------------ fair against a player

	private static void fair(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			Techniques.clear(player);
			Techniques.teach(player, TechniqueRules.FALLING, "test");
			Techniques.teach(player, TechniqueRules.BIND, "test");
			check(Techniques.set(player, 0, "Heavy Fall", TechniqueRules.FALLING, TechniqueRules.ON_BLADE, TechniqueRules.BIND, "full full low"), "written");
			Techniques.setXp(player, 0, TechniqueRules.threshold(TechniqueRules.PEERLESS));
			Techniques.choose(player, 0, false, TechniqueRules.HEAVY);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH, 400, 4));
			rival(player);
			return null;
		});
		context.waitTicks(10);
		float before = on(world, p -> rival.getHealth());
		PERFORMED.clear();
		String view = clientView(context);
		// The rival stands where the swings land; the first swings hit them (and the duel's blows are theirs to take).
		playFullFullLow(context);
		context.waitTicks(4);
		check(PERFORMED.contains(TechniqueRules.artId(0)), "it plays on a rival (" + PERFORMED + "; " + view + ")");
		String held = on(world, player -> {
			int hold = rival.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS) == null ? 0 : rival.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)
				.getDuration();
			return hold <= dev.wildercord.aura.ArtRules.PVP_HOLD_TICKS + 1 ? null : "a bind holds a player at most the arts' cap (" + hold + ")";
		});
		check(held == null, held);
		dropRival(world);
	}

	// ------------------------------------------------------------------ a Way's lent part

	private static void lent(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "gale", AuraRules.FORM);
			Techniques.clear(player);
			Ways.set(player, WayRules.SHADOWSTEP);
			check(Techniques.knows(player, TechniqueRules.AFTERIMAGE), "the Shadowstep lends its afterimage");
			check(Techniques.set(player, 0, "Gale Shadow", TechniqueRules.DRAW, TechniqueRules.AFTERIMAGE, TechniqueRules.INFUSE, "full full low"), "written");
			check(Techniques.usable(player, 0), "playable while the Way is walked");
			Ways.set(player, "");
			check(!Techniques.usable(player, 0), "left the Way, the technique rests");
			return null;
		});
		context.waitTicks(4);
		context.runOnClient(mc -> AuraScreen.showWriting(true, 0));
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		hover(context, "slot:0");
		context.waitTicks(2);
		shot(context, "technique_page_lent_gone");
		context.setScreen(() -> null);
		check(context.computeOnClient(mc -> !Techniques.usable(mc.player, 0)), "the client knows it rests too");
	}

	// ------------------------------------------------------------------ playing and clicking

	/** What the swordsman's own client thinks of the technique in slot I (for a failure's message). */
	private static String clientView(ClientGameTestContext context) {
		return context.computeOnClient(mc -> "client: usable " + Techniques.usable(mc.player, 0) + ", strings "
			+ AuraApi.stringsOf(mc.player).stream().map(AuraApi.StringArt::id).toList() + ", aura " + Aura.aura(mc.player) + ", stage " + Aura.stage(mc.player)
			+ ", known " + Techniques.known(mc.player));
	}

	/** Two full swings and a low one, with the real keys (the blade fully recovered first: a weapon just put in hand isn't). */
	private static void playFullFullLow(ClientGameTestContext context) {
		for (int t = 0; t < 30 && context.computeOnClient(mc -> mc.player.getAttackStrengthScale(0.5F) < 0.99F); t++) {
			context.waitTicks(1);
		}
		swing(context);
		context.waitTicks(FULL);
		swing(context);
		context.waitTicks(FULL);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(2);
		swing(context);
		context.waitTicks(2);
		context.getInput().releaseKey(o -> o.keyShift);
	}

	private static void swing(ClientGameTestContext context) {
		context.getInput().pressKey(o -> o.keyAttack);
	}

	private static void click(ClientGameTestContext context, Function<AuraScreen, double[]> where) {
		double[] p = context.computeOnClient(mc -> mc.gui.screen() instanceof AuraScreen s ? where.apply(s) : null);
		check(p != null, "nothing to click there");
		double guiScale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(p[0] * guiScale, p[1] * guiScale);
		context.waitTicks(1);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(2);
	}

	private static void clickWriting(ClientGameTestContext context, String key) {
		context.waitTicks(1);
		double[] p = context.computeOnClient(mc -> mc.gui.screen() instanceof AuraScreen s ? s.writingPoint(key) : null);
		check(p != null, "the writing page should draw " + key);
		double guiScale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(p[0] * guiScale, p[1] * guiScale);
		context.waitTicks(1);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(2);
	}

	private static void hover(ClientGameTestContext context, String key) {
		double[] p = context.computeOnClient(mc -> mc.gui.screen() instanceof AuraScreen s ? s.writingPoint(key) : null);
		check(p != null, "the writing page should draw " + key);
		double guiScale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(p[0] * guiScale, p[1] * guiScale);
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------ views

	private static void thirdPerson(ClientGameTestContext context, TestSingleplayerContext world, float yaw, float pitch, double distance, boolean hud) {
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			if (mc.gui.hud.isHidden() == hud) {
				mc.gui.hud.toggle();
			}
		});
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(distance);
			// The swordsman keeps facing the foes; the camera's angle is the pitch looked down at.
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), yaw, pitch, false);
			return null;
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
		dropRival(world);
		dropAlly(world);
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runCommand("fill " + (STAGE.getX() - 14) + " " + STAGE.getY() + " " + (STAGE.getZ() - 14) + " " + (STAGE.getX() + 14) + " "
			+ (STAGE.getY() + 10) + " " + (STAGE.getZ() + 24) + " minecraft:air");
		context.waitTicks(6);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			if (player.level().getScoreboard().getPlayersTeam(player.getScoreboardName()) != null) {
				player.level().getScoreboard().removePlayerFromTeam(player.getScoreboardName());
			}
			player.removeAttached(AuraAttachments.STATE);
			player.removeAttached(SwordStrings.COOLDOWNS);
			player.removeAttached(Momentum.MOMENTUM);
			player.removeAttached(Stance.STANCE);
			player.setAttached(AuraPresence.TIMERS, AuraPresence.Timers.NONE);
			Ways.set(player, "");
			AuraDominion.end(player);
			dev.wildercord.aura.arts.MethodArts.forget(player.getUUID());
		});
		firstPerson(context, world);
		context.runOnClient(mc -> {
			mc.player.setXRot(8);
			mc.player.xRotO = 8;
		});
		// Out of the heat of the last fight (writing waits for it) and of the last string.
		context.waitTicks(StringRules.ENGAGED_TICKS + 10);
	}

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

	/** Every part a scroll can carry, learned (a technique written with parts not known rests, and its string plays the art beneath). */
	private static void learnAll(ServerPlayer player) {
		for (String part : TechniqueRules.scrollParts()) {
			Techniques.teach(player, part, "test");
		}
	}

	private static Vec3 at(double side, double ahead) {
		return new Vec3(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead);
	}

	/** A foe that stands its ground (no speed, no sight, its AI left on), facing {@code yaw}. */
	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, Vec3 at, double health, float yaw) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		if (mob == null) {
			throw new AssertionError("couldn't make a " + type);
		}
		mob.snapTo(at.x, at.y, at.z, yaw, 0);
		mob.setYHeadRot(yaw);
		mob.setYBodyRot(yaw);
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
		// Standing its ground against the swings' knockback too, so the string's every swing lands on it (a foe knocked out of reach
		// leaves the crosshair on the ground, and a swing at a block is no swing of a string).
		AttributeInstance steady = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (steady != null) {
			steady.setBaseValue(1.0);
		}
		mob.setHealth((float) health);
		level.addFreshEntity(mob);
		return mob;
	}

	/** The scene's husks, in the order they were spawned. */
	private static List<Mob> foes(ServerPlayer player) {
		List<Mob> found = new ArrayList<>(player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48),
			m -> m.entityTags().contains(TAG) && m.getType() == EntityTypes.HUSK && m.isAlive()));
		found.sort(java.util.Comparator.comparingInt(Mob::getId));
		if (found.isEmpty()) {
			throw new AssertionError("the foes are gone");
		}
		return found;
	}

	// ------------------------------------------------------------------ other players

	/** Another player for a duel (a fake one that can be hurt). */
	private static final class Rival extends FakePlayer {
		Rival(ServerLevel level) {
			super(level, new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("wildercord-techniques-rival".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
				"Rival"));
		}

		@Override
		public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
			return false;
		}

		@Override
		public PlayerTeam getTeam() {
			return level().getScoreboard().getPlayersTeam(getScoreboardName());
		}
	}

	/** A friend beside the swordsman (a fake player on their team). */
	private static final class Ally extends FakePlayer {
		Ally(ServerLevel level) {
			super(level, new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("wildercord-techniques-ally".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
				"Ally"));
		}

		@Override
		public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
			return false;
		}

		@Override
		public PlayerTeam getTeam() {
			return level().getScoreboard().getPlayersTeam(getScoreboardName());
		}
	}

	private static Rival rival;
	private static Ally ally;

	private static Rival rival(ServerPlayer player) {
		if (rival == null || rival.isRemoved() || rival.level() != player.level()) {
			rival = new Rival(player.level());
			Vec3 p = at(0, 2.2);
			rival.snapTo(p.x, p.y, p.z, 180, 0);
			rival.setYHeadRot(180);
			player.connection.send(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(rival)));
			player.level().addNewPlayer(rival);
		}
		return rival;
	}

	private static Ally ally(ServerPlayer player) {
		if (ally == null || ally.isRemoved() || ally.level() != player.level()) {
			ally = new Ally(player.level());
			Vec3 p = at(-2.0, -0.5);
			ally.snapTo(p.x, p.y, p.z, 0, 0);
			player.connection.send(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(ally)));
			player.level().addNewPlayer(ally);
		}
		return ally;
	}

	private static void dropRival(TestSingleplayerContext world) {
		on(world, player -> {
			if (rival != null) {
				rival.discard();
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(List.of(rival.getUUID())));
				rival = null;
			}
			return null;
		});
	}

	private static void dropAlly(TestSingleplayerContext world) {
		on(world, player -> {
			if (ally != null) {
				ally.discard();
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(List.of(ally.getUUID())));
				ally = null;
			}
			return null;
		});
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
