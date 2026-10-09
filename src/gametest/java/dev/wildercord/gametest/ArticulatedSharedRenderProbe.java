package dev.wildercord.gametest;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.renderpearl.api.textures.GpuTexture;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.client.combat.ArticulatedAuraShellRenderer;
import dev.wildercord.client.render.AuraShellLayer;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.client.combat.ArticulatedCombat;
import dev.wildercord.client.combat.ArticulatedModelAccess;
import dev.wildercord.client.combat.ArticulatedRig;
import dev.wildercord.client.combat.ArticulatedViewModel;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.impl.client.gametest.screenshot.TestScreenshotCommonOptionsImpl;
import net.fabricmc.fabric.impl.client.gametest.screenshot.TestScreenshotOptionsImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/** Passive GameTest-only observer. It never updates, extracts, renders, samples or changes game state. */
public final class ArticulatedSharedRenderProbe {
	private static final String RUN = UUID.randomUUID().toString();
	private static final AtomicLong SEQUENCE = new AtomicLong();
	private static final Map<String, Expected> OPENINGS = new LinkedHashMap<>();
	private static final Map<Object, Token> TOKENS = new IdentityHashMap<>();
	private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();
	private static final ThreadLocal<CopyCall> COPYING = new ThreadLocal<>();
	private static final List<WeakIdentity> IDENTITIES = new ArrayList<>();
	private ArticulatedSharedRenderProbe() {}

	public static final class Token {
		final ArticulatedRenderReceipt.Session session;
		final AtomicLong diagnosticSequence = new AtomicLong();
		final Expected expected;
		Token(String name) {
			synchronized (OPENINGS) { expected = OPENINGS.remove(name); }
			session = new ArticulatedRenderReceipt.Session(new ArticulatedRenderReceipt.Identity(RUN,
				SEQUENCE.incrementAndGet(), name, ArticulatedRenderReceipt.requestedPhase(name)), System.getenv("WILDERCORD_SHARED_RECEIPT_NONCE"));
		}
	}
	public static final class Scope {
		final Token token;
		final Scope previous;
		final IdentityHashMap<AvatarRenderState, Body> bodies = new IdentityHashMap<>();
		final IdentityHashMap<ArticulatedViewModel.Frame, View> views = new IdentityHashMap<>();
		final List<String> extractions = new ArrayList<>();
		final IdentityHashMap<AvatarRenderState, Extracted> openingExtractions = new IdentityHashMap<>();
		int owner;
		boolean extracting, rendering;
		RenderTarget renderedTarget;
		GpuTexture renderedTexture;
		int renderWidth, renderHeight;
		ViewCall viewCall;
		DeferredCall deferredCall;
		Scope(Token token, Scope previous) { this.token = token; this.previous = previous; }
	}
	public record ViewCall(AvatarRenderState avatar, InteractionHand hand, float partial, ViewCall previous) {}
	public record DeferredCall(Model<?> model, Object state, DeferredCall previous) {}
	private record Expected(int owner, String ownerUuid, int move, long startTick) {}
	private record Extracted(int owner, String ownerUuid, long stateId, ArticulatedRenderReceipt.Palette palette) {}
	public record FallbackView(FirstPersonHandsAndItemsRenderer renderer, FirstPersonHandsAndItemsRenderState hands,
		AvatarRenderState avatar, ArticulatedRenderReceipt.Palette palette, Map<String, String> attributes) {}
	private record Body(Model<?> model, long stateId) {}
	private record View(ArticulatedViewModel model, AvatarRenderState avatar, ArticulatedRenderReceipt.Palette palette,
			long stateId, Map<String, String> appearance) {}
	private record CopyCall(Token token, RenderTarget target, Consumer<NativeImage> consumer, GpuTexture texture, long targetId, long textureId,
			int width, int height, CopyCall previous) {}
	private record WeakIdentity(WeakReference<Object> reference, long sequence) {}

	/** Bind the existing admission's exact accepted action; no renderer call or scheduling change. */
	public static void armOpening(String name, MastersArts.Performed accepted, Minecraft mc) {
		if (!ArticulatedRenderReceipt.opening(name)) throw new IllegalArgumentException("Not an opening capture");
		synchronized (OPENINGS) {
			if (OPENINGS.putIfAbsent(name, new Expected(mc.player.getId(), mc.player.getUUID().toString(), accepted.move(), accepted.startTick())) != null)
				throw new IllegalStateException("Opening capture already armed: " + name);
		}
	}
	public static void disarmOpening(String name) { synchronized (OPENINGS) { OPENINGS.remove(name); } }

