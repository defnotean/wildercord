package dev.wildercord.gametest;

import dev.wildercord.cast.SpellCaster;
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
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Films the fire and blood runes after the presentation pass, so the displays can be looked at (and the noisy ones cut): each
 * cast at husks (or a tamed wolf, or the caster) on a cleared stage and screenshotted at the moments that matter: the wind-up,
 * the impact, the aftermath. Screenshots land in build/run/clientGameTest/screenshots/firebl_*.
 *
 * <p>Runs only with {@code WILDERCORD_FIREBLOOD_SHOTS=1}, and checks nothing: a cast that goes wrong is logged and skipped.
 * {@code WILDERCORD_FIREBLOOD_ONLY=ember,meteor} films only those.</p>
 */
public class WildercordFireBloodShots implements FabricClientGameTest {
	private static final Logger LOG = LoggerFactory.getLogger("Wildercord fire and blood shots");
	private static final String TAG = "wildercord.fireblood.shots";

	private static Vec3 stage;
	private static Vec3 lane;

	/** One cast: its name, its runes, the ticks after the cast to shoot at, and how it is staged. */
	private record Shot(String name, List<RuneDef> runes, int[] ticks, Stage stage) {}

	private enum Stage { FOES, CLOSE, SELF, ALLY, PROVOKED }

	private static Shot foes(String name, RuneDef shape, RuneDef effect, int... ticks) {
		return new Shot(name, List.of(shape, effect), ticks, Stage.FOES);
	}

	/** One creature, filmed close: for the displays that live on a body. */
	private static Shot close(String name, RuneDef shape, RuneDef effect, int... ticks) {
		return new Shot(name, List.of(shape, effect), ticks, Stage.CLOSE);
	}

