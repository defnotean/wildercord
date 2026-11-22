package net.minecraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21.3 stand-in for the 1.21.4 item render state. 1.21.3 resolves and draws items in one call on
 * {@link net.minecraft.client.renderer.entity.ItemRenderer}, with no stacked render state; this class
 * keeps the resolved stack/model so the shared sources and render-state mixins compile. Its
 * {@link #render} goes straight through 1.21.3's real {@code ItemRenderer.render} rather than faking a
 * draw, so an item is still drawn when a caller submits one.
 */
public class ItemStackRenderState {
	public enum FoilType { NONE, STANDARD, SPECIAL }

	/** The 1.21.4 layer state, reduced to the two members the aura/brace mixins shadow or inject. */
	public static class LayerRenderState {
		BakedModel model;
		final ItemStackRenderState field_55345;

		public LayerRenderState(ItemStackRenderState owner) {
			this.field_55345 = owner;
		}

		public void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
			pose.pushPose();
			pose.popPose();
		}
	}

	private ItemStack stack = ItemStack.EMPTY;
	private ItemDisplayContext displayContext = ItemDisplayContext.NONE;
	private LivingEntity entity;
	private BakedModel model;

	void prepare(ItemStack stack, ItemDisplayContext displayContext, LivingEntity entity) {
		this.stack = stack == null ? ItemStack.EMPTY : stack;
		this.displayContext = displayContext == null ? ItemDisplayContext.NONE : displayContext;
		this.entity = entity;
		this.model = null;
	}

	public boolean isEmpty() {
		return stack.isEmpty();
	}

	public ItemTransform transform() {
		return ItemTransform.NO_TRANSFORM;
	}

	public void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		if (stack.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		BakedModel baked = model != null ? model : mc.getItemRenderer().getModel(stack,
			entity != null ? entity.level() : mc.level, entity, 0);
		mc.getItemRenderer().render(stack, displayContext, false, pose, buffers, light, overlay, baked);
	}
}
