package dev.wildercord.world.ferryman;

import dev.wildercord.aura.world.Guardians;
import dev.wildercord.world.sites.SiteStructure;
import dev.wildercord.world.sites.water.LastCrossingPiece;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import java.util.UUID;

/** Saves the identity, never replacing a guardian merely because its chunk is not currently loaded. */
public final class CrossingBellEntity extends BlockEntity {
	private UUID guardian;
	private boolean slain;
	public CrossingBellEntity(BlockPos pos, BlockState state) { super(FerrymanContent.BELL_ENTITY, pos, state); }
	public UUID guardian() { return guardian; }
	public boolean slain() { return slain; }
	public void use(ServerPlayer player) {
		ServerLevel server = player.level();
		if (player.isSpectator() || !player.isAlive() || !worldPosition.closerToCenterThan(player.position(), 6)) return;
		var start = server.structureManager().getStructureWithPieceAt(worldPosition,
			h -> h.value() instanceof SiteStructure site && site.site().equals("water_last_crossing"));
		LastCrossingPiece crossing = null;
		if (start.isValid()) for (var piece : start.getPieces()) {
			if (piece instanceof LastCrossingPiece found && found.bellPosition().equals(worldPosition)) { crossing = found; break; }
		}
		if (crossing == null) { say(player, "decorative"); return; }
		if (slain) { say(player, "finished"); return; }
		if (!Guardians.enabled() || server.getDifficulty() == Difficulty.PEACEFUL || player.isCreative()) { say(player, "quiet"); return; }
		if (guardian != null) {
			if (server.getEntity(guardian) instanceof ReedCrownedFerryman ferryman && ferryman.belongsTo(worldPosition)) {
				if (ferryman.challenge(player)) ring(server); else say(player, "active");
			} else say(player, "waiting");
			return;
		}
		var ferryman = FerrymanContent.FERRYMAN.create(server, EntitySpawnReason.STRUCTURE);
		if (ferryman == null) return;
		ferryman.bind(crossing.mooringPosition(), worldPosition);
		// Store identity only after successful insertion. No cost, and no discarded challenge on a failed spawn.
		if (!server.addFreshEntity(ferryman)) return;
		guardian = ferryman.getUUID(); setChanged();
		if (ferryman.challenge(player)) ring(server);
	}
	private void ring(ServerLevel server) { server.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.4F, .65F); }
	private static void say(ServerPlayer player, String key) { player.sendSystemMessage(Component.translatable("message.wildercord.ferryman." + key), true); }
	/** Called before rewards; only the actual saved guardian can complete this crossing. */
	public boolean complete(UUID id) {
		if (slain || !id.equals(guardian)) return false;
		slain = true; setChanged(); return true;
	}
	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input); guardian = input.read("guardian", UUIDUtil.CODEC).orElse(null); slain = input.getBooleanOr("slain", false);
	}
	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output); if (guardian != null) output.store("guardian", UUIDUtil.CODEC, guardian); output.putBoolean("slain", slain);
	}
}
