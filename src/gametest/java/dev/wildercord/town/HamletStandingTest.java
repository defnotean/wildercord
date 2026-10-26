package dev.wildercord.town;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A hamlet in the real game: a bell is one only with three villagers about it, the traveller is greeted on the way in,
 * a trade and monsters slain near the bell earn its standing (none from a kill far off) up to a day's limit, the next
 * day's earnings raise a stranger to known with the hamlet's favour on them, and the standing is kept through a reopen.
 */
public final class HamletStandingTest implements FabricClientGameTest {
	private static final BlockPos BELL = new BlockPos(0, 101, 0);
	private static final BlockPos FAR = new BlockPos(0, 101, 72);

	private static void check(boolean x, String why) {
		if (!x) throw new AssertionError(why);
	}

	private static ServerPlayer p(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private static void villager(ServerLevel l, double x, double z) {
		Villager villager = EntityTypes.VILLAGER.create(l, EntitySpawnReason.COMMAND);
		villager.snapTo(x, 101, z, 0, 0);
		villager.setNoAi(true);
		l.addFreshEntity(villager);
	}

	/** The traveller kills a monster at {@code pos} with their own hand. */
	private static void slay(MinecraftServer s, BlockPos pos) {
		ServerLevel l = s.overworld();
		var zombie = EntityTypes.ZOMBIE.create(l, EntitySpawnReason.COMMAND);
		zombie.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		zombie.setNoAi(true);
		l.addFreshEntity(zombie);
		zombie.hurtServer(l, p(s).damageSources().playerAttack(p(s)), 1000);
		check(!zombie.isAlive(), "The monster dies");
	}

	private static Hamlets.Standing standing(MinecraftServer s) {
		return Hamlets.standing(p(s), s.overworld(), BELL);
	}

	/** Waits until just after the server's next hamlet visit check. */
	private static void nextVisit(ClientGameTestContext c, TestServerContext server) {
		int tick = server.computeOnServer(MinecraftServer::getTickCount);
		c.waitTicks(100 - tick % 100 + 3);
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		TestWorldSave save;
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			TestServerContext server = w.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("time set day");
			server.runOnServer(s -> {
				var l = s.overworld();
				for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
					for (int y = 101; y <= 104; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				l.setBlock(FAR.below(), Blocks.STONE.defaultBlockState(), 2);
				l.setBlock(BELL, Blocks.BELL.defaultBlockState(), 3);
				p(s).setGameMode(GameType.SURVIVAL);
				p(s).teleportTo(l, 0.5, 101, -5.5, Set.<Relative>of(), 0, 10, false);

				villager(l, 3.5, 3.5);
				villager(l, -3.5, 3.5);
				check(Hamlets.bell(l, p(s).blockPosition()) == null, "A bell with two villagers isn't a hamlet");
				villager(l, 3.5, -3.5);
				check(BELL.equals(Hamlets.bell(l, p(s).blockPosition())), "A bell with three villagers is");
				check(Hamlets.bell(l, FAR) == null, "Far from the bell is outside the hamlet");
				check(standing(s).tier() == BountyRules.Tier.STRANGER, "A traveller starts a stranger");
			});

			// Greeted on the way in, as a stranger, and given no favour yet.
			nextVisit(c, server);
			c.takeScreenshot(TestScreenshotOptions.of("hamlet_enter").disableCounterPrefix());
			server.runOnServer(s -> check(!p(s).hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "A stranger has no favour"));

			server.runOnServer(s -> {
				var l = s.overworld();
				// A trade with one of its villagers counts.
				Villager trader = l.getEntitiesOfClass(Villager.class, new AABB(BELL).inflate(8)).getFirst();
				trader.setTradingPlayer(p(s));
				trader.notifyTrade(new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(Items.BREAD), 12, 1, 0.05F));
				trader.setTradingPlayer(null);
				check(standing(s).reputation() == HamletRules.TRADE, "A trade earns standing, had " + standing(s).reputation());

				// A kill far off counts for nothing; kills near the bell count up to the day's limit.
				slay(s, FAR);
				check(standing(s).reputation() == HamletRules.TRADE, "A kill outside the hamlet earns nothing");
				for (int i = 0; i < HamletRules.DAILY; i++) slay(s, new BlockPos(-2 + i % 5, 101, 5));
				check(standing(s).reputation() == HamletRules.DAILY, "No more than the day's limit, had " + standing(s).reputation());
				check(standing(s).today() == HamletRules.DAILY, "The day's earnings are counted");

				// The next day it gives again: enough to be known, with the hamlet's favour.
				long day = l.getGameTime() / BountyRules.DAY;
				Map<String, Hamlets.Standing> all = new HashMap<>(p(s).getAttachedOrElse(Hamlets.STANDINGS, Map.of()));
				all.put(Hamlets.key(l, BELL), new Hamlets.Standing(HamletRules.DAILY, day - 1, HamletRules.DAILY));
				p(s).setAttached(Hamlets.STANDINGS, all);
				int need = BountyRules.Tier.KNOWN.needs - HamletRules.DAILY;
				for (int i = 0; i < need; i++) slay(s, new BlockPos(-2 + i % 5, 101, -2));
				check(standing(s).reputation() == BountyRules.Tier.KNOWN.needs && standing(s).tier() == BountyRules.Tier.KNOWN,
					"A new day's earnings make the traveller known, had " + standing(s).reputation());
				var hero = p(s).getEffect(MobEffects.HERO_OF_THE_VILLAGE);
				check(hero != null && hero.getAmplifier() == HamletRules.favour(BountyRules.Tier.KNOWN), "Known brings the hamlet's favour");
			});

			// The favour is kept on the traveller while they stay.
			nextVisit(c, server);
			server.runOnServer(s -> {
				var hero = p(s).getEffect(MobEffects.HERO_OF_THE_VILLAGE);
				check(hero != null && hero.getDuration() > HamletRules.FAVOUR_TICKS - 20, "Staying refreshes the favour");
			});
			c.takeScreenshot(TestScreenshotOptions.of("hamlet_known").disableCounterPrefix());
			save = w.getWorldSave();
		}

		try (var w = save.open()) {
			c.waitTicks(30);
			w.getServer().runOnServer(s -> check(standing(s).reputation() == BountyRules.Tier.KNOWN.needs,
				"The hamlet's standing is kept through a save and reopen"));
		}
	}
}
