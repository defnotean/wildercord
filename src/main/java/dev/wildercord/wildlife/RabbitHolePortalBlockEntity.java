package dev.wildercord.wildlife;

import dev.wildercord.cast.Vfx;
import dev.wildercord.compat.McValueInput;
import dev.wildercord.compat.McValueOutput;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent state for a {@link RabbitHolePortalBlock}, lazily generating and linking the rolled
 * destination (Random Cave, Treasure Vault, or All Bosses Colosseum) or returning the player out
 * of a destination's Return Burrow Portal.
 */
public class RabbitHolePortalBlockEntity extends BlockEntity {
	private static final Map<UUID, Long> LAST_TELEPORT = new HashMap<>();

	private WhiteRabbitRules.Outcome outcome = WhiteRabbitRules.Outcome.RANDOM_CAVE;
	private boolean returnPortal;
	private boolean destinationBuilt;
	private BlockPos destinationPos;

	public RabbitHolePortalBlockEntity(BlockPos pos, BlockState state) {
		super(WhiteRabbitContent.RABBIT_HOLE_PORTAL_ENTITY, pos, state);
	}

	public static void clearCooldowns() {
		LAST_TELEPORT.clear();
	}

	public static void clearPlayerCooldown(ServerPlayer player) {
		if (player != null) {
			LAST_TELEPORT.remove(player.getUUID());
		}
	}

	public void configureOutcome(WhiteRabbitRules.Outcome outcome) {
		this.outcome = outcome == null ? WhiteRabbitRules.Outcome.RANDOM_CAVE : outcome;
		this.returnPortal = false;
		setChanged();
	}

	public void configureReturn(BlockPos returnTarget) {
		this.returnPortal = true;
		this.destinationBuilt = true;
		this.destinationPos = returnTarget == null ? null : returnTarget.immutable();
		setChanged();
	}

	public WhiteRabbitRules.Outcome outcome() {
		return outcome;
	}

	public boolean isReturnPortal() {
		return returnPortal;
	}

	public BlockPos destinationPos() {
		return destinationPos;
	}

	/** Teleports {@code player} through this burrow portal if off cooldown, building the destination on first entry. */
	public void enter(ServerLevel level, ServerPlayer player) {
		long now = level.getGameTime();
		Long prev = LAST_TELEPORT.get(player.getUUID());
		if (prev != null && now - prev < WhiteRabbitRules.PORTAL_COOLDOWN_TICKS) {
			return;
		}
		LAST_TELEPORT.put(player.getUUID(), now);

		if (returnPortal) {
			BlockPos target = destinationPos != null ? destinationPos : worldPosition.above(2);
			teleportPlayer(level, player, target, false);
			level.playSound(null, target, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.15F);
			player.sendSystemMessage(Component.translatable("message.wildercord.rabbit_hole.returned")
				.withStyle(ChatFormatting.GREEN));
			return;
		}

		if (!destinationBuilt || destinationPos == null) {
			destinationPos = switch (outcome) {
				case RANDOM_CAVE -> HollowTreeBuilder.buildOrFindRandomCave(level, worldPosition, level.getRandom());
				case TREASURE_VAULT -> HollowTreeBuilder.buildTreasureVault(level, worldPosition, level.getRandom());
				case ALL_BOSSES -> HollowTreeBuilder.buildAllBossesArena(level, worldPosition, player, level.getRandom());
			};
			destinationBuilt = true;
			setChanged();
		} else if (outcome == WhiteRabbitRules.Outcome.ALL_BOSSES) {
			HollowTreeBuilder.challengeEncounterBossesNear(level, destinationPos, player);
		}

		teleportPlayer(level, player, destinationPos, outcome == WhiteRabbitRules.Outcome.ALL_BOSSES);
		announceArrival(level, player, destinationPos);
	}

	private static void teleportPlayer(ServerLevel level, ServerPlayer player, BlockPos dest, boolean bossProtect) {
		level.getChunk(dest);
		player.resetFallDistance();
		player.teleportTo(level, dest.getX() + 0.5, dest.getY() + 0.1, dest.getZ() + 0.5,
			Set.of(), 180.0F, 0.0F, false);
		if (bossProtect) {
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 1, false, true));
		}
		Vec3 at = Vec3.atCenterOf(dest);
		Vfx.emit(level, ParticleTypes.PORTAL, at, 36, 0.6, 0.15);
		Vfx.emit(level, ParticleTypes.END_ROD, at.add(0, 0.5, 0), 16, 0.4, 0.06);
	}

	private void announceArrival(ServerLevel level, ServerPlayer player, BlockPos dest) {
		switch (outcome) {
			case RANDOM_CAVE -> {
				level.playSound(null, dest, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 0.9F);
				player.sendSystemMessage(Component.translatable("message.wildercord.rabbit_hole.cave")
					.withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
			}
			case TREASURE_VAULT -> {
				level.playSound(null, dest, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.1F, 1.3F);
				level.playSound(null, dest, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
				player.sendSystemMessage(Component.translatable("message.wildercord.rabbit_hole.treasure")
					.withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
			}
			case ALL_BOSSES -> {
				level.playSound(null, dest, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.85F, 0.85F);
				player.sendSystemMessage(Component.translatable("message.wildercord.rabbit_hole.boss_rush")
					.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
			}
		}
	}

	@Override
	protected void loadAdditional(CompoundTag inputTag, HolderLookup.Provider registries) { McValueInput input = new McValueInput(inputTag, (level == null ? RegistryAccess.EMPTY : level.registryAccess()));
		super.loadAdditional(inputTag, registries);
		outcome = WhiteRabbitRules.Outcome.fromId(input.getStringOr("Outcome", WhiteRabbitRules.Outcome.RANDOM_CAVE.id()));
		returnPortal = input.getBooleanOr("ReturnPortal", false);
		destinationBuilt = input.getBooleanOr("DestBuilt", false);
		destinationPos = input.read("DestPos", BlockPos.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(CompoundTag outputTag, HolderLookup.Provider registries) { McValueOutput output = new McValueOutput(outputTag, (level == null ? RegistryAccess.EMPTY : level.registryAccess()));
		super.saveAdditional(outputTag, registries);
		output.putString("Outcome", outcome.id());
		output.putBoolean("ReturnPortal", returnPortal);
		output.putBoolean("DestBuilt", destinationBuilt);
		if (destinationPos != null) {
			output.store("DestPos", BlockPos.CODEC, destinationPos);
		}
	}
}