	public static Token begin(TestScreenshotOptions options) {
		if (!(options instanceof TestScreenshotOptionsImpl actual) || !ArticulatedRenderReceipt.inScope(actual.name)) return null;
		Token token = new Token(actual.name);
		synchronized (TOKENS) {
			Token prior = TOKENS.putIfAbsent(options, token);
			if (prior != null) { prior.session.reject("concurrent_options_reuse"); token.session.reject("concurrent_options_reuse"); }
		}
		return token;
	}
	public static void unregister(TestScreenshotOptions options, Token token) {
		synchronized (TOKENS) { TOKENS.remove(options, token); }
	}
	public static Scope enter(TestScreenshotCommonOptionsImpl<?> options, Minecraft mc) {
		Token token;
		synchronized (TOKENS) { token = TOKENS.get(options); }
		if (token == null) {
			Scope previous = CURRENT.get();
			if (previous == null) return null;
			CURRENT.remove();
			return new Scope(null, previous); // Unmatched nested/comparison screenshots suspend every observer.
		}
		Scope scope = new Scope(token, CURRENT.get());
		CURRENT.set(scope);
		guard(token, () -> {
			if (scope.previous != null) token.session.reject("nested_screenshot_render");
			var s = token.session;
			String launchNonce = System.getenv("WILDERCORD_SHARED_RECEIPT_NONCE");
			s.observe("nativeLaunchNonce", launchNonce == null ? "unbound" : launchNonce);
			scope.owner = mc.player.getId();
			var timeline = MastersArtsClient.timeline(mc.player);
			s.observe("worldSession", identity(mc.level)); s.observe("dimension", mc.level.dimension().identifier());
			s.observe("ownerUuid", mc.player.getUUID()); s.observe("ownerId", scope.owner);
			s.observe("clientGameTick", mc.level.getGameTime()); s.observe("ownerTickCount", mc.player.tickCount);
			s.observe("acceptedMove", timeline == null ? -1 : timeline.move());
			if (ArticulatedRenderReceipt.opening(token.session.identity().trial())) {
				s.observe("openingArmed", token.expected != null);
				s.observe("acceptedEntity", timeline == null ? -1 : timeline.entity());
				s.observe("expectedBackend", ArticulatedRenderReceipt.fallback(token.session.identity().trial()) ? "full_fallback" : "segmented");
				s.observe("shellAdapterEnabled", ArticulatedAuraShellRenderer.enabled());
				s.observe("bannerClassification", "unclassified:not_observed");
				s.observe("nativeMainHandCalled", false); s.observe("viewSubmitCalled", false); s.observe("viewSubmitReturned", "not_called");
				s.observe("hudHidden", mc.gui.hud.isHidden()); s.observe("screen", mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getName());
				if (token.expected != null) {
					s.observe("expectedOwnerId", token.expected.owner()); s.observe("expectedOwnerUuid", token.expected.ownerUuid());
					s.observe("expectedMove", token.expected.move()); s.observe("expectedStartTick", token.expected.startTick());
				}
			}
			s.observe("acceptedStartTick", timeline == null ? Long.MIN_VALUE : timeline.startTick());
			s.observe("acceptedWindup", timeline == null ? -1 : timeline.windup());
			s.observe("acceptedRecovery", timeline == null ? -1 : timeline.recovery());
			s.observe("acceptedYaw", timeline == null ? "unknown" : timeline.yaw());
			s.observe("acceptedPitch", timeline == null ? "unknown" : timeline.pitch());
			s.observe("paused", mc.isPaused()); s.observe("frozen", mc.level.tickRateManager().isEntityFrozen(mc.player));
			s.observe("firstPerson", mc.options.getCameraType().isFirstPerson());
			s.observe("viewportWidth", mc.getWindow().getWidth()); s.observe("viewportHeight", mc.getWindow().getHeight());
			s.observe("guiWidth", mc.getWindow().getGuiScaledWidth()); s.observe("guiHeight", mc.getWindow().getGuiScaledHeight());
			s.observe("guiScale", mc.options.guiScale().get()); s.observe("optionsDeltaTicks", options.deltaTicks);
			s.observe("renderedAgeBasis", "unknown:submitted_palette_after_hitstop");
			if (timeline == null) s.reject("missing_accepted_activation");
		});
		return scope;
	}
	public static void leave(Scope scope) {
		if (scope == null) return;
		CURRENT.set(scope.previous);
		// Drop every mutable game object before the asynchronous readback can outlive this invocation.
		scope.bodies.clear(); scope.views.clear(); scope.openingExtractions.clear(); scope.viewCall = null; scope.deferredCall = null;
		scope.renderedTarget = null; scope.renderedTexture = null;
	}
	public static void extractBegin(DeltaTracker delta) {
		Scope scope = CURRENT.get(); if (scope == null) return;
		scope.extracting = true;
		guard(scope.token, () -> {
			var s = scope.token.session;
			s.observe("trackerGameDeltaTicks", delta.getGameTimeDeltaTicks());
			s.observe("trackerPartialPaused", delta.getGameTimeDeltaPartialTick(true));
			s.observe("trackerPartialUnpaused", delta.getGameTimeDeltaPartialTick(false));
			s.observe("trackerRealtimeDeltaTicks", delta.getRealtimeDeltaTicks());
		});
	}
	public static void extractEnd(boolean completed) {
		Scope scope = CURRENT.get(); if (scope == null) return;
		scope.extracting = false;
		guard(scope.token, () -> {
			if (completed) scope.token.session.extracted(SEQUENCE.incrementAndGet());
			scope.token.session.observe("entityExtractions", String.join(";", scope.extractions));
		});
	}
	public static void extracted(Entity entity, float partial, EntityRenderState state) {
		Scope scope = CURRENT.get();
		if (scope == null || !scope.extracting || entity.getId() != scope.owner || !(state instanceof AvatarRenderState avatar)) return;
		guard(scope.token, () -> {
			var raw = palette(avatar.getData(ArticulatedCombat.FRAME));
			scope.extractions.add("state=" + identity(avatar) + ",entityPartial=" + partial + ",postHitStop=" + raw);
			if (opening(scope)) scope.openingExtractions.put(avatar, new Extracted(entity.getId(), entity.getUUID().toString(), identity(avatar), raw));
		});
	}
	public static void renderBegin() {
		Scope scope = CURRENT.get(); if (scope == null) return;
		scope.rendering = true;
		guard(scope.token, () -> {
			scope.renderedTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
			scope.renderedTexture = scope.renderedTarget.getColorTexture();
			scope.renderWidth = scope.renderedTarget.width; scope.renderHeight = scope.renderedTarget.height;
			scope.token.session.observe("renderTargetGeneration", identity(scope.renderedTarget));
			scope.token.session.observe("renderTextureGeneration", identity(scope.renderedTexture));
		});
	}
	public static void renderEnd(boolean completed) {
		Scope scope = CURRENT.get(); if (scope == null) return;
		scope.rendering = false;
		guard(scope.token, () -> { if (completed) scope.token.session.rendered(SEQUENCE.incrementAndGet()); });
	}

