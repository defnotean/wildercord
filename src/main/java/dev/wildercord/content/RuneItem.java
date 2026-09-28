package dev.wildercord.content;

import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

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
		ItemStack stack = new ItemStack(WildercordItems.RUNE);
		stack.set(WildercordComponents.RUNE, runeId);
		return stack;
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
		return Component.translatable("item.wildercord.rune.named", runeName(def)).withColor(RuneColors.of(def));
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
		builder.accept(runeDescription(def).withStyle(ChatFormatting.GRAY));
		if (def.tier() > 1) {
			builder.accept(Component.translatable("tooltip.wildercord.needs_cord", Component.translatable(CordTier.forRuneTier(def.tier()).itemKey())).withStyle(ChatFormatting.DARK_GRAY));
		}
		sources(def, builder);
		builder.accept(Component.translatable("tooltip.wildercord.learn").withStyle(ChatFormatting.DARK_AQUA));
	}

	/** Where a rune comes from: its recipe (Tier I-III) and the chests and mobs it's found in. */
	public static void sources(RuneDef def, Consumer<Component> builder) {
		String base = "rune." + def.id().replace(':', '.');
		net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
		if (language.has(base + ".craft")) {
			builder.accept(Component.translatable(base + ".craft").withStyle(ChatFormatting.DARK_GRAY));
		} else if (def.id().startsWith("wildercord:")) {
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
			if (Runes.innate(def) && !serverPlayer.isCreative()) {
				serverPlayer.sendOverlayMessage(Component.translatable("message.wildercord.innate_item").withStyle(ChatFormatting.GRAY));
				return InteractionResult.FAIL;
			}
			if (Spellbooks.knows(serverPlayer, def.id())) {
				serverPlayer.sendOverlayMessage(Component.translatable("message.wildercord.already_known", runeName(def)).withStyle(ChatFormatting.GRAY));
				return InteractionResult.FAIL;
			}
			Spellbooks.learn(serverPlayer, def.id());
			stack.consume(1, player);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.3F);
			serverPlayer.sendOverlayMessage(Component.translatable("message.wildercord.learned", runeName(def).withColor(RuneColors.of(def))));
		}
		return InteractionResult.SUCCESS;
	}

	public static String roman(int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			default -> Integer.toString(n);
		};
	}
}
