package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraArmour;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
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
	static final float CAPTURE_DELTA_TICKS = .5F;

	static void body(ClientGameTestContext context) { capture(context, false); }
	static void hud(ClientGameTestContext context) { capture(context, true); }

	/** Every phase uses its own fresh real key input; screenshot latency cannot skip the next phase. */
	private static void capture(ClientGameTestContext context, boolean hudMatrix) {
		ArticulatedSharedCaptureTimingChecks.verify();
		String[] properties = {ArticulatedCombat.ENABLE_PROPERTY, ArticulatedCombat.STABLE_CAMERA_PROPERTY,
			ArticulatedArmorRenderer.ENABLE_PROPERTY, ArticulatedArmorRenderer.VIEW_PROPERTY};
		String[] previous = Arrays.stream(properties).map(System::getProperty).toArray(String[]::new);
		// String-only reference keeps this fixture independently valid before the separate shell adapter lands.
		String shellAdapterProperty = "wildercord.articulated.auraShell";
		String previousShellAdapter = System.getProperty(shellAdapterProperty);
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
				prepare(player, false, false, MOVES[0]);
				// Separate negative control: the ordinary well-funded Form shell must retain fallback.
				player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(100));
			});
			context.waitTicks(12);
			System.setProperty(shellAdapterProperty, "false");
			context.runOnClient(mc -> mc.gui.setScreen(null));
			world.getServer().runOnServer(server -> serverReceipt(server.getPlayerList().getPlayers().getFirst(), MOVES[0], "well_funded_before_input"));
			var negativeAdmission = new java.util.ArrayDeque<String>();
			negativeAdmission.add(context.computeOnClient(mc -> "before_input " + clientReceipt(mc, MOVES[0])));
			context.getInput().pressKey(MastersArtsClient.mapping(MOVES[0]));
			try {
				context.waitFor(mc -> {
					retainAdmissionSample(negativeAdmission, clientReceipt(mc, MOVES[0]));
					var timeline = MastersArtsClient.timeline(mc.player);
					var state = state(mc); var raw = state.getData(ArticulatedCombat.FRAME);
					return timeline != null && timeline.move() == MOVES[0] && raw != null && raw.pose().weight() > 0
						&& state.getData(dev.wildercord.client.render.AuraShellLayer.SHELL_GLOW) != null;
				}, 35);
			} catch (AssertionError failure) {
				System.out.println("ARTICULATED_SHARED_NEGATIVE_TIMEOUT boundedClientSamples=" + negativeAdmission);
				world.getServer().runOnServer(server -> serverReceipt(server.getPlayerList().getPlayers().getFirst(), MOVES[0], "well_funded_timeout"));
				throw failure;
			}
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(Aura.stage(player) == 4 && AuraArmour.up(player) && MastersArts.committed(player)
					&& Math.abs(Aura.aura(player) - (100 - MastersArtRules.move(MOVES[0]).cost())) < .0001,
					"The well-funded negative control pays the real cost and retains its real Form shell");
				serverReceipt(player, MOVES[0], "well_funded_negative_control");
			});
			context.runOnClient(mc -> {
				var state = state(mc);
				var renderer = (net.minecraft.client.renderer.entity.player.AvatarRenderer<?>) mc.getEntityRenderDispatcher().getRenderer(mc.player);
				PlayerModel model = renderer.getModel(); model.setupAnim(state);
				ArticulatedRig rig = ((ArticulatedModelAccess) model).wildercord$rig();
				check(AuraPresence.look(mc.player).shell() && state.getData(dev.wildercord.client.render.AuraShellLayer.SHELL_GLOW) != null,
					"The well-funded negative control observes the actual synced shell");
				check(ArticulatedCombat.frame(state) == null && ArticulatedCombat.viewFrame(state) == null && !rig.root.visible
					&& model.body.visible && model.head.visible && model.rightArm.visible && model.leftArm.visible && model.rightLeg.visible && model.leftLeg.visible,
					"The active well-funded Form shell keeps the complete original body and first-person fallback");
				System.out.println("ARTICULATED_SHARED_NEGATIVE_CONTROL supportedPresentation=false auraShellAdapter=false " + clientReceipt(mc, MOVES[0]));
			});
			if (previousShellAdapter == null) System.clearProperty(shellAdapterProperty); else System.setProperty(shellAdapterProperty, previousShellAdapter);
			context.waitTicks(MastersArtRules.move(MOVES[0]).windup() + MastersArtRules.move(MOVES[0]).recovery() + 2);
			if (!hudMatrix) context.runOnClient(ArticulatedSharedPlayerChecks::syntheticBridge);
			for (Viewport viewport : hudMatrix ? VIEWPORTS : new Viewport[] {VIEWPORTS[1]})
				for (boolean armor : new boolean[] {false, true})
					for (HumanoidArm hand : HumanoidArm.values()) for (int move : MOVES)
						for (ArticulatedCombatPose.Phase phase : new ArticulatedCombatPose.Phase[] {
							ArticulatedCombatPose.Phase.WINDUP, ArticulatedCombatPose.Phase.ACTIVE, ArticulatedCombatPose.Phase.RECOVERY}) {
							context.waitTicks(105); // Existing 100/80 tick individual rests remain authoritative.
							world.getServer().runOnServer(server -> prepare(server.getPlayerList().getPlayers().getFirst(), armor, !hudMatrix, move));
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
							world.getServer().runOnServer(server -> {
								ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
								serverReceipt(player, move, name + "_before_input");
								check(Aura.enabled(player) && Aura.stage(player) >= rule.stage() && Aura.holdsWeapon(player)
									&& Math.abs(Aura.aura(player) - rule.cost()) < .0001 && !MastersArts.committed(player),
									"Supported trial starts with a real weapon, stage and exactly the selected move's cost");
							});
							var admission = new java.util.ArrayDeque<String>();
							admission.add(context.computeOnClient(mc -> "before_input " + clientReceipt(mc, move)));
							context.getInput().pressKey(MastersArtsClient.mapping(move));
							try {
								context.waitFor(mc -> {
									retainAdmissionSample(admission, clientReceipt(mc, move));
									var timeline = MastersArtsClient.timeline(mc.player);
									if (timeline == null || timeline.move() != move) return false;
									var frame = ArticulatedCombat.frame(state(mc));
									return frame != null && frame.pose().phase() == phase;
								}, 35);
							} catch (AssertionError failure) {
								System.out.println("ARTICULATED_SHARED_ADMISSION_TIMEOUT name=" + name + " boundedClientSamples=" + admission);
								world.getServer().runOnServer(server -> serverReceipt(server.getPlayerList().getPlayers().getFirst(), move, name + "_timeout"));
								throw failure;
							}
							world.getServer().runOnServer(server -> {
								ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
								serverReceipt(player, move, name + "_accepted");
								check(Math.abs(Aura.aura(player)) < .0001 && !AuraArmour.up(player),
									"The real accepted art paid its complete budget and naturally lowered the Aura shell");
							});
							context.runOnClient(mc -> {
								var timeline = MastersArtsClient.timeline(mc.player);
								var state = state(mc); var frame = ArticulatedCombat.frame(state);
								check(timeline != null && timeline.move() == move && frame != null && frame.move() == move && !frame.master(), "Exact accepted player activation owns the capture");
								check(!AuraPresence.look(mc.player).shell() && state.getData(dev.wildercord.client.render.AuraShellLayer.SHELL_GLOW) == null,
									"Supported presentation captures require the actual synced Aura shell to be down");
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
									+ " actualSkin=" + state.skin.model() + " preCaptureAge=" + (mc.level.getGameTime() - timeline.startTick() + CAPTURE_DELTA_TICKS)
									+ " preCapturePhase=" + frame.pose().phase() + " requestedPhase=" + phase + " auraShell=down supportedPresentation=shell_down_only renderedPhase=unknown nativePixelReviewRequired=true exactImpactPixelCoverage=unverified");
							});
							context.takeScreenshot(screenshotOptions(name));
							context.waitTicks(rule.windup() + rule.recovery() + 2);
							check(context.computeOnClient(mc -> MastersArtsClient.timeline(mc.player) == null && ArticulatedCombat.viewFrame(state(mc)) != null), "Accepted expiry returns to the stable view idle");
						}
			world.getServer().runOnServer(server -> MastersArts.cancel(server.getPlayerList().getPlayers().getFirst()));
		} finally {
			if (previousShellAdapter == null) System.clearProperty(shellAdapterProperty); else System.setProperty(shellAdapterProperty, previousShellAdapter);
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

	static TestScreenshotOptions screenshotOptions(String name) {
		// Fabric defaults to 1.0: at the accepted impact tick this projects into RECOVERY.
		// Use the same supported interpolation as admission; real receipt phases remain authoritative.
		return TestScreenshotOptions.of(name).disableCounterPrefix().withDeltaTicks(CAPTURE_DELTA_TICKS);
	}

	private static AvatarRenderState state(Minecraft mc) {
		return (AvatarRenderState) mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, CAPTURE_DELTA_TICKS);
	}
	private static void prepare(ServerPlayer player, boolean armor, boolean fullArmor, int move) {
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
		// Form's always-on shell deliberately has no articulated adapter. Exact real payment
		// leaves it naturally down; do not suppress its render data or lower the required stage.
		var rule = MastersArtRules.move(move);
		check(rule != null && rule.cost() > 0 && rule.cost() <= AuraRules.ARMOUR_MIN, "Known shared-art fixture budget");
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("stone", 4, 1800, (float) rule.cost(), 0));
		player.inventoryMenu.broadcastChanges();
	}
	/** Retain the initial input conditions and latest 35 polls, even on the final timeout poll. */
	private static void retainAdmissionSample(java.util.ArrayDeque<String> samples, String sample) {
		if (samples.size() == 36) {
			String beforeInput = samples.removeFirst(); samples.removeFirst(); samples.addFirst(beforeInput);
		}
		samples.addLast(sample);
	}

	/** Eligibility receipts retain normal getter housekeeping, including expired CastLock cleanup. */
	private static void serverReceipt(ServerPlayer player, int move, String at) {
		System.out.println("ARTICULATED_SHARED_SERVER at=" + at + " move=" + move + " tick=" + player.level().getGameTime()
			+ " aura=" + Aura.aura(player) + " cost=" + MastersArtRules.move(move).cost() + " stage=" + Aura.stage(player) + " method=" + Aura.data(player).method()
			+ " enabled=" + Aura.enabled(player) + " weapon=" + Aura.holdsWeapon(player) + " item=" + player.getMainHandItem()
			+ " shellUp=" + AuraArmour.up(player) + " syncedShell=" + AuraPresence.look(player).shell() + " committed=" + MastersArts.committed(player)
			+ " alive=" + player.isAlive() + " spectator=" + player.isSpectator() + " sleeping=" + player.isSleeping() + " passenger=" + player.isPassenger()
			+ " spent=" + dev.wildercord.aura.Awakening.spent(player) + " guard=" + dev.wildercord.aura.AuraGuard.guarding(player)
			+ " clash=" + dev.wildercord.aura.Clashes.holding(player) + " silence=" + dev.wildercord.aura.arts.ArtWards.silenced(player)
			+ " castLock=" + dev.wildercord.cast.CastLock.locked(player) + " stanceOpen=" + dev.wildercord.aura.Stance.opened(player)
			+ " yaw=" + player.getYRot() + " pitch=" + player.getXRot());
	}
	private static String clientReceipt(Minecraft mc, int move) {
		var state = state(mc); var raw = state.getData(ArticulatedCombat.FRAME);
		return "tick=" + mc.level.getGameTime() + " move=" + move + " key=" + MastersArtsClient.mapping(move).getTranslatedKeyMessage().getString()
			+ " keyDown=" + MastersArtsClient.mapping(move).isDown() + " canSend=" + net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(MastersArts.Activate.TYPE)
			+ " screen=" + (mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName()) + " paused=" + mc.isPaused()
			+ " timeline=" + MastersArtsClient.timeline(mc.player) + " raw=" + (raw == null ? "none" : raw.move() + ":" + raw.pose().phase() + ":" + raw.pose().weight())
			+ " bodyEligible=" + (ArticulatedCombat.frame(state) != null) + " viewEligible=" + (ArticulatedCombat.viewFrame(state) != null)
			+ " aura=" + Aura.aura(mc.player) + " stage=" + Aura.stage(mc.player) + " shell=" + AuraPresence.look(mc.player).shell()
			+ " shellGlow=" + state.getData(dev.wildercord.client.render.AuraShellLayer.SHELL_GLOW)
			+ " afterimages=" + (state.getData(dev.wildercord.client.render.AuraShellLayer.IMAGES) != null)
			+ " knownLayers=" + state.getData(ArticulatedCombat.KNOWN_LAYERS) + " armorEligible=" + ArticulatedArmorRenderer.compatible(state)
			+ " walk=" + state.walkAnimationSpeed + " swing=" + state.swingAnimation + " crouching=" + state.isCrouching + " usingItem=" + state.isUsingItem
			+ " invisible=" + state.isInvisible + " passenger=" + state.isPassenger + " flying=" + state.isFallFlying
			+ " main=" + state.getMainHandItemStack() + " off=" + mc.player.getOffhandItem()
			+ " cape=" + (state.showCape && state.skin != null && state.skin.cape() != null)
			+ " pack=" + (state.getData(dev.wildercord.client.render.GearLook.PACK) != null) + " gear=" + (state.getData(dev.wildercord.client.render.GearLook.PIECES) != null);
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
