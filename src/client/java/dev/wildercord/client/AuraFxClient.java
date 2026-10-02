package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.SwordString;
import dev.wildercord.client.fx.AuraBurst;
import dev.wildercord.client.fx.AuraTrail;
import dev.wildercord.client.fx.AuraWisp;
import dev.wildercord.client.fx.Glimmer;
import dev.wildercord.client.fx.HitStop;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.client.fx.ScreenEffects;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Aura's feel on the client (the server's side is {@link AuraFx}): every trail, impact, banner, burst and flare it hears of is
 * drawn here as this player sees it, and the body's aura is kept moving.
 * <ul>
 * <li><b>Your own swings</b> draw their trail at once, as you swing ({@code mixin.MinecraftStringsMixin}), cut the way the swing
 * was ({@link AuraFxRules#stroke}), each cut of a run back the other way, and the method's blade sounds for you. Only a blade with
 * aura enough to coat a blow, and never digging. A swing that completes a sword string cuts brighter.</li>
 * <li><b>Others' swings</b> and every technique's trail come from the server.</li>
 * <li><b>Impacts</b> flash where they land; for the striker and a struck player the moment holds ({@link HitStop}) and the view is
 * nudged ({@link ScreenEffects#nudge}).</li>
 * <li><b>The body's aura</b>: {@link #bodyIntensity} for the layer that draws it ({@code render.AuraBodyLayer}), and its wisps,
 * glints and embers here. In your own first-person view your body isn't drawn: only a faint glow at the bottom edge of the
 * screen tells of your aura flaring.</li>
 * </ul>
 * Every part follows the player's visual settings ({@link MagicQuality}: trails, body aura, impact, banners, camera motion,
 * reduced flash, and others' effects on minimal).
 */
public final class AuraFxClient {
	private AuraFxClient() {}

	/** A surge of an entity's body aura: when it began (game time), how long it lasts and how strong. */
	private record Surge(long start, int ticks, float strength) {}

	private static final Map<Integer, Surge> SURGES = new HashMap<>();
	private static final Glimmer.Budget BODY = new Glimmer.Budget(260);

	/** What the player's swing was as it began (before vanilla's attack changes it), for its trail, and whether it sweeps with Flow. */
	private record Pending(int marks, boolean block, boolean sweep, long at) {}

	private static Pending pending;
	private static long lastSwing = Long.MIN_VALUE / 4;
	private static boolean lastMirrored;
	/** When this client last drew its own Flow sweep (game time): the server's word of the same sweep isn't drawn again. */
	private static long lastOwnSweep = Long.MIN_VALUE / 4;

	/** What the game tests read: trails drawn (all, and your own), impacts, bursts, flares, body motes and wisps, whisper frames. */
	private static int trails;
	private static int ownTrails;
	private static int impacts;
	private static int bursts;
	private static int flares;
	private static int bodyMotes;
	private static int whispers;
	/** Pieces of your own arts' spectacle drawn (in third person) and left out (through your own eyes). */
	private static int shown;
	private static int spared;
	private static String lastOwnStroke = "";

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(AuraFx.Trail.TYPE, (payload, context) -> trail(payload));
		ClientPlayNetworking.registerGlobalReceiver(AuraFx.Impact.TYPE, (payload, context) -> impact(payload));
		ClientPlayNetworking.registerGlobalReceiver(AuraFx.BurstCue.TYPE, (payload, context) -> burst(payload));
		ClientPlayNetworking.registerGlobalReceiver(AuraFx.Flare.TYPE, (payload, context) -> flare(payload));
		ClientPlayNetworking.registerGlobalReceiver(AuraFx.Banner.TYPE, (payload, context) -> AuraBanners.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(AuraFx.Shown.TYPE, (payload, context) -> shown(payload));
		ClientTickEvents.END_CLIENT_TICK.register(AuraFxClient::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
		HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, Wildercord.id("aura_whisper"), AuraFxClient::whisper);
		AuraBanners.init();
		StanceHud.init();
	}

	private static void reset() {
		SURGES.clear();
		pending = null;
		lastSwing = Long.MIN_VALUE / 4;
		lastOwnSweep = Long.MIN_VALUE / 4;
		HitStop.clear();
		AuraBanners.reset();
	}

	// ------------------------------------------------------------------ your own swings

	/** As an attack begins: what the swing is, before vanilla's swing resets the blade and ends a sprint. */
	public static void attackBegins(Minecraft mc) {
		pending = null;
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || player.isSpectator() || !Aura.coated(player)) {
			return;
		}
		HitResult hit = mc.hitResult;
		boolean block = hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK && !mc.level.getBlockState(b.getBlockPos()).isAir()
			&& !(hit instanceof EntityHitResult);
		boolean atCreature = hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity living && living.isAlive();
		float strength = player.getAttackStrengthScale(0.5F);
		// Flow's sweep, as the server will judge it: drawn here at once, for the sweeper.
		boolean sweep = AuraFxRules.sweeps(Aura.stage(player), true, atCreature, strength, player.onGround(), player.isSprinting(),
			player.getKnownMovement().horizontalDistance(), player.getSpeed());
		int marks = SwordString.Token.SWING.bit();
		if (strength >= AuraRules.FULL_SWING - 1.0E-4) {
			marks |= SwordString.Token.FULL.bit();
		}
		if (player.isShiftKeyDown()) {
			marks |= SwordString.Token.LOW.bit();
		}
		if (SwordStringsClient.leaping(player)) {
			marks |= SwordString.Token.LEAP.bit();
		}
		if (player.isSprinting()) {
			marks |= SwordString.Token.RUN.bit();
		}
		marks |= SwordStringsClient.cueMarks(mc.level.getGameTime());
		pending = new Pending(marks, block, sweep, mc.level.getGameTime());
	}

	/** A swing just went: its trail, at once, and the method's blade cutting the air. */
	public static void swung(Minecraft mc, boolean thrust) {
		Pending p = pending;
		pending = null;
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (p == null || player == null || level == null || (p.block() && !thrust)) {
			return;
		}
		long now = level.getGameTime();
		boolean mirror = AuraFxRules.mirrored(lastSwing, lastMirrored, now);
		lastSwing = now;
		lastMirrored = mirror;
		AuraFxRules.Stroke stroke = p.sweep() && !thrust ? AuraFxRules.Stroke.SWEEP : AuraFxRules.stroke(p.marks(), thrust);
		if (stroke == AuraFxRules.Stroke.SWEEP) {
			lastOwnSweep = now;
		}
		boolean full = SwordString.Token.FULL.fits(p.marks());
		// A swing that played a sword string to its end cuts brighter.
		boolean completed = SwordStringsClient.completedAt() == now;
		float power = (stroke == AuraFxRules.Stroke.SWEEP ? 1.35F : full ? 1.15F : 0.9F) * (completed ? 1.3F : 1.0F);
		if (MagicQuality.bladeTrails != MagicQuality.Trails.OFF) {
			spawn(level, player, stroke, mirror, Aura.color(player), Aura.stage(player), power, MagicQuality.bladeTrails == MagicQuality.Trails.SUBTLE);
			ownTrails++;
			lastOwnStroke = stroke.name().toLowerCase(java.util.Locale.ROOT);
		}
		float pitch = switch (stroke) {
			case SWEEP -> 0.9F;
			case FALLING -> 0.85F;
			case THRUST -> 1.08F;
			default -> full ? 1.0F : 1.12F;
		};
		play(mc, player, AuraFx.family(Aura.data(player).method()).swing(), full ? 0.6F : 0.45F, pitch);
	}

	/** A trail round {@code anchor}, and for a cross its mirror two ticks behind. */
	private static void spawn(ClientLevel level, LivingEntity anchor, AuraFxRules.Stroke stroke, boolean mirror, int color, int stage, float power,
			boolean plain) {
		Minecraft mc = Minecraft.getInstance();
		mc.particleEngine.add(new AuraTrail(level, anchor, stroke, mirror, color, stage, power, 0, plain));
		if (stroke == AuraFxRules.Stroke.CROSS) {
			mc.particleEngine.add(new AuraTrail(level, anchor, stroke, !mirror, color, stage, power, 2, plain));
		}
		trails++;
	}

	private static void play(Minecraft mc, Player player, String kit, float volume, float pitch) {
		SoundEvent event = WildercordSounds.kit(kit);
		if (event == null || mc.level == null) {
			return;
		}
		float jitter = 1.0F + (player.getRandom().nextFloat() - 0.5F) * 0.06F;
		mc.level.playLocalSound(player.getX(), player.getEyeY() - 0.3, player.getZ(), event, SoundSource.PLAYERS, volume, pitch * jitter, false);
	}

	// ------------------------------------------------------------------ what the server tells

	static void trail(AuraFx.Trail payload) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || MagicQuality.bladeTrails == MagicQuality.Trails.OFF) {
			return;
		}
		if (!(level.getEntity(payload.entity()) instanceof LivingEntity anchor) || anchor.isInvisible()) {
			return;
		}
		boolean ordinary = (payload.flags() & AuraFx.ORDINARY) != 0;
		boolean mine = anchor == mc.player;
		if (ordinary && (mine || MagicQuality.bladeTrails == MagicQuality.Trails.SUBTLE || MagicQuality.others == MagicQuality.Level.MINIMAL)) {
			// Your own ordinary swings you drew yourself; on low settings others' ordinary swings are left out (never a technique's).
			return;
		}
		if (mc.player != null && anchor.distanceToSqr(mc.player) > AuraFxRules.SEEN * AuraFxRules.SEEN) {
			return;
		}
		if (mine && payload.stroke() == AuraFxRules.Stroke.SWEEP.ordinal() && level.getGameTime() - lastOwnSweep <= AuraFxRules.SWEEP_ECHO) {
			// The server's word of the sweep this client already drew as it swung.
			return;
		}
		boolean plain = MagicQuality.bladeTrails == MagicQuality.Trails.SUBTLE || !mine && MagicQuality.others == MagicQuality.Level.MINIMAL;
		spawn(level, anchor, AuraFxRules.Stroke.of(payload.stroke()), payload.mirror(), payload.color(), payload.stage(), payload.power(), plain);
		if (mine) {
			ownTrails++;
			lastOwnStroke = AuraFxRules.Stroke.of(payload.stroke()).name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	static void impact(AuraFx.Impact payload) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		if (level == null || player == null) {
			return;
		}
		impacts++;
		AuraFxRules.Weight weight = AuraFxRules.Weight.of(payload.weight());
		Entity attacker = payload.attacker() < 0 ? null : level.getEntity(payload.attacker());
		Entity target = level.getEntity(payload.target());
		boolean striker = attacker == player;
		boolean struck = target == player;
		boolean firstPerson = mc.getCameraEntity() == player && mc.options.getCameraType().isFirstPerson();
		// The flash. Seen down your own blade in first person it sits where you're looking, so there it's only a small, quick flash
		// (no sparks, glint or ring across your view: the hit-stop and the sound carry it); on a struck player's own screen, none.
		float strength = MagicQuality.impact.flash * (MagicQuality.reducedFlash ? 0.5F : 1.0F);
		boolean own = striker && firstPerson;
		float size = AuraFxRules.flash(weight, payload.stage()) * (own ? 0.5F : 1.0F);
		if (!(struck && firstPerson)) {
			int style = own ? AuraFx.Burst.FLASH : switch (weight) {
				case LIGHT -> AuraFx.Burst.FLASH | AuraFx.Burst.SPARKS;
				case FULL -> AuraFx.Burst.FLASH | AuraFx.Burst.SPARKS | AuraFx.Burst.STAR;
				case HEAVY -> AuraFx.Burst.FLASH | AuraFx.Burst.STAR | AuraFx.Burst.RING | AuraFx.Burst.SPARKS;
				case GRAND -> AuraFx.Burst.FLASH | AuraFx.Burst.STAR | AuraFx.Burst.RING | AuraFx.Burst.ECHO | AuraFx.Burst.SPARKS;
			};
			mc.particleEngine.add(new AuraBurst(level, payload.at(), Vec3.ZERO, payload.color(), size, style, false, own ? strength * 0.8F : strength));
		}
		if (striker || struck) {
			// The moment held for both, and the view nudged: down a hair into the cut for the striker, back from it for the struck.
			HitStop.hold(AuraFxRules.hitStop(weight, MagicQuality.impact.hitStop), payload.attacker(), payload.target());
			float nudge = weight.nudge * (float) MagicQuality.impact.hitStop;
			if (striker) {
				ScreenEffects.nudge(nudge * 0.35F * (player.getMainArm() == HumanoidArm.RIGHT ? -1 : 1), nudge, AuraFxRules.NUDGE_MILLIS);
			} else {
				ScreenEffects.nudge(nudge * 0.5F * side(player, attacker), -nudge * 1.2F, AuraFxRules.NUDGE_MILLIS);
			}
		}
	}

	/** Which way the view turns away from a blow by {@code attacker}: -1 or 1 (0 without one). */
	private static float side(Player player, Entity attacker) {
		if (attacker == null) {
			return 0;
		}
		Vec3 look = player.getLookAngle();
		Vec3 to = attacker.position().subtract(player.position());
		double cross = look.x * to.z - look.z * to.x;
		return cross > 0 ? -1 : 1;
	}

	static void burst(AuraFx.BurstCue payload) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			return;
		}
		bursts++;
		boolean whisper = AuraBurst.nearOwnEyes(mc, payload.owner(), payload.at());
		if (whisper) {
			whispers++;
		}
		float strength = MagicQuality.reducedFlash ? 0.5F : 1.0F;
		mc.particleEngine.add(new AuraBurst(level, payload.at(), payload.facing(), payload.color(), payload.size(), payload.style(), whisper, strength));
	}

	/**
	 * A piece of your own art's spectacle (a whirlwind round you, a lance down your line of sight): drawn as everyone else sees it
	 * while you watch yourself in third person, and left out through your own eyes, where it would fill the view.
	 */
	static void shown(AuraFx.Shown payload) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			return;
		}
		if (mc.getCameraEntity() == mc.player && mc.options.getCameraType().isFirstPerson()) {
			spared++;
			return;
		}
		shown++;
		Vec3 at = payload.at();
		level.addParticle(payload.particle(), true, true, at.x, at.y, at.z, 0, 0, 0);
	}

	static void flare(AuraFx.Flare payload) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		flares++;
		SURGES.put(payload.entity(), new Surge(mc.level.getGameTime(), payload.ticks(), payload.strength()));
	}

	// ------------------------------------------------------------------ the body's aura

	/** What is left of {@code entity}'s surge at {@code time} (game time and partial tick). */
	public static float surge(int entity, float time) {
		Surge s = SURGES.get(entity);
		return s == null ? 0 : AuraFxRules.surgeLeft(s.strength(), time - s.start(), s.ticks());
	}

	/** How strongly {@code player}'s body aura shows at {@code time}: calm, flaring in a fight, surging, faint while low. */
	public static float bodyIntensity(Player player, float time) {
		AuraAttachments.Look look = Aura.look(player);
		if (look.stage() <= AuraRules.NONE || MagicQuality.bodyAura == MagicQuality.BodyAura.OFF) {
			return 0;
		}
		if (MagicQuality.bodyAura == MagicQuality.BodyAura.CALM) {
			return AuraFxRules.intensity(look.lit(), false, 0);
		}
		AuraPresence.Look presence = AuraPresence.look(player);
		boolean fighting = presence.fighting(player.level().getGameTime());
		// Momentum burns it brighter at each tier, blazing at the peak.
		return AuraFxRules.intensity(look.lit(), fighting, surge(player.getId(), time)) + AuraFxRules.momentumGlow(presence.momentum(), look.lit());
	}

	static void tick(Minecraft mc) {
		AuraTrail.tickBudget();
		BODY.tick();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			return;
		}
		if (mc.isPaused()) {
			return;
		}
		long now = level.getGameTime();
		SURGES.entrySet().removeIf(e -> now - e.getValue().start() > e.getValue().ticks() || now < e.getValue().start());
		AuraBanners.tick(now);
		if (MagicQuality.bodyAura != MagicQuality.BodyAura.FULL) {
			return;
		}
		Vec3 camera = mc.gameRenderer.mainCamera().position();
		for (AbstractClientPlayer player : level.players()) {
			if (player.isInvisible() || player.isSpectator() || player.position().distanceToSqr(camera) > 40 * 40) {
				continue;
			}
			if (player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) {
				// Your own, through your own eyes: only the whisper at the bottom of the screen.
				continue;
			}
			if (player != mc.player && MagicQuality.others == MagicQuality.Level.MINIMAL) {
				continue;
			}
			AuraAttachments.Look look = Aura.look(player);
			if (look.stage() <= AuraRules.NONE) {
				continue;
			}
			motes(mc, level, player, look, bodyIntensity(player, now));
		}
	}

	/** The body's moving light: glints at Glow, wisps from the shoulders and blade from Flow, embers off the corona at Sovereign. */
	private static void motes(Minecraft mc, ClientLevel level, AbstractClientPlayer player, AuraAttachments.Look look, float k) {
		RandomSource random = player.getRandom();
		int stage = look.stage();
		int color = look.color();
		int hot = mix(color, 0xFFFFFF, 0.5F);
		double yaw = Math.toRadians(player.yBodyRot);
		double fx = -Math.sin(yaw);
		double fz = Math.cos(yaw);
		double rx = -fz;
		double rz = fx;
		double shoulder = player.isCrouching() ? 1.12 : 1.36;
		// Glints: a speck rising off the body now and then, more in a fight.
		if (BODY.hasRoom() && random.nextFloat() < 0.03F + 0.08F * k * (stage == AuraRules.GLOW ? 1.0F : 0.5F)) {
			double a = random.nextDouble() * Math.PI * 2;
			Vec3 at = player.position().add(Math.cos(a) * 0.36, 0.2 + random.nextDouble() * 1.5, Math.sin(a) * 0.36);
			int life = 18 + random.nextInt(14);
			mc.particleEngine.add(Glimmer.mote(level, at, random.nextInt(3) == 0 ? hot : color, 0.05F + random.nextFloat() * 0.03F, 0.6F, life,
				Math.cos(a) * 0.004, 0.012 + random.nextDouble() * 0.01, Math.sin(a) * 0.004, 0.003F));
			BODY.spend(life);
			bodyMotes++;
		}
		if (stage >= AuraRules.FLOW) {
			// Wisps from the shoulders: Flow's mark, fewer once the haze, the mantle and the corona carry the aura.
			float share = switch (stage) {
				case AuraRules.FLOW -> 1.0F;
				case AuraRules.EDGE -> 0.7F;
				case AuraRules.FORM -> 0.4F;
				default -> 0.3F;
			};
			for (int side = -1; side <= 1; side += 2) {
				if (BODY.hasRoom() && random.nextFloat() < (0.05F + 0.22F * k) * share) {
					Vec3 at = player.position().add(rx * side * 0.28 - fx * 0.05, shoulder, rz * side * 0.28 - fz * 0.05);
					int life = 18 + random.nextInt(12);
					mc.particleEngine.add(new AuraWisp(level, at, random.nextInt(4) == 0 ? hot : color, 0.1F + 0.05F * k, 0.55F + 0.3F * Math.min(1, k),
						life, 0.035 + 0.025 * k, 0.014, new Vec3(-fx * 0.006, 0, -fz * 0.006)));
					BODY.spend(life);
					bodyMotes++;
				}
			}
			// And off the blade.
			if (Aura.holdsWeapon(player) && BODY.hasRoom() && random.nextFloat() < (0.03F + 0.1F * k) * share) {
				double hand = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
				Vec3 at = player.position().add(fx * 0.55 + rx * hand * 0.4, 1.15 + random.nextDouble() * 0.4, fz * 0.55 + rz * hand * 0.4);
				int life = 12 + random.nextInt(10);
				mc.particleEngine.add(new AuraWisp(level, at, color, 0.07F + 0.03F * k, 0.5F, life, 0.025, 0.01, Vec3.ZERO));
				BODY.spend(life);
				bodyMotes++;
			}
		}
		if (stage >= AuraRules.SOVEREIGN) {
			// Embers flying off the corona, thick in a fight.
			int count = random.nextFloat() < 0.2F + 0.5F * Math.min(1.2F, k) ? (k > 1.0F ? 2 : 1) : 0;
			for (int i = 0; i < count && BODY.hasRoom(); i++) {
				double a = random.nextDouble() * Math.PI * 2;
				double side = Math.cos(a) * 0.4;
				double fore = Math.sin(a) * 0.28;
				Vec3 at = player.position().add(rx * side + fx * fore, 0.3 + random.nextDouble() * 1.4, rz * side + fz * fore);
				int life = 12 + random.nextInt(10);
				mc.particleEngine.add(Glimmer.mote(level, at, random.nextBoolean() ? hot : color, 0.05F + random.nextFloat() * 0.03F, 0.9F, life,
					Math.cos(a) * 0.006, 0.04 + random.nextDouble() * 0.04, Math.sin(a) * 0.006, 0.004F));
				BODY.spend(life);
				bodyMotes++;
			}
		}
	}

	/**
	 * Your own body aura, through your own eyes: from Edge, while it flares in a fight or surges, a faint glow of its colour rising
	 * from the bottom edge of the screen behind the hotbar, a little stronger at Sovereign, and nothing at all at rest. Never more
	 * than a whisper: a thin band at the very bottom, at most a seventh opaque at its foot and fading to nothing above.
	 */
	static void whisper(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.getCameraEntity() != player || !mc.options.getCameraType().isFirstPerson()
				|| MagicQuality.bodyAura != MagicQuality.BodyAura.FULL || player.isSpectator()) {
			return;
		}
		AuraAttachments.Look look = Aura.look(player);
		if (look.stage() < AuraRules.EDGE) {
			return;
		}
		float time = mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false);
		float k = bodyIntensity(player, time);
		float over = k - AuraFxRules.IDLE - 0.15F;
		if (over <= 0) {
			return;
		}
		float flicker = look.stage() >= AuraRules.SOVEREIGN ? 0.85F + 0.15F * Mth.sin(time * 0.7F) * Mth.sin(time * 0.31F) : 1.0F;
		float alpha = Math.min(0.14F, over * 0.26F) * (0.85F + 0.075F * (look.stage() - AuraRules.EDGE)) * flicker;
		int w = g.guiWidth();
		int h = g.guiHeight();
		int band = Math.max(8, h / 16);
		int color = mix(look.color(), 0xFFFFFF, 0.15F);
		g.fillGradient(0, h - band, w, h, color & 0xFFFFFF, (Mth.clamp(Math.round(alpha * 255), 0, 255) << 24) | (color & 0xFFFFFF));
	}

	private static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int gr = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return (r << 16) | (gr << 8) | bl;
	}

	// ------------------------------------------------------------------ for the game tests

	/**
	 * Trails drawn (everyone's), your own trails, impacts, bursts, flares heard, body motes and wisps let off, and bursts drawn as a
	 * whisper (close to your own eyes in first person), since the game started.
	 */
	public static int[] counts() {
		return new int[] {trails, ownTrails, impacts, bursts, flares, bodyMotes, whispers};
	}

	/** Pieces of your own arts' spectacle drawn in third person, and left out through your own eyes, since the game started. */
	public static int[] spectacle() {
		return new int[] {shown, spared};
	}

	/** The stroke of your last own trail ("cut", "sweep"...). */
	public static String lastOwnStroke() {
		return lastOwnStroke;
	}

	/** Whether {@code entity}'s body aura is surging now. */
	public static boolean surging(int entity) {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && surge(entity, mc.level.getGameTime()) > 0;
	}
}
