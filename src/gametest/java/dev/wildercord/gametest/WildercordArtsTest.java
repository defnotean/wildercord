package dev.wildercord.gametest;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.StringRules;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.aura.arts.ArtBlocks;
import dev.wildercord.aura.arts.ArtFields;
import dev.wildercord.aura.arts.ArtWards;
import dev.wildercord.aura.arts.EmberArts;
import dev.wildercord.aura.arts.GaleArts;
import dev.wildercord.aura.arts.RimeArts;
import dev.wildercord.client.AuraFxClient;
import dev.wildercord.client.AuraScreen;
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
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The breathing methods' arts, step 3 of the aura overhaul: all twenty-five of Ember, Rime, Thunder, Gale and Stone played with
 * the real keys (the attack key, sneak, jump, and the Aura key for the guard and the step), on a long stone platform in the sky,
 * against husks that stand their ground (their legs and their sight taken away, their AI left, so a lift or a throw shows):
 * <ul>
 *   <li>each method's five are registered on the five strings, and the common arts are left for a method without its own;</li>
 *   <li>each art, played by its string, is the one performed (never the common art, never another), its price spent and its rest
 *       begun, it goes in the Grimoire, and it does what it says to the husks: set alight, frozen, shattered, struck by lightning,
 *       chained, lifted, thrown, carried, cracked; the swordsman rushed, blinked, leapt or hardened; fields laid on the ground;
 *       Skate's frost on water (frosted ice), the mirror and the eye turning arrows;</li>
 *   <li>each art filmed from inside it (first person, as it's played) and from behind and above (third person, played again);</li>
 *   <li>the Aura page's Sword strings tab: your own method's arts, another method's, and the common arts of one still to come.</li>
 * </ul>
 * Screenshots ({@code art_<id>_fp}, {@code art_<id>_tp}, the Final Arts also {@code _night}; {@code art_page_*}).
 *
 * <p>Runs in the full suite; {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it.</p>
 */
public class WildercordArtsTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 190, 0);
	private static final String TAG = "wildercord.arts_test";
	/** The arts the server performed, in order. */
	private static final List<String> PERFORMED = Collections.synchronizedList(new ArrayList<>());
	private static boolean hooked;
	/** A sword's full swing comes back in 11 ticks: a swing as soon as it's full again. */
	private static final int FULL = 13;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
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
			run(failures, "the arts and their strings", () -> registry(context));
			// WILDERCORD_ARTS=a,b plays only those (for working on a few); the page always.
			String only = System.getenv("WILDERCORD_ARTS");
			for (Scene scene : scenes()) {
				if (only != null && !only.isBlank() && !List.of(only.split(",")).contains(scene.id)) {
					continue;
				}
				reset(context, world);
				run(failures, scene.id, () -> play(context, world, scene));
			}
			reset(context, world);
			run(failures, "the Aura page", () -> page(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("The methods' arts went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				AuraScreen.listArts(false);
				AuraScreen.browse(null);
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
			for (String method : List.of("ember", "rime", "thunder", "gale", "stone")) {
				List<AuraApi.StringArt> arts = AuraApi.arts(method);
				if (arts.size() != 5 || !AuraApi.hasArts(method)) {
					return method + " should have five arts of its own on the client (" + arts.size() + ")";
				}
				for (int i = 0; i < 5; i++) {
					AuraApi.StringArt art = arts.get(i);
					if (AuraApi.ArtSlot.of(art).orElse(null) != AuraApi.ArtSlot.values()[i] || !ArtRules.art(art.id()).method().equals(method)) {
						return art.id() + " should be " + method + "'s art in slot " + i;
					}
				}
			}
			return AuraApi.hasArts("verdant") ? "verdant should still play the common arts" : null;
		});
		check(problem == null, problem);
		check(AuraApi.hasArts("ember") && AuraApi.arts("stone").size() == 5, "the arts should be registered on the server too");
	}

	// ------------------------------------------------------------------ the scenes

	/** How an art is played: its id, method, slot, where the husks stand, and when to film it (ticks after the last swing). */
	private record Scene(String id, String method, AuraApi.ArtSlot slot, List<double[]> foes, int fpDelay, int tpDelay, Check check) {}

	/** What an art must have done, asked on the server once it's had time ({@code at} the moment its last swing went). */
	@FunctionalInterface
	private interface Check {
		String verify(ServerPlayer player, Before before);
	}

	/** What stood where before the art went: each husk's position and health, the swordsman's spot and aura. */
	private record Before(Map<Integer, Vec3> at, Map<Integer, Float> health, Vec3 player, float aura) {
		Vec3 at(LivingEntity e) {
			return at.getOrDefault(e.getId(), e.position());
		}

		float health(LivingEntity e) {
			return health.getOrDefault(e.getId(), e.getMaxHealth());
		}
	}

	private static double[] foe(double side, double ahead) {
		return new double[] {side, ahead};
	}

	private static List<Scene> scenes() {
		List<Scene> out = new ArrayList<>();
		// ------------------------------------------------------------ Ember
		out.add(new Scene(EmberArts.KINDLING_DRAW, "ember", AuraApi.ArtSlot.FIRST, List.of(foe(0, 2.2), foe(0.3, 5.2)), 2, 6, (p, b) -> {
			List<Mob> foes = foes(p);
			Mob near = foes.getFirst();
			Mob far = foes.get(1);
			if (near.getRemainingFireTicks() <= 0 || near.getHealth() >= b.health(near)) {
				return "the draw-cut should cut the husk in front and set it alight";
			}
			if (ArtFields.count(p, EmberArts.FIRE_LINE) != 1) {
				return "a line of fire should burn on the ground ahead";
			}
			return far.getRemainingFireTicks() > 0 ? null : "the line of fire should set alight the husk standing in it, five blocks on";
		}));
		out.add(new Scene(EmberArts.RISING_CINDERS, "ember", AuraApi.ArtSlot.SECOND, List.of(foe(0, 2.2), foe(1.3, 2.6), foe(2.6, 2.9)), 4, 5, (p, b) -> {
			Mob a = foes(p).getFirst();
			if (a.getRemainingFireTicks() <= 0) {
				return "the cut husk should be alight";
			}
			if (!(dev.wildercord.cast.Reactions.has(a, dev.wildercord.cast.Reactions.Mark.AIRBORNE) || a.getY() > b.at(a).y + 0.3)) {
				return "the husk should have been thrown up (" + (a.getY() - b.at(a).y) + ")";
			}
			return null;
		}));
		out.add(new Scene(EmberArts.BACKDRAFT, "ember", AuraApi.ArtSlot.THIRD, List.of(foe(0, 1.6), foe(1.4, 2.8)), 3, 4, (p, b) -> {
			Mob a = foes(p).getFirst();
			if (a.getRemainingFireTicks() <= 0 || a.getHealth() >= b.health(a)) {
				return "whoever struck should take the gout of flame and burn";
			}
			return a.position().distanceTo(p.position()) > b.at(a).distanceTo(b.player()) + 0.4 ? null : "and be thrown back";
		}));
		out.add(new Scene(EmberArts.WILDFIRE_RUSH, "ember", AuraApi.ArtSlot.FOURTH, List.of(foe(0.4, 9.5), foe(-0.4, 11.5)), 3, 3, (p, b) -> {
			if (p.getZ() < b.player().z + 9) {
				return "the swordsman should rush on past the step (" + (p.getZ() - b.player().z) + " blocks)";
			}
			int alight = 0;
			for (Mob m : foes(p)) {
				if (m.getRemainingFireTicks() > 0 && m.getHealth() < b.health(m)) {
					alight++;
				}
			}
			if (alight < 2) {
				return "both husks in the way should be cut and set alight (" + alight + ")";
			}
			return ArtFields.count(p, EmberArts.FIRE_TRAIL) == 1 ? null : "the ground behind should burn";
		}));
		out.add(new Scene(EmberArts.SUNFALL, "ember", AuraApi.ArtSlot.FINAL, List.of(foe(0, 2.2), foe(-2.5, 1.0), foe(2.6, 0.5), foe(0.5, -2.2)), 22, 20,
			(p, b) -> {
				int hurt = 0;
				for (Mob m : foes(p)) {
					if (m.getHealth() < b.health(m) && m.getRemainingFireTicks() > 0) {
						hurt++;
					}
				}
				if (hurt < 4) {
					return "every husk round where it lands should be struck and set alight (" + hurt + " of 4)";
				}
				if (ArtFields.count(p, EmberArts.FIRE_RING) != 1) {
					return "a ring of fire should stand round the spot";
				}
				return p.getHealth() >= p.getMaxHealth() - 0.01F ? null : "the leap should never hurt its swordsman (" + p.getHealth() + ")";
			}));
		// ------------------------------------------------------------ Rime
		out.add(new Scene(RimeArts.FROSTBITE, "rime", AuraApi.ArtSlot.FIRST, List.of(foe(0, 2.2)), 2, 4, (p, b) -> {
			Mob a = foes(p).getFirst();
			MobEffectInstance slow = a.getEffect(MobEffects.SLOWNESS);
			if (slow == null || slow.getAmplifier() < 1) {
				return "the cut should crust and slow the husk (" + slow + ")";
			}
			if (ArtWards.crusts(a) != 1) {
				return "it should carry one crust (" + ArtWards.crusts(a) + ")";
			}
			// Two more, and it freezes solid.
			AuraApi.StringArt art = AuraApi.string(RimeArts.FROSTBITE).orElseThrow();
			for (int i = 0; i < 2; i++) {
				p.removeAttached(SwordStrings.COOLDOWNS);
				SwordStrings.perform(p, art, marks(art));
			}
			return RimeArts.frozen(a) && a.isNoAi() ? null : "the third crust should freeze it solid";
		}));
		out.add(new Scene(RimeArts.HAILFALL, "rime", AuraApi.ArtSlot.SECOND, List.of(foe(0, 2.2), foe(0.8, 3.6), foe(-1.0, 3.9)), 10, 12, (p, b) -> {
			int struck = 0;
			for (Mob m : foes(p)) {
				MobEffectInstance slow = m.getEffect(MobEffects.SLOWNESS);
				if (m.getHealth() < b.health(m) && slow != null) {
					struck++;
				}
			}
			return struck >= 3 ? null : "the cut and the hail should strike and slow all three husks (" + struck + ")";
		}));
		out.add(new Scene(RimeArts.GLACIER_MIRROR, "rime", AuraApi.ArtSlot.THIRD, List.of(foe(0, 1.6), foe(1.5, 2.4)), 3, 4, (p, b) -> {
			Mob a = foes(p).getFirst();
			if (!RimeArts.frozen(a)) {
				return "whoever struck should be frozen solid";
			}
			if (!ArtWards.mirrored(p)) {
				return "the mirror should stand before the swordsman";
			}
			return arrowTurned(p, true);
		}));
		out.add(new Scene(RimeArts.SKATE, "rime", AuraApi.ArtSlot.FOURTH, List.of(foe(0.3, 9.6), foe(-0.3, 12.0)), 3, 3, (p, b) -> {
			if (p.getZ() < b.player().z + 10) {
				return "the swordsman should glide on past the step (" + (p.getZ() - b.player().z) + " blocks)";
			}
			Mob frozen = foes(p).getFirst();
			if (frozen.getHealth() > b.health(frozen) - 6) {
				return "the frozen husk in the way should shatter, hard (" + (b.health(frozen) - frozen.getHealth()) + ")";
			}
			if (frozen.isNoAi()) {
				return "and thaw";
			}
			if (ArtFields.count(p, RimeArts.ICE_PATH) != 1 || !p.hasEffect(MobEffects.SPEED)) {
				return "the ice path should lie behind, quick under the swordsman";
			}
			BlockPos water = waterSpot();
			return p.level().getBlockState(water).is(Blocks.FROSTED_ICE) ? null : "the water under the glide should be frosted over (" + p.level().getBlockState(water) + ")";
		}));
		out.add(new Scene(RimeArts.WINTERS_HUSH, "rime", AuraApi.ArtSlot.FINAL, List.of(foe(0, 2.2), foe(-1.6, 4.2), foe(1.8, 5.0)), 4, 33, (p, b) -> {
			int shattered = 0;
			for (Mob m : foes(p)) {
				if (b.health(m) - m.getHealth() > 10) {
					shattered++;
				}
			}
			return shattered == 3 ? null : "all three husks in the cone should be frozen and then shattered (" + shattered + ")";
		}));
		// ------------------------------------------------------------ Thunder
		out.add(new Scene(dev.wildercord.aura.arts.ThunderArts.CRACKLE, "thunder", AuraApi.ArtSlot.FIRST, List.of(foe(0, 2.2), foe(2.0, 3.2)), 3, 3,
			(p, b) -> {
				List<Mob> foes = foes(p);
				Mob main = foes.getFirst();
				Mob near = foes.get(1);
				if (main.getHealth() >= b.health(main) - 3) {
					return "three cuts should land on the husk in front (" + (b.health(main) - main.getHealth()) + ")";
				}
				return near.getHealth() < b.health(near) ? null : "the sparks should leap to the husk beside it";
			}));
		out.add(new Scene(dev.wildercord.aura.arts.ThunderArts.SKYFALL, "thunder", AuraApi.ArtSlot.SECOND, List.of(foe(0, 2.2), foe(2.5, 3.0), foe(-2.6, 3.3)),
			7, 8, (p, b) -> {
				int struck = 0;
				for (Mob m : foes(p)) {
					if (m.getHealth() < b.health(m) && dev.wildercord.cast.Reactions.has(m, dev.wildercord.cast.Reactions.Mark.IONISED)) {
						struck++;
					}
				}
				return struck >= 3 ? null : "the bolt should strike the husk and arc to the two beside it, ionised (" + struck + ")";
			}));
		out.add(new Scene(dev.wildercord.aura.arts.ThunderArts.STATIC_RIPOSTE, "thunder", AuraApi.ArtSlot.THIRD,
			List.of(foe(0, 1.6), foe(2.5, 3.0), foe(4.5, 4.0), foe(1.0, 5.6)), 3, 4, (p, b) -> {
				int struck = 0;
				for (Mob m : foes(p)) {
					if (m.getHealth() < b.health(m)) {
						struck++;
					}
				}
				return struck >= 4 ? null : "the lightning should chain from whoever struck through the husks near (" + struck + " of 4)";
			}));
		out.add(new Scene(dev.wildercord.aura.arts.ThunderArts.BOLT_STEP, "thunder", AuraApi.ArtSlot.FOURTH,
			List.of(foe(2.5, 9.0), foe(-2.5, 10.5), foe(0.5, 13.0)), 4, 4, (p, b) -> {
				int struck = 0;
				for (Mob m : foes(p)) {
					if (m.getHealth() < b.health(m)) {
						struck++;
					}
				}
				if (struck < 3) {
					return "the swordsman should blink to and cut all three husks (" + struck + ")";
				}
				return p.position().distanceTo(foes(p).get(2).position()) < 3 ? null : "and end beside the last";
			}));
		out.add(new Scene(dev.wildercord.aura.arts.ThunderArts.HEAVENS_SPEAR, "thunder", AuraApi.ArtSlot.FINAL,
			List.of(foe(0, 2.2), foe(0.2, 7.0), foe(-0.3, 12.0), foe(0.1, 17.0)), 16, 16, (p, b) -> {
				int struck = 0;
				for (Mob m : foes(p)) {
					if (m.getHealth() < b.health(m) - 5 && dev.wildercord.cast.Reactions.has(m, dev.wildercord.cast.Reactions.Mark.IONISED)) {
						struck++;
					}
				}
				return struck >= 4 ? null : "the lance should run through every husk down the line (" + struck + " of 4)";
			}));
		// ------------------------------------------------------------ Gale
		out.add(new Scene(GaleArts.CUTTING_BREEZE, "gale", AuraApi.ArtSlot.FIRST, List.of(foe(0, 2.2), foe(0.3, 8.0)), 3, 3, (p, b) -> {
			Mob far = foes(p).get(1);
			if (far.getHealth() >= b.health(far)) {
				return "the wind blade should cut the husk eight blocks on, far past the sword's reach";
			}
			return far.getZ() > b.at(far).z + 0.3 ? null : "and push it back (" + (far.getZ() - b.at(far).z) + ")";
		}));
		out.add(new Scene(GaleArts.UPDRAFT, "gale", AuraApi.ArtSlot.SECOND, List.of(foe(0, 2.2), foe(1.4, 2.6)), 6, 7, (p, b) -> {
			Mob a = foes(p).getFirst();
			if (a.getY() < b.at(a).y + 0.8) {
				return "the husk should be thrown high (" + (a.getY() - b.at(a).y) + ")";
			}
			if (!p.hasEffect(MobEffects.SLOW_FALLING)) {
				return "the swordsman should rise after it and hang";
			}
			return ArtWards.juggle(p, a) > 1.0 ? null : "while it's up, the swordsman's blade should bite it harder";
		}));
		out.add(new Scene(GaleArts.EYE_OF_THE_STORM, "gale", AuraApi.ArtSlot.THIRD, List.of(foe(0, 1.6), foe(-2.0, -0.5), foe(2.2, 0.8)), 3, 6, (p, b) -> {
			int pushed = 0;
			for (Mob m : foes(p)) {
				if (m.getHealth() < b.health(m) && m.position().distanceTo(p.position()) > b.at(m).distanceTo(b.player()) + 0.3) {
					pushed++;
				}
			}
			if (pushed < 3) {
				return "the spin should cut and throw back every husk round the swordsman (" + pushed + ")";
			}
			if (!ArtWards.inEye(p)) {
				return "the eye should hold round the swordsman";
			}
			return arrowTurned(p, false);
		}));
		out.add(new Scene(GaleArts.TAILWIND, "gale", AuraApi.ArtSlot.FOURTH, List.of(foe(0.2, 10.0)), 3, 3, (p, b) -> {
			if (p.getZ() < b.player().z + 12) {
				return "the swordsman should dash far on past the step (" + (p.getZ() - b.player().z) + " blocks)";
			}
			Mob a = foes(p).getFirst();
			if (a.getHealth() >= b.health(a)) {
				return "the husk in the way should be cut and shoved aside";
			}
			if (!p.hasEffect(MobEffects.SPEED)) {
				return "the wind at the swordsman's back should speed them";
			}
			List<Wolf> wolves = p.level().getEntitiesOfClass(Wolf.class, p.getBoundingBox().inflate(32), w -> w.entityTags().contains(TAG));
			MobEffectInstance swift = wolves.isEmpty() ? null : wolves.getFirst().getEffect(MobEffects.SPEED);
			return swift != null && swift.getAmplifier() >= 1 ? null : "the ally near the way should be swept along faster (" + swift + ")";
		}));
		out.add(new Scene(GaleArts.HUNDRED_WINDS, "gale", AuraApi.ArtSlot.FINAL, List.of(foe(0, 2.2), foe(-3.0, 1.0), foe(3.2, -1.0)), 20, 30, (p, b) -> {
			int cut = 0;
			StringBuilder seen = new StringBuilder();
			for (Mob m : foes(p)) {
				float taken = b.health(m) - m.getHealth();
				seen.append(String.format(java.util.Locale.ROOT, " %.1f taken at %.1f;", taken, m.position().distanceTo(p.position())));
				if (taken > 4) {
					cut++;
				}
			}
			return cut == 3 ? null : "the whirlwind should draw in and cut every husk round the swordsman, again and again (" + cut + ":" + seen + ")";
		}));
		// ------------------------------------------------------------ Stone
		out.add(new Scene(dev.wildercord.aura.arts.StoneArts.ROCKBREAKER, "stone", AuraApi.ArtSlot.FIRST, List.of(foe(0, 2.2), foe(1.2, 2.6)), 2, 4,
			(p, b) -> {
				List<Mob> foes = foes(p);
				Mob a = foes.getFirst();
				if (!dev.wildercord.cast.Reactions.has(a, dev.wildercord.cast.Reactions.Mark.CRACKED)) {
					return "the husk struck should be cracked";
				}
				MobEffectInstance slow = a.getEffect(MobEffects.SLOWNESS);
				if (slow == null || slow.getAmplifier() < 2) {
					return "and heavy-footed (" + slow + ")";
				}
				return foes.get(1).getHealth() < b.health(foes.get(1)) ? null : "the shock should spill onto the husk beside it";
			}));
		out.add(new Scene(dev.wildercord.aura.arts.StoneArts.AVALANCHE, "stone", AuraApi.ArtSlot.SECOND, List.of(foe(0, 2.2), foe(-2.6, 1.6), foe(2.4, -1.5)),
			6, 4, (p, b) -> {
				int thrown = 0;
				for (Mob m : foes(p)) {
					if (m.getHealth() < b.health(m) && m.hasEffect(MobEffects.SLOWNESS)) {
						thrown++;
					}
				}
				return thrown == 3 ? null : "the shockwave should reach and slow every husk round where it landed (" + thrown + ")";
			}));
		out.add(new Scene(dev.wildercord.aura.arts.StoneArts.UNMOVED, "stone", AuraApi.ArtSlot.THIRD, List.of(foe(0, 1.6)), 3, 5, (p, b) -> {
			Mob a = foes(p).getFirst();
			if (a.position().distanceTo(p.position()) < b.at(a).distanceTo(b.player()) + 1.0) {
				return "whoever struck should be hurled back (" + (a.position().distanceTo(p.position()) - b.at(a).distanceTo(b.player())) + ")";
			}
			if (!ArtWards.hardened(p) || !p.hasEffect(MobEffects.RESISTANCE) || p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 0.99) {
				return "and the swordsman hardened like stone";
			}
			return null;
		}));
		out.add(new Scene(dev.wildercord.aura.arts.StoneArts.LANDSLIDE, "stone", AuraApi.ArtSlot.FOURTH, List.of(foe(0, 8.0)), 5, 5, (p, b) -> {
			Mob a = foes(p).getFirst();
			if (a.getZ() < b.at(a).z + 4) {
				return "the husk in front should be carried along and thrown on (" + (a.getZ() - b.at(a).z) + ")";
			}
			return a.getHealth() < b.health(a) ? null : "and struck";
		}));
		out.add(new Scene(dev.wildercord.aura.arts.StoneArts.MOUNTAIN_SPLITTER, "stone", AuraApi.ArtSlot.FINAL,
			List.of(foe(0, 2.2), foe(0.4, 6.0), foe(-0.5, 10.0), foe(0.3, 13.5)), 12, 14, (p, b) -> {
				int thrown = 0;
				for (Mob m : foes(p)) {
					if (m.getHealth() < b.health(m) - 5) {
						thrown++;
					}
				}
				if (thrown < 4) {
					return "the rising stone should throw up every husk down the line (" + thrown + " of 4)";
				}
				return ArtBlocks.live() > 0 ? null : "the stone should still be standing";
			}));
		return out;
	}

	// ------------------------------------------------------------------ playing a scene

	private static void play(ClientGameTestContext context, TestSingleplayerContext world, Scene scene) {
		// Night for the Final Arts' third-person picture, where their light reads best; everything else by day.
		set(world, scene);
		context.waitTicks(20);
		PERFORMED.clear();
		Before before = on(world, player -> snapshot(player));
		// ---- played with the keys, in first person.
		boolean step = scene.slot == AuraApi.ArtSlot.FOURTH;
		switch (scene.slot) {
			case FIRST -> {
				swing(context);
				regroup(context, world, scene);
				context.waitTicks(FULL - 2);
				swing(context);
				regroup(context, world, scene);
				context.waitTicks(FULL - 7);
				lowSwing(context);
			}
			case SECOND -> {
				context.runOnClient(mc -> {
					mc.player.setXRot(25);
					mc.player.xRotO = 25;
				});
				context.waitTicks(2);
				context.getInput().holdKeyFor(o -> o.keyJump, 1);
				context.waitTicks(4);
				swing(context);
				regroup(context, world, scene);
				for (int t = 0; t < 14 && !context.computeOnClient(mc -> mc.player.onGround()); t++) {
					context.waitTicks(1);
				}
				// Down again: the low swing at the husk in front, not the ground.
				context.runOnClient(mc -> {
					mc.player.setXRot(16);
					mc.player.xRotO = 16;
				});
				lowSwing(context);
			}
			case THIRD -> {
				context.getInput().holdKey(o -> o.keyShift);
				context.waitTicks(2);
				context.getInput().pressKey(WildercordKeys.auraMapping());
				for (int t = 0; t < 4 && !on(world, player -> AuraGuard.perfectNow(player)); t++) {
					context.waitTicks(1);
				}
				float taken = on(world, player -> {
					Mob attacker = foes(player).getFirst();
					float hp = player.getHealth();
					attacker.doHurtTarget(player.level(), player);
					return hp - player.getHealth();
				});
				check(taken <= 0, scene.id + ": the guard should have been perfect (took " + taken + ")");
				context.waitTicks(2);
				before = on(world, player -> snapshot(player));
				swing(context);
			}
			case FOURTH -> {
				context.getInput().pressKey(WildercordKeys.auraMapping());
				context.waitTicks(2);
				context.getInput().pressKey(WildercordKeys.auraMapping());
				context.waitTicks(5);
				swing(context);
			}
			case FINAL -> {
				for (int i = 0; i < 3; i++) {
					swing(context);
					regroup(context, world, scene);
					context.waitTicks(FULL - 2);
				}
				lowSwing(context);
			}
		}
		context.waitTicks(scene.fpDelay);
		shot(context, "art_" + scene.id + "_fp");
		int settle = Math.max(4, settleTicks(scene) - scene.fpDelay);
		context.waitTicks(settle);
		context.getInput().releaseKey(o -> o.keyShift);
		check(PERFORMED.equals(List.of(scene.id)), scene.id + ": its string should play it, once (" + PERFORMED + ")");
		Before b = before;
		String verified = on(world, player -> scene.check.verify(player, b));
		check(verified == null, scene.id + ": " + verified);
		String paid = on(world, player -> {
			long now = player.level().getGameTime();
			long ready = SwordStrings.readyAt(player, scene.id);
			if (ready <= now - settleTicks(scene) || ready > now + ArtRules.art(scene.id).cooldown()) {
				return "it should rest " + ArtRules.art(scene.id).cooldown() + " ticks (ready at " + ready + ", now " + now + ")";
			}
			return Heart.grimoire(player).contains(ArtRules.grimoireKey(scene.id)) ? null : "it should go into the Grimoire";
		});
		check(paid == null, scene.id + ": " + paid);
		// ---- played again, from behind and above.
		boolean finalArt = scene.slot == AuraApi.ArtSlot.FINAL;
		for (int view = 0; view < (finalArt ? 2 : 1); view++) {
			boolean night = view == 1;
			reset(context, world);
			world.getServer().runCommand(night ? "time set 18000" : "time set 3000");
			set(world, scene);
			on(world, player -> {
				player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(5.5);
				player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, tpPitch(scene), false);
				return null;
			});
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.waitTicks(12);
			int[] spectacle = context.computeOnClient(mc -> AuraFxClient.spectacle());
			String done = on(world, player -> {
				AuraApi.StringArt art = AuraApi.string(scene.id).orElseThrow();
				if (step) {
					// A rush or a blink starts where a step ended: move the swordsman there first.
					player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ() + 6, Set.<Relative>of(), 0.0F, tpPitch(scene), false);
				}
				return SwordStrings.perform(player, art, marks(art)) ? null : scene.id + " didn't go off on the server";
			});
			check(done == null, done);
			context.waitTicks(scene.tpDelay);
			shot(context, "art_" + scene.id + (night ? "_night" : "_tp"));
			if (!night && scene.id.equals(GaleArts.HUNDRED_WINDS) || scene.id.equals(dev.wildercord.aura.arts.ThunderArts.HEAVENS_SPEAR)) {
				int[] after = context.computeOnClient(mc -> AuraFxClient.spectacle());
				check(after[0] > spectacle[0], scene.id + ": its spectacle should show to its swordsman in third person");
			}
			context.waitTicks(Math.max(4, settleTicks(scene) - scene.tpDelay));
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

	/**
	 * How far down the swordsman looks for the third-person picture (the camera follows the view): down on the scene from behind,
	 * except for Heaven's Spear, whose lance follows a clear aim up or down, so it's filmed looking along the line.
	 */
	private static float tpPitch(Scene scene) {
		if (scene.id.equals(dev.wildercord.aura.arts.ThunderArts.HEAVENS_SPEAR)) {
			return 10.0F;
		}
		return scene.slot == AuraApi.ArtSlot.FOURTH ? 24.0F : 34.0F;
	}

	/** How long an art takes to have done everything its check looks at. */
	private static int settleTicks(Scene scene) {
		return switch (scene.id) {
			case "sunfall" -> 34;
			case "winters_hush" -> 36;
			case "hundred_winds" -> 66;
			case "heavens_spear" -> 24;
			case "mountain_splitter", "hailfall" -> 24;
			case "kindling_draw" -> 18;
			case "bolt_step", "static_riposte", "skyfall" -> 14;
			case "landslide" -> 14;
			default -> 8;
		};
	}

	/** The swordsman at the art's stage (all of it: the Final Art needs a full pool), a diamond sword, the husks where the scene puts them. */
	private static void set(TestSingleplayerContext world, Scene scene) {
		on(world, player -> {
			kill(player);
			stand(player);
			setAura(player, scene.method, AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			player.removeAttached(SwordStrings.COOLDOWNS);
			ServerLevel level = player.level();
			boolean step = scene.slot == AuraApi.ArtSlot.FOURTH;
			for (double[] f : scene.foes) {
				spawn(level, EntityTypes.HUSK, at(f[0], f[1]), 400);
			}
			if (scene.id.equals(RimeArts.SKATE)) {
				// The first husk in the way stands frozen, for the glide to shatter; and water lies under the way, for its frost.
				Mob first = foes(player).getFirst();
				dev.wildercord.cast.Spirits.freeze(first, 200);
				BlockPos water = waterSpot();
				for (BlockPos p : List.of(water.west(), water, water.east())) {
					// A shallow pool: stone under it so the water stays put.
					level.setBlockAndUpdate(p.below(), Blocks.STONE.defaultBlockState());
					level.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
				}
			}
			if (scene.id.equals(GaleArts.TAILWIND)) {
				Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
				if (wolf != null) {
					Vec3 p = at(2.5, 9.0);
					wolf.snapTo(p.x, p.y, p.z, 180, 0);
					wolf.tame(player);
					wolf.setOrderedToSit(true);
					wolf.addTag(TAG);
					level.addFreshEntity(wolf);
				}
			}
			return null;
		});
	}

	/**
	 * The husks step back to where the scene stood them after a swing threw them back (as a real foe presses in again), so the
	 * string's next swing still finds one. Two ticks after the swing, once its knockback has landed.
	 */
	private static void regroup(ClientGameTestContext context, TestSingleplayerContext world, Scene scene) {
		context.waitTicks(2);
		on(world, player -> {
			List<Mob> foes = foes(player);
			for (int i = 0; i < foes.size() && i < scene.foes.size(); i++) {
				Vec3 p = at(scene.foes.get(i)[0], scene.foes.get(i)[1]);
				Mob m = foes.get(i);
				m.teleportTo(p.x, p.y, p.z);
				m.setDeltaMovement(Vec3.ZERO);
			}
			return null;
		});
	}

	/** Where Skate's water lies: on the way, a little past where the step lands. */
	private static BlockPos waterSpot() {
		return new BlockPos(STAGE.getX(), STAGE.getY() - 1, STAGE.getZ() + 8);
	}

	/**
	 * An arrow loosed at the swordsman from in front, a few blocks off: Glacier Mirror's ice should send it back (it becomes theirs,
	 * flying away); the eye's wind should turn it aside, past them. Either way it mustn't hurt them.
	 */
	private static String arrowTurned(ServerPlayer player, boolean back) {
		ServerLevel level = player.level();
		Arrow arrow = new Arrow(EntityTypes.ARROW, level);
		// From in front and a little aside, clear of the husk that struck (frozen where it stands).
		Vec3 from = player.getEyePosition().add(1.6, -0.3, 2.6);
		Vec3 aim = player.getEyePosition().subtract(0, 0.3, 0).subtract(from).normalize();
		arrow.snapTo(from.x, from.y, from.z);
		arrow.setDeltaMovement(aim.scale(1.6));
		level.addFreshEntity(arrow);
		float health = player.getHealth();
		// Run the world a few ticks right here: the arrow flies, meets the ward, turns.
		for (int i = 0; i < 6 && !arrow.isRemoved(); i++) {
			arrow.tick();
		}
		if (player.getHealth() < health) {
			return "the arrow should never have hurt the swordsman";
		}
		Vec3 v = arrow.getDeltaMovement();
		if (v.lengthSqr() < 1.0E-4) {
			return "the arrow should still be flying, turned (" + v + ", " + (arrow.isRemoved() ? "gone" : "here") + ")";
		}
		double along = v.normalize().dot(aim);
		if (back) {
			return along < -0.3 ? null : "the mirror should have sent the arrow back the way it came (" + v + ")";
		}
		return along < 0.75 ? null : "the eye should have turned the arrow aside (" + v + ")";
	}

	// ------------------------------------------------------------------ the Aura page

	private static void page(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			return null;
		});
		context.waitTicks(5);
		context.runOnClient(mc -> {
			AuraScreen.listArts(true);
			AuraScreen.browse(null);
		});
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		shot(context, "art_page_own");
		// Another method's arts, chosen by its swatch as a player would.
		double[] chip = context.computeOnClient(mc -> ((AuraScreen) mc.gui.screen()).chipPoint("thunder"));
		check(chip != null, "the page should draw Thunder's swatch");
		double guiScale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(chip[0] * guiScale, chip[1] * guiScale);
		context.waitTicks(1);
		context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(3);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "art_page_thunder");
		// A method still to come: the common arts.
		context.runOnClient(mc -> AuraScreen.browse("verdant"));
		context.waitTicks(3);
		shot(context, "art_page_verdant");
		context.setScreen(() -> null);
		context.runOnClient(mc -> {
			AuraScreen.listArts(false);
			AuraScreen.browse(null);
		});
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

	/** A long stone platform in the sky (room for a step and a rush); the player in survival with an Echo Cord, facing down it (+z). */
	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 14) + " " + (y - 1) + " " + (z - 10) + " " + (x + 14) + " " + (y - 1) + " " + (z + 40) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 14) + " " + y + " " + (z - 10) + " " + (x + 14) + " " + (y + 10) + " " + (z + 40) + " minecraft:air");
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
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=arrow]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		// Skate's water and its frost go back to stone.
		BlockPos water = waterSpot();
		world.getServer().runCommand("fill " + (water.getX() - 1) + " " + water.getY() + " " + water.getZ() + " " + (water.getX() + 1) + " " + water.getY() + " "
			+ water.getZ() + " minecraft:stone");
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			player.removeAttached(AuraAttachments.STATE);
			player.removeAttached(SwordStrings.COOLDOWNS);
			player.setAttached(AuraPresence.TIMERS, AuraPresence.Timers.NONE);
			dev.wildercord.aura.arts.MethodArts.forget(player.getUUID());
		});
		// Long enough for any string to run out and the last blow to be forgotten.
		context.waitTicks(StringRules.ENGAGED_TICKS / 2);
	}

	private static Before snapshot(ServerPlayer player) {
		Map<Integer, Vec3> at = new HashMap<>();
		Map<Integer, Float> health = new HashMap<>();
		for (Mob m : foes(player)) {
			at.put(m.getId(), m.position());
			health.put(m.getId(), m.getHealth());
		}
		return new Before(at, health, player.position(), Aura.aura(player));
	}

	private static List<Integer> marks(AuraApi.StringArt art) {
		return art.string().tokens().stream().map(t -> SwordString.Token.marks(t)).toList();
	}

	private static void setAura(ServerPlayer player, String method, int stage, float aura) {
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(method, stage, AuraRules.threshold(stage), aura, 0));
		player.removeAttached(AuraAttachments.STATE);
	}

	private static Vec3 at(double side, double ahead) {
		return new Vec3(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead);
	}

	/**
	 * A husk that stands its ground: its legs taken (no speed) and its sight (no follow range, so it never comes for you), its AI
	 * left on, so a lift or a throw carries it as it would a real foe.
	 */
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
		return mob;
	}

	/** The scene's husks, in the order they were spawned. */
	private static List<Mob> foes(ServerPlayer player) {
		List<Mob> found = new ArrayList<>(player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48),
			m -> m.entityTags().contains(TAG) && m instanceof net.minecraft.world.entity.monster.zombie.Husk));
		found.sort(java.util.Comparator.comparingInt(Mob::getId));
		if (found.isEmpty()) {
			throw new AssertionError("the husks are gone");
		}
		return found;
	}

	private static void kill(ServerPlayer player) {
		for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64), e -> e.entityTags().contains(TAG))) {
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