	public static void bodySubmitted(Model<?> model, Object renderState) {
		Scope scope = rendering();
		if (scope == null || !(renderState instanceof AvatarRenderState avatar) || avatar.id != scope.owner) return;
		guard(scope.token, () -> {
			scope.bodies.put(avatar, new Body(model, identity(avatar)));
			scope.token.session.pass(pass("body_submit", avatar, model, palette(avatar.getData(ArticulatedCombat.FRAME)), appearance(avatar, model)));
		});
	}
	public static void bodyPalette(PlayerModel model, AvatarRenderState avatar) {
		Scope scope = rendering(); if (scope == null) return;
		Body submitted = scope.bodies.get(avatar); if (submitted == null || submitted.model() != model) return;
		guard(scope.token, () -> {
			Map<String, String> appearance = appearance(avatar, model);
			boolean visible = model instanceof ArticulatedModelAccess access && access.wildercord$bodyOwned()
				&& bodyVisible(access.wildercord$rig()) && !model.head.visible && !model.body.visible && !model.leftArm.visible && !model.rightArm.visible
				&& !model.leftLeg.visible && !model.rightLeg.visible;
			appearance.put("segmentedVisible", String.valueOf(visible));
			if (opening(scope)) {
				appearance.put("rigidVisible", String.valueOf(rigidVisible(model)));
				appearance.put("segmentedRootVisible", String.valueOf(model instanceof ArticulatedModelAccess access && access.wildercord$bodyOwned() && access.wildercord$rig().root.visible));
				appearance.put("fallbackFrameCompatible", String.valueOf(ArticulatedCombat.frame(avatar) != null));
				appearance.put("actualPaletteSha256", visible && model instanceof ArticulatedModelAccess access ? rigHash(access.wildercord$rig()) : rigidHash(model));
			}
			if (model instanceof ArticulatedModelAccess access && access.wildercord$bodyOwned()) appearance.put("rigPaletteSha256", rigHash(access.wildercord$rig()));
			String kind = deferred(scope, model, avatar) ? "body_deferred" : "body_attachment";
			scope.token.session.pass(pass(kind, avatar, model, palette(avatar.getData(ArticulatedCombat.FRAME)), appearance));
		});
	}
	public static ViewCall viewEnter(AvatarRenderState avatar, float partial, InteractionHand hand) {
		Scope scope = rendering(); if (scope == null || avatar.id != scope.owner) return null;
		if (opening(scope) && hand == InteractionHand.MAIN_HAND) guard(scope.token, () -> scope.token.session.observe("viewSubmitCalled", true));
		ViewCall call = new ViewCall(avatar, hand, partial, scope.viewCall); scope.viewCall = call; return call;
	}
	public static void viewLeave(ViewCall call, boolean submitted) {
		Scope scope = rendering(); if (scope == null || call == null) return;
		if (opening(scope) && call.hand() == InteractionHand.MAIN_HAND) guard(scope.token, () -> scope.token.session.observe("viewSubmitReturned", submitted));
		scope.viewCall = call.previous();
	}
	/** The real hand callback, including unsupported/fallback attempts; observations never count as passes. */
	public static void viewAttempt(AvatarRenderState avatar, FirstPersonHandsAndItemsRenderState hands, float partial, InteractionHand hand) {
		Scope scope = rendering();
		if (scope == null || !opening(scope) || avatar == null || avatar.id != scope.owner || hand != InteractionHand.MAIN_HAND) return;
		guard(scope.token, () -> {
			var s = scope.token.session; var combat = ArticulatedCombat.viewFrame(avatar);
			s.observe("nativeMainHandCalled", true);
			s.observe("viewHandAdmission", combat == null ? "UNSUPPORTED_POSE" : ArticulatedViewModel.handAdmission(combat, avatar, hands, partial));
			s.observe("viewOldMainHandHeight", hands.oldMainHandHeight); s.observe("viewMainHandHeight", hands.mainHandHeight);
			s.observe("viewInterpolatedHeight", net.minecraft.util.Mth.lerp(partial, hands.oldMainHandHeight, hands.mainHandHeight));
			s.observe("viewEquipKnown", hands instanceof dev.wildercord.client.MastersHandMotionState);
			s.observe("viewEquipping", hands instanceof dev.wildercord.client.MastersHandMotionState motion ? String.valueOf(motion.wildercord$mainHandEquipping()) : "unknown");
			s.observe("viewSameItem", ItemStack.isSameItemSameComponents(hands.mainHandItem, avatar.getMainHandItemStack()));
			s.observe("viewSwing", avatar.swingAnimation); s.observe("viewCrouching", avatar.isCrouching); s.observe("viewUsingItem", avatar.isUsingItem);
			var model = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(avatar).getModel();
			s.observe("viewModelSupported", model.getClass() == PlayerModel.class && model instanceof ArticulatedModelAccess access && access.wildercord$bodyOwned());
		});
	}
	public static void viewSubmitted(Model<?> model, Object renderState) {
		Scope scope = rendering();
		if (scope == null || scope.viewCall == null || scope.viewCall.hand() != InteractionHand.MAIN_HAND
			|| !(model instanceof ArticulatedViewModel view) || !(renderState instanceof ArticulatedViewModel.Frame frame)) return;
		guard(scope.token, () -> {
			AvatarRenderState avatar = scope.viewCall.avatar();
			var combat = avatar.getData(ArticulatedCombat.FRAME);
			var observed = combat == null ? null : new ArticulatedRenderReceipt.Palette(combat.activation(), combat.move(),
				frame.pose().phase().name(), frame.pose().weight(), combat.leftHanded(), combat.master(), localsHash(frame.pose()::local));
			Map<String, String> appearance = appearance(avatar, model);
			appearance.put("viewPartial", String.valueOf(scope.viewCall.partial()));
			appearance.put("hand", scope.viewCall.hand().name());
			scope.views.put(frame, new View(view, avatar, observed, identity(frame), Map.copyOf(appearance)));
			scope.token.session.pass(new ArticulatedRenderReceipt.Pass("view_submit", identity(frame), identity(model), avatar.id, observed, appearance));
		});
	}
	public static void viewPalette(ArticulatedViewModel model, ArticulatedViewModel.Frame frame) {
		Scope scope = rendering(); if (scope == null) return;
		View view = scope.views.get(frame); if (view == null || view.model() != model) return;
		guard(scope.token, () -> {
			Map<String, String> attributes = new LinkedHashMap<>(view.appearance());
			attributes.put("segmentedVisible", String.valueOf(viewVisible(model.rig())));
			attributes.put("rigPaletteSha256", rigHash(model.rig()));
			String kind = deferred(scope, model, frame) ? "view_deferred" : "view_attachment";
			scope.token.session.pass(new ArticulatedRenderReceipt.Pass(kind, view.stateId(), identity(model), view.avatar().id, view.palette(), attributes));
		});
	}
	public static DeferredCall deferredEnter(Model<?> model, Object state) {
		Scope scope = rendering(); if (scope == null) return null;
		DeferredCall call = new DeferredCall(model, state, scope.deferredCall); scope.deferredCall = call; return call;
	}
	public static void deferredLeave(DeferredCall call) {
		Scope scope = rendering(); if (scope != null && call != null) scope.deferredCall = call.previous();
	}
	private static boolean deferred(Scope scope, Model<?> model, Object state) {
		return scope.deferredCall != null && scope.deferredCall.model() == model && scope.deferredCall.state() == state;
	}
	public static void viewItem(ItemStackRenderState item) {
		Scope scope = rendering(); if (scope == null || scope.viewCall == null || scope.viewCall.hand() != InteractionHand.MAIN_HAND) return;
		guard(scope.token, () -> {
			AvatarRenderState avatar = scope.viewCall.avatar();
			List<View> views = scope.views.values().stream().filter(v -> v.avatar() == avatar).toList();
			if (views.size() != 1 || item != avatar.getMainHandItemState() || item.isEmpty()) { scope.token.session.reject("wrong_view_item_submission"); return; }
			View view = views.getFirst();
			Map<String, String> attributes = new LinkedHashMap<>(view.appearance()); attributes.put("itemStateIdentity", String.valueOf(identity(item)));
			scope.token.session.pass(new ArticulatedRenderReceipt.Pass("view_item", view.stateId(), identity(view.model()), avatar.id, view.palette(), attributes));
		});
	}

