package dev.wildercord.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Casting gear in its slots, seen on the wearer by everyone (and in the inventory's paper doll). Each
 * slot's {@link dev.wildercord.gear.GearSlot.Look} says how its piece is worn, using the item's own model:
 * <ul>
 * <li>{@code BACK}: strapped across the back, head up over the right shoulder, leaning a little away from
 * the head, with two leather ties;</li>
 * <li>{@code SHOULDER}: hovering just off the left shoulder, bobbing and slowly turning, with a faint
 * glimmer circling it;</li>
 * <li>{@code HIP}: hanging at the left hip from a belt.</li>
 * </ul>
 * Everything is drawn in the body's space, so it follows a sneak, a swim or a glide, and stands clear of
 * a chestplate, a cape or elytra (each puts the back and front surface out by a different amount). A piece
 * is skipped for an invisible or spectating wearer.
 */
public class GearLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public static final ModelLayerLocation BELT = new ModelLayerLocation(Wildercord.id("gear"), "belt");
	public static final ModelLayerLocation ARMORED_BELT = new ModelLayerLocation(Wildercord.id("gear"), "armored_belt");
	public static final ModelLayerLocation TIE = new ModelLayerLocation(Wildercord.id("gear"), "tie");
	public static final ModelLayerLocation LOOP = new ModelLayerLocation(Wildercord.id("gear"), "loop");
	public static final ModelLayerLocation MOTE = new ModelLayerLocation(Wildercord.id("gear"), "mote");
	private static final Identifier LEATHER = Wildercord.id("textures/entity/gear/leather.png");
	private static final Identifier MOTE_TEXTURE = Wildercord.id("textures/entity/cord/bead.png");

	/** The staff's slant: its sprite runs at 45 degrees, and this turns it to about 63 (steeper, from the hip to over the shoulder). */
	private static final float STAFF_TURN = 18.4F;
	/** How far the head end leans back off the back (degrees). */
	private static final float STAFF_LEAN = 8.0F;
	/** Half of the staff's length along its shaft, in pixels (a 16-pixel sprite's diagonal). */
	private static final float STAFF_HALF = 10.6F;
	private static final float SIN45 = 0.7071F;
	/** The belt's outer face at the front, in pixels: the body (2) and the belt's deformation. */
	private static final float BELT_OUTER = 2.42F;
	private static final float BELT_ARMORED_OUTER = 3.1F;
	/** The tome: its size against the sprite's 16 pixels, where it hangs (pixels left of the middle) and how long its strap is. */
	private static final float TOME_SIZE = 0.4F;
	private static final float TOME_X = 2.2F;
	private static final float LOOP_LENGTH = 3.0F;

	private final GearModel belt;
	private final GearModel armoredBelt;
	private final GearModel tie;
	private final GearModel loop;
	private final GearModel mote;

	public GearLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
		super(parent);
		this.belt = GearModel.of(context.bakeLayer(BELT));
		this.armoredBelt = GearModel.of(context.bakeLayer(ARMORED_BELT));
		this.tie = GearModel.of(context.bakeLayer(TIE));
		this.loop = GearModel.of(context.bakeLayer(LOOP));
		this.mote = GearModel.of(context.bakeLayer(MOTE));
	}

	/** How far out the wearer's surfaces are, in pixels from the body's middle: the back (skin, chestplate, cape, elytra) and the front. */
	private record Surface(float back, float front, boolean armored) {}

	private static Surface surface(AvatarRenderState state) {
		ItemStack chest = state.chestEquipment;
		boolean wings = chest.has(DataComponents.GLIDER);
		boolean armored = !wings && !chest.isEmpty() && chest.has(DataComponents.EQUIPPABLE);
		boolean cape = !wings && state.showCape && state.skin != null && state.skin.cape() != null;
		float back = wings ? 5.0F : cape ? armored ? 4.2F : 3.2F : armored ? 3.05F : 2.3F;
		return new Surface(back, armored ? 3.05F : 2.3F, armored);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector nodes, int light, AvatarRenderState state, float yRot, float xRot) {
		List<GearLook.Piece> pieces = state.getData(GearLook.PIECES);
		if (pieces == null || state.isInvisible || state.isSpectator) {
			return;
		}
		Surface surface = surface(state);
		int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
		for (GearLook.Piece piece : pieces) {
			pose.pushPose();
			getParentModel().body.translateAndRotate(pose);
			switch (piece.slot().look()) {
				case BACK -> back(piece, pose, nodes, light, overlay, state, surface);
				case SHOULDER -> shoulder(piece, pose, nodes, light, overlay, state);
				case HIP -> hip(piece, pose, nodes, light, overlay, state, surface);
			}
			pose.popPose();
		}
	}

	/** The staff, strapped across the back. In item space: +x to the wearer's right, +y up, +z out of the back, in pixels of 1/16. */
	private void back(GearLook.Piece piece, PoseStack pose, SubmitNodeCollector nodes, int light, int overlay, AvatarRenderState state, Surface surface) {
		float sin = Mth.sin(STAFF_LEAN * Mth.DEG_TO_RAD);
		// The middle of the staff: its low end (leaning towards the body) stays just clear of the back.
		float rise = STAFF_HALF * 0.894F * sin;
		float z = surface.back() + 0.9F + rise;
		pose.rotateDegrees(Axis.ZP, 180.0F);
		pose.translate(0.4F / 16, -4.5F / 16, z / 16);
		pose.rotateDegrees(Axis.XP, STAFF_LEAN);
		pose.rotateDegrees(Axis.ZP, STAFF_TURN);
		piece.item().submit(pose, nodes, light, overlay, state.outlineColor);
		// Two ties round the shaft, reaching in to the back.
		for (float along : new float[]{-6.6F, -0.6F}) {
			float y = along * 0.894F;
			float reach = 0.9F + rise + y * sin + 0.1F;
			float depth = reach + 0.75F;
			pose.pushPose();
			pose.translate(along * SIN45 / 16, along * SIN45 / 16, (0.75F - depth / 2) / 16);
			pose.rotateDegrees(Axis.ZP, -45.0F);
			pose.scale(1.0F, 1.0F, depth);
			nodes.submitModel(tie, Unit.INSTANCE, pose, RenderTypes.entityCutout(LEATHER), light, overlay, -1);
			pose.popPose();
		}
	}

	/** A focus, hovering off the left shoulder. */
	private void shoulder(GearLook.Piece piece, PoseStack pose, SubmitNodeCollector nodes, int light, int overlay, AvatarRenderState state) {
		float age = state.ageInTicks;
		float bob = Mth.sin(age * 0.09F) * 1.0F;
		pose.rotateDegrees(Axis.ZP, 180.0F);
		pose.translate(-7.6F / 16, (5.0F + bob) / 16, 0.4F / 16);
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, age * 2.6F);
		pose.scale(0.42F, 0.42F, 0.42F);
		piece.item().submit(pose, nodes, light, overlay, state.outlineColor);
		pose.popPose();
		// A faint glimmer circling it, neutral in colour like the focus is in element.
		for (int i = 0; i < 2; i++) {
			float phase = age * 0.13F + i * Mth.PI;
			pose.pushPose();
			pose.translate(Mth.cos(phase) * 4.4F / 16, Mth.sin(phase * 2) * 0.9F / 16, Mth.sin(phase) * 4.4F / 16);
			float fade = 0.35F + 0.25F * Mth.sin(age * 0.2F + i * 2.0F);
			int alpha = Mth.clamp(Math.round(255 * fade), 0, 255);
			nodes.submitModel(mote, Unit.INSTANCE, pose, RenderTypes.eyes(MOTE_TEXTURE), LightCoordsUtil.FULL_BRIGHT, overlay, (alpha << 24) | 0xE4DCFF);
			pose.popPose();
		}
	}

	/** The tome, hung from a strap on a belt at the left hip. In body space: y down, the front at -z, the wearer's left at +x. */
	private void hip(GearLook.Piece piece, PoseStack pose, SubmitNodeCollector nodes, int light, int overlay, AvatarRenderState state, Surface surface) {
		nodes.submitModel(surface.armored() ? armoredBelt : belt, Unit.INSTANCE, pose, RenderTypes.entityCutout(LEATHER), light, overlay, -1);
		float outer = surface.armored() ? BELT_ARMORED_OUTER : BELT_OUTER;
		float top = GearModel.BELT_TOP + GearModel.BELT_HEIGHT;
		// Swings a little with the wearer's stride, from where the strap meets the belt.
		float swing = Mth.cos(state.walkAnimationPos * 0.6662F) * 9.0F * Math.min(1.0F, state.walkAnimationSpeed * 1.5F);
		pose.pushPose();
		pose.translate(TOME_X / 16, top / 16, -(outer + 0.5F) / 16);
		pose.rotateDegrees(Axis.XP, swing);
		nodes.submitModel(loop, Unit.INSTANCE, pose, RenderTypes.entityCutout(LEATHER), light, overlay, -1);
		// Below the strap, the book hangs with its cover out (item space, turned to face the front).
		pose.translate(0, (LOOP_LENGTH + 0.2F + TOME_SIZE * 8) / 16, -0.75F / 16);
		pose.rotateDegrees(Axis.ZP, 180.0F);
		pose.rotateDegrees(Axis.YP, 180.0F);
		pose.scale(TOME_SIZE, TOME_SIZE, TOME_SIZE);
		piece.item().submit(pose, nodes, light, overlay, state.outlineColor);
		pose.popPose();
	}
}
