package dev.wildercord.content;

import dev.wildercord.cast.Archivist;
import dev.wildercord.content.dungeons.DungeonAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Waits for a player to come within 12 blocks, then wakes the Archivist, and remembers which one it
 * woke (saved). Once the Archivist falls the lectern stays quiet for good; but if it's gone without
 * falling and nobody finds it in the Archive for a few minutes while players are there, the lectern
 * re-arms, as a dungeon's altar does, so the Archive isn't left without its keeper.
 */
public class ArchiveLecternBlockEntity extends BlockEntity {
	/** How near a player must be for the missing Archivist to count (they'd see it if it were there). */
	private static final double WATCH = 32.0;

	private UUID boss;
	private boolean slain;
	/** When players were first in the Archive with its keeper missing (not saved: the count starts again). */
	private long missingSince;

	public ArchiveLecternBlockEntity(BlockPos pos, BlockState state) {
		super(WildercordBlocks.ARCHIVE_LECTERN_ENTITY, pos, state);
	}

	/** The Archivist has fallen: the lectern goes quiet for good. */
	public void slain() {
		slain = true;
		setChanged();
	}

	public boolean isSlain() {
		return slain;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, ArchiveLecternBlockEntity entity) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		if (state.getValue(ArchiveLecternBlock.AWAKE)) {
			entity.watch(server, pos, state);
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
				Archivist woken = Archivist.rise(server, pos);
				entity.boss = woken == null ? null : woken.getUUID();
				entity.slain = false;
				entity.missingSince = 0;
				entity.setChanged();
				return;
			}
		}
	}

	/** Awake: every few seconds, whether the Archivist is still about, and if it's long gone, re-arms. */
	private void watch(ServerLevel level, BlockPos pos, BlockState state) {
		long time = level.getGameTime();
		if (slain || (time + pos.asLong()) % 100 != 0 || !level.isPositionEntityTicking(pos)) {
			return;
		}
		Vec3 heart = Vec3.atCenterOf(pos);
		boolean watched = false;
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && player.position().distanceTo(heart) <= WATCH) {
				watched = true;
				break;
			}
		}
		// (A lectern that never knew its Archivist, woken before lecterns remembered them, is taken to have seen it fall.)
		if (!watched || boss == null || bossHere(level, pos)) {
			missingSince = 0;
			return;
		}
		if (missingSince == 0) {
			missingSince = time;
		} else if (time - missingSince >= DungeonAltarBlockEntity.REARM_TICKS) {
			// Gone without falling: the lectern gathers itself, and wakes the Archivist again for whoever comes near.
			missingSince = 0;
			boss = null;
			setChanged();
			level.setBlock(pos, state.setValue(ArchiveLecternBlock.AWAKE, false), Block.UPDATE_ALL);
		}
	}

	/** Whether its Archivist (or any Archivist of this Archive's) is alive near the lectern. */
	private boolean bossHere(ServerLevel level, BlockPos pos) {
		if (boss != null && level.getEntity(boss) instanceof Archivist found && found.isAlive()) {
			return true;
		}
		List<Archivist> near = level.getEntitiesOfClass(Archivist.class, new AABB(pos).inflate(48), Archivist::isAlive);
		if (!near.isEmpty()) {
			boss = near.getFirst().getUUID();
			setChanged();
			return true;
		}
		return false;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		boss = input.read("boss", UUIDUtil.CODEC).orElse(null);
		slain = input.getBooleanOr("slain", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (boss != null) {
			output.store("boss", UUIDUtil.CODEC, boss);
		}
		output.putBoolean("slain", slain);
	}
}
