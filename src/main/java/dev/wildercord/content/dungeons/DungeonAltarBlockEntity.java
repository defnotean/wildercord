package dev.wildercord.content.dungeons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Waits for a player to come near, then wakes its dungeon's boss. */
public class DungeonAltarBlockEntity extends BlockEntity {
	public DungeonAltarBlockEntity(BlockPos pos, BlockState state) {
		super(DungeonBlocks.ALTAR_ENTITY, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, DungeonAltarBlockEntity entity) {
		if (!(level instanceof ServerLevel server) || state.getValue(DungeonAltarBlock.AWAKE)) {
			return;
		}
		DungeonAltarBlock.Kind kind = state.getValue(DungeonAltarBlock.KIND);
		long time = server.getGameTime();
		Vec3 top = Vec3.atCenterOf(pos).add(0, 0.7, 0);
		if (time % 4 == 0) {
			ParticleOptions mote = switch (kind) {
				case CINDER -> ParticleTypes.SMALL_FLAME;
				case ASTRAL -> ParticleTypes.END_ROD;
				case TIDE -> ParticleTypes.BUBBLE_POP;
			};
			server.sendParticles(mote, top.x, top.y + 0.4, top.z, 2, 0.5, 0.3, 0.5, 0.02);
		}
		if (time % 20 != 0) {
			return;
		}
		for (ServerPlayer player : server.players()) {
			if (!player.isSpectator() && player.position().distanceTo(Vec3.atCenterOf(pos)) <= kind.reach) {
				level.setBlock(pos, state.setValue(DungeonAltarBlock.AWAKE, true), Block.UPDATE_ALL);
				kind.wake(server, pos);
				return;
			}
		}
	}
}
