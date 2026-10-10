package dev.wildercord.town;

import dev.wildercord.aura.Aura;
import dev.wildercord.monster.Tempering;
import dev.wildercord.monster.TemperingRules;
import dev.wildercord.player.Heart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Named elites for the bounty board (0.13): a champion of one of Wildercord's monsters, set loose somewhere 48 to 96 blocks
 * from the board when the bounty is taken. It's tempered to the hunter (at the least to threat 6), three times as tough as an
 * elite of its kind, keeps its name over its head and never despawns. Only the hunter who took it can count its kill.
 */
public final class BountyHunts {
	private BountyHunts() {}

	/** Sets the bounty's named elite loose near the board; returns which way it lies from the board, or null if no spot was found. */
	public static Direction loose(ServerPlayer player, ServerLevel level, BlockPos board, Town.Active bounty) {
		Identifier id = Identifier.tryParse(bounty.target());
		if (id == null) return null;
		var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
		if (type.isEmpty()) return null;
		var random = level.getRandom();
		for (int attempt = 0; attempt < 16; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = BountyRules.ELITE_NEAR + random.nextDouble() * (BountyRules.ELITE_FAR - BountyRules.ELITE_NEAR);
			int x = board.getX() + (int) Math.round(Math.cos(angle) * distance);
			int z = board.getZ() + (int) Math.round(Math.sin(angle) * distance);
			BlockPos column = new BlockPos(x, board.getY(), z);
			if (!level.hasChunkAt(column)) continue;
			BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (!level.getFluidState(ground.below()).isEmpty() || !level.getFluidState(ground).isEmpty()) continue;
			if (!(type.get().create(level, EntitySpawnReason.EVENT) instanceof Mob mob)) return null;
			mob.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, random.nextFloat() * 360, 0);
			if (!level.noCollision(mob)) continue;
			int threat = Math.max(BountyRules.ELITE_THREAT_MIN, TemperingRules.threat(Heart.circles(player), Aura.stage(player)));
			TemperingRules.Elite[] kinds = TemperingRules.Elite.values();
			Component name = Component.translatable("town.wildercord.named_elite", bounty.name(), mob.getType().getDescription()).withColor(0xFFB040);
			Tempering.champion(mob, threat, kinds[random.nextInt(kinds.length)], BountyRules.ELITE_HEALTH, name);
			mob.addTag(Town.markTag(player.getUUID()));
			level.addFreshEntity(mob);
			level.playSound(null, mob.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.6F, 1.4F);
			return Direction.getApproximateNearest(x - board.getX(), 0, z - board.getZ());
		}
		return null;
	}
}
