package dev.wildercord.client.render;

import dev.wildercord.backpack.BackpackTier;
import dev.wildercord.backpack.Backpacks;
import dev.wildercord.gear.GearSlot;
import dev.wildercord.gear.GearSlots;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The casting gear a player wears in its slots, and their worn backpack, carried from the entity into the
 * render state as it is extracted (see {@code AvatarRendererGearMixin}): a layer only ever sees the state,
 * and an item's model has to be resolved with the entity in hand. {@link GearLayer} draws it.
 */
public final class GearLook {
	private GearLook() {}

	/** One piece worn: the slot it's in (which decides how it's drawn) and its resolved model. */
	public record Piece(GearSlot slot, ItemStackRenderState item) {}

	/** A worn backpack: which one, and its colour (ARGB, its own unless it's been dyed). */
	public record Pack(BackpackTier tier, int color) {}

	/** What to draw this frame; absent means nothing worn. */
	public static final RenderStateDataKey<List<Piece>> PIECES = RenderStateDataKey.create(() -> "wildercord:gear_pieces");
	/** The backpack on their back; absent means none. */
	public static final RenderStateDataKey<Pack> PACK = RenderStateDataKey.create(() -> "wildercord:backpack");

	public static void extract(Avatar avatar, AvatarRenderState state) {
		ItemStack backpack = Backpacks.look(avatar);
		BackpackTier tier = Backpacks.tierOf(backpack);
		state.setData(PACK, tier == null ? null : new Pack(tier, DyedItemColor.getOrDefault(backpack, 0xFF000000 | tier.color)));
		Map<GearSlot, ItemStack> worn = GearSlots.equipped(avatar);
		if (worn.isEmpty()) {
			state.setData(PIECES, null);
			return;
		}
		List<Piece> pieces = new ArrayList<>(worn.size());
		for (Map.Entry<GearSlot, ItemStack> entry : worn.entrySet()) {
			ItemStack stack = entry.getValue();
			// A copy of it in a hand is the one to see: no second staff on the back.
			if (avatar.getMainHandItem().is(stack.getItem()) || avatar.getOffhandItem().is(stack.getItem())) {
				continue;
			}
			ItemStackRenderState item = new ItemStackRenderState();
			Minecraft.getInstance().getItemModelResolver().updateForLiving(item, stack, ItemDisplayContext.NONE, avatar);
			if (!item.isEmpty()) {
				pieces.add(new Piece(entry.getKey(), item));
			}
		}
		state.setData(PIECES, pieces.isEmpty() ? null : List.copyOf(pieces));
	}
}
