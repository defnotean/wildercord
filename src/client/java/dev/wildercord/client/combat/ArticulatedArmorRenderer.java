package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;

/**
 * Renderer-owned armor geometry with vanilla's complete material submission path. The independent
 * preview switch cannot weaken the default unarmored gate. Equipment stays live during hit-stop;
 * only this submission's resolved pose and copied item components are captured.
 */
public final class ArticulatedArmorRenderer {
	public static final String ENABLE_PROPERTY = "wildercord.articulated.armor";
	public static final String VIEW_PROPERTY = "wildercord.articulated.armorArms";
	public static final RenderStateDataKey<Boolean> READY = RenderStateDataKey.create(() -> "wildercord:articulated_armor_ready");
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD};
	private final EquipmentLayerRenderer equipmentRenderer;
	private final EquipmentAssetManager equipmentAssets;
	private final EnumMap<EquipmentSlot, ArticulatedArmorGeometry> models = new EnumMap<>(EquipmentSlot.class);
	private final ArticulatedArmorGeometry viewModel;

	public ArticulatedArmorRenderer(EntityModelSet modelSet, boolean slim, EquipmentLayerRenderer equipmentRenderer, EquipmentAssetManager equipmentAssets) {
		this.equipmentRenderer = equipmentRenderer;
		this.equipmentAssets = java.util.Objects.requireNonNull(equipmentAssets);
		var layers = slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR;
		for (EquipmentSlot slot : SLOTS) models.put(slot, new ArticulatedArmorGeometry(modelSet.bakeLayer(layers.get(slot)), slot));
		viewModel = new ArticulatedArmorGeometry(modelSet.bakeLayer(layers.chest()), EquipmentSlot.CHEST, true);
	}

	public static boolean enabled() { return dev.wildercord.client.CombatPresentation.effective().armor(); }
	public static boolean viewEnabled() { return dev.wildercord.client.CombatPresentation.effective().armorArms(); }

	/** Unknown items/components always retain the entire previous renderer, including their layers. */
	public static boolean compatible(HumanoidRenderState state) {
		if (empty(state.headEquipment) && empty(state.chestEquipment) && empty(state.legsEquipment) && empty(state.feetEquipment)) return true;
		return state instanceof AvatarRenderState && enabled() && Boolean.TRUE.equals(state.getData(READY)) && supports(state);
	}

	public static boolean supports(HumanoidRenderState state) {
		for (EquipmentSlot slot : SLOTS) if (!supported(stack(state, slot), slot)) return false;
		return true;
	}

	/** Enchantments and trims use the actual equipment renderer; no foil or trim approximation. */
	public static boolean supported(ItemStack stack, EquipmentSlot slot) {
		if (empty(stack)) return true;
		boolean item = switch (slot) {
			case HEAD -> stack.is(Items.NETHERITE_HELMET);
			case CHEST -> stack.is(Items.NETHERITE_CHESTPLATE);
			case LEGS -> stack.is(Items.NETHERITE_LEGGINGS);
			case FEET -> stack.is(Items.NETHERITE_BOOTS);
			default -> false;
		};
		var equippable = stack.get(DataComponents.EQUIPPABLE);
		return item && equippable != null && equippable.slot() == slot
			&& equippable.assetId().filter(EquipmentAssets.NETHERITE::equals).isPresent() && equippable.cameraOverlay().isEmpty();
	}

	public boolean submitWorld(PlayerModel body, AvatarRenderState state, PoseStack stack, SubmitNodeCollector collector, int light) {
		if (!enabled() || !supportsAssets() || !supports(state) || !Boolean.TRUE.equals(state.getData(READY))
			|| !(body instanceof ArticulatedModelAccess access) || !access.wildercord$bodyOwned() || access.wildercord$armor() != this
			|| !ArticulatedCombat.applyPlayer(body, state)) return false;
		if (empty(state.headEquipment) && empty(state.chestEquipment) && empty(state.legsEquipment) && empty(state.feetEquipment)) return true;
		// applyPlayer includes vanilla held-item/breathing and bounded head look. Capture AFTER it.
		var palette = capture(access.wildercord$rig()).withHat(state.showHat);
		stack.pushPose();
		try {
			body.root().translateAndRotate(stack);
			for (EquipmentSlot slot : SLOTS) submit(slot, stack(state, slot), models.get(slot), palette, stack, collector, light, state.outlineColor);
		} finally {
			stack.popPose();
		}
		return true;
	}

	/** Intentional enhanced view: only the chestplate's original arm surfaces, never invented gloves. */
	public boolean submitView(ArticulatedRig viewRig, AvatarRenderState state, PoseStack stack, SubmitNodeCollector collector, int light) {
		if (!viewEnabled() || !supportsAssets() || ArticulatedCombat.viewFrame(state) == null || !supports(state) || empty(state.chestEquipment)) return false;
		submit(EquipmentSlot.CHEST, state.chestEquipment, viewModel, capture(viewRig), stack, collector, light, 0);
		return true;
	}

	private void submit(EquipmentSlot slot, ItemStack item, ArticulatedArmorGeometry model, ArticulatedArmorGeometry.Palette palette,
			PoseStack stack, SubmitNodeCollector collector, int light, int outline) {
		if (empty(item)) return;
		ItemStack snapshot = item.copy();
		var equippable = snapshot.get(DataComponents.EQUIPPABLE);
		if (equippable == null || equippable.slot() != slot || equippable.assetId().isEmpty()) throw new IllegalStateException("Armor eligibility changed within submission");
		equipmentRenderer.renderLayers(slot == EquipmentSlot.LEGS ? LayerType.HUMANOID_LEGGINGS : LayerType.HUMANOID,
			equippable.assetId().orElseThrow(), model, palette, snapshot, stack, collector, light, outline);
	}

	/** Immutable model-space matrices in pixels; deferred A/B/A passes never read the live skin rig. */
	public static ArticulatedArmorGeometry.Palette capture(ArticulatedRig rig) {
		return ArticulatedArmorGeometry.Palette.capture(rig);
	}

	private static ItemStack stack(HumanoidRenderState state, EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> state.headEquipment;
			case CHEST -> state.chestEquipment;
			case LEGS -> state.legsEquipment;
			case FEET -> state.feetEquipment;
			default -> ItemStack.EMPTY;
		};
	}
	private static boolean empty(ItemStack stack) { return stack == null || stack.isEmpty(); }
	public boolean supportsAssets() { return ArticulatedArmorAssets.supported(equipmentAssets.get(EquipmentAssets.NETHERITE)); }
	public EquipmentAssetManager equipmentAssets() { return equipmentAssets; }
	public ArticulatedArmorGeometry model(EquipmentSlot slot) { return models.get(slot); }
	public ArticulatedArmorGeometry viewModel() { return viewModel; }
	public EquipmentLayerRenderer equipmentRenderer() { return equipmentRenderer; }
}
