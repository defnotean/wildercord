package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.client.fx.HitStop;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Native HUD-on review matrix. Actual accepted casts are never replaced by synthetic timelines.
 * Screenshot indices identify samples, not exact impact boundaries. The logged pre-capture age
 * is contextual only: the framebuffer may advance before screenshot readback. Native screenshot
 * review is still required; compile success and submit receipts are not pixel acceptance.
 */
public final class ArticulatedFirstPersonCompositionTest implements FabricClientGameTest {
	private record Viewport(int width, int height, int gui) {}
	private static final Viewport[] VIEWPORTS = {
		new Viewport(854, 480, 2), new Viewport(1280, 720, 3),
		new Viewport(1280, 960, 4), new Viewport(1920, 810, 3)
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		String[] properties = {ArticulatedCombat.ENABLE_PROPERTY, ArticulatedCombat.STABLE_CAMERA_PROPERTY,
			ArticulatedArmorRenderer.ENABLE_PROPERTY, ArticulatedArmorRenderer.VIEW_PROPERTY};
		String[] previous = Arrays.stream(properties).map(System::getProperty).toArray(String[]::new);
		CameraType camera = context.computeOnClient(mc -> mc.options.getCameraType());
		HumanoidArm hand = context.computeOnClient(mc -> mc.options.mainHand().get());
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
				prepare(player, false);
			});
			context.waitTicks(12);
			context.runOnClient(ArticulatedFirstPersonCompositionTest::submissionBoundaries);
			for (Viewport viewport : VIEWPORTS) for (boolean armor : new boolean[] {false, true})
				for (HumanoidArm arm : HumanoidArm.values()) {
					context.waitTicks(105);
					world.getServer().runOnServer(server -> prepare(server.getPlayerList().getPlayers().getFirst(), armor));
					context.runOnClient(mc -> {
						mc.getWindow().setWindowed(viewport.width(), viewport.height());
						mc.options.guiScale().set(viewport.gui());
						mc.resizeGui();
						mc.options.mainHand().set(arm);
						mc.options.broadcastOptions();
						mc.options.setCameraType(CameraType.FIRST_PERSON);
						if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
						mc.gui.setScreen(null);
						mc.player.setYRot(0); mc.player.setXRot(0);
					});
					context.waitTicks(8);
					String prefix = "articulated_hud_" + viewport.width() + "x" + viewport.height() + "_gui" + viewport.gui()
						+ "_" + (armor ? "netherite" : "skin") + "_" + arm.name().toLowerCase(Locale.ROOT);
					context.runOnClient(mc -> {
						check(mc.getWindow().getWidth() == viewport.width() && mc.getWindow().getHeight() == viewport.height(), "Actual framebuffer matches capture size");
						check(mc.options.guiScale().get() == viewport.gui() && !mc.gui.hud.isHidden(), "HUD stays visible at the requested UI scale");
						check(ArticulatedCombat.viewFrame(state(mc)) != null, "Fully equipped idle owns the viewmodel before the cast");
					});
					context.takeScreenshot(TestScreenshotOptions.of(prefix + "_idle_before").disableCounterPrefix());
					context.getInput().pressKey(MastersArtsClient.mapping(0));
					context.waitFor(mc -> MastersArtsClient.timeline(mc.player) != null, 30);
					int captures = 0;
					EnumSet<ArticulatedCombatPose.Phase> observedPhases = EnumSet.noneOf(ArticulatedCombatPose.Phase.class);
					for (int sample = 0; sample < 10; sample++) {
						context.waitTicks(1);
						String name = prefix + "_sample_" + sample;
						ArticulatedCombatPose.Phase sampledPhase = context.computeOnClient(mc -> {
							var state = state(mc);
							var frame = ArticulatedCombat.frame(state);
							if (frame == null) return null;
							check(state.mainArm == arm && !mc.gui.hud.isHidden(), "Live capture retains selected hand and HUD");
							check(mc.player.getYRot() == 0 && mc.player.getXRot() == 0, "Combat never changes the player's stable camera");
							check(ArticulatedCombat.viewFrame(state) != null, "Actual accepted state owns first-person arms");
							var timeline = MastersArtsClient.timeline(mc.player);
							System.out.println("ARTICULATED_HUD_SAMPLE name=" + name + " actualSkin=" + state.skin.model()
								+ " preCaptureAge=" + (mc.level.getGameTime() - timeline.startTick() + .5F)
								+ " preCapturePhase=" + frame.pose().phase() + " handFov=70 nativePixelReviewRequired=true");
							return frame.pose().phase();
						});
						if (sampledPhase != null) {
							context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
							observedPhases.add(sampledPhase); captures++;
						}
					}
					check(captures >= 2 && observedPhases.contains(ArticulatedCombatPose.Phase.RECOVERY),
						"HUD matrix includes a live recovery sample, not only early windup: " + observedPhases);
					System.out.println("ARTICULATED_HUD_COVERAGE name=" + prefix + " preCapturePhases=" + observedPhases
						+ " exactImpactPixelCoverage=unverified");
					context.waitTicks(25);
					check(context.computeOnClient(mc -> MastersArtsClient.timeline(mc.player) == null && ArticulatedCombat.viewFrame(state(mc)) != null),
						"Unchanged server expiry returns directly to the stable articulated idle");
					context.takeScreenshot(TestScreenshotOptions.of(prefix + "_idle_after").disableCounterPrefix());
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
				mc.getWindow().setWindowed(original.width(), original.height());
				mc.getWindow().setFullscreen(fullscreen);
				mc.options.guiScale().set(original.gui()); mc.resizeGui();
				if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
				MastersArtsClient.mapping(0).setDown(false);
			});
		}
	}

	/** Native-model/submit checks are explicitly synthetic and do not label screenshots as wide/slim. */
	private static void submissionBoundaries(Minecraft mc) {
		for (boolean slim : new boolean[] {false, true}) for (boolean left : new boolean[] {false, true}) {
			ArticulatedViewModel model = new ArticulatedViewModel(slim);
			Vector3f baseline = null;
			for (float age : new float[] {-1, 0, .0001F, 15.9999F, 16}) {
				var view = ArticulatedCombatPose.view(ArticulatedCombatPose.sampleSpellcut(0, age, 4, 12, left), left);
				model.setupAnim(new ArticulatedViewModel.Frame(view, true, true));
				PoseStack stack = new PoseStack(); model.rig().socket(left ? HumanoidArm.LEFT : HumanoidArm.RIGHT, stack);
				Vector3f grip = stack.last().pose().transformPosition(new Vector3f());
				if (baseline == null) baseline = grip;
				else check(grip.distance(baseline) < .0001F, "Actual wide/slim ModelPart grip converges across entry and exit");
			}
		}
		AvatarRenderState state = state(mc);
		state.setData(dev.wildercord.client.render.AuraShellLayer.SHELL_GLOW, null);
		state.showCape = false; state.walkAnimationSpeed = 0; state.swingAnimation = 0;
		state.setData(ArticulatedCombat.KNOWN_LAYERS, true);
		state.setData(ArticulatedCombat.FRAME, new ArticulatedCombat.Frame(ArticulatedCombatPose.NONE, Long.MIN_VALUE, -1, false,
			state.mainArm == HumanoidArm.LEFT, 0, 0));
		FirstPersonHandsAndItemsRenderState hands = new FirstPersonHandsAndItemsRenderState();
		hands.mainHandItem = state.getMainHandItemStack().copy(); hands.oldMainHandHeight = hands.mainHandHeight = 1;
		List<String> calls = new ArrayList<>(); SubmitNodeCollector collector = collector(calls);
		PoseStack stack = new PoseStack(); Matrix4f before = new Matrix4f(stack.last().pose());
		check(ArticulatedViewModel.submit(state, hands, .5F, InteractionHand.MAIN_HAND, stack, collector, 0x00F000F0), "Idle native submit owns the viewmodel");
		check(calls.contains("submitModel") && calls.contains("submitItem"), "Ownership submits real skin and held-item nodes");
		check(before.equals(stack.last().pose()), "Native submit restores the caller's transform");
		int count = calls.size();
		check(ArticulatedViewModel.submit(state, hands, .5F, InteractionHand.OFF_HAND, stack, collector, 0x00F000F0) && count == calls.size(), "Offhand callback never duplicates the arms");
		hands.oldMainHandHeight = hands.mainHandHeight = .9F;
		check(!ArticulatedViewModel.submit(state, hands, .5F, InteractionHand.MAIN_HAND, stack, collector, 0x00F000F0) && count == calls.size(), "Equip transition retains the complete vanilla path");
		hands.oldMainHandHeight = hands.mainHandHeight = 1;
		hands.mainHandItem = new ItemStack(Items.STONE_SWORD);
		check(!ArticulatedViewModel.submit(state, hands, .5F, InteractionHand.MAIN_HAND, stack, collector, 0x00F000F0) && count == calls.size(), "Unmatched item swap cannot submit a stale viewmodel");
		hands.mainHandItem = state.getMainHandItemStack().copy(); state.swingAnimation = .5F;
		check(!ArticulatedViewModel.submit(state, hands, .5F, InteractionHand.MAIN_HAND, stack, collector, 0x00F000F0) && count == calls.size(), "Ordinary swing retains its native first-person animation");
		state.swingAnimation = 0; state.chestEquipment = new ItemStack(Items.DIAMOND_CHESTPLATE);
		check(!ArticulatedViewModel.submit(state, hands, .5F, InteractionHand.MAIN_HAND, stack, collector, 0x00F000F0) && count == calls.size(), "Unsupported armor retains all fallback gear and hands");
	}

	private static SubmitNodeCollector collector(List<String> calls) {
		return (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(), new Class<?>[] {SubmitNodeCollector.class},
			(proxy, method, args) -> {
				if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
					case "toString" -> "CompositionReceipts";
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default -> throw new AssertionError(method);
				};
				if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
				if (method.getName().equals("order")) return proxy;
				calls.add(method.getName()); return null;
			});
	}
	private static AvatarRenderState state(Minecraft mc) {
		return (AvatarRenderState) mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, .5F);
	}
	private static void prepare(ServerPlayer player, boolean armor) {
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND})
			player.setItemSlot(slot, ItemStack.EMPTY);
		if (armor) {
			ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
			chest.enchant(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION), 4);
			player.setItemSlot(EquipmentSlot.CHEST, chest);
		}
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("stone", 3, 1800, 100, 0));
		player.inventoryMenu.broadcastChanges();
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
