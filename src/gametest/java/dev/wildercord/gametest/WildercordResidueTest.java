package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Climate;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.cast.PowerPlaces;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Residues;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.content.Reagents;
import dev.wildercord.content.ResidueBlock;
import dev.wildercord.content.ResidueBlocks;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.menu.FusionAltarMenu;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.ClimateRules;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.WovenRunes;
import dev.wildercord.world.LeyLines;
import dev.wildercord.world.ResidueRules;
import dev.wildercord.world.ResidueRules.Kind;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A world that remembers magic. In a real (superflat) world: a strong spell of each of the ten elements leaves its
 * residue on natural ground, a small one doesn't, and none lands on a player's planks, with residues switched off,
 * or inside a dungeon's ward; a reaction leaves one too; residues do what they say (everfrost slick, a void scar
 * drawing items in) and harvesting gives their reagents (everfrost giving the grass back, an eddy bottled); a
 * reagent at the Fusion Altar halves a weave's XP, another keeps its amethyst; a full moon and a thunderstorm change
 * a spell's damage, and a ley crossing makes spells stronger and cheaper. Then a real save and reload keeps every
 * residue, and moving their clock on fades them back into the ground, one waiting for its chunk to load too.
 *
 * <p>Screenshots every residue by day and by night ({@code residue_gallery_day}, {@code residue_gallery_night}), each
 * close up at night ({@code residue_<kind>}), the HUD saying why under a full moon ({@code residue_hud_full_moon}) and
 * a ley crossing's shimmer with its line ({@code residue_ley_crossing}).</p>
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordResidueTest implements FabricClientGameTest {
	/** One rune of each element, strong enough with a 60-mana weight to leave three blocks of residue. */
	private static final Map<Kind, RuneDef> BY_ELEMENT = new LinkedHashMap<>();

	static {
		BY_ELEMENT.put(Kind.SMOULDERING_ASH, Runes.FIRE);
		BY_ELEMENT.put(Kind.EVERFROST, Runes.FROST);
		BY_ELEMENT.put(Kind.FULGURITE, Runes.SHOCK);
		BY_ELEMENT.put(Kind.LINGERING_EDDY, Runes.PUSH);
		BY_ELEMENT.put(Kind.RIVEN_STONE, Runes.AFTERSHOCK);
		BY_ELEMENT.put(Kind.WILDBLOOM, Runes.VENOM);
		BY_ELEMENT.put(Kind.VOID_SCAR, Runes.PULL);
		BY_ELEMENT.put(Kind.STAR_GLYPH, Runes.HARM);
		BY_ELEMENT.put(Kind.STILLED_SAND, Runes.COUNTDOWN);
		BY_ELEMENT.put(Kind.BLOODMOSS, Runes.LEECH);
	}

	@FunctionalInterface
	private interface Check {
		String run(ClientGameTestContext context, TestSingleplayerContext world);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		List<String> failures = new ArrayList<>();
		TestWorldSave save;
		List<BlockPos> kept = new ArrayList<>();
		Path config = Config.path();
		String originalConfig = read(config);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule random_tick_speed 0");
			world.getServer().runCommand("gamerule fire_spread_radius_around_player 0");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				player.setGameMode(GameType.CREATIVE);
				Spellbook book = Spellbooks.get(player).withStarterGiven();
				for (RuneDef rune : Runes.all()) {
					book = book.learn(rune.id());
				}
				Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
				Spellbooks.set(player, book.withSpell(0, List.of(Runes.BOLT.id(), Runes.HARM.id())).withSelected(0));
				teleport(player, 0.5, 4, 0.5, 0, 30);
			});
			context.runOnClient(mc -> mc.getWindow().setWindowed(1280, 720));
			context.waitTicks(20);
			List<Object[]> checks = List.of(
				new Object[] {"A strong spell of each element", (Check) WildercordResidueTest::eachElement},
				new Object[] {"A small spell", (Check) WildercordResidueTest::smallSpell},
				new Object[] {"Never on what players built", (Check) WildercordResidueTest::neverOnPlanks},
				new Object[] {"The server's switch", (Check) (c, w) -> switchedOff(c, w, config)},
				new Object[] {"A dungeon's ward", (Check) WildercordResidueTest::ward},
				new Object[] {"A reaction", (Check) WildercordResidueTest::reaction},
				new Object[] {"What residues do", (Check) WildercordResidueTest::behaviours},
				new Object[] {"Harvesting", (Check) WildercordResidueTest::harvesting},
				new Object[] {"Reagents at the altar", (Check) WildercordResidueTest::altar},
				new Object[] {"The moon and the storm", (Check) WildercordResidueTest::moonAndStorm},
				new Object[] {"A ley crossing", (Check) WildercordResidueTest::leyCrossing},
				new Object[] {"Screenshots", (Check) WildercordResidueTest::gallery});
			for (Object[] check : checks) {
				String failure;
				try {
					failure = ((Check) check[1]).run(context, world);
				} catch (RuntimeException | AssertionError e) {
					failure = "threw " + e;
				} finally {
					world.getServer().runOnServer(server -> {
						Residues.clearRests();
						Residues.reactionChance(-1);
					});
				}
				if (failure != null) {
					failures.add(check[0] + ": " + failure);
				}
			}
			// Kept for the save: every residue left near the start, and where.
			kept.addAll(world.getServer().computeOnServer(server -> Residues.all(player(server).level())));
			if (kept.size() < 10) {
				failures.add("Save: expected the residues left above to still stand before the save (" + kept.size() + ")");
			}
			save = world.getWorldSave();
		} finally {
			write(config, originalConfig);
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
		}
		try (TestSingleplayerContext again = save.open()) {
			context.waitTicks(40);
			String after = afterTheSave(context, again, kept);
			if (after != null) {
				failures.add("After the save: " + after);
			}
		}
		if (!failures.isEmpty()) {
			throw new AssertionError("Residues and places of power went wrong:\n  " + String.join("\n  ", failures));
		}
	}

	// ------------------------------------------------------------------ the checks

	/** A 60-mana spell of each element, cast at the grass, leaves that element's residue round where it landed. */
	private static String eachElement(ClientGameTestContext context, TestSingleplayerContext world) {
		String left = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			int i = 0;
			for (Map.Entry<Kind, RuneDef> entry : BY_ELEMENT.entrySet()) {
				Residues.clearRests();
				BlockPos ground = ground(level, -45 + i * 10, 40);
				i++;
				Effects.apply(new Cast(player).weigh(60), node(entry.getValue()), hitOn(player, ground));
				int found = count(level, ground, 3, entry.getKey());
				if (found == 0) {
					return entry.getValue().name() + " (" + entry.getKey().element + ") left no " + entry.getKey().path + " round " + ground.toShortString();
				}
				if (found > ResidueRules.cells(2.0)) {
					return entry.getKey().path + ": a strength 2 spell should leave at most " + ResidueRules.cells(2.0) + " blocks (" + found + ")";
				}
			}
			return null;
		});
		context.waitTicks(5);
		return left;
	}

	/** A 12-mana spell leaves nothing. */
	private static String smallSpell(ClientGameTestContext context, TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			BlockPos ground = ground(player.level(), 60, 40);
			Effects.apply(new Cast(player).weigh(12), node(Runes.FIRE), hitOn(player, ground));
			return count(player.level(), ground, 4, null) == 0 ? null : "a small spell shouldn't leave a residue";
		});
	}

	/** A strong spell cast at a floor of planks leaves nothing on it, nor on the cobblestone beside. */
	private static String neverOnPlanks(ClientGameTestContext context, TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			BlockPos centre = ground(level, 0, 70);
			for (int dx = -4; dx <= 4; dx++) {
				for (int dz = -4; dz <= 4; dz++) {
					level.setBlockAndUpdate(centre.offset(dx, 0, dz), (dx + dz) % 3 == 0 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState());
				}
			}
			for (Kind kind : List.of(Kind.EVERFROST, Kind.VOID_SCAR, Kind.RIVEN_STONE, Kind.FULGURITE)) {
				Residues.clearRests();
				Effects.apply(new Cast(player).weigh(90), node(BY_ELEMENT.get(kind)), hitOn(player, centre));
			}
			for (int dx = -4; dx <= 4; dx++) {
				for (int dz = -4; dz <= 4; dz++) {
					BlockState floor = level.getBlockState(centre.offset(dx, 0, dz));
					if (!floor.is(Blocks.OAK_PLANKS) && !floor.is(Blocks.COBBLESTONE)) {
						return "a residue took a built floor at " + centre.offset(dx, 0, dz).toShortString() + " (" + floor + ")";
					}
				}
			}
			return count(level, centre, 4, null) == 0 ? null : "a residue was left on the planks";
		});
	}

	/** With residues switched off (and reloaded), a strong spell leaves nothing; back on, it does again. */
	private static String switchedOff(ClientGameTestContext context, TestSingleplayerContext world, Path config) {
		String off = WildercordConfig.DEFAULTS.toJson().replaceFirst("(\"residues\": \\{[^}]*?\"enabled\": )true", "$1false");
		if (off.equals(WildercordConfig.DEFAULTS.toJson())) {
			return "the default file should list residues.enabled (test setup)";
		}
		write(config, off);
		world.getServer().runCommand("wildercord reload");
		context.waitTicks(2);
		String none = world.getServer().computeOnServer(server -> {
			if (Config.get().residues().enabled()) {
				return "/wildercord reload should read residues.enabled: false";
			}
			ServerPlayer player = player(server);
			BlockPos ground = ground(player.level(), 0, 100);
			Effects.apply(new Cast(player).weigh(90), node(Runes.FROST), hitOn(player, ground));
			return count(player.level(), ground, 4, null) == 0 ? null : "with residues off, a strong spell still left one";
		});
		write(config, WildercordConfig.DEFAULTS.toJson());
		world.getServer().runCommand("wildercord reload");
		context.waitTicks(2);
		if (none != null) {
			return none;
		}
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Residues.clearRests();
			BlockPos ground = ground(player.level(), 10, 100);
			Effects.apply(new Cast(player).weigh(90), node(Runes.FROST), hitOn(player, ground));
			return count(player.level(), ground, 4, Kind.EVERFROST) > 0 ? null : "switched back on, a strong spell should leave everfrost again";
		});
	}

	/** Inside a dungeon's ward (filed as a dungeon piece files its arena), a strong spell leaves nothing. */
	private static String ward(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			BlockPos ground = ground(player(server).level(), -40, 100);
			BoundingBox room = new BoundingBox(ground.getX() - 8, ground.getY() - 4, ground.getZ() - 8, ground.getX() + 8, ground.getY() + 10, ground.getZ() + 8);
			DungeonWards.remember(player(server).level(), () -> List.of(room));
		});
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			BlockPos ground = ground(player.level(), -40, 100);
			if (!DungeonWards.warded(player.level(), ground.above())) {
				return "the ward should be filed (test setup)";
			}
			Effects.apply(new Cast(player).weigh(90), node(Runes.SHOCK), hitOn(player, ground));
			return count(player.level(), ground, 4, null) == 0 ? null : "a residue was left inside a ward";
		});
	}

	/** Fire on a frozen husk sets off Shatter, and (its chance pinned) the reaction leaves everfrost where it stood. */
	private static String reaction(ClientGameTestContext context, TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Residues.reactionChance(1.0);
			BlockPos ground = ground(level, 40, 70);
			Husk husk = husk(level, Vec3.atBottomCenterOf(ground.above()));
			Reactions.mark(husk, Reactions.Mark.FROZEN, 100);
			Effects.apply(new Cast(player), node(Runes.FIRE), hitEntity(player, husk));
			husk.discard();
			return count(level, ground, 2, Kind.EVERFROST) > 0 ? null : "Shatter should have left everfrost (a small spell's reaction, its chance pinned)";
		});
	}

	/** Everfrost is slick, a void scar draws a loose item in and closes as it ages, and a residue is left alone by other spells. */
	private static String behaviours(ClientGameTestContext context, TestSingleplayerContext world) {
		String setup = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			BlockPos everfrost = firstOf(level, Kind.EVERFROST);
			if (everfrost == null) {
				return "no everfrost to stand on (left by the first check)";
			}
			if (level.getBlockState(everfrost).getBlock().getFriction() < 0.95F) {
				return "everfrost should be slick as ice (friction " + level.getBlockState(everfrost).getBlock().getFriction() + ")";
			}
			if (!Effects.isTemporary(level, everfrost)) {
				return "other spells should leave a residue alone (it counts as a spell's passing block)";
			}
			Residues.clearRests();
			BlockPos ground = ground(level, 70, 70);
			if (Residues.leave(level, "void", Vec3.atCenterOf(ground), 1.0, player) != 1) {
				return "the residue hook should leave one block at strength 1";
			}
			BlockPos scar = firstOf(level, Kind.VOID_SCAR, ground, 3);
			if (scar == null) {
				return "no void scar where the hook left one";
			}
			ItemEntity loose = new ItemEntity(level, scar.getX() + 3.5, scar.getY() + 1.1, scar.getZ() + 0.5, new ItemStack(Items.STICK));
			loose.setPickUpDelay(400);
			level.addFreshEntity(loose);
			return null;
		});
		if (setup != null) {
			return setup;
		}
		context.waitTicks(40);
		String pulled = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			BlockPos scar = firstOf(level, Kind.VOID_SCAR, ground(level, 70, 70), 3);
			List<ItemEntity> sticks = level.getEntitiesOfClass(ItemEntity.class, new AABB(scar).inflate(6), e -> e.getItem().is(Items.STICK));
			if (sticks.isEmpty()) {
				return "the stick by the void scar vanished";
			}
			double distance = sticks.getFirst().position().distanceTo(Vec3.atCenterOf(scar));
			sticks.forEach(ItemEntity::discard);
			if (distance > 2.6) {
				return "a void scar should draw a loose item in (still " + distance + " blocks off)";
			}
			// As it ages it narrows.
			Residues.fastForward(level, 4000);
			return null;
		});
		return pulled != null ? pulled : closes(context, world);
	}

	private static String closes(ClientGameTestContext context, TestSingleplayerContext world) {
		context.waitTicks(15);
		return world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			BlockPos scar = firstOf(level, Kind.VOID_SCAR, ground(level, 70, 70), 3);
			if (scar == null) {
				return "the void scar faded too early";
			}
			int stage = level.getBlockState(scar).getValue(ResidueBlock.STAGE);
			// Put the clock back (the rest of the test wants the residues fresh).
			Residues.fastForward(level, -4000);
			return stage >= 2 ? null : "two thirds through its life a void scar should have narrowed (stage " + stage + ")";
		});
	}

	/** Breaking everfrost gives an Everfrost Shard and the grass back; a wildbloom a petal; a bottle takes an eddy as a Bottled Gale. */
	private static String harvesting(ClientGameTestContext context, TestSingleplayerContext world) {
		String broke = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			player.setGameMode(GameType.SURVIVAL);
			BlockPos frost = firstOf(level, Kind.EVERFROST);
			BlockPos bloom = firstOf(level, Kind.WILDBLOOM);
			BlockPos eddy = firstOf(level, Kind.LINGERING_EDDY);
			if (frost == null || bloom == null || eddy == null) {
				return "the first check should have left everfrost, a wildbloom and an eddy";
			}
			if (!player.gameMode.destroyBlock(frost) || !player.gameMode.destroyBlock(bloom)) {
				player.setGameMode(GameType.CREATIVE);
				return "a survival player should be able to harvest residues";
			}
			if (!level.getBlockState(frost).is(Blocks.GRASS_BLOCK)) {
				return "harvested everfrost should give the grass back (" + level.getBlockState(frost) + ")";
			}
			if (Residues.isResidue(level, frost) || Residues.isResidue(level, bloom)) {
				return "a harvested residue's record should go";
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
			player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(eddy), Direction.UP, eddy, false));
			boolean bottled = player.getMainHandItem().is(Reagents.BOTTLED_GALE) || player.getInventory().contains(new ItemStack(Reagents.BOTTLED_GALE));
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setGameMode(GameType.CREATIVE);
			if (!bottled) {
				return "a glass bottle used on an eddy should give a Bottled Gale";
			}
			return level.getBlockState(eddy).isAir() ? null : "the bottled eddy should be gone";
		});
		if (broke != null) {
			return broke;
		}
		context.waitTicks(5);
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.CREATIVE);
			boolean shard = false;
			boolean petal = false;
			for (ItemEntity drop : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(80))) {
				shard |= drop.getItem().is(Reagents.EVERFROST_SHARD);
				petal |= drop.getItem().is(Reagents.WILDBLOOM_PETAL);
			}
			for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
				shard |= stack.is(Reagents.EVERFROST_SHARD);
				petal |= stack.is(Reagents.WILDBLOOM_PETAL);
			}
			if (!shard || !petal) {
				return "harvesting should give an Everfrost Shard (" + shard + ") and a Wildbloom Petal (" + petal + ")";
			}
			return Reagents.is(new ItemStack(Reagents.HOLLOW_DUST)) && new ItemStack(Reagents.STAR_DUST).is(Reagents.TAG) ? null
				: "every reagent should be in #wildercord:reagents";
		});
	}

	/** An Everfrost Shard halves a weave's XP and is used up with it; Geode Grit keeps the amethyst block on the altar. */
	private static String altar(ClientGameTestContext context, TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			player.setGameMode(GameType.SURVIVAL);
			try {
				BlockPos at = player.blockPosition().offset(2, 0, 0);
				level.setBlockAndUpdate(at, WildercordBlocks.FUSION_ALTAR.defaultBlockState());
				FusionAltarMenu menu = new FusionAltarMenu(0, player.getInventory(), ContainerLevelAccess.create(level, at));
				RuneDef pair = WovenRunes.bind(Runes.FIRE, Runes.HEAL);
				menu.getSlot(0).set(RuneItem.stack(pair));
				menu.getSlot(1).set(RuneItem.stack(Runes.SHOCK));
				menu.getSlot(3).set(new ItemStack(Items.AMETHYST_BLOCK));
				int plain = menu.plan().xp();
				// The first fusion's feat (and its advancement's XP reward) earned beforehand, so the price can be read off the levels.
				dev.wildercord.cast.Grimoire.feat(player, dev.wildercord.spell.Feats.COMBINE);
				menu.getSlot(2).set(new ItemStack(Reagents.EVERFROST_SHARD));
				int stilled = menu.plan().xp();
				if (plain != 6 || stilled != 3) {
					return "an Everfrost Shard should halve a three-effect weave's 6 XP levels to 3 (" + plain + " then " + stilled + ")";
				}
				player.experienceLevel = 10;
				player.experienceProgress = 0;
				if (!menu.clickMenuButton(player, FusionAltarMenu.BUTTON_FUSE)) {
					return "the altar should weave with a reagent on it";
				}
				if (player.experienceLevel != 7) {
					return "the stilled weave should cost 3 levels (left " + player.experienceLevel + " of 10)";
				}
				RuneDef made = RuneItem.runeOf(menu.getSlot(FusionAltarMenu.RESULT).getItem()).orElse(null);
				if (made == null || WovenRunes.contents(made).size() != 3) {
					return "the weave should hold all three effects";
				}
				if (!menu.getSlot(2).getItem().isEmpty() || !menu.getSlot(3).getItem().isEmpty()) {
					return "the shard and the amethyst should both be used up";
				}
				menu.getSlot(FusionAltarMenu.RESULT).set(ItemStack.EMPTY);
				menu.getSlot(0).set(RuneItem.stack(Runes.FIRE));
				menu.getSlot(1).set(RuneItem.stack(Runes.FROST));
				menu.getSlot(2).set(new ItemStack(Reagents.GEODE_GRIT));
				menu.getSlot(3).set(new ItemStack(Items.AMETHYST_SHARD));
				if (!menu.clickMenuButton(player, FusionAltarMenu.BUTTON_FUSE)) {
					return "the altar should fuse with Geode Grit on it";
				}
				if (!menu.getSlot(3).getItem().is(Items.AMETHYST_SHARD) || !menu.getSlot(2).getItem().isEmpty()) {
					return "Geode Grit should keep the amethyst on the altar, and be used up itself";
				}
				return RuneItem.runeOf(menu.getSlot(FusionAltarMenu.RESULT).getItem()).map(r -> r == Runes.STEAM).orElse(false) ? null
					: "Fire and Frost should still make Steam";
			} finally {
				player.setGameMode(GameType.CREATIVE);
				player.getInventory().clearContent();
			}
		});
	}

	/** A full moon strengthens arcane (Harm) about 15% over a quarter moon; a thunderstorm strengthens storm (Shock) about 25%. */
	private static String moonAndStorm(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		context.waitTicks(25);
		String full = world.getServer().computeOnServer(server -> Climate.conditions(player(server)).contains(ClimateRules.Condition.FULL_MOON) ? null
			: "the first night should be a full moon (conditions " + Climate.conditions(player(server)) + ")");
		if (full != null) {
			return full;
		}
		// The HUD says why, for a few seconds after the moon rose.
		context.runOnClient(mc -> {
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
			mc.options.setCameraType(CameraType.FIRST_PERSON);
		});
		context.waitTicks(10);
		shot(context, "residue_hud_full_moon");
		double fullMoon = damage(world, Runes.HARM);
		world.getServer().runCommand("time set 66000");
		context.waitTicks(25);
		double quarter = damage(world, Runes.HARM);
		if (quarter <= 0 || Math.abs(fullMoon / quarter - 1.15) > 0.02) {
			return "a full moon should make Harm about 15% stronger (" + fullMoon + " against " + quarter + ")";
		}
		world.getServer().runCommand("time set 6000");
		context.waitTicks(25);
		double clear = damage(world, Runes.SHOCK);
		world.getServer().runCommand("weather thunder");
		// The sky darkens over a few seconds before it counts as a thunderstorm (as it clears over a few after).
		context.waitTicks(120);
		double stormFactor = world.getServer().computeOnServer(server -> Climate.factor(player(server), "storm"));
		double storm = damage(world, Runes.SHOCK);
		world.getServer().runCommand("weather clear");
		context.waitTicks(120);
		if (Math.abs(stormFactor - 1.25) > 1e-6) {
			return "a thunderstorm overhead should make storm 25% stronger (factor " + stormFactor + ")";
		}
		if (clear <= 0 || storm < clear * 1.2) {
			return "a thunderstorm should make Shock measurably stronger (" + storm + " against " + clear + ")";
		}
		return null;
	}

	/** At a ley crossing a spell hits 10% harder and costs 10% less, and the HUD and the ground say so. */
	private static String leyCrossing(ClientGameTestContext context, TestSingleplayerContext world) {
		double[] heart = world.getServer().computeOnServer(server -> strongCrossing(LeyWalker.seed(player(server).level())));
		if (heart == null) {
			return "no ley crossing within 3000 blocks of spawn";
		}
		double off = damageAt(context, world, heart[0] + 40, heart[1] + 40, Runes.FIRE);
		int offCost = world.getServer().computeOnServer(server -> Heart.manaCost(player(server), SpellCompiler.compile(List.of(Runes.BURST, Runes.EXPLODE))));
		double on = damageAt(context, world, heart[0], heart[1], Runes.FIRE);
		String state = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			if (!LeyWalker.atCrossing(player) || !Climate.conditions(player).contains(ClimateRules.Condition.LEY_CROSSING)) {
				return "standing at the crossing's heart should count as a ley crossing";
			}
			if (!PowerPlaces.isPlaceOfPower(player.level(), player.blockPosition()) || !PowerPlaces.at(player.level(), player.blockPosition()).placeOfPower()) {
				return "the place-of-power hook should agree it's a ley crossing";
			}
			// Blood, which nothing else here (the noon sun, the deep) touches.
			double blood = PowerPlaces.of(player).factor("blood");
			return Math.abs(blood - 1.10) < 1e-6 ? null : "every element should be 10% stronger here (blood " + blood + ")";
		});
		if (state != null) {
			return state;
		}
		int onCost = world.getServer().computeOnServer(server -> Heart.manaCost(player(server), SpellCompiler.compile(List.of(Runes.BURST, Runes.EXPLODE))));
		if (off <= 0 || Math.abs(on / off - 1.10) > 0.02) {
			return "at a ley crossing Fire should hit 10% harder (" + on + " against " + off + ")";
		}
		if (onCost >= offCost || onCost != (int) Math.ceil(offCost * 0.9 - 1e-9) && onCost != offCost - 1) {
			return "at a ley crossing a spell should cost 10% less (" + onCost + " against " + offCost + ")";
		}
		// Hovering over the heart (still on the crossing, so the HUD's line stays), looking down on its rings and light.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			// As far back as the crossing still reaches (it's only a few blocks across), for a better view of its rings.
			long seed = LeyWalker.seed(player.level());
			double back = 0;
			for (double d = 2.0; d > 0; d -= 0.25) {
				if (LeyLines.atCrossing(seed, heart[0], heart[1] - d)) {
					back = d;
					break;
				}
			}
			teleport(player, heart[0], player.getY() + 4, heart[1] - back, 0, back > 1 ? 62 : 72);
		});
		context.waitTicks(30);
		shot(context, "residue_ley_crossing");
		world.getServer().runOnServer(server -> teleport(player(server), 0.5, 4, 0.5, 0, 30));
		context.waitTicks(40);
		return null;
	}

	/** One of each residue, by day and by night, and each close up at night. */
	private static String gallery(ClientGameTestContext context, TestSingleplayerContext world) {
		String laid = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			int i = 0;
			for (Kind kind : Kind.values()) {
				Residues.clearRests();
				BlockPos spot = ground(level, -12 + (i % 5) * 6, 130 + (i / 5) * 6);
				i++;
				if (Residues.leave(level, kind.element, Vec3.atCenterOf(spot), 1.0, player) != 1) {
					return "the hook should lay one " + kind.path + " for the gallery";
				}
			}
			return null;
		});
		if (laid != null) {
			return laid;
		}
		context.runOnClient(mc -> {
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			teleport(player, 0.5, ground(player.level(), 0, 118).getY() + 8, 118.5, 0, 34);
		});
		world.getServer().runCommand("time set 6000");
		context.waitTicks(40);
		shot(context, "residue_gallery_day");
		world.getServer().runCommand("time set 18000");
		context.waitTicks(30);
		shot(context, "residue_gallery_night");
		int i = 0;
		for (Kind kind : Kind.values()) {
			int x = -12 + (i % 5) * 6;
			int z = 130 + (i / 5) * 6;
			i++;
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				teleport(player, x + 0.5, ground(player.level(), x, z).getY() + 1.9, z - 1.3, 0, 50);
			});
			context.waitTicks(12);
			shot(context, "residue_" + kind.path);
		}
		world.getServer().runCommand("time set 6000");
		context.runOnClient(mc -> {
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		world.getServer().runOnServer(server -> teleport(player(server), 0.5, 4, 0.5, 0, 30));
		context.waitTicks(10);
		return null;
	}

	/** After a real save and reload: every residue still there, with its record; moved on in time they fade, one waiting for its chunk. */
	private static String afterTheSave(ClientGameTestContext context, TestSingleplayerContext world, List<BlockPos> kept) {
		world.getServer().runCommand("gamerule random_tick_speed 0");
		String still = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			if (Residues.count(level) != kept.size()) {
				return "the save should keep every residue's record (" + Residues.count(level) + " of " + kept.size() + ")";
			}
			for (BlockPos pos : kept) {
				if (level.isLoaded(pos) && !ResidueBlocks.is(level.getBlockState(pos))) {
					return "the residue at " + pos.toShortString() + " should still stand after the reload";
				}
			}
			return null;
		});
		if (still != null) {
			return still;
		}
		// One far off, in ground that won't stay loaded: it should wait for its chunk.
		world.getServer().runOnServer(server -> teleport(player(server), 3000.5, 4, 3000.5, 0, 30));
		context.waitTicks(40);
		BlockPos distant = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			BlockPos ground = ground(player.level(), 3000, 3010);
			Residues.clearRests();
			return Residues.leave(player.level(), "frost", Vec3.atCenterOf(ground), 1.0, player) == 1 ? firstOf(player.level(), Kind.EVERFROST, ground, 3) : null;
		});
		if (distant == null) {
			return "couldn't leave everfrost far off (test setup)";
		}
		world.getServer().runOnServer(server -> teleport(player(server), 0.5, 4, 0.5, 0, 30));
		try {
			world.getServer().waitFor(server -> player(server).level().getChunkSource()
				.getChunkNow(distant.getX() >> 4, distant.getZ() >> 4) == null);
		} catch (AssertionError timeout) {
			return world.getServer().computeOnServer(server -> "the far chunk should become inaccessible before expiry (test setup; "
				+ residueState(server, distant) + ")");
		}
		world.getServer().runOnServer(server -> Residues.fastForward(player(server).level(), 200_000));
		context.waitTicks(45);
		String faded = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			for (BlockPos pos : kept) {
				if (!level.isLoaded(pos)) {
					continue;
				}
				BlockState now = level.getBlockState(pos);
				if (ResidueBlocks.is(now)) {
					return "moved on past its time, the residue at " + pos.toShortString() + " should have faded";
				}
				if (Residues.isResidue(level, pos)) {
					return "a faded residue's record should go";
				}
			}
			if (!Residues.isResidue(level, distant)) {
				return "the far residue, its chunk unloaded, should wait for it rather than go unseen";
			}
			return null;
		});
		if (faded != null) {
			return faded;
		}
		// The everfrost near the start gave its grass back.
		String grass = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			BlockPos ground = ground(level, -45 + 10, 40);
			return level.getBlockState(ground).is(Blocks.GRASS_BLOCK) ? null : "faded everfrost should give the grass back (" + level.getBlockState(ground) + ")";
		});
		if (grass != null) {
			return grass;
		}
		world.getServer().runOnServer(server -> teleport(player(server), distant.getX() + 0.5, 4, distant.getZ() - 4.5, 0, 30));
		try {
			// A block-state read could itself load the chunk. Establish readiness without doing so first.
			world.getServer().waitFor(server -> player(server).level().getChunkSource()
				.getChunkNow(distant.getX() >> 4, distant.getZ() >> 4) != null);
			int accessibleAt = world.getServer().computeOnServer(MinecraftServer::getTickCount);
			// Give the decay schedule one actual server sweep after the full-chunk future completes.
			world.getServer().waitFor(server -> server.getTickCount() >= accessibleAt + 20);
		} catch (AssertionError timeout) {
			return world.getServer().computeOnServer(server -> "the far chunk should become accessible and reach a decay sweep ("
				+ residueState(server, distant) + ")");
		}
		return world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			var chunk = level.getChunkSource().getChunkNow(distant.getX() >> 4, distant.getZ() >> 4);
			if (chunk == null) {
				return "the far chunk became inaccessible before the decay check (" + residueState(server, distant) + ")";
			}
			BlockState now = chunk.getBlockState(distant);
			if (ResidueBlocks.is(now) || Residues.isResidue(level, distant)) {
				return "the far everfrost should fade once its chunk loads (" + residueState(server, distant) + ")";
			}
			return now.is(Blocks.GRASS_BLOCK) ? null : "and give its grass back (" + residueState(server, distant) + ")";
		});
	}

	/** Failure detail that never loads the chunk it describes. */
	private static String residueState(MinecraftServer server, BlockPos pos) {
		ServerLevel level = player(server).level();
		var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
		return "pos=" + pos.toShortString() + ", serverTick=" + server.getTickCount() + ", gameTime=" + level.getGameTime()
			+ ", accessible=" + (chunk != null) + ", isLoaded=" + level.isLoaded(pos) + ", record=" + Residues.isResidue(level, pos)
			+ ", block=" + (chunk == null ? "unavailable" : chunk.getBlockState(pos));
	}

	// ------------------------------------------------------------------ helpers

	/** Fire's (or any rune's) damage to a fresh, sheltered husk three blocks off, cast by the player where they stand. */
	private static double damage(TestSingleplayerContext world, RuneDef rune) {
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			BlockPos ground = ground(level, (int) Math.floor(player.getX()) + 3, (int) Math.floor(player.getZ()));
			// A roof over it, so rain never leaves it wet (wet storm conducts) while the caster stays under the sky.
			level.setBlockAndUpdate(ground.above(3), Blocks.GLASS.defaultBlockState());
			Husk husk = husk(level, Vec3.atBottomCenterOf(ground.above()));
			float before = husk.getHealth();
			Effects.apply(new Cast(player), node(rune), hitEntity(player, husk));
			float after = husk.getHealth();
			husk.discard();
			level.setBlockAndUpdate(ground.above(3), Blocks.AIR.defaultBlockState());
			return (double) (before - after);
		});
	}

	/** Damage with the player standing at (x, z) on the ground there, once its chunks have loaded and the climate has caught up. */
	private static double damageAt(ClientGameTestContext context, TestSingleplayerContext world, double x, double z, RuneDef rune) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			BlockPos ground = ground(player.level(), (int) Math.floor(x), (int) Math.floor(z));
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
			teleport(player, x, ground.getY() + 1, z, 0, 30);
		});
		context.waitTicks(50);
		return damage(world, rune);
	}

	/** The nearest ley crossing to spawn whose heart runs strongly (so a block or two off it still counts), or null. */
	private static double[] strongCrossing(long seed) {
		for (int ring = 0; ring <= 200; ring++) {
			for (int ix = -ring; ix <= ring; ix++) {
				for (int iz = -ring; iz <= ring; iz++) {
					if (Math.max(Math.abs(ix), Math.abs(iz)) != ring) {
						continue;
					}
					double[] heart = LeyLines.crossingIn(seed, ix, iz);
					if (heart != null && heart[2] >= 0.6) {
						return heart;
					}
				}
			}
		}
		return null;
	}

	private static Husk husk(ServerLevel level, Vec3 at) {
		Husk husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		husk.setNoAi(true);
		husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
		husk.setHealth(200);
		husk.snapTo(at.x, at.y, at.z, 180, 0);
		level.addFreshEntity(husk);
		return husk;
	}

	private static SpellPlan.EffectNode node(RuneDef rune) {
		return SpellCompiler.compile(List.of(Runes.BEAM, rune)).root().groups.getFirst().effects.getFirst();
	}

	private static Cast.Hit hitOn(ServerPlayer player, BlockPos ground) {
		return new Cast.Hit(List.of(), Vec3.atCenterOf(ground.above()), new Vec3(0, 0, 1), player.position(), ground, Direction.UP, false);
	}

	private static Cast.Hit hitEntity(ServerPlayer player, Husk husk) {
		return new Cast.Hit(List.of(husk), husk.getBoundingBox().getCenter(), new Vec3(1, 0, 0), player.getEyePosition(), null, null, false);
	}

	/** The top block of the ground at a column (loaded for it). */
	private static BlockPos ground(ServerLevel level, int x, int z) {
		level.getChunk(x >> 4, z >> 4);
		return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
	}

	/** Residue blocks (of {@code kind}, or any) within {@code radius} of a spot. */
	private static int count(ServerLevel level, BlockPos at, int radius, Kind kind) {
		int n = 0;
		for (BlockPos pos : Residues.all(level)) {
			if (Math.abs(pos.getX() - at.getX()) <= radius && Math.abs(pos.getZ() - at.getZ()) <= radius && Math.abs(pos.getY() - at.getY()) <= 3
					&& (kind == null || ResidueBlocks.kindOf(level.getBlockState(pos)) == kind)) {
				n++;
			}
		}
		return n;
	}

	private static BlockPos firstOf(ServerLevel level, Kind kind) {
		for (BlockPos pos : Residues.all(level)) {
			if (ResidueBlocks.kindOf(level.getBlockState(pos)) == kind) {
				return pos;
			}
		}
		return null;
	}

	private static BlockPos firstOf(ServerLevel level, Kind kind, BlockPos near, int radius) {
		for (BlockPos pos : Residues.all(level)) {
			if (pos.closerThan(near, radius + 2) && ResidueBlocks.kindOf(level.getBlockState(pos)) == kind) {
				return pos;
			}
		}
		return null;
	}

	private static void teleport(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
		player.teleportTo(player.level(), x, y, z, Set.<Relative>of(), yaw, pitch, false);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		context.waitTicks(1);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
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
			if (text == null) {
				Files.deleteIfExists(path);
				return;
			}
			Files.createDirectories(path.getParent());
			Files.writeString(path, text, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AssertionError("couldn't write " + path + ": " + e);
		}
	}
}
