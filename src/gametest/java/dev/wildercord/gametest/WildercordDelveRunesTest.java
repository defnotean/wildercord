package dev.wildercord.gametest;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.TemporaryBlocks;
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
import net.minecraft.world.Container;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The delving pack (fx-mine) in a real game, on a stone platform high in the air: a Plumb Line digs down and stops a
 * block above the drop, Silklift lifts stone whole, Luckstrike breaks an ore for its drops, Polish, Brickwork and
 * Agestone dress stone in place, Concrete Set hardens powder, Kiln Bake bakes cobblestone to stone, Lava Seal turns a
 * lava pool to obsidian, Lever Flip throws a lever, Doorcall opens a wooden door and leaves an iron one shut, Chest Sort
 * joins and orders a chest, Stow moves only what the chest already holds and never the hotbar, Block Pack and Millstone
 * change items without making any, Floorlay lays a floor from the pack one for one, and Pit Floor sets a passing floor.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordDelveRunesTest implements FabricClientGameTest {
	/** The platform: high enough that no terrain gets in the way. */
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	/** The block a Ray from the caster strikes: head height, four blocks south. */
	private static final BlockPos AIM = STAGE.offset(0, 1, 4);

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
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			List<Object[]> checks = List.of(
				new Object[] {"Plumb Line", (Check) WildercordDelveRunesTest::plumbline},
				new Object[] {"Silklift", (Check) WildercordDelveRunesTest::silklift},
				new Object[] {"Luckstrike", (Check) WildercordDelveRunesTest::luckstrike},
				new Object[] {"Polish", (Check) WildercordDelveRunesTest::polish},
				new Object[] {"Brickwork", (Check) WildercordDelveRunesTest::brickwork},
				new Object[] {"Agestone", (Check) WildercordDelveRunesTest::agestone},
				new Object[] {"Concrete Set", (Check) WildercordDelveRunesTest::concreteset},
				new Object[] {"Kiln Bake", (Check) WildercordDelveRunesTest::kilnbake},
				new Object[] {"Lava Seal", (Check) WildercordDelveRunesTest::lavaseal},
				new Object[] {"Lever Flip", (Check) WildercordDelveRunesTest::leverflip},
				new Object[] {"Doorcall", (Check) WildercordDelveRunesTest::doorcall},
				new Object[] {"Chest Sort", (Check) WildercordDelveRunesTest::chestsort},
				new Object[] {"Stow", (Check) WildercordDelveRunesTest::stow},
				new Object[] {"Block Pack", (Check) WildercordDelveRunesTest::blockpack},
				new Object[] {"Millstone", (Check) WildercordDelveRunesTest::millstone},
				new Object[] {"Floorlay", (Check) WildercordDelveRunesTest::floorlay},
				// Last: its passing blocks stay written down until they go.
				new Object[] {"Pit Floor", (Check) WildercordDelveRunesTest::pitfloor});
			for (Object[] check : checks) {
				String failure;
				try {
					failure = ((Check) check[1]).run(context, world);
				} catch (RuntimeException e) {
					failure = "threw " + e;
				} finally {
					cleanup(context, world);
				}
				if (failure != null) {
					failures.add(check[0] + ": " + failure);
				}
			}
			if (!failures.isEmpty()) {
				throw new AssertionError("The delving runes went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	// ------------------------------------------------------------------ the stage

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void floor(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 10) + " " + (y - 2) + " " + (z - 10) + " " + (x + 10) + " " + (y - 2) + " " + (z + 10) + " minecraft:air");
		world.getServer().runCommand("fill " + (x - 10) + " " + (y - 1) + " " + (z - 10) + " " + (x + 10) + " " + (y - 1) + " " + (z + 10) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 10) + " " + y + " " + (z - 10) + " " + (x + 10) + " " + (y + 8) + " " + (z + 10) + " minecraft:air");
	}

	/** A 21x21 stone floor with open air above it, an Echo Cord, every rune known, and plenty of mana. */
	private static void stage(TestSingleplayerContext world) {
		floor(world);
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

	/** The caster at the middle of the platform, facing south (+Z) and a little down. */
	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.clearFire();
	}

	/** Threads {@code runes} as spell 1 and casts it. Returns what went wrong, or null. */
	private static String cast(ServerPlayer player, RuneDef... runes) {
		List<String> ids = java.util.Arrays.stream(runes).map(RuneDef::id).toList();
		SpellCaster.edit(player, 0, List.of());
		SpellCaster.edit(player, 0, ids);
		if (!Spellbooks.get(player).spells().getFirst().equals(ids)) {
			return "the spell didn't thread (has " + Spellbooks.get(player).spells().getFirst() + ")";
		}
		Spellbooks.setReadyAt(player, 0, 0);
		Spellbooks.setMana(player, Mana.max(player));
		player.setAttached(WildercordAttachments.RHYTHM, WildercordAttachments.Rhythm.NONE);
		long now = player.level().getGameTime();
		SpellCaster.cast(player, 0);
		return Spellbooks.readyAt(player, 0) > now ? null : "it didn't cast";
	}

	/** Sets the blocks (pos, state pairs), then casts a Ray of {@code rune} at {@link #AIM}, and waits for it to land. */
	private static String rayAt(ClientGameTestContext context, TestSingleplayerContext world, RuneDef rune, Object... blocks) {
		String cast = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			for (int i = 0; i < blocks.length; i += 2) {
				level.setBlockAndUpdate((BlockPos) blocks[i], (BlockState) blocks[i + 1]);
			}
			return cast(player(server), Runes.RAY, rune);
		});
		if (cast == null) {
			context.waitTicks(10);
		}
		return cast;
	}

	/** Casts {@code rune} on the caster, with these stacks in their inventory (slot, stack pairs), and waits a moment. */
	private static String selfWith(ClientGameTestContext context, TestSingleplayerContext world, RuneDef rune, Object... stacks) {
		String cast = world.getServer().computeOnServer(server -> {
			Inventory inv = player(server).getInventory();
			for (int i = 0; i < stacks.length; i += 2) {
				inv.setItem((Integer) stacks[i], (ItemStack) stacks[i + 1]);
			}
			return cast(player(server), Runes.SELF, rune);
		});
		if (cast == null) {
			context.waitTicks(5);
		}
		return cast;
	}

	private static BlockState state(Block block) {
		return block.defaultBlockState();
	}

	private static int dropped(ServerLevel level, BlockPos near, Item item) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(near).inflate(2.5), e -> e.getItem().is(item)).stream()
			.mapToInt(e -> e.getItem().getCount()).sum();
	}

	private static int carried(ServerPlayer player, Item item) {
		Inventory inv = player.getInventory();
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i).is(item)) {
				n += inv.getItem(i).getCount();
			}
		}
		return n;
	}

	private static int held(Container box, Item item) {
		int n = 0;
		for (int i = 0; i < box.getContainerSize(); i++) {
			if (box.getItem(i).is(item)) {
				n += box.getItem(i).getCount();
			}
		}
		return n;
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			// Empty any chest first, so clearing it drops nothing.
			for (BlockPos p : BlockPos.betweenClosed(STAGE.offset(-4, 0, 0), STAGE.offset(4, 3, 8))) {
				if (level.getBlockEntity(p) instanceof Container box) {
					box.clearContent();
				}
			}
		});
		floor(world);
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.getInventory().clearContent();
			player.setGameMode(GameType.SURVIVAL);
			stand(player);
		});
		context.waitTicks(10);
	}

	// ------------------------------------------------------------------ mining

	/** A two-block pillar on the floor: the shaft takes both and stops a block above the drop under the floor. */
	private static String plumbline(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos low = AIM.below();
		String cast = rayAt(context, world, Runes.PLUMBLINE, AIM, state(Blocks.STONE), low, state(Blocks.STONE));
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			if (!level.getBlockState(AIM).isAir() || !level.getBlockState(low).isAir()) {
				return "both pillar blocks should be dug (" + level.getBlockState(AIM) + ", " + level.getBlockState(low) + ")";
			}
			if (!level.getBlockState(low.below()).is(Blocks.STONE)) {
				return "the floor over the open drop should be left, found " + level.getBlockState(low.below());
			}
			int cobble = dropped(level, low, Items.COBBLESTONE);
			return cobble == 2 ? null : "the two stones should drop two cobblestone as a pickaxe would, dropped " + cobble;
		});
	}

	/** Silklift: the stone comes out whole, as itself. */
	private static String silklift(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = rayAt(context, world, Runes.SILKLIFT, AIM, state(Blocks.STONE));
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			if (!level.getBlockState(AIM).isAir()) {
				return "the stone should be lifted";
			}
			int stone = dropped(level, AIM, Items.STONE);
			int cobble = dropped(level, AIM, Items.COBBLESTONE);
			return stone == 1 && cobble == 0 ? null : "one stone (not cobblestone) should drop, dropped " + stone + " stone, " + cobble + " cobblestone";
		});
	}

	/** Luckstrike: the iron ore breaks for raw iron. */
	private static String luckstrike(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = rayAt(context, world, Runes.LUCKSTRIKE, AIM, state(Blocks.IRON_ORE));
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			if (!level.getBlockState(AIM).isAir()) {
				return "the ore should be broken";
			}
			int raw = dropped(level, AIM, Items.RAW_IRON);
			return raw >= 1 && raw <= 4 ? null : "the ore should drop 1 to 4 raw iron (Fortune I), dropped " + raw;
		});
	}

	// ------------------------------------------------------------------ masonry

	private static String dressed(ClientGameTestContext context, TestSingleplayerContext world, RuneDef rune, Block from, Block into) {
		BlockPos west = AIM.west();
		BlockPos east = AIM.east();
		BlockPos far = AIM.offset(3, 0, 0);
		String cast = rayAt(context, world, rune, AIM, state(from), west, state(from), east, state(from), far, state(from));
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			for (BlockPos p : List.of(AIM, west, east)) {
				if (!level.getBlockState(p).is(into)) {
					return "the " + from + " at " + p.subtract(STAGE).toShortString() + " should become " + into + ", found " + level.getBlockState(p);
				}
			}
			if (!level.getBlockState(far).is(from)) {
				return "the block three away should be left alone";
			}
			return level.getEntitiesOfClass(ItemEntity.class, new AABB(AIM).inflate(4)).isEmpty() ? null : "dressing stone should drop nothing";
		});
	}

	private static String polish(ClientGameTestContext context, TestSingleplayerContext world) {
		return dressed(context, world, Runes.POLISH, Blocks.ANDESITE, Blocks.POLISHED_ANDESITE);
	}

	private static String brickwork(ClientGameTestContext context, TestSingleplayerContext world) {
		return dressed(context, world, Runes.BRICKWORK, Blocks.STONE, Blocks.STONE_BRICKS);
	}

	private static String agestone(ClientGameTestContext context, TestSingleplayerContext world) {
		return dressed(context, world, Runes.AGESTONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
	}

	/** Concrete Set: the powder (held up by a dirt block) hardens. */
	private static String concreteset(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = rayAt(context, world, Runes.CONCRETESET, AIM.below(), state(Blocks.DIRT), AIM, state(Blocks.CONCRETE_POWDER.asList().getFirst()));
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			BlockState s = player(server).level().getBlockState(AIM);
			return s.is(Blocks.CONCRETE.asList().getFirst()) ? null : "the powder should set to concrete of its colour, found " + s;
		});
	}

	/** Kiln Bake: cobblestone bakes to stone in place. */
	private static String kilnbake(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = rayAt(context, world, Runes.KILNBAKE, AIM, state(Blocks.COBBLESTONE));
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			BlockState s = player(server).level().getBlockState(AIM);
			return s.is(Blocks.STONE) ? null : "the cobblestone should bake to stone, found " + s;
		});
	}

	/** Lava Seal: a lava source set in the floor near the strike turns to obsidian. */
	private static String lavaseal(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos pool = STAGE.offset(2, -1, 4);
		String cast = rayAt(context, world, Runes.LAVASEAL, pool.below(), state(Blocks.STONE), pool, state(Blocks.LAVA), AIM, state(Blocks.STONE));
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			BlockState s = player(server).level().getBlockState(pool);
			return s.is(Blocks.OBSIDIAN) ? null : "the lava source should seal to obsidian, found " + s;
		});
	}

	// ------------------------------------------------------------------ light redstone

	/** Lever Flip: the lever on the floor beside the strike is thrown. */
	private static String leverflip(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos lever = STAGE.offset(1, 0, 4);
		BlockState off = Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
		String cast = rayAt(context, world, Runes.LEVERFLIP, lever, off, AIM, state(Blocks.STONE));
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			BlockState s = player(server).level().getBlockState(lever);
			return s.is(Blocks.LEVER) && s.getValue(BlockStateProperties.POWERED) ? null : "the lever should be thrown on, found " + s;
		});
	}

	/** Doorcall: the oak door opens; the iron door beside it stays shut. */
	private static String doorcall(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos oak = STAGE.offset(1, 0, 4);
		BlockPos iron = STAGE.offset(-1, 0, 4);
		String cast = rayAt(context, world, Runes.DOORCALL,
			oak, state(Blocks.OAK_DOOR), oak.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER),
			iron, state(Blocks.IRON_DOOR), iron.above(), Blocks.IRON_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER),
			AIM, state(Blocks.STONE));
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			BlockState o = level.getBlockState(oak);
			BlockState i = level.getBlockState(iron);
			if (!o.is(Blocks.OAK_DOOR) || !o.getValue(BlockStateProperties.OPEN)) {
				return "the oak door should open, found " + o;
			}
			return i.is(Blocks.IRON_DOOR) && !i.getValue(BlockStateProperties.OPEN) ? null : "the iron door should stay shut, found " + i;
		});
	}

	// ------------------------------------------------------------------ storage

	/** Chest Sort: three scattered stacks become two, in order, with nothing made or lost. */
	private static String chestsort(ClientGameTestContext context, TestSingleplayerContext world) {
		String set = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			level.setBlockAndUpdate(AIM, state(Blocks.CHEST));
			if (!(level.getBlockEntity(AIM) instanceof Container box)) {
				return "no chest";
			}
			box.setItem(0, new ItemStack(Items.DIRT, 10));
			box.setItem(5, new ItemStack(Items.COBBLESTONE, 3));
			box.setItem(9, new ItemStack(Items.DIRT, 20));
			return null;
		});
		if (set != null) {
			return set;
		}
		String cast = rayAt(context, world, Runes.CHESTSORT);
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			if (!(player(server).level().getBlockEntity(AIM) instanceof Container box)) {
				return "the chest is gone";
			}
			ItemStack first = box.getItem(0);
			ItemStack second = box.getItem(1);
			if (!first.is(Items.COBBLESTONE) || first.getCount() != 3 || !second.is(Items.DIRT) || second.getCount() != 30) {
				return "the chest should hold 3 cobblestone then 30 dirt, holds " + first + ", " + second;
			}
			for (int i = 2; i < box.getContainerSize(); i++) {
				if (!box.getItem(i).isEmpty()) {
					return "slot " + i + " should be empty, holds " + box.getItem(i);
				}
			}
			return null;
		});
	}

	/** Stow: the pack's cobblestone joins the chest's; the dirt (not in the chest) and the hotbar stay. */
	private static String stow(ClientGameTestContext context, TestSingleplayerContext world) {
		String set = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			level.setBlockAndUpdate(AIM, state(Blocks.CHEST));
			if (!(level.getBlockEntity(AIM) instanceof Container box)) {
				return "no chest";
			}
			box.setItem(0, new ItemStack(Items.COBBLESTONE, 1));
			Inventory inv = player.getInventory();
			inv.setItem(0, new ItemStack(Items.COBBLESTONE, 7));
			inv.setItem(10, new ItemStack(Items.COBBLESTONE, 20));
			inv.setItem(11, new ItemStack(Items.DIRT, 5));
			return null;
		});
		if (set != null) {
			return set;
		}
		String cast = rayAt(context, world, Runes.STOW);
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			if (!(player.level().getBlockEntity(AIM) instanceof Container box)) {
				return "the chest is gone";
			}
			Inventory inv = player.getInventory();
			if (held(box, Items.COBBLESTONE) != 21) {
				return "the chest should hold 21 cobblestone, holds " + held(box, Items.COBBLESTONE);
			}
			if (held(box, Items.DIRT) != 0 || carried(player, Items.DIRT) != 5) {
				return "the dirt (not in the chest yet) should stay in the pack";
			}
			return inv.getItem(0).is(Items.COBBLESTONE) && inv.getItem(0).getCount() == 7 && carried(player, Items.COBBLESTONE) == 7 ? null
				: "the hotbar's 7 cobblestone should stay, and only they (carried " + carried(player, Items.COBBLESTONE) + ")";
		});
	}

	/** Block Pack: 20 iron ingots become 2 iron blocks and 2 ingots. */
	private static String blockpack(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = selfWith(context, world, Runes.BLOCKPACK, 12, new ItemStack(Items.IRON_INGOT, 20));
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			int blocks = carried(player, Items.IRON_BLOCK);
			int ingots = carried(player, Items.IRON_INGOT);
			return blocks == 2 && ingots == 2 ? null : "20 ingots should pack to 2 blocks and 2 ingots, have " + blocks + " blocks and " + ingots + " ingots";
		});
	}

	/** Millstone: 10 cobblestone grind to 10 gravel. */
	private static String millstone(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = selfWith(context, world, Runes.MILLSTONE, 12, new ItemStack(Items.COBBLESTONE, 10));
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			int gravel = carried(player, Items.GRAVEL);
			int cobble = carried(player, Items.COBBLESTONE);
			return gravel == 10 && cobble == 0 ? null : "10 cobblestone should grind to 10 gravel, have " + gravel + " gravel and " + cobble + " cobblestone";
		});
	}

	// ------------------------------------------------------------------ building

	/** Floorlay: a 5x5 floor around the struck block, one cobblestone from the pack for each block laid. */
	private static String floorlay(ClientGameTestContext context, TestSingleplayerContext world) {
		String set = world.getServer().computeOnServer(server -> {
			player(server).getInventory().setItem(12, new ItemStack(Items.COBBLESTONE, 30));
			return null;
		});
		String cast = rayAt(context, world, Runes.FLOORLAY, AIM, state(Blocks.STONE));
		if (cast != null) {
			return cast;
		}
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			int laid = 0;
			for (BlockPos p : BlockPos.betweenClosed(AIM.offset(-2, 0, -2), AIM.offset(2, 0, 2))) {
				if (level.getBlockState(p).is(Blocks.COBBLESTONE)) {
					laid++;
				}
			}
			int left = carried(player, Items.COBBLESTONE);
			if (laid != 24) {
				return "24 cobblestone should be laid around the struck stone, laid " + laid;
			}
			return left == 6 ? null : "the pack should have 6 of 30 left (one spent per block), has " + left;
		});
	}

	/** Pit Floor: a passing 3x3 of packed mud before the struck face, written down to go. */
	private static String pitfloor(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = rayAt(context, world, Runes.PITFLOOR, AIM, state(Blocks.STONE));
		if (cast != null) {
			return cast;
		}
		BlockPos center = AIM.north();
		return world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			int mud = 0;
			for (BlockPos p : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
				if (level.getBlockState(p).is(Blocks.PACKED_MUD)) {
					mud++;
				}
			}
			if (mud != 8) {
				return "8 packed mud should be set around the spot before the stone, set " + mud;
			}
			return TemporaryBlocks.recorded(level, center) ? null : "the floor should be written down to go";
		});
	}
}
