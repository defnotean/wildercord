package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraArmour;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.aura.MastersStyleRules;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.client.SwordStringsClient;
import dev.wildercord.client.fx.HitStop;
import dev.wildercord.client.render.AuraShellLayer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Additive native first-form checks, ready for the existing articulated body and first-person suites.
 * Every screenshot follows a fresh, server-accepted attack/attack/low string. Synthetic skin-width,
 * socket and fallback probes are separate and never stand in for a live player's screenshot.
 * The opening-style screenshot prefix is intentionally outside the shared-art readback receipt scope:
 * requested/pre-capture phase is observed here; screenshot latency leaves rendered phase unverified.
 */
final class ArticulatedOpeningStyleChecks {
	private ArticulatedOpeningStyleChecks() {}
	private static final float CAPTURE_TICK_DELTA = .5F;
	private static final int[] MOVES = {3, 4};
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final ArticulatedCombatPose.Phase[] PHASES = {ArticulatedCombatPose.Phase.WINDUP,
		ArticulatedCombatPose.Phase.ACTIVE, ArticulatedCombatPose.Phase.RECOVERY};
	private record Settings(CameraType camera, HumanoidArm hand, int width, int height, int gui,
		boolean fullscreen, boolean hidden, boolean toggleCrouch, boolean attackDown, boolean shiftDown) {}
	private record Spend(String art, long tick, double paid, double expected, float remaining,
		int rest, boolean backlash, boolean committed) {}
	private record Completion(String art, long acceptedAt, long performedAt, List<Integer> marks) {}

	static void body(ClientGameTestContext context) { capture(context, false); }
	static void hud(ClientGameTestContext context) { capture(context, true); }

	/** Temporary real server hooks isolate art payment from legitimate swing-coat costs and gains. */
	private static final class Audit implements AutoCloseable {
		volatile UUID owner;
		final List<Spend> spends = new CopyOnWriteArrayList<>();
		final List<Completion> completions = new CopyOnWriteArrayList<>();
		final AuraApi.SpendHook spend = (player, paid, reason, backlash) -> {
			if (!player.getUUID().equals(owner) || !reason.startsWith("art:")) return;
			String id = reason.substring(4);
			if (!opening(id)) return;
			var art = AuraApi.string(id).orElseThrow();
			spends.add(new Spend(id, player.level().getGameTime(), paid, SwordStrings.price(player, art),
				Aura.aura(player), SwordStrings.rest(player, art), backlash, MastersArts.committed(player)));
		};
		final AuraApi.StringHook completion = (player, art, context) -> {
			if (player.getUUID().equals(owner) && opening(art.id()))
				completions.add(new Completion(art.id(), context.at(), player.level().getGameTime(), context.marks()));
		};
		Audit() { AuraApi.onSpend(spend); AuraApi.onString(completion); }
		void reset(ServerPlayer player) { owner = player.getUUID(); spends.clear(); completions.clear(); }
		@Override public void close() { owner = null; AuraApi.spendHooks().remove(spend); AuraApi.stringHooks().remove(completion); }
	}

	private static boolean opening(String art) { return art.equals("kindling_draw") || art.equals("frostbite"); }

