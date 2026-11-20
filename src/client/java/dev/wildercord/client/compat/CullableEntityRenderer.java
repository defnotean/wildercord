package dev.wildercord.client.compat;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** Culling seam for non-mob entity renderers whose 26.2 vs 26.3 override signatures differ on {@code partial}. */
public abstract class CullableEntityRenderer<T extends Entity, S extends EntityRenderState> extends EntityRenderer<T, S> {
	protected CullableEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public final boolean shouldRender(T entity, Frustum frustum, double x, double y, double z, float partial) {
		return shouldRenderEntity(entity, frustum, x, y, z, partial);
	}

	@Override
	protected final AABB getBoundingBoxForCulling(T entity, float partial) {
		return cullingBounds(entity, partial);
	}

	protected boolean shouldRenderEntity(T entity, Frustum frustum, double x, double y, double z, float partial) {
		return superShouldRenderEntity(entity, frustum, x, y, z, partial);
	}

	protected final boolean superShouldRenderEntity(T entity, Frustum frustum, double x, double y, double z, float partial) {
		return super.shouldRender(entity, frustum, x, y, z, partial);
	}

	protected AABB cullingBounds(T entity, float partial) {
		return superCullingBounds(entity, partial);
	}

	protected final AABB superCullingBounds(T entity, float partial) {
		return super.getBoundingBoxForCulling(entity, partial);
	}
}
