package dev.wildercord.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.model.monster.zombie.ZombieVillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import org.jspecify.annotations.Nullable;

/**
 * Rune marks: a Runebound's Cord written on its body in glyphs of light, in its spell's colour.
 * The monster's own model is drawn a second time, emissive, with a texture of white marks laid out
 * for that model's skin (drawn by {@code tools/world_art.py}, tinted here). The marks breathe
 * slowly, and flare from the moment a cast is telegraphed until it leaves the monster's hand.
 *
 * <p>Added to every monster renderer whose model has a marks texture: zombies, husks, drowned and
 * zombified piglins, every skeleton, zombie villagers, witches and illagers. Babies (their own
 * model in 26.x) and any other monster bound by command get only the aura ({@code RuneAura}).</p>
 */
public class RuneMarksLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
	/** The marks' colour this frame, as ARGB with the alpha as brightness; absent on anything that isn't Runebound. */
	public static final RenderStateDataKey<Integer> MARKS = RenderStateDataKey.create(() -> "wildercord:rune_marks");

	/** How long a Runebound telegraphs, in ticks (as {@code Runebound.TELEGRAPH}). */
	private static final float TELEGRAPH = 22;
	/** Ticks the flare takes to reach full strength once the telegraph begins. */
	private static final float FLARE_IN = 4;
	/** Ticks the flare takes to die down after the cast. */
	private static final float FLARE_OUT = 8;

	private final RenderType renderType;

	public RuneMarksLayer(RenderLayerParent<S, M> parent, Identifier texture) {
		super(parent);
		this.renderType = RenderTypes.eyes(texture);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		Integer marks = state.getData(MARKS);
		if (marks == null || state.isInvisible || state.isBaby) {
			return;
		}
		collector.order(1).submitModel(getParentModel(), state, poseStack, renderType, light, OverlayTexture.NO_OVERLAY, marks, null,
			state.outlineColor);
	}

	/** Reads the synced marks into the render state (called as every living entity's state is extracted). */
	public static void extract(LivingEntity entity, LivingEntityRenderState state, float partial) {
		WildercordAttachments.RuneMarks marks = entity.getAttached(WildercordAttachments.RUNE_MARKS);
		if (marks == null) {
			return;
		}
		float t = entity.tickCount + partial;
		// A slow breath, out of step between monsters.
		float glow = (marks.adept() ? 0.92F : 0.82F) + 0.12F * Mth.sin(t * 0.07F + entity.getId() * 1.7F);
		float flare = 0;
		if (marks.castAt() > 0) {
			float left = marks.castAt() - (entity.level().getGameTime() + partial);
			if (left >= 0 && left <= TELEGRAPH + 2) {
				flare = Mth.clamp((TELEGRAPH + 2 - left) / FLARE_IN, 0, 1);
			} else if (left < 0 && left > -FLARE_OUT) {
				flare = 1 + left / FLARE_OUT;
			}
		}
		glow = Math.min(1, glow + flare * 0.5F);
		if (state.deathTime > 0) {
			glow *= Math.max(0, 1 - state.deathTime / 16F);
		}
		// Flaring, the colour runs hot toward white.
		int rgb = hot(marks.color(), 0.04F + flare * 0.5F);
		state.setData(MARKS, (Mth.clamp((int) (glow * 255), 0, 255) << 24) | rgb);
	}

	private static int hot(int rgb, float t) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return ((r + Math.round((255 - r) * t)) << 16) | ((g + Math.round((255 - g) * t)) << 8) | (b + Math.round((255 - b) * t));
	}

	// ------------------------------------------------------------------ registration

	public static void register() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (type.getCategory() != MobCategory.MONSTER) {
				return;
			}
			Identifier texture = textureFor(type, renderer);
			if (texture != null) {
				add(renderer, helper, texture);
			}
		});
	}

	/** The marks drawn for a model's skin layout, or null for a model that has none. */
	private static @Nullable Identifier textureFor(EntityType<?> type, LivingEntityRenderer<?, ?, ?> renderer) {
		EntityModel<?> model = renderer.getModel();
		String layout;
		if (model instanceof IllagerModel<?>) {
			layout = "illager";
		} else if (model instanceof WitchModel) {
			layout = "witch";
		} else if (model instanceof ZombieVillagerModel<?>) {
			layout = "zombie_villager";
		} else if (model instanceof SkeletonModel<?>) {
			// The parched is a skeleton with a second, looser body over the first, on a taller skin.
			layout = type == EntityTypes.PARCHED ? "parched" : "skeleton";
		} else if (model instanceof HumanoidModel<?>) {
			layout = "humanoid";
		} else {
			return null;
		}
		return Wildercord.id("textures/entity/runebound/" + layout + ".png");
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void add(LivingEntityRenderer<?, ?, ?> renderer, LivingEntityRenderLayerRegistrationCallback.RegistrationHelper helper,
			Identifier texture) {
		helper.register(new RuneMarksLayer((RenderLayerParent) renderer, texture));
	}
}
