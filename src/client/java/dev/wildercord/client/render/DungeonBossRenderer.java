package dev.wildercord.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.cast.DungeonBoss;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.function.ToIntFunction;

/**
 * What the dungeon bosses' renderers share, after {@link ArchivistRenderer}: the model on its own
 * skin, emissive layers drawn over it at full brightness whatever the light (each with its strength
 * worked out per frame), and a death without the vanilla topple: each model plays its own.
 */
public abstract class DungeonBossRenderer<T extends DungeonBoss, M extends EntityModel<DungeonBossRenderState>>
		extends MobRenderer<T, DungeonBossRenderState, M> {
	private final ResourceLocation texture;

	protected DungeonBossRenderer(EntityRendererProvider.Context context, M model, float shadow, ResourceLocation texture) {
		super(context, model, shadow);
		this.texture = texture;
	}

	/** Adds a layer holding only what glows, drawn at full brightness with the ARGB {@code color} gives each frame. */
	protected void glow(ResourceLocation texture, ToIntFunction<DungeonBossRenderState> color) {
		addLayer(new Glow<>(this, texture, color));
	}

	@Override
	public ResourceLocation getTextureLocation(DungeonBossRenderState state) {
		return texture;
	}

	@Override
	public DungeonBossRenderState createRenderState() {
		return new DungeonBossRenderState();
	}

	@Override
	public void extractRenderState(T boss, DungeonBossRenderState state, float partial) {
		super.extractRenderState(boss, state, partial);
		state.casting = boss.castPose(partial);
		state.shifting = boss.shiftPose(partial);
		state.guarded = boss.guardPose(partial);
		state.exposed = boss.exposedPose(partial);
		state.slamming = boss.slamPose(partial);
		state.phase = boss.phase();
		state.rootBindings = boss instanceof dev.wildercord.cast.RootGuardian root ? root.bindings() : 3;
		state.move = boss.currentMove();
		state.since = boss.moveSince(partial);
		state.tell = boss.moveTell();
		for (int i = 1; i < DungeonBoss.MOVES; i++) {
			state.moves[i] = boss.moveBlend(i, partial);
		}
		for (int i = 0; i < DungeonBoss.EXTRA; i++) {
			state.extra[i] = boss.extra(i, partial);
		}
	}

	@Override
	protected float getFlipDegrees() {
		return 0;
	}

	/** An alpha (0 to 1) as the white ARGB a glow layer is tinted with. */
	protected static int white(float alpha) {
		return (Math.max(0, Math.min(255, (int) (alpha * 255))) << 24) | 0xFFFFFF;
	}

	/** How much a glow has faded as it dies: 1 alive, 0 when it's gone. */
	protected static float alive(DungeonBossRenderState state) {
		return state.deathTime > 0 ? Math.max(0, 1 - state.deathTime / (float) DungeonBoss.DEATH_TICKS) : 1;
	}

	/** An emissive copy of the model with a texture holding only what glows, its strength per frame. */
	private static final class Glow<M extends EntityModel<DungeonBossRenderState>> extends RenderLayer<DungeonBossRenderState, M> {
		private final RenderType renderType;
		private final ToIntFunction<DungeonBossRenderState> color;

		Glow(RenderLayerParent<DungeonBossRenderState, M> parent, ResourceLocation texture, ToIntFunction<DungeonBossRenderState> color) {
			super(parent);
			this.renderType = RenderType.eyes(texture);
			this.color = color;
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource buffer, int light, DungeonBossRenderState state, float yRot, float xRot) {
			if (state.isInvisible) {
				return;
			}
			int argb = color.applyAsInt(state);
			if ((argb >>> 24) < 4) {
				return;
			}
			dev.wildercord.client.compat.RenderCompat.submitModel(buffer, getParentModel(), state, poseStack, renderType, light, OverlayTexture.NO_OVERLAY, argb, dev.wildercord.client.compat.RenderCompat.outlineOf(state));
		}
	}
}
