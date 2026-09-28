package dev.wildercord.gametest;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.Charging;
import dev.wildercord.cast.RuneBolt;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.WildSurge;
import dev.wildercord.cast.events.EventContent;
import dev.wildercord.cast.events.FallenStars;
import dev.wildercord.cast.events.ManaStorm;
import dev.wildercord.cast.events.RiftSiege;
import dev.wildercord.cast.events.WorldEvents;
import dev.wildercord.chorus.Chorus;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.FusionAltarScreen;
import dev.wildercord.client.cosmetic.CordStyleScreen;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.cosmetic.CordCosmetics;
import dev.wildercord.cosmetic.CordStyles;
import dev.wildercord.familiar.FamiliarContent;
import dev.wildercord.familiar.Familiars;
import dev.wildercord.familiar.Wisp;
import dev.wildercord.familiar.WispRules;
import dev.wildercord.familiar.WispSpawner;
import dev.wildercord.gear.GearDef;
import dev.wildercord.gear.GearItems;
import dev.wildercord.gear.SpellSlots;
import dev.wildercord.menu.FusionAltarMenu;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.runesmith.Runesmith;
import dev.wildercord.spell.Knots;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.WildMagic;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A showcase of the newest features, for a designer to judge how they look: the Fusion Altar,
 * parrying and wild magic, magic that changes the world, the world events, familiars and Cord
 * cosmetics, the Runesmith and chorus casting, casting gear, the runes of the world, the
 * advancement tab and the new screens. It checks nothing: every section is filmed on a cleared
 * stage and screenshotted into build/run/clientGameTest/screenshots/showcase_*, and a section that
 * goes wrong is logged and skipped, so one problem never loses the rest of the shots.
 *
 * <p>Runs only with {@code WILDERCORD_SHOWCASE=1} (which makes every other test skip itself).</p>
 */
public class WildercordShowcase implements FabricClientGameTest {
	private static final Logger LOG = LoggerFactory.getLogger("Wildercord showcase");
	private static final String TAG = "wildercord.showcase";

	/** The middle of a cleared, flat 64x64 stage (grass, open sky), made in setup. */
	private static Vec3 stage;
	/** Where the caster stands to cast down the stage (+z) at husks. */
	private static Vec3 lane;
	/** The Fusion Altar, behind the lane. */
	private static BlockPos altar;

