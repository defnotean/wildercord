package dev.wildercord.gametest;

import dev.wildercord.cast.Charging;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Runebound;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.Statuses;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The frost and wind runes' identity mechanics, cast for real at husks on a stone platform high in the air:
 * the shared seams (Silenced, Airborne, interrupts, the once-in-a-while guard) and each rune's own verb
 * (Chill stacking, Bubble, Hoarfrost, Absolute Zero counting the cold, Shatter ending a hold, Glacier calving,
 * Hail's pummel, Coldsnap's window, Tidecall, Undertow, Drowning Word, Cryostasis' burst, Frostward, Launch,
 * Summit Wind, Repel, Push's wall slam, Windcut's interrupt, Cushion's landing, Deflect's return, Prune as
 * shears, Icepath's strip, Zephyr, Feather Fall's glide, and Disarm giving a dead husk its sword back).
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordFrostWindTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.frost_wind";

	private interface Check {
		String run(ClientGameTestContext context, TestSingleplayerContext world);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule natural_regeneration false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			if ("1".equals(System.getenv("WILDERCORD_FROSTWIND_SHOTS"))) {
				shots(context, world);
				return;
			}
			List<String> failures = new ArrayList<>();
			List<Object[]> checks = List.of(
				new Object[] {"Silenced", (Check) WildercordFrostWindTest::silenced},
				new Object[] {"Interrupting a charge", (Check) WildercordFrostWindTest::interruptCharge},
				new Object[] {"Windcut breaks a Runebound's cast", (Check) WildercordFrostWindTest::windcutInterrupts},
				new Object[] {"Airborne", (Check) WildercordFrostWindTest::airborne},
				new Object[] {"Launch marks airborne", (Check) WildercordFrostWindTest::launch},
				new Object[] {"Chill stacks", (Check) WildercordFrostWindTest::chill},
				new Object[] {"Hoarfrost", (Check) WildercordFrostWindTest::hoarfrost},
				new Object[] {"Bubble", (Check) WildercordFrostWindTest::bubble},
				new Object[] {"Absolute Zero", (Check) WildercordFrostWindTest::absoluteZero},
				new Object[] {"Shatter ends a hold", (Check) WildercordFrostWindTest::shatterEndsHold},
				new Object[] {"Glacier", (Check) WildercordFrostWindTest::glacier},
				new Object[] {"Hail", (Check) WildercordFrostWindTest::hail},
				new Object[] {"Coldsnap", (Check) WildercordFrostWindTest::coldsnap},
				new Object[] {"Repel", (Check) WildercordFrostWindTest::repel},
				new Object[] {"Summit Wind", (Check) WildercordFrostWindTest::summitWind},
				new Object[] {"Tidecall", (Check) WildercordFrostWindTest::tidecall},
				new Object[] {"Undertow", (Check) WildercordFrostWindTest::undertow},
				new Object[] {"Drowning Word", (Check) WildercordFrostWindTest::drowningWord},
				new Object[] {"Frostward", (Check) WildercordFrostWindTest::frostward},
				new Object[] {"Cryostasis", (Check) WildercordFrostWindTest::cryostasis},
				new Object[] {"Push", (Check) WildercordFrostWindTest::push},
				new Object[] {"Cushion", (Check) WildercordFrostWindTest::cushion},
				new Object[] {"Deflect", (Check) WildercordFrostWindTest::deflect},
				new Object[] {"Prune", (Check) WildercordFrostWindTest::prune},
				new Object[] {"Icepath", (Check) WildercordFrostWindTest::icepath},
				new Object[] {"Zephyr", (Check) WildercordFrostWindTest::zephyr},
				new Object[] {"Feather Fall", (Check) WildercordFrostWindTest::featherFall},
				new Object[] {"Disarm", (Check) WildercordFrostWindTest::disarm});
			for (Object[] check : checks) {
				String failure;
				try {
					failure = ((Check) check[1]).run(context, world);
				} finally {
					cleanup(context, world);
				}
				if (failure != null) {
					failures.add(check[0] + ": " + failure);
				}
			}
			if (!failures.isEmpty()) {
				throw new AssertionError("The frost and wind runes went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	// ------------------------------------------------------------------ screenshots (WILDERCORD_FROSTWIND_SHOTS=1)

	/** One picture: what to cast (after an optional first cast), how many husks, and how long after the cast to look. */
	private record Shot(String name, RuneDef[] prep, int prepWait, RuneDef[] spell, int husks, int after) {}

	private static Shot shot(String name, RuneDef[] spell, int husks, int wait) {
		return new Shot(name, null, 0, spell, husks, wait);
	}

	private static RuneDef[] r(RuneDef... runes) {
		return runes;
	}

	private static void shots(ClientGameTestContext context, TestSingleplayerContext world) {
		// Films from a fixed point at the side, through an invisible marker, with the HUD hidden: the caster and the target both in frame.
		int cameraId = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			net.minecraft.world.entity.Display.TextDisplay camera = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.COMMAND);
			Vec3 eye = new Vec3(STAGE.getX() + 6.2, STAGE.getY() + 2.2, STAGE.getZ() + 1.0);
			Vec3 look = new Vec3(STAGE.getX() + 0.5, STAGE.getY() + 1.0, STAGE.getZ() + 3.5);
			Vec3 d = look.subtract(eye);
			camera.snapTo(eye.x, eye.y, eye.z, (float) Math.toDegrees(Math.atan2(-d.x, d.z)), (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
			camera.addTag("wildercord.camera");
			level.addFreshEntity(camera);
			return camera.getId();
		});
		context.waitTicks(3);
		context.runOnClient(mc -> {
			net.minecraft.world.entity.Entity camera = mc.level.getEntity(cameraId);
			if (camera != null) {
				mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
				mc.setCameraEntity(camera);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			}
		});
		List<Shot> all = List.of(
			shot("chill", r(Runes.BEAM, Runes.CHILL), 1, 3),
			shot("frost", r(Runes.BEAM, Runes.FROST), 1, 3),
			shot("icicle", r(Runes.BEAM, Runes.ICICLE), 1, 2),
			shot("coldsnap", r(Runes.BEAM, Runes.COLDSNAP), 3, 3),
			shot("hail", r(Runes.BEAM, Runes.HAIL), 1, 10),
			shot("freeze", r(Runes.BEAM, Runes.FREEZE), 1, 4),
			shot("glacier", r(Runes.BEAM, Runes.GLACIER), 3, 4),
			new Shot("absolute_zero", r(Runes.BEAM, Runes.CHILL), 8, r(Runes.BEAM, Runes.ABSOLUTE_ZERO), 1, 4),
			shot("hoarfrost", r(Runes.BEAM, Runes.HOARFROST), 1, 45),
			shot("bubble", r(Runes.BEAM, Runes.BUBBLE), 1, 14),
			shot("flash_freeze", r(Runes.BEAM, Runes.FLASH_FREEZE), 1, 3),
			shot("undertow", r(Runes.BEAM, Runes.UNDERTOW), 1, 4),
			shot("tidecall", r(Runes.BEAM, Runes.TIDECALL), 3, 8),
			shot("tidewrit", r(Runes.BEAM, Runes.TIDEWRIT), 1, 4),
			shot("tidehook", r(Runes.BEAM, Runes.TIDEHOOK), 1, 3),
			shot("drowning_word_silenced", r(Runes.BEAM, Runes.DROWNING_WORD), 1, 3),
			shot("blizzard", r(Runes.BEAM, Runes.BLIZZARD), 2, 30),
			shot("avalanche", r(Runes.BEAM, Runes.AVALANCHE), 2, 4),
			shot("black_ice", r(Runes.BEAM, Runes.BLACK_ICE), 1, 4),
			shot("cryostasis", r(Runes.SELF, Runes.CRYOSTASIS), 0, 8),
			shot("frostward", r(Runes.SELF, Runes.FROSTWARD), 0, 4),
			shot("push", r(Runes.BEAM, Runes.PUSH), 1, 2),
			shot("launch_airborne", r(Runes.BEAM, Runes.LAUNCH), 1, 6),
			shot("windcut", r(Runes.BEAM, Runes.WINDCUT), 1, 2),
			shot("repel", r(Runes.SELF, Runes.REPEL), 2, 3),
			shot("disarm", r(Runes.BEAM, Runes.DISARM), 1, 3),
			shot("cyclone", r(Runes.BEAM, Runes.CYCLONE), 2, 20),
			shot("updraft", r(Runes.BEAM, Runes.UPDRAFT), 1, 6),
			shot("summit_wind", r(Runes.BEAM, Runes.SUMMIT_WIND), 1, 6),
			shot("razorgale", r(Runes.BEAM, Runes.RAZORGALE), 2, 4),
			shot("dust_devil", r(Runes.BEAM, Runes.DUST_DEVIL), 1, 30),
			shot("dash", r(Runes.SELF, Runes.DASH), 0, 2),
			shot("swift", r(Runes.SELF, Runes.SWIFT), 0, 2),
			shot("leap", r(Runes.SELF, Runes.LEAP), 0, 6),
			shot("feather_fall", r(Runes.SELF, Runes.FEATHER_FALL), 0, 3),
			shot("cushion", r(Runes.SELF, Runes.CUSHION), 0, 3),
			shot("deflect", r(Runes.SELF, Runes.DEFLECT), 0, 3),
			shot("zephyr", r(Runes.SELF, Runes.ZEPHYR), 1, 4),
			shot("prune", r(Runes.SELF, Runes.PRUNE), 0, 2),
			shot("levitate", r(Runes.BEAM, Runes.LEVITATE), 1, 18),
			shot("recoil", r(Runes.BEAM, Runes.RECOIL), 1, 8));
		double[][] spots = {{0, 5}, {1.6, 5.5}, {-1.6, 5}, {0.5, 7}, {-2.4, 7}, {2.4, 7}, {0, 9}, {1.5, 9}};
		for (Shot s : all) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				stand(player);
				for (int i = 0; i < s.husks(); i++) {
					husk(player.level(), spots[i][0], spots[i][1], true);
				}
			});
			context.waitTicks(4);
			if (s.prep() != null) {
				world.getServer().runOnServer(server -> cast(player(server), s.prep()));
				context.waitTicks(s.prepWait());
			}
			world.getServer().runOnServer(server -> cast(player(server), s.spell()));
			context.waitTicks(s.after());
			context.runOnClient(mc -> mc.gui.toastManager().clear());
			context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("frostwind_" + s.name()).disableCounterPrefix());
			cleanup(context, world);
		}
	}

	// ------------------------------------------------------------------ the stage

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 10) + " " + (y - 1) + " " + (z - 10) + " " + (x + 10) + " " + (y - 1) + " " + (z + 10) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 10) + " " + (y - 2) + " " + (z - 10) + " " + (x + 10) + " " + (y - 2) + " " + (z + 10) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 10) + " " + y + " " + (z - 10) + " " + (x + 10) + " " + (y + 8) + " " + (z + 10) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book);
			player.setAttached(WildercordAttachments.CRYSTALS, Mana.MAX_CRYSTALS);
			stand(player);
		});
	}

	/** The caster at the middle, facing south (+Z), pitch level. */
	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.clearFire();
		player.getFoodData().setFoodLevel(20);
	}

	/** A husk {@code dz} in front and {@code dx} aside, {@code dy} up; a mind of its own if {@code ai}. */
	private static Mob husk(ServerLevel level, double dx, double dz, boolean ai) {
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		if (husk == null) {
			throw new IllegalStateException("couldn't make a husk");
		}
		husk.snapTo(STAGE.getX() + 0.5 + dx, STAGE.getY(), STAGE.getZ() + 0.5 + dz, 180, 0);
		husk.setNoAi(!ai);
		if (ai) {
			// With its wits (so holds and shoves work on it) but no legs of its own.
			husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);
		}
		husk.addTag(TAG);
		husk.addTag("wildercord.rolled");
		level.addFreshEntity(husk);
		return husk;
	}

	private static String cast(ServerPlayer player, RuneDef... runes) {
		List<String> ids = java.util.Arrays.stream(runes).map(RuneDef::id).toList();
		SpellCaster.edit(player, 0, List.of());
		SpellCaster.edit(player, 0, ids);
		if (!Spellbooks.get(player).spells().getFirst().equals(ids)) {
			return "the spell didn't thread";
		}
		Spellbooks.setReadyAt(player, 0, 0);
		Spellbooks.setMana(player, Mana.max(player));
		player.setAttached(WildercordAttachments.RHYTHM, WildercordAttachments.Rhythm.NONE);
		long now = player.level().getGameTime();
		SpellCaster.cast(player, 0);
		return Spellbooks.readyAt(player, 0) > now ? null : "it didn't cast";
	}

	private static Mob mob(MinecraftServer server, int id) {
		return player(server).level().getEntity(id) instanceof Mob mob ? mob : null;
	}

	private static float taken(LivingEntity e) {
		return e == null ? -1 : e.getMaxHealth() - e.getHealth();
	}

	private static boolean between(float value, double low, double high) {
		return value >= low && value <= high;
	}

	private static String f(double value) {
		return String.format("%.2f", value);
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=arrow]");
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 10) + " " + (y - 1) + " " + (z - 10) + " " + (x + 10) + " " + (y - 1) + " " + (z + 10) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 10) + " " + y + " " + (z - 10) + " " + (x + 10) + " " + (y + 8) + " " + (z + 10) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			stand(player);
		});
		context.waitTicks(20);
	}

	private static <T> T on(TestSingleplayerContext world, java.util.function.Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	// ------------------------------------------------------------------ the seams

	/** A silenced player's cast does nothing and a charge can't begin; after it ends they cast again. */
	private static String silenced(ClientGameTestContext context, TestSingleplayerContext world) {
		String during = on(world, player -> {
			Statuses.silence(player, 40);
			if (!Statuses.silenced(player)) {
				return "silence should be on";
			}
			if (cast(player, Runes.SELF, Runes.SWIFT) == null) {
				return "a silenced player shouldn't be able to cast";
			}
			Charging.request(player, -1, true);
			return player.hasAttached(WildercordAttachments.CHARGE) ? "a silenced player shouldn't begin a charge" : null;
		});
		if (during != null) {
			return during;
		}
		context.waitTicks(45);
		return on(world, player -> Statuses.silenced(player) ? "silence should have ended"
			: cast(player, Runes.SELF, Runes.SWIFT) == null ? null : "after the silence they should cast");
	}

	/** Interrupting a charge closes it; the same creature can't be interrupted again for 8 seconds. */
	private static String interruptCharge(ClientGameTestContext context, TestSingleplayerContext world) {
		return on(world, player -> {
			String threaded = cast(player, Runes.SELF, Runes.LEAP);
			if (threaded != null) {
				return threaded;
			}
			Spellbooks.setReadyAt(player, 0, 0);
			Charging.request(player, -1, true);
			if (!player.hasAttached(WildercordAttachments.CHARGE)) {
				return "the charge should have begun";
			}
			if (!Statuses.interrupt(player)) {
				return "a charging player should be interrupted";
			}
			if (player.hasAttached(WildercordAttachments.CHARGE)) {
				return "the charge should be closed";
			}
			Charging.request(player, -1, true);
			return Statuses.interrupt(player) ? "the same creature shouldn't be interrupted twice within 8 seconds" : null;
		});
	}

	/** A Runebound husk telegraphing a spell has it broken by a Windcut. */
	private static String windcutInterrupts(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		on(world, player -> {
			Mob husk = husk(player.level(), 0, 4, true);
			Runebound.bind(husk, List.of(Runes.BOLT, Runes.HARM), false);
			husk.setTarget(player);
			id[0] = husk.getId();
			return null;
		});
		boolean casting = false;
		for (int i = 0; i < 120 && !casting; i++) {
			context.waitTicks(2);
			casting = world.getServer().computeOnServer(server -> mob(server, id[0]) != null && Runebound.casting(mob(server, id[0])));
		}
		if (!casting) {
			return "the bound husk never began a cast (test setup)";
		}
		String cast = on(world, player -> {
			Mob husk = mob(player.level().getServer(), id[0]);
			husk.setPos(STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 4.5);
			return cast(player, Runes.BEAM, Runes.WINDCUT);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		return world.getServer().computeOnServer(server -> mob(server, id[0]) != null && Runebound.casting(mob(server, id[0]))
			? "a Windcut should break the spell a Runebound is telegraphing" : null);
	}

	/** A husk hanging in the air under the Airborne mark takes a fifth more from any spell; a marked one on the ground does not. */
	private static String airborne(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String plain = on(world, player -> {
			Mob husk = husk(player.level(), 0, 5, false);
			husk.setNoGravity(true);
			husk.snapTo(STAGE.getX() + 0.5, STAGE.getY() + 1.0, STAGE.getZ() + 5.5, 180, 0);
			id[0] = husk.getId();
			return cast(player, Runes.BEAM, Runes.HARM);
		});
		if (plain != null) {
			return plain;
		}
		context.waitTicks(3);
		float base = world.getServer().computeOnServer(server -> taken(mob(server, id[0])));
		cleanup(context, world);
		String marked = on(world, player -> {
			Mob husk = husk(player.level(), 0, 5, false);
			husk.setNoGravity(true);
			husk.snapTo(STAGE.getX() + 0.5, STAGE.getY() + 1.0, STAGE.getZ() + 5.5, 180, 0);
			Statuses.airborne(husk, 100);
			id[0] = husk.getId();
			return cast(player, Runes.BEAM, Runes.HARM);
		});
		if (marked != null) {
			return marked;
		}
		context.waitTicks(3);
		float bonus = world.getServer().computeOnServer(server -> taken(mob(server, id[0])));
		if (!between(base, 6.5, 7.5)) {
			return "plain Harm should deal 7 (took " + f(base) + ")";
		}
		if (!between(bonus, 8.0, 8.8)) {
			return "an airborne husk should take a fifth more: 8.4 (took " + f(bonus) + ")";
		}
		cleanup(context, world);
		String grounded = on(world, player -> {
			Mob husk = husk(player.level(), 0, 5, true);
			Statuses.airborne(husk, 100);
			id[0] = husk.getId();
			return null;
		});
		context.waitTicks(4);
		grounded = on(world, player -> cast(player, Runes.BEAM, Runes.HARM));
		if (grounded != null) {
			return grounded;
		}
		context.waitTicks(3);
		float onGround = world.getServer().computeOnServer(server -> taken(mob(server, id[0])));
		return between(onGround, 6.5, 7.5) ? null : "a marked husk standing on the ground gets no bonus (took " + f(onGround) + ")";
	}

	private static String launch(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 5, true).getId();
			return cast(player, Runes.BEAM, Runes.LAUNCH);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(6);
		return world.getServer().computeOnServer(server -> {
			Mob h = mob(server, id[0]);
			if (h == null) {
				return "the husk is gone";
			}
			if (!Reactions.has(h, Reactions.Mark.AIRBORNE)) {
				return "a launched husk should be marked airborne";
			}
			return h.getY() > STAGE.getY() + 0.5 ? null : "it should be flung up (at " + f(h.getY() - STAGE.getY()) + ")";
		});
	}

	// ------------------------------------------------------------------ frost

	/** Three Chills a second apart take a husk from Slowness II to III to IV, and no further. */
	private static String chill(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		on(world, player -> {
			id[0] = husk(player.level(), 0, 5, false).getId();
			return null;
		});
		int[] levels = new int[4];
		for (int i = 0; i < 4; i++) {
			String cast = on(world, player -> cast(player, Runes.BEAM, Runes.CHILL));
			if (cast != null) {
				return cast;
			}
			context.waitTicks(4);
			int at = i;
			levels[at] = world.getServer().computeOnServer(server -> {
				MobEffectInstance slow = mob(server, id[0]).getEffect(MobEffects.SLOWNESS);
				return slow == null ? -1 : slow.getAmplifier();
			});
			context.waitTicks(22);
		}
		return levels[0] == 1 && levels[1] == 2 && levels[2] == 3 && levels[3] == 3 ? null
			: "Chill should stack II, III, IV and stop there (saw " + java.util.Arrays.toString(levels) + ")";
	}

	/** Cast twice, Hoarfrost is one creep (6 damage, not 12), and it blooms onto a neighbour for 3. */
	private static String hoarfrost(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = new int[2];
		String cast = on(world, player -> {
			ids[0] = husk(player.level(), 0, 5, false).getId();
			ids[1] = husk(player.level(), 1.4, 5, false).getId();
			String first = cast(player, Runes.BEAM, Runes.HOARFROST);
			return first != null ? first : cast(player, Runes.BEAM, Runes.HOARFROST);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(80);
		return world.getServer().computeOnServer(server -> {
			Mob a = mob(server, ids[0]);
			Mob b = mob(server, ids[1]);
			if (a == null || b == null) {
				return "a husk is gone";
			}
			if (!between(taken(a), 5.5, 8.0)) {
				return "cast twice, one creep: 6 damage (took " + f(taken(a)) + ")";
			}
			if (!between(taken(b), 2.5, 4.5) || !b.hasEffect(MobEffects.SLOWNESS)) {
				return "the frost should bloom onto the husk beside it for 3 and a slow (took " + f(taken(b)) + ")";
			}
			return null;
		});
	}

	/** A bubble is one at a time (a second cast changes nothing) and pops for 4 once; it lifts the husk. */
	private static String bubble(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 5, true).getId();
			String first = cast(player, Runes.BEAM, Runes.BUBBLE);
			return first != null ? first : cast(player, Runes.BEAM, Runes.BUBBLE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(30);
		String held = world.getServer().computeOnServer(server -> {
			Mob h = mob(server, id[0]);
			if (h == null || !h.hasAttached(WildercordAttachments.FROZEN_UNTIL)) {
				return "a bubbled husk should be held";
			}
			return h.getY() > STAGE.getY() + 1.0 ? null : "the bubble should lift it (at " + f(h.getY() - STAGE.getY()) + ")";
		});
		if (held != null) {
			return held;
		}
		context.waitTicks(60);
		return world.getServer().computeOnServer(server -> {
			Mob h = mob(server, id[0]);
			if (h == null) {
				return "the husk is gone";
			}
			if (h.hasAttached(WildercordAttachments.FROZEN_UNTIL)) {
				return "a bubble on a husk (0.6 wide) holds 2.5 seconds, not longer";
			}
			return between(taken(h), 3.5, 5.5) && Reactions.has(h, Reactions.Mark.SOAKED) || Reactions.has(h, Reactions.Mark.SOAKED) && between(taken(h), 3.5, 5.5)
				? null : "one pop of 4 and a soak, however often it was cast (took " + f(taken(h)) + ")";
		});
	}

	/** Chill (slowed and brittle: two signs of cold) then Absolute Zero: 1 + 7. */
	private static String absoluteZero(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 5, true).getId();
			return cast(player, Runes.BEAM, Runes.CHILL);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(10);
		cast = on(world, player -> cast(player, Runes.BEAM, Runes.ABSOLUTE_ZERO));
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		return world.getServer().computeOnServer(server -> {
			Mob h = mob(server, id[0]);
			if (h == null) {
				return "the husk is gone";
			}
			return between(taken(h), 7.0, 9.5) && h.hasAttached(WildercordAttachments.FROZEN_UNTIL) ? null
				: "two signs of cold should be 7 and a hold on top of Chill's 1 (took " + f(taken(h)) + ")";
		});
	}

	/** Fire on a husk held by Freeze bursts the ice and lets it go. */
	private static String shatterEndsHold(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 5, true).getId();
			return cast(player, Runes.BEAM, Runes.FREEZE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		String frozen = world.getServer().computeOnServer(server -> mob(server, id[0]).hasAttached(WildercordAttachments.FROZEN_UNTIL) ? null : "Freeze should hold it");
		if (frozen != null) {
			return frozen;
		}
		cast = on(world, player -> cast(player, Runes.BEAM, Runes.FIRE));
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> {
			Mob h = mob(server, id[0]);
			return h != null && !h.hasAttached(WildercordAttachments.FROZEN_UNTIL) && !h.isNoAi() ? null : "Shatter should end the hold";
		});
	}

	/** Glacier holds the husk and, spreading, its neighbour; when it cracks each takes 2. */
	private static String glacier(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = new int[3];
		String cast = on(world, player -> {
			ids[0] = husk(player.level(), 0, 5, true).getId();
			ids[1] = husk(player.level(), 1.6, 5, true).getId();
			ids[2] = husk(player.level(), 7, 5, true).getId();
			return cast(player, Runes.BEAM, Runes.GLACIER);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		String held = world.getServer().computeOnServer(server -> {
			if (!mob(server, ids[0]).hasAttached(WildercordAttachments.FROZEN_UNTIL)) {
				return "the target should be held";
			}
			if (!mob(server, ids[1]).hasAttached(WildercordAttachments.FROZEN_UNTIL)) {
				return "the ice should spread to the husk 1.6 blocks off";
			}
			return mob(server, ids[2]).hasAttached(WildercordAttachments.FROZEN_UNTIL) ? "a husk 7 blocks off shouldn't be frozen" : null;
		});
		if (held != null) {
			return held;
		}
		context.waitTicks(60);
		return world.getServer().computeOnServer(server -> between(taken(mob(server, ids[0])), 1.5, 3.5) && between(taken(mob(server, ids[1])), 1.5, 3.5) ? null
			: "when the ice cracks each takes 2 (took " + f(taken(mob(server, ids[0]))) + " and " + f(taken(mob(server, ids[1]))) + ")");
	}

	/** Five stones of 2. */
	private static String hail(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 5, true).getId();
			return cast(player, Runes.BEAM, Runes.HAIL);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(35);
		return world.getServer().computeOnServer(server -> between(taken(mob(server, id[0])), 9.0, 11.5) ? null
			: "five stones of 2 should be 10 (took " + f(taken(mob(server, id[0]))) + ")");
	}

	/** Coldsnap: 4 damage, and everything struck is still brittle 3 seconds on (the window is 4). */
	private static String coldsnap(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 5, false).getId();
			return cast(player, Runes.BEAM, Runes.COLDSNAP);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(60);
		return world.getServer().computeOnServer(server -> {
			Mob h = mob(server, id[0]);
			if (!between(taken(h), 3.5, 4.6)) {
				return "Coldsnap should deal 4 (took " + f(taken(h)) + ")";
			}
			return Reactions.has(h, Reactions.Mark.FROZEN) ? null : "Coldsnap should leave it brittle for 4 seconds";
		});
	}

	private static String repel(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 2, false).getId();
			return cast(player, Runes.SELF, Runes.REPEL);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> between(taken(mob(server, id[0])), 3.5, 4.6) ? null
			: "Repel should deal 4 (took " + f(taken(mob(server, id[0]))) + ")");
	}

	private static String summitWind(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 5, true).getId();
			return cast(player, Runes.BEAM, Runes.SUMMIT_WIND);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> {
			Mob h = mob(server, id[0]);
			if (h == null || !h.hasEffect(MobEffects.SLOWNESS) && !h.hasEffect(MobEffects.SLOW_FALLING)) {
				return "the wind should hold the husk aloft (slow falling)";
			}
			return h.hasEffect(MobEffects.SLOW_FALLING) ? null : "an exiled enemy should be given slow falling";
		});
	}

	/** Tidecall: hurt, soaked and pulled (Collapse can find it), for about 6 and no more than 7. */
	private static String tidecall(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 5, false).getId();
			return cast(player, Runes.BEAM, Runes.TIDECALL);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(12);
		return world.getServer().computeOnServer(server -> {
			Mob h = mob(server, id[0]);
			if (!Reactions.has(h, Reactions.Mark.SOAKED)) {
				return "the tide should leave it soaked";
			}
			if (!between(taken(h), 5.5, 7.5)) {
				return "a lone husk takes 6 (took " + f(taken(h)) + ")";
			}
			return Reactions.has(h, Reactions.Mark.PULLED) ? null : "the tide should leave it pulled (so Repel's Collapse works)";
		});
	}

	/** With no water within 6 blocks Undertow leaves 3 damage, a slow and a soak. */
	private static String undertow(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 5, false).getId();
			return cast(player, Runes.BEAM, Runes.UNDERTOW);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		return world.getServer().computeOnServer(server -> {
			Mob h = mob(server, id[0]);
			MobEffectInstance slow = h.getEffect(MobEffects.SLOWNESS);
			if (slow == null || slow.getAmplifier() != 2 || !Reactions.has(h, Reactions.Mark.SOAKED)) {
				return "it should be slowed (III) and soaked";
			}
			return between(taken(h), 2.5, 3.6) ? null : "with no water near the ground turns to slurry for 3 (took " + f(taken(h)) + ")";
		});
	}

	private static String drowningWord(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 5, false).getId();
			return cast(player, Runes.BEAM, Runes.DROWNING_WORD);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> Statuses.silenced(mob(server, id[0])) ? null : "lungs full of water it can't cast: it should be silenced");
	}

	/** A frost hold on a warded creature lasts a second at most. */
	private static String frostward(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = on(world, player -> cast(player, Runes.SELF, Runes.FROSTWARD));
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		return on(world, player -> {
			Spirits.freeze(player, 100);
			MobEffectInstance slow = player.getEffect(MobEffects.SLOWNESS);
			return slow != null && slow.getDuration() <= 22 ? null
				: "a frost hold on a Frostward lasts a second at most (" + (slow == null ? "none" : slow.getDuration() + " ticks") + ")";
		});
	}

	/** Cryostasis on yourself: when the ice opens, a husk 2 blocks off takes 3 and is left brittle. */
	private static String cryostasis(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 2, false).getId();
			return cast(player, Runes.SELF, Runes.CRYOSTASIS);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(55);
		return world.getServer().computeOnServer(server -> {
			Mob h = mob(server, id[0]);
			return between(taken(h), 2.5, 4.0) ? null : "the ice should burst for 3 when it opens (took " + f(taken(h)) + ")";
		});
	}

	// ------------------------------------------------------------------ wind

	/** A husk pushed into a wall takes 2 from the slam (Push itself does nothing). */
	private static String push(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		world.getServer().runCommand("fill " + (STAGE.getX() - 3) + " " + STAGE.getY() + " " + (STAGE.getZ() + 7) + " " + (STAGE.getX() + 3) + " " + (STAGE.getY() + 3) + " "
			+ (STAGE.getZ() + 7) + " minecraft:stone");
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 3, true).getId();
			return cast(player, Runes.BEAM, Runes.PUSH);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(25);
		return world.getServer().computeOnServer(server -> {
			Mob h = mob(server, id[0]);
			return between(taken(h), 1.5, 3.5) ? null : "a husk thrown into a wall should take 2 (took " + f(taken(h)) + ")";
		});
	}

	/** Fall from 14 blocks with a Cushion: no damage, and a husk beside the landing takes the fall (6 at most). */
	private static String cushion(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 1.5, 0, false).getId();
			String c = cast(player, Runes.SELF, Runes.CUSHION);
			player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY() + 14, STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
			return c;
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(60);
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			if (player.getHealth() < player.getMaxHealth()) {
				return "the Cushion should stop the fall's damage";
			}
			Mob h = mob(server, id[0]);
			return between(taken(h), 3.5, 6.6) ? null : "the landing should hurt the husk for about 6 (took " + f(taken(h)) + ")";
		});
	}

	/** An arrow shot at a warded player by a husk is sent back and now the player's. */
	private static String deflect(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			id[0] = husk(player.level(), 0, 8, false).getId();
			return cast(player, Runes.SELF, Runes.DEFLECT);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		on(world, player -> {
			ServerLevel level = player.level();
			Projectile arrow = (Projectile) EntityTypes.ARROW.create(level, EntitySpawnReason.COMMAND);
			Mob husk = mob(player.level().getServer(), id[0]);
			arrow.setOwner(husk);
			arrow.snapTo(STAGE.getX() + 0.5, STAGE.getY() + 1.4, STAGE.getZ() + 3.0, 0, 0);
			arrow.setDeltaMovement(0, 0, -0.9);
			arrow.addTag(TAG);
			level.addFreshEntity(arrow);
			return null;
		});
		context.waitTicks(8);
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			for (Projectile p : player.level().getEntitiesOfClass(Projectile.class, new AABB(STAGE).inflate(12), e -> e.entityTags().contains(TAG))) {
				if (p.entityTags().contains("wildercord.deflected") && p.getOwner() == player && p.getDeltaMovement().z > 0) {
					return null;
				}
			}
			return "the arrow should be sent back toward its shooter and belong to the ward";
		});
	}

	/** Prune drops leaves and string as shears would. */
	private static String prune(ClientGameTestContext context, TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("setblock " + (x + 1) + " " + y + " " + z + " minecraft:cobweb");
		world.getServer().runCommand("setblock " + (x - 1) + " " + y + " " + z + " minecraft:oak_leaves[persistent=false]");
		String cast = on(world, player -> cast(player, Runes.SELF, Runes.PRUNE));
		if (cast != null) {
			return cast;
		}
		context.waitTicks(5);
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			boolean string = false;
			boolean leaves = false;
			for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6))) {
				string |= item.getItem().is(Items.COBWEB);
				leaves |= item.getItem().is(Items.OAK_LEAVES);
			}
			return string && leaves ? null : "the web and the leaves should drop as themselves, as shears do (web " + string + ", leaves " + leaves + ")";
		});
	}

	/** On Self, Icepath lays a strip ahead of you over water. */
	private static String icepath(ClientGameTestContext context, TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 2) + " " + (y - 1) + " " + (z + 1) + " " + (x + 2) + " " + (y - 1) + " " + (z + 10) + " minecraft:water");
		context.waitTicks(5);
		String cast = on(world, player -> cast(player, Runes.SELF, Runes.ICEPATH));
		if (cast != null) {
			return cast;
		}
		context.waitTicks(20);
		return world.getServer().computeOnServer(server -> {
			int ice = 0;
			ServerLevel level = player(server).level();
			for (int dz = 1; dz <= 10; dz++) {
				for (int dx = -2; dx <= 2; dx++) {
					if (level.getBlockState(new BlockPos(x + dx, y - 1, z + dz)).is(Blocks.FROSTED_ICE)) {
						ice++;
					}
				}
			}
			return ice >= 15 ? null : "a strip of ice should run ahead over the water (" + ice + " blocks frozen)";
		});
	}

	private static String zephyr(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = on(world, player -> {
			player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400, 0));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 400, 1));
			return cast(player, Runes.SELF, Runes.ZEPHYR);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		return on(world, player -> {
			if (player.hasEffect(MobEffects.BLINDNESS) || player.hasEffect(MobEffects.SLOWNESS)) {
				return "the breeze should blow away blindness and slowness";
			}
			MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
			return speed != null && speed.getDuration() > 170 ? null : "Speed should last 10 seconds";
		});
	}

	/** Falling under Feather Fall, you drift the way you look. */
	private static String featherFall(ClientGameTestContext context, TestSingleplayerContext world) {
		double[] start = new double[1];
		String cast = on(world, player -> {
			player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY() + 30, STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
			start[0] = player.getZ();
			return cast(player, Runes.SELF, Runes.FEATHER_FALL);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(40);
		return on(world, player -> player.getZ() - start[0] >= 1.5 ? null
			: "under Feather Fall you should drift the way you look (moved " + f(player.getZ() - start[0]) + ")");
	}

	/** A husk killed while disarmed drops its sword. */
	private static String disarm(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = on(world, player -> {
			Mob husk = husk(player.level(), 0, 5, false);
			husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
			husk.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
			id[0] = husk.getId();
			return cast(player, Runes.BEAM, Runes.DISARM);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		String killed = world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			if (husk == null || !husk.getMainHandItem().isEmpty()) {
				return "the sword should be snatched first";
			}
			husk.hurtServer(player(server).level(), player(server).level().damageSources().playerAttack(player(server)), 1000);
			return null;
		});
		if (killed != null) {
			return killed;
		}
		context.waitTicks(5);
		return world.getServer().computeOnServer(server -> {
			for (ItemEntity item : player(server).level().getEntitiesOfClass(ItemEntity.class, new AABB(STAGE).inflate(12))) {
				if (item.getItem().is(Items.DIAMOND_SWORD)) {
					return null;
				}
			}
			return "a husk killed while disarmed should drop its sword";
		});
	}
}
