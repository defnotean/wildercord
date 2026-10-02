package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtLight;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.PowerPlaces;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A Crossroads Incense: how a swordsman changes their Way. Burned (held in use for two seconds) at a place of power (a ley crossing,
 * where breakthroughs are earned; the server can lift that with {@code aura.way_change_at_power}), it unbinds the Way they walk (its
 * nodes go dark, its standard rising out of them and breaking) and the crossroads rises round them at once to choose again. The new
 * Way's later nodes wake only once they've earned a stretch of experience walking it ({@link WayRules#SETTLE_XP}).
 *
 * <p>Why it costs what it does: the incense is made from two Aura Shards (a fallen knight's), an amethyst shard and blaze powder, so
 * changing takes a fight and a journey; it must be burned at a place of power, so it's a pilgrimage, not a click; and the settling
 * means a new Way is walked into, not bought, even for a Sovereign with shards to spare. Nothing is lost for good: any Way can be
 * walked again, at the same price.</p>
 */
public class CrossroadsIncense extends Item {
	private static final ResourceKey<Item> KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("crossroads_incense"));
	public static final Item INCENSE = Registry.register(BuiltInRegistries.ITEM, KEY, new CrossroadsIncense(new Item.Properties().setId(KEY)
		.stacksTo(16).rarity(Rarity.RARE)
		.component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(2.0F).animation(ItemUseAnimation.BLOCK)
			.sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.CAMPFIRE_CRACKLE)).hasConsumeParticles(false).build())));

	public CrossroadsIncense(Properties properties) {
		super(properties);
	}

	/** Why {@code player} can't burn one here, or null when they can. Both sides (the place of power only on the server). */
	static String refusal(Player player) {
		if (!Ways.on(player)) {
			return "message.wildercord.aura.way.incense_off";
		}
		if (Aura.stage(player) < WayRules.FROM) {
			return "message.wildercord.aura.way.incense_stage";
		}
		if (Ways.way(player).isEmpty()) {
			return "message.wildercord.aura.way.incense_none";
		}
		if (player instanceof ServerPlayer server && Config.get().aura().ways().changeAtPower()
				&& !PowerPlaces.isPlaceOfPower(server.level(), server.blockPosition())) {
			return "message.wildercord.aura.way.incense_place";
		}
		return null;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		String why = refusal(player);
		if (why != null) {
			if (player instanceof ServerPlayer server) {
				server.sendOverlayMessage(Component.translatable(why).withColor(0xA89CC8));
			}
			return InteractionResult.FAIL;
		}
		return super.use(level, player, hand);
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
		// Smoke curling up from the incense as it burns.
		if (level instanceof ServerLevel server && entity instanceof ServerPlayer player && remaining % 4 == 0) {
			Vec3 hand = player.getEyePosition().add(player.getViewVector(1.0F).scale(0.5)).subtract(0, 0.35, 0);
			int color = Ways.way(player).map(AuraApi.Way::color).orElse(0xB8A8C8);
			Motes.clouds(server, hand, 2, 0.08, AuraRules.mix(color, 0x5A5468, 0.6), 0.22, 30, new Vec3(0, 0.04, 0), 0.015, 0.4);
		}
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) {
			return stack;
		}
		String why = refusal(player);
		if (why != null) {
			player.sendOverlayMessage(Component.translatable(why).withColor(0xA89CC8));
			return stack;
		}
		Optional<AuraApi.Way> left = Ways.unbind(player);
		if (left.isEmpty()) {
			return stack;
		}
		unbound(player, left.get());
		stack.consume(1, player);
		// The crossroads rises where the incense burned, for the choice to be made again.
		dev.wildercord.cast.Scheduler.later(30, () -> {
			if (player.isAlive() && !player.hasDisconnected()) {
				Crossroads.open(player, Crossroads.Reason.INCENSE);
			}
		});
		return stack;
	}

	/** The Way leaving: its standard rising out of the swordsman and breaking apart, smoke, and a falling note. */
	private static void unbound(ServerPlayer player, AuraApi.Way way) {
		ServerLevel level = player.level();
		int color = way.color();
		Vec3 feet = player.position();
		ArtLight.spectacle(player).ray(feet.add(0, 0.2, 0), feet.add(0, 3.4, 0), AuraVfx.hot(color, 0.4), 0.08, 16);
		ArtLight.world(player).shards(feet.add(0, 3.0, 0), 1.3, 10, color, AuraVfx.hot(color, 0.5)).flash(feet.add(0, 3.0, 0), color, 1.6F);
		Motes.clouds(level, feet.add(0, 1.0, 0), 10, 0.4, AuraRules.mix(color, 0x4A4458, 0.65), 0.35, 40, new Vec3(0, 0.03, 0), 0.02, 0.5);
		Feels.sound(level, feet.add(0, 1, 0), "aura_way_unbound", 1.0F, 1.0F);
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.way.unbound", Component.translatable(way.nameKey())
			.withColor(0xFF000000 | color)).withColor(0xE8D8B0));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		out.accept(Component.translatable("item.wildercord.crossroads_incense.lore").withStyle(ChatFormatting.ITALIC).withColor(0xB8A8D8));
		out.accept(Component.translatable("item.wildercord.crossroads_incense.use").withColor(0xB8D8A8));
	}

	static void init() {
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"));
		CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> output.accept(INCENSE));
	}
}
