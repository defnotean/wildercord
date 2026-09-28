package dev.wildercord.content;

import dev.wildercord.cast.Archivist;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Waits for a player to come within 12 blocks, then wakes the Archivist. */
public class ArchiveLecternBlockEntity extends BlockEntity {
	public ArchiveLecternBlockEntity(BlockPos pos, BlockState state) {
		super(WildercordBlocks.ARCHIVE_LECTERN_ENTITY, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, ArchiveLecternBlockEntity entity) {
		if (!(level instanceof ServerLevel server) || state.getValue(ArchiveLecternBlock.AWAKE)) {
			return;
		}
		long time = server.getGameTime();
		Vec3 top = Vec3.atCenterOf(pos).add(0, 0.7, 0);
		if (time % 4 == 0) {
			server.sendParticles(ParticleTypes.ENCHANT, top.x, top.y + 0.5, top.z, 4, 0.6, 0.4, 0.6, 0.6);
		}
		if (time % 20 != 0) {
			return;
		}
		for (ServerPlayer player : server.players()) {
			if (!player.isSpectator() && player.position().distanceTo(Vec3.atCenterOf(pos)) <= 12) {
				level.setBlock(pos, state.setValue(ArchiveLecternBlock.AWAKE, true), Block.UPDATE_ALL);
				Archivist.rise(server, pos);
				return;
			}
		}
	}
}
