package dev.wildercord.content.dungeons;

import dev.wildercord.cast.DungeonBoss;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
 * Waits for a player to come near, then wakes its dungeon's boss, and remembers which boss it woke
 * (saved). Once that boss falls the altar stays quiet for good; but if it's gone without falling
 * (removed some other way) and nobody finds it in its arena for a few minutes while players are
 * there, the altar re-arms, so the dungeon isn't left without its boss.
 */
public class DungeonAltarBlockEntity extends BlockEntity {
	/** How long players may be in the arena with its boss missing before the altar re-arms. */
	public static final int REARM_TICKS = 3 * 60 * 20;
	/** How near a player must be for the missing boss to count (they'd see it if it were there). */
	private static final double WATCH = 32.0;

	private UUID boss;
	private boolean slain;
	/** When players were first in the arena with its boss missing (not saved: the count starts again). */
	private long missingSince;

	public DungeonAltarBlockEntity(BlockPos pos, BlockState state) {
		super(DungeonBlocks.ALTAR_ENTITY, pos, state);
	}

	/** Its boss has fallen: the altar goes quiet for good. */
	public void slain() {
		slain = true;
		setChanged();
	}

	public boolean isSlain() {
		return slain;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, DungeonAltarBlockEntity entity) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		if (state.getValue(DungeonAltarBlock.AWAKE)) {
			entity.watch(server, pos, state);
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
				case ROOT -> ParticleTypes.HAPPY_VILLAGER;
				case STORM -> ParticleTypes.ELECTRIC_SPARK;
			};
			dev.wildercord.cast.Fx.sendParticles(server, mote, top.x, top.y + 0.4, top.z, 2, 0.5, 0.3, 0.5, 0.02);
		}
		if (time % 20 != 0) {
			return;
		}
		for (ServerPlayer player : server.players()) {
			if (!player.isSpectator() && player.position().distanceTo(Vec3.atCenterOf(pos)) <= kind.reach) {
				level.setBlock(pos, state.setValue(DungeonAltarBlock.AWAKE, true), Block.UPDATE_ALL);
				Entity woken = kind.wake(server, pos);
				entity.boss = woken == null ? null : woken.getUUID();
				entity.slain = false;
				entity.missingSince = 0;
				entity.setChanged();
				return;
			}
		}
	}

	/** Awake: every few seconds, whether its boss is still about, and if it's long gone, re-arms. */
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
		// (An altar that never knew its boss, woken before altars remembered them, is taken to have seen it fall.)
		if (!watched || bossHere(level, pos) || boss == null) {
			missingSince = 0;
			return;
		}
		if (missingSince == 0) {
			missingSince = time;
		} else if (time - missingSince >= REARM_TICKS) {
			// Gone without falling: the altar gathers itself, and wakes a boss again for whoever comes near.
			missingSince = 0;
			boss = null;
			setChanged();
			level.setBlock(pos, state.setValue(DungeonAltarBlock.AWAKE, false), Block.UPDATE_ALL);
		}
	}

	/** Whether its boss (or any boss of this altar's) is alive in its arena. */
	private boolean bossHere(ServerLevel level, BlockPos pos) {
		if (boss != null && level.getEntity(boss) instanceof DungeonBoss found && found.isAlive()) {
			return true;
		}
		List<DungeonBoss> near = level.getEntitiesOfClass(DungeonBoss.class, new AABB(pos).inflate(48),
			b -> b.isAlive() && (b.home() == null || b.home().equals(pos)));
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
