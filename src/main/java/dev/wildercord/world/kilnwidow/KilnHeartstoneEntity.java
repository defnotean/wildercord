package dev.wildercord.world.kilnwidow;

import dev.wildercord.compat.McEntities;

import dev.wildercord.aura.world.Guardians;
import dev.wildercord.world.sites.SiteStructure;
import dev.wildercord.world.sites.emberwood.EmberwoodSites;
import dev.wildercord.world.sites.emberwood.KilnCourtPiece;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import java.util.UUID;
import dev.wildercord.compat.McValueInput;
import dev.wildercord.compat.McValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;

/** Saves the guardian's identity and completion state, never duplicating The Kiln Widow on replay or reload. */
public final class KilnHeartstoneEntity extends BlockEntity {
	private UUID guardian;
	private boolean slain;
	private long slainAt;

	public KilnHeartstoneEntity(BlockPos pos, BlockState state) { super(KilnWidowContent.HEARTSTONE_ENTITY, pos, state); }
	public UUID guardian() { return guardian; }
	public boolean slain() { return slain; }

	public InteractionResult stoke(ServerPlayer player) {
		ServerLevel server = (ServerLevel) player.level();
		if (player.isSpectator() || !player.isAlive() || !worldPosition.closerToCenterThan(player.position(), 6)) return InteractionResult.CONSUME;
		var start = server.structureManager().getStructureWithPieceAt(worldPosition,
			h -> h.value() instanceof SiteStructure site && site.site().equals(EmberwoodSites.KILN_COURT));
		KilnCourtPiece court = null;
		if (start.isValid()) for (var piece : start.getPieces()) {
			if (piece instanceof KilnCourtPiece found && found.heartstone().equals(worldPosition)) { court = found; break; }
		}
		if (court == null) { say(player, "decorative"); return InteractionResult.CONSUME; }
		if (slain) {
			long remaining = dev.wildercord.cast.BossRules.respawnTicksRemaining(slainAt, server.getGameTime());
			if (remaining > 0) {
				player.sendSystemMessage(Component.literal("The arena rests. You can challenge the guardian again in "
					+ dev.wildercord.cast.BossRules.formatRespawnRemaining(remaining) + "."), true);
				return InteractionResult.CONSUME;
			}
			slain = false; slainAt = 0; guardian = null; setChanged();
		}
		if (!Guardians.enabled() || server.getDifficulty() == Difficulty.PEACEFUL || player.isCreative()) { say(player, "quiet"); return InteractionResult.CONSUME; }
		if (guardian != null) {
			if (server.getEntity(guardian) instanceof KilnWidow widow && widow.belongsTo(worldPosition)) {
				if (widow.challenge(player)) kindle(server); else say(player, "active");
			} else say(player, "waiting");
			return InteractionResult.SUCCESS;
		}
		var widow = McEntities.create(KilnWidowContent.WIDOW, server, MobSpawnType.STRUCTURE);
		if (widow == null) return InteractionResult.FAIL;
		widow.bind(court.plinth(), worldPosition);
		if (!server.addFreshEntity(widow)) return InteractionResult.FAIL;
		guardian = widow.getUUID();
		setChanged();
		if (widow.challenge(player)) kindle(server);
		return InteractionResult.SUCCESS;
	}

	private void kindle(ServerLevel server) {
		server.playSound(null, worldPosition, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.4F, .8F);
	}
	private static void say(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable("message.wildercord.kiln_widow." + key), true);
	}

	public boolean complete(UUID id) {
		if (slain || !id.equals(guardian)) return false;
		slain = true;
		slainAt = level != null ? level.getGameTime() : 0L;
		setChanged();
		return true;
	}

	@Override protected void loadAdditional(CompoundTag inputTag, HolderLookup.Provider registries) { McValueInput input = new McValueInput(inputTag, (level == null ? RegistryAccess.EMPTY : level.registryAccess()));
		super.loadAdditional(inputTag, registries);
		guardian = input.read("guardian", UUIDUtil.CODEC).orElse(null);
		slain = input.getBooleanOr("slain", false);
		slainAt = input.getLongOr("slain_at", 0L);
	}
	@Override protected void saveAdditional(CompoundTag outputTag, HolderLookup.Provider registries) { McValueOutput output = new McValueOutput(outputTag, (level == null ? RegistryAccess.EMPTY : level.registryAccess()));
		super.saveAdditional(outputTag, registries);
		if (guardian != null) output.store("guardian", UUIDUtil.CODEC, guardian);
		output.putBoolean("slain", slain);
		output.putLong("slain_at", slainAt);
	}
}
