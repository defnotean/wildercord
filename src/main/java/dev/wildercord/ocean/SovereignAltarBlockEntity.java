package dev.wildercord.ocean;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side arena controller for Thalassor inside the Abyssal Trench Citadel Colosseum.
 * Handles initial guardian binding, automatic arena respawn after countdown, and pearl offering summons.
 */
public final class SovereignAltarBlockEntity extends BlockEntity {
	private UUID bossUuid;
	private int respawnTicksRemaining = -1;
	private boolean initialSpawnDone = false;

	public SovereignAltarBlockEntity(BlockPos pos, BlockState state) {
		super(OceanBlocks.SOVEREIGN_ALTAR_ENTITY, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, SovereignAltarBlockEntity altar) {
		if (!(level instanceof ServerLevel server)) return;
		altar.tickArena(server);
	}

	private void tickArena(ServerLevel server) {
		if (!initialSpawnDone && bossUuid == null && respawnTicksRemaining < 0) {
			if (!server.getPlayers(p -> p.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 48.0 * 48.0).isEmpty()) {
				summonThalassor(server);
			}
			return;
		}
		if (bossUuid != null) {
			Entity existing = server.getEntity(bossUuid);
			if (existing == null || !existing.isAlive()) {
				bossUuid = null;
				respawnTicksRemaining = OceanRules.THALASSOR_RESPAWN_TICKS;
				setChanged();
			}
			return;
		}
		if (respawnTicksRemaining > 0) {
			respawnTicksRemaining--;
			if (respawnTicksRemaining % 20 == 0) {
				server.sendParticles(
					ParticleTypes.NAUTILUS,
					worldPosition.getX() + 0.5,
					worldPosition.getY() + 1.2,
					worldPosition.getZ() + 0.5,
					3, 0.35, 0.35, 0.35, 0.02
				);
			}
			if (respawnTicksRemaining == 0) {
				respawnTicksRemaining = -1;
				summonThalassor(server);
			}
		}
	}

	public void bindExistingBoss(ThalassorBoss boss) {
		this.bossUuid = boss.getUUID();
		this.initialSpawnDone = true;
		this.respawnTicksRemaining = -1;
		setChanged();
	}

	public InteractionResult interact(Player player, ItemStack stack) {
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
			return InteractionResult.SUCCESS;
		}
		if (bossUuid != null && server.getEntity(bossUuid) instanceof ThalassorBoss boss && boss.isAlive()) {
			sp.sendSystemMessage(Component.translatable("message.wildercord.sovereign_altar.active")
				.withStyle(ChatFormatting.AQUA));
			return InteractionResult.SUCCESS;
		}
		boolean offering = stack.is(OceanItems.LEVIATHAN_HEART_PEARL)
			|| (stack.is(OceanItems.ABYSSAL_PEARL) && stack.getCount() >= 3);
		if (offering || sp.isCreative()) {
			if (!sp.isCreative()) {
				if (stack.is(OceanItems.LEVIATHAN_HEART_PEARL)) {
					stack.shrink(1);
				} else {
					stack.shrink(3);
				}
			}
			summonThalassor(server);
			sp.sendSystemMessage(Component.translatable("message.wildercord.sovereign_altar.summoned")
				.withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
			return InteractionResult.SUCCESS;
		}
		if (respawnTicksRemaining > 0) {
			int seconds = Math.max(1, respawnTicksRemaining / 20);
			sp.sendSystemMessage(Component.translatable("message.wildercord.sovereign_altar.cooldown", seconds)
				.withStyle(ChatFormatting.GRAY));
		} else {
			sp.sendSystemMessage(Component.translatable("message.wildercord.sovereign_altar.offering")
				.withStyle(ChatFormatting.AQUA));
		}
		return InteractionResult.SUCCESS;
	}

	public ThalassorBoss summonThalassor(ServerLevel server) {
		ThalassorBoss boss = OceanEntities.THALASSOR.create(server, EntitySpawnReason.EVENT);
		if (boss == null) return null;
		boss.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 3.0, worldPosition.getZ() + 0.5);
		boss.setArenaCenter(worldPosition);
		server.addFreshEntity(boss);
		this.bossUuid = boss.getUUID();
		this.initialSpawnDone = true;
		this.respawnTicksRemaining = -1;
		server.playSound(null, worldPosition, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.6f, 0.65f);
		server.sendParticles(
			ParticleTypes.END_ROD,
			worldPosition.getX() + 0.5,
			worldPosition.getY() + 2.0,
			worldPosition.getZ() + 0.5,
			32, 1.5, 1.5, 1.5, 0.08
		);
		setChanged();
		return boss;
	}

	public UUID getBossUuid() {
		return bossUuid;
	}

	public int getRespawnTicksRemaining() {
		return respawnTicksRemaining;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		bossUuid = input.read("BossUuid", UUIDUtil.CODEC).orElse(null);
		respawnTicksRemaining = input.getIntOr("RespawnTicks", -1);
		initialSpawnDone = input.getBooleanOr("InitialSpawnDone", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (bossUuid != null) {
			output.store("BossUuid", UUIDUtil.CODEC, bossUuid);
		}
		output.putInt("RespawnTicks", respawnTicksRemaining);
		output.putBoolean("InitialSpawnDone", initialSpawnDone);
	}
}
