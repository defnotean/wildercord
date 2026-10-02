package dev.wildercord.aura.world;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.TechniqueRules;
import dev.wildercord.aura.Techniques;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A technique scroll: an old swordsman's notes on one part of a technique (a stroke, a release or an intent), found in the places where
 * swords were drawn in earnest ({@code aura.ScrollSources}), now and then in a fallen knight's armour, and set down by a swordsman whose
 * technique has reached Peerless. Read it (use it) to learn its part for good; one already known leaves the scroll whole, to give away.
 * Every part shares this one item, its part a data component ({@link #PART}), so an add-on's intents have scrolls too; the item model picks
 * the seal's colour by the part's kind.
 */
public class TechniqueScrollItem extends Item {
	/** Which part a scroll teaches, by id. An unknown id (an add-on gone) is kept and teaches nothing. */
	public static final DataComponentType<String> PART = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Wildercord.id("technique_part"),
		DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build()
	);

	private static final ResourceKey<Item> KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("technique_scroll"));
	public static final Item SCROLL = Registry.register(BuiltInRegistries.ITEM, KEY,
		new TechniqueScrollItem(new Item.Properties().setId(KEY).stacksTo(16).rarity(Rarity.UNCOMMON)));

	public TechniqueScrollItem(Properties properties) {
		super(properties);
	}

	/** A scroll of part {@code partId}. */
	public static ItemStack of(String partId) {
		ItemStack stack = new ItemStack(SCROLL);
		stack.set(PART, partId);
		return stack;
	}

	/** The part {@code stack} teaches, if it's a part at all. */
	public static Optional<String> partOf(ItemStack stack) {
		String id = stack.getOrDefault(PART, "");
		return TechniqueRules.family(id).isPresent() ? Optional.of(id) : Optional.empty();
	}

	@Override
	public Component getName(ItemStack stack) {
		return partOf(stack).<Component>map(p -> Component.translatable("item.wildercord.technique_scroll.named", Component.translatable(TechniqueRules.nameKey(p))))
			.orElseGet(() -> Component.translatable("item.wildercord.technique_scroll"));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		String part = partOf(stack).orElse(null);
		if (part == null) {
			return InteractionResult.FAIL;
		}
		if (player instanceof ServerPlayer server) {
			String why = refusal(server, part);
			if (why != null) {
				server.sendOverlayMessage(Component.translatable(why, Component.translatable(TechniqueRules.nameKey(part))).withColor(0xA89CC8));
				return InteractionResult.FAIL;
			}
			if (!Techniques.teach(server, part, "scroll")) {
				return InteractionResult.FAIL;
			}
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** Why {@code player} can't learn {@code part} from a scroll now, as a language key (given the part's name), or null. */
	static String refusal(ServerPlayer player, String part) {
		if (!Aura.enabled(player)) {
			return "message.wildercord.aura.disabled";
		}
		if (!Techniques.on(player)) {
			return "message.wildercord.aura.technique.off";
		}
		if (Aura.stage(player) <= dev.wildercord.aura.AuraRules.NONE) {
			return "message.wildercord.aura.technique.scroll_method";
		}
		if (Techniques.learned(player, part)) {
			return "message.wildercord.aura.technique.scroll_known";
		}
		return null;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		String part = partOf(stack).orElse(null);
		if (part == null) {
			builder.accept(Component.translatable("tooltip.wildercord.technique_scroll.unknown").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		TechniqueRules.Family family = TechniqueRules.family(part).orElseThrow();
		builder.accept(Component.translatable("item.wildercord.technique_scroll.lore").withStyle(ChatFormatting.ITALIC).withColor(0xC8B89A));
		builder.accept(Component.translatable("tooltip.wildercord.technique_scroll.kind",
			Component.translatable("screen.wildercord.aura.writing." + family.id).withColor(0xFF000000 | Techniques.familyColor(family))).withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable(TechniqueRules.nameKey(part) + ".desc").withColor(0xB8A8D8));
		builder.accept(Component.translatable("tooltip.wildercord.technique_scroll.use").withStyle(ChatFormatting.DARK_GRAY));
	}

	public static void init() {
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"));
		CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> {
			for (String part : TechniqueRules.scrollParts()) {
				output.accept(of(part));
			}
		});
	}
}