	private static Shot self(String name, RuneDef effect, int... ticks) {
		return new Shot(name, List.of(Runes.SELF, effect), ticks, Stage.SELF);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!"1".equals(System.getenv("WILDERCORD_FIREBLOOD_SHOTS"))) {
			return;
		}
		String only = System.getenv("WILDERCORD_FIREBLOOD_ONLY");
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
			context.waitTicks(60);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1600, 900);
				mc.options.guiScale().set(2);
				mc.resizeGui();
			});
			world.getServer().runCommand("time set 13000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> server.setDifficulty(Difficulty.NORMAL, true));
			setup(world);
			context.waitTicks(20);
			for (Shot shot : shots()) {
				if (only != null && !only.isBlank() && java.util.Arrays.stream(only.split(",")).noneMatch(s -> shot.name().equals(s.trim()))) {
					continue;
				}
				try {
					film(context, world, shot);
				} catch (Exception | AssertionError e) {
					LOG.warn("Shot '{}' went wrong, moving on", shot.name(), e);
				}
			}
		}
	}

	private static List<Shot> shots() {
		List<Shot> all = new ArrayList<>();
		// Fire.
		all.add(close("ember", Runes.BEAM, Runes.EMBER, 3));
		all.add(close("fire", Runes.BEAM, Runes.FIRE, 3, 50));
		all.add(foes("flashfire", Runes.BEAM, Runes.FLASHFIRE, 2, 6));
		all.add(foes("explode", Runes.BEAM, Runes.EXPLODE, 2, 12));
		all.add(foes("meteor", Runes.BEAM, Runes.METEOR, 6, 14, 26, 34));
		all.add(foes("inferno", Runes.BEAM, Runes.INFERNO, 3, 25, 80));
		all.add(foes("primer", Runes.BEAM, Runes.PRIMER, 4, 24, 39, 43));
		all.add(new Shot("kindling", List.of(Runes.BEAM, Runes.KINDLING), new int[] {3, 8, 13, 18, 23, 26}, Stage.PROVOKED));
		all.add(foes("firestorm", Runes.BEAM, Runes.FIRESTORM, 3, 30));
		all.add(foes("steam", Runes.BEAM, Runes.STEAM, 3, 20));
		all.add(close("sunscorch", Runes.BEAM, Runes.SUNSCORCH, 2));
		all.add(close("soulfire", Runes.BEAM, Runes.SOULFIRE, 3, 30));
		all.add(foes("blazecall", Runes.BEAM, Runes.BLAZECALL, 2, 10));
		all.add(close("cinderbrand", Runes.BEAM, Runes.CINDERBRAND, 3, 45));
		all.add(self("ashen_veil", Runes.ASHEN_VEIL, 4));
		all.add(self("cinderheart", Runes.CINDERHEART, 4, 25));
		all.add(self("searing_edge", Runes.SEARING_EDGE, 4));
		all.add(self("fireward", Runes.FIREWARD, 4));
		all.add(foes("hellmouth", Runes.BEAM, Runes.HELLMOUTH, 10));
		all.add(foes("starfire", Runes.BEAM, Runes.STARFIRE, 10));
		all.add(close("everburn", Runes.BEAM, Runes.EVERBURN, 6));
		all.add(foes("conflagration", Runes.BEAM, Runes.CONFLAGRATION, 12));
		all.add(self("phoenix_pyre", Runes.PHOENIX_PYRE, 10));
		all.add(close("bloodboil", Runes.BEAM, Runes.BLOODBOIL, 6));
		// Blood.
		all.add(close("rend", Runes.BEAM, Runes.REND, 3, 25));
		all.add(close("leech", Runes.BEAM, Runes.LEECH, 3));
		all.add(close("bleed", Runes.BEAM, Runes.BLEED, 3, 25));
		all.add(close("gash", Runes.BEAM, Runes.GASH, 3, 45));
		all.add(close("dismantle", Runes.BEAM, Runes.DISMANTLE, 1, 5));
		all.add(close("cleave", Runes.BEAM, Runes.CLEAVE, 2));
		all.add(self("overdrive", Runes.OVERDRIVE, 4, 45));
		all.add(self("warcry", Runes.WARCRY, 3, 9));
		all.add(close("blood_moss", Runes.BEAM, Runes.BLOOD_MOSS, 3, 30));
		all.add(close("parasite", Runes.BEAM, Runes.PARASITE, 4));
		all.add(close("lifesteal", Runes.BEAM, Runes.LIFESTEAL, 3));
		all.add(foes("crimson_mist", Runes.BEAM, Runes.CRIMSON_MIST, 6, 40));
		all.add(close("heartstopper", Runes.BEAM, Runes.HEARTSTOPPER, 3, 30));
		all.add(close("sanguine_rite", Runes.BEAM, Runes.SANGUINE_RITE, 3, 8));
		all.add(close("hemomancy", Runes.BEAM, Runes.HEMOMANCY, 3));
		all.add(new Shot("transfusion", List.of(Runes.BEAM, Runes.TRANSFUSION), new int[] {4, 10}, Stage.ALLY));
		all.add(foes("blood_thread", Runes.BEAM, Runes.BLOOD_THREAD, 3, 22));
		all.add(foes("seethe", Runes.BEAM, Runes.SEETHE, 4, 30));
		all.add(foes("skyburst", Runes.BEAM, Runes.SKYBURST, 8, 24));
		return all;
	}

	private void film(ClientGameTestContext context, TestSingleplayerContext world, Shot shot) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			place(player, lane, 0, 6);
			switch (shot.stage()) {
				case FOES, CLOSE, PROVOKED -> {
					standing(husk(level, lane.add(0, 0, 6), 180));
					standing(husk(level, lane.add(-1.8, 0, 6.6), 180));
					standing(husk(level, lane.add(1.8, 0, 7.2), 180));
				}
				case ALLY -> {
					wolf(player, lane.add(0, 0, 6), 180);
					player.setHealth(6.0F);
				}
				case SELF -> { }
			}
		});
		if (shot.stage() == Stage.SELF) {
			director(context, world, lane.add(-3.6, 2.2, 3.4), lane.add(0, 0.8, 0.4));
		} else if (shot.stage() == Stage.CLOSE) {
			director(context, world, lane.add(-2.6, 1.9, 3.2), lane.add(0, 1.1, 6.0));
		} else {
			director(context, world, lane.add(-7.5, 3.4, 4.2), lane.add(0, 1.0, 5.2));
		}
		context.waitTicks(3);
		int at = 0;
		int repeats = shot.name().equals("kindling") ? 5 : 1;
		for (int i = 0; i < repeats; i++) {
			world.getServer().runOnServer(server -> castNow(player(server), shot.runes()));
			if (repeats > 1) {
				context.waitTicks(4);
				at += 4;
			}
		}
		for (int tick : shot.ticks()) {
			int wait = tick - at;
			if (wait > 0) {
				context.waitTicks(wait);
				at = tick;
			}
			context.runOnClient(mc -> mc.gui.toastManager().clear());
			context.takeScreenshot(TestScreenshotOptions.of("firebl_" + shot.name() + "_t" + tick).disableCounterPrefix());
		}
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
		});
		context.waitTicks(20);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.removeAllEffects();
			player.clearFire();
			player.setHealth(player.getMaxHealth());
		});
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=evoker_fangs]");
		context.waitTicks(30);
	}

	// ------------------------------------------------------------------ staging

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void place(ServerPlayer player, Vec3 at, float yaw, float pitch) {
		player.teleportTo(player.level(), at.x, at.y, at.z, Set.<Relative>of(), yaw, pitch, false);
		player.setDeltaMovement(Vec3.ZERO);
	}

	private static Vec3 ground(ServerLevel level, double x, double z) {
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
		return new Vec3(Math.floor(x) + 0.5, y, Math.floor(z) + 0.5);
	}

	private static void setup(TestSingleplayerContext world) {
		int[] where = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Vec3 at = ground(player.level(), player.getX(), player.getZ());
			return new int[] {(int) Math.floor(at.x), (int) at.y - 1, (int) Math.floor(at.z)};
		});
		int cx = where[0];
		int y = where[1];
		int cz = where[2];
		for (int x0 = cx - 32; x0 < cx + 32; x0 += 16) {
			for (int z0 = cz - 32; z0 < cz + 32; z0 += 16) {
				world.getServer().runCommand("fill " + x0 + " " + (y + 1) + " " + z0 + " " + (x0 + 15) + " " + (y + 40) + " " + (z0 + 15) + " air");
				world.getServer().runCommand("fill " + x0 + " " + y + " " + z0 + " " + (x0 + 15) + " " + y + " " + (z0 + 15) + " grass_block");
				world.getServer().runCommand("fill " + x0 + " " + (y - 3) + " " + z0 + " " + (x0 + 15) + " " + (y - 1) + " " + (z0 + 15) + " dirt");
			}
		}
		world.getServer().runCommand("kill @e[type=!player,type=!text_display]");
		stage = new Vec3(cx + 0.5, y + 1, cz + 0.5);
		lane = stage.add(0, 0, -8);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.CREATIVE);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book.withSelected(0));
			player.setAttached(WildercordAttachments.CIRCLES, 6);
			player.setAttached(WildercordAttachments.CRYSTALS, Mana.MAX_CRYSTALS);
			player.setExperienceLevels(100);
			Spellbooks.setMana(player, Mana.max(player));
			place(player, lane, 0, 6);
		});
	}

	private static void castNow(ServerPlayer player, List<RuneDef> runes) {
		SpellCaster.edit(player, 0, List.of());
		SpellCaster.edit(player, 0, runes.stream().map(RuneDef::id).toList());
		Spellbooks.setMana(player, Mana.max(player));
		Spellbooks.setReadyAt(player, 0, 0);
		player.removeAttached(WildercordAttachments.RHYTHM);
		SpellCaster.cast(player, 0);
	}

	private static void director(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 target) {
		int id = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			level.getEntitiesOfClass(Display.TextDisplay.class, player(server).getBoundingBox().inflate(160), e -> e.entityTags().contains("wildercord.camera"))
				.forEach(Entity::discard);
			Display.TextDisplay camera = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.COMMAND);
			Vec3 d = target.subtract(eye);
			float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
			float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
			camera.snapTo(eye.x, eye.y, eye.z, yaw, pitch);
			camera.addTag("wildercord.camera");
			level.addFreshEntity(camera);
			return camera.getId();
		});
		context.waitTicks(3);
		context.runOnClient(mc -> {
			Entity camera = mc.level.getEntity(id);
			if (camera != null) {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.setCameraEntity(camera);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			}
		});
	}

	private static Mob husk(ServerLevel level, Vec3 at, float yaw) {
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		if (husk == null) {
			return null;
		}
		husk.snapTo(at.x, at.y, at.z, yaw, 0);
		husk.setNoAi(true);
		husk.addTag(TAG);
		husk.addTag("wildercord.rolled");
		level.addFreshEntity(husk);
		return husk;
	}

	/** A husk that can still be moved (throws and pulls show) but can't walk. */
	private static void standing(Mob husk) {
		if (husk != null) {
			husk.setNoAi(false);
			husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);
		}
	}

	private static void wolf(ServerPlayer owner, Vec3 at, float yaw) {
		Wolf wolf = EntityTypes.WOLF.create(owner.level(), EntitySpawnReason.COMMAND);
		if (wolf == null) {
			return;
		}
		wolf.snapTo(at.x, at.y, at.z, yaw, 0);
		wolf.setNoAi(true);
		wolf.tame(owner);
		wolf.addTag(TAG);
		owner.level().addFreshEntity(wolf);
		wolf.setHealth(wolf.getMaxHealth() / 2);
	}
}
