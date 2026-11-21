package dev.wildercord;

import dev.wildercord.cast.TrainingDummy;
import dev.wildercord.familiar.Wisp;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Name tags on Wildercord's own creatures. Vanilla only lets a name tag through when the creature's own interaction passes,
 * and many of ours answer every right click themselves (sitting, brushing, feeding, trading), so a tag could be spent with
 * the name lost or never shown. Here the tag is read first: the name is set, kept on show above the creature, and the
 * creature is kept from despawning. A familiar (which keeps its name on its bond) and the training dummy keep their own rules.
 */
public final class NameTags {
	private NameTags() {}

	public static void init() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			ItemStack held = player.getItemInHand(hand);
			if (!held.is(Items.NAME_TAG) || !ours(entity) || player.isSpectator()) {
				return InteractionResult.PASS;
			}
			Component name = held.get(DataComponents.CUSTOM_NAME);
			if (name == null || !(entity instanceof LivingEntity living) || !living.isAlive()) {
				return InteractionResult.PASS;
			}
			if (!level.isClientSide()) {
				name(living, name, player);
				held.consume(1, player);
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** Whether {@code entity} is one of Wildercord's creatures that a name tag names here. */
	static boolean ours(Entity entity) {
		if (entity instanceof Player || entity instanceof Wisp || entity instanceof TrainingDummy) {
			return false;
		}
		return entity.getType().builtInRegistryHolder().key().identifier().getNamespace().equals(Wildercord.MOD_ID)
			&& entity.getType().canSerialize();
	}

	static void name(LivingEntity entity, Component name, Player by) {
		entity.setCustomName(name);
		entity.setCustomNameVisible(true);
		if (entity instanceof Mob mob) {
			mob.setPersistenceRequired();
		}
	}
}
