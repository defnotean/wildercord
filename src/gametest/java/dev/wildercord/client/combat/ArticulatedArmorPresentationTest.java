package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.client.fx.HitStop;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.GameType;
import org.joml.Matrix4f;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Native acceptance fixture, deliberately distinct from CPU previews. Synthetic probes exercise
 * real baked player/armor models and vanilla equipment submissions. Screenshots below use actual
 * key input and a server-accepted Spellcut; no synthetic frame is injected into those captures.
 * Merely compiling/registering this fixture is not evidence that its native assertions passed.
 */
public final class ArticulatedArmorPresentationTest implements FabricClientGameTest {
	private static final int LIGHT = 0x00F000F0;
	private static final int OUTLINE = 0xFF48AACC;
	private static final EquipmentSlot[] ARMOR_ORDER = {
		EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		String[] properties = {ArticulatedCombat.ENABLE_PROPERTY, ArticulatedArmorRenderer.ENABLE_PROPERTY,
			ArticulatedArmorRenderer.VIEW_PROPERTY};
		String[] previous = Arrays.stream(properties).map(System::getProperty).toArray(String[]::new);
		CameraType camera = context.computeOnClient(mc -> mc.options.getCameraType());
		HumanoidArm hand = context.computeOnClient(mc -> mc.options.mainHand().get());
		for (String property : properties) System.setProperty(property, "true");
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(30);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("fill -10 99 -10 10 99 10 minecraft:stone_bricks");
			world.getServer().runCommand("fill -10 100 -10 10 108 10 minecraft:air");
			world.getServer().runCommand("time set 3000");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(server.overworld(), .5, 100, .5, Set.<Relative>of(), 0, 0, false);
				prepare(player, true);
			});
			context.waitTicks(8);
			context.runOnClient(ArticulatedArmorPresentationTest::syntheticMatrix);

			// This is a real resource reload, not a second call to a fixture constructor.
			ArticulatedModelAccess beforeReload = context.computeOnClient(mc -> access(renderer(mc).getModel()));
			var reload = context.computeOnClient(Minecraft::reloadResourcePacks);
			context.waitFor(mc -> reload.isDone(), 1200);
			reload.join();
			context.runOnClient(mc -> {
				ArticulatedModelAccess afterReload = access(renderer(mc).getModel());
				check(afterReload != beforeReload && afterReload.wildercord$bodyOwned(), "Resource reload creates a fresh primary body owner");
				check(afterReload.wildercord$rig() != beforeReload.wildercord$rig(), "Reload never reuses the old body rig");
				check(afterReload.wildercord$armor() != beforeReload.wildercord$armor(), "Reload creates a fresh equipment service");
				for (EquipmentSlot slot : ARMOR_ORDER)
					check(afterReload.wildercord$armor().model(slot) != beforeReload.wildercord$armor().model(slot), "Reload rebuilds " + slot + " armor geometry");
				check(afterReload.wildercord$armor().viewModel() != beforeReload.wildercord$armor().viewModel(), "Reload rebuilds arm-only armor geometry");
				syntheticMatrix(mc);
			});

			for (boolean full : new boolean[] {true, false}) for (HumanoidArm arm : HumanoidArm.values())
				for (CameraType view : new CameraType[] {CameraType.THIRD_PERSON_FRONT, CameraType.THIRD_PERSON_BACK, CameraType.FIRST_PERSON}) {
					context.waitTicks(105);
					world.getServer().runOnServer(server -> prepare(server.getPlayerList().getPlayers().getFirst(), full));
					context.runOnClient(mc -> {
						mc.options.mainHand().set(arm);
						mc.options.broadcastOptions();
						mc.options.setCameraType(view);
						mc.gui.setScreen(null);
					});
					context.waitTicks(4);
					context.getInput().pressKey(MastersArtsClient.mapping(0));
					context.waitFor(mc -> MastersArtsClient.timeline(mc.player) != null, 30);
					boolean observed = false;
					int captures = 0;
					for (int tick = 0; tick < 9; tick++) {
						context.waitTicks(1);
						boolean owns = context.computeOnClient(mc -> {
							AvatarRenderState state = renderer(mc).createRenderState(mc.player, .5F);
							check(state.mainArm == arm, "Accepted live clip uses the selected main hand");
							check(state.chestEquipment.hasFoil(), "Live screenshot fixture wears genuinely enchanted armor");
							boolean eligible = ArticulatedCombat.frame(state) != null && Boolean.TRUE.equals(state.getData(ArticulatedArmorRenderer.READY));
							if (eligible && view == CameraType.FIRST_PERSON) integratedFirstPerson(state);
							return eligible;
						});
						observed |= owns;
						if (owns && (tick == 2 || tick == 5)) {
							context.takeScreenshot(TestScreenshotOptions.of("articulated_armor_live_" + (full ? "full" : "partial")
								+ "_" + arm.name().toLowerCase(Locale.ROOT) + "_" + (view == CameraType.FIRST_PERSON ? "first" : view == CameraType.THIRD_PERSON_FRONT ? "third_front" : "third_back")
								+ "_frame_" + tick).disableCounterPrefix());
							captures++;
						}
					}
					check(observed && captures > 0, "Armored screenshots include an actual server-accepted Spellcut");
					context.waitTicks(25);
					check(context.computeOnClient(mc -> MastersArtsClient.timeline(mc.player) == null), "Armored rendering preserves the accepted clip's expiry");
				}
			world.getServer().runOnServer(server -> MastersArts.cancel(server.getPlayerList().getPlayers().getFirst()));
		} finally {
			HitStop.clear();
			for (int i = 0; i < properties.length; i++) {
				if (previous[i] == null) System.clearProperty(properties[i]); else System.setProperty(properties[i], previous[i]);
			}
			context.runOnClient(mc -> {
				mc.options.setCameraType(camera);
				mc.options.mainHand().set(hand);
				mc.options.broadcastOptions();
				MastersArtsClient.mapping(0).setDown(false);
			});
		}
	}

	private static void syntheticMatrix(Minecraft mc) {
		var renderer = renderer(mc);
		var owner = access(renderer.getModel());
		check(owner.wildercord$bodyOwned() && owner.wildercord$armor() != null, "Actual AvatarRenderer primary owns the body and armor service");
		check(ArticulatedArmorInputChecks.verify(mc.getEntityModels(), owner.wildercord$armor().equipmentAssets()
			.get(net.minecraft.world.item.equipment.EquipmentAssets.NETHERITE)) > 0, "Slot inventory and resolved-asset negative fixtures pass");
		resolvedAssetReadiness(mc, renderer, owner);
		var layers = ((ArticulatedRendererAccess) renderer).wildercord$layers();
		check(layers.stream().filter(layer -> layer instanceof ArticulatedArmorLayer).count() == 1, "Exactly one articulated armor adapter is installed");
		check(layers.stream().noneMatch(layer -> layer instanceof HumanoidArmorLayer<?, ?, ?>), "Adapter replaces, rather than appends beside, the vanilla armor layer");
		check(layers.getFirst() instanceof ArticulatedArmorLayer && layers.get(1) instanceof net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer,
			"The adapter retains vanilla's armor-before-held-item layer position");
		for (boolean slim : new boolean[] {false, true}) for (boolean left : new boolean[] {false, true}) {
			AvatarRenderState state = renderer.createRenderState(mc.player, .5F);
			state.setData(ArticulatedCombat.KNOWN_LAYERS, true);
			state.setData(ArticulatedArmorRenderer.READY, true);
			state.setData(dev.wildercord.client.render.AuraShellLayer.SHELL_GLOW, null);
			state.walkAnimationSpeed = 0;
			state.showCape = false;
			state.mainArm = left ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
			state.rightHandItemStack = left ? ItemStack.EMPTY : new ItemStack(Items.DIAMOND_SWORD);
			state.leftHandItemStack = left ? new ItemStack(Items.DIAMOND_SWORD) : ItemStack.EMPTY;
			state.outlineColor = OUTLINE;
			state.setData(ArticulatedCombat.FRAME, frame(4, left));
			setArmor(state, mc.level.registryAccess(), true);
			PlayerModel body = new PlayerModel(mc.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
			ArticulatedModelAccess bodyAccess = access(body);
			check(!bodyAccess.wildercord$bodyOwned() && bodyAccess.wildercord$rig() == null && bodyAccess.wildercord$armor() == null,
				"Constructing a PlayerModel alone grants no body ownership");
			bodyAccess.wildercord$ownBody(slim);
			var armor = new ArticulatedArmorRenderer(mc.getEntityModels(), slim, owner.wildercord$armor().equipmentRenderer(), owner.wildercord$armor().equipmentAssets());
			bodyAccess.wildercord$setArmor(armor);
			var vanilla = ArmorModelSet.bake(slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR,
				mc.getEntityModels(), root -> new PlayerModel(root, slim));
			for (EquipmentSlot slot : ARMOR_ORDER) {
				PlayerModel shell = vanilla.get(slot);
				var shellAccess = access(shell);
				check(!shellAccess.wildercord$bodyOwned() && shellAccess.wildercord$rig() == null && shellAccess.wildercord$viewModel() == null,
					"Vanilla armor PlayerModels remain unowned: " + slot);
				check(!shell.root().hasChild(ArticulatedRig.CHILD), "No articulated skin subtree contaminates armor geometry: " + slot);
				shell.setupAnim(state);
				check(!shell.root().hasChild(ArticulatedRig.CHILD), "Armor setupAnim never attaches a body rig");
			}
			var vanillaLayer = new HumanoidArmorLayer<AvatarRenderState, PlayerModel, PlayerModel>(() -> body, vanilla, armor.equipmentRenderer());
			for (boolean full : new boolean[] {true, false}) {
				setArmor(state, mc.level.registryAccess(), full);
				check(ArticulatedArmorRenderer.supports(state) && ArticulatedCombat.frame(state) != null, "Full and partial P4 netherite are supported in both hands and skin widths");
				body.setupAnim(state);
				check(bodyAccess.wildercord$rig().root.visible && !body.body.visible && !body.leftArm.visible && !body.rightLeg.visible,
					"Armored accepted pose selects exactly one complete body backend");
				ReceiptCollector actual = new ReceiptCollector();
				armor.submitWorld(body, state, new PoseStack(), actual.collector(), LIGHT);
				ReceiptCollector expected = new ReceiptCollector();
				vanillaLayer.submit(new PoseStack(), expected.collector(), LIGHT, state, 0, 0);
				materialParity(actual, expected, full ? 4 : 2, false);
				int pass = 0;
				for (EquipmentSlot slot : ARMOR_ORDER) if (!equipment(state, slot).isEmpty()) {
					for (int material = 0; material < 3; material++)
						check(actual.models.get(pass++).model == armor.model(slot), "Each equipped slot submits only its baked armor mask: " + slot);
				}
				check(actual.models.stream().allMatch(receipt -> receipt.palette instanceof ArticulatedArmorGeometry.Palette),
					"Every deferred armor node stores an immutable final palette");
				deferredReplay(body, state, armor, actual);
				firstPerson(bodyAccess, state, armor);
			}
			untrimmedMaterial(mc, body, state, armor, vanillaLayer);
			materialMatrix(mc, body, state, armor, vanilla);
			hatVisibility(body, state, armor, vanilla);
			setArmor(state, mc.level.registryAccess(), true);
			finalPalette(body, state, armor);
			fallbacks(mc, body, state, armor);
			hitStop(mc, body, state, armor);
			state.setData(ArticulatedCombat.FRAME, null);
			body.setupAnim(state);
			check(!bodyAccess.wildercord$rig().root.visible && body.body.visible, "Cancellation restores rigid body ownership");
		}
	}

	private static void resolvedAssetReadiness(Minecraft mc, AvatarRenderer<?> renderer, ArticulatedModelAccess owner) {
		var original = owner.wildercord$armor();
		var key = net.minecraft.world.item.equipment.EquipmentAssets.NETHERITE;
		var stock = original.equipmentAssets().get(key);
		var layers = new java.util.EnumMap<net.minecraft.client.resources.model.EquipmentClientInfo.LayerType,
			java.util.List<net.minecraft.client.resources.model.EquipmentClientInfo.Layer>>(stock.layers());
		layers.put(net.minecraft.client.resources.model.EquipmentClientInfo.LayerType.WINGS,
			stock.getLayers(net.minecraft.client.resources.model.EquipmentClientInfo.LayerType.HUMANOID));
		var winged = new net.minecraft.client.resources.model.EquipmentClientInfo(layers, stock.trimOverrides());
		var syntheticAssets = new net.minecraft.client.resources.model.EquipmentAssetManager() {
			@Override public net.minecraft.client.resources.model.EquipmentClientInfo get(
					net.minecraft.resources.ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> asset) {
				return asset.equals(key) ? winged : original.equipmentAssets().get(asset);
			}
		};
		var unsupported = new ArticulatedArmorRenderer(mc.getEntityModels(), owner.wildercord$rig().slim(),
			original.equipmentRenderer(), syntheticAssets);
		owner.wildercord$setArmor(unsupported);
		try {
			check(!unsupported.supportsAssets(), "Unchanged netherite item IDs cannot admit a resolved wing-bearing asset");
			AvatarRenderState extracted = renderer(mc).createRenderState(mc.player, .5F);
			check(!Boolean.TRUE.equals(extracted.getData(ArticulatedArmorRenderer.READY)), "Actual render extraction disables armor readiness for resolved wings");
			extracted.setData(ArticulatedCombat.FRAME, frame(4, extracted.mainArm == HumanoidArm.LEFT));
			check(ArticulatedCombat.frame(extracted) == null && ArticulatedCombat.viewFrame(extracted) == null,
				"Resolved wings preserve the complete body and first-person fallback");
			ReceiptCollector rejected = new ReceiptCollector();
			check(!unsupported.submitWorld(renderer.getModel(), extracted, new PoseStack(), rejected.collector(), LIGHT)
				&& rejected.models.isEmpty(), "Unsupported resolved asset cannot partially submit armor");
		} finally { owner.wildercord$setArmor(original); }
	}

	private static void materialParity(ReceiptCollector actual, ReceiptCollector expected, int pieces, boolean view) {
		check(actual.models.size() == pieces * 3, "P4 trimmed armor emits material, trim, and glint for every equipped piece");
		check(actual.models.size() == expected.models.size(), "Articulated and vanilla equipment submit equal pass counts");
		for (int i = 0; i < actual.models.size(); i++) {
			Receipt a = actual.models.get(i), b = expected.models.get(i);
			check(a.order == b.order && a.type == b.type && a.light == b.light && a.overlay == b.overlay && a.color == b.color
				&& a.outline == b.outline && a.uv == b.uv, "Vanilla material, trim texture mapping, glint, ordering, lighting and outline are preserved at pass " + i);
			check(a.order == i % 3 + 1, "Per-piece passes preserve vanilla's material/trim/glint order");
			if (i % 3 == 0) check(a.uv == null && a.outline == (view ? 0 : OUTLINE), "Base material retains the correct outline scope");
			if (i % 3 == 1) check(a.uv != null, "Trim is the actual palette-backed vanilla mapping");
			if (i % 3 == 2) check(a.type == RenderTypes.trimmedArmorGlint() && a.outline == 0, "Vanilla trimmed glint stays last and has no outline");
		}
	}

	private static void deferredReplay(PlayerModel body, AvatarRenderState state, ArticulatedArmorRenderer armor, ReceiptCollector a) {
		var oldFrame = state.getData(ArticulatedCombat.FRAME);
		List<List<Integer>> first = a.models.stream().map(ArticulatedArmorPresentationTest::geometry).toList();
		state.setData(ArticulatedCombat.FRAME, frame(10, state.mainArm == HumanoidArm.LEFT));
		body.setupAnim(state);
		ReceiptCollector b = new ReceiptCollector();
		check(armor.submitWorld(body, state, new PoseStack(), b.collector(), LIGHT), "A second pose submits through the same renderer-owned models");
		List<List<Integer>> second = b.models.stream().map(ArticulatedArmorPresentationTest::geometry).toList();
		check(!first.equals(second), "Different accepted poses really deform submitted armor vertices");
		// Deliberately leave the shared rig and model at B, then execute A's deferred nodes again.
		check(first.equals(a.models.stream().map(ArticulatedArmorPresentationTest::geometry).toList()), "Deferred A/B/A rendering replays A exactly after the live rig/model moved to B");
		var palette = (ArticulatedArmorGeometry.Palette) a.models.getFirst().palette;
		Matrix4f copy = palette.matrix(Joint.CHEST);
		copy.zero();
		check(!palette.matrix(Joint.CHEST).equals(copy), "Palette access returns a defensive matrix copy");
		check(first.equals(a.models.stream().map(ArticulatedArmorPresentationTest::geometry).toList()), "External matrix mutation cannot corrupt pending armor nodes");
		state.setData(ArticulatedCombat.FRAME, oldFrame);
		body.setupAnim(state);
	}

	private static void firstPerson(ArticulatedModelAccess body, AvatarRenderState state, ArticulatedArmorRenderer armor) {
		ArticulatedViewModel skin = body.wildercord$viewModel();
		var combat = state.getData(ArticulatedCombat.FRAME);
		skin.setupAnim(new ArticulatedViewModel.Frame(ArticulatedCombatPose.view(combat.pose(), combat.leftHanded()), true, true));
		ReceiptCollector actual = new ReceiptCollector();
		check(armor.submitView(skin.rig(), state, new PoseStack(), actual.collector(), LIGHT), "Enhanced first person submits the equipped chestplate arms");
		ReceiptCollector expected = new ReceiptCollector();
		armor.equipmentRenderer().renderLayers(net.minecraft.client.resources.model.EquipmentClientInfo.LayerType.HUMANOID,
			state.chestEquipment.get(DataComponents.EQUIPPABLE).assetId().orElseThrow(), armor.viewModel(),
			ArticulatedArmorRenderer.capture(skin.rig()), state.chestEquipment.copy(), new PoseStack(), expected.collector(), LIGHT, 0);
		materialParity(actual, expected, 1, true);
		check(actual.models.stream().allMatch(r -> r.model == armor.viewModel() && r.outline == 0), "Every camera-space armor pass is arm-only and suppresses entity outlines");
		check(armor.viewModel().armsOnly(), "First-person geometry has an explicit arm-only mask");
		var palette = ArticulatedArmorRenderer.capture(skin.rig());
		var alienBody = ArticulatedArmorGeometry.Palette.from(joint -> {
			Matrix4f matrix = palette.matrix(joint);
			return armJoint(joint) ? matrix : matrix.translate(1000, -1000, 1000);
		});
		List<Integer> arms = geometry(armor.viewModel(), palette);
		check(!arms.isEmpty() && arms.equals(geometry(armor.viewModel(), alienBody)), "First-person mesh contains no head, torso, pelvis, or leg surfaces");
		check(!geometry(armor.model(EquipmentSlot.CHEST), palette).equals(geometry(armor.model(EquipmentSlot.CHEST), alienBody)),
			"The arm-only probe would detect an accidentally submitted full chestplate");
		System.setProperty(ArticulatedArmorRenderer.VIEW_PROPERTY, "false");
		try {
			ReceiptCollector off = new ReceiptCollector();
			check(!armor.submitView(skin.rig(), state, new PoseStack(), off.collector(), LIGHT) && off.models.isEmpty(), "Independent arm-overlay switch disables all camera-space armor passes");
			check(ArticulatedCombat.frame(state) != null, "Disabling the optional first-person overlay leaves the world armor feature available");
		} finally { System.setProperty(ArticulatedArmorRenderer.VIEW_PROPERTY, "true"); }
		ItemStack chest = state.chestEquipment;
		state.chestEquipment = ItemStack.EMPTY;
		ReceiptCollector absent = new ReceiptCollector();
		check(!armor.submitView(skin.rig(), state, new PoseStack(), absent.collector(), LIGHT) && absent.models.isEmpty(), "No chestplate means no invented first-person gauntlets");
		state.chestEquipment = chest;
	}


	/** Runs on a genuinely accepted live render state; no FRAME is injected by this probe. */
	private static void integratedFirstPerson(AvatarRenderState state) {
		FirstPersonHandsAndItemsRenderState hands = new FirstPersonHandsAndItemsRenderState();
		hands.mainHandItem = state.getMainHandItemStack().copy();
		hands.oldMainHandHeight = hands.mainHandHeight = 1;
		ReceiptCollector submitted = new ReceiptCollector();
		check(ArticulatedViewModel.submit(state, hands, .5F, InteractionHand.MAIN_HAND, new PoseStack(), submitted.collector(), LIGHT),
			"The real accepted first-person path owns both arms and the held sword");
		check(submitted.models.size() >= 4 && submitted.models.getFirst().model instanceof ArticulatedViewModel,
			"Camera-space skin submits before any armor overlay");
		for (int pass = 1; pass <= 3; pass++) {
			Receipt receipt = submitted.models.get(pass);
			check(receipt.model instanceof ArticulatedArmorGeometry geometry && geometry.armsOnly() && receipt.outline == 0,
				"Native first-person integration submits all three arm-only armor passes after skin");
		}
		int sword = submitted.calls.indexOf("submitItem");
		check(sword >= 4, "Held-item submission follows skin, armor material, trim, and glint");
		int beforeOffhand = submitted.calls.size();
		check(ArticulatedViewModel.submit(state, hands, .5F, InteractionHand.OFF_HAND, new PoseStack(), submitted.collector(), LIGHT)
			&& submitted.calls.size() == beforeOffhand, "The offhand callback cannot double-submit the enhanced arms");
	}

	private static void untrimmedMaterial(Minecraft mc, PlayerModel body, AvatarRenderState state, ArticulatedArmorRenderer armor,
			HumanoidArmorLayer<AvatarRenderState, PlayerModel, PlayerModel> vanilla) {
		setArmor(state, mc.level.registryAccess(), false);
		state.chestEquipment.remove(DataComponents.TRIM);
		state.legsEquipment = ItemStack.EMPTY;
		ReceiptCollector actual = new ReceiptCollector(), expected = new ReceiptCollector();
		check(armor.submitWorld(body, state, new PoseStack(), actual.collector(), LIGHT), "Protection IV does not require a trim to remain supported");
		vanilla.submit(new PoseStack(), expected.collector(), LIGHT, state, 0, 0);
		check(actual.models.size() == 1 && expected.models.size() == 1, "Untrimmed enchanted armor retains vanilla's combined base/glint material pass");
		Receipt a = actual.models.getFirst(), b = expected.models.getFirst();
		check(a.type == b.type && a.order == b.order && a.uv == null && a.outline == OUTLINE, "Untrimmed Protection IV uses the exact vanilla material pipeline");
	}


	/** Vanilla material semantics are tested directly even for assets outside production eligibility. */
	private static void materialMatrix(Minecraft mc, PlayerModel body, AvatarRenderState state, ArticulatedArmorRenderer armor,
			ArmorModelSet<PlayerModel> vanilla) {
		var registries = mc.level.registryAccess();
		ItemStack unenchanted = armor(Items.NETHERITE_CHESTPLATE, registries);
		unenchanted.remove(DataComponents.ENCHANTMENTS);
		ItemStack netheriteTrim = armor(Items.NETHERITE_CHESTPLATE, registries);
		netheriteTrim.set(DataComponents.TRIM, new ArmorTrim(registries.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.NETHERITE),
			registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.SENTRY)));
		ItemStack undyed = new ItemStack(Items.LEATHER_CHESTPLATE);
		ItemStack dyed = undyed.copy();
		dyed.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(0x1976D2));
		ItemStack decal = armor(Items.NETHERITE_CHESTPLATE, registries);
		var ordinaryTrim = decal.get(DataComponents.TRIM);
		var pattern = ordinaryTrim.pattern().value();
		decal.set(DataComponents.TRIM, new ArmorTrim(ordinaryTrim.material(), net.minecraft.core.Holder.direct(
			new net.minecraft.world.item.equipment.trim.TrimPattern(pattern.assetId(), pattern.description(), true))));
		String[] labels = {"Unenchanted trim", "Netherite-on-netherite override", "Undyed leather", "Dyed leather", "Synthetic decal trim"};
		ItemStack[] items = {unenchanted, netheriteTrim, undyed, dyed, decal};
		int[] passes = {2, 3, 2, 2, 3};
		body.setupAnim(state);
		var palette = ArticulatedArmorRenderer.capture(access(body).wildercord$rig());
		for (int sample = 0; sample < items.length; sample++) {
			ItemStack item = items[sample];
			ReceiptCollector actual = new ReceiptCollector(), expected = new ReceiptCollector();
			var asset = item.get(DataComponents.EQUIPPABLE).assetId().orElseThrow();
			armor.equipmentRenderer().renderLayers(net.minecraft.client.resources.model.EquipmentClientInfo.LayerType.HUMANOID,
				asset, armor.model(EquipmentSlot.CHEST), palette, item, new PoseStack(), actual.collector(), LIGHT, OUTLINE);
			armor.equipmentRenderer().renderLayers(net.minecraft.client.resources.model.EquipmentClientInfo.LayerType.HUMANOID,
				asset, vanilla.chest(), state, item, new PoseStack(), expected.collector(), LIGHT, OUTLINE);
			check(actual.models.size() == passes[sample] && actual.models.size() == expected.models.size(), labels[sample] + " preserves vanilla pass count");
			for (int pass = 0; pass < actual.models.size(); pass++) {
				Receipt a = actual.models.get(pass), b = expected.models.get(pass);
				check(a.order == b.order && a.type == b.type && a.uv == b.uv && a.color == b.color && a.light == b.light
					&& a.overlay == b.overlay && a.outline == b.outline, labels[sample] + " preserves every material receipt field at pass " + pass);
			}
			if (sample == 0) check(actual.models.stream().noneMatch(r -> r.type == RenderTypes.trimmedArmorGlint()), "Unenchanted trim never invents glint");
			if (sample == 1) check(actual.models.get(1).uv != null, "Same-material trim uses the real vanilla override mapping");
			if (sample == 2 || sample == 3) {
				check(actual.models.getFirst().color != -1 && actual.models.get(1).color == -1, labels[sample] + " separates the tinted base and untinted overlay");
				check(!ArticulatedArmorRenderer.supported(item, EquipmentSlot.CHEST), "Direct leather material probe does not expand production eligibility");
			}
			if (sample == 4) {
				var normal = new ReceiptCollector();
				ItemStack noDecal = decal.copy();
				noDecal.set(DataComponents.TRIM, ordinaryTrim);
				armor.equipmentRenderer().renderLayers(net.minecraft.client.resources.model.EquipmentClientInfo.LayerType.HUMANOID,
					asset, armor.model(EquipmentSlot.CHEST), palette, noDecal, new PoseStack(), normal.collector(), LIGHT, OUTLINE);
				check(actual.models.get(1).type != normal.models.get(1).type, "Decal trim selects its distinct vanilla depth pipeline");
			}
		}
	}

	private static void hatVisibility(PlayerModel body, AvatarRenderState state, ArticulatedArmorRenderer armor,
			ArmorModelSet<PlayerModel> vanilla) {
		boolean showHat = state.showHat;
		state.headEquipment = new ItemStack(Items.NETHERITE_HELMET);
		for (boolean visible : new boolean[] {false, true, false}) {
			state.showHat = visible;
			body.setupAnim(state);
			ReceiptCollector submitted = new ReceiptCollector();
			check(armor.submitWorld(body, state, new PoseStack(), submitted.collector(), LIGHT), "Helmet visibility probe owns its frame");
			Receipt helmet = submitted.models.stream().filter(receipt -> receipt.model == armor.model(EquipmentSlot.HEAD)).findFirst().orElseThrow();
			var actual = geometry(helmet);
			var expected = geometry(vanilla.head(), state);
			check(actual.size() == expected.size(), "Helmet outer hat geometry follows vanilla skin-hat visibility through deferred passes");
		}
		state.showHat = showHat;
	}

	private static void finalPalette(PlayerModel body, AvatarRenderState state, ArticulatedArmorRenderer armor) {
		var previous = state.getData(ArticulatedCombat.FRAME);
		float xRot = state.xRot, yRot = state.yRot;
		state.xRot = 85;
		state.yRot = 40;
		var edge = frame(.8F, state.mainArm == HumanoidArm.LEFT);
		state.setData(ArticulatedCombat.FRAME, edge);
		check(edge.pose().weight() > 0 && edge.pose().weight() < 1, "Final-palette probe uses a genuine fractional backend blend");
		body.setupAnim(state);
		ReceiptCollector submitted = new ReceiptCollector();
		check(armor.submitWorld(body, state, new PoseStack(), submitted.collector(), LIGHT), "Fractional-weight frame submits armor");
		var finalPalette = (ArticulatedArmorGeometry.Palette) submitted.models.getFirst().palette;
		check(submitted.models.stream().anyMatch(r -> r.model == armor.model(EquipmentSlot.HEAD))
			&& submitted.models.stream().allMatch(r -> r.palette == finalPalette), "All equipped slots share one final immutable palette, including the helmet");
		for (Joint joint : Joint.values()) {
			PoseStack nativeTransform = new PoseStack();
			access(body).wildercord$rig().transformTo(joint, nativeTransform);
			Matrix4f expected = new Matrix4f(nativeTransform.last().pose());
			expected.m30(expected.m30() * 16).m31(expected.m31() * 16).m32(expected.m32() * 16);
			check(finalPalette.matrix(joint).equals(expected, .00001F), "Submitted armor palette includes the final native transform for " + joint);
		}
		Matrix4f rawHead = new Matrix4f().set(edge.pose().world(Joint.HEAD).values());
		Matrix4f rawArm = new Matrix4f().set(edge.pose().world(Joint.RIGHT_UPPER_ARM).values());
		check(!finalPalette.matrix(Joint.HEAD).equals(rawHead, .00001F), "Armor head follows resolved free look rather than the raw attack clip");
		check(!finalPalette.matrix(Joint.RIGHT_UPPER_ARM).equals(rawArm, .00001F), "Armor arms retain the fractional vanilla held-item/breathing baseline");
		var mutable = new java.util.EnumMap<Joint, Matrix4f>(Joint.class);
		for (Joint joint : Joint.values()) mutable.put(joint, finalPalette.matrix(joint));
		var immutable = ArticulatedArmorGeometry.Palette.from(mutable::get);
		List<Integer> before = geometry(armor.model(EquipmentSlot.CHEST), immutable);
		mutable.values().forEach(Matrix4f::zero);
		check(before.equals(geometry(armor.model(EquipmentSlot.CHEST), immutable)), "Palette construction snapshots caller-owned matrices defensively");
		state.xRot = xRot;
		state.yRot = yRot;
		state.setData(ArticulatedCombat.FRAME, previous);
		body.setupAnim(state);
	}

	private static boolean armJoint(Joint joint) {
		return switch (joint) {
			case LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_UPPER_ARM, RIGHT_UPPER_ARM, LEFT_FOREARM, RIGHT_FOREARM,
				LEFT_HAND, RIGHT_HAND, LEFT_SOCKET, RIGHT_SOCKET -> true;
			default -> false;
		};
	}

	private static void fallbacks(Minecraft mc, PlayerModel body, AvatarRenderState state, ArticulatedArmorRenderer armor) {
		setArmor(state, mc.level.registryAccess(), true);
		for (EquipmentSlot slot : ARMOR_ORDER) {
			ItemStack saved = equipment(state, slot);
			ItemStack unsupported = new ItemStack(switch (slot) {
				case HEAD -> Items.CARVED_PUMPKIN;
				case CHEST -> Items.ELYTRA;
				case LEGS -> Items.DIAMOND_LEGGINGS;
				default -> Items.DIAMOND_BOOTS;
			});
			setEquipment(state, slot, unsupported);
			check(!ArticulatedArmorRenderer.supports(state), "A mixed unsupported " + slot + " rejects the complete armor backend");
			assertFallback(body, state, armor, "Mixed " + slot);
			check(equipment(state, slot) == unsupported, "Fallback never strips an unsupported equipped item");
			setEquipment(state, slot, saved);
		}
		state.isInvisible = true;
		assertFallback(body, state, armor, "Invisibility preserves vanilla equipment semantics");
		state.isInvisible = false;
		state.isSpectator = true;
		check(ArticulatedCombat.frame(state) == null && ArticulatedCombat.viewFrame(state) == null, "Spectators retain vanilla ownership");
		state.isSpectator = false;
		state.isCrouching = true;
		assertFallback(body, state, armor, "Unsupported crouching pose");
		state.isCrouching = false;
		// Eligibility-only synthetic cape metadata; this texture is never submitted as a cape.
		var skin = state.skin;
		state.skin = new net.minecraft.world.entity.player.PlayerSkin(skin.body(), skin.body(), skin.elytra(), skin.model(), skin.secure());
		state.showCape = true;
		assertFallback(body, state, armor, "Visible cape keeps the complete renderer");
		state.showCape = false;
		state.skin = skin;
		ItemStack changed = state.chestEquipment.copy();
		changed.set(DataComponents.EQUIPPABLE, new ItemStack(Items.DIAMOND_CHESTPLATE).get(DataComponents.EQUIPPABLE));
		ItemStack saved = state.chestEquipment;
		state.chestEquipment = changed;
		assertFallback(body, state, armor, "Custom equipment asset on a netherite item");
		state.chestEquipment = saved;
		state.setData(ArticulatedArmorRenderer.READY, false);
		assertFallback(body, state, armor, "Missing/ambiguous installed armor adapter");
		state.setData(ArticulatedArmorRenderer.READY, true);
		state.setData(ArticulatedCombat.KNOWN_LAYERS, false);
		assertFallback(body, state, armor, "Unknown feature layer");
		state.setData(ArticulatedCombat.KNOWN_LAYERS, true);
		System.setProperty(ArticulatedArmorRenderer.ENABLE_PROPERTY, "false");
		try { assertFallback(body, state, armor, "Armor preview switch off"); }
		finally { System.setProperty(ArticulatedArmorRenderer.ENABLE_PROPERTY, "true"); }
		body.setupAnim(state);
		check(access(body).wildercord$rig().root.visible, "Model reuse recovers articulated ownership after a fallback frame");

		// Exercise the actual installed adapter, rather than merely testing its eligibility helper.
		var actualRenderer = renderer(mc);
		var adapter = ((ArticulatedRendererAccess) actualRenderer).wildercord$layers().stream()
			.filter(layer -> layer instanceof ArticulatedArmorLayer).map(layer -> (ArticulatedArmorLayer) layer).findFirst().orElseThrow();
		state.feetEquipment = new ItemStack(Items.DIAMOND_BOOTS);
		ReceiptCollector throughAdapter = new ReceiptCollector();
		ReceiptCollector throughOriginal = new ReceiptCollector();
		adapter.submit(new PoseStack(), throughAdapter.collector(), LIGHT, state, 0, 0);
		adapter.fallback().submit(new PoseStack(), throughOriginal.collector(), LIGHT, state, 0, 0);
		check(!throughAdapter.models.isEmpty() && throughAdapter.models.size() == throughOriginal.models.size(), "Unsupported mixed equipment still submits every original armor pass");
		for (int i = 0; i < throughAdapter.models.size(); i++) {
			Receipt a = throughAdapter.models.get(i), b = throughOriginal.models.get(i);
			check(a.model == b.model && a.type == b.type && a.order == b.order && a.uv == b.uv,
				"Adapter fallback is the retained original layer and models, pass " + i);
			check(!(a.model instanceof ArticulatedArmorGeometry), "No articulated armor leaks into an all-or-nothing fallback");
		}
		setArmor(state, mc.level.registryAccess(), true);
	}

	private static void assertFallback(PlayerModel body, AvatarRenderState state, ArticulatedArmorRenderer armor, String reason) {
		check(ArticulatedCombat.frame(state) == null && ArticulatedCombat.viewFrame(state) == null, reason + " restores both presentation fallbacks");
		body.setupAnim(state);
		check(!access(body).wildercord$rig().root.visible && body.body.visible && body.rightArm.visible && body.leftLeg.visible,
			reason + " leaves the complete rigid body visible");
		ReceiptCollector rejected = new ReceiptCollector();
		check(!armor.submitWorld(body, state, new PoseStack(), rejected.collector(), LIGHT) && rejected.models.isEmpty(), reason + " emits no partial articulated armor");
	}

	private static void hitStop(Minecraft mc, PlayerModel body, AvatarRenderState state, ArticulatedArmorRenderer armor) {
		setArmor(state, mc.level.registryAccess(), true);
		boolean left = state.mainArm == HumanoidArm.LEFT;
		var held = frame(2, left);
		HitStop.clear();
		HitStop.hold(10_000, mc.player.getId());
		state.setData(ArticulatedCombat.FRAME, held);
		HitStop.extracted(mc.player, state);
		state.setData(ArticulatedCombat.FRAME, frame(4, left));
		state.headEquipment = ItemStack.EMPTY; // The next extracted state after a helmet breaks.
		HitStop.extracted(mc.player, state);
		check(state.getData(ArticulatedCombat.FRAME) == held && state.headEquipment.isEmpty(), "Hit-stop freezes pose but never revives broken equipment");
		ReceiptCollector broken = new ReceiptCollector();
		check(armor.submitWorld(body, state, new PoseStack(), broken.collector(), LIGHT), "Supported armor remaining after a break keeps rendering");
		check(broken.models.size() == 9 && broken.models.stream().noneMatch(r -> r.model == armor.model(EquipmentSlot.HEAD)), "Broken helmet has zero deferred submissions during hold");
		state.setData(ArticulatedCombat.FRAME, frame(5, left));
		ItemStack replacement = new ItemStack(Items.DIAMOND_CHESTPLATE);
		state.chestEquipment = replacement;
		HitStop.extracted(mc.player, state);
		check(state.chestEquipment == replacement && state.getData(ArticulatedCombat.FRAME) == held, "Equipment swaps stay live while the cosmetic pose is held");
		assertFallback(body, state, armor, "Unsupported equipment swap during hit-stop");
		state.setData(ArticulatedCombat.FRAME, null);
		HitStop.extracted(mc.player, state);
		check(state.getData(ArticulatedCombat.FRAME) == null, "A hold cannot revive a cancelled armored clip");
		HitStop.clear();
		setArmor(state, mc.level.registryAccess(), true);
	}

	/** Records the real equipment renderer's calls without asking the GPU to execute them. */
	private static final class ReceiptCollector {
		final List<Receipt> models = new ArrayList<>();
		final List<String> calls = new ArrayList<>();
		SubmitNodeCollector collector() { return ordered(0); }
		private SubmitNodeCollector ordered(int order) {
			return (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(), new Class<?>[] {SubmitNodeCollector.class},
				(proxy, method, args) -> {
					if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
						case "toString" -> "ArmorReceiptCollector[" + order + "]";
						case "hashCode" -> System.identityHashCode(proxy);
						case "equals" -> proxy == args[0];
						default -> throw new AssertionError(method);
					};
					if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
					if (method.getName().equals("order")) return ordered((int) args[0]);
					calls.add(method.getName());
					if (method.getName().equals("submitModel")) {
						check(args.length == 9, "Native equipment submission retains the expected complete model signature");
						models.add(new Receipt((Model<?>) args[0], args[1], new Matrix4f(((PoseStack) args[2]).last().pose()),
							(RenderType) args[3], (int) args[4], (int) args[5], (int) args[6], (UvMapping) args[7], (int) args[8], order));
					}
					return null;
				});
		}
	}

	private record Receipt(Model<?> model, Object palette, Matrix4f pose, RenderType type,
		int light, int overlay, int color, UvMapping uv, int outline, int order) {}

	private static List<Integer> geometry(Receipt receipt) { return geometry(receipt.model, receipt.palette, receipt.pose); }
	private static List<Integer> geometry(Model<?> model, Object palette) { return geometry(model, palette, new Matrix4f()); }
	@SuppressWarnings("unchecked")
	private static List<Integer> geometry(Model<?> model, Object palette, Matrix4f pose) {
		((Model<Object>) model).setupAnim(palette);
		PoseStack stack = new PoseStack();
		stack.last().pose().set(pose);
		VertexReceipt sink = new VertexReceipt();
		model.renderToBuffer(stack, sink, LIGHT, OverlayTexture.NO_OVERLAY, -1);
		check(sink.vertices > 0, "Deferred armor mesh renders real vertices");
		return List.copyOf(sink.values);
	}

	/** Values include positions, UVs and normals so pose replay also catches stale seam normals. */
	private static final class VertexReceipt implements VertexConsumer {
		final List<Integer> values = new ArrayList<>();
		int vertices;
		private VertexConsumer floats(float... floats) {
			for (float value : floats) {
				check(Float.isFinite(value), "Armor geometry contains no NaN/infinite positions, UVs or normals");
				values.add(Float.floatToIntBits(value));
			}
			return this;
		}
		@Override public VertexConsumer addVertex(float x, float y, float z) { vertices++; return floats(x, y, z); }
		@Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
		@Override public VertexConsumer setColor(int color) { return this; }
		@Override public VertexConsumer setUv(float u, float v) { return floats(u, v); }
		@Override public VertexConsumer setUv1(int u, int v) { return this; }
		@Override public VertexConsumer setUv2(int u, int v) { return this; }
		@Override public VertexConsumer setUv3(float u, float v) { return floats(u, v); }
		@Override public VertexConsumer setNormal(float x, float y, float z) { return floats(x, y, z); }
		@Override public VertexConsumer setLineWidth(float width) { return this; }
	}

	private static ArticulatedCombat.Frame frame(float age, boolean left) {
		return new ArticulatedCombat.Frame(ArticulatedCombatPose.sampleSpellcut(0, age, 4, 12, left), 0xA440, 0, false, left, 0, 0);
	}

	private static void setArmor(AvatarRenderState state, RegistryAccess registries, boolean full) {
		state.headEquipment = full ? armor(Items.NETHERITE_HELMET, registries) : ItemStack.EMPTY;
		state.chestEquipment = armor(Items.NETHERITE_CHESTPLATE, registries);
		state.legsEquipment = armor(Items.NETHERITE_LEGGINGS, registries);
		state.feetEquipment = full ? armor(Items.NETHERITE_BOOTS, registries) : ItemStack.EMPTY;
	}

	private static ItemStack armor(Item item, RegistryAccess registries) {
		ItemStack stack = new ItemStack(item);
		var protection = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
		stack.enchant(protection, 4);
		stack.set(DataComponents.TRIM, new ArmorTrim(
			registries.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.GOLD),
			registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.SENTRY)));
		check(stack.getEnchantments().getLevel(protection) == 4 && stack.hasFoil(), "Fixture armor has actual Protection IV and foil components");
		return stack;
	}

	private static ItemStack equipment(AvatarRenderState state, EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> state.headEquipment;
			case CHEST -> state.chestEquipment;
			case LEGS -> state.legsEquipment;
			case FEET -> state.feetEquipment;
			default -> throw new AssertionError("Not an armor slot: " + slot);
		};
	}
	private static void setEquipment(AvatarRenderState state, EquipmentSlot slot, ItemStack item) {
		switch (slot) {
			case HEAD -> state.headEquipment = item;
			case CHEST -> state.chestEquipment = item;
			case LEGS -> state.legsEquipment = item;
			case FEET -> state.feetEquipment = item;
			default -> throw new AssertionError("Not an armor slot: " + slot);
		}
	}

	private static void prepare(ServerPlayer player, boolean full) {
		player.setItemSlot(EquipmentSlot.HEAD, full ? armor(Items.NETHERITE_HELMET, player.level().registryAccess()) : ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.CHEST, armor(Items.NETHERITE_CHESTPLATE, player.level().registryAccess()));
		player.setItemSlot(EquipmentSlot.LEGS, armor(Items.NETHERITE_LEGGINGS, player.level().registryAccess()));
		player.setItemSlot(EquipmentSlot.FEET, full ? armor(Items.NETHERITE_BOOTS, player.level().registryAccess()) : ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("stone", 3, 1800, 100, 0));
		player.inventoryMenu.broadcastChanges();
	}

	@SuppressWarnings("unchecked")
	private static AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer> renderer(Minecraft mc) {
		return (AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer>) mc.getEntityRenderDispatcher().getRenderer(mc.player);
	}
	private static ArticulatedModelAccess access(PlayerModel model) {
		check(model instanceof ArticulatedModelAccess, "Native player model mixin is installed");
		return (ArticulatedModelAccess) model;
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