	/** The real vanilla sword path has no arm model. Preserve that fact instead of inventing a deferred pass. */
	public static FallbackView fallbackViewBegin(FirstPersonHandsAndItemsRenderer renderer, AvatarRenderState avatar,
			FirstPersonHandsAndItemsRenderState hands, InteractionHand hand, ItemStack stack) {
		Scope scope = rendering();
		if (scope == null || !opening(scope) || !ArticulatedRenderReceipt.fallback(scope.token.session.identity().trial())
			|| avatar == null || avatar.id != scope.owner || hand != InteractionHand.MAIN_HAND) return null;
		try {
			if (!ItemStack.isSameItemSameComponents(stack, avatar.getMainHandItemStack())
				|| !ItemStack.isSameItemSameComponents(stack, hands.mainHandItem) || hands.mainHandRenderState.isEmpty()) {
				scope.token.session.reject("wrong_native_fallback_item"); return null;
			}
			var model = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(avatar).getModel();
			Map<String, String> attributes = appearance(avatar, model);
			attributes.put("fallbackFrameCompatible", String.valueOf(ArticulatedCombat.viewFrame(avatar) != null));
			attributes.put("segmentedVisible", "false");
			attributes.put("itemStateIdentity", String.valueOf(identity(hands.mainHandRenderState)));
			attributes.put("nativeRenderer", renderer.getClass().getName());
			return new FallbackView(renderer, hands, avatar, palette(avatar.getData(ArticulatedCombat.FRAME)), Map.copyOf(attributes));
		} catch (Throwable failure) { scope.token.session.reject("fallback_observer_failed:" + failure); return null; }
	}
	public static void fallbackViewItem(FallbackView call, ItemStackRenderState item) {
		Scope scope = rendering(); if (scope == null || call == null) return;
		guard(scope.token, () -> {
			if (item != call.hands().mainHandRenderState || item.isEmpty()) { scope.token.session.reject("wrong_native_fallback_submission"); return; }
			scope.token.session.pass(new ArticulatedRenderReceipt.Pass("view_fallback_item", identity(call.hands()), identity(call.renderer()),
				call.avatar().id, call.palette(), call.attributes()));
		});
	}
	public static void fallbackViewEnd(FallbackView call, boolean completed) {
		Scope scope = rendering(); if (scope == null || call == null) return;
		guard(scope.token, () -> {
			if (!completed) { scope.token.session.reject("incomplete_native_fallback"); return; }
			scope.token.session.pass(new ArticulatedRenderReceipt.Pass("view_fallback_submit", identity(call.hands()), identity(call.renderer()),
				call.avatar().id, call.palette(), call.attributes()));
		});
	}