	private static final List<String> TAKEN = new ArrayList<>();
	private static final List<String> SKIPPED = new ArrayList<>();

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!"1".equals(System.getenv("WILDERCORD_SHOWCASE"))) {
			return;
		}
		TAKEN.clear();
		SKIPPED.clear();
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
			context.waitTicks(60);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1920, 1080);
				mc.options.guiScale().set(2);
				mc.resizeGui();
			});
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> server.setDifficulty(Difficulty.NORMAL, true));
			setup(world);
			context.waitTicks(20);

			section(context, world, "fusion altar", () -> fusionAltar(context, world));
			section(context, world, "fused spells", () -> fusedSpells(context, world));
			section(context, world, "parry", () -> parry(context, world));
			section(context, world, "wild magic", () -> wildMagic(context, world));
			section(context, world, "world magic", () -> worldMagic(context, world));
			section(context, world, "mana storm", () -> manaStorm(context, world));
			section(context, world, "fallen star", () -> fallenStar(context, world));
			section(context, world, "rift siege", () -> rift(context, world));
			section(context, world, "familiars", () -> familiars(context, world));
			section(context, world, "cosmetics", () -> cosmetics(context, world));
			section(context, world, "runesmith", () -> runesmith(context, world));
			section(context, world, "chorus", () -> chorus(context, world));
			section(context, world, "gear", () -> gear(context, world));
			section(context, world, "new runes", () -> newRunes(context, world));
			section(context, world, "attunement", () -> attunement(context, world));
			section(context, world, "advancements", () -> advancements(context, world));
			section(context, world, "screens", () -> screens(context, world));
			section(context, world, "creative tab", () -> creativeTab(context, world));
			LOG.info("Showcase done: {} screenshots{}", TAKEN.size(), SKIPPED.isEmpty() ? "" : ", skipped: " + String.join("; ", SKIPPED));
		}
	}

	// ------------------------------------------------------------------ helpers

	/** Runs one section; anything it throws is logged and the showcase moves on to the next. */
	private static void section(ClientGameTestContext context, TestSingleplayerContext world, String name, Runnable body) {
		try {
			body.run();
		} catch (Exception | AssertionError | LinkageError e) {
			LOG.warn("Showcase section '{}' went wrong, moving on", name, e);
			SKIPPED.add(name + " (" + e + ")");
		}
		try {
			reset(context, world);
		} catch (Exception | AssertionError e) {
			LOG.warn("Couldn't tidy up after showcase section '{}'", name, e);
		}
	}

	/** One shot within a section: logged and skipped on its own if it goes wrong. */
	private static void attempt(String name, Runnable body) {
		try {
			body.run();
		} catch (Exception | AssertionError | LinkageError e) {
			LOG.warn("Showcase shot '{}' went wrong, moving on", name, e);
			SKIPPED.add(name + " (" + e + ")");
		}
	}

	/** Back to a clean stage: no screen, the player's own camera, creative, empty hands, nothing of ours standing. */
	private static void reset(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> {
			if (mc.gui.screen() != null) {
				if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) {
					mc.player.closeContainer();
				}
				mc.gui.setScreen(null);
			}
			if (mc.player != null) {
				mc.setCameraEntity(mc.player);
			}
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		clean(world);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.CREATIVE);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			player.removeAllEffects();
			player.clearFire();
			player.setTicksFrozen(0);
			player.setHealth(player.getMaxHealth());
			player.setShiftKeyDown(false);
			player.removeAttached(WildercordAttachments.SPELL_SHIELD);
			player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
			WildSurge.force(player, null);
			Spellbooks.setMana(player, Mana.max(player));
		});
		world.getServer().runCommand("time set 6000");
		world.getServer().runCommand("weather clear");
		context.waitTicks(5);
	}

	private static void clean(TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runCommand("kill @e[type=arrow]");
		world.getServer().runCommand("kill @e[type=evoker_fangs]");
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static List<String> ids(RuneDef... runes) {
		return java.util.Arrays.stream(runes).map(RuneDef::id).toList();
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.takeScreenshot(TestScreenshotOptions.of("showcase_" + name).disableCounterPrefix());
		TAKEN.add("showcase_" + name);
	}

	private static void camera(ClientGameTestContext context, CameraType type) {
		context.runOnClient(mc -> mc.options.setCameraType(type));
	}

	/** Stands the player at a point, looking a given way. */
	private static void place(ServerPlayer player, Vec3 at, float yaw, float pitch) {
		player.teleportTo(player.level(), at.x, at.y, at.z, Set.<Relative>of(), yaw, pitch, false);
		player.setDeltaMovement(Vec3.ZERO);
	}

	private static Vec3 ground(ServerLevel level, double x, double z) {
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
		return new Vec3(Math.floor(x) + 0.5, y, Math.floor(z) + 0.5);
	}

	/** The block of air at stage level {@code dx}, {@code dz} from the stage's middle. */
	private static BlockPos at(int dx, int dz) {
		return BlockPos.containing(stage.x + dx, stage.y, stage.z + dz);
	}

	/** Films from a fixed point: the client looks through an invisible marker placed there, with the HUD hidden. */
	private static void director(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 target) {
		int id = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, player(server).getBoundingBox().inflate(160),
				e -> e.entityTags().contains("wildercord.camera")).forEach(Entity::discard);
			net.minecraft.world.entity.Display.TextDisplay camera = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.COMMAND);
			Vec3 d = target.subtract(eye);
			float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
			float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
			camera.snapTo(eye.x, eye.y, eye.z, yaw, pitch);
			// Not tagged with TAG: clearing the stage between shots mustn't take the camera away mid-scene.
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

	/** Back to the player's own eyes (first person, HUD on). */
	private static void cut(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
	}

	/** A still husk (never rolled Runebound), tagged for clearing. */
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

	/** Threads {@code runes} into spell {@code spell} and casts it at once, with full mana and no cooldown. */
	private static void castNow(ServerPlayer player, int spell, List<RuneDef> runes) {
		SpellCaster.edit(player, spell, List.of());
		SpellCaster.edit(player, spell, runes.stream().map(RuneDef::id).toList());
		Spellbooks.setMana(player, Mana.max(player));
		Spellbooks.setReadyAt(player, spell, 0);
		player.removeAttached(WildercordAttachments.RHYTHM);
		SpellCaster.cast(player, spell);
	}

	/** Lands a spell's first group on {@code at}, as the player's, coming from {@code origin}. */
	private static void apply(ServerPlayer player, List<RuneDef> runes, Vec3 at, List<Entity> struck, Vec3 origin) {
		SpellPlan.Group group = SpellCompiler.compile(runes).root().groups.getFirst();
		Vec3 dir = at.subtract(origin).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : at.subtract(origin).normalize();
		CastEngine.onHit(new Cast(player), group, new Cast.Hit(struck, at, dir, origin, null, null, false), null);
	}

	/** Flattens a square of ground: grass at y, dirt below, air above. */
	private static void clear(TestSingleplayerContext world, int cx, int y, int cz, int half) {
		for (int x0 = cx - half; x0 < cx + half; x0 += 16) {
			for (int z0 = cz - half; z0 < cz + half; z0 += 16) {
				int x1 = Math.min(cx + half - 1, x0 + 15);
				int z1 = Math.min(cz + half - 1, z0 + 15);
				world.getServer().runCommand("fill " + x0 + " " + (y + 1) + " " + z0 + " " + x1 + " " + (y + 40) + " " + z1 + " air");
				world.getServer().runCommand("fill " + x0 + " " + y + " " + z0 + " " + x1 + " " + y + " " + z1 + " grass_block");
				world.getServer().runCommand("fill " + x0 + " " + (y - 3) + " " + z0 + " " + x1 + " " + (y - 1) + " " + z1 + " dirt");
			}
		}
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=!player,type=!text_display]");
	}

	private static void setup(TestSingleplayerContext world) {
		int[] where = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Vec3 at = ground(player.level(), player.getX(), player.getZ());
			return new int[] {(int) Math.floor(at.x), (int) at.y - 1, (int) Math.floor(at.z)};
		});
		clear(world, where[0], where[1], where[2], 32);
		stage = new Vec3(where[0] + 0.5, where[1] + 1, where[2] + 0.5);
		lane = stage.add(0, 0, -8);
		altar = at(0, -24);
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
			player.level().getGameRules().set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 128, server);
			Spellbooks.setMana(player, Mana.max(player));
			place(player, lane, 0, 0);
		});
	}

	// ------------------------------------------------------------------ the Fusion Altar

	private static void fusionAltar(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.level().setBlockAndUpdate(altar, WildercordBlocks.FUSION_ALTAR.defaultBlockState());
			place(player, stage, 0, 0);
		});
		Vec3 top = Vec3.atBottomCenterOf(altar);
		// The block on its own, close and from three quarters, by day and by night.
		director(context, world, top.add(1.9, 1.7, 2.1), top.add(0, 0.45, 0));
		context.waitTicks(5);
		shot(context, "fusion_altar_block");
		world.getServer().runCommand("time set 18000");
		context.waitTicks(3);
		shot(context, "fusion_altar_block_night");
		world.getServer().runCommand("time set 6000");
		cut(context);

		// The screen, for each kind of fusion: stand at the altar and open it.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Spellbooks.set(player, Spellbooks.get(player)
				.withSpell(0, ids(Runes.BOLT, Runes.FIRE, Runes.AMPLIFY, Runes.SPLIT_MOD))
				.withSpell(1, ids(Runes.BEAM, Runes.LIGHTNING, Runes.CHAIN_MOD))
				.withSpell(2, ids(Runes.SELF, Runes.HEAL, Runes.SWIFT)));
			SpellCaster.rename(player, 0, "Ember Fan");
			place(player, top.add(0, 0, 2.2), 180, 30);
			player.openMenu(player.level().getBlockState(altar).getMenuProvider(player.level(), altar));
		});
		context.waitTicks(10);
		context.getInput().setCursorPos(4, 4);
		altarSlots(world, ItemStack.EMPTY, RuneItem.stack(Runes.FIRE), RuneItem.stack(Runes.FIRE), RuneItem.stack(Runes.FIRE));
		context.waitTicks(20);
		shot(context, "fusion_screen_upgrade");
		altarSlots(world, new ItemStack(Items.AMETHYST_SHARD), RuneItem.stack(Runes.FIRE), ItemStack.EMPTY, RuneItem.stack(Runes.PUSH));
		context.waitTicks(20);
		shot(context, "fusion_screen_combine");
		altarSlots(world, new ItemStack(Items.STRING), ItemStack.EMPTY, new ItemStack(WildercordItems.BLANK_RUNE), ItemStack.EMPTY);
		context.waitTicks(10);
		attempt("fusion_screen_knot pick", () -> {
			double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
			double[] row = context.computeOnClient(mc -> mc.gui.screen() instanceof FusionAltarScreen s ? s.spellPoint(0) : null);
			if (row != null) {
				context.getInput().setCursorPos(row[0] * scale, row[1] * scale);
				context.waitTicks(1);
				context.getInput().pressMouse(0);
				context.waitTicks(2);
				context.getInput().setCursorPos(4, 4);
			}
		});
		context.waitTicks(20);
		shot(context, "fusion_screen_knot");
		context.runOnClient(mc -> mc.player.closeContainer());
		context.waitTicks(5);

		// The flourish of each fusion, filmed on the altar with nobody in the way.
		world.getServer().runOnServer(server -> place(player(server), stage, 0, 0));
		director(context, world, top.add(2.6, 2.2, 3.0), top.add(0, 0.9, 0));
		String[] kinds = {"upgrade", "combine", "knot"};
		for (String kind : kinds) {
			attempt("fusion_flourish_" + kind, () -> {
				world.getServer().runOnServer(server -> {
					ServerPlayer player = player(server);
					FusionAltarMenu menu = new FusionAltarMenu(0, player.getInventory(), ContainerLevelAccess.create(player.level(), altar));
					switch (kind) {
						case "upgrade" -> load(menu, ItemStack.EMPTY, RuneItem.stack(Runes.SHOCK), RuneItem.stack(Runes.SHOCK), RuneItem.stack(Runes.SHOCK));
						case "combine" -> load(menu, new ItemStack(Items.AMETHYST_SHARD), RuneItem.stack(Runes.FIRE), ItemStack.EMPTY, RuneItem.stack(Runes.PUSH));
						default -> load(menu, new ItemStack(Items.STRING), ItemStack.EMPTY, new ItemStack(WildercordItems.BLANK_RUNE), ItemStack.EMPTY);
					}
					menu.clickMenuButton(player, kind.equals("knot") ? FusionAltarMenu.BUTTON_KNOT : FusionAltarMenu.BUTTON_FUSE);
					menu.getSlot(FusionAltarMenu.RESULT).set(ItemStack.EMPTY);
					for (int i = 0; i <= FusionAltarMenu.CATALYST; i++) {
						menu.getSlot(i).set(ItemStack.EMPTY);
					}
				});
				context.waitTicks(4);
				shot(context, "fusion_flourish_" + kind);
				context.waitTicks(40);
			});
		}
		cut(context);
	}

	/** Puts a catalyst and up to three runes on the open altar, as the player would. */
	private static void altarSlots(TestSingleplayerContext world, ItemStack catalyst, ItemStack... runes) {
		world.getServer().runOnServer(server -> {
			if (player(server).containerMenu instanceof FusionAltarMenu menu) {
				load(menu, catalyst, runes);
				menu.broadcastChanges();
			}
		});
	}

	private static void load(FusionAltarMenu menu, ItemStack catalyst, ItemStack... runes) {
		for (int i = 0; i < FusionAltarMenu.RUNE_SLOTS; i++) {
			menu.getSlot(i).set(i < runes.length ? runes[i].copy() : ItemStack.EMPTY);
		}
		menu.getSlot(FusionAltarMenu.CATALYST).set(catalyst.copy());
	}

	// ------------------------------------------------------------------ spell galleries (fused effects, runes of the world)

	/** One spell for a gallery: its shape and effect, the ticks from the cast to the shot, and whether it's filmed by night. */
	private record Sample(String name, RuneDef shape, RuneDef effect, int ticks, boolean night) {}

	/**
	 * Casts each sample from the lane at three husks and films it from the side at its moment. Each
	 * one stands alone: a sample that goes wrong is skipped.
	 */
	private static void gallery(ClientGameTestContext context, TestSingleplayerContext world, String prefix, List<Sample> samples) {
		for (Sample sample : samples) {
			attempt(prefix + sample.name(), () -> {
				world.getServer().runCommand(sample.night() ? "time set 18000" : "time set 6000");
				boolean self = sample.shape() == Runes.SELF;
				world.getServer().runOnServer(server -> {
					ServerPlayer player = player(server);
					ServerLevel level = player.level();
					place(player, lane, 0, 8);
					if (!self) {
						husk(level, lane.add(0, 0, 6), 180);
						husk(level, lane.add(-1.8, 0, 6.6), 180);
						husk(level, lane.add(1.8, 0, 7.2), 180);
					}
				});
				if (self) {
					director(context, world, lane.add(-3.6, 2.2, 3.4), lane.add(0, 0.8, 0.4));
				} else {
					director(context, world, lane.add(-7.5, 3.6, 4.2), lane.add(0, 1.0, 5.2));
				}
				context.waitTicks(3);
				world.getServer().runOnServer(server -> castNow(player(server), 0, List.of(sample.shape(), sample.effect())));
				context.waitTicks(Math.max(1, sample.ticks()));
				shot(context, prefix + sample.name());
				cut(context);
				context.waitTicks(30);
				clean(world);
				context.waitTicks(40);
			});
		}
		world.getServer().runCommand("time set 6000");
	}

	private static void fusedSpells(ClientGameTestContext context, TestSingleplayerContext world) {
		gallery(context, world, "fused_", List.of(
			new Sample("firestorm", Runes.BOLT, Runes.FIRESTORM, 6, false),
			new Sample("tempest", Runes.BEAM, Runes.TEMPEST, 3, false),
			new Sample("glacier", Runes.CONE, Runes.GLACIER, 4, false),
			new Sample("warp", Runes.BOLT, Runes.WARP, 6, false),
			new Sample("lifesteal", Runes.BEAM, Runes.LIFESTEAL, 3, false),
			new Sample("bloom", Runes.SELF, Runes.BLOOM, 8, false),
			new Sample("steam", Runes.BOLT, Runes.STEAM, 6, false),
			new Sample("magma", Runes.BOLT, Runes.MAGMA, 10, false),
			new Sample("plasma", Runes.BEAM, Runes.PLASMA, 3, false),
			new Sample("hail", Runes.BOLT, Runes.HAIL, 8, false),
			new Sample("surge", Runes.SELF, Runes.SURGE, 5, false),
			new Sample("nullify", Runes.BEAM, Runes.NULLIFY, 3, false)));
	}

	// ------------------------------------------------------------------ parrying

	private static void parry(ClientGameTestContext context, TestSingleplayerContext world) {
		standForParry(world);
		// A husk's Harm bolt turned back by a Shield raised at the last moment, from the side.
		director(context, world, lane.add(-6.5, 2.0, 3.0), lane.add(0, 1.3, 3.0));
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Mob husk = husk(player.level(), lane.add(0, 0, 6), 180);
			if (husk != null) {
				husk.lookAt(EntityAnchorArgument.Anchor.EYES, lane.add(0, 1.2, 0));
				fireBolt(player, husk);
			}
			SpellCaster.cast(player, 0);
		});
		context.waitTicks(2);
		shot(context, "parry_flash_a");
		context.waitTicks(2);
		shot(context, "parry_flash_b");
		context.waitTicks(3);
		shot(context, "parry_reflect");
		context.waitTicks(30);
		clean(world);

		// A beam can't be turned: it's negated, and a counter-burst answers it.
		standForParry(world);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Mob husk = husk(player.level(), lane.add(0, 0, 7), 180);
			SpellCaster.cast(player, 0);
			if (husk != null) {
				husk.lookAt(EntityAnchorArgument.Anchor.EYES, lane.add(0, 1.2, 0));
				CastEngine.cast(new Cast(husk), SpellCompiler.compile(List.of(Runes.BEAM, Runes.HARM)).root());
			}
		});
		context.waitTicks(2);
		shot(context, "parry_counter_burst");
		context.waitTicks(30);
		cut(context);
		// And from behind, with the HUD (the "Parried!" line above the hotbar).
		clean(world);
		standForParry(world);
		camera(context, CameraType.THIRD_PERSON_BACK);
		context.waitTicks(3);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Mob husk = husk(player.level(), lane.add(0, 0, 6), 180);
			if (husk != null) {
				husk.lookAt(EntityAnchorArgument.Anchor.EYES, lane.add(0, 1.2, 0));
				fireBolt(player, husk);
			}
			SpellCaster.cast(player, 0);
		});
		context.waitTicks(6);
		shot(context, "parry_hud");
		context.waitTicks(20);
	}

	private static void standForParry(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 4, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 2, false, false));
			player.setHealth(player.getMaxHealth());
			player.removeAttached(WildercordAttachments.SPELL_SHIELD);
			place(player, lane, 0, 0);
			SpellCaster.edit(player, 0, List.of());
			SpellCaster.edit(player, 0, ids(Runes.SELF, Runes.SHIELD));
			Spellbooks.setMana(player, Mana.max(player));
			for (int i = 0; i < SpellSlots.ALL; i++) {
				Spellbooks.setReadyAt(player, i, 0);
			}
		});
	}

	/** The husk's Harm bolt, fired from just in front of it straight at the player's chest. */
	private static void fireBolt(ServerPlayer player, Mob husk) {
		SpellPlan.Group group = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM)).root().groups.getFirst();
		Vec3 chest = player.getBoundingBox().getCenter();
		Vec3 from = husk.getEyePosition().add(chest.subtract(husk.getEyePosition()).normalize().scale(0.8));
		RuneBolt.launch(new Cast(husk), group, null, from, chest.subtract(from), false);
	}

	// ------------------------------------------------------------------ wild magic

	private static void wildMagic(ClientGameTestContext context, TestSingleplayerContext world) {
		WildMagic.Surge[] surges = {WildMagic.Surge.BUTTERFLIES, WildMagic.Surge.LEVITATE, WildMagic.Surge.ELEMENT, WildMagic.Surge.GRAND, WildMagic.Surge.WISPS};
		for (WildMagic.Surge surge : surges) {
			attempt("wild_" + surge.id, () -> {
				world.getServer().runOnServer(server -> {
					ServerPlayer player = player(server);
					ServerLevel level = player.level();
					player.setGameMode(GameType.SURVIVAL);
					player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 4, false, false));
					player.setHealth(player.getMaxHealth());
					player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
					place(player, lane, 0, 0);
					// Something to aim at, and two close enough to float.
					husk(level, lane.add(0, 0, 8), 180);
					husk(level, lane.add(-2.2, 0, 3), 160);
					husk(level, lane.add(2.4, 0, 3.6), 200);
					SpellCaster.edit(player, 1, List.of());
					SpellCaster.edit(player, 1, ids(surge == WildMagic.Surge.GRAND ? Runes.BURST : Runes.BOLT, Runes.FIRE));
					Spellbooks.setReadyAt(player, 1, 0);
				});
				director(context, world, lane.add(-5.5, 2.6, 5.5), lane.add(0, 1.3, 2.6));
				world.getServer().runOnServer(server -> {
					ServerPlayer player = player(server);
					Spellbooks.setMana(player, 1);
					WildSurge.force(player, surge);
					// The first press only asks; the second overcasts, and the forced surge goes off.
					SpellCaster.cast(player, 1);
					SpellCaster.cast(player, 1);
				});
				context.waitTicks(surge == WildMagic.Surge.LEVITATE ? 12 : 6);
				shot(context, "wild_" + surge.id);
				cut(context);
				world.getServer().runOnServer(server -> WildSurge.force(player(server), null));
				context.waitTicks(40);
				clean(world);
				context.waitTicks(5);
			});
		}
	}

	// ------------------------------------------------------------------ magic that changes the world

	private static void worldMagic(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> place(player(server), stage, 0, 0));

		attempt("world_fire_grass", () -> {
			BlockPos patch = at(16, -16);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				meadow(player.level(), patch);
				apply(player, List.of(Runes.TOUCH, Runes.FIRE), Vec3.atBottomCenterOf(patch), List.of(), player.position());
			});
			director(context, world, Vec3.atBottomCenterOf(patch).add(3.6, 2.8, 3.8), Vec3.atBottomCenterOf(patch).add(0, 0.2, 0));
			context.waitTicks(20);
			shot(context, "world_fire_grass");
			cut(context);
			world.getServer().runOnServer(server -> douse(player(server).level(), patch, 6));
		});

		attempt("world_frost_pond", () -> {
			BlockPos pool = at(16, 0);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				pool(player.level(), pool, 1);
			});
			Vec3 middle = Vec3.atCenterOf(pool);
			director(context, world, middle.add(0.4, 8.5, 2.6), middle.add(0, -1, 0));
			context.waitTicks(3);
			shot(context, "world_frost_pond_before");
			world.getServer().runOnServer(server -> apply(player(server), List.of(Runes.TOUCH, Runes.FROST, Runes.WIDEN), middle, List.of(), player(server).position()));
			context.waitTicks(6);
			shot(context, "world_frost_pond");
			cut(context);
		});

		attempt("world_steam", () -> {
			BlockPos pool = at(16, 14);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				pool(player.level(), pool, 2);
				apply(player, List.of(Runes.TOUCH, Runes.FIRE), Vec3.atCenterOf(pool.below()), List.of(), player.position());
			});
			director(context, world, Vec3.atCenterOf(pool).add(-4.5, 2.0, -4.5), Vec3.atCenterOf(pool).add(0, 0.2, 0));
			context.waitTicks(10);
			shot(context, "world_steam");
			cut(context);
		});

		attempt("world_storm_conduct", () -> {
			BlockPos pool = at(-16, 14);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ServerLevel level = player.level();
				pool(level, pool, 2);
				husk(level, new Vec3(pool.getX() + 0.5, pool.getY() - 2, pool.getZ() + 0.5), 0);
				husk(level, new Vec3(pool.getX() - 1.5, pool.getY() - 2, pool.getZ() + 1.5), 90);
				husk(level, new Vec3(pool.getX() + 1.5, pool.getY() - 2, pool.getZ() - 1.5), 200);
			});
			director(context, world, Vec3.atCenterOf(pool).add(4.8, 3.0, -4.8), Vec3.atCenterOf(pool).add(0, -0.8, 0));
			// Let the husks notice they're standing in water.
			context.waitTicks(5);
			world.getServer().runOnServer(server -> apply(player(server), List.of(Runes.TOUCH, Runes.SHOCK),
				Vec3.atCenterOf(pool.offset(2, -1, 2)), List.of(), player(server).position()));
			context.waitTicks(2);
			shot(context, "world_storm_conduct");
			context.waitTicks(3);
			shot(context, "world_storm_conduct_b");
			cut(context);
			clean(world);
		});

		attempt("world_wind_arrows", () -> {
			BlockPos spot = at(-16, 0);
			Vec3 point = Vec3.atBottomCenterOf(spot).add(0, 1.5, 0);
			director(context, world, point.add(-1.2, 1.2, -5.0), point.add(0.6, -0.2, 0));
			world.getServer().runOnServer(server -> {
				ServerLevel level = player(server).level();
				for (int i = 0; i < 4; i++) {
					Entity arrow = EntityTypes.ARROW.create(level, EntitySpawnReason.COMMAND);
					if (arrow != null) {
						arrow.snapTo(point.x + 2.4, point.y - 0.6 + i * 0.4, point.z - 0.9 + i * 0.6, 90, 0);
						arrow.setNoGravity(true);
						arrow.setDeltaMovement(-0.6, 0, 0);
						arrow.addTag(TAG);
						level.addFreshEntity(arrow);
					}
				}
			});
			context.waitTicks(2);
			shot(context, "world_wind_arrows_before");
			// The gust comes from the west, so it blows the arrows back east.
			world.getServer().runOnServer(server -> apply(player(server), List.of(Runes.TOUCH, Runes.PUSH), point, List.of(), point.add(-3, 0, 0)));
			context.waitTicks(3);
			shot(context, "world_wind_arrows");
			cut(context);
			clean(world);
		});

		attempt("world_earth_heave", () -> {
			BlockPos spot = at(-16, -16);
			Vec3 point = Vec3.atBottomCenterOf(spot);
			world.getServer().runOnServer(server -> {
				ServerLevel level = player(server).level();
				husk(level, point.add(1.2, 0, 0.4), 200);
				husk(level, point.add(-1.0, 0, -0.8), 20);
			});
			director(context, world, point.add(4.5, 2.4, 4.5), point.add(0, 0.6, 0));
			world.getServer().runOnServer(server -> apply(player(server), List.of(Runes.TOUCH, Runes.TREMOR), point, List.of(), player(server).position()));
			context.waitTicks(5);
			shot(context, "world_earth_heave");
			cut(context);
			clean(world);
		});

		attempt("world_life_bloom", () -> {
			BlockPos spot = at(-4, 20);
			Vec3 point = Vec3.atBottomCenterOf(spot);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				apply(player, List.of(Runes.TOUCH, Runes.REGROWTH), point, List.of(), player.position());
				apply(player, List.of(Runes.TOUCH, Runes.HEAL), point.add(0.8, 0, 0.6), List.of(), player.position());
				apply(player, List.of(Runes.TOUCH, Runes.HEAL), point.add(-0.7, 0, -0.5), List.of(), player.position());
			});
			director(context, world, point.add(2.8, 2.2, 3.0), point.add(0, 0.2, 0));
			context.waitTicks(10);
			shot(context, "world_life_bloom");
			cut(context);
		});
	}

	/** Digs a 5x5 pool {@code depth} deep, walled in stone, whose middle surface block is {@code centre.below()} (open air above). */
	private static void pool(ServerLevel level, BlockPos centre, int depth) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int dy = 0; dy <= 3; dy++) {
					level.setBlockAndUpdate(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
				}
				for (int dy = 1; dy <= depth + 1; dy++) {
					boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3 || dy == depth + 1;
					level.setBlockAndUpdate(centre.offset(dx, -dy, dz), rim ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
				}
			}
		}
	}

	/** A 5x5 patch of grass blocks with short grass and a few flowers growing on them. */
	private static void meadow(ServerLevel level, BlockPos centre) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				level.setBlockAndUpdate(centre.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState());
				boolean flower = (dx + dz) % 3 == 0 && dx != 0;
				level.setBlockAndUpdate(centre.offset(dx, 0, dz), flower ? Blocks.POPPY.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
			}
		}
	}

	/** Puts out any fire around {@code centre}. */
	private static void douse(ServerLevel level, BlockPos centre, int r) {
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
			if (level.getBlockState(pos).is(net.minecraft.tags.BlockTags.FIRE)) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
		}
	}

	// ------------------------------------------------------------------ world events

	private static void manaStorm(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, stage.add(0, 0, -12), 0, -14);
			WorldEvents.startStorm(player.level(), player, true);
		});
		context.waitTicks(60);
		camera(context, CameraType.THIRD_PERSON_BACK);
		context.waitTicks(5);
		shot(context, "event_mana_storm");
		camera(context, CameraType.FIRST_PERSON);
		director(context, world, stage.add(-22, 9, -26), stage.add(0, 6, 4));
		context.waitTicks(20);
		shot(context, "event_mana_storm_wide");
		// A cast under the storm (it may surge).
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, stage.add(0, 0, -12), 0, 0);
			castNow(player, 0, List.of(Runes.BOLT, Runes.LIGHTNING));
		});
		context.waitTicks(4);
		shot(context, "event_mana_storm_cast");
		cut(context);
		world.getServer().runOnServer(server -> WorldEvents.storms().forEach(ManaStorm::stop));
		context.waitTicks(10);
	}

	private static void fallenStar(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		BlockPos land = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, stage.add(0, 0, -6), 0, 0);
			return WorldEvents.startStar(player.level(), player, true);
		});
		if (land == null) {
			throw new IllegalStateException("the star found nowhere to land");
		}
		Vec3 ground = Vec3.atBottomCenterOf(land);
		// Well back and looking up over where it lands: the last of its streak comes down into the frame.
		director(context, world, ground.add(-18, 2.5, -14), ground.add(0, 14, 0));
		context.waitTicks(34);
		shot(context, "event_star_falling_a");
		context.waitTicks(8);
		shot(context, "event_star_falling_b");
		context.waitTicks(5);
		shot(context, "event_star_falling_c");
		// Landed, with its guards risen (the player is near).
		context.waitTicks(70);
		BlockPos star = world.getServer().computeOnServer(server -> find(player(server).level(), land));
		Vec3 heart = star == null ? ground : Vec3.atBottomCenterOf(star);
		director(context, world, heart.add(4.2, 3.2, 4.8), heart.add(0, 0.4, 0));
		context.waitTicks(10);
		shot(context, "event_fallen_star");
		director(context, world, heart.add(-16, 6, -16), heart.add(0, 7, 0));
		context.waitTicks(5);
		shot(context, "event_star_column");
		cut(context);
		camera(context, CameraType.THIRD_PERSON_BACK);
		world.getServer().runOnServer(server -> place(player(server), heart.add(0, 0, -9), 0, 12));
		context.waitTicks(10);
		shot(context, "event_star_guards");
		// Tidy: the guards gone, the star opened (so the crater fills back in).
		world.getServer().runOnServer(server -> {
			if (star != null) {
				ServerLevel level = player(server).level();
				FallenStars.guards(level, star).forEach(guard -> guard.kill(level));
				FallenStars.open(level, star, player(server));
			}
		});
		context.waitTicks(40);
		world.getServer().runCommand("kill @e[type=!player,type=!text_display]");
	}

	/** The star near where it was meant to land: in its cell, or sunk to its crater's floor. */
	private static BlockPos find(ServerLevel level, BlockPos land) {
		for (int dy = 1; dy >= -5; dy--) {
			BlockPos p = land.offset(0, dy, 0);
			if (level.getBlockState(p).is(EventContent.FALLEN_STAR)) {
				return p;
			}
		}
		return null;
	}

	private static void rift(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		boolean opened = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, stage.add(0, 0, -12), 0, 0);
			return WorldEvents.startRift(player.level(), player, true) != null;
		});
		if (!opened) {
			throw new IllegalStateException("no room for a rift");
		}
		Vec3 base = world.getServer().computeOnServer(server -> WorldEvents.riftIn(player(server).level()).base());
		director(context, world, base.add(-7, 2.6, -5), base.add(0, 1.9, 0));
		context.waitTicks(20);
		shot(context, "event_rift_open");
		// The first wave steps out from three seconds in.
		context.waitTicks(70);
		shot(context, "event_rift_wave");
		cut(context);
		camera(context, CameraType.THIRD_PERSON_BACK);
		context.waitTicks(15);
		shot(context, "event_rift_bossbar");
		camera(context, CameraType.FIRST_PERSON);
		// Straight on to the last wave, and its Riftcaller.
		world.getServer().runOnServer(server -> {
			RiftSiege rift = WorldEvents.riftIn(player(server).level());
			if (rift == null) {
				return;
			}
			try {
				java.lang.reflect.Method spawnWave = RiftSiege.class.getDeclaredMethod("spawnWave", int.class, long.class);
				spawnWave.setAccessible(true);
				spawnWave.invoke(rift, 3, player(server).level().getGameTime());
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("couldn't call the last wave", e);
			}
		});
		context.waitTicks(80);
		camera(context, CameraType.THIRD_PERSON_BACK);
		context.waitTicks(5);
		shot(context, "event_riftcaller_bossbar");
		cut(context);
		Vec3 caller = world.getServer().computeOnServer(server -> {
			RiftSiege rift = WorldEvents.riftIn(player(server).level());
			Mob mob = rift == null ? null : rift.riftcaller();
			return mob == null ? null : mob.position();
		});
		if (caller != null) {
			Vec3 toward = stage.add(0, 0, -12).subtract(caller).multiply(1, 0, 1);
			Vec3 dir = toward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, -1) : toward.normalize();
			director(context, world, caller.add(dir.scale(4.0)).add(dir.z * 1.5, 1.9, -dir.x * 1.5), caller.add(0, 1.3, 0));
			context.waitTicks(3);
			shot(context, "event_riftcaller");
			cut(context);
		}
		// Sealed with three elements, and everything it let out cleared away.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			RiftSiege rift = WorldEvents.riftIn(player.level());
			if (rift != null) {
				for (String element : List.of("fire", "frost", "storm")) {
					rift.strike(player, element);
				}
			}
		});
		context.waitTicks(20);
		world.getServer().runCommand("kill @e[type=!player,type=!text_display]");
	}

	// ------------------------------------------------------------------ familiars

	private static void familiars(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		String[] elements = {"fire", "frost", "storm", "wind", "earth", "life", "void", "arcane"};
		Vec3 row = stage.add(0, 1.4, 6);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, stage.add(0, 0, -10), 0, 0);
			for (int i = 0; i < elements.length; i++) {
				Vec3 spot = row.add((i - 3.5) * 1.5, (i % 2) * 0.35, 0);
				Wisp wisp = WispSpawner.spawn(player.level(), BlockPos.containing(spot), elements[i]);
				if (wisp != null) {
					wisp.setNoAi(true);
					wisp.setNoGravity(true);
					wisp.snapTo(spot.x, spot.y, spot.z, 180, 0);
					wisp.addTag(TAG);
				}
			}
		});
		director(context, world, row.add(0, 0.5, -6.2), row.add(0, 0.1, 0));
		context.waitTicks(10);
		shot(context, "familiar_wild_wisps");
		director(context, world, row.add(-4.4, 0.4, -2.0), row.add(-5.25, 0.2, 0));
		context.waitTicks(4);
		shot(context, "familiar_wisp_close");
		cut(context);
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");

		// A Fire wisp fed three times bonds, and settles at the player's shoulder.
		world.getServer().runCommand("time set 6000");
		int id = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, stage, 0, 10);
			Wisp wisp = WispSpawner.spawn(player.level(), BlockPos.containing(stage.add(0, 1.5, 2.5)), "fire");
			return wisp == null ? -1 : wisp.getId();
		});
		for (int i = 0; i < WispRules.TAMING_HITS; i++) {
			world.getServer().runOnServer(server -> {
				if (player(server).level().getEntity(id) instanceof Wisp wisp) {
					wisp.offer(player(server), "fire", null);
				}
			});
			context.waitTicks(WispRules.MIN_GAP_TICKS + 3);
		}
		context.waitTicks(60);
		camera(context, CameraType.THIRD_PERSON_FRONT);
		context.waitTicks(20);
		shot(context, "familiar_shoulder_day");
		world.getServer().runCommand("time set 18000");
		context.waitTicks(10);
		shot(context, "familiar_shoulder_night");
		world.getServer().runCommand("time set 6000");

		// The Wisp Lantern, held.
		world.getServer().runOnServer(server -> player(server).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FamiliarContent.WISP_LANTERN)));
		context.waitTicks(5);
		shot(context, "familiar_lantern_third_person");
		camera(context, CameraType.FIRST_PERSON);
		world.getServer().runOnServer(server -> place(player(server), stage, 0, 20));
		context.waitTicks(5);
		shot(context, "familiar_lantern_first_person");
		// Sent home, so it's out of the next shots.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			if (!Familiars.get(player).out().isEmpty()) {
				ItemStack lantern = player.getItemInHand(InteractionHand.MAIN_HAND);
				lantern.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
			}
		});
		context.waitTicks(5);
	}

	// ------------------------------------------------------------------ Cord cosmetics

	private static void cosmetics(ClientGameTestContext context, TestSingleplayerContext world) {
		// The Cosmetics page, opened from the Cord screen's tab, with an option pointed at.
		context.setScreen(CordScreen::new);
		context.waitTicks(10);
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		double[] tab = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).pagePoint(3));
		context.getInput().setCursorPos(tab[0] * scale, tab[1] * scale);
		context.waitTicks(1);
		context.getInput().pressMouse(0);
		context.waitTicks(10);
		for (String key : List.of("material:amethyst", "trail:embers")) {
			attempt("cosmetics_page " + key, () -> {
				double[] option = context.computeOnClient(mc -> mc.gui.screen() instanceof CordStyleScreen s ? s.optionPoint(key) : null);
				if (option != null) {
					context.getInput().setCursorPos(option[0] * scale, option[1] * scale);
					context.waitTicks(12);
					shot(context, "cosmetics_page_" + key.replace(':', '_'));
				}
			});
		}
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitTicks(5);

		// The Cord on the wrist, in a few styles, just after a cast (the beads flare, the trail streams).
		String[][] styles = {{"gold", "spell", "embers"}, {"amethyst", "magenta", "petals"}, {"obsidian", "cyan", "sparks"}, {"prismarine", "spell", "snow"}};
		camera(context, CameraType.THIRD_PERSON_FRONT);
		for (String[] style : styles) {
			attempt("cosmetics_wrist_" + style[0], () -> {
				world.getServer().runOnServer(server -> {
					ServerPlayer player = player(server);
					CordCosmetics.buy(player, "material:" + style[0]);
					CordCosmetics.buy(player, "glow:" + style[1]);
					CordCosmetics.buy(player, "trail:" + style[2]);
					CordCosmetics.wear(player, new CordStyles.Style(style[0], style[1], style[2]));
					place(player, stage, 0, 6);
					SpellCaster.edit(player, 0, List.of());
					SpellCaster.edit(player, 0, ids(Runes.SELF, Runes.HEAL));
					Spellbooks.setReadyAt(player, 0, 0);
					Charging.request(player, 0, true);
				});
				context.waitTicks(20);
				shot(context, "cosmetics_wrist_" + style[0] + "_charging");
				world.getServer().runOnServer(server -> Charging.request(player(server), 0, false));
				context.waitTicks(3);
				shot(context, "cosmetics_wrist_" + style[0] + "_" + style[2]);
				context.waitTicks(30);
			});
		}
		camera(context, CameraType.FIRST_PERSON);
	}

	// ------------------------------------------------------------------ the Runesmith, contracts and chorus

	private static void runesmith(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos desk = at(10, -20);
		Vec3 deskFoot = Vec3.atBottomCenterOf(desk);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			level.setBlockAndUpdate(desk, Runesmith.SCRIBING_DESK.defaultBlockState());
			Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
			if (villager != null) {
				villager.snapTo(deskFoot.x, deskFoot.y, deskFoot.z + 1.3, 180, 0);
				villager.setNoAi(true);
				villager.addTag(TAG);
				villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), Runesmith.PROFESSION).withLevel(5));
				level.addFreshEntity(villager);
				villager.setYHeadRot(180);
			}
			place(player, stage, 0, 0);
		});
		director(context, world, deskFoot.add(3.2, 2.1, 3.6), deskFoot.add(0, 0.9, 0.7));
		context.waitTicks(10);
		shot(context, "runesmith_desk");
		cut(context);

		// Its trades, with known runes in the pack (so the buyback and reroll offers appear too).
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			player.getInventory().clearContent();
			ItemStack heals = RuneItem.stack(Runes.HEAL);
			heals.setCount(2);
			player.getInventory().add(heals);
			player.getInventory().add(RuneItem.stack(Runes.BEAM));
			player.getInventory().add(RuneItem.stack(Runes.BURST));
			player.getInventory().add(new ItemStack(Items.EMERALD, 32));
			place(player, deskFoot.add(0, 0, 3.6), 180, 10);
			List<Villager> villagers = player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(6), v -> v.entityTags().contains(TAG));
			if (!villagers.isEmpty()) {
				villagers.getFirst().interact(player, InteractionHand.MAIN_HAND, villagers.getFirst().position());
			}
		});
		context.waitTicks(15);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "runesmith_trades");
		context.runOnClient(mc -> {
			if (mc.player.containerMenu != mc.player.inventoryMenu) {
				mc.player.closeContainer();
			}
		});
		context.waitTicks(5);

		// The contract board: right-clicking the desk reads it out.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.getInventory().clearContent();
			place(player, deskFoot.add(-1.6, 0, 2.2), 215, 30);
			player.level().getBlockState(desk).useWithoutItem(player.level(), player, new BlockHitResult(Vec3.atCenterOf(desk), Direction.UP, desk, false));
		});
		context.waitTicks(5);
		shot(context, "runesmith_contract_board");
		world.getServer().runOnServer(server -> player(server).setGameMode(GameType.CREATIVE));
	}

	private static void chorus(ClientGameTestContext context, TestSingleplayerContext world) {
		// Filmed from the side, then again from behind with the HUD.
		for (int take = 0; take < 2; take++) {
			boolean side = take == 0;
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ServerLevel level = player.level();
				place(player, lane, 0, 10);
				husk(level, lane.add(-1.6, 0, 2.2), 180);
				husk(level, lane.add(1.4, 0, 2.6), 180);
				husk(level, lane.add(0.2, 0, -2.4), 0);
			});
			if (side) {
				director(context, world, lane.add(-8, 3.2, 1.2), lane.add(1.2, 0.8, 0.6));
			} else {
				camera(context, CameraType.THIRD_PERSON_BACK);
				context.waitTicks(3);
			}
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ServerLevel level = player.level();
				// The second voice: a husk beside the player singing the same shape (a test has only one real player).
				Mob singer = husk(level, lane.add(3, 0, 0), 0);
				if (singer == null) {
					return;
				}
				List<RuneDef> frost = List.of(Runes.BURST, Runes.FROST);
				SpellCompiler.Compiled first = SpellCompiler.compile(frost);
				Chorus.sing(new Cast(singer, 1, Heart.Bonuses.NONE, false, null, new Cast.Info(first.root(), frost.size(), "", frost)), frost, first.root());
				List<RuneDef> fire = List.of(Runes.BURST, Runes.FIRE);
				SpellCompiler.Compiled second = SpellCompiler.compile(fire);
				Cast mine = new Cast(player, 1, Heart.bonuses(player), false, null, new Cast.Info(second.root(), fire.size(), "", fire));
				Chorus.Sung sung = Chorus.sing(mine, fire, second.root());
				CastEngine.cast(sung.cast(), sung.root());
			});
			context.waitTicks(4);
			shot(context, side ? "chorus_burst" : "chorus_hud");
			cut(context);
			context.waitTicks(40);
			clean(world);
			context.waitTicks(5);
		}
	}

	// ------------------------------------------------------------------ casting gear

	private static ItemStack gearStack(GearDef def) {
		return new ItemStack(GearItems.get(def));
	}

	private static void gear(ClientGameTestContext context, TestSingleplayerContext world) {
		camera(context, CameraType.THIRD_PERSON_FRONT);
		Object[][] holds = {
			{"staff_fire", GearDef.staff("fire"), null},
			{"staff_frost", GearDef.staff("frost"), null},
			{"greater_staff_void", GearDef.greaterStaff("void"), null},
			{"staff_and_focus", GearDef.staff("storm"), GearDef.HASTE},
			{"foci", GearDef.ECHOES, GearDef.DEEP_WELL},
		};
		for (Object[] hold : holds) {
			attempt("gear_" + hold[0], () -> {
				world.getServer().runOnServer(server -> {
					ServerPlayer player = player(server);
					place(player, stage, 0, 8);
					player.setItemInHand(InteractionHand.MAIN_HAND, gearStack((GearDef) hold[1]));
					player.setItemInHand(InteractionHand.OFF_HAND, hold[2] == null ? ItemStack.EMPTY : gearStack((GearDef) hold[2]));
				});
				context.waitTicks(8);
				shot(context, "gear_" + hold[0]);
			});
		}
		// The foci from the player's own eyes.
		camera(context, CameraType.FIRST_PERSON);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, stage, 0, 15);
			player.setItemInHand(InteractionHand.MAIN_HAND, gearStack(GearDef.THRIFT));
			player.setItemInHand(InteractionHand.OFF_HAND, gearStack(GearDef.HASTE));
		});
		context.waitTicks(8);
		shot(context, "gear_foci_first_person");

		// A charged cast of the staff's element leaves the staff's flourish.
		for (String element : List.of("fire", "frost")) {
			attempt("gear_flourish_" + element, () -> {
				world.getServer().runOnServer(server -> {
					ServerPlayer player = player(server);
					place(player, lane, 0, 4);
					player.setItemInHand(InteractionHand.MAIN_HAND, gearStack(GearDef.staff(element)));
					player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
					husk(player.level(), lane.add(0, 0, 7), 180);
					SpellCaster.edit(player, 0, List.of());
					SpellCaster.edit(player, 0, ids(Runes.BOLT, element.equals("fire") ? Runes.FIRE : Runes.FROST));
					Spellbooks.setMana(player, Mana.max(player));
					Spellbooks.setReadyAt(player, 0, 0);
					Charging.request(player, 0, true);
				});
				director(context, world, lane.add(-4.2, 2.0, 2.6), lane.add(0, 1.3, 1.4));
				context.waitTicks(Charging.FULL);
				shot(context, "gear_charging_" + element);
				world.getServer().runOnServer(server -> Charging.request(player(server), 0, false));
				context.waitTicks(2);
				shot(context, "gear_flourish_" + element);
				context.waitTicks(4);
				shot(context, "gear_flourish_" + element + "_b");
				cut(context);
				context.waitTicks(30);
				clean(world);
			});
		}

		// The Tome of the Fifth Page opens a fifth row in the Cord screen, and a fifth spell on the HUD.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, stage, 0, 0);
			player.setItemInHand(InteractionHand.MAIN_HAND, gearStack(GearDef.staff("arcane")));
			player.setItemInHand(InteractionHand.OFF_HAND, gearStack(GearDef.TOME));
			Spellbooks.set(player, Spellbooks.get(player)
				.withSpell(0, ids(Runes.BOLT, Runes.FIRE, Runes.SPLIT_MOD))
				.withSpell(1, ids(Runes.BEAM, Runes.LIGHTNING, Runes.CHAIN_MOD))
				.withSpell(2, ids(Runes.SELF, Runes.SWIFT, Runes.STONESKIN))
				.withSpell(3, ids(Runes.ZONE, Runes.FROST, Runes.WIDEN)));
			SpellCaster.edit(player, SpellSlots.TOME, List.of());
			SpellCaster.edit(player, SpellSlots.TOME, ids(Runes.CONSTELLATION, Runes.STARSHARD, Runes.AMPLIFY));
		});
		context.waitTicks(5);
		context.setScreen(CordScreen::new);
		context.waitTicks(10);
		attempt("gear_tome_row", () -> {
			double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
			double[] row = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).rowPoint(SpellSlots.TOME));
			context.getInput().setCursorPos(row[0] * scale, row[1] * scale);
			context.waitTicks(1);
			context.getInput().pressMouse(0);
			context.waitTicks(3);
			context.getInput().setCursorPos(4, 4);
			context.waitTicks(10);
			shot(context, "gear_tome_row");
		});
		context.runOnClient(mc -> mc.gui.setScreen(null));
		world.getServer().runOnServer(server -> Spellbooks.set(player(server), Spellbooks.get(player(server)).withSelected(SpellSlots.TOME)));
		context.waitTicks(10);
		shot(context, "gear_tome_hud");
		world.getServer().runOnServer(server -> Spellbooks.set(player(server), Spellbooks.get(player(server)).withSelected(0)));
	}

	// ------------------------------------------------------------------ runes of the world

	private static void newRunes(ClientGameTestContext context, TestSingleplayerContext world) {
		gallery(context, world, "rune_", List.of(
			new Sample("sandstorm", Runes.BEAM, Runes.SANDSTORM, 14, false),
			new Sample("tidecall", Runes.BEAM, Runes.TIDECALL, 6, false),
			new Sample("resonant_shriek", Runes.BEAM, Runes.RESONANT_SHRIEK, 6, false),
			new Sample("vinelash", Runes.BEAM, Runes.VINELASH, 4, false),
			new Sample("fangs", Runes.BEAM, Runes.FANGS, 8, false),
			new Sample("moonpetal", Runes.BEAM, Runes.MOONPETAL, 8, true),
			new Sample("soulfire", Runes.BEAM, Runes.SOULFIRE, 14, true),
			new Sample("starlight_tether", Runes.BEAM, Runes.STARLIGHT_TETHER, 10, true),
			new Sample("constellation", Runes.CONSTELLATION, Runes.HARM, 4, true),
			new Sample("vortex", Runes.VORTEX, Runes.HARM, 24, false),
			new Sample("eclipse", Runes.BEAM, Runes.ECLIPSE, 16, false),
			new Sample("manaburn", Runes.BEAM, Runes.MANABURN, 4, false),
			new Sample("blazecall", Runes.BEAM, Runes.BLAZECALL, 12, false),
			new Sample("basalt_surge", Runes.BEAM, Runes.BASALT_SURGE, 6, false),
			new Sample("rootsnare", Runes.BEAM, Runes.ROOTSNARE, 6, false),
			new Sample("stalactite", Runes.BEAM, Runes.STALACTITE, 8, false),
			new Sample("hoarfrost", Runes.BEAM, Runes.HOARFROST, 64, false),
			new Sample("riftcall", Runes.BEAM, Runes.RIFTCALL, 20, true),
			new Sample("starshard", Runes.BEAM, Runes.STARSHARD, 5, true)));
	}

	/** A Blank Rune attuning: the player meditates in mushroom fields (made round a corner of the stage). */
	private static void attunement(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3 spot = Vec3.atBottomCenterOf(at(8, 8));
		BlockPos c = BlockPos.containing(spot);
		world.getServer().runCommand("fillbiome " + (c.getX() - 8) + " " + (c.getY() - 4) + " " + (c.getZ() - 8) + " "
			+ (c.getX() + 8) + " " + (c.getY() + 8) + " " + (c.getZ() + 8) + " minecraft:mushroom_fields");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			place(player, spot, 0, 10);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WildercordItems.BLANK_RUNE, 2));
		});
		context.waitTicks(10);
		context.getInput().holdKey(options -> options.keyShift);
		try {
			// Meditation settles in after a second of stillness; then the circle opens and the blank brightens.
			context.waitTicks(80);
			director(context, world, spot.add(2.8, 3.6, 3.4), spot.add(0, 0.3, 0));
			context.waitTicks(10);
			shot(context, "attune_circle");
			cut(context);
			camera(context, CameraType.THIRD_PERSON_FRONT);
			context.waitTicks(100);
			shot(context, "attune_player");
			camera(context, CameraType.FIRST_PERSON);
			context.waitTicks(5);
			shot(context, "attune_first_person");
		} finally {
			context.getInput().releaseKey(options -> options.keyShift);
		}
		context.waitTicks(5);
	}

	// ------------------------------------------------------------------ advancements

	private static void advancements(ClientGameTestContext context, TestSingleplayerContext world) {
		for (String path : List.of("cords/echo", "casting/rhythm", "casting/long_cast", "heart/circle_4", "discovery/runes_50", "discovery/secret",
				"world/archive", "shields/glyph")) {
			attempt("grant " + path, () -> world.getServer().runCommand("advancement grant @a until wildercord:" + path));
		}
		context.waitTicks(40);
		context.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			var advancements = mc.player.connection.getAdvancements();
			var root = advancements.get(Wildercord.id("root"));
			if (root != null) {
				advancements.setSelectedTab(root, false);
			}
			mc.gui.setScreen(new AdvancementsScreen(advancements));
		});
		context.waitTicks(10);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(3);
		shot(context, "advancements_tab");
		// Closer, at a larger GUI scale.
		context.runOnClient(mc -> {
			mc.options.guiScale().set(4);
			mc.resizeGui();
		});
		context.waitTicks(5);
		shot(context, "advancements_tab_large");
		context.runOnClient(mc -> {
			mc.options.guiScale().set(2);
			mc.resizeGui();
			mc.gui.setScreen(null);
		});
		context.waitTicks(3);
	}

	// ------------------------------------------------------------------ screens

	private static void screens(ClientGameTestContext context, TestSingleplayerContext world) {
		// The Codex, searched for the new runes, with one pointed at.
		context.setScreen(CordScreen::new);
		context.waitTicks(10);
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		String[][] searches = {{"fusion", Runes.FIRESTORM.id()}, {"firestorm", Runes.FIRESTORM.id()}, {"tide", Runes.TIDECALL.id()}, {"star", Runes.STARSHARD.id()}};
		for (String[] search : searches) {
			attempt("codex_" + search[0], () -> {
				context.runOnClient(mc -> ((CordScreen) mc.gui.screen()).searchFor(search[0]));
				context.waitTicks(3);
				double[] cell = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).codexPoint(search[1]));
				if (cell != null) {
					context.getInput().setCursorPos(cell[0] * scale, cell[1] * scale);
				} else {
					context.getInput().setCursorPos(4, 4);
				}
				context.waitTicks(5);
				shot(context, "codex_" + search[0]);
			});
		}
		attempt("codex_grimoire", () -> {
			context.runOnClient(mc -> {
				CordScreen screen = (CordScreen) mc.gui.screen();
				screen.searchFor("");
				screen.showPage(2);
			});
			context.getInput().setCursorPos(4, 4);
			context.waitTicks(5);
			shot(context, "codex_grimoire");
		});
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitTicks(3);

		// Item tooltips: a ranked rune and a Knot, pointed at in the inventory.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			player.getInventory().clearContent();
			player.getInventory().setItem(0, RuneItem.stack(Runes.FIRE, 2));
			player.getInventory().setItem(1, RuneItem.stack(Knots.id(List.of(Runes.BOLT, Runes.FIRE, Runes.AMPLIFY, Runes.SPLIT_MOD), "Ember Fan")));
			player.getInventory().setItem(2, RuneItem.stack(Runes.FIRESTORM, 3));
			player.getInventory().setItem(3, RuneItem.stack(Runes.TIDECALL));
		});
		context.waitTicks(5);
		context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
		context.waitTicks(8);
		String[] names = {"tooltip_fire_ii", "tooltip_knot", "tooltip_firestorm_iii", "tooltip_tidecall"};
		for (int i = 0; i < names.length; i++) {
			int slot = i;
			attempt(names[i], () -> {
				double[] point = context.computeOnClient(mc -> hotbarPoint(mc.gui.screen(), mc.player.getInventory(), slot));
				if (point != null) {
					context.getInput().setCursorPos(point[0] * scale, point[1] * scale);
					context.waitTicks(4);
					shot(context, names[slot]);
				}
			});
		}
		context.runOnClient(mc -> mc.gui.setScreen(null));
		world.getServer().runOnServer(server -> {
			player(server).getInventory().clearContent();
			player(server).setGameMode(GameType.CREATIVE);
		});
		context.waitTicks(3);
	}

	/** The middle of hotbar slot {@code index} in an open container screen, in GUI coordinates. */
	private static double[] hotbarPoint(net.minecraft.client.gui.screens.Screen screen, net.minecraft.world.entity.player.Inventory inventory, int index) {
		if (!(screen instanceof AbstractContainerScreen<?> container)) {
			return null;
		}
		int left;
		int top;
		try {
			java.lang.reflect.Field leftPos = AbstractContainerScreen.class.getDeclaredField("leftPos");
			java.lang.reflect.Field topPos = AbstractContainerScreen.class.getDeclaredField("topPos");
			leftPos.setAccessible(true);
			topPos.setAccessible(true);
			left = leftPos.getInt(container);
			top = topPos.getInt(container);
		} catch (ReflectiveOperationException e) {
			left = (screen.width - 176) / 2;
			top = (screen.height - 166) / 2;
		}
		for (Slot slot : container.getMenu().slots) {
			if (slot.container == inventory && slot.getContainerSlot() == index) {
				return new double[] {left + slot.x + 8, top + slot.y + 8};
			}
		}
		return null;
	}

	/** The creative inventory, on the Wildercord tab. */
	private static void creativeTab(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> player(server).setGameMode(GameType.CREATIVE));
		context.waitTicks(5);
		context.runOnClient(mc -> {
			try {
				java.lang.reflect.Field selected = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
				selected.setAccessible(true);
				selected.set(null, WildercordItems.TAB);
			} catch (ReflectiveOperationException e) {
				LOG.warn("Couldn't pick the Wildercord creative tab", e);
			}
			mc.gui.setScreen(new InventoryScreen(mc.player));
		});
		context.waitTicks(10);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(3);
		shot(context, "creative_tab");
		context.runOnClient(mc -> {
			mc.options.guiScale().set(4);
			mc.resizeGui();
		});
		context.waitTicks(5);
		shot(context, "creative_tab_large");
		context.runOnClient(mc -> {
			mc.options.guiScale().set(2);
			mc.resizeGui();
			mc.gui.setScreen(null);
		});
		context.waitTicks(3);
	}
}
