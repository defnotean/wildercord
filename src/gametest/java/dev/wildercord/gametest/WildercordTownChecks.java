package dev.wildercord.gametest;

import dev.wildercord.town.BountyBoardBlock;
import dev.wildercord.town.BountyRules;
import dev.wildercord.town.Town;
import dev.wildercord.town.WayfarerKeeper;
import dev.wildercord.wildlife.MountContent;
import dev.wildercord.wildlife.RidgebackStag;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
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
			server.runOnServer(this::keepers);
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
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
