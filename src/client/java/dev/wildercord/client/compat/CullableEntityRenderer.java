package dev.wildercord.client.compat;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** 1.21.1 compat seam for non-mob entity renderers; inert in the dropped client. */
public abstract class CullableEntityRenderer<T extends Entity, M extends EntityModel<T>> extends EntityRenderer<T> {
	protected CullableEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
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
		return super.getBoundingBoxForCulling(entity);
	}
}
