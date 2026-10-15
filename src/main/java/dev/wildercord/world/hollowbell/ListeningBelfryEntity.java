package dev.wildercord.world.hollowbell;

import dev.wildercord.aura.world.Guardians;
import dev.wildercord.world.sites.SiteStructure;
import dev.wildercord.world.sites.belowkeeper.BellSanctumPiece;
import dev.wildercord.world.sites.belowkeeper.BelowkeeperSites;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import java.util.UUID;

/** Saves the guardian's identity and completion state, never duplicating The Hollow Bell on replay or reload. */
public final class ListeningBelfryEntity extends BlockEntity {
	private UUID guardian;
	private boolean slain;

	public ListeningBelfryEntity(BlockPos pos, BlockState state) { super(HollowBellContent.BELFRY_ENTITY, pos, state); }
	public UUID guardian() { return guardian; }
	public boolean slain() { return slain; }

	public InteractionResult ring(ServerPlayer player) {
		ServerLevel server = player.level();
		if (player.isSpectator() || !player.isAlive() || !worldPosition.closerToCenterThan(player.position(), 6)) return InteractionResult.CONSUME;
		var start = server.structureManager().getStructureWithPieceAt(worldPosition,
			h -> h.value() instanceof SiteStructure site && site.site().equals(BelowkeeperSites.BELL_SANCTUM));
		BellSanctumPiece sanctum = null;
		if (start.isValid()) for (var piece : start.getPieces()) {
			if (piece instanceof BellSanctumPiece found && found.belfry().equals(worldPosition)) { sanctum = found; break; }
		}
		if (sanctum == null) { say(player, "decorative"); return InteractionResult.CONSUME; }
		if (slain) { say(player, "finished"); return InteractionResult.CONSUME; }
		if (!Guardians.enabled() || server.getDifficulty() == Difficulty.PEACEFUL || player.isCreative()) { say(player, "quiet"); return InteractionResult.CONSUME; }
		if (guardian != null) {
			if (server.getEntity(guardian) instanceof HollowBell bell && bell.belongsTo(worldPosition)) {
				if (bell.challenge(player)) chime(server); else say(player, "active");
			} else say(player, "waiting");
			return InteractionResult.SUCCESS;
		}
		var bell = HollowBellContent.BELL.create(server, EntitySpawnReason.STRUCTURE);
		if (bell == null) return InteractionResult.FAIL;
		bell.bind(sanctum.plinth(), worldPosition);
		if (!server.addFreshEntity(bell)) return InteractionResult.FAIL;
		guardian = bell.getUUID();
		setChanged();
		if (bell.challenge(player)) chime(server);
		return InteractionResult.SUCCESS;
	}

	private void chime(ServerLevel server) {
		server.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.4F, .75F);
	}
	private static void say(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable("message.wildercord.hollow_bell." + key), true);
	}

	public boolean complete(UUID id) {
		if (slain || !id.equals(guardian)) return false;
		slain = true;
		setChanged();
		return true;
	}

	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		guardian = input.read("guardian", UUIDUtil.CODEC).orElse(null);
		slain = input.getBooleanOr("slain", false);
	}
	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (guardian != null) output.store("guardian", UUIDUtil.CODEC, guardian);
		output.putBoolean("slain", slain);
	}
}
