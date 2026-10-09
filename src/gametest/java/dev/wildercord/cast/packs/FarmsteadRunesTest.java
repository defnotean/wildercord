package dev.wildercord.cast.packs;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The farmstead runes cast for real by a Survival player in a real world: a field is tilled, wetted,
 * sown and ripened; the kitchen cooks only from the pack; a herd is shorn, milked and courted; a log
 * is stripped and coarse dirt loosened; and the gentle support and movement runes land on the caster.
 */
public final class FarmsteadRunesTest implements FabricClientGameTest {
	private static final int FIELD_Y = 99;
	private final List<Animal> herd = new ArrayList<>();

	@Override
	public void runTest(ClientGameTestContext c) {
		try (TestSingleplayerContext world = c.worldBuilder().create()) {
			c.waitTicks(40);
			var server = world.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule random_tick_speed 0");
			server.runCommand("time set 6000");
			server.runCommand("fill -8 90 -8 8 98 8 stone");
			server.runCommand("fill -8 99 -8 8 99 8 dirt");
			server.runCommand("fill -8 100 -8 8 110 8 air");
			server.runCommand("fill 22 90 22 38 99 38 stone");
			server.runCommand("fill 22 100 22 38 110 38 air");
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				p.setGameMode(GameType.SURVIVAL);
				p.teleportTo(s.overworld(), 0.5, 100, 0.5, Set.<Relative>of(), 0, 90, false);
				Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
				var book = Spellbooks.get(p).withStarterGiven();
				for (RuneDef rune : Runes.all()) {
					book = book.learn(rune.id());
				}
				Spellbooks.set(p, book);
				p.getInventory().clearContent();
			});
			c.waitTicks(10);

			// ---- The field: Furrow, Dewfall, Sow, Ripen, all cast on yourself over the dirt you stand on.
			cast(c, server, Runes.TILLAGE);
			server.runOnServer(s -> {
				int farmland = count(s.overworld(), FIELD_Y, st -> st.is(Blocks.FARMLAND));
				check(farmland >= 20 && farmland <= 25, "Self Furrow tills the 5-by-5 patch under the caster (" + farmland + ")");
				check(s.overworld().getBlockState(new BlockPos(4, FIELD_Y, 4)).is(Blocks.DIRT), "Furrow stays within its patch");
				check(s.overworld().getBlockState(new BlockPos(1, FIELD_Y, 1)).getValue(FarmlandBlock.MOISTURE) == 0, "Fresh farmland starts dry");
			});
			cast(c, server, Runes.DEWFALL);
			server.runOnServer(s -> check(s.overworld().getBlockState(new BlockPos(1, FIELD_Y, 1)).getValue(FarmlandBlock.MOISTURE) == FarmlandBlock.MAX_MOISTURE,
				"Dewfall wets the farmland to full moisture"));
			server.runOnServer(s -> player(s).getInventory().add(new ItemStack(Items.WHEAT_SEEDS, 40)));
			cast(c, server, Runes.SOW);
			int[] sown = {0};
			server.runOnServer(s -> {
				sown[0] = count(s.overworld(), FIELD_Y + 1, st -> st.is(Blocks.WHEAT));
				check(sown[0] >= 20, "Sow plants wheat on the farmland (" + sown[0] + ")");
				check(player(s).getInventory().countItem(Items.WHEAT_SEEDS) == 40 - sown[0], "Sow spends exactly one of the caster's seeds per plant");
				check(((CropBlock) Blocks.WHEAT).getAge(s.overworld().getBlockState(new BlockPos(1, FIELD_Y + 1, 1))) == 0, "Newly sown wheat is a seedling");
			});
			cast(c, server, Runes.RIPEN);
			server.runOnServer(s -> {
				int aged = count(s.overworld(), FIELD_Y + 1, st -> st.is(Blocks.WHEAT) && ((CropBlock) Blocks.WHEAT).getAge(st) == 1);
				check(aged >= 20, "Ripen moves the sown wheat one stage on, with random ticks off (" + aged + ")");
			});

			// ---- The kitchen: cooks only what the caster carries.
			server.runOnServer(s -> {
				var inv = player(s).getInventory();
				inv.clearContent();
				inv.add(new ItemStack(Items.BEEF, 3));
				inv.add(new ItemStack(Items.RED_MUSHROOM, 2));
				inv.add(new ItemStack(Items.BROWN_MUSHROOM, 2));
				inv.add(new ItemStack(Items.BOWL, 2));
				inv.add(new ItemStack(Items.WHEAT, 10));
			});
			cast(c, server, Runes.HEARTHCOOK);
			server.runOnServer(s -> {
				var inv = player(s).getInventory();
				check(inv.countItem(Items.COOKED_BEEF) == 3 && inv.countItem(Items.BEEF) == 0, "Hearthcook smokes the raw beef in the pack");
			});
			cast(c, server, Runes.STEWPOT);
			server.runOnServer(s -> {
				var inv = player(s).getInventory();
				check(inv.countItem(Items.MUSHROOM_STEW) == 2, "Stewpot makes a stew per red, brown and bowl");
				check(inv.countItem(Items.BOWL) == 0 && inv.countItem(Items.RED_MUSHROOM) == 0 && inv.countItem(Items.BROWN_MUSHROOM) == 0,
					"Stewpot spends exactly its ingredients");
			});
			cast(c, server, Runes.BAKEHOUSE);
			server.runOnServer(s -> {
				var inv = player(s).getInventory();
				check(inv.countItem(Items.BREAD) == 3 && inv.countItem(Items.WHEAT) == 1, "Bakehouse bakes three wheat a loaf, leaving the remainder");
			});

