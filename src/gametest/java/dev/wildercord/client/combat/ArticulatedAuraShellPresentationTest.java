package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraArmour;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.MastersArtRules;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.client.fx.HitStop;
import dev.wildercord.client.render.AuraShellLayer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
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
 * Funded stage-four/five native acceptance, separate from the exact-cost shell-down baseline.
 * Captures use real key input and ordinary shell upkeep. Injected alternating-state probes are
 * explicitly separate from screenshots. Compilation and receipts do not establish pixel quality.
 */
public final class ArticulatedAuraShellPresentationTest implements FabricClientGameTest {
	private static final int LIGHT = 0x00F000F0;
	private static final int[] MOVES = {0, 1, 2};
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	@Override public void runTest(ClientGameTestContext context) {
		String[] properties = {ArticulatedCombat.ENABLE_PROPERTY, ArticulatedCombat.STABLE_CAMERA_PROPERTY,
			ArticulatedArmorRenderer.ENABLE_PROPERTY, ArticulatedArmorRenderer.VIEW_PROPERTY, ArticulatedAuraShellRenderer.ENABLE_PROPERTY};
		String[] previous = Arrays.stream(properties).map(System::getProperty).toArray(String[]::new);
		CameraType camera = context.computeOnClient(mc -> mc.options.getCameraType());
		HumanoidArm hand = context.computeOnClient(mc -> mc.options.mainHand().get());
		boolean hidden = context.computeOnClient(mc -> mc.gui.hud.isHidden());
		for (String property : properties) System.setProperty(property, "true");
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(30);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("fill -10 99 -10 10 99 10 minecraft:stone_bricks");
			world.getServer().runCommand("fill -10 100 -10 10 108 10 minecraft:air");
			world.getServer().runCommand("time set 3000");
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(server.overworld(), .5, 100, .5, Set.<Relative>of(), 0, 0, false);
				prepare(player, false, 4);
			});
			context.waitTicks(12);
			ArticulatedAuraShellRenderer before = context.computeOnClient(mc -> state(mc).getData(ArticulatedAuraShellRenderer.READY));
			check(before != null, "Primary renderer exposes its exact shell adapter");
			var reload = context.computeOnClient(Minecraft::reloadResourcePacks);
			context.waitFor(mc -> reload.isDone(), 1200); reload.join();
			context.runOnClient(mc -> {
				var after = state(mc).getData(ArticulatedAuraShellRenderer.READY);
				check(after != null && after != before && after.worldModel() != before.worldModel() && after.viewModel() != before.viewModel(),
					"Actual resource reload recreates the complete body/shell owner and both geometries");
			});
			for (boolean armored : new boolean[] {false, true}) for (HumanoidArm arm : HumanoidArm.values())
				for (CameraType view : new CameraType[] {CameraType.THIRD_PERSON_FRONT, CameraType.THIRD_PERSON_BACK, CameraType.FIRST_PERSON})
					for (int move : MOVES) {
						context.waitTicks(105);
						int stage = arm == HumanoidArm.RIGHT ? 4 : 5;
						world.getServer().runOnServer(server -> prepare(server.getPlayerList().getPlayers().getFirst(), armored, stage));
						context.runOnClient(mc -> {
							mc.options.mainHand().set(arm); mc.options.broadcastOptions(); mc.options.setCameraType(view);
							if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
							mc.gui.setScreen(null); mc.player.setYRot(0); mc.player.setXRot(0);
						});
						context.waitTicks(8);
						context.runOnClient(mc -> {
							check(AuraPresence.look(mc.player).shell() && state(mc).getData(AuraShellLayer.SHELL_GLOW) == null, "Funded aura armour stays up but its shell hides until the aura is used");
						});
						context.getInput().pressKey(MastersArtsClient.mapping(move));
						context.waitFor(mc -> {
							var accepted = MastersArtsClient.timeline(mc.player);
							var frame = ArticulatedCombat.frame(state(mc));
							return accepted != null && accepted.move() == move && frame != null && frame.move() == move;
						}, 35);
						world.getServer().runOnServer(server -> {
							var player = server.getPlayerList().getPlayers().getFirst();
							check(Aura.stage(player) == stage && AuraArmour.up(player) && Aura.aura(player) < 100 - MastersArtRules.move(move).cost() + 2,
								"Real accepted input pays the ordinary art price while retaining funded Aura Armour");
						});
						String name = "articulated_funded_shell_" + MastersArtRules.move(move).id() + "_" + view.name().toLowerCase(Locale.ROOT)
							+ "_" + arm.name().toLowerCase(Locale.ROOT) + "_stage" + stage + (armored ? "_netherite" : "_skin");
						context.runOnClient(mc -> {
							MastersArtsClient.mapping(move).setDown(false);
							var state = state(mc); var frame = ArticulatedCombat.frame(state); var accepted = MastersArtsClient.timeline(mc.player);
							check(frame != null && state.mainArm == arm && accepted.move() == move, "Exact real activation owns both selected hands");
							check(accepted.windup() == MastersArtRules.move(move).windup() && accepted.recovery() == MastersArtRules.move(move).recovery(), "Server windows are unchanged");
							assertWorld(mc, state, armored); assertView(state, armored);
							System.out.println("ARTICULATED_FUNDED_SHELL name=" + name + " activation=" + accepted.startTick() + " shellGlow=" + Integer.toHexString(state.getData(AuraShellLayer.SHELL_GLOW))
								+ " actualSkin=" + state.skin.model() + " preCapturePhase=" + frame.pose().phase() + " preCaptureAge=" + (mc.level.getGameTime() - accepted.startTick() + .5F)
								+ " renderedPhase=unknown nativePixelReviewRequired=true");
						});
						context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
						// Observe every available simulation tick through expiry. No shell or armor is cleared.
						for (int tick = 0; tick < MastersArtRules.move(move).windup() + MastersArtRules.move(move).recovery() + 2; tick++) {
							context.runOnClient(mc -> { var current = state(mc); assertWorld(mc, current, armored); assertView(current, armored); });
							context.waitTicks(1);
						}
						context.runOnClient(mc -> {
							check(MastersArtsClient.timeline(mc.player) == null && ArticulatedCombat.viewFrame(state(mc)) != null,
								"Ordinary expiry retains the funded first-person shell and idle arms");
						});
						// A fresh real activation seeds clearly labelled synthetic replay/fallback probes.
						context.waitTicks(105); context.getInput().pressKey(MastersArtsClient.mapping(move));
						context.waitFor(mc -> ArticulatedCombat.frame(state(mc)) != null, 35);
						context.runOnClient(mc -> { MastersArtsClient.mapping(move).setDown(false); ownershipAndDeferred(mc); });
						long cancelled = context.computeOnClient(mc -> MastersArtsClient.timeline(mc.player).startTick());
						world.getServer().runOnServer(server -> check(MastersArts.cancel(server.getPlayerList().getPlayers().getFirst()), "Server actually cancels the pending funded activation"));
						context.waitFor(mc -> MastersArtsClient.timeline(mc.player) == null, 35);
						context.runOnClient(mc -> {
							var idle = state(mc); var frame = ArticulatedCombat.viewFrame(idle);
							check(ArticulatedCombat.frame(idle) == null && frame != null && frame.move() == -1 && frame.activation() != cancelled,
								"Cancelled activation disappears; complete world fallback and funded first-person idle remain");
							assertWorld(mc, idle, armored); assertView(idle, armored);
						});
					}
		} finally {
			HitStop.clear();
			for (int i = 0; i < properties.length; i++) { if (previous[i] == null) System.clearProperty(properties[i]); else System.setProperty(properties[i], previous[i]); }
			context.runOnClient(mc -> {
				mc.options.setCameraType(camera); mc.options.mainHand().set(hand); mc.options.broadcastOptions();
				if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
				for (int move : MOVES) MastersArtsClient.mapping(move).setDown(false);
			});
		}
	}

	private static void assertWorld(Minecraft mc, AvatarRenderState state, boolean armored) {
		PlayerModel body = renderer(mc).getModel(); var access = (ArticulatedModelAccess) body;
		body.setupAnim(state);
		boolean active = ArticulatedCombat.frame(state) != null;
		check(access.wildercord$rig().root.visible == active && body.body.visible != active && body.leftArm.visible != active && body.rightLeg.visible != active,
			"Exactly one complete body owns each sampled funded frame");
		check(state.getData(AuraShellLayer.SHELL_GLOW) != null, "Funded sample retains the real shell");
		Receipts shell = new Receipts(); shell(mc).submit(new PoseStack(), shell.collector(), LIGHT, state, 0, 0);
		check(shell.models.size() == 1, "Exactly one shell node, including fallback and clip edges");
		check(shell.models.getFirst().model() instanceof ArticulatedAuraShellGeometry == active, "Rigid and articulated shell never overlap");
		material(shell.models.getFirst(), state);
		var armorLayer = ((ArticulatedRendererAccess) renderer(mc)).wildercord$layers().stream()
			.filter(layer -> layer.getClass() == ArticulatedArmorLayer.class).map(layer -> (ArticulatedArmorLayer) layer).findFirst().orElseThrow();
		Receipts armor = new Receipts(); armorLayer.submit(new PoseStack(), armor.collector(), LIGHT, state, 0, 0);
		check(armor.models.size() == (armored ? 12 : 0), "All four genuine trimmed enchanted armor slots survive every body transition");
		if (armored) check(armor.models.stream().allMatch(r -> r.model() instanceof ArticulatedArmorGeometry == active), "Every armor slot follows the same whole backend");
	}

	private static void assertView(AvatarRenderState state, boolean armored) {
		FirstPersonHandsAndItemsRenderState hands = new FirstPersonHandsAndItemsRenderState();
		hands.mainHandItem = state.getMainHandItemStack().copy(); hands.oldMainHandHeight = hands.mainHandHeight = 1;
		((dev.wildercord.client.MastersHandMotionState) hands).wildercord$mainHandEquipping(false);
		Receipts receipts = new Receipts();
		check(ArticulatedViewModel.submit(state, hands, .5F, InteractionHand.MAIN_HAND, new PoseStack(), receipts.collector(), LIGHT), "Funded first-person path owns arms and sword");
		check(receipts.models.stream().filter(r -> r.model() instanceof ArticulatedViewModel).count() == 1, "First person submits skin exactly once");
		var shells = receipts.models.stream().filter(r -> r.model() instanceof ArticulatedAuraShellGeometry).toList();
		check(shells.size() == 1 && shells.getFirst().model() == state.getData(ArticulatedAuraShellRenderer.READY).viewModel(), "First person submits only arm-shell geometry exactly once");
		material(shells.getFirst(), state);
		check(receipts.models.stream().filter(r -> r.model() instanceof ArticulatedArmorGeometry).count() == (armored ? 3 : 0), "Camera-space chestplate retains material, trim and glint");
		check(receipts.calls.contains("submitItem"), "Actual resolved sword is submitted after skin, armor and shell");
		int before = receipts.calls.size();
		check(ArticulatedViewModel.submit(state, hands, .5F, InteractionHand.OFF_HAND, new PoseStack(), receipts.collector(), LIGHT)
			&& before == receipts.calls.size(), "Offhand callback cannot submit duplicate arms, shell, armor or weapon");
	}

	/** Injected probes; none of these states are used for the real-input screenshots above. */
	private static void ownershipAndDeferred(Minecraft mc) {
		var a = state(mc); var original = a.getData(ArticulatedCombat.FRAME); var body = renderer(mc).getModel(); var layer = shell(mc);
		check(ArticulatedCombat.frame(a) != null, "Synthetic probes begin from a real accepted activation");
		body.setupAnim(a); Receipts ra = new Receipts(); layer.submit(new PoseStack(), ra.collector(), LIGHT, a, 0, 0);
		var first = ra.models.getFirst(); var bits = geometry(first);
		var b = state(mc);
		int alternate = original.move() == 1 ? 0 : 1;
		var rule = MastersArtRules.move(alternate);
		var pose = ArticulatedCombatPose.samplePlayer(alternate, rule.windup(), rule.windup(), rule.recovery(), original.leftHanded());
		b.setData(ArticulatedCombat.FRAME, new ArticulatedCombat.Frame(pose, original.activation() + 1, alternate, false, original.leftHanded(), 0, 0));
		b.setData(AuraShellLayer.SHELL_GLOW, 0xC0FF2040); b.outlineColor = 0xFF123456;
		b.headEquipment = b.chestEquipment = b.legsEquipment = b.feetEquipment = ItemStack.EMPTY;
		body.setupAnim(b); Receipts rb = new Receipts(); layer.submit(new PoseStack(), rb.collector(), LIGHT, b, 0, 0);
		check(rb.models.size() == 1 && rb.models.getFirst().color() == 0xC0FF2040, "Alternating funded state carries its current color/alpha and armor choice");
		check(!bits.equals(geometry(rb.models.getFirst())), "Alternating pose really changes shell geometry");
		b.setData(AuraShellLayer.SHELL_GLOW, 0x01000001);
		check(first.color() == a.getData(AuraShellLayer.SHELL_GLOW) && rb.models.getFirst().color() == 0xC0FF2040,
			"Queued shell colors and alpha do not read a later entity state");
		check(bits.equals(geometry(first)), "Deferred shell A/B/A restores exact positions, UVs and normals");
		var palette = (ArticulatedArmorGeometry.Palette) first.state();
		palette.matrix(ArticulatedCombatPose.Joint.HEAD).zero();
		check(bits.equals(geometry(first)), "A caller cannot alter the captured final shell palette");
		// Compare all material arguments with the real original ShellModel submission.
		System.setProperty(ArticulatedAuraShellRenderer.ENABLE_PROPERTY, "false");
		try {
			body.setupAnim(a); Receipts fallback = new Receipts(); layer.submit(new PoseStack(), fallback.collector(), LIGHT, a, 0, 0);
			check(ArticulatedCombat.frame(a) == null && ArticulatedCombat.viewFrame(a) == null && body.body.visible,
				"Adapter kill switch restores the whole rigid backend");
			check(fallback.models.size() == 1 && !(fallback.models.getFirst().model() instanceof ArticulatedAuraShellGeometry), "Kill switch retains one actual rigid shell");
			var old = fallback.models.getFirst();
			check(first.type() == old.type() && first.light() == old.light() && first.overlay() == old.overlay() && first.color() == old.color()
				&& first.outline() == old.outline() && first.order() == old.order() && first.uv() == old.uv(), "Adapter preserves exact original shell material, alpha, texture, outline and ordering");
		} finally { System.setProperty(ArticulatedAuraShellRenderer.ENABLE_PROPERTY, "true"); }
		for (int scenario = 0; scenario < 5; scenario++) {
			var unsupported = state(mc);
			switch (scenario) {
				case 0 -> unsupported.setData(ArticulatedCombat.KNOWN_LAYERS, false);
				case 1 -> unsupported.chestEquipment = new ItemStack(Items.DIAMOND_CHESTPLATE);
				case 2 -> { if (unsupported.mainArm == HumanoidArm.LEFT) unsupported.rightHandItemStack = new ItemStack(Items.SHIELD); else unsupported.leftHandItemStack = new ItemStack(Items.SHIELD); }
				case 3 -> unsupported.setData(AuraShellLayer.IMAGES, List.of());
				case 4 -> unsupported.setData(ArticulatedAuraShellRenderer.READY, null);
			}
			body.setupAnim(unsupported); Receipts receipt = new Receipts(); layer.submit(new PoseStack(), receipt.collector(), LIGHT, unsupported, 0, 0);
			check(ArticulatedCombat.frame(unsupported) == null && ArticulatedCombat.viewFrame(unsupported) == null && body.body.visible,
				"Unsupported feature retains complete body/view fallback: " + scenario);
			check(receipt.models.size() == 1 && !(receipt.models.getFirst().model() instanceof ArticulatedAuraShellGeometry), "Unsupported feature still draws exactly one original shell");
		}
		body.setupAnim(a);
	}

	private static void material(Receipt receipt, AvatarRenderState state) {
		check(receipt.color() == state.getData(AuraShellLayer.SHELL_GLOW) && receipt.light() == LIGHT && receipt.overlay() == net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY
			&& receipt.outline() == 0 && receipt.order() == 1 && receipt.uv() == null, "Original shell ARGB, brightness, order, overlay and outline survive submission");
	}
	private record Receipt(Model<?> model, Object state, RenderType type, int light, int overlay, int color, Object uv, int outline, int order) {}
	private static final class Receipts {
		final List<Receipt> models = new ArrayList<>(); final List<String> calls = new ArrayList<>();
		SubmitNodeCollector collector() { return ordered(0); }
		SubmitNodeCollector ordered(int order) {
			return (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(), new Class<?>[] {SubmitNodeCollector.class}, (proxy, method, args) -> {
				if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
					case "toString" -> "ShellReceipts[" + order + "]"; case "hashCode" -> System.identityHashCode(proxy); case "equals" -> proxy == args[0]; default -> throw new AssertionError(method);
				};
				if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
				if (method.getName().equals("order")) return ordered((int) args[0]);
				calls.add(method.getName());
				if (method.getName().equals("submitModel")) {
					check(args.length == 9, "Full original submitModel signature");
					models.add(new Receipt((Model<?>) args[0], args[1], (RenderType) args[3], (int) args[4], (int) args[5], (int) args[6], args[7], (int) args[8], order));
				}
				return null;
			});
		}
	}
	@SuppressWarnings("unchecked") private static List<Integer> geometry(Receipt receipt) {
		((Model<Object>) receipt.model()).setupAnim(receipt.state()); List<Integer> values = new ArrayList<>();
		receipt.model().root().visit(new PoseStack(), (pose, path, index, cube) -> {
			for (var polygon : cube.polygons) {
				for (var v : polygon.vertices()) add(values, v.x(), v.y(), v.z(), v.u(), v.v());
				add(values, polygon.normal().x(), polygon.normal().y(), polygon.normal().z());
			}
		});
		return List.copyOf(values);
	}
	private static void add(List<Integer> values, float... floats) { for (float f : floats) { check(Float.isFinite(f), "Finite shell geometry"); values.add(Float.floatToIntBits(f)); } }
	private static void prepare(ServerPlayer player, boolean armored, int stage) {
		for (EquipmentSlot slot : SLOTS) player.setItemSlot(slot, armored ? armor(switch (slot) {
			case HEAD -> Items.NETHERITE_HELMET; case CHEST -> Items.NETHERITE_CHESTPLATE; case LEGS -> Items.NETHERITE_LEGGINGS; default -> Items.NETHERITE_BOOTS;
		}, player) : ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD)); player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(stage == 4 ? "stone" : "gale", stage, 1800, 100, 0));
		player.inventoryMenu.broadcastChanges();
		check(AuraArmour.up(player), "Fixture starts with genuine automatic Aura Armour, without forced shell flags");
	}
	private static ItemStack armor(Item item, ServerPlayer player) {
		var stack = new ItemStack(item); var registry = player.level().registryAccess();
		stack.enchant(registry.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION), 4);
		stack.set(DataComponents.TRIM, new ArmorTrim(registry.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.GOLD),
			registry.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.SENTRY)));
		return stack;
	}
	private static AuraShellLayer shell(Minecraft mc) { return ((ArticulatedRendererAccess) renderer(mc)).wildercord$layers().stream().filter(layer -> layer.getClass() == AuraShellLayer.class).map(layer -> (AuraShellLayer) layer).findFirst().orElseThrow(); }
	@SuppressWarnings("unchecked") private static AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer> renderer(Minecraft mc) { return (AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer>) mc.getEntityRenderDispatcher().getRenderer(mc.player); }
	private static AvatarRenderState state(Minecraft mc) { return renderer(mc).createRenderState(mc.player, .5F); }
	private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
