package dev.wildercord.town;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Escort bounties (0.13): the board hands you a traveller's pack llama on a lead, and you bring it to where they're bound, 160 to
 * 240 blocks off. Now and then an ambush comes for the llama on the road. It arrives and the traveller pays you there; it dies
 * and the bounty is lost. Only the hunter who took the bounty can lead it home. Ambushes only come for a llama on the move, at
 * most {@link BountyRules#AMBUSH_MAX} a road, and the traveller takes the llama back (dropping what's in its chest) when the
 * escort is abandoned or done: it's never yours to keep.
 */
public final class BountyEscorts {
	private BountyEscorts() {}

	/** How close to where it's bound the llama has to come (blocks, across the ground). */
	public static final double ARRIVE = 8;
	/** The chance each second on the road that an ambush comes, and how far from the board and the end the road is safe. */
	public static final double AMBUSH_CHANCE = 1.0 / 45;
	public static final double SAFE = 32;
	private static final List<EntityType<? extends Mob>> AMBUSHERS = List.of(EntityTypes.ZOMBIE, EntityTypes.PILLAGER, EntityTypes.VINDICATOR,
		EntityTypes.HUSK);

	/** Each hunter's road so far, not saved: where their llama was a second ago and how many ambushes have come. */
	private static final class Road {
		double x = Double.NaN, z;
		int ambushes;
	}

	private static final Map<UUID, Road> ROADS = new HashMap<>();
	/** Escort llamas loaded this tick, checked once the tick is over (a bounty being taken stores itself after its llama spawns). */
	private static final List<Llama> LOADED = new ArrayList<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!LOADED.isEmpty()) {
				for (Llama llama : List.copyOf(LOADED)) reclaim(llama);
				LOADED.clear();
			}
			if (server.getTickCount() % 20 != 0) return;
			ROADS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
			for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player, server.getTickCount());
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity.level() instanceof ServerLevel level)) return;
			for (String tag : entity.entityTags()) {
				if (!tag.startsWith(Town.ESCORT_TAG)) continue;
				UUID owner;
				try {
					owner = UUID.fromString(tag.substring(Town.ESCORT_TAG.length()));
				} catch (IllegalArgumentException e) {
					return;
				}
				ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
				if (player == null) return;
				ROADS.remove(owner);
				Town.Standing standing = Town.standing(player);
				if (standing.bounty().isPresent() && standing.bounty().get().type() == BountyRules.Kind.ESCORT) {
					Town.set(player, standing.with(Optional.empty()));
					player.sendSystemMessage(Component.translatable("message.wildercord.bounty.escort_lost", Town.targetName(standing.bounty().get()))
						.withStyle(ChatFormatting.RED));
				}
				return;
			}
		});
		// A llama whose escort was abandoned or lost while it was out of reach goes back to its traveller when it next loads.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Llama llama && llama.entityTags().stream().anyMatch(tag -> tag.startsWith(Town.ESCORT_TAG))) LOADED.add(llama);
		});
	}

	private static void reclaim(Llama llama) {
		if (!llama.isAlive() || !(llama.level() instanceof ServerLevel level)) return;
		for (String tag : llama.entityTags()) {
			if (!tag.startsWith(Town.ESCORT_TAG)) continue;
			UUID owner;
			try {
				owner = UUID.fromString(tag.substring(Town.ESCORT_TAG.length()));
			} catch (IllegalArgumentException e) {
				return;
			}
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
			if (player != null && !escorting(player)) release(level, llama);
			return;
		}
	}

	private static boolean escorting(ServerPlayer player) {
		Optional<Town.Active> bounty = Town.standing(player).bounty();
		return bounty.isPresent() && bounty.get().type() == BountyRules.Kind.ESCORT;
	}

	/** The traveller takes their llama back: what's in its chest falls where it stands, and it's gone. */
	public static void release(ServerLevel level, Llama llama) {
		for (int i = 0; i < llama.getInventorySize(); i++) {
			var slot = llama.getSlot(AbstractHorse.INVENTORY_SLOT_OFFSET + i);
			ItemStack stack = slot.get();
			if (stack.isEmpty()) continue;
			llama.spawnAtLocation(level, stack.copy());
			slot.set(ItemStack.EMPTY);
		}
		level.sendParticles(ParticleTypes.POOF, llama.getX(), llama.getY() + 0.8, llama.getZ(), 12, 0.5, 0.5, 0.5, 0.02);
		llama.discard();
	}

	/** Abandons {@code player}'s escort: the traveller takes the llama back if it's about (or when it next loads). */
	public static void abandon(ServerPlayer player) {
		ROADS.remove(player.getUUID());
		Llama llama = llama(player);
		if (llama != null) release(player.level(), llama);
	}

	/** Hands {@code player} the escort's pack llama by the board; returns the bounty bound for its end, or null with no room. */
	public static Town.Active start(ServerPlayer player, ServerLevel level, BlockPos board, Town.Active bounty) {
		Llama llama = EntityTypes.LLAMA.create(level, EntitySpawnReason.EVENT);
		if (llama == null) return null;
		BlockPos at = player.blockPosition();
		llama.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot(), 0);
		if (!level.noCollision(llama)) {
			at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, board.relative(Direction.SOUTH, 3));
			llama.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
			if (!level.noCollision(llama)) return null;
		}
		double angle = level.getRandom().nextDouble() * Math.PI * 2;
		int destX = board.getX() + (int) Math.round(Math.cos(angle) * bounty.needed());
		int destZ = board.getZ() + (int) Math.round(Math.sin(angle) * bounty.needed());
		llama.setTamed(true);
		llama.setChest(true);
		llama.setPersistenceRequired();
		var health = llama.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.addOrReplacePermanentModifier(new AttributeModifier(dev.wildercord.Wildercord.id("escort_health"), 40 - health.getBaseValue(),
				AttributeModifier.Operation.ADD_VALUE));
			llama.setHealth(llama.getMaxHealth());
		}
		llama.setCustomName(Component.translatable("town.wildercord.pack_llama", bounty.name()).withColor(0xE8C870));
		llama.setCustomNameVisible(true);
		llama.addTag(Town.ESCORT_TAG + player.getUUID());
		level.addFreshEntity(llama);
		llama.setLeashedTo(player, true);
		ROADS.remove(player.getUUID());
		return bounty.bound(destX, destZ);
	}

	/** The escort's llama near {@code player}, if it's about. */
	public static Llama llama(ServerPlayer player) {
		String tag = Town.ESCORT_TAG + player.getUUID();
		List<Llama> found = player.level().getEntitiesOfClass(Llama.class, player.getBoundingBox().inflate(64), l -> l.isAlive() && l.entityTags().contains(tag));
		return found.isEmpty() ? null : found.getFirst();
	}

	private static void tick(ServerPlayer player, int tickCount) {
		Town.Standing standing = Town.standing(player);
		if (standing.bounty().isEmpty()) return;
		Town.Active bounty = standing.bounty().get();
		if (bounty.type() != BountyRules.Kind.ESCORT || !bounty.dimension().equals(player.level().dimension().identifier().toString())) return;
		ServerLevel level = player.level();
		Llama llama = llama(player);
		if (llama == null) return;
		double toEnd = Math.hypot(llama.getX() - (bounty.destX() + 0.5), llama.getZ() - (bounty.destZ() + 0.5));
		if (toEnd <= ARRIVE) {
			arrive(player, level, standing, bounty, llama);
			return;
		}
		if (tickCount % 200 == 0) {
			Direction way = Direction.getApproximateNearest(bounty.destX() - llama.getX(), 0, bounty.destZ() - llama.getZ());
			player.sendOverlayMessage(Component.translatable("message.wildercord.bounty.escort_road", Town.targetName(bounty), (int) toEnd,
				Component.translatable("town.wildercord.way." + way.getSerializedName())).withStyle(ChatFormatting.YELLOW));
		}
		double fromBoard = Math.hypot(llama.getX() - bounty.boardX(), llama.getZ() - bounty.boardZ());
		Road road = ROADS.computeIfAbsent(player.getUUID(), id -> new Road());
		double moved = Double.isNaN(road.x) ? 0 : Math.hypot(llama.getX() - road.x, llama.getZ() - road.z);
		road.x = llama.getX();
		road.z = llama.getZ();
		if (fromBoard > SAFE && toEnd > SAFE && level.getDifficulty() != Difficulty.PEACEFUL && BountyRules.ambushMayRoll(moved, road.ambushes)
			&& level.getRandom().nextDouble() < AMBUSH_CHANCE && ambush(player, level, llama)) {
			road.ambushes++;
		}
	}

	private static boolean ambush(ServerPlayer player, ServerLevel level, Llama llama) {
		RandomSource random = level.getRandom();
		int count = 2 + random.nextInt(2);
		double side = random.nextDouble() * Math.PI * 2;
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			EntityType<? extends Mob> type = AMBUSHERS.get(random.nextInt(AMBUSHERS.size()));
			if (!(type.create(level, EntitySpawnReason.EVENT) instanceof Mob mob)) continue;
			for (int attempt = 0; attempt < 6; attempt++) {
				double angle = side + (random.nextDouble() - 0.5);
				double distance = 14 + random.nextDouble() * 6;
				BlockPos column = BlockPos.containing(llama.getX() + Math.cos(angle) * distance, llama.getY(), llama.getZ() + Math.sin(angle) * distance);
				if (!level.isPositionEntityTicking(column)) continue;
				BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
				if (!level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.below()).isEmpty()) continue;
				mob.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, random.nextFloat() * 360, 0);
				if (!level.noCollision(mob)) continue;
				mob.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), EntitySpawnReason.EVENT, null);
				mob.setCanPickUpLoot(false);
				level.addFreshEntity(mob);
				mob.setTarget(llama);
				spawned++;
				break;
			}
		}
		if (spawned > 0) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.bounty.escort_ambush").withStyle(ChatFormatting.RED));
			level.playSound(null, llama.blockPosition(), SoundEvents.LLAMA_ANGRY, SoundSource.NEUTRAL, 1F, 1F);
		}
		return spawned > 0;
	}

	private static void arrive(ServerPlayer player, ServerLevel level, Town.Standing standing, Town.Active bounty, Llama llama) {
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, llama.getX(), llama.getY() + 1, llama.getZ(), 16, 0.6, 0.6, 0.6, 0);
		player.sendSystemMessage(Component.translatable("message.wildercord.bounty.escort_arrived", Town.targetName(bounty)).withStyle(ChatFormatting.GOLD));
		ROADS.remove(player.getUUID());
		BlockPos at = llama.blockPosition();
		release(level, llama);
		BountyBoardBlock.turnIn(player, level, at, standing, bounty, level.getGameTime() / BountyRules.DAY);
	}
}
