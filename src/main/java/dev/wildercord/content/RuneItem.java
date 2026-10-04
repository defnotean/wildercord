package dev.wildercord.content;

import dev.wildercord.player.RuneRanks;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Knots;
import dev.wildercord.spell.WovenRunes;
import dev.wildercord.spell.Ranks;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/** The one item every rune shares. Right-click to learn it forever. */
public class RuneItem extends Item {
	public RuneItem(Properties properties) {
		super(properties);
	}

	/** A rune item for {@code rune}, with its texture selected. */
	public static ItemStack stack(RuneDef rune) {
		return stack(rune.id());
	}

	public static ItemStack stack(String runeId) {
		ItemStack stack = new ItemStack(Knots.isKnot(runeId) ? WildercordItems.KNOT : WovenRunes.isWoven(runeId) ? WildercordItems.WOVEN_RUNE : WildercordItems.RUNE);
		stack.set(WildercordComponents.RUNE, runeId);
		return stack;
	}

	/** A rune item at a rank made by the Fusion Altar (rank I is a plain rune). Ranked runes shimmer. */
	public static ItemStack stack(RuneDef rune, int rank) {
		ItemStack stack = stack(rune.id());
		if (rank > 1) {
			stack.set(WildercordComponents.RANK, Ranks.clamp(rank));
			stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		return stack;
	}

	/** A rune item's rank: 1 unless the Fusion Altar made it higher. */
	public static int rankOf(ItemStack stack) {
		Integer rank = stack.get(WildercordComponents.RANK);
		return rank == null ? 1 : Ranks.clamp(rank);
	}

	/**
	 * A rune item with no rune at all (not one from a missing add-on, which keeps its id) is a blank that
	 * something failed to fill: loot written in an older format, before 0.4.1. Held by a player, it
	 * wakes as a random rune of Tier I to IV, as a Runesmith would pick one. A whole stack becomes the same rune.
	 */
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if (stack.get(WildercordComponents.RUNE) == null && owner instanceof Player) {
			dev.wildercord.runesmith.RuneTrades.random(1, 4, new java.util.Random(level.getRandom().nextLong()))
				.ifPresent(rune -> stack.set(WildercordComponents.RUNE, rune.id()));
		}
	}

	public static Optional<RuneDef> runeOf(ItemStack stack) {
		String id = stack.get(WildercordComponents.RUNE);
		return id == null ? Optional.empty() : Runes.get(id);
	}

	public static MutableComponent runeName(RuneDef def) {
		return Component.translatableWithFallback("rune." + def.id().replace(':', '.'), def.name());
	}

	public static MutableComponent runeDescription(RuneDef def) {
		return Component.translatableWithFallback("rune." + def.id().replace(':', '.') + ".desc", def.description());
	}

	/**
	 * How a rune item's tooltip tells its text: in full here. The client tells it as far as its player has read the rune
	 * (a hint, the text with its numbers veiled, or in full: see {@code spell.RuneReading}), and sets its own.
	 */
	public static volatile java.util.function.Function<RuneDef, MutableComponent> describe = RuneItem::runeDescription;

