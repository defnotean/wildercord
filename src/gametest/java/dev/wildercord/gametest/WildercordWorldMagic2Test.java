package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.TemporaryBlocks;
import dev.wildercord.cast.WorldMagic;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.mixin.AbstractFurnaceBlockEntityAccessor;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.WorldRules;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Magic that changes the world, the second layer, checked in a real world: frost crusts lava over
 * (never round a creature in it), the crust glows as magma before it melts back and melts back after a
 * crash too, and broken it drops nothing; storm scrapes copper a stage clean, pulses a lightning rod
 * (lighting the lamp under it) and in time charges a creeper; fire primes TNT and lights a candle that
 * frost then snuffs; time ripens a crop a stage, ages a baby cow and jumps a furnace ahead; void
 * anchors an enderman; life starts curing a weakened zombie villager (and only a weakened one); blood
 * ripens nether wart; arcane shows an invisible creature; and a claim, a monster's spell or a server
 * with {@code spells_edit_blocks} off stops the block changes.
 *
 * <p>Spells are applied straight to a hit at a chosen point ({@link CastEngine#onHit}), as in
 * {@link WildercordWorldMagicTest}. The crust takes 25 seconds to melt, so it's laid first and the
 * other checks run while it waits.</p>
 *
 * <p>Runs in the full suite; {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} or
 * {@code WILDERCORD_SHOWCASE} skip it.</p>
 */
public class WildercordWorldMagic2Test implements FabricClientGameTest {
	/** The stand-in claim: while set, breaking (so changing) any block inside it is refused. */
	private static volatile AABB claim;
	private static boolean listening;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		if (!listening) {
			listening = true;
			PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
				AABB box = claim;
				return box == null || !box.contains(Vec3.atCenterOf(pos));
			});
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			var server = world.getServer();
			List<String> failures = new ArrayList<>();

			server.runOnServer(s -> player(s).setGameMode(GameType.SURVIVAL));
			context.waitTicks(2);

			// Frost on lava: the surface cools into a crust of basalt, written down to melt back, but never
			// round a creature standing in the lava.
			long[] crustedAt = new long[1];
			BlockPos[] lavaPool = new BlockPos[1];
			String crust = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos pool = site(player, 12, 0);
				lavaPool[0] = pool;
				pool(level, pool, 1, Blocks.LAVA.defaultBlockState());
				BlockPos occupied = pool.below().east();
				Mob husk = mob(level, EntityTypes.HUSK, Vec3.atBottomCenterOf(occupied));
				crustedAt[0] = level.getGameTime();
				apply(player, List.of(Runes.TOUCH, Runes.FROST), Vec3.atCenterOf(pool), List.of());
				husk.discard();
				BlockState top = level.getBlockState(pool.below());
				if (!top.is(Blocks.BASALT)) {
					return "frost on lava should cool its surface into basalt (found " + top + ")";
				}
				if (!TemporaryBlocks.recorded(level, pool.below())) {
					return "the crust should be written down, to melt back even after a crash";
				}
				if (!level.getBlockState(occupied).is(Blocks.LAVA)) {
					return "frost shouldn't crust lava a creature is standing in (found " + level.getBlockState(occupied) + ")";
				}
				return null;
			});
			note(failures, crust);

			// A block of crust broken with a pickaxe drops nothing: the lava comes back.
			String broken = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos pool = site(player, 24, 0);
				pool(level, pool, 1, Blocks.LAVA.defaultBlockState());
				apply(player, List.of(Runes.TOUCH, Runes.FROST), Vec3.atCenterOf(pool), List.of());
				BlockPos crusted = pool.below();
				if (!level.getBlockState(crusted).is(Blocks.BASALT)) {
					return "frost should crust the second pool (found " + level.getBlockState(crusted) + ")";
				}
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
				try {
					player.gameMode.destroyBlock(crusted);
				} finally {
					player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				}
				List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(crusted).inflate(3),
					e -> e.getItem().is(Items.BASALT) || e.getItem().is(Items.MAGMA_BLOCK));
				drops.forEach(Entity::discard);
				if (!drops.isEmpty()) {
					return "breaking the crust shouldn't drop anything";
				}
				if (!level.getBlockState(crusted).is(Blocks.LAVA)) {
					return "breaking the crust should bring the lava back (found " + level.getBlockState(crusted) + ")";
				}
				return TemporaryBlocks.recorded(level, crusted) ? "a crust block broken should be crossed off" : null;
			});
			note(failures, broken);

			// Storm scrapes weathered copper back a stage, and pulses a lightning rod: the lamp under it lights.
			BlockPos[] rodAt = new BlockPos[1];
			String storm = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos copper = site(player, 0, 12);
				level.setBlockAndUpdate(copper, Blocks.COPPER_BLOCK.weathering().weathered().defaultBlockState());
				apply(player, List.of(Runes.TOUCH, Runes.SHOCK), Vec3.atCenterOf(copper), List.of());
				BlockState scraped = level.getBlockState(copper);
				level.setBlockAndUpdate(copper, Blocks.AIR.defaultBlockState());
				if (!scraped.is(Blocks.COPPER_BLOCK.weathering().exposed())) {
					return "storm should scrape weathered copper back to exposed (found " + scraped + ")";
				}
				BlockPos rod = site(player, 6, 12).above();
				rodAt[0] = rod;
				level.setBlockAndUpdate(rod.below(), Blocks.REDSTONE_LAMP.defaultBlockState());
				level.setBlockAndUpdate(rod, Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState());
				apply(player, List.of(Runes.TOUCH, Runes.SHOCK), Vec3.atCenterOf(rod), List.of());
				if (!level.getBlockState(rod).getValue(LightningRodBlock.POWERED)) {
					return "storm should pulse a lightning rod";
				}
				return level.getBlockState(rod.below()).getValue(RedstoneLampBlock.LIT) ? null : "the rod's pulse should light the lamp under it";
			});
			note(failures, storm);
			context.waitTicks(20);
			String rodOff = server.computeOnServer(s -> {
				BlockState state = player(s).level().getBlockState(rodAt[0]);
				return state.getBlock() instanceof LightningRodBlock && !state.getValue(LightningRodBlock.POWERED) ? null
					: "a lightning rod storm pulsed should switch off again, as after a real strike (found " + state + ")";
			});
			note(failures, rodOff);

			// Storm on a creeper charges it now and then (a quarter of the time): in forty shocks, surely once.
			String creeper = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				Creeper mob = (Creeper) mob(level, EntityTypes.CREEPER, Vec3.atBottomCenterOf(site(player, -12, 12)));
				try {
					for (int i = 0; i < 40 && !mob.isPowered(); i++) {
						apply(player, List.of(Runes.TOUCH, Runes.SHOCK), mob.getBoundingBox().getCenter(), List.of(mob));
						mob.setHealth(mob.getMaxHealth());
					}
					return mob.isPowered() ? null : "forty storm hits should charge a creeper at least once";
				} finally {
					mob.discard();
				}
			});
			note(failures, creeper);

			// Fire primes TNT (taken away again at once) and lights a candle; frost then snuffs the candle.
			String fire = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos tnt = site(player, 12, 12);
				platform(level, tnt);
				level.setBlockAndUpdate(tnt, Blocks.TNT.defaultBlockState());
				apply(player, List.of(Runes.TOUCH, Runes.FIRE), Vec3.atCenterOf(tnt), List.of());
				List<PrimedTnt> primed = level.getEntitiesOfClass(PrimedTnt.class, new AABB(tnt).inflate(2));
				primed.forEach(Entity::discard);
				boolean gone = level.getBlockState(tnt).isAir();
				level.setBlockAndUpdate(tnt, Blocks.AIR.defaultBlockState());
				if (primed.isEmpty() || !gone) {
					return "fire should prime TNT (" + primed.size() + " primed, block " + (gone ? "gone" : "still there") + ")";
				}
				BlockPos candle = site(player, 18, 12);
				platform(level, candle);
				level.setBlockAndUpdate(candle, Blocks.CANDLE.defaultBlockState());
				apply(player, List.of(Runes.TOUCH, Runes.FIRE), Vec3.atCenterOf(candle), List.of());
				if (!level.getBlockState(candle).getValue(CandleBlock.LIT)) {
					return "fire should light a candle";
				}
				apply(player, List.of(Runes.TOUCH, Runes.FROST), Vec3.atCenterOf(candle), List.of());
				return level.getBlockState(candle).getValue(CandleBlock.LIT) ? "frost should snuff a lit candle" : null;
			});
			note(failures, fire);

			// Time ripens wheat one stage and ages a baby cow (nothing struck directly: it lands beside them).
			String time = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos wheat = site(player, -12, 0);
				level.setBlockAndUpdate(wheat.below(), Blocks.FARMLAND.defaultBlockState());
				level.setBlockAndUpdate(wheat, Blocks.WHEAT.defaultBlockState());
				Cow calf = (Cow) mob(level, EntityTypes.COW, Vec3.atBottomCenterOf(wheat.offset(2, 0, 0)));
				calf.setBaby(true);
				int before = calf.getAge();
				apply(player, List.of(Runes.TOUCH, Runes.COUNTDOWN), Vec3.atCenterOf(wheat), List.of());
				int after = calf.getAge();
				calf.discard();
				BlockState grown = level.getBlockState(wheat);
				if (!(grown.getBlock() instanceof CropBlock crop) || crop.getAge(grown) != 1) {
					return "time should ripen wheat exactly one stage (found " + grown + ")";
				}
				return after > before ? null : "time should age a baby cow (age " + before + " -> " + after + ")";
			});
			note(failures, time);

			// Time on a furnace that's smelting jumps it ahead.
			BlockPos furnaceAt = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				BlockPos pos = site(player, -18, 0);
				player.level().setBlockAndUpdate(pos, Blocks.FURNACE.defaultBlockState());
				if (player.level().getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace) {
					furnace.setItem(0, new ItemStack(Items.RAW_IRON, 4));
					furnace.setItem(1, new ItemStack(Items.COAL, 1));
				}
				return pos;
			});
			context.waitTicks(10);
			String furnace = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				if (!(player.level().getBlockEntity(furnaceAt) instanceof AbstractFurnaceBlockEntity furnaceEntity)) {
					return "the furnace is gone";
				}
				AbstractFurnaceBlockEntityAccessor smelting = (AbstractFurnaceBlockEntityAccessor) furnaceEntity;
				int before = smelting.wildercord$cookingTimer();
				if (before <= 0) {
					return "the furnace should be smelting before time lands on it";
				}
				apply(player, List.of(Runes.TOUCH, Runes.COUNTDOWN), Vec3.atCenterOf(furnaceAt), List.of());
				int after = smelting.wildercord$cookingTimer();
				player.level().setBlockAndUpdate(furnaceAt, Blocks.AIR.defaultBlockState());
				return after >= before + WorldRules.FURNACE_SKIP_TICKS - 1 ? null
					: "time should jump a furnace's smelting ahead (" + before + " -> " + after + " ticks)";
			});
			note(failures, furnace);

			// Void anchors an enderman: it can't teleport.
			String anchor = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				Enderman enderman = (Enderman) mob(level, EntityTypes.ENDERMAN, Vec3.atBottomCenterOf(site(player, 0, -12)));
				try {
					apply(player, List.of(Runes.TOUCH, Runes.BLIND), enderman.getBoundingBox().getCenter(), List.of(enderman));
					if (!WorldMagic.anchored(enderman)) {
						return "void should anchor an enderman it strikes";
					}
					Vec3 was = enderman.position();
					BlockPos away = site(player, 6, -12);
					boolean moved = enderman.teleport(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
					return !moved && enderman.position().distanceTo(was) < 0.5 ? null : "an anchored enderman shouldn't teleport";
				} finally {
					enderman.discard();
				}
			});
			note(failures, anchor);

			// Life on a zombie villager with Weakness starts its cure; one without Weakness is left as it is.
			String cure = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				ZombieVillager weak = (ZombieVillager) mob(level, EntityTypes.ZOMBIE_VILLAGER, Vec3.atBottomCenterOf(site(player, -12, -12)));
				ZombieVillager strong = (ZombieVillager) mob(level, EntityTypes.ZOMBIE_VILLAGER, Vec3.atBottomCenterOf(site(player, -18, -12)));
				try {
					weak.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
					apply(player, List.of(Runes.TOUCH, Runes.HEAL), weak.getBoundingBox().getCenter(), List.of(weak));
					apply(player, List.of(Runes.TOUCH, Runes.HEAL), strong.getBoundingBox().getCenter(), List.of(strong));
					if (!weak.isConverting()) {
						return "life on a weakened zombie villager should start curing it";
					}
					return strong.isConverting() ? "life on a zombie villager without Weakness shouldn't cure it" : null;
				} finally {
					weak.discard();
					strong.discard();
				}
			});
			note(failures, cure);

			// Blood ripens nether wart a stage; arcane shows an invisible husk nearby (it glows).
			String bloodAndArcane = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos wart = site(player, -24, -12);
				level.setBlockAndUpdate(wart.below(), Blocks.SOUL_SAND.defaultBlockState());
				level.setBlockAndUpdate(wart, Blocks.NETHER_WART.defaultBlockState());
				apply(player, List.of(Runes.TOUCH, Runes.LEECH), Vec3.atCenterOf(wart), List.of());
				int age = level.getBlockState(wart).getValue(NetherWartBlock.AGE);
				if (age != 1) {
					return "blood should ripen nether wart one stage (its age is " + age + ")";
				}
				BlockPos spot = site(player, 12, 24);
				Mob husk = mob(level, EntityTypes.HUSK, Vec3.atBottomCenterOf(spot.offset(2, 0, 0)));
				try {
					husk.setInvisible(true);
					apply(player, List.of(Runes.TOUCH, Runes.HARM), Vec3.atCenterOf(spot), List.of());
					return husk.hasEffect(MobEffects.GLOWING) ? null : "arcane should show an invisible creature nearby";
				} finally {
					husk.discard();
				}
			});
			note(failures, bloodAndArcane);

			// Protected ground: a rod inside a claim isn't pulsed, and a monster's frost crusts no lava.
			String protectedGround = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos rod = site(player, 12, -12).above();
				level.setBlockAndUpdate(rod, Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState());
				claim = new AABB(rod).inflate(3);
				try {
					apply(player, List.of(Runes.TOUCH, Runes.SHOCK), Vec3.atCenterOf(rod), List.of());
				} finally {
					claim = null;
				}
				boolean pulsed = level.getBlockState(rod).getValue(LightningRodBlock.POWERED);
				level.setBlockAndUpdate(rod, Blocks.AIR.defaultBlockState());
				if (pulsed) {
					return "storm inside a claim shouldn't pulse a lightning rod";
				}
				BlockPos pool = site(player, 24, -12);
				pool(level, pool, 1, Blocks.LAVA.defaultBlockState());
				Mob mob = mob(level, EntityTypes.HUSK, Vec3.atBottomCenterOf(pool.offset(-4, 0, 0)));
				SpellPlan.Group group = SpellCompiler.compile(List.of(Runes.TOUCH, Runes.FROST)).root().groups.getFirst();
				CastEngine.onHit(new Cast(mob), group, new Cast.Hit(List.of(), Vec3.atCenterOf(pool), new Vec3(1, 0, 0), mob.position(), null, null, false), null);
				mob.discard();
				return count(level, pool, 3, state -> state.is(Blocks.BASALT)) > 0 ? "a monster's frost shouldn't crust lava" : null;
			});
			note(failures, protectedGround);

			// A server that turned casting.spells_edit_blocks off: none of it changes a block.
			note(failures, noEdits(context, world));

			// A crust left by a server that stopped without warning melts back once its ground is loaded.
			BlockPos leftover = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos pool = site(player, 0, 24);
				pool(level, pool, 1, Blocks.LAVA.defaultBlockState());
				BlockPos at = pool.below();
				level.setBlockAndUpdate(at, Blocks.BASALT.defaultBlockState());
				TemporaryBlocks.put(level, at, Blocks.BASALT.defaultBlockState(), Blocks.LAVA.defaultBlockState(), level.getGameTime() - 200);
				return at;
			});
			context.waitTicks(30);
			String safetyNet = server.computeOnServer(s -> {
				ServerLevel level = player(s).level();
				if (!level.getBlockState(leftover).is(Blocks.LAVA)) {
					return "a crust left past its time should melt back into lava (found " + level.getBlockState(leftover) + ")";
				}
				return TemporaryBlocks.recorded(level, leftover) ? "a crust melted back should be crossed off" : null;
			});
			note(failures, safetyNet);

			// The first crust, for its last seconds, glows as magma; then it melts back into lava.
			waitUntil(context, world, crustedAt[0] + WorldRules.CRUST_TICKS - WorldRules.CRUST_WARN_TICKS + 40);
			String glowing = server.computeOnServer(s -> {
				BlockState top = player(s).level().getBlockState(lavaPool[0].below());
				return top.is(Blocks.MAGMA_BLOCK) ? null : "the crust should turn to magma for its last seconds (found " + top + ")";
			});
			note(failures, glowing);
			waitUntil(context, world, crustedAt[0] + WorldRules.CRUST_TICKS + 40);
			String melted = server.computeOnServer(s -> {
				ServerLevel level = player(s).level();
				BlockPos pool = lavaPool[0];
				int left = count(level, pool, 3, state -> state.is(Blocks.BASALT) || state.is(Blocks.MAGMA_BLOCK));
				if (left > 0) {
					return "the crust should melt back into lava on time (" + left + " blocks still standing)";
				}
				return TemporaryBlocks.recorded(level, pool.below()) ? "a crust melted back should be crossed off" : null;
			});
			note(failures, melted);

			if (!failures.isEmpty()) {
				throw new AssertionError("Magic that changes the world (the second layer) went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			claim = null;
		}
	}

	/**
	 * With {@code casting.spells_edit_blocks} off, frost crusts no lava, fire lights no candle, storm
	 * scrapes no copper and time ripens no wheat. The config file is put back afterwards.
	 */
	private static String noEdits(ClientGameTestContext context, TestSingleplayerContext world) {
		Path path = Config.path();
		String original;
		try {
			original = Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : null;
		} catch (IOException e) {
			return "couldn't read " + path + ": " + e;
		}
		try {
			String noEdits = WildercordConfig.DEFAULTS.toJson().replace("\"spells_edit_blocks\": true", "\"spells_edit_blocks\": false");
			if (!noEdits.contains("\"spells_edit_blocks\": false")) {
				return "the default config should list spells_edit_blocks";
			}
			write(path, noEdits);
			world.getServer().runCommand("wildercord reload");
			context.waitTicks(2);
			return world.getServer().computeOnServer(s -> {
				if (Config.get().spellsEditBlocks()) {
					return "/wildercord reload should read spells_edit_blocks: false";
				}
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos pool = site(player, -24, 12);
				pool(level, pool, 1, Blocks.LAVA.defaultBlockState());
				apply(player, List.of(Runes.TOUCH, Runes.FROST), Vec3.atCenterOf(pool), List.of());
				if (count(level, pool, 3, state -> state.is(Blocks.BASALT)) > 0) {
					return "with spells_edit_blocks false, frost shouldn't crust lava";
				}
				BlockPos candle = site(player, -24, -6);
				platform(level, candle);
				level.setBlockAndUpdate(candle, Blocks.CANDLE.defaultBlockState());
				apply(player, List.of(Runes.TOUCH, Runes.FIRE), Vec3.atCenterOf(candle), List.of());
				if (level.getBlockState(candle).getValue(CandleBlock.LIT)) {
					return "with spells_edit_blocks false, fire shouldn't light a candle";
				}
				BlockPos copper = site(player, -30, -6);
				level.setBlockAndUpdate(copper, Blocks.COPPER_BLOCK.weathering().weathered().defaultBlockState());
				apply(player, List.of(Runes.TOUCH, Runes.SHOCK), Vec3.atCenterOf(copper), List.of());
				if (!level.getBlockState(copper).is(Blocks.COPPER_BLOCK.weathering().weathered())) {
					return "with spells_edit_blocks false, storm shouldn't scrape copper";
				}
				BlockPos wheat = site(player, -30, 6);
				level.setBlockAndUpdate(wheat.below(), Blocks.FARMLAND.defaultBlockState());
				level.setBlockAndUpdate(wheat, Blocks.WHEAT.defaultBlockState());
				apply(player, List.of(Runes.TOUCH, Runes.COUNTDOWN), Vec3.atCenterOf(wheat), List.of());
				BlockState grown = level.getBlockState(wheat);
				return grown.getBlock() instanceof CropBlock crop && crop.getAge(grown) == 0 ? null
					: "with spells_edit_blocks false, time shouldn't ripen wheat (found " + grown + ")";
			});
		} finally {
			try {
				if (original != null) {
					write(path, original);
				} else {
					Files.deleteIfExists(path);
				}
			} catch (AssertionError | IOException ignored) {
				// Best effort: the next start writes the defaults again.
			}
			world.getServer().runCommand("wildercord reload");
			context.waitTicks(2);
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void note(List<String> failures, String failure) {
		if (failure != null) {
			failures.add(failure);
		}
	}

	/** Waits (a few ticks at a time) until the server's game time reaches {@code time}. */
	private static void waitUntil(ClientGameTestContext context, TestSingleplayerContext world, long time) {
		long now = world.getServer().computeOnServer(s -> player(s).level().getGameTime());
		if (time > now) {
			context.waitTicks((int) (time - now));
		}
	}

	/** A mob that stands still where it's put: never a random Runebound (tests pick their monsters). */
	private static Mob mob(ServerLevel level, EntityType<? extends Mob> type, Vec3 at) {
		Mob mob = type.create(level, EntitySpawnReason.COMMAND);
		mob.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		mob.setNoAi(true);
		mob.setPersistenceRequired();
		mob.addTag("wildercord.rolled");
		level.addFreshEntity(mob);
		return mob;
	}

	/** A spot on the ground {@code dx}, {@code dz} blocks from the player: the air block just above the surface. */
	private static BlockPos site(ServerPlayer player, int dx, int dz) {
		ServerLevel level = player.level();
		BlockPos at = player.blockPosition().offset(dx, 0, dz);
		int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
		return new BlockPos(at.getX(), top, at.getZ());
	}

	/**
	 * Digs a 5x5 pool of {@code fluid} {@code depth} deep, walled in stone, whose middle surface block is
	 * {@code centre.below()} (open air above). Each call digs from the same surface, so it can be rebuilt.
	 */
	private static void pool(ServerLevel level, BlockPos centre, int depth, BlockState fluid) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int dy = 0; dy <= 3; dy++) {
					level.setBlockAndUpdate(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
				}
				for (int dy = 1; dy <= depth + 1; dy++) {
					boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3 || dy == depth + 1;
					level.setBlockAndUpdate(centre.offset(dx, -dy, dz), rim ? Blocks.STONE.defaultBlockState() : fluid);
				}
			}
		}
	}

	/** A 5x5 floor of stone with open air above, around {@code centre} (the air above the floor): nothing about it burns. */
	private static void platform(ServerLevel level, BlockPos centre) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				level.setBlockAndUpdate(centre.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
			}
		}
	}

	/** How many blocks within {@code r} of {@code centre} (in a cube) pass {@code test}. */
	private static int count(ServerLevel level, BlockPos centre, int r, java.util.function.Predicate<BlockState> test) {
		int n = 0;
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
			if (test.test(level.getBlockState(pos))) {
				n++;
			}
		}
		return n;
	}

	/** Lands a spell's first group on {@code at}, as the player's, striking {@code struck}. */
	private static void apply(ServerPlayer player, List<RuneDef> runes, Vec3 at, List<Entity> struck) {
		SpellPlan.Group group = SpellCompiler.compile(runes).root().groups.getFirst();
		Vec3 origin = player.position();
		Vec3 dir = at.subtract(origin).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : at.subtract(origin).normalize();
		CastEngine.onHit(new Cast(player), group, new Cast.Hit(struck, at, dir, origin, null, null, false), null);
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
