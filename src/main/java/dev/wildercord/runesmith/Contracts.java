package dev.wildercord.runesmith;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The daily contract board at the Scribing Desk: three contracts a day for each player (see
 * {@link ContractRules}), counted by the server as they're done, and handed in at any Scribing
 * Desk for their reward. The rest of the mod reports what happens through the {@code on...} hooks.
 */
public final class Contracts {
	private Contracts() {}

	private static final Codec<ContractRules.Contract> CONTRACT_CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("kind").forGetter(ContractRules.Contract::kind),
		Codec.STRING.optionalFieldOf("arg", "").forGetter(ContractRules.Contract::arg),
		Codec.INT.fieldOf("target").forGetter(ContractRules.Contract::target),
		Codec.INT.optionalFieldOf("progress", 0).forGetter(ContractRules.Contract::progress),
		Codec.BOOL.optionalFieldOf("claimed", false).forGetter(ContractRules.Contract::claimed),
		Codec.STRING.fieldOf("reward").forGetter(ContractRules.Contract::reward)
	).apply(i, ContractRules.Contract::new));

	private static final Codec<ContractRules.Board> BOARD_CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.LONG.fieldOf("day").forGetter(ContractRules.Board::day),
		CONTRACT_CODEC.listOf().fieldOf("contracts").forGetter(ContractRules.Board::contracts)
	).apply(i, ContractRules.Board::new));

	/** Each player's board. Saved, kept through death, never synced (the board is read out in chat). */
	public static final AttachmentType<ContractRules.Board> BOARD = AttachmentRegistry.create(
		Wildercord.id("contracts"),
		builder -> builder
			.initializer(() -> ContractRules.Board.EMPTY)
			.persistent(BOARD_CODEC)
			.copyOnDeath()
	);

	/** Who last hurt each creature with a spell, with what element and when: a Runebound dying soon after is theirs. */
	private record SpellHit(UUID caster, String element, long time) {}

	private static final Map<UUID, SpellHit> LAST_HIT = new HashMap<>();
	/** Casts and reactions waiting for a spell to land on a real creature before they count (see {@link ContractRules.Credit}). */
	private static final Map<UUID, ContractRules.Credit> CREDIT = new HashMap<>();

	public static void init() {
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(ShowBoard.TYPE, ShowBoard.CODEC);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			SpellHit hit = LAST_HIT.remove(entity.getUUID());
			if (hit == null || !(entity.level() instanceof ServerLevel level) || level.getGameTime() - hit.time() > 100) {
				return;
			}
			ServerPlayer caster = level.getServer().getPlayerList().getPlayer(hit.caster());
			if (caster == null) {
				return;
			}
			if (entity instanceof Enemy) {
				progress(caster, ContractRules.SPELL_KILLS, "", 1);
			}
			if (entity.hasAttached(WildercordAttachments.RUNEBOUND) && !hit.element().isEmpty()) {
				progress(caster, ContractRules.RUNEBOUND, hit.element(), 1);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 400 == 0 && !LAST_HIT.isEmpty()) {
				long now = server.overworld().getGameTime();
				LAST_HIT.values().removeIf(hit -> now - hit.time() > 200);
			}
			if (server.getTickCount() % 200 == 0 && !CREDIT.isEmpty()) {
				long now = server.getTickCount();
				CREDIT.values().removeIf(credit -> credit.idle(now));
			}
		});
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CREDIT.remove(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LAST_HIT.clear();
			CREDIT.clear();
		});
	}

	// ------------------------------------------------------------------ hooks

	/**
	 * A player's spell hurt a creature with an element ("" for none). A real creature (not a
	 * Training Dummy or a mannequin) is what makes the player's casts and reactions count.
	 */
	public static void onSpellHit(LivingEntity caster, LivingEntity target, String element) {
		if (!(caster instanceof ServerPlayer player) || target == player || !real(target)) {
			return;
		}
		LAST_HIT.put(target.getUUID(), new SpellHit(player.getUUID(), element == null ? "" : element, player.level().getGameTime()));
		credit(player, credit(player).hit(now(player)));
	}

	/** A caster set off an element reaction: it counts once the spell lands on a real creature. */
	public static void onReaction(LivingEntity caster, String reaction) {
		if (caster instanceof ServerPlayer player) {
			credit(player, credit(player).reaction(now(player), reaction));
		}
	}

	/**
	 * A player cast a spell (ley line casts, and casts of each element in it, a Knot's runes included,
	 * as for leaning). It counts once the spell lands on a real creature: casting at the air or a
	 * Training Dummy earns nothing.
	 */
	public static void onCast(ServerPlayer player, List<RuneDef> runes) {
		Set<String> elements = dev.wildercord.gear.GearBonuses.elements(runes);
		boolean ley = player.getAttachedOrElse(WildercordAttachments.ON_LEY, false);
		credit(player, credit(player).cast(now(player), elements, ley));
	}

	/** Whether a creature counts for the contracts: alive in the world, and not something set up to be hit. */
	private static boolean real(LivingEntity target) {
		return !(target instanceof dev.wildercord.cast.TrainingDummy) && !(target instanceof net.minecraft.world.entity.decoration.Mannequin)
			&& !(target instanceof net.minecraft.world.entity.decoration.ArmorStand);
	}

	private static long now(ServerPlayer player) {
		return player.level().getServer().getTickCount();
	}

	private static ContractRules.Credit credit(ServerPlayer player) {
		return CREDIT.computeIfAbsent(player.getUUID(), id -> new ContractRules.Credit());
	}

	/** Counts whatever just landed toward the player's contracts. */
	private static void credit(ServerPlayer player, ContractRules.Credit.Credited credited) {
		for (ContractRules.Credit.Cast cast : credited.casts()) {
			if (cast.ley()) {
				progress(player, ContractRules.LEY, "", 1);
			}
			for (String element : cast.elements()) {
				progress(player, ContractRules.ELEMENT_CASTS, element, 1);
			}
		}
		for (String reaction : credited.reactions()) {
			progress(player, ContractRules.REACTION, reaction, 1);
		}
	}

	/** The last charge of an imbued item was used: a weapon's counts. */
	public static void onImbueSpent(ServerPlayer player, ItemStack stack) {
		if (stack.is(ItemTags.WEAPON_ENCHANTABLE) || stack.is(ItemTags.BOW_ENCHANTABLE) || stack.is(ItemTags.CROSSBOW_ENCHANTABLE)
				|| stack.is(ItemTags.TRIDENT_ENCHANTABLE) || stack.is(ItemTags.MACE_ENCHANTABLE) || stack.is(ItemTags.SPEARS)) {
			progress(player, ContractRules.IMBUE, "", 1);
		}
	}

	// ------------------------------------------------------------------ the board

	public static long day(ServerPlayer player) {
		return player.level().getServer().overworld().getOverworldClockTime() / 24000L;
	}

	private static long seed(ServerPlayer player) {
		return player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits();
	}

	/** Today's board for this player (a new one each dawn). */
	public static ContractRules.Board board(ServerPlayer player) {
		ContractRules.Board held = player.getAttachedOrElse(BOARD, ContractRules.Board.EMPTY);
		ContractRules.Board today = ContractRules.today(held, day(player), seed(player));
		if (today != held) {
			player.setAttached(BOARD, today);
		}
		return today;
	}

	/** Counts toward today's contracts and tells the player how far along they are. */
	public static void progress(ServerPlayer player, String kind, String arg, int amount) {
		ContractRules.Progress progress = ContractRules.progress(board(player), kind, arg, amount);
		if (!progress.changed()) {
			return;
		}
		player.setAttached(BOARD, progress.board());
		for (int i : progress.advanced()) {
			ContractRules.Contract contract = progress.board().contracts().get(i);
			if (progress.finished().contains(i)) {
				player.sendSystemMessage(Component.translatable("message.wildercord.contract_done", describe(contract).withStyle(ChatFormatting.WHITE))
					.withStyle(ChatFormatting.GOLD));
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
			} else {
				player.sendOverlayMessage(Component.translatable("message.wildercord.contract_progress", describe(contract), contract.progress(), contract.target())
					.withStyle(ChatFormatting.GOLD));
			}
		}
	}

	/** Right-clicking a Scribing Desk: hands in what's finished, then reads the board out. */
	public static void readBoard(ServerPlayer player, BlockPos desk) {
		ContractRules.Claim claim = ContractRules.claim(board(player));
		if (!claim.rewards().isEmpty()) {
			player.setAttached(BOARD, claim.board());
			for (ContractRules.Reward reward : claim.rewards()) {
				give(player, reward);
			}
			player.level().playSound(null, desk, SoundEvents.VILLAGER_WORK_LIBRARIAN, SoundSource.BLOCKS, 1.0F, 1.0F);
			player.level().playSound(null, desk, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
		}
		// The board opens on the player's screen: today's three contracts, and what was just handed in.
		long clock = player.level().getServer().overworld().getOverworldClockTime();
		// With time standing still (or the board kept from a later day after time was turned back), there's no countdown to show.
		boolean turning = player.level().getServer().overworld().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME)
			&& claim.board().day() <= day(player);
		int dawn = turning ? (int) (24000L - Math.floorMod(clock, 24000L)) : -1;
		List<String> handedIn = claim.rewards().stream().map(r -> r.type() + ":" + r.amount()).toList();
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new ShowBoard(claim.board().contracts(), dawn, handedIn));
	}

	/**
	 * Server to client: open the contract board with these contracts, ticks until the next dawn (-1
	 * when time isn't moving toward it), and the rewards just handed in ("type:amount").
	 */
	public record ShowBoard(List<ContractRules.Contract> contracts, int ticksToDawn, List<String> handedIn)
			implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final Type<ShowBoard> TYPE = new Type<>(dev.wildercord.Wildercord.id("contract_board"));
		private static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, ContractRules.Contract> CONTRACT =
			net.minecraft.network.codec.StreamCodec.composite(
				net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, ContractRules.Contract::kind,
				net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, ContractRules.Contract::arg,
				net.minecraft.network.codec.ByteBufCodecs.VAR_INT, ContractRules.Contract::target,
				net.minecraft.network.codec.ByteBufCodecs.VAR_INT, ContractRules.Contract::progress,
				net.minecraft.network.codec.ByteBufCodecs.BOOL, ContractRules.Contract::claimed,
				net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, ContractRules.Contract::reward,
				ContractRules.Contract::new);
		public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, ShowBoard> CODEC =
			net.minecraft.network.codec.StreamCodec.composite(
				CONTRACT.apply(net.minecraft.network.codec.ByteBufCodecs.list(8)), ShowBoard::contracts,
				net.minecraft.network.codec.ByteBufCodecs.INT, ShowBoard::ticksToDawn,
				net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.apply(net.minecraft.network.codec.ByteBufCodecs.list(8)), ShowBoard::handedIn,
				ShowBoard::new);

		@Override
		public Type<ShowBoard> type() {
			return TYPE;
		}
	}

	/** What a reward is called, e.g. "a Tier II rune" or "6 Blank Runes". */
	public static Component rewardLabel(ContractRules.Reward reward) {
		return rewardName(reward);
	}

	/** "Defeat 4 Runebound with Frost spells" and the like. */
	public static MutableComponent describe(ContractRules.Contract contract) {
		String key = "contract.wildercord." + contract.kind();
		return switch (contract.kind()) {
			case ContractRules.RUNEBOUND, ContractRules.ELEMENT_CASTS -> Component.translatable(key, contract.target(),
				Component.translatable("element.wildercord." + contract.arg()).withColor(RuneColors.element(contract.arg())));
			case ContractRules.REACTION -> Component.translatable(key, contract.target(), Component.translatable("contract.wildercord.reaction." + contract.arg()));
			default -> Component.translatable(key, contract.target());
		};
	}

	private static Component rewardName(ContractRules.Reward reward) {
		return switch (reward.type()) {
			case "rune" -> Component.translatable("message.wildercord.contract_reward_rune", RuneItem.roman(reward.amount()));
			case "blank_rune" -> Component.literal(reward.amount() + " ").append(Component.translatable("item.wildercord.blank_rune"));
			case "mana_crystal" -> Component.literal(reward.amount() + " ").append(Component.translatable("item.wildercord.mana_crystal"));
			default -> Component.literal(reward.amount() + " ").append(Component.translatable("item.minecraft.emerald"));
		};
	}

	private static void give(ServerPlayer player, ContractRules.Reward reward) {
		ItemStack stack = switch (reward.type()) {
			case "rune" -> RuneTrades.random(reward.amount(), reward.amount(), new Random(player.getRandom().nextLong())).map(RuneItem::stack).orElse(ItemStack.EMPTY);
			case "blank_rune" -> new ItemStack(WildercordItems.BLANK_RUNE, reward.amount());
			case "mana_crystal" -> new ItemStack(WildercordItems.MANA_CRYSTAL, reward.amount());
			default -> new ItemStack(Items.EMERALD, reward.amount());
		};
		if (stack.isEmpty()) {
			return;
		}
		if (stack.is(WildercordItems.RUNE)) {
			RuneItem.runeOf(stack).ifPresent(rune -> player.sendSystemMessage(Component.translatable("message.wildercord.contract_rune",
				RuneItem.runeName(rune).withColor(RuneColors.of(rune))).withStyle(ChatFormatting.GOLD)));
		}
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}
	}
}