	/** "Effect · Tier II · Fire", coloured. */
	public static MutableComponent familyLine(RuneDef def) {
		String family = def.family().name().toLowerCase(Locale.ROOT);
		MutableComponent line = Component.translatable("family.wildercord." + family).withColor(RuneColors.of(def))
			.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.translatable("tooltip.wildercord.tier", roman(def.tier())).withStyle(ChatFormatting.GRAY));
		if (!def.element().isEmpty()) {
			line.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
				.append(Component.translatable("element.wildercord." + def.element()).withColor(RuneColors.element(def.element())));
		}
		return line;
	}

	@Override
	public Component getName(ItemStack stack) {
		Optional<RuneDef> rune = runeOf(stack);
		if (rune.isEmpty()) {
			return Component.translatable("item.wildercord.rune.silent").withStyle(ChatFormatting.DARK_GRAY);
		}
		RuneDef def = rune.get();
		if (Knots.isKnot(def)) {
			return Component.translatable("item.wildercord.knot.named", runeName(def)).withColor(RuneColors.of(def));
		}
		int rank = rankOf(stack);
		return rank > 1
			? Component.translatable("item.wildercord.rune.ranked", runeName(def), roman(rank)).withColor(RuneColors.of(def))
			: Component.translatable("item.wildercord.rune.named", runeName(def)).withColor(RuneColors.of(def));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		Optional<RuneDef> rune = runeOf(stack);
		if (rune.isEmpty()) {
			String id = stack.get(WildercordComponents.RUNE);
			builder.accept(Component.translatable("tooltip.wildercord.silent", id == null ? "?" : id).withStyle(ChatFormatting.GRAY));
			return;
		}
		RuneDef def = rune.get();
		builder.accept(familyLine(def));
		if (Knots.isKnot(def)) {
			knotLines(def, builder);
			return;
		}
		builder.accept(describe.apply(def).withStyle(ChatFormatting.GRAY));
		int rank = rankOf(stack);
		if (rank > 1) {
			builder.accept(Component.translatable("tooltip.wildercord.rank", roman(rank), Math.round((Ranks.power(rank) - 1) * 100)).withColor(0xE8C46A));
		}
		if (def.tier() > 1) {
			builder.accept(Component.translatable("tooltip.wildercord.needs_cord", Component.translatable(CordTier.forRuneTier(def.tier()).itemKey())).withStyle(ChatFormatting.DARK_GRAY));
		}
		sources(def, builder);
		builder.accept(Component.translatable("tooltip.wildercord.learn").withStyle(ChatFormatting.DARK_AQUA));
	}

	/** A Knot's tooltip: the spell inside, rune by rune, and what tying it did. */
	private static void knotLines(RuneDef knot, Consumer<Component> builder) {
		List<RuneDef> inside = Knots.contents(knot);
		builder.accept(Component.translatable("tooltip.wildercord.knot.holds", Knots.flatten(inside).size()).withStyle(ChatFormatting.GRAY));
		for (RuneDef rune : inside) {
			knotLine(rune, "  ", builder);
		}
		builder.accept(Component.translatable("tooltip.wildercord.knot.rules", Math.round((1 - Knots.DISCOUNT) * 100)).withStyle(ChatFormatting.DARK_GRAY));
		if (knot.tier() > 1) {
			builder.accept(Component.translatable("tooltip.wildercord.needs_cord", Component.translatable(CordTier.forRuneTier(knot.tier()).itemKey())).withStyle(ChatFormatting.DARK_GRAY));
		}
		builder.accept(Component.translatable("tooltip.wildercord.knot.learn").withStyle(ChatFormatting.DARK_AQUA));
	}

	private static void knotLine(RuneDef rune, String indent, Consumer<Component> builder) {
		builder.accept(Component.literal(indent + "\u2022 ").withStyle(ChatFormatting.DARK_GRAY).append(runeName(rune).withColor(RuneColors.of(rune))));
		if (Knots.isKnot(rune)) {
			for (RuneDef inner : Knots.contents(rune)) {
				knotLine(inner, indent + "  ", builder);
			}
		}
	}

	/** Where a rune comes from: its recipe (Tier I-III) and the chests and mobs it's found in. */
	public static void sources(RuneDef def, Consumer<Component> builder) {
		String base = "rune." + def.id().replace(':', '.');
		net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
		if (language.has(base + ".craft")) {
			builder.accept(Component.translatable(base + ".craft").withStyle(ChatFormatting.DARK_GRAY));
		} else if (def.id().startsWith("wildercord:") && !Runes.fused(def)) {
			// A rune of the world is found only in its own places, whatever its tier.
			String why = dev.wildercord.spell.RuneSources.foundOnly(def) ? "tooltip.wildercord.found_only" : "tooltip.wildercord.not_craftable";
			builder.accept(Component.translatable(why).withStyle(ChatFormatting.DARK_GRAY));
		}
		if (language.has(base + ".found")) {
			builder.accept(Component.translatable(base + ".found").withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Optional<RuneDef> rune = runeOf(stack);
		if (rune.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			RuneDef def = rune.get();
			if(!dev.wildercord.cast.SoulWeaving.owns(serverPlayer,java.util.List.of(def))){
				serverPlayer.sendOverlayMessage(Component.translatable("message.wildercord.innate_other_heart").withStyle(ChatFormatting.GRAY));return InteractionResult.FAIL;
			}
			if (Runes.innate(def) && !serverPlayer.isCreative()) {
				serverPlayer.sendOverlayMessage(Component.translatable("message.wildercord.innate_item").withStyle(ChatFormatting.GRAY));
				return InteractionResult.FAIL;
			}
			int rank = rankOf(stack);
			boolean known = Spellbooks.knows(serverPlayer, def.id());
			if (known && rank <= RuneRanks.rank(serverPlayer, def.id())) {
				serverPlayer.sendOverlayMessage(Component.translatable("message.wildercord.already_known", runeName(def)).withStyle(ChatFormatting.GRAY));
				return InteractionResult.FAIL;
			}
			Spellbooks.learn(serverPlayer, def.id());
			if (!known) {
				// Learning something new is an arcane thing.
				dev.wildercord.cast.PlayerAffinities.learnedRune(serverPlayer);
				// It's known now, not yet understood: its text stays a hint until it's been cast and seen at work.
				dev.wildercord.cast.RuneReadings.learned(serverPlayer, def);
			}
			// A higher rank upgrades the rune everywhere it's threaded: every spell reads it from here.
			RuneRanks.raise(serverPlayer, def.id(), rank);
			stack.consume(1, player);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, rank > 1 ? 1.6F : 1.3F);
			Component name = runeName(def).withColor(RuneColors.of(def));
			serverPlayer.sendOverlayMessage(rank > 1
				? Component.translatable(known ? "message.wildercord.ranked_up" : "message.wildercord.learned_ranked", name, roman(rank))
				: Component.translatable("message.wildercord.learned", name));
		}
		return InteractionResult.SUCCESS;
	}

	public static String roman(int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> Integer.toString(n);
		};
	}
}
