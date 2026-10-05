package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.MastersArtRules;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.client.fx.HitStop;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/** Additive checks called by the existing articulated suites, never a separate CI selection. */
final class ArticulatedSharedPlayerChecks {
	private ArticulatedSharedPlayerChecks() {}
	private record Viewport(int width, int height, int gui) {}
	private static final Viewport[] VIEWPORTS = {
		new Viewport(854, 480, 2), new Viewport(1280, 720, 3),
		new Viewport(1280, 960, 4), new Viewport(1920, 810, 3)
	};
	private static final int[] MOVES = {ArticulatedCombatPose.RISING_BREAK, ArticulatedCombatPose.DRIVING_CUT};

	static void body(ClientGameTestContext context) { capture(context, false); }
	static void hud(ClientGameTestContext context) { capture(context, true); }

	/** Every phase uses its own fresh real key input; screenshot latency cannot skip the next phase. */
	private static void capture(ClientGameTestContext context, boolean hudMatrix) {
		String[] properties = {ArticulatedCombat.ENABLE_PROPERTY, ArticulatedCombat.STABLE_CAMERA_PROPERTY,
			ArticulatedArmorRenderer.ENABLE_PROPERTY, ArticulatedArmorRenderer.VIEW_PROPERTY};
		String[] previous = Arrays.stream(properties).map(System::getProperty).toArray(String[]::new);
		CameraType oldCamera = context.computeOnClient(mc -> mc.options.getCameraType());
		HumanoidArm oldHand = context.computeOnClient(mc -> mc.options.mainHand().get());
		Viewport original = context.computeOnClient(mc -> new Viewport(mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight(), mc.options.guiScale().get()));
		boolean hidden = context.computeOnClient(mc -> mc.gui.hud.isHidden());
		boolean fullscreen = context.computeOnClient(mc -> mc.options.fullscreen().get());
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
				prepare(player, false, false);
			});
			context.waitTicks(12);
			if (!hudMatrix) context.runOnClient(ArticulatedSharedPlayerChecks::syntheticBridge);
			for (Viewport viewport : hudMatrix ? VIEWPORTS : new Viewport[] {VIEWPORTS[1]})
				for (boolean armor : new boolean[] {false, true})
					for (HumanoidArm hand : HumanoidArm.values()) for (int move : MOVES)
						for (ArticulatedCombatPose.Phase phase : new ArticulatedCombatPose.Phase[] {
							ArticulatedCombatPose.Phase.WINDUP, ArticulatedCombatPose.Phase.ACTIVE, ArticulatedCombatPose.Phase.RECOVERY}) {
							context.waitTicks(105); // Existing 100/80 tick individual rests remain authoritative.
							world.getServer().runOnServer(server -> prepare(server.getPlayerList().getPlayers().getFirst(), armor, !hudMatrix));
							context.runOnClient(mc -> {
								mc.getWindow().setWindowed(viewport.width(), viewport.height());
								mc.options.guiScale().set(viewport.gui()); mc.resizeGui();
								mc.options.mainHand().set(hand); mc.options.broadcastOptions();
								mc.options.setCameraType(hudMatrix ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_FRONT);
								if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
								mc.gui.setScreen(null);
								mc.player.setYRot(0); mc.player.setXRot(0);
							});
							context.waitTicks(8);
							var rule = MastersArtRules.move(move);
							String name = "articulated_shared_" + rule.id() + "_" + (hudMatrix ? "hud_" : "third_")
								+ viewport.width() + "x" + viewport.height() + "_gui" + viewport.gui() + "_"
								+ (armor ? "netherite_" : "skin_") + hand.name().toLowerCase(Locale.ROOT) + "_requested_" + phase.name().toLowerCase(Locale.ROOT);
							context.getInput().pressKey(MastersArtsClient.mapping(move));
							context.waitFor(mc -> {
								var timeline = MastersArtsClient.timeline(mc.player);
								if (timeline == null || timeline.move() != move) return false;
								var frame = ArticulatedCombat.frame(state(mc));
								return frame != null && frame.pose().phase() == phase;
							}, 35);
							context.runOnClient(mc -> {
								var timeline = MastersArtsClient.timeline(mc.player);
								var state = state(mc); var frame = ArticulatedCombat.frame(state);
								check(timeline != null && timeline.move() == move && frame != null && frame.move() == move && !frame.master(), "Exact accepted player activation owns the capture");
								check(frame.pose().phase() == phase, "The requested phase still owns the pre-capture state");
								check(timeline.windup() == rule.windup() && timeline.recovery() == rule.recovery(), "Server windows remain unchanged");
								check(timeline.yaw() == 0 && timeline.pitch() == 0 && mc.player.getYRot() == 0 && mc.player.getXRot() == 0, "Accepted aim and free camera remain unchanged");
								check(state.mainArm == hand && ArticulatedCombat.viewFrame(state) != null && !mc.gui.hud.isHidden(), "Hands and HUD retain supported ownership");
								if (armor) {
									check(state.chestEquipment.is(Items.NETHERITE_CHESTPLATE) && state.chestEquipment.hasFoil(), "Native armor trial retains enchanted chestplate");
									if (!hudMatrix) check(state.headEquipment.is(Items.NETHERITE_HELMET) && state.legsEquipment.is(Items.NETHERITE_LEGGINGS)
										&& state.feetEquipment.is(Items.NETHERITE_BOOTS), "Third-person native trial wears all four supported armor slots");
								}
								check(mc.getWindow().getWidth() == viewport.width() && mc.getWindow().getHeight() == viewport.height(), "Actual viewport matches the capture name");
								System.out.println("ARTICULATED_SHARED_SAMPLE name=" + name + " acceptedMove=" + timeline.move()
									+ " activation=" + timeline.startTick() + " windup=" + timeline.windup() + " recovery=" + timeline.recovery()
									+ " actualSkin=" + state.skin.model() + " preCaptureAge=" + (mc.level.getGameTime() - timeline.startTick() + .5F)
									+ " preCapturePhase=" + frame.pose().phase() + " requestedPhase=" + phase + " renderedPhase=unknown nativePixelReviewRequired=true exactImpactPixelCoverage=unverified");
							});
							context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
							context.waitTicks(rule.windup() + rule.recovery() + 2);
							check(context.computeOnClient(mc -> MastersArtsClient.timeline(mc.player) == null && ArticulatedCombat.viewFrame(state(mc)) != null), "Accepted expiry returns to the stable view idle");
						}
			world.getServer().runOnServer(server -> MastersArts.cancel(server.getPlayerList().getPlayers().getFirst()));
		} finally {
			HitStop.clear();
			for (int i = 0; i < properties.length; i++) {
				if (previous[i] == null) System.clearProperty(properties[i]); else System.setProperty(properties[i], previous[i]);
			}
			context.runOnClient(mc -> {
				mc.options.setCameraType(oldCamera); mc.options.mainHand().set(oldHand); mc.options.broadcastOptions();
				mc.getWindow().setWindowed(original.width(), original.height()); mc.getWindow().setFullscreen(fullscreen);
				mc.options.guiScale().set(original.gui()); mc.resizeGui();
				if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
				for (int move : MOVES) MastersArtsClient.mapping(move).setDown(false);
			});
		}
	}

	/** Injected native model/hold regression assertions. These are not accepted-input captures. */
	private static void syntheticBridge(Minecraft mc) {
		for (boolean slim : new boolean[] {false, true}) for (boolean left : new boolean[] {false, true}) for (int move : MOVES) {
			var rule = MastersArtRules.move(move);
			PlayerModel model = new PlayerModel(mc.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
			((ArticulatedModelAccess) model).wildercord$ownBody(slim);
			ArticulatedRig rig = ((ArticulatedModelAccess) model).wildercord$rig();
			AvatarRenderState state = state(mc);
			state.setData(ArticulatedCombat.KNOWN_LAYERS, true);
			state.setData(dev.wildercord.client.render.AuraShellLayer.SHELL_GLOW, null);
			state.walkAnimationSpeed = 0; state.showCape = false;
			state.mainArm = left ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
			state.rightHandItemStack = left ? ItemStack.EMPTY : new ItemStack(Items.DIAMOND_SWORD);
			state.leftHandItemStack = left ? new ItemStack(Items.DIAMOND_SWORD) : ItemStack.EMPTY;
			var frame = new ArticulatedCombat.Frame(ArticulatedCombatPose.samplePlayer(move, rule.windup(), rule.windup(), rule.recovery(), left), 999, move, false, left, 0, 0);
			state.setData(ArticulatedCombat.FRAME, frame); model.setupAnim(state);
			check(rig.root.visible && !model.body.visible && ArticulatedCombat.viewFrame(state) == frame, "Shared player form owns body and view together");
			PoseStack actual = new PoseStack(); model.translateToHand(state, state.mainArm, actual);
			actual.rotateDegrees(com.mojang.math.Axis.XP, -90); actual.rotateDegrees(com.mojang.math.Axis.YP, 180);
			actual.translate((left ? -1 : 1) / 16F, 2F / 16, -10F / 16);
			PoseStack expected = new PoseStack(); rig.socket(state.mainArm, expected);
			check(actual.last().pose().transformPosition(new Vector3f(0, -1.327F / 16, 1.439F / 16))
				.distance(expected.last().pose().transformPosition(new Vector3f())) < .00001F, "Both skin widths keep the blade hilt at the articulated wrist");
			state.setData(ArticulatedCombat.FRAME, null); model.setupAnim(state);
			PoseStack baseline = new PoseStack(); model.translateToHand(state, state.mainArm, baseline);
			baseline.rotateDegrees(com.mojang.math.Axis.XP, -90); baseline.rotateDegrees(com.mojang.math.Axis.YP, 180);
			baseline.translate((left ? -1 : 1) / 16F, 2F / 16, -10F / 16);
			Vector3f baselineGrip = baseline.last().pose().transformPosition(new Vector3f(0, -1.327F / 16, 1.439F / 16));
			for (float edge : new float[] {.0001F, rule.windup() + rule.recovery() - .005F}) {
				var edgePose = ArticulatedCombatPose.samplePlayer(move, edge, rule.windup(), rule.recovery(), left);
				check(edgePose.weight() > 0, "New-art hand seam probe must actually own the segmented backend");
				state.setData(ArticulatedCombat.FRAME, new ArticulatedCombat.Frame(edgePose, 999, move, false, left, 0, 0));
				model.setupAnim(state);
				check(ArticulatedCombat.frame(state) != null && rig.root.visible, "Both new-art seams must use the actual segmented backend");
				PoseStack edgeSocket = new PoseStack(); rig.socket(state.mainArm, edgeSocket);
				check(edgeSocket.last().pose().transformPosition(new Vector3f()).distance(baselineGrip) < .001F,
					"New-art grip converges to the real held-arm baseline at both edges: move=" + move + " slim=" + slim + " left=" + left + " age=" + edge);
			}
			state.setData(ArticulatedCombat.FRAME, frame);
			state.isCrouching = true; model.setupAnim(state);
			check(!rig.root.visible && model.body.visible && ArticulatedCombat.viewFrame(state) == null, "Unsupported posture restores the whole backend");
			state.isCrouching = false; state.setData(ArticulatedCombat.KNOWN_LAYERS, false); model.setupAnim(state);
			check(!rig.root.visible && model.body.visible && ArticulatedCombat.viewFrame(state) == null, "Unknown layers restore the whole backend");
			state.setData(ArticulatedCombat.KNOWN_LAYERS, true);
			state.chestEquipment = new ItemStack(Items.DIAMOND_CHESTPLATE); model.setupAnim(state);
			check(!rig.root.visible && model.body.visible && ArticulatedCombat.viewFrame(state) == null, "Unsupported armor restores the whole backend");
			state.chestEquipment = ItemStack.EMPTY;
			if (left) state.rightHandItemStack = new ItemStack(Items.SHIELD); else state.leftHandItemStack = new ItemStack(Items.SHIELD);
			model.setupAnim(state);
			check(!rig.root.visible && model.body.visible && ArticulatedCombat.viewFrame(state) == null, "Offhand equipment is never hidden by a new art");
			if (left) state.rightHandItemStack = ItemStack.EMPTY; else state.leftHandItemStack = ItemStack.EMPTY;
			state.walkAnimationSpeed = .8F; model.setupAnim(state);
			check(!rig.root.visible && model.body.visible, "Ordinary locomotion retains the complete world body");
			state.walkAnimationSpeed = 0;
			HitStop.clear(); HitStop.hold(1000, mc.player.getId()); HitStop.extracted(mc.player, state);
			var later = new ArticulatedCombat.Frame(ArticulatedCombatPose.samplePlayer(move, rule.windup() + 2, rule.windup(), rule.recovery(), left), 999, move, false, left, 0, 0);
			state.setData(ArticulatedCombat.FRAME, later); HitStop.extracted(mc.player, state);
			check(state.getData(ArticulatedCombat.FRAME) == frame, "Cosmetic hold preserves the immutable shared-player palette");
			state.setData(ArticulatedCombat.FRAME, new ArticulatedCombat.Frame(later.pose(), 1000, move, false, left, 0, 0));
			HitStop.extracted(mc.player, state);
			check(state.getData(ArticulatedCombat.FRAME).activation() == 1000, "Replacement activation discards old held art");
			state.setData(ArticulatedCombat.FRAME, null); HitStop.extracted(mc.player, state); model.setupAnim(state);
			check(state.getData(ArticulatedCombat.FRAME) == null && !rig.root.visible && model.body.visible, "Cancellation cannot revive the previous palette");
			HitStop.clear();
		}
	}

	private static AvatarRenderState state(Minecraft mc) {
		return (AvatarRenderState) mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, .5F);
	}
	private static void prepare(ServerPlayer player, boolean armor, boolean fullArmor) {
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND})
			player.setItemSlot(slot, ItemStack.EMPTY);
		if (armor) {
			ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
			chest.enchant(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION), 4);
			player.setItemSlot(EquipmentSlot.CHEST, chest);
			if (fullArmor) {
				for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
					ItemStack item = new ItemStack(switch (slot) {
						case HEAD -> Items.NETHERITE_HELMET;
						case LEGS -> Items.NETHERITE_LEGGINGS;
						default -> Items.NETHERITE_BOOTS;
					});
					item.enchant(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION), 4);
					player.setItemSlot(slot, item);
				}
			}
		}
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("stone", 4, 1800, 100, 0));
		player.inventoryMenu.broadcastChanges();
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
