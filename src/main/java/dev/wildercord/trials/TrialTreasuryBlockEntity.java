package dev.wildercord.trials;

import dev.wildercord.compat.McEntities;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneItem;
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
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Persists contributor eligibility and claimed-once status for each cleared Leyline Trial. */
public final class TrialTreasuryBlockEntity extends BlockEntity {
	private boolean unlocked;
	private @Nullable BlockPos gatePos;
	private final Set<UUID> eligible = new LinkedHashSet<>();
	private final Set<UUID> claimed = new LinkedHashSet<>();

	public TrialTreasuryBlockEntity(BlockPos pos, BlockState state) {
		super(CoopTrialContent.TRIAL_TREASURY_ENTITY, pos, state);
	}

	public boolean isUnlocked() {
		return unlocked;
	}

	public void resetForTrial(BlockPos gate) {
		unlocked = false;
		gatePos = gate.immutable();
		eligible.clear();
		claimed.clear();
		setChanged();
	}

	public void unlockFor(Collection<UUID> contributors, @Nullable BlockPos gate) {
		unlocked = true;
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
		ItemStack primaryRelic = (rng.nextInt(2) == 0)
			? new ItemStack(CoopTrialContent.SOVEREIGN_HALBERD)
			: new ItemStack(CoopTrialContent.LEYLINE_BELL_CHARM);
		give(player, primaryRelic);
		give(player, new ItemStack(WildercordItems.MANA_CRYSTAL, 2));
		give(player, new ItemStack(WildercordItems.BLANK_RUNE, 4));
		give(player, new ItemStack(dev.wildercord.aura.world.AuraWorld.AURA_SHARD, 2));
		give(player, new ItemStack(Items.DIAMOND, 2));

		List<RuneDef> tierPool = Runes.ALL.stream().filter(r -> r.tier() >= 2 && r.tier() <= 3).toList();
		if (!tierPool.isEmpty()) {
			RuneDef chosen = tierPool.get(rng.nextInt(tierPool.size()));
			give(player, RuneItem.stack(chosen));
		}

		server.playSound(null, Vec3.atCenterOf(worldPosition).x, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
			SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.9F, 1.1F);
		player.sendSystemMessage(Component.translatable("message.wildercord.trial.treasury_awarded")
			.withStyle(ChatFormatting.AQUA));
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
		if (gatePos != null) {
			output.store("gate_pos", BlockPos.CODEC, gatePos);
		}
		output.store("eligible", UUIDUtil.CODEC.listOf(), List.copyOf(eligible));
		output.store("claimed", UUIDUtil.CODEC.listOf(), List.copyOf(claimed));
	}
}
