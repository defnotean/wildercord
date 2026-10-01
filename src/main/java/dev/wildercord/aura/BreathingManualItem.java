package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * A breathing manual: an old fighter's notes on one way of breathing. Read it (use it) to learn the method; one already
 * breathing another way is asked to read it again to be sure, since switching costs the road to their next breakthrough.
 * Every method shares this one item, its method a data component ({@code wildercord:breathing_method}), so an add-on's
 * methods have manuals too; the item model picks each method's cover from the component.
 */
public class BreathingManualItem extends Item {
	/** Which method a manual teaches, by id. An unknown id (an add-on gone) is kept and teaches nothing. */
	public static final DataComponentType<String> METHOD = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Wildercord.id("breathing_method"),
		DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build()
	);

	private static final ResourceKey<Item> KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("breathing_manual"));
	public static final Item MANUAL = Registry.register(BuiltInRegistries.ITEM, KEY,
		new BreathingManualItem(new Item.Properties().setId(KEY).stacksTo(16).rarity(Rarity.UNCOMMON)));

	public BreathingManualItem(Properties properties) {
		super(properties);
	}

	/** A manual of {@code method}. */
	public static ItemStack of(String method) {
		ItemStack stack = new ItemStack(MANUAL);
		stack.set(METHOD, method);
		return stack;
	}

	public static java.util.Optional<BreathingMethod> methodOf(ItemStack stack) {
		return BreathingMethods.byId(stack.getOrDefault(METHOD, ""));
	}

	@Override
	public Component getName(ItemStack stack) {
		return methodOf(stack).<Component>map(m -> Component.translatable("item.wildercord.breathing_manual.named", Component.translatable(m.nameKey())))
			.orElseGet(() -> Component.translatable("item.wildercord.breathing_manual"));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		BreathingMethod method = methodOf(stack).orElse(null);
		if (method == null) {
			return InteractionResult.FAIL;
		}
		if (player instanceof ServerPlayer server) {
			if (!AuraMethods.learn(server, method, "manual", false)) {
				return InteractionResult.FAIL;
			}
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		BreathingMethod method = methodOf(stack).orElse(null);
		if (method == null) {
			builder.accept(Component.translatable("tooltip.wildercord.breathing_manual.unknown").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		String key = method.nameKey();
		builder.accept(Component.translatable(key + ".lore").withStyle(ChatFormatting.ITALIC).withColor(0xC8B89A));
		if (!method.element().isEmpty()) {
			builder.accept(Component.translatable("tooltip.wildercord.breathing_manual.element",
				Component.translatable("element.wildercord." + method.element()).withColor(method.color())).withStyle(ChatFormatting.GRAY));
		}
		builder.accept(Component.translatable(key + ".flavour").withColor(0xB8A8D8));
		builder.accept(Component.translatable("tooltip.wildercord.breathing_manual.use").withStyle(ChatFormatting.DARK_GRAY));
	}

	public static void init() {}
}
