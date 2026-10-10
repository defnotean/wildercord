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
