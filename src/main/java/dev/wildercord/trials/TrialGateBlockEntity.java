package dev.wildercord.trials;

import dev.wildercord.aura.world.Guardians;
import dev.wildercord.cast.BossRules;
import dev.wildercord.world.sites.SiteStructure;
import dev.wildercord.world.sites.trials.LeylineSanctumPiece;
import dev.wildercord.world.sites.trials.TrialSites;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/** Persistent controller binding an authentic Leyline Sanctum, its boss, and its Treasury. */
public final class TrialGateBlockEntity extends BlockEntity {
	private @Nullable UUID guardian;
	private boolean slain;
	private long slainAt;

	public TrialGateBlockEntity(BlockPos pos, BlockState state) {
		super(CoopTrialContent.TRIAL_GATE_ENTITY, pos, state);
	}

	public @Nullable UUID guardian() {
		return guardian;
	}

	public boolean slain() {
		return slain;
	}

	public void use(ServerPlayer player, ItemStack held) {
		ServerLevel server = player.level();
		if (player.isSpectator() || !player.isAlive() || !worldPosition.closerToCenterThan(player.position(), 8.0)) {
			return;
		}
		LeylineSanctumPiece sanctum = findSanctum(server, worldPosition);
		if (sanctum == null || !sanctum.gatePosition().equals(worldPosition)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.trial.decorative"));
			return;
		}
		if (!Guardians.enabled() || server.getDifficulty() == Difficulty.PEACEFUL || player.isCreative()) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.trial.quiet"));
			return;
		}
		if (guardian != null) {
			if (server.getEntity(guardian) instanceof VaelkhorBoss existing && existing.isAlive()) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.trial.active").withStyle(ChatFormatting.GOLD));
				return;
			}
			guardian = null;
			setChanged();
		}
		if (slain) {
			long remaining = BossRules.respawnTicksRemaining(slainAt, server.getGameTime());
			if (remaining > 0) {
				if (held.is(CoopTrialContent.LEYLINE_SIGIL)) {
					held.shrink(1);
				} else {
					player.sendOverlayMessage(Component.translatable(
						"message.wildercord.trial.cooldown", BossRules.formatRespawnRemaining(remaining)));
					return;
				}
			}
			slain = false;
			slainAt = 0L;
			setChanged();
		}
		BlockPos center = sanctum.arenaCenter();
		BlockPos treasuryPos = sanctum.treasuryPosition();
		List<ServerPlayer> party = gatherParty(server, center);
		if (!party.contains(player)) {
			party = List.of(player);
		}
		VaelkhorBoss boss = CoopTrialContent.VAELKHOR.create(server, EntitySpawnReason.STRUCTURE);
		if (boss == null) {
			return;
		}
		boss.configureTrial(center, worldPosition, treasuryPos, party.size());
		if (!server.addFreshEntity(boss)) {
			return;
		}
		guardian = boss.getUUID();
		setChanged();
		if (server.getBlockEntity(treasuryPos) instanceof TrialTreasuryBlockEntity treasury) {
			treasury.resetForTrial(worldPosition);
		}
		server.playSound(null, worldPosition, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.9F, 1.15F);
		for (ServerPlayer member : party) {
			member.sendSystemMessage(Component.translatable("message.wildercord.trial.started", party.size())
				.withStyle(ChatFormatting.AQUA));
		}
	}

	private static List<ServerPlayer> gatherParty(ServerLevel server, BlockPos arenaCenter) {
		Vec3 center = Vec3.atBottomCenterOf(arenaCenter);
		return server.players().stream()
			.filter(p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
				&& p.position().distanceToSqr(center) <= 24.0 * 24.0)
			.limit(VaelkhorRules.MAX_PARTY_PLAYERS)
			.toList();
	}

	public void resetActiveTrial(UUID id) {
		if (guardian != null && guardian.equals(id)) {
			guardian = null;
			setChanged();
		}
	}

	public boolean complete(UUID id) {
		if (guardian == null || !guardian.equals(id)) {
			return false;
		}
		slain = true;
		slainAt = level != null ? level.getGameTime() : 0L;
		guardian = null;
		setChanged();
		return true;
	}

	private static @Nullable LeylineSanctumPiece findSanctum(ServerLevel server, BlockPos pos) {
		var start = server.structureManager().getStructureWithPieceAt(
			pos, h -> h.value() instanceof SiteStructure site && site.site().equals(TrialSites.SANCTUM_ID));
		if (!start.isValid()) {
			return null;
		}
		for (var piece : start.getPieces()) {
			if (piece instanceof LeylineSanctumPiece found) {
				return found;
			}
		}
		return null;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		guardian = input.read("guardian", UUIDUtil.CODEC).orElse(null);
		slain = input.getBooleanOr("slain", false);
		slainAt = input.getLongOr("slain_at", 0L);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (guardian != null) {
			output.store("guardian", UUIDUtil.CODEC, guardian);
		}
		output.putBoolean("slain", slain);
		output.putLong("slain_at", slainAt);
	}
}
