package dev.wildercord.client.compat;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/** 1.21.1 compat seam for mob renderers; inert in the dropped client. */
public abstract class CullableMobRenderer<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
	protected CullableMobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
		super(context, model, shadowRadius);
	}

	protected boolean shouldRenderEntity(T entity, Frustum frustum, double x, double y, double z, float partial) {
		return superShouldRenderEntity(entity, frustum, x, y, z, partial);
	}

	protected final boolean superShouldRenderEntity(T entity, Frustum frustum, double x, double y, double z, float partial) {
		return super.shouldRender(entity, frustum, x, y, z);
	}

	protected AABB cullingBounds(T entity, float partial) {
		return superCullingBounds(entity, partial);
	}

	protected final AABB superCullingBounds(T entity, float partial) {
		return entity.getBoundingBox();
	}
}