			// ---- The table and the lane: gentle effects on the caster.
			server.runOnServer(s -> player(s).getFoodData().setFoodLevel(8));
			cast(c, server, Runes.PICNIC);
			server.runOnServer(s -> check(player(s).getFoodData().getFoodLevel() >= 8 + FarmRules.PICNIC_HUNGER, "Picnic feeds the caster"));
			cast(c, server, Runes.LEAFSHADE);
			server.runOnServer(s -> check(player(s).hasEffect(MobEffects.FIRE_RESISTANCE), "Canopy shades the caster from fire"));
			cast(c, server, Runes.FIELDSTRIDE);
			server.runOnServer(s -> check(player(s).hasEffect(MobEffects.SPEED) && player(s).hasEffect(MobEffects.JUMP_BOOST), "Fieldstride quickens the caster's step"));
			double[] before = {0};
			server.runOnServer(s -> before[0] = player(s).getY());
			castNow(server, Runes.HAYLOFT);
			c.waitTicks(6);
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				check(p.hasEffect(MobEffects.SLOW_FALLING), "Hayloft lets the caster float down");
				check(p.getY() > before[0] + 0.5, "Hayloft tosses the caster up (" + before[0] + " -> " + p.getY() + ")");
			});
			c.waitTicks(80);
			server.runOnServer(s -> check(player(s).getHealth() >= player(s).getMaxHealth() - 0.01F, "Hayloft's landing does no harm"));

			// ---- The herd and the wood, on a stone pad of their own.
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				ServerPlayer p = player(s);
				p.teleportTo(level, 30.5, 100, 30.5, Set.<Relative>of(), 0, 90, false);
				p.getInventory().clearContent();
				p.getInventory().add(new ItemStack(Items.BUCKET, 1));
				herd.add(spawn(level, EntityTypes.SHEEP, 32.5, 30.5));
				herd.add(spawn(level, EntityTypes.COW, 28.5, 30.5));
				herd.add(spawn(level, EntityTypes.COW, 30.5, 32.5));
				level.setBlockAndUpdate(new BlockPos(31, 100, 29), Blocks.OAK_LOG.defaultBlockState());
				level.setBlockAndUpdate(new BlockPos(29, 99, 29), Blocks.COARSE_DIRT.defaultBlockState());
			});
			c.waitTicks(5);
			cast(c, server, Runes.FLEECE);
			server.runOnServer(s -> check(((Sheep) herd.getFirst()).isSheared(), "Fleece shears the sheep beside the caster"));
			cast(c, server, Runes.MILKMAID);
			server.runOnServer(s -> {
				var inv = player(s).getInventory();
				check(inv.countItem(Items.MILK_BUCKET) == 1 && inv.countItem(Items.BUCKET) == 0, "Milkmaid fills the one bucket carried, and no more");
			});
			cast(c, server, Runes.COURTSHIP);
			server.runOnServer(s -> {
				check(herd.get(1).isInLove() && herd.get(2).isInLove(), "Courtship puts the grown cows in love");
			});
			cast(c, server, Runes.BARKSTRIP);
			server.runOnServer(s -> check(s.overworld().getBlockState(new BlockPos(31, 100, 29)).is(Blocks.STRIPPED_OAK_LOG), "Barkstrip strips the oak log"));
			cast(c, server, Runes.TILTH);
			server.runOnServer(s -> check(s.overworld().getBlockState(new BlockPos(29, 99, 29)).is(Blocks.DIRT), "Tilth loosens coarse dirt to dirt"));
			server.runOnServer(s -> {
				for (Animal animal : herd) {
					check(animal.isAlive() && animal.getHealth() >= animal.getMaxHealth() - 0.01F, "No farmstead rune hurts the herd");
				}
			});
		} finally {
			herd.clear();
		}
	}

	private static Animal spawn(ServerLevel level, EntityType<? extends Animal> type, double x, double z) {
		Animal animal = type.create(level, EntitySpawnReason.COMMAND);
		check(animal != null, "Spawn " + type);
		animal.setPos(x, 100, z);
		animal.setNoAi(true);
		level.addFreshEntity(animal);
		return animal;
	}

	private static void cast(ClientGameTestContext c, TestServerContext server, RuneDef effect) {
		castNow(server, effect);
		c.waitTicks(20);
	}

	private static void castNow(TestServerContext server, RuneDef effect) {
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			check(SpellCaster.edit(p, 0, List.of(Runes.SELF.id(), effect.id())) == null, "Accepted Self " + effect.id());
			Spellbooks.setMana(p, 100);
			Spellbooks.setReadyAt(p, 0, 0);
			float mana = Spellbooks.mana(p);
			SpellCaster.cast(p, 0);
			check(Spellbooks.mana(p) < mana, "Self " + effect.id() + " is cast and spends mana");
		});
	}

	private static int count(ServerLevel level, int y, java.util.function.Predicate<BlockState> what) {
		int n = 0;
		for (BlockPos p : BlockPos.betweenClosed(-3, y, -3, 3, y, 3)) {
			if (what.test(level.getBlockState(p))) {
				n++;
			}
		}
		return n;
	}

	private static ServerPlayer player(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private static void check(boolean yes, String why) {
		if (!yes) {
			throw new AssertionError(why);
		}
	}
}