	private static void capture(ClientGameTestContext context, boolean firstPerson) {
		String[] properties = {ArticulatedCombat.ENABLE_PROPERTY, ArticulatedCombat.STABLE_CAMERA_PROPERTY,
			ArticulatedArmorRenderer.ENABLE_PROPERTY, ArticulatedArmorRenderer.VIEW_PROPERTY, ArticulatedAuraShellRenderer.ENABLE_PROPERTY};
		String[] previous = Arrays.stream(properties).map(System::getProperty).toArray(String[]::new);
		Settings saved = context.computeOnClient(mc -> new Settings(mc.options.getCameraType(), mc.options.mainHand().get(),
			mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight(), mc.options.guiScale().get(),
			mc.options.fullscreen().get(), mc.gui.hud.isHidden(), mc.options.toggleCrouch().get(),
			mc.options.keyAttack.isDown(), mc.options.keyShift.isDown()));
		try {
			for (String property : properties) System.setProperty(property, "true");
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720); mc.options.guiScale().set(3); mc.resizeGui();
				mc.options.toggleCrouch().set(false);
				mc.options.keyAttack.setDown(false); mc.options.keyShift.setDown(false);
				if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			try (Audit audit = new Audit(); var world = context.worldBuilder().create()) {
				context.waitTicks(30);
				world.getServer().runCommand("gamerule spawn_mobs false");
				world.getServer().runCommand("gamerule advance_time false");
				world.getServer().runCommand("fill -10 99 -10 10 99 10 minecraft:stone_bricks");
				world.getServer().runCommand("fill -10 100 -10 10 108 10 minecraft:air");
				world.getServer().runCommand("time set 3000");
				world.getServer().runCommand("weather clear");
				world.getServer().runOnServer(server -> prepare(server.getPlayerList().getPlayers().getFirst(), style(3), false, false));
				context.waitTicks(12);
				if (!firstPerson) context.runOnClient(ArticulatedOpeningStyleChecks::syntheticBridge);
				for (int move : MOVES) for (HumanoidArm hand : HumanoidArm.values()) {
					// Full phase coverage for the bare Glow baseline and the combined armor/funded-shell backend.
					// The crossed combinations retain their own ACTIVE captures; exhaustive offline geometry
					// sweeps cover all ages. This representative matrix is 36 requests per camera mode.
					for (boolean armored : new boolean[] {false, true}) for (boolean shell : new boolean[] {false, true})
						for (var phase : armored == shell ? PHASES : new ArticulatedCombatPose.Phase[] {ArticulatedCombatPose.Phase.ACTIVE})
							trial(context, world, audit, style(move), hand, armored, shell, false, phase, firstPerson);
					// A real funded, armored activation with the shell adapter disabled must retain both complete fallbacks.
					trial(context, world, audit, style(move), hand, true, true, true, ArticulatedCombatPose.Phase.ACTIVE, firstPerson);
				}
			}
		} finally {
			context.getInput().releaseKey(o -> o.keyAttack); context.getInput().releaseKey(o -> o.keyShift);
			HitStop.clear();
			for (int i = 0; i < properties.length; i++) restore(properties[i], previous[i]);
			context.runOnClient(mc -> {
				mc.options.setCameraType(saved.camera()); mc.options.mainHand().set(saved.hand()); mc.options.broadcastOptions();
				mc.getWindow().setWindowed(saved.width(), saved.height()); mc.getWindow().setFullscreen(saved.fullscreen());
				mc.options.guiScale().set(saved.gui()); mc.resizeGui(); mc.options.toggleCrouch().set(saved.toggleCrouch());
				if (mc.gui.hud.isHidden() != saved.hidden()) mc.gui.hud.toggle();
				mc.options.keyAttack.setDown(saved.attackDown()); mc.options.keyShift.setDown(saved.shiftDown());
			});
		}
	}

	private static MastersStyleRules.Style style(int move) {
		var style = MastersStyleRules.animation(move);
		check(style != null && opening(style.art()) && style.windup() == 6 && style.recovery() == 12,
			"Opening styles retain the existing six-tick tell and twelve-tick recovery");
		return style;
	}

	private static void trial(ClientGameTestContext context, TestSingleplayerContext world, Audit audit,
		MastersStyleRules.Style style, HumanoidArm hand, boolean armored, boolean shell, boolean disabled,
		ArticulatedCombatPose.Phase phase, boolean firstPerson) {
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(105); // Retain real individual rests, client prediction, and the prior first form's effects.
		System.setProperty(ArticulatedAuraShellRenderer.ENABLE_PROPERTY, Boolean.toString(!disabled));
		String name = "articulated_opening_style_" + style.art() + (firstPerson ? "_first_" : "_third_")
			+ hand.name().toLowerCase(Locale.ROOT) + (armored ? "_netherite" : "_skin")
			+ (shell ? "_funded_shell" : "_glow_shell_down") + (disabled ? "_adapter_disabled" : "")
			+ "_requested_" + phase.name().toLowerCase(Locale.ROOT);
		Mob[] target = new Mob[1];
		try {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				audit.reset(player); prepare(player, style, armored, shell);
				Mob foe = EntityTypes.HUSK.create(player.level(), EntitySpawnReason.COMMAND);
				check(foe != null, "Real string input has a living target");
				foe.addTag("wildercord.rolled"); foe.setNoAi(true); foe.setNoGravity(true);
				foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); foe.setHealth(200);
				foe.snapTo(.5, 100, 3.1, 180, 0); player.level().addFreshEntity(foe); target[0] = foe;
			});
			context.runOnClient(mc -> {
				mc.gui.setScreen(null); mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.options.mainHand().set(hand); mc.options.broadcastOptions();
				mc.player.setYRot(0); mc.player.setXRot(3);
			});
			context.waitTicks(15);
			int requests = context.computeOnClient(mc -> SwordStringsClient.counts()[0]);
			var samples = new ArrayDeque<String>();
			samples.add(context.computeOnClient(mc -> "before_input " + receipt(mc)));
			// Same supported controls as WildercordMastersArtsPresentationTest.captureStyle; no direct perform call or forged payload.
			for (int swing = 0; swing < 2; swing++) {
				context.getInput().pressKey(o -> o.keyAttack); context.waitTicks(2);
				world.getServer().runOnServer(server -> { target[0].snapTo(.5, 100, 3.1, 180, 0); target[0].setDeltaMovement(Vec3.ZERO); });
				context.waitTicks(12);
			}
			check(context.computeOnClient(mc -> SwordStringsClient.chain().size() == 2), "Two genuine attacks precede the low finishing swing");
			context.getInput().holdKey(o -> o.keyShift); context.waitTicks(2);
			context.getInput().pressKey(o -> o.keyAttack); context.getInput().releaseKey(o -> o.keyShift);
			context.runOnClient(mc -> mc.options.setCameraType(firstPerson ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_FRONT));
			try {
				context.waitFor(mc -> {
					if (samples.size() == 36) { String before = samples.removeFirst(); samples.removeFirst(); samples.addFirst(before); }
					samples.addLast(receipt(mc));
					var accepted = MastersArtsClient.timeline(mc.player); var state = state(mc);
					var frame = disabled ? state.getData(ArticulatedCombat.FRAME) : ArticulatedCombat.frame(state);
					return accepted != null && accepted.move() == style.animation() && frame != null && frame.move() == style.animation()
						&& frame.pose().phase() == phase && !state.isCrouching;
				}, 35);
			} catch (AssertionError failure) {
				System.out.println("ARTICULATED_OPENING_ADMISSION_TIMEOUT name=" + name + " boundedClientSamples=" + samples
					+ " serverSpends=" + audit.spends + " serverCompletions=" + audit.completions);
				throw failure;
			}
			MastersArts.Performed accepted = context.computeOnClient(mc -> {
				var timeline = MastersArtsClient.timeline(mc.player); var state = state(mc);
				var raw = state.getData(ArticulatedCombat.FRAME);
				check(timeline != null && timeline.entity() == mc.player.getId() && timeline.move() == style.animation()
					&& timeline.windup() == style.windup() && timeline.recovery() == style.recovery(), "Real server Performed packet owns the unchanged first-form windows");
				check(SwordStringsClient.counts()[0] == requests + 1 && SwordStringsClient.lastAsked().equals(style.art()), "Exactly one real reader request names the existing first form");
				check(raw != null && raw.move() == style.animation() && raw.activation() == timeline.startTick() && !raw.master()
					&& raw.pose().phase() == phase && raw.leftHanded() == (hand == HumanoidArm.LEFT), "Exact accepted activation, hand and requested phase own pre-capture state");
				check(state.mainArm == hand && mc.player.getMainArm() == hand && !mc.gui.hud.isHidden(), "Live selected hand and HUD are retained");
				check(timeline.yaw() == 0 && timeline.pitch() == 0 && mc.player.getYRot() == 0 && mc.player.getXRot() == 3,
					"The accepted horizontal first-form aim does not overwrite the free camera");
				check(AuraPresence.look(mc.player).shell() == shell && (state.getData(AuraShellLayer.SHELL_GLOW) != null) == shell,
					"Live synced shell state follows ordinary Glow/Form eligibility");
				assertBackend(mc, state, !disabled);
				if (armored) check(state.headEquipment.is(Items.NETHERITE_HELMET) && state.chestEquipment.is(Items.NETHERITE_CHESTPLATE)
					&& state.legsEquipment.is(Items.NETHERITE_LEGGINGS) && state.feetEquipment.is(Items.NETHERITE_BOOTS)
					&& state.chestEquipment.hasFoil(), "All four real enchanted netherite slots survive the first form");
				check(mc.getWindow().getWidth() == 1280 && mc.getWindow().getHeight() == 720 && mc.options.guiScale().get() == 3,
					"Opening-style screenshots use the declared native viewport");
				System.out.println("ARTICULATED_OPENING_SAMPLE name=" + name + " source=accepted_sword_string actualSkin=" + state.skin.model()
					+ " viewport=1280x720 guiScale=3 acceptedMove=" + timeline.move() + " activation=" + timeline.startTick()
					+ " windup=" + timeline.windup() + " recovery=" + timeline.recovery() + " requestedPhase=" + phase
					+ " preCapturePhase=" + raw.pose().phase() + " preCaptureAge=" + (mc.level.getGameTime() - timeline.startTick() + CAPTURE_TICK_DELTA)
					+ " shell=" + shell + " shellAdapter=" + !disabled + " supportedPresentation=" + !disabled
					+ " renderedPhase=unknown screenshotLatencyMayChangePhase=true imageReceiptBinding=not_in_scope nativePixelReviewRequired=true exactImpactPixelCoverage=unverified");
				return timeline;
			});
			// No server call between the observed pre-capture state and the screenshot request.
			context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDeltaTicks(CAPTURE_TICK_DELTA));
			context.waitTicks(style.windup() + style.recovery() + 2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(audit.spends.size() == 1 && audit.completions.size() == 1, "The real first form pays and performs exactly once");
				Spend spend = audit.spends.getFirst(); Completion done = audit.completions.getFirst();
				check(spend.art().equals(style.art()) && spend.tick() == accepted.startTick() && spend.paid() > 0
					&& Math.abs(spend.paid() - spend.expected()) < .0001 && !spend.backlash() && spend.committed(),
					"The accepted server windup immediately paid its actual SwordStrings price exactly once");
				check(SwordStrings.readyAt(player, style.art()) == accepted.startTick() + spend.rest(), "Ordinary individual string rest begins at the accepted server tick");
				// Scheduler runs at END_SERVER_TICK; a packet accepted across the level-tick boundary can differ by one tick.
				check(done.art().equals(style.art()) && done.acceptedAt() == accepted.startTick()
					&& done.performedAt() >= done.acceptedAt() + style.windup() - 1
					&& done.performedAt() <= done.acceptedAt() + style.windup() + 1 && done.marks().size() == 3
					&& SwordString.Token.LOW.fits(done.marks().getLast()), "The actual deferred performer retains its accepted input identity and low finishing mark");
				check(AuraArmour.up(player) == shell && !MastersArts.committed(player), "Real recovery expires while funded shell eligibility remains ordinary");
				System.out.println("ARTICULATED_OPENING_SERVER name=" + name + " spend=" + spend + " completion=" + done
					+ " performedDelta=" + (done.performedAt() - accepted.startTick()) + " schedulerTickTolerance=1 readyAt=" + SwordStrings.readyAt(player, style.art())
					+ " finalAura=" + Aura.aura(player) + " shellUp=" + AuraArmour.up(player));
			});
			context.runOnClient(mc -> {
				var idle = state(mc);
				check(MastersArtsClient.timeline(mc.player) == null && ArticulatedCombat.frame(idle) == null,
					"Accepted expiry removes the opening style and returns the world body to vanilla");
				check((ArticulatedCombat.viewFrame(idle) != null) == !disabled, "Stable first-person idle respects the live shell adapter fallback");
			});
		} finally {
			context.getInput().releaseKey(o -> o.keyShift); context.getInput().releaseKey(o -> o.keyAttack);
			world.getServer().runOnServer(server -> {
				if (target[0] != null) target[0].discard();
				MastersArts.cancel(server.getPlayerList().getPlayers().getFirst());
			});
			System.setProperty(ArticulatedAuraShellRenderer.ENABLE_PROPERTY, "true");
		}
	}

	private static void prepare(ServerPlayer player, MastersStyleRules.Style style, boolean armored, boolean shell) {
		player.setGameMode(GameType.SURVIVAL);
		player.teleportTo(player.level(), .5, 100, .5, Set.<Relative>of(), 0, 3, false); player.setDeltaMovement(Vec3.ZERO);
		player.removeAllEffects(); player.setHealth(player.getMaxHealth()); player.getFoodData().setFoodLevel(20);
		player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		for (EquipmentSlot slot : ARMOR) {
			ItemStack stack = !armored ? ItemStack.EMPTY : new ItemStack(switch (slot) {
				case HEAD -> Items.NETHERITE_HELMET;
				case CHEST -> Items.NETHERITE_CHESTPLATE;
				case LEGS -> Items.NETHERITE_LEGGINGS;
				default -> Items.NETHERITE_BOOTS;
			});
			if (armored) stack.enchant(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION), 4);
			player.setItemSlot(slot, stack);
		}
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		var art = AuraApi.string(style.art()).orElseThrow();
		check(art.stage() == AuraRules.GLOW && art.string().text().equals("swing swing low"), "Existing first forms retain their real learned stage and input string");
		int stage = shell ? AuraRules.FORM : AuraRules.GLOW;
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(ArtRules.art(style.art()).method(), stage,
			AuraRules.threshold(stage), shell ? 100 : AuraRules.capacity(stage), 0));
		player.removeAttached(AuraAttachments.STATE); player.inventoryMenu.broadcastChanges();
	}

	/** Explicitly synthetic width/socket/seam/fallback probes; none changes the live player's skin or produces screenshots. */
	private static void syntheticBridge(Minecraft mc) {
		for (boolean slim : new boolean[] {false, true}) for (boolean left : new boolean[] {false, true}) for (int move : MOVES) {
			var style = style(move);
			PlayerModel model = new PlayerModel(mc.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
			((ArticulatedModelAccess) model).wildercord$ownBody(slim);
			ArticulatedRig rig = ((ArticulatedModelAccess) model).wildercord$rig();
			AvatarRenderState state = state(mc);
			state.setData(ArticulatedCombat.KNOWN_LAYERS, true); state.setData(AuraShellLayer.SHELL_GLOW, null);
			state.walkAnimationSpeed = 0; state.isCrouching = false; state.showCape = false;
			state.mainArm = left ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
			state.rightHandItemStack = left ? ItemStack.EMPTY : new ItemStack(Items.DIAMOND_SWORD);
			state.leftHandItemStack = left ? new ItemStack(Items.DIAMOND_SWORD) : ItemStack.EMPTY;
			for (float age : new float[] {style.windup() / 2F, style.windup(), style.windup() + style.recovery() / 2F}) {
				var frame = new ArticulatedCombat.Frame(ArticulatedCombatPose.samplePlayer(move, age, style.windup(), style.recovery(), left), 999, move, false, left, 0, 0);
				state.setData(ArticulatedCombat.FRAME, frame); model.setupAnim(state);
				check(rig.root.visible && !model.body.visible && ArticulatedCombat.viewFrame(state) == frame,
					"Synthetic opening-style width/phase probe owns body and view together");
				PoseStack actual = held(model, state); PoseStack expected = new PoseStack(); rig.socket(state.mainArm, expected);
				check(hilt(actual).distance(expected.last().pose().transformPosition(new Vector3f())) < .00001F,
					"Synthetic wide/slim and left/right first-form blade hilt remains at its actual wrist socket");
			}
			state.setData(ArticulatedCombat.FRAME, null); model.setupAnim(state);
			Vector3f baseline = hilt(held(model, state));
			for (float edge : new float[] {.0001F, style.windup() + style.recovery() - .005F}) {
				var pose = ArticulatedCombatPose.samplePlayer(move, edge, style.windup(), style.recovery(), left);
				check(pose.weight() > 0, "Synthetic opening-style seam actually owns the segmented backend");
				state.setData(ArticulatedCombat.FRAME, new ArticulatedCombat.Frame(pose, 999, move, false, left, 0, 0)); model.setupAnim(state);
				PoseStack socket = new PoseStack(); rig.socket(state.mainArm, socket);
				check(rig.root.visible && socket.last().pose().transformPosition(new Vector3f()).distance(baseline) < .001F,
					"Synthetic opening-style entry/expiry socket converges to the true held-arm baseline");
			}
			var frame = new ArticulatedCombat.Frame(ArticulatedCombatPose.samplePlayer(move, style.windup(), style.windup(), style.recovery(), left), 999, move, false, left, 0, 0);
			state.setData(ArticulatedCombat.FRAME, frame);
			state.isCrouching = true; fallback(model, state, rig, "crouching"); state.isCrouching = false;
			state.setData(ArticulatedCombat.KNOWN_LAYERS, false); fallback(model, state, rig, "unknown layers"); state.setData(ArticulatedCombat.KNOWN_LAYERS, true);
			state.chestEquipment = new ItemStack(Items.DIAMOND_CHESTPLATE); fallback(model, state, rig, "unsupported armor"); state.chestEquipment = ItemStack.EMPTY;
			if (left) state.rightHandItemStack = new ItemStack(Items.SHIELD); else state.leftHandItemStack = new ItemStack(Items.SHIELD);
			fallback(model, state, rig, "offhand shield");
			if (left) state.rightHandItemStack = ItemStack.EMPTY; else state.leftHandItemStack = ItemStack.EMPTY;
			state.walkAnimationSpeed = .8F; model.setupAnim(state);
			check(!rig.root.visible && rigidVisible(model) && ArticulatedCombat.frame(state) == null, "Synthetic locomotion restores the entire world body");
			state.walkAnimationSpeed = 0; state.setData(ArticulatedCombat.FRAME, null); model.setupAnim(state);
			check(!rig.root.visible && rigidVisible(model), "Synthetic cancelled first form cannot retain the segmented body");
			System.out.println("ARTICULATED_OPENING_SYNTHETIC move=" + move + " syntheticWidth=" + (slim ? "slim" : "wide")
				+ " hand=" + state.mainArm + " coverage=phase_socket_entry_expiry_fallback liveSkinUnchanged=" + state.skin.model()
				+ " acceptedInput=false screenshotEvidence=false");
		}
	}

	private static PoseStack held(PlayerModel model, AvatarRenderState state) {
		PoseStack stack = new PoseStack(); model.translateToHand(state, state.mainArm, stack);
		stack.rotateDegrees(Axis.XP, -90); stack.rotateDegrees(Axis.YP, 180);
		stack.translate((state.mainArm == HumanoidArm.LEFT ? -1 : 1) / 16F, 2F / 16, -10F / 16); return stack;
	}
	private static Vector3f hilt(PoseStack stack) { return stack.last().pose().transformPosition(new Vector3f(0, -1.327F / 16, 1.439F / 16)); }
	private static void fallback(PlayerModel model, AvatarRenderState state, ArticulatedRig rig, String reason) {
		model.setupAnim(state);
		check(!rig.root.visible && rigidVisible(model) && ArticulatedCombat.frame(state) == null && ArticulatedCombat.viewFrame(state) == null,
			"Synthetic first-form " + reason + " restores the complete body and first-person fallback");
	}
	private static void assertBackend(Minecraft mc, AvatarRenderState state, boolean active) {
		var renderer = (net.minecraft.client.renderer.entity.player.AvatarRenderer<?>) mc.getEntityRenderDispatcher().getRenderer(mc.player);
		PlayerModel model = renderer.getModel(); model.setupAnim(state);
		var rig = ((ArticulatedModelAccess) model).wildercord$rig();
		check((ArticulatedCombat.frame(state) != null) == active && (ArticulatedCombat.viewFrame(state) != null) == active
			&& rig.root.visible == active && (active ? !model.body.visible && !model.head.visible && !model.leftArm.visible
				&& !model.rightArm.visible && !model.leftLeg.visible && !model.rightLeg.visible : rigidVisible(model)),
			"Live accepted first form and funded-shell negative each retain exactly one complete body and view backend");
	}
	private static boolean rigidVisible(PlayerModel model) {
		return model.body.visible && model.head.visible && model.rightArm.visible && model.leftArm.visible && model.rightLeg.visible && model.leftLeg.visible;
	}
	private static AvatarRenderState state(Minecraft mc) {
		return (AvatarRenderState) mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, CAPTURE_TICK_DELTA);
	}
	private static String receipt(Minecraft mc) {
		var state = state(mc); var frame = state.getData(ArticulatedCombat.FRAME);
		return "tick=" + mc.level.getGameTime() + " asked=" + SwordStringsClient.lastAsked() + " refused=" + SwordStringsClient.lastRefused()
			+ " counts=" + Arrays.toString(SwordStringsClient.counts()) + " chain=" + SwordStringsClient.chain().size()
			+ " timeline=" + MastersArtsClient.timeline(mc.player) + " raw=" + (frame == null ? "none" : frame.move() + ":" + frame.pose().phase())
			+ " body=" + (ArticulatedCombat.frame(state) != null) + " view=" + (ArticulatedCombat.viewFrame(state) != null)
			+ " aura=" + Aura.aura(mc.player) + " stage=" + Aura.stage(mc.player) + " method=" + Aura.data(mc.player).method()
			+ " shell=" + AuraPresence.look(mc.player).shell() + " crouching=" + state.isCrouching + " walk=" + state.walkAnimationSpeed
			+ " actualSkin=" + state.skin.model() + " armor=" + ArticulatedArmorRenderer.compatible(state);
	}
	private static void restore(String property, String value) { if (value == null) System.clearProperty(property); else System.setProperty(property, value); }
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
