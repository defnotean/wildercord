package dev.wildercord.trials;

import dev.wildercord.compat.McEntities;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Persists contributor eligibility, trial tier/Apex state, and claimed-once status for each Trial clear. */
public final class TrialTreasuryBlockEntity extends BlockEntity {
	private boolean unlocked;
	private boolean apex;
	private String trialKind = "leyline";
	private @Nullable BlockPos gatePos;
	private final Set<UUID> eligible = new LinkedHashSet<>();
	private final Set<UUID> claimed = new LinkedHashSet<>();

	public TrialTreasuryBlockEntity(BlockPos pos, BlockState state) {
		super(CoopTrialContent.TRIAL_TREASURY_ENTITY, pos, state);
	}

	public boolean isUnlocked() {
		return unlocked;
	}

	public boolean isApex() {
		return apex;
	}

	public String trialKind() {
		return trialKind;
	}

	public void resetForTrial(BlockPos gate) {
		unlocked = false;
		apex = false;
		gatePos = gate.immutable();
		eligible.clear();
		claimed.clear();
		setChanged();
	}

	public void unlockFor(Collection<UUID> contributors, @Nullable BlockPos gate) {
		unlockFor(contributors, gate, "leyline", false);
	}

	public void unlockFor(Collection<UUID> contributors, @Nullable BlockPos gate, String kind, boolean apex) {
		unlocked = true;
		this.apex = apex;
		this.trialKind = kind == null ? "leyline" : kind.toLowerCase(Locale.ROOT);
		if (gate != null) {
			gatePos = gate.immutable();
		}
		eligible.clear();
		contributors.stream().limit(64).forEach(eligible::add);
		claimed.clear();
		setChanged();
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.25F);
		}
	}

	public void claim(ServerPlayer player) {
		if (!(level instanceof ServerLevel server) || player.isSpectator() || !player.isAlive()) {
			return;
		}
		if (!worldPosition.closerToCenterThan(player.position(), 7.0)) {
			return;
		}
		if (!unlocked) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.trial.treasury_locked"));
			return;
		}
		if (!eligible.contains(player.getUUID())) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.trial.treasury_not_eligible"));
			return;
		}
		if (!claimed.add(player.getUUID())) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.trial.treasury_claimed"));
			return;
		}
		setChanged();

		var rng = new Random(worldPosition.asLong() ^ player.getUUID().getLeastSignificantBits());
		give(player, primaryRelic(trialKind, rng));
		give(player, trialSeal(trialKind));
		give(player, new ItemStack(WildercordItems.MANA_CRYSTAL, apex ? 4 : 2));
		give(player, new ItemStack(WildercordItems.BLANK_RUNE, apex ? 6 : 4));
		give(player, new ItemStack(dev.wildercord.aura.world.AuraWorld.AURA_SHARD, apex ? 4 : 2));
		give(player, new ItemStack(Items.DIAMOND, apex ? 4 : 2));
		if (apex) {
			give(player, new ItemStack(CoopTrialContent.SOVEREIGN_CROWN));
		}

		List<RuneDef> tierPool = Runes.all().stream()
			.filter(r -> Runes.obtainable(r) && !Runes.fused(r) && (apex ? r.tier() == 3 : (r.tier() >= 2 && r.tier() <= 3)))
			.toList();
		if (tierPool.isEmpty()) {
			tierPool = Runes.all().stream()
				.filter(r -> Runes.obtainable(r) && !Runes.fused(r) && r.tier() >= 2 && r.tier() <= 3)
				.toList();
		}
		if (!tierPool.isEmpty()) {
			RuneDef chosen = tierPool.get(rng.nextInt(tierPool.size()));
			give(player, RuneItem.stack(chosen));
		}

		server.playSound(null, Vec3.atCenterOf(worldPosition).x, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
			SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.9F, 1.1F);
		player.sendSystemMessage(Component.translatable("message.wildercord.trial.treasury_awarded")
			.withStyle(apex ? ChatFormatting.GOLD : ChatFormatting.AQUA));
	}

	private static ItemStack primaryRelic(String kind, Random rng) {
		return switch (kind) {
			case "crucible" -> (rng.nextInt(2) == 0)
				? new ItemStack(CoopTrialContent.CRUCIBLE_MAUL)
				: new ItemStack(CoopTrialContent.FORGEHEART_CENSER);
			case "eclipse" -> (rng.nextInt(2) == 0)
				? new ItemStack(CoopTrialContent.ECLIPSE_TWINBLADE)
				: new ItemStack(CoopTrialContent.ASTROLABE_DIAL);
			default -> (rng.nextInt(2) == 0)
				? new ItemStack(CoopTrialContent.SOVEREIGN_HALBERD)
				: new ItemStack(CoopTrialContent.LEYLINE_BELL_CHARM);
		};
	}

	private static ItemStack trialSeal(String kind) {
		return switch (kind) {
			case "crucible" -> new ItemStack(CoopTrialContent.CRUCIBLE_SEAL);
			case "eclipse" -> new ItemStack(CoopTrialContent.ECLIPSE_SEAL);
			default -> new ItemStack(CoopTrialContent.LEYLINE_SEAL);
		};
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			McEntities.drop(player, stack, false);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		unlocked = input.getBooleanOr("unlocked", false);
		apex = input.getBooleanOr("apex", false);
		trialKind = input.getStringOr("trial_kind", "leyline");
		gatePos = input.read("gate_pos", BlockPos.CODEC).orElse(null);
		eligible.clear();
		claimed.clear();
		input.read("eligible", UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(64).forEach(eligible::add);
		input.read("claimed", UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(64).forEach(claimed::add);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("unlocked", unlocked);
		output.putBoolean("apex", apex);
		output.putString("trial_kind", trialKind);
		if (gatePos != null) {
			output.store("gate_pos", BlockPos.CODEC, gatePos);
		}
		output.store("eligible", UUIDUtil.CODEC.listOf(), List.copyOf(eligible));
		output.store("claimed", UUIDUtil.CODEC.listOf(), List.copyOf(claimed));
	}
}
