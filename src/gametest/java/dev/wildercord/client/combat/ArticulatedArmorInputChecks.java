package dev.wildercord.client.combat;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.world.entity.EquipmentSlot;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared native-fixture and pristine-runtime negative tests; never mutates game resources. */
public final class ArticulatedArmorInputChecks {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final Set<LayerType> STOCK = Set.of(LayerType.HUMANOID, LayerType.HUMANOID_LEGGINGS,
		LayerType.HUMANOID_BABY, LayerType.HORSE_BODY, LayerType.NAUTILUS_BODY);
	private ArticulatedArmorInputChecks() {}

	public static int verify(EntityModelSet roots, EquipmentClientInfo stock) {
		int checked = 0;
		for (boolean slim : new boolean[] {false, true}) {
			var layers = slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR;
			for (EquipmentSlot slot : SLOTS) {
				ModelPart source = roots.bakeLayer(layers.get(slot));
				new ArticulatedArmorGeometry(source, slot); checked++;
				reject(() -> new ArticulatedArmorGeometry(new ModelPart(List.of(), Map.of()), slot), "Empty " + slot); checked++;
				List<String> paths = new ArrayList<>();
				source.visit(new PoseStack(), (pose, path, index, cube) -> paths.add(path));
				for (String path : paths) {
					reject(() -> new ArticulatedArmorGeometry(without(source, path), slot), "Missing " + path + " in " + slot); checked++;
				}
				for (EquipmentSlot wrong : SLOTS) if (slot != wrong) {
					reject(() -> new ArticulatedArmorGeometry(source, wrong), "Wrong slot " + slot + " as " + wrong); checked++;
				}
				if (slot != EquipmentSlot.CHEST) {
					reject(() -> new ArticulatedArmorGeometry(source, slot, true), "Non-chest first-person source"); checked++;
				}
			}
			ModelPart chest = roots.bakeLayer(layers.chest());
			new ArticulatedArmorGeometry(chest, EquipmentSlot.CHEST, true); checked++;
			for (String path : List.of("/body", "/right_arm", "/left_arm")) {
				reject(() -> new ArticulatedArmorGeometry(without(chest, path), EquipmentSlot.CHEST, true), "Incomplete source before arm filtering " + path); checked++;
			}
			var wrongInflation = children(chest);
			wrongInflation.put("body", roots.bakeLayer(layers.legs()).getChild("body"));
			reject(() -> new ArticulatedArmorGeometry(new ModelPart(List.of(), wrongInflation), EquipmentSlot.CHEST), "Leggings inflation in a chest slot"); checked++;
			var extra = children(chest);
			extra.put("right_leg", roots.bakeLayer(layers.feet()).getChild("right_leg"));
			reject(() -> new ArticulatedArmorGeometry(new ModelPart(List.of(), extra), EquipmentSlot.CHEST, true), "Extra geometry before arm filtering"); checked++;
			reject(() -> new ArticulatedArmorGeometry(chest, EquipmentSlot.MAINHAND), "Non-armor slot"); checked++;
		}
		check(ArticulatedArmorAssets.supported(stock), "Resolved stock netherite remains supported"); checked++;
		check(!ArticulatedArmorAssets.supported(new EquipmentClientInfo(Map.of(), List.of())), "Missing resolved asset uses fallback"); checked++;
		for (LayerType layer : LayerType.values()) if (!STOCK.contains(layer)) {
			Map<LayerType, List<EquipmentClientInfo.Layer>> changed = new EnumMap<>(stock.layers());
			changed.put(layer, stock.getLayers(LayerType.HUMANOID));
			check(!ArticulatedArmorAssets.supported(new EquipmentClientInfo(changed, stock.trimOverrides())), "Additional " + layer + " geometry uses fallback"); checked++;
		}
		for (LayerType required : List.of(LayerType.HUMANOID, LayerType.HUMANOID_LEGGINGS)) {
			Map<LayerType, List<EquipmentClientInfo.Layer>> changed = new EnumMap<>(stock.layers()); changed.remove(required);
			check(!ArticulatedArmorAssets.supported(new EquipmentClientInfo(changed, stock.trimOverrides())), "Missing " + required + " material uses fallback"); checked++;
		}
		Map<LayerType, List<EquipmentClientInfo.Layer>> textureOnly = new EnumMap<>(stock.layers());
		textureOnly.put(LayerType.HUMANOID, List.of(new EquipmentClientInfo.Layer(net.minecraft.resources.Identifier.withDefaultNamespace("diamond")),
			EquipmentClientInfo.Layer.leatherDyeable(net.minecraft.resources.Identifier.withDefaultNamespace("leather"), true)));
		textureOnly.put(LayerType.WINGS, List.of());
		check(ArticulatedArmorAssets.supported(new EquipmentClientInfo(textureOnly, stock.trimOverrides())), "Texture/dye layers and empty unused layer definitions remain supported"); checked++;
		return checked;
	}

	private static ModelPart without(ModelPart source, String path) {
		Map<String, ModelPart> children = children(source);
		if (path.equals("/head/hat")) {
			ModelPart head = source.getChild("head");
			List<ModelPart.Cube> cubes = new ArrayList<>();
			head.visit(new PoseStack(), (pose, relative, index, cube) -> { if (relative.isEmpty()) cubes.add(cube); });
			ModelPart noHat = new ModelPart(cubes, Map.of()); noHat.setInitialPose(head.getInitialPose()); noHat.resetPose();
			children.put("head", noHat);
		} else children.remove(path.substring(1));
		return new ModelPart(List.of(), children);
	}
	private static Map<String, ModelPart> children(ModelPart root) {
		Map<String, ModelPart> result = new LinkedHashMap<>();
		for (String name : List.of("head", "body", "right_arm", "left_arm", "right_leg", "left_leg"))
			if (root.hasChild(name)) result.put(name, root.getChild(name));
		return result;
	}
	private static void reject(Runnable create, String message) {
		try { create.run(); } catch (IllegalArgumentException expected) { return; }
		throw new AssertionError("Accepted unsupported source: " + message);
	}
	private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }

	public static void main(String[] args) throws Exception {
		try (var stream = ArticulatedArmorInputChecks.class.getResourceAsStream("/assets/minecraft/equipment/netherite.json")) {
			if (stream == null) throw new IllegalStateException("Missing official netherite asset");
			var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			var stock = EquipmentClientInfo.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
			int checked = verify(EntityModelSet.vanilla(), stock);
			System.out.println("{\"kind\":\"offline slot and resolved-asset validation\",\"checks\":" + checked + ",\"passes\":true,\"native\":\"not run\"}");
		}
	}
}
