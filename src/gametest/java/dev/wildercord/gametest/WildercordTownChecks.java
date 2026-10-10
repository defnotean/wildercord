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
			InnRaid.begin(player, BOARD, 0);
			InnRaid.suspend(s);
			check(!InnRaid.active(BOARD) && InnRaid.saved(s) == 1, "a stopping server saves the raid");
			check(player.level().getEntitiesOfClass(Mob.class, new AABB(BOARD).inflate(64), m -> m.entityTags().contains(InnRaid.TAG)).isEmpty(), "its bandits go with the server");
			InnRaid.resume(s);
			check(InnRaid.active(BOARD) && InnRaid.saved(s) == 0, "a starting server takes the raid up again");
			InnRaid.endAll(s);
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
		room(player, level);
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
		hamlet(player, level);
		mageHunter(player, level);
		ritual(player, level);
		twistedRunes(player, level);
		arena(player, level);
		skyMount(player, level);
		deepMounts(player, level);
		mountBonds(player, level);
		predators(player, level);
		giants(player, level);
		rareMeals(player, level);
		elixirs(player, level);
		bladeSmithing(player, level);
		guilds(player, level);
		coopTribulation(player, level);
		mentoring(player, level);
		codex(player, level);
		stats(player);
		safeTelegraphs();
	}

	/** A Room Key used near a keeper lets a room: the traveller wakes there until the stay runs out. */
	private void room(ServerPlayer player, ServerLevel level) {
		check(WayfarerKeeper.offers(WayfarerKeeper.Role.COOK, BountyRules.Tier.STRANGER, 7).stream()
			.anyMatch(o -> o.getResult().is(Town.ROOM_KEY)), "the cook lets rooms");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Town.ROOM_KEY));
		Town.ROOM_KEY.use(level, player, InteractionHand.MAIN_HAND);
		check(player.getMainHandItem().isEmpty(), "the key is handed over");
		check(dev.wildercord.town.InnRooms.room(player).isPresent(), "a key near a keeper lets a room");
		ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
		check(respawn != null && respawn.forced() && respawn.respawnData().pos().equals(player.blockPosition()), "you wake in your room");
		player.setAttached(dev.wildercord.town.InnRooms.ROOM, new dev.wildercord.town.InnRooms.Room(level.getGameTime() - 1,
			net.minecraft.core.GlobalPos.of(level.dimension(), player.blockPosition())));
		dev.wildercord.town.InnRooms.expire(player);
		check(dev.wildercord.town.InnRooms.room(player).isEmpty() && player.getRespawnConfig() == null, "a stay runs out");
	}

	/** A bell with villagers about it is a hamlet with its own standing, earned a little each day; it favours those it knows. */
	private void hamlet(ServerPlayer player, ServerLevel level) {
		net.minecraft.core.BlockPos bell = new net.minecraft.core.BlockPos(-6, GROUND, 6);
		level.setBlockAndUpdate(bell, net.minecraft.world.level.block.Blocks.BELL.defaultBlockState());
		List<net.minecraft.world.entity.npc.villager.Villager> villagers = new java.util.ArrayList<>();
		for (int i = 0; i < dev.wildercord.town.HamletRules.VILLAGERS; i++) {
			var villager = net.minecraft.world.entity.EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
			if (villager == null) continue;
			villager.snapTo(-4.5 + i, GROUND, 8.5, 0, 0);
			villager.setNoAi(true);
			level.addFreshEntity(villager);
			villagers.add(villager);
		}
		check(dev.wildercord.town.Hamlets.bell(level, player.blockPosition()) != null, "a bell with villagers is a hamlet");
		for (int i = 0; i < 40; i++) dev.wildercord.town.Hamlets.earn(player, level, bell, 1);
		check(dev.wildercord.town.Hamlets.standing(player, level, bell).reputation() == dev.wildercord.town.HamletRules.DAILY, "only so much a day");
		player.setAttached(dev.wildercord.town.Hamlets.STANDINGS, java.util.Map.of());
		for (int day = 0; day < 3; day++) {
			player.setAttached(dev.wildercord.town.Hamlets.STANDINGS, java.util.Map.of(level.dimension().identifier() + "|" + bell.asLong(),
				new dev.wildercord.town.Hamlets.Standing(dev.wildercord.town.Hamlets.standing(player, level, bell).reputation(), -1, 0)));
			for (int i = 0; i < 10; i++) dev.wildercord.town.Hamlets.earn(player, level, bell, 1);
		}
		check(dev.wildercord.town.Hamlets.standing(player, level, bell).tier() == BountyRules.Tier.KNOWN, "a few days' help and it knows you");
		check(player.hasEffect(net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE), "a hamlet that knows you treats you as its hero");
		check(Town.standing(player).reputation() != dev.wildercord.town.Hamlets.standing(player, level, bell).reputation()
			|| Town.standing(player).reputation() == 0, "a hamlet's standing is its own");
		player.removeEffect(net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE);
		player.removeAttached(dev.wildercord.town.Hamlets.STANDINGS);
		villagers.forEach(net.minecraft.world.entity.Entity::discard);
		level.removeBlock(bell, false);
	}

	/** A mage-hunter shrugs off half a spell, takes mana with its axe, and leaves casters without five circles alone. */
	private void mageHunter(ServerPlayer player, ServerLevel level) {
		check(dev.wildercord.monster.MageHunters.send(level, player).isEmpty() == !dev.wildercord.monster.MageHunterRules.hunted(
			dev.wildercord.player.Heart.circles(player)), "hunters come only for a heart of five circles");
		level.getEntitiesOfClass(dev.wildercord.monster.MageHunter.class, player.getBoundingBox().inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);
		var hunter = dev.wildercord.monster.MonsterContent.MAGE_HUNTER.create(level, EntitySpawnReason.COMMAND);
		check(hunter != null, "a mage-hunter can be made");
		if (hunter == null) return;
		hunter.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0, 0);
		hunter.setNoAi(true);
		level.addFreshEntity(hunter);
		float full = hunter.getHealth();
		hunter.hurtServer(level, level.damageSources().magic(), 10);
		check(Math.abs(full - hunter.getHealth() - dev.wildercord.monster.MageHunterRules.spellHarm(10)) < 0.01F, "magic hurts it half as much");
		check(hunter.runeboundSpells().isEmpty(), "it never carries a spell");
		if (dev.wildercord.player.Spellbooks.tier(player) != null) {
			float before = dev.wildercord.player.Spellbooks.mana(player);
			dev.wildercord.player.Spellbooks.setMana(player, Math.max(before, dev.wildercord.monster.MageHunterRules.DRAIN));
			float had = dev.wildercord.player.Spellbooks.mana(player);
			hunter.doHurtTarget(level, player);
			check(dev.wildercord.player.Spellbooks.mana(player) <= had - dev.wildercord.monster.MageHunterRules.DRAIN + 0.01F || player.isInvulnerable()
				|| player.isCreative(), "its axe takes mana");
			player.setHealth(player.getMaxHealth());
			dev.wildercord.player.Spellbooks.setMana(player, before);
		}
		hunter.discard();
	}

	/** A Ritual Tablet cycles its rituals, refuses what the heart can't lead, and Bounty ripens the field around it. */
	private void ritual(ServerPlayer player, ServerLevel level) {
		ItemStack tablet = new ItemStack(dev.wildercord.ritual.Rituals.RITUAL_TABLET);
		check(dev.wildercord.ritual.Rituals.ritual(tablet) == dev.wildercord.ritual.RitualRules.Ritual.BOUNTY, "a new tablet is set to Bounty");
		dev.wildercord.ritual.Rituals.setRitual(tablet, dev.wildercord.ritual.RitualRules.Ritual.BOUNTY.next());
		check(dev.wildercord.ritual.Rituals.ritual(tablet) == dev.wildercord.ritual.RitualRules.Ritual.CLEAR_SKIES, "it keeps the ritual it is set to");
		if (!dev.wildercord.ritual.RitualRules.canLead(dev.wildercord.ritual.RitualRules.Ritual.DAWN, dev.wildercord.player.Heart.circles(player))
			&& dev.wildercord.player.Spellbooks.tier(player) != null) {
			check(dev.wildercord.ritual.Rituals.check(level, player, dev.wildercord.ritual.RitualRules.Ritual.DAWN)
				== dev.wildercord.ritual.Rituals.Result.LOW_CIRCLE, "Dawn waits for the tenth circle");
		}
		BlockPos crop = player.blockPosition().offset(4, 0, 4);
		BlockPos soil = crop.below();
		var oldSoil = level.getBlockState(soil);
		var oldCrop = level.getBlockState(crop);
		level.setBlockAndUpdate(soil, net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState());
		level.setBlockAndUpdate(crop, net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState());
		check(dev.wildercord.ritual.Rituals.bounty(level, player.blockPosition()) >= 1, "Bounty finds the wheat");
		var grown = level.getBlockState(crop);
		check(grown.getBlock() instanceof net.minecraft.world.level.block.CropBlock c && c.getAge(grown) > 0, "and ripens it");
		level.setBlockAndUpdate(crop, oldCrop);
		level.setBlockAndUpdate(soil, oldSoil);
		var weather = level.getWeatherData();
		boolean raining = weather.isRaining();
		dev.wildercord.ritual.Rituals.weather(level, true);
		check(level.getWeatherData().isThundering(), "Call Storm brings thunder");
		dev.wildercord.ritual.Rituals.weather(level, false);
		check(!level.getWeatherData().isRaining() && level.getWeatherData().getClearWeatherTime() == dev.wildercord.ritual.RitualRules.CLEAR_TICKS,
			"Clear Skies holds the sky clear");
		if (raining) weather.setRaining(true);
		dev.wildercord.ritual.Rituals.sanctuary(level, player.position());
		check(dev.wildercord.ritual.Rituals.warded(level, player.position()), "Sanctuary wards where it was worked");
		check(!dev.wildercord.ritual.Rituals.warded(level, player.position().add(dev.wildercord.ritual.RitualRules.SANCTUARY_RADIUS + 4, 0, 0)),
			"and not beyond its edge");
		dev.wildercord.ritual.Rituals.suspendWards(level.getServer());
		check(dev.wildercord.ritual.Rituals.wards() == 0, "a stopping server saves its Sanctuaries");
		dev.wildercord.ritual.Rituals.resumeWards(level.getServer());
		check(dev.wildercord.ritual.Rituals.warded(level, player.position()), "a starting server holds them again");
		dev.wildercord.ritual.Rituals.liftWards();
	}

	/** A twisted rune found in a chest: learning it twists the rune, a plain copy sneak-used smooths it out. */
	private void twistedRunes(ServerPlayer player, ServerLevel level) {
		var found = dev.wildercord.content.TwistedRunes.randomEffect(new java.util.Random(7));
		check(found.isPresent() && found.get().family() == dev.wildercord.spell.RuneFamily.EFFECT, "a chest's twisted rune is an effect rune");
		if (found.isEmpty()) return;
		var rune = found.get();
		var flawed = dev.wildercord.spell.RuneTwistRules.Twist.FLAWED;
		var old = dev.wildercord.player.RuneTwists.all(player);
		ItemStack held = player.getMainHandItem().copy();
		ItemStack twisted = dev.wildercord.content.TwistedRunes.stack(rune, flawed);
		check(dev.wildercord.content.TwistedRunes.twistOf(twisted) == flawed, "the twist is cut into the item");
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, twisted);
		twisted.getItem().use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
		check(dev.wildercord.player.RuneTwists.twist(player, rune.id()) == flawed && dev.wildercord.player.Spellbooks.knows(player, rune.id()),
			"learning it twists the rune");
		check(Math.abs(dev.wildercord.player.RuneTwists.power(player, rune) - flawed.power) < 1e-9, "a flawed rune is weaker");
		check(dev.wildercord.player.RuneTwists.costFactor(player, List.of(rune)) < 1.0, "and its spell is cheaper");
		ItemStack plain = dev.wildercord.content.RuneItem.stack(rune);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, plain);
		player.setShiftKeyDown(true);
		plain.getItem().use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
		player.setShiftKeyDown(false);
		check(dev.wildercord.player.RuneTwists.twist(player, rune.id()) == null, "a plain copy smooths it out");
		player.setAttached(dev.wildercord.player.WildercordAttachments.RUNE_TWISTS, old);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
	}

	/** The arena stone takes a caster's place, and a ranked bout moves both ratings on the season's ladder. */
	private void arena(ServerPlayer player, ServerLevel level) {
		BlockPos pos = player.blockPosition().offset(2, 0, 0);
		var old = level.getBlockState(pos);
		level.setBlockAndUpdate(pos, dev.wildercord.content.WildercordBlocks.ARENA_STONE.defaultBlockState());
		check(level.getBlockState(pos).is(dev.wildercord.content.WildercordBlocks.ARENA_STONE), "an arena stone stands");
		player.setShiftKeyDown(true);
		dev.wildercord.duel.Arena.use(level, pos, player);
		player.setShiftKeyDown(false);
		check(!dev.wildercord.duel.Duels.inDuel(player), "reading the ladder starts nothing");
		level.setBlockAndUpdate(pos, old);
		java.util.UUID one = java.util.UUID.randomUUID(), two = java.util.UUID.randomUUID();
		var ladder = dev.wildercord.duel.ArenaLadder.of(level.getServer());
		int[] change = dev.wildercord.duel.Arena.record(level, one, "Winner", two, "Loser", 1.0);
		check(change[0] > 0 && change[1] < 0, "the winner climbs and the loser falls");
		int season = dev.wildercord.duel.Arena.season(level);
		var won = ladder.standing(one, "Winner", season);
		check(won.rating() == dev.wildercord.duel.ArenaRules.START + change[0] && won.wins() == 1, "the ladder keeps the win");
		check(ladder.top(season, 50).stream().anyMatch(s -> s.name().equals("Winner")), "and names the winner");
		ladder.forget(one);
		ladder.forget(two);
	}

	/** The bridle calls a skyray of your own and seats you on it; stepping off drifts you down, and it goes back to the sky. */
	private void skyMount(ServerPlayer player, ServerLevel level) {
		check(dev.wildercord.wildlife.SkyMountRules.rewards(10), "the Tenth Circle's tribulation earns the bridle");
		var ray = dev.wildercord.wildlife.BondedSkyray.call(level, player);
		check(ray != null, "the bridle calls a skyray down");
		if (ray == null) return;
		check(player.getVehicle() == ray && player.getUUID().equals(ray.owner()), "and seats its owner on it");
		check(ray.getControllingPassenger() == player, "who steers it");
		check(!ray.hurtServer(level, level.damageSources().playerAttack(player), 4F), "its rider can't strike it");
		check(dev.wildercord.wildlife.BondedSkyray.call(level, player) == null, "one can't be called while riding");
		player.stopRiding();
		check(player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING), "stepping off drifts you down");
		player.removeEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);
		ray.leave(level);
		check(ray.isRemoved(), "and it goes back to the sky");
	}

	/** The bobcat's kin: a lynx tamed by raw rabbit whose bite chills, a cougar tamed by red meat that marks monsters, and kittens of their own kind. */
	private void predators(ServerPlayer player, ServerLevel level) {
		var spawn = net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED;
		var lynx = dev.wildercord.wildlife.PredatorContent.FROST_LYNX.create(level, spawn);
		var cougar = dev.wildercord.wildlife.PredatorContent.DUNE_COUGAR.create(level, spawn);
		var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, spawn);
		check(lynx != null && cougar != null && zombie != null, "a lynx, a cougar and a zombie");
		check(lynx.tames(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RABBIT)) && !lynx.tames(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COD)),
			"raw rabbit, not fish, wins a lynx over");
		check(cougar.tames(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEEF)) && !cougar.tames(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RABBIT)),
			"red meat wins a cougar over");
		check(!lynx.canFreeze(), "cold never touches a lynx");
		net.minecraft.core.BlockPos at = player.blockPosition().above(40);
		for (var mob : new net.minecraft.world.entity.Mob[] {lynx, cougar, zombie}) {
			mob.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			mob.setNoGravity(true);
			mob.setNoAi(true);
			level.addFreshEntity(mob);
		}
		zombie.setPos(at.getX() + 3.5, at.getY(), at.getZ() + 0.5);
		lynx.chill(zombie);
		check(zombie.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS) && zombie.getTicksFrozen() > 0, "a lynx's bite chills");
		lynx.tame(player);
		cougar.tame(player);
		check(cougar.mark(level, player) == 0, "nothing near the player to mark yet");
		zombie.setPos(player.getX() + 4, player.getY(), player.getZ());
		check(cougar.mark(level, player) >= 1 && zombie.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "a cougar marks a monster near its owner");
		var kitten = lynx.getBreedOffspring(level, lynx);
		check(kitten instanceof dev.wildercord.wildlife.FrostLynx cub && cub.isOwnedBy(player), "a lynx raises a lynx that's yours");
		check(!lynx.canMate(cougar), "a lynx and a cougar don't pair");
		lynx.discard();
		cougar.discard();
		zombie.discard();
	}

	/** A caster with no party in the ring faces their tribulation alone, the storm its lone size. */
	private void coopTribulation(ServerPlayer player, ServerLevel level) {
		dev.wildercord.cast.events.Tribulation.cancel(player);
		if (!dev.wildercord.cast.events.Tribulation.begin(player, 5) || !dev.wildercord.cast.events.Tribulation.active(player)) return;
		check(dev.wildercord.cast.events.Tribulation.engaged(player), "the caster is in their own tribulation");
		check(dev.wildercord.cast.events.Tribulation.party(player) == 0, "no party, no allies");
		dev.wildercord.cast.events.Tribulation.cancel(player);
		check(!dev.wildercord.cast.events.Tribulation.engaged(player), "a cancelled tribulation lets the caster go");
	}

	/** The stats page reads the player as they are: their circles and the whole field guide, time played first. */
	/** The colour-blind-safe swap keeps a warning red and a ward green apart and leaves the dark-particle flag alone. */
	private void safeTelegraphs() {
		int warning = dev.wildercord.presentation.SafeColourRules.safe(0xFF3030), ward = dev.wildercord.presentation.SafeColourRules.safe(0x40D060);
		check(warning != ward, "safe telegraphs keep red and green apart");
		check((dev.wildercord.presentation.SafeColourRules.safe(0x01FF3030) & 0xFF000000) == 0x01000000, "safe telegraphs keep a particle's dark flag");
	}

	private void stats(ServerPlayer player) {
		var snapshot = dev.wildercord.player.Stats.snapshot(player);
		check(snapshot.circles() == dev.wildercord.player.Heart.circles(player), "the stats page shows the circles formed");
		check(snapshot.guide() == dev.wildercord.spell.FieldGuide.all().size() && snapshot.met() <= snapshot.guide(), "the stats page counts the whole field guide");
		var rows = dev.wildercord.player.StatsRules.rows(snapshot);
		check(rows.getFirst().key().equals("play_time") && rows.getLast().key().equals("deaths"), "the stats page runs from time played to deaths");
	}

	/** Slaying a field-guide creature tallies it in the codex, ten earn it studied, and other creatures aren't counted. */
	private void codex(ServerPlayer player, ServerLevel level) {
		var before = dev.wildercord.player.Codex.tally(player);
		player.setAttached(dev.wildercord.player.WildercordAttachments.CODEX, java.util.Map.of());
		var stag = dev.wildercord.wildlife.Wildlife.LUMEN_STAG.create(level, EntitySpawnReason.COMMAND);
		var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
		check(stag != null && zombie != null, "a stag and a zombie to slay");
		dev.wildercord.player.Codex.slain(player, stag);
		check(dev.wildercord.player.Codex.kills(player, "wildercord:lumen_stag") == 1, "a slain stag is tallied");
		for (int i = 1; i < dev.wildercord.spell.CodexRules.STUDIED; i++) dev.wildercord.player.Codex.slain(player, stag);
		check(dev.wildercord.spell.CodexRules.rank(dev.wildercord.player.Codex.kills(player, "wildercord:lumen_stag")) == dev.wildercord.spell.CodexRules.Rank.STUDIED,
			"ten stags slain is a stag studied");
		dev.wildercord.player.Codex.slain(player, zombie);
		check(dev.wildercord.player.Codex.tally(player).size() == 1, "a creature outside the field guide isn't tallied");
		stag.discard();
		zombie.discard();
		player.setAttached(dev.wildercord.player.WildercordAttachments.CODEX, before);
	}

	/** An apprenticeship is saved with the world, teaches only near the mentor, and ends at the 10th circle. */
	private void mentoring(ServerPlayer player, ServerLevel level) {
		var ledger = dev.wildercord.guild.Mentors.ledger(level.getServer());
		java.util.UUID mentor = new java.util.UUID(0x5EED, 0x7EAC);
		ledger.end(mentor, player.getUUID());
		ledger.take(mentor, player.getUUID());
		check(mentor.equals(ledger.book().mentorOf(player.getUUID())), "the apprentice is written in the book");
		check(ledger.isDirty(), "the book is saved after a change");
		check(dev.wildercord.guild.Mentors.gain(player) == 1.0, "a mentor who isn't here teaches nothing");
		var ops = level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
		var saved = dev.wildercord.guild.Mentors.Ledger.CODEC.encodeStart(ops, ledger).getOrThrow();
		var loaded = dev.wildercord.guild.Mentors.Ledger.CODEC.parse(ops, saved).getOrThrow();
		check(mentor.equals(loaded.book().mentorOf(player.getUUID())), "an apprenticeship outlasts a reload");
		dev.wildercord.guild.Mentors.formed(player, dev.wildercord.guild.MentorRules.GRADUATE_AT - 1);
		check(ledger.book().mentorOf(player.getUUID()) != null, "the 9th circle is still an apprentice's");
		dev.wildercord.guild.Mentors.formed(player, dev.wildercord.guild.MentorRules.GRADUATE_AT);
		check(ledger.book().mentorOf(player.getUUID()) == null, "the 10th circle graduates the apprentice");
	}

	/** A coven is saved with the world, outlasts a reload of its ledger, and training alone earns no bonus. */
	private void guilds(ServerPlayer player, ServerLevel level) {
		var kind = dev.wildercord.guild.GuildRules.Kind.COVEN;
		var ledger = dev.wildercord.guild.Guilds.ledger(level.getServer());
		ledger.change(r -> r.leave(player.getUUID(), kind));
		check(ledger.change(r -> r.found(player.getUUID(), kind, "Test Circle")) == dev.wildercord.guild.GuildRules.Result.OK, "a coven is founded");
		check(dev.wildercord.guild.Guilds.of(player, kind) != null && dev.wildercord.guild.Guilds.of(player, kind).name().equals("Test Circle"), "its founder stands in it");
		check(dev.wildercord.guild.Guilds.of(player, dev.wildercord.guild.GuildRules.Kind.GUILD) == null, "a coven is not a guild");
		check(dev.wildercord.guild.Guilds.bonus(player, kind) == 1.0, "training alone earns no coven bonus");
		check(ledger.isDirty(), "the ledger is saved after a change");
		var ops = level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
		var saved = dev.wildercord.guild.Guilds.Ledger.CODEC.encodeStart(ops, ledger).getOrThrow();
		var loaded = dev.wildercord.guild.Guilds.Ledger.CODEC.parse(ops, saved).getOrThrow();
		check(loaded.roster().named("test circle") != null && loaded.roster().named("test circle").leader().equals(player.getUUID()), "a coven outlasts a reload");
		ledger.change(r -> r.disband(player.getUUID(), kind));
		check(dev.wildercord.guild.Guilds.of(player, kind) == null, "a disbanded coven is gone");
	}

	/** A Master's fall leaves Master's Steel; an anvil tempers only a bonded blade with it. */
	private void bladeSmithing(ServerPlayer player, ServerLevel level) {
		check(new ItemStack(dev.wildercord.aura.BladeSmithing.MASTER_STEEL).getRarity() == net.minecraft.world.item.Rarity.EPIC, "Master's Steel looks it");
		check(dev.wildercord.aura.BladeSmithing.temper(new ItemStack(Items.IRON_SWORD), new ItemStack(dev.wildercord.aura.BladeSmithing.MASTER_STEEL, 64), player) == null,
			"an unbonded sword takes no temper");
		check(dev.wildercord.aura.BladeSmithing.temper(new ItemStack(Items.IRON_SWORD)) == 0, "a plain sword is untempered");
		int before = player.getInventory().countItem(dev.wildercord.aura.BladeSmithing.MASTER_STEEL);
		dev.wildercord.aura.BladeSmithing.reward(player, true);
		check(player.getInventory().countItem(dev.wildercord.aura.BladeSmithing.MASTER_STEEL) == before + 2, "a Master's first fall leaves two slivers of steel");
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).is(dev.wildercord.aura.BladeSmithing.MASTER_STEEL)) player.getInventory().setItem(i, ItemStack.EMPTY);
		}
	}

	/** A mana elixir trades one side of mana for another, and brews from a residue harvest. */
	private void elixirs(ServerPlayer player, ServerLevel level) {
		var before = dev.wildercord.player.Spellbooks.cord(player).copy();
		dev.wildercord.player.Spellbooks.setCord(player, new net.minecraft.world.item.ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));
		var plain = dev.wildercord.player.Mana.of(player);
		player.addEffect(new net.minecraft.world.effect.MobEffectInstance(dev.wildercord.content.WildercordEffects.DEEP_WELL, 200));
		var deep = dev.wildercord.player.Mana.of(player);
		check(deep.max() > plain.max() && deep.regen() < plain.regen(), "a Deep Well elixir holds more and refills slower");
		player.removeEffect(dev.wildercord.content.WildercordEffects.DEEP_WELL);
		player.addEffect(new net.minecraft.world.effect.MobEffectInstance(dev.wildercord.content.WildercordEffects.TORRENT, 200));
		var torrent = dev.wildercord.player.Mana.of(player);
		check(torrent.max() < plain.max() && torrent.regen() > plain.regen(), "a Torrent elixir refills faster and holds less");
		player.removeEffect(dev.wildercord.content.WildercordEffects.TORRENT);
		dev.wildercord.player.Spellbooks.setCord(player, before);
		check(level.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
			dev.wildercord.Wildercord.id("brewing/potion_awkward_hourglass_sand"))).isPresent(), "Condensing brews from hourglass sand");
	}

	/** A rare meal is a rare item that feeds like a feast and carries stronger buffs. */
	private void rareMeals(ServerPlayer player, ServerLevel level) {
		var roast = new net.minecraft.world.item.ItemStack(dev.wildercord.cooking.Meals.MEALS.get("titans_roast"));
		check(roast.getRarity() == net.minecraft.world.item.Rarity.EPIC, "a rare meal looks it");
		check(dev.wildercord.cooking.CookingRules.recipe("titans_roast").needs().stream()
			.anyMatch(n -> n.item().equals("wildercord:giant_heart")), "the titan's roast needs a giant's heart");
		player.removeEffect(net.minecraft.world.effect.MobEffects.HEALTH_BOOST);
		roast.finishUsingItem(level, player);
		var boost = player.getEffect(net.minecraft.world.effect.MobEffects.HEALTH_BOOST);
		check(boost != null && boost.getAmplifier() == 1, "eating it gives Health Boost II");
		player.removeEffect(net.minecraft.world.effect.MobEffects.HEALTH_BOOST);
		player.removeEffect(net.minecraft.world.effect.MobEffects.RESISTANCE);
	}

	/** A giant is a monster of the wilds grown huge and tough; its stomp throws what's near, its bar shows, and it drops its heart. */
	private void giants(ServerPlayer player, ServerLevel level) {
		var at = new net.minecraft.world.phys.Vec3(player.getX() + 8, player.getY() + 40, player.getZ());
		var giant = dev.wildercord.monster.Giants.make(level, dev.wildercord.monster.GiantRules.Kind.ELDER, at, 6, false);
		check(giant != null, "a giant wakes");
		if (giant == null) return;
		giant.setNoAi(true);
		giant.setNoGravity(true);
		level.addFreshEntity(giant);
		check(dev.wildercord.monster.Giants.is(giant) && giant.isPersistenceRequired() && giant.hasCustomName(), "a named giant that stays");
		check(giant.getScale() > 2.5F, "three times its kind's size");
		check(giant.getMaxHealth() > 200, "and far tougher");
		var matriarch = dev.wildercord.monster.Giants.make(level, dev.wildercord.monster.GiantRules.Kind.MATRIARCH, at, 6, false);
		check(matriarch instanceof dev.wildercord.monster.Gloomstalker g && g.alpha()
			&& g.variant() == dev.wildercord.monster.MonsterVariantRules.Variant.FROST, "the matriarch is a frost alpha gloomstalker");
		var pig = net.minecraft.world.entity.EntityTypes.PIG.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		check(pig != null, "a pig underfoot");
		if (pig == null) return;
		pig.snapTo(giant.getX() + giant.getBbWidth() / 2 + 1, giant.getY(), giant.getZ(), 0, 0);
		pig.setNoGravity(true);
		level.addFreshEntity(pig);
		float before = pig.getHealth();
		check(dev.wildercord.monster.Giants.stomp(giant) >= 1 && pig.getHealth() < before, "its stomp hurts what's near");
		check(pig.getDeltaMovement().y > 0, "and throws it");
		dev.wildercord.monster.Giants.update(giant);
		var bar = dev.wildercord.monster.Giants.bar(giant);
		check(bar != null && bar.getPlayers().contains(player), "a near player sees its bar");
		giant.kill(level);
		var hearts = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, giant.getBoundingBox().inflate(4),
			item -> item.getItem().is(dev.wildercord.monster.Giants.GIANT_HEART));
		check(!hearts.isEmpty(), "it drops its heart");
		check(bar == null || bar.getPlayers().isEmpty(), "its bar goes when it falls");
		hearts.forEach(net.minecraft.world.entity.Entity::discard);
		pig.discard();
	}

	/** Riding a tame mount grows a bond that quickens it, teaches it to dash and then to strike as it lands; it wears barding. */
	private void mountBonds(ServerPlayer player, ServerLevel level) {
		var stag = dev.wildercord.wildlife.MountContent.RIDGEBACK_STAG.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		check(stag != null, "a stag to bond with");
		if (stag == null) return;
		stag.snapTo(player.getX(), player.getY() + 40, player.getZ(), 0, 0);
		stag.setNoGravity(true);
		stag.setTamed(true);
		stag.setOwner(player);
		level.addFreshEntity(stag);
		check(dev.wildercord.wildlife.MountBonds.owns(player, stag), "its tamer owns it");
		double speed = stag.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
		float health = stag.getMaxHealth();
		var bond = dev.wildercord.wildlife.MountBonds.grow(level, player, stag, 1400);
		check(bond.level() == 3 && dev.wildercord.wildlife.MountBonds.bond(stag, player).points() == 1400, "riding grows a bond");
		check(stag.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) > speed, "a bond quickens it");
		check(stag.getMaxHealth() == health + 6, "and makes it hardier");
		dev.wildercord.wildlife.MountBonds.dash(level, stag);
		check(stag.getDeltaMovement().horizontalDistance() > 1.0, "at level 3 it dashes");
		stag.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		check(zombie != null, "something to strike");
		if (zombie == null) return;
		zombie.snapTo(stag.getX() + 2, stag.getY(), stag.getZ(), 0, 0);
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		level.addFreshEntity(zombie);
		dev.wildercord.wildlife.MountBonds.grow(level, player, stag, 6000);
		check(dev.wildercord.wildlife.MountBonds.bond(stag, player).level() == dev.wildercord.wildlife.MountBondRules.MAX_LEVEL, "the bond tops out");
		check(dev.wildercord.wildlife.MountBonds.strike(level, player, stag) >= 1 && zombie.getHealth() < zombie.getMaxHealth(), "at level 5 its landing strikes");
		zombie.discard();
		stag.setItemSlot(net.minecraft.world.entity.EquipmentSlot.BODY, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HORSE_ARMOR));
		check(stag.isWearingBodyArmor(), "a stag wears barding");
		stag.setOwner(null);
		stag.discard();
	}

	/** A saddled delver digs the soft ground ahead of its rider but not stone; a reefback takes kelp and never drowns its rider. */
	private void deepMounts(ServerPlayer player, ServerLevel level) {
		var stag = dev.wildercord.wildlife.MountContent.RIDGEBACK_STAG.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		check(stag != null, "a ridgeback stag can be made");
		if (stag == null) return;
		stag.setTamed(true);
		stag.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE));
		check(stag.isSaddled(), "a tame stag takes a saddle");
		stag.discard();
		BlockPos base = player.blockPosition().above(40);
		var mole = dev.wildercord.wildlife.MountContent.DELVER_MOLE.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		check(mole != null, "a delver mole can be made");
		if (mole == null) return;
		mole.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
		mole.setNoGravity(true);
		mole.setTamed(true);
		mole.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE));
		level.addFreshEntity(mole);
		BlockPos ahead = base.south();
		level.setBlockAndUpdate(ahead, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
		level.setBlockAndUpdate(ahead.above(), net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState());
		level.setBlockAndUpdate(ahead.above(2), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
		net.minecraft.world.phys.Vec3 home = player.position();
		player.startRiding(mole);
		player.setYRot(0);
		player.setXRot(0);
		check(mole.isSaddled() && mole.getControllingPassenger() == player, "a saddled delver is steered by its rider");
		int dug = mole.burrow(level, player);
		check(dug >= 2 && level.getBlockState(ahead).isAir() && level.getBlockState(ahead.above()).isAir(), "it digs the dirt and gravel ahead");
		check(level.getBlockState(ahead.above(2)).is(net.minecraft.world.level.block.Blocks.STONE), "but not stone");
		player.stopRiding();
		level.setBlockAndUpdate(ahead.above(2), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
		level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, mole.getBoundingBox().inflate(4)).forEach(net.minecraft.world.entity.Entity::discard);
		mole.discard();

		var turtle = dev.wildercord.wildlife.MountContent.REEFBACK_TURTLE.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		check(turtle != null, "a reefback turtle can be made");
		if (turtle == null) return;
		turtle.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
		turtle.setNoGravity(true);
		level.addFreshEntity(turtle);
		check(turtle.isFood(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.KELP)), "a reefback eats kelp");
		int temper = turtle.getTemper();
		turtle.fedFood(player, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.KELP));
		check(turtle.getTemper() > temper, "which calms a wild one");
		check(turtle.canBreatheUnderwater() && !turtle.dismountsUnderwater(), "it never throws its rider off under water");
		double[] dive = dev.wildercord.wildlife.ReefbackRules.swim(0, 45, 1, 0);
		check(dive[1] < 0 && dive[2] > 0, "and swims down the way its rider looks");
		turtle.discard();
		player.teleportTo(home.x, home.y, home.z);
	}

	/** A caravan makes camp near the traveller with its pack llamas, stays a day, and never two close together. */
	private void caravan(ServerPlayer player, ServerLevel level) {
		check(WayfarerKeeper.offers(WayfarerKeeper.Role.CARAVANEER, BountyRules.Tier.STRANGER, 7).stream()
			.filter(o -> o.getResult().getItem() instanceof dev.wildercord.content.RuneItem).count() == CaravanRules.runes(BountyRules.Tier.STRANGER),
			"a caravan carries runes");
		check(WayfarerKeeper.offers(WayfarerKeeper.Role.CARAVANEER, BountyRules.Tier.STRANGER, 7).stream()
			.anyMatch(o -> o.getResult().is(dev.wildercord.cooking.Meals.WAYFARER_SAFFRON)), "and the saffron rare meals need");
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
