package dev.wildercord.gametest;

import dev.wildercord.town.BountyBoardBlock;
import dev.wildercord.town.BountyEscorts;
import dev.wildercord.town.BountyRules;
import dev.wildercord.town.InnRaid;
import dev.wildercord.town.InnRaidRules;
import dev.wildercord.town.Town;
import dev.wildercord.town.WayfarerKeeper;
import dev.wildercord.town.CaravanRules;
import dev.wildercord.town.Caravans;
import dev.wildercord.wildlife.MountContent;
import dev.wildercord.wildlife.RidgebackStag;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The Wayfarer Inn's town life in a real world (0.12 "Tempering"): the bounty board offers a hunt and takes it on a second click,
 * kills of the right creature near the board count (and far ones don't), the finished bounty pays emeralds and reputation and
 * the board is closed to that traveller till tomorrow; the keepers can't be hurt and sell more at each standing, the
 * stablemaster's deed leads up a tame, saddled Ridgeback Stag.
 *
 * <p>Run at the end of {@link WildercordWildlifeTest}.</p>
 */
public final class WildercordTownChecks {
	private static final int GROUND = 100;
	private static final BlockPos BOARD = new BlockPos(0, GROUND, 0);

	private final List<String> failures = new ArrayList<>();

	private WildercordTownChecks() {}

	/** Runs the checks in a fresh world of their own. */
	public static void run(ClientGameTestContext context) {
		new WildercordTownChecks().runTest(context);
	}

	private void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			var server = world.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("difficulty normal");
			server.runCommand("fill -12 " + (GROUND - 1) + " -12 12 " + (GROUND - 1) + " 12 minecraft:grass_block");
			server.runOnServer(s -> {
				ServerPlayer player = player(s);
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(player.level(), 0.5, GROUND, 3.5, Set.<Relative>of(), 180, 0, false);
				player.level().setBlockAndUpdate(BOARD, Town.BOUNTY_BOARD.defaultBlockState().setValue(BountyBoardBlock.FACING, Direction.SOUTH));
			});
			context.waitTicks(5);
			server.runOnServer(this::bounty);
			context.waitTicks(45);
			server.runOnServer(this::escortArrived);
			server.runOnServer(this::keepers);
			raid(context, server);
		}
		if (!failures.isEmpty()) {
			throw new AssertionError("Town: " + String.join("; ", failures));
		}
	}

	private void check(boolean ok, String what) {
		if (!ok) failures.add(what);
	}

	private void bounty(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		Town.set(player, Town.Standing.NONE);
		BountyBoardBlock.use(player, level, BOARD);
		check(Town.standing(player).bounty().isEmpty(), "the first click only shows the offer");
		BountyBoardBlock.use(player, level, BOARD);
		Optional<Town.Active> taken = Town.standing(player).bounty();
		check(taken.isPresent(), "the second click takes it");
		if (taken.isEmpty()) return;
		Town.Active bounty = taken.get();
		BountyRules.Bounty offered = BountyRules.offer(player.getUUID(), level.getGameTime() / BountyRules.DAY, BOARD.asLong());
		check(bounty.target().equals(offered.target()) && bounty.needed() == offered.needed(), "it's the bounty that was offered");
		String dimension = level.dimension().identifier().toString();
		// A kill far from the board, or of the wrong creature, doesn't count.
		Town.counted(player, bounty.target(), BOARD.getX() + 400, BOARD.getZ(), dimension);
		Town.counted(player, "minecraft:cow", BOARD.getX(), BOARD.getZ(), dimension);
		check(Town.standing(player).bounty().get().kills() == 0, "far or wrong kills don't count");
		// One real kill, then the rest counted straight in.
		var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(bounty.target()));
		if (type.create(level, EntitySpawnReason.COMMAND) instanceof LivingEntity victim) {
			victim.snapTo(BOARD.getX() + 3.5, GROUND, BOARD.getZ() + 3.5, 0, 0);
			level.addFreshEntity(victim);
			victim.hurtServer(level, player.damageSources().playerAttack(player), 10000);
			check(Town.standing(player).bounty().get().kills() == 1, "a real kill of " + bounty.target() + " near the board counts");
		} else {
			failures.add("couldn't create " + bounty.target());
		}
		while (!Town.standing(player).bounty().get().done()) {
			Town.counted(player, bounty.target(), BOARD.getX() + 10, BOARD.getZ() - 10, dimension);
		}
		int emeraldsBefore = player.getInventory().countItem(Items.EMERALD);
		BountyBoardBlock.use(player, level, BOARD);
		Town.Standing after = Town.standing(player);
		check(after.bounty().isEmpty(), "turning in clears the bounty");
		check(after.reputation() == bounty.reputation(), "turning in pays reputation");
		check(player.getInventory().countItem(Items.EMERALD) - emeraldsBefore == bounty.emeralds(), "turning in pays emeralds");
		BountyBoardBlock.use(player, level, BOARD);
		BountyBoardBlock.use(player, level, BOARD);
		check(Town.standing(player).bounty().isEmpty(), "one bounty a day");
		gathering(player, level);
		escort(player, level);
	}

	/** An escort hands over a pack llama on a lead; bringing it where it's bound pays on the spot. */
	private void escort(ServerPlayer player, ServerLevel level) {
		Town.Active offered = new Town.Active("escort", BountyRules.ESCORT_TARGET, 200, 0, 11, 10, BOARD.getX(), BOARD.getZ(),
			level.dimension().identifier().toString(), "Old Maren");
		Town.Active bound = BountyEscorts.start(player, level, BOARD, offered);
		check(bound != null, "an escort hands over its llama");
		if (bound == null) return;
		check(Math.abs(Math.hypot(bound.destX() - BOARD.getX(), bound.destZ() - BOARD.getZ()) - 200) < 2, "an escort is bound 200 blocks off");
		Town.set(player, Town.standing(player).with(Optional.of(bound)));
		var llama = BountyEscorts.llama(player);
		check(llama != null && llama.isLeashed(), "the pack llama comes on a lead");
		if (llama == null) return;
		escortEmeralds = player.getInventory().countItem(Items.EMERALD);
		escortReputation = Town.standing(player).reputation();
		BountyBoardBlock.use(player, level, BOARD);
		check(Town.standing(player).bounty().isPresent(), "the board doesn't pay an escort");
		// Bound right beside the llama, so the next road check finds it there.
		Town.Standing standing = Town.standing(player);
		Town.Active near = new Town.Active(bound.kind(), bound.target(), bound.needed(), 0, bound.emeralds(), bound.reputation(), bound.boardX(),
			bound.boardZ(), bound.dimension(), bound.name(), llama.getBlockX(), llama.getBlockZ());
		Town.set(player, new Town.Standing(standing.reputation(), standing.lastDay(), Optional.of(near), standing.lastGreatWeek(), standing.lastRaidDay()));
		escortLlama = llama;
	}

	private int escortEmeralds, escortReputation;
	private net.minecraft.world.entity.animal.equine.Llama escortLlama;

	/** After a road check: the escort ended where it was bound, and paid there. */
	private void escortArrived(MinecraftServer server) {
		if (escortLlama == null) return;
		ServerPlayer player = player(server);
		check(Town.standing(player).bounty().isEmpty(), "an escort ends where it's bound");
		check(player.getInventory().countItem(Items.EMERALD) - escortEmeralds == 11
			&& Town.standing(player).reputation() - escortReputation == 10, "an escort pays there");
		check(escortLlama.isRemoved(), "the traveller takes their llama");
	}

	/** A gathering bounty is turned in from the pack: not before the goods are there, and it takes just what it asked. */
	private void gathering(ServerPlayer player, ServerLevel level) {
		Town.set(player, new Town.Standing(20, -1, Optional.of(new Town.Active("gather", "minecraft:bone", 8, 0, 5, 6, BOARD.getX(), BOARD.getZ(),
			level.dimension().identifier().toString(), "")), -1));
		player.getInventory().clearContent();
		player.getInventory().add(new ItemStack(Items.BONE, 5));
		BountyBoardBlock.use(player, level, BOARD);
		check(Town.standing(player).bounty().isPresent(), "a gathering isn't paid without the goods");
		player.getInventory().add(new ItemStack(Items.BONE, 5));
		BountyBoardBlock.use(player, level, BOARD);
		check(Town.standing(player).bounty().isEmpty(), "a gathering is turned in with the goods");
		check(player.getInventory().countItem(Items.BONE) == 2, "it takes just the goods it asked for");
		check(player.getInventory().countItem(Items.EMERALD) == 5 && Town.standing(player).reputation() == 26, "a gathering pays");
	}

	/** Bandits raid the inn: they come in waves, beating every wave pays the defenders, and leaving the inn ends a raid. */
	private void raid(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("fill -34 " + (GROUND - 1) + " -34 34 " + (GROUND - 1) + " 34 minecraft:grass_block");
		server.runCommand("fill -34 " + GROUND + " -34 34 " + (GROUND + 5) + " 34 minecraft:air");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.getAbilities().invulnerable = true;
			player.onUpdateAbilities();
			player.teleportTo(player.level(), 0.5, GROUND, 3.5, Set.<Relative>of(), 180, 0, false);
			player.getInventory().clearContent();
			Town.set(player, new Town.Standing(20, -1, Optional.empty(), -1));
			InnRaid.begin(player, BOARD, player.level().getGameTime() / BountyRules.DAY);
			check(InnRaid.active(BOARD), "a raid begins");
			check(Town.standing(player).lastRaidDay() >= 0, "a raid is remembered on the defender");
		});
		boolean[] seen = {false};
		for (int i = 0; i < 20; i++) {
			context.waitTicks(30);
			boolean[] over = {false};
			server.runOnServer(s -> {
				ServerLevel level = player(s).level();
				for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(BOARD).inflate(48), m -> m.entityTags().contains(InnRaid.TAG))) {
					seen[0] = true;
					mob.discard();
				}
				over[0] = !InnRaid.active(BOARD);
			});
			if (over[0]) break;
		}
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(seen[0], "bandits come");
			check(!InnRaid.active(BOARD), "the raid ends when every wave is beaten");
			BountyRules.Tier known = BountyRules.Tier.KNOWN;
			check(player.getInventory().countItem(Items.EMERALD) == InnRaidRules.emeralds(known), "holding the inn pays emeralds");
			check(Town.standing(player).reputation() == 20 + InnRaidRules.reputation(known), "holding the inn pays reputation");
			InnRaid.begin(player, BOARD, 0);
			player.teleportTo(player.level(), 0.5, GROUND, InnRaidRules.RADIUS + 20, Set.<Relative>of(), 180, 0, false);
		});
		context.waitTicks(5);
		server.runOnServer(s -> {
			check(!InnRaid.active(BOARD), "leaving the inn ends a raid");
			ServerPlayer player = player(s);
			player.getAbilities().invulnerable = false;
			player.onUpdateAbilities();
		});
	}

	private void keepers(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		for (WayfarerKeeper.Role role : WayfarerKeeper.Role.values()) {
			int last = -1;
			for (BountyRules.Tier tier : BountyRules.Tier.values()) {
				MerchantOffers offers = WayfarerKeeper.offers(role, tier, 7);
				check(offers.size() > last, role.id + " sells more at " + tier.id);
				last = offers.size();
			}
		}
		check(WayfarerKeeper.offers(WayfarerKeeper.Role.STABLEMASTER, BountyRules.Tier.FRIEND, 7).stream()
			.anyMatch(o -> o.getResult().is(Town.RIDGEBACK_DEED)), "a friend can buy a ridgeback deed");
		check(WayfarerKeeper.offers(WayfarerKeeper.Role.STABLEMASTER, BountyRules.Tier.STRANGER, 7).stream()
			.noneMatch(o -> o.getResult().is(Town.RIDGEBACK_DEED)), "a stranger can't");

		WayfarerKeeper keeper = Town.KEEPER.create(level, EntitySpawnReason.STRUCTURE);
		if (keeper == null) {
			failures.add("couldn't create a keeper");
			return;
		}
		keeper.snapTo(4.5, GROUND, 4.5, 0, 0);
		keeper.setRole(WayfarerKeeper.Role.STABLEMASTER);
		level.addFreshEntity(keeper);
		keeper.hurtServer(level, player.damageSources().playerAttack(player), 10000);
		check(keeper.isAlive(), "keepers can't be hurt");
		check(!keeper.removeWhenFarAway(10000) && keeper.getDespawnDelay() == 0, "keepers stay");
		keeper.discard();

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Town.RIDGEBACK_DEED));
		Town.RIDGEBACK_DEED.use(level, player, InteractionHand.MAIN_HAND);
		List<RidgebackStag> stags = level.getEntitiesOfClass(RidgebackStag.class, new AABB(player.blockPosition()).inflate(3));
		check(stags.size() == 1, "the deed leads up one stag");
		check(stags.stream().allMatch(s -> s.isTamed() && s.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE)
			&& s.getOwnerReference() != null && s.getOwnerReference().matches(player)), "tame, saddled and the player's");
		check(player.getMainHandItem().isEmpty(), "the deed is used up");
		check(MountContent.RIDGEBACK_STAG != null, "ridgeback registered");
		stags.forEach(RidgebackStag::discard);
		caravan(player, level);
	}

	/** A caravan makes camp near the traveller with its pack llamas, stays a day, and never two close together. */
	private void caravan(ServerPlayer player, ServerLevel level) {
		check(WayfarerKeeper.offers(WayfarerKeeper.Role.CARAVANEER, BountyRules.Tier.STRANGER, 7).stream()
			.filter(o -> o.getResult().getItem() instanceof dev.wildercord.content.RuneItem).count() == CaravanRules.runes(BountyRules.Tier.STRANGER),
			"a caravan carries runes");
		WayfarerKeeper caravaneer = Caravans.send(level, player);
		check(caravaneer != null, "a caravan makes camp");
		if (caravaneer == null) return;
		double distance = Math.sqrt(caravaneer.distanceToSqr(player.getX(), caravaneer.getY(), player.getZ()));
		check(distance >= CaravanRules.SPAWN_NEAR - 1 && distance <= CaravanRules.SPAWN_FAR + 1, "it camps down the road, not on top of you");
		check(caravaneer.role() == WayfarerKeeper.Role.CARAVANEER && caravaneer.getDespawnDelay() == CaravanRules.STAY_TICKS, "it stays a day");
		List<net.minecraft.world.entity.animal.equine.TraderLlama> llamas = level.getEntitiesOfClass(
			net.minecraft.world.entity.animal.equine.TraderLlama.class, caravaneer.getBoundingBox().inflate(8), l -> l.getLeashHolder() == caravaneer);
		check(llamas.size() == CaravanRules.LLAMAS, "with its pack llamas on their leads");
		check(Caravans.send(level, player) == null, "no second caravan close by");
		llamas.forEach(net.minecraft.world.entity.Entity::discard);
		caravaneer.discard();
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