	/** Only the exact Fabric call site arms COPYING, after GameRenderer's thumbnail work has finished. */
	public static void screenshot(RenderTarget target, Consumer<NativeImage> originalConsumer,
			java.util.function.BiConsumer<RenderTarget, Consumer<NativeImage>> originalCall) {
		Scope scope = CURRENT.get();
		if (scope == null) { originalCall.accept(target, originalConsumer); return; }
		Token token = scope.token;
		CopyCall previous = COPYING.get();
		Consumer<NativeImage> wrapped = ArticulatedRenderReceipt.observeConsumer(token.session, image -> {
			token.session.received(ArticulatedRenderReceipt.pixels(image.getWidth(), image.getHeight(), image::getPixel));
			if (token.session.ended()) persist(token, "late");
		}, originalConsumer);
		guard(token, () -> {
			if (target != scope.renderedTarget || target.getColorTexture() != scope.renderedTexture
				|| target.width != scope.renderWidth || target.height != scope.renderHeight) token.session.reject("render_target_changed_after_render");
			COPYING.set(new CopyCall(token, target, wrapped, target.getColorTexture(), identity(target),
				identity(target.getColorTexture()), target.width, target.height, previous));
		});
		try { originalCall.accept(target, wrapped); }
		finally { COPYING.set(previous); }
	}
	public static void copyEnqueued(GpuTexture texture, int mip, RenderTarget target, Consumer<NativeImage> consumer) {
		CopyCall call = COPYING.get();
		if (call == null || target != call.target() || consumer != call.consumer()) return;
		guard(call.token(), () -> {
			if (texture != call.texture() || call.target().getColorTexture() != texture
				|| call.target().width != call.width() || call.target().height != call.height()) call.token().session.reject("render_target_changed_before_copy");
			call.token().session.enqueue(SEQUENCE.incrementAndGet(), new ArticulatedRenderReceipt.Target(
				call.targetId(), identity(texture), call.width(), call.height(), mip));
		});
	}
	public static void success(Token token, Path returnedPath) {
		guard(token, () -> {
			try { token.session.written(ArticulatedRenderReceipt.bind(returnedPath, FabricLoader.getInstance().getGameDir())); }
			catch (Exception failure) { throw new IllegalStateException("Unable to bind returned PNG", failure); }
		});
		token.session.finish(); persist(token, "receipt");
		var report = token.session.report();
		System.out.println((ArticulatedRenderReceipt.opening(report.identity().trial()) ? "ARTICULATED_OPENING_RENDER_RECEIPT name=" : "ARTICULATED_SHARED_RENDER_RECEIPT name=") + report.identity().trial() + " requestedPhase="
			+ report.identity().requestedPhase() + " renderedPhase=" + report.renderedPhase() + " verified=" + report.verified()
			+ " requestedPhaseObserved=" + report.requestedPhaseObserved() + " nativePixelReviewRequired=true exactImpactPixelCoverage=unverified");
		if (!report.verified()) throw new AssertionError("Passive shared-player screenshot receipt rejected: " + report.failures());
	}
	public static void failed(Token token, Throwable failure) {
		if (token == null) return;
		try { token.session.failed(failure); persist(token, "failed"); }
		catch (Throwable observerFailure) { System.err.println("ARTICULATED_SHARED_RECEIPT_FAILURE " + observerFailure); }
	}
	private static void persist(Token token, String suffix) {
		try {
			Path directory = FabricLoader.getInstance().getGameDir().resolve(ArticulatedRenderReceipt.opening(token.session.identity().trial()) ? "screenshots/articulated-opening-receipts" : "screenshots/articulated-shared-receipts").resolve(RUN);
			Files.createDirectories(directory);
			Path receipt = directory.resolve(String.format(java.util.Locale.ROOT, "%06d-%s-%d.json", token.session.identity().captureSequence(), suffix, token.diagnosticSequence.incrementAndGet()));
			Files.writeString(receipt, new GsonBuilder().setPrettyPrinting().create().toJson(token.session.report()) + "\n", StandardOpenOption.CREATE_NEW);
			System.out.println("ARTICULATED_SHARED_RECEIPT_FILE path=" + receipt);
		} catch (Throwable failure) {
			token.session.reject("receipt_persistence_failed:" + failure);
			System.err.println("ARTICULATED_SHARED_RECEIPT_FAILURE name=" + token.session.identity().trial() + " failure=" + failure);
		}
	}
	private static boolean opening(Scope scope) { return ArticulatedRenderReceipt.opening(scope.token.session.identity().trial()); }
	private static Scope rendering() { Scope scope = CURRENT.get(); return scope != null && scope.rendering ? scope : null; }
	private static void guard(Token token, Runnable observer) {
		try { observer.run(); }
		catch (Throwable failure) { token.session.reject("observer_failed:" + failure); }
	}
	private static synchronized long identity(Object value) {
		if (value == null) return 0;
		IDENTITIES.removeIf(entry -> entry.reference().get() == null);
		for (WeakIdentity entry : IDENTITIES) if (entry.reference().get() == value) return entry.sequence();
		long id = SEQUENCE.incrementAndGet(); IDENTITIES.add(new WeakIdentity(new WeakReference<>(value), id)); return id;
	}
	private static ArticulatedRenderReceipt.Pass pass(String kind, AvatarRenderState avatar, Model<?> model,
			ArticulatedRenderReceipt.Palette palette, Map<String, String> attributes) {
		return new ArticulatedRenderReceipt.Pass(kind, identity(avatar), identity(model), avatar.id, palette, attributes);
	}
	private static ArticulatedRenderReceipt.Palette palette(ArticulatedCombat.Frame frame) {
		return frame == null ? null : new ArticulatedRenderReceipt.Palette(frame.activation(), frame.move(), frame.pose().phase().name(),
			frame.pose().weight(), frame.leftHanded(), frame.master(), localsHash(frame.pose()::local));
	}
	private static String localsHash(Function<ArticulatedCombatPose.Joint, ArticulatedCombatPose.Transform> local) {
		StringBuilder values = new StringBuilder("joint-local-floathex-v1\n");
		for (var joint : ArticulatedCombatPose.Joint.values()) {
			var t = local.apply(joint);
			values.append(joint).append(':'); append(values, t.x(), t.y(), t.z(), t.rotation().x(), t.rotation().y(), t.rotation().z());
		}
		return ArticulatedRenderReceipt.sha256(values.toString().getBytes(StandardCharsets.US_ASCII));
	}
	private static String rigHash(ArticulatedRig rig) {
		StringBuilder values = new StringBuilder("rig-local-floathex-visible-skipdraw-v1\n");
		for (var joint : ArticulatedCombatPose.Joint.values()) {
			var part = rig.part(joint); values.append(joint).append(':').append(part.visible).append(':').append(part.skipDraw).append(':');
			append(values, part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale);
		}
		return ArticulatedRenderReceipt.sha256(values.toString().getBytes(StandardCharsets.US_ASCII));
	}
	private static boolean rigidVisible(PlayerModel model) {
		return model.root().visible && List.of(model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg)
			.stream().allMatch(part -> part.visible && !part.skipDraw);
	}
	private static String rigidHash(PlayerModel model) {
		StringBuilder values = new StringBuilder("rigid-local-floathex-visible-skipdraw-v1\n");
		for (var part : List.of(model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg)) {
			values.append(part.visible).append(':').append(part.skipDraw).append(':');
			append(values, part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale);
		}
		return ArticulatedRenderReceipt.sha256(values.toString().getBytes(StandardCharsets.US_ASCII));
	}
	private static boolean bodyVisible(ArticulatedRig rig) {
		if (!rig.root.visible) return false;
		for (var joint : ArticulatedCombatPose.Joint.values()) if (!rig.part(joint).visible || rig.part(joint).skipDraw) return false;
		return true;
	}
	private static boolean viewVisible(ArticulatedRig rig) {
		if (!rig.root.visible) return false;
		for (var joint : new ArticulatedCombatPose.Joint[] {ArticulatedCombatPose.Joint.PELVIS, ArticulatedCombatPose.Joint.SPINE,
				ArticulatedCombatPose.Joint.CHEST, ArticulatedCombatPose.Joint.LEFT_SHOULDER, ArticulatedCombatPose.Joint.RIGHT_SHOULDER})
			if (!rig.part(joint).visible) return false;
		for (var joint : new ArticulatedCombatPose.Joint[] {ArticulatedCombatPose.Joint.LEFT_UPPER_ARM, ArticulatedCombatPose.Joint.RIGHT_UPPER_ARM,
				ArticulatedCombatPose.Joint.LEFT_FOREARM, ArticulatedCombatPose.Joint.RIGHT_FOREARM, ArticulatedCombatPose.Joint.LEFT_HAND, ArticulatedCombatPose.Joint.RIGHT_HAND})
			if (!rig.part(joint).visible || rig.part(joint).skipDraw) return false;
		return true;
	}
	private static void append(StringBuilder out, float... values) { for (float value : values) out.append(Float.toHexString(value)).append(','); out.append('\n'); }
	private static Map<String, String> appearance(AvatarRenderState avatar, Model<?> model) {
		Map<String, String> out = new LinkedHashMap<>();
		out.put("skinModel", avatar.skin == null ? "unknown" : avatar.skin.model().toString());
		out.put("skinTexture", avatar.skin == null ? "unknown" : avatar.skin.body().texturePath().toString());
		out.put("rigWidth", model instanceof ArticulatedViewModel view ? (view.rig().slim() ? "slim" : "wide")
			: model instanceof ArticulatedModelAccess access && access.wildercord$bodyOwned() ? (access.wildercord$rig().slim() ? "slim" : "wide") : "unknown");
		Scope scope = rendering();
		if (scope != null && opening(scope)) {
			Extracted extracted = scope.openingExtractions.get(avatar);
			out.put("avatarStateIdentity", String.valueOf(identity(avatar)));
			out.put("extractedStateIdentity", extracted == null ? "0" : String.valueOf(extracted.stateId()));
			out.put("ownerUuid", extracted == null ? "unknown" : extracted.ownerUuid());
			out.put("postHitStopExtractionMatched", String.valueOf(extracted != null && extracted.owner() == avatar.id
				&& java.util.Objects.equals(extracted.palette(), palette(avatar.getData(ArticulatedCombat.FRAME)))));
			if (extracted != null && extracted.palette() != null) {
				out.put("rawAcceptedPhase", extracted.palette().phase()); out.put("rawActivation", String.valueOf(extracted.palette().activation()));
				out.put("rawMove", String.valueOf(extracted.palette().move())); out.put("rawLeftHanded", String.valueOf(extracted.palette().leftHanded()));
				out.put("rawPaletteSha256", extracted.palette().localsSha256());
			}
			out.put("shellGlowPresent", String.valueOf(avatar.getData(AuraShellLayer.SHELL_GLOW) != null));
			out.put("shellAdapterEnabled", String.valueOf(ArticulatedAuraShellRenderer.enabled()));
		}
		out.put("mainArm", avatar.mainArm.name());
		out.put("mainHand", item(avatar.getMainHandItemStack())); out.put("head", item(avatar.headEquipment));
		out.put("chest", item(avatar.chestEquipment)); out.put("legs", item(avatar.legsEquipment)); out.put("feet", item(avatar.feetEquipment));
		return out;
	}
	private static String item(ItemStack stack) { return stack == null || stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()) + ":foil=" + stack.hasFoil(); }
}
