package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.WildercordSounds;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Aura's feel: the look and sound every technique shares, on the server. Everything here is only how a thing looks and sounds:
 * what happens is decided elsewhere, and these are called once it has.
 * <ul>
 * <li>{@link #trail}: a ribbon of the aura's light along the blade's arc ({@link AuraFxRules.Stroke}), brighter and wider at
 * each stage;</li>
 * <li>{@link #impact}: a blow landing ({@link AuraFxRules.Weight}): a flash and sparks where it struck, and for the striker and a
 * struck player a brief hit-stop and a nudge of the view;</li>
 * <li>{@link #banner}: a technique's name, shown briefly in the method's colour (by the side of the screen for its swordsman,
 * over their head for everyone else);</li>
 * <li>{@link #burst}: a burst of light ({@link Burst}): a flash, a ring, a star, sparks;</li>
 * <li>{@link #bodyAuraFlare}: the swordsman's body aura blazing up for a moment;</li>
 * <li>{@link #sound}: the method's own sounds ({@link SoundFamily}), so an Ember swordsman never sounds like a Rime one.</li>
 * </ul>
 * One call does all of it for an art: {@link #art} hands back an {@link Art} that remembers the swordsman's colour and stage.
 *
 * <p>Each client draws these itself (see {@code client.AuraFxClient}), because only it knows how it's looking: your own trails,
 * bursts and body aura in first person are thin, short and low, never in the middle of the screen, while third person and
 * everyone else see the whole of them. The server-sent light of {@link AuraVfx} stays for the big shapes in the world (a slash's
 * crescent in flight, a Dominion's circle, an art's arc), which everyone sees the same and your own view leaves out up close.</p>
 *
 * <p>A player's ordinary swings draw their trail on their own client at once (no waiting on the server); everyone else hears of
 * each swing of a blade with aura enough to coat from the server ({@link #swung}), never of digging.</p>
 */
public final class AuraFx {
	private AuraFx() {}

	// ------------------------------------------------------------------ payloads (server to client)

	/** Flag on a {@link Trail}: an ordinary swing (an onlooker on low settings may skip those, never a technique's). */
	public static final int ORDINARY = 1;

	/**
	 * A trail cut by {@code entity}'s blade: {@code stroke} ({@link AuraFxRules.Stroke}'s ordinal), cut back the other way when
	 * {@code mirror}, in {@code color} at {@code stage}, {@code power} times as wide, with {@link #ORDINARY} and other flags. The
	 * client lays it round the entity as it faces then, and carries it with them.
	 */
	public record Trail(int entity, int stroke, boolean mirror, int color, int stage, float power, int flags) implements CustomPacketPayload {
		public static final Type<Trail> TYPE = new Type<>(Wildercord.id("aura_fx_trail"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Trail> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Trail::entity, ByteBufCodecs.VAR_INT, Trail::stroke, ByteBufCodecs.BOOL, Trail::mirror, ByteBufCodecs.INT, Trail::color,
			ByteBufCodecs.VAR_INT, Trail::stage, ByteBufCodecs.FLOAT, Trail::power, ByteBufCodecs.VAR_INT, Trail::flags, Trail::new).cast();

		@Override
		public Type<Trail> type() {
			return TYPE;
		}
	}

	/**
	 * A blow landing: {@code attacker} (or -1) struck {@code target} at {@code at}, in {@code color} at {@code stage}, weighing
	 * {@code weight} ({@link AuraFxRules.Weight}'s ordinal). The striker's and a struck player's clients hold the moment.
	 */
	public record Impact(int attacker, int target, Vec3 at, int color, int stage, int weight) implements CustomPacketPayload {
		public static final Type<Impact> TYPE = new Type<>(Wildercord.id("aura_fx_impact"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Impact> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Impact::attacker, ByteBufCodecs.VAR_INT, Impact::target, Vec3.STREAM_CODEC, Impact::at, ByteBufCodecs.INT, Impact::color,
			ByteBufCodecs.VAR_INT, Impact::stage, ByteBufCodecs.VAR_INT, Impact::weight, Impact::new).cast();

		@Override
		public Type<Impact> type() {
			return TYPE;
		}
	}

	/** A technique's banner over {@code entity}: its {@code name}, a small {@code kicker} line above it, in {@code color}, of {@code kind}. */
	public record Banner(int entity, Component name, Component kicker, int color, int kind) implements CustomPacketPayload {
		public static final Type<Banner> TYPE = new Type<>(Wildercord.id("aura_fx_banner"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Banner> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Banner::entity, ComponentSerialization.TRUSTED_STREAM_CODEC, Banner::name, ComponentSerialization.TRUSTED_STREAM_CODEC,
			Banner::kicker, ByteBufCodecs.INT, Banner::color, ByteBufCodecs.VAR_INT, Banner::kind, Banner::new).cast();

		@Override
		public Type<Banner> type() {
			return TYPE;
		}
	}

	/**
	 * A burst of light at {@code at}, {@code size} blocks across, in {@code color}, facing {@code facing} (zero: whoever sees it),
	 * of {@code style} ({@link Burst} bits). {@code owner} (or -1) is whose it is: in their own first-person view a burst close to
	 * their eyes is drawn small and low.
	 */
	public record BurstCue(int owner, Vec3 at, Vec3 facing, int color, float size, int style) implements CustomPacketPayload {
		public static final Type<BurstCue> TYPE = new Type<>(Wildercord.id("aura_fx_burst"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BurstCue> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, BurstCue::owner, Vec3.STREAM_CODEC, BurstCue::at, Vec3.STREAM_CODEC, BurstCue::facing, ByteBufCodecs.INT, BurstCue::color,
			ByteBufCodecs.FLOAT, BurstCue::size, ByteBufCodecs.VAR_INT, BurstCue::style, BurstCue::new).cast();

		@Override
		public Type<BurstCue> type() {
			return TYPE;
		}
	}

	/** {@code entity}'s body aura blazes up, {@code strength} (0 to 1) over its flare, dying away over {@code ticks}. */
	public record Flare(int entity, int ticks, float strength) implements CustomPacketPayload {
		public static final Type<Flare> TYPE = new Type<>(Wildercord.id("aura_fx_flare"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Flare> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Flare::entity, ByteBufCodecs.VAR_INT, Flare::ticks, ByteBufCodecs.FLOAT, Flare::strength, Flare::new).cast();

		@Override
		public Type<Flare> type() {
			return TYPE;
		}
	}

	/** What a burst is made of: bits, combined freely. */
	public static final class Burst {
		private Burst() {}

		/** A soft flash of light. */
		public static final int FLASH = 1;
		/** A ring racing out (in the plane facing {@code facing}, or toward whoever sees it). */
		public static final int RING = 2;
		/** Sparks flung out and falling. */
		public static final int SPARKS = 4;
		/** A four-pointed glint across the flash. */
		public static final int STAR = 8;
		/** A second, wider ring a moment after the first. */
		public static final int ECHO = 16;
		/** Everything a perfect guard has: the parry's flash, glint, ring and sparks. */
		public static final int GUARD = FLASH | RING | SPARKS | STAR;
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Trail.TYPE, Trail.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Impact.TYPE, Impact.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Banner.TYPE, Banner.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BurstCue.TYPE, BurstCue.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Flare.TYPE, Flare.CODEC);
		// Every art: its name, the body's aura blazing up and the method's technique sound (an art adds its own on top).
		AuraApi.onString((player, art, context) -> performed(player, art));
	}

	static void forget(UUID id) {
		COMBO.remove(id);
	}

	static void clear() {
		COMBO.clear();
	}

	// ------------------------------------------------------------------ sending

	/** To everyone who can see {@code source} (within {@code range}), and to {@code source} itself when it's a player and {@code self}. */
	private static void send(Entity source, CustomPacketPayload payload, boolean self, double range) {
		if (Fx.muted() || !(source.level() instanceof ServerLevel)) {
			return;
		}
		for (ServerPlayer viewer : PlayerLookup.tracking(source)) {
			if (viewer != source && viewer.distanceToSqr(source) <= range * range && ServerPlayNetworking.canSend(viewer, payload.type())) {
				ServerPlayNetworking.send(viewer, payload);
			}
		}
		if (self && source instanceof ServerPlayer player && ServerPlayNetworking.canSend(player, payload.type())) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	/** To everyone within {@code range} of {@code at}. */
	private static void send(ServerLevel level, Vec3 at, CustomPacketPayload payload, double range) {
		if (Fx.muted()) {
			return;
		}
		for (ServerPlayer viewer : PlayerLookup.around(level, at, range)) {
			if (ServerPlayNetworking.canSend(viewer, payload.type())) {
				ServerPlayNetworking.send(viewer, payload);
			}
		}
	}

	// ------------------------------------------------------------------ trails

	/** A trail cut by a player's blade, in their aura's colour at their stage: for everyone, their own first-person view drawing it small. */
	public static void trail(ServerPlayer player, AuraFxRules.Stroke stroke) {
		trail(player, stroke, false, 1.0F);
	}

	public static void trail(ServerPlayer player, AuraFxRules.Stroke stroke, boolean mirror, float power) {
		trail(player, stroke, mirror, Aura.color(player), Aura.stage(player), power);
	}

	/** A trail cut by any creature's blade (a duelist's, a knight's), in {@code color} at {@code stage}. */
	public static void trail(LivingEntity entity, AuraFxRules.Stroke stroke, boolean mirror, int color, int stage, float power) {
		send(entity, new Trail(entity.getId(), stroke.ordinal(), mirror, color & 0xFFFFFF, stage, power, 0), true, AuraFxRules.SEEN);
	}

	/** Each player's last ordinary swing (game time) and whether it cut back, for the next to cut the other way. */
	private static final Map<UUID, long[]> COMBO = new HashMap<>();
	/** What each player's swing was as it began (sword string marks and the game time), noted before vanilla's blow ends a sprint. */
	private static final Map<ServerPlayer, long[]> STARTED = new WeakHashMap<>();
	/** When each player last swept with Flow (game time): that swing's trail is the sweep, not an ordinary cut as well. */
	private static final Map<ServerPlayer, Long> SWEPT = new WeakHashMap<>();

	/** Notes what a player's swing is as it begins (from {@code AuraCombat.swing}, at the head of the attack). */
	static void swingBegins(ServerPlayer player, float strength) {
		STARTED.put(player, new long[] {marks(player, strength), player.level().getGameTime()});
	}

	/**
	 * An ordinary swing the server saw (the punch every swing sends, or a spear's thrust): if the blade has aura enough to coat a
	 * blow, everyone else who can see it draws its trail, cut the way the swing was (a low swing sweeps, a leaping one falls, a
	 * running one thrusts). The swinger's own client drew its trail already. A swing that only met a block (digging) cuts none.
	 */
	public static void swung(ServerPlayer player, boolean thrust) {
		if (player.isSpectator() || !Aura.coated(player)) {
			return;
		}
		long now = player.level().getGameTime();
		Long swept = SWEPT.get(player);
		if (swept != null && swept == now) {
			// The sweep drew this swing's trail.
			return;
		}
		long[] started = STARTED.get(player);
		boolean struck = started != null && started[1] == now;
		if (!struck && !thrust && digging(player)) {
			return;
		}
		int marks = struck ? (int) started[0] : marks(player, player.getAttackStrengthScale(0.5F));
		marks |= SwordStrings.cues(player);
		long[] combo = COMBO.computeIfAbsent(player.getUUID(), k -> new long[] {Long.MIN_VALUE / 4, 0});
		boolean mirror = AuraFxRules.mirrored(combo[0], combo[1] != 0, now);
		combo[0] = now;
		combo[1] = mirror ? 1 : 0;
		AuraFxRules.Stroke stroke = AuraFxRules.stroke(marks, thrust);
		float power = SwordString.Token.FULL.fits(marks) ? 1.15F : 0.9F;
		send(player, new Trail(player.getId(), stroke.ordinal(), mirror, Aura.color(player), Aura.stage(player), power, ORDINARY), false,
			AuraFxRules.SEEN);
	}

	/**
	 * A Flow sweep (from {@code AuraCombat.sweep}): a wide, level trail round the front for everyone, and a broader whoosh of the
	 * method's blade. The sweeper's client drew it already as it swung (and leaves this out); it stands in for the swing's
	 * ordinary trail for everyone else.
	 */
	static void swept(ServerPlayer player) {
		SWEPT.put(player, player.level().getGameTime());
		trail(player, AuraFxRules.Stroke.SWEEP, false, 1.35F);
		sound(player, Sound.SWING, 0.55F, 0.78F);
	}

	/** What a swing is as the server sees it: full, low, leaping and running marks. */
	private static int marks(ServerPlayer player, float strength) {
		int marks = SwordString.Token.SWING.bit();
		if (strength >= AuraRules.FULL_SWING - 1.0E-4) {
			marks |= SwordString.Token.FULL.bit();
		}
		if (player.isShiftKeyDown()) {
			marks |= SwordString.Token.LOW.bit();
		}
		if (!player.onGround() && !player.isInWater() && !player.onClimbable() && !player.isPassenger() && !player.getAbilities().flying
				&& !player.isFallFlying()) {
			marks |= SwordString.Token.LEAP.bit();
		}
		if (player.isSprinting()) {
			marks |= SwordString.Token.RUN.bit();
		}
		return marks;
	}

	/** Whether a swing that struck nothing was at a block in reach (digging, or knocking at a wall): no trail for that. */
	private static boolean digging(ServerPlayer player) {
		HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
		return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK && !player.level().getBlockState(block.getBlockPos()).isAir();
	}

	// ------------------------------------------------------------------ impacts

	/** A player's blow landing on {@code target}, in their aura's colour: a flash where it struck, and the moment held for both. */
	public static void impact(ServerPlayer attacker, LivingEntity target, AuraFxRules.Weight weight) {
		impact(attacker, target, Aura.color(attacker), Aura.stage(attacker), weight);
	}

	/** Any creature's blow (or a burst of aura with no one behind it, {@code attacker} null) landing on {@code target}. */
	public static void impact(Entity attacker, LivingEntity target, int color, int stage, AuraFxRules.Weight weight) {
		Vec3 at = struckAt(attacker, target);
		Impact cue = new Impact(attacker == null ? -1 : attacker.getId(), target.getId(), at, color & 0xFFFFFF, stage, weight.ordinal());
		// Everyone who can see the one struck (the striker among them), and a struck player themselves.
		send(target, cue, true, AuraFxRules.SEEN);
		if (attacker instanceof ServerPlayer player && player.distanceToSqr(target) > 48 * 48 && ServerPlayNetworking.canSend(player, Impact.TYPE)) {
			ServerPlayNetworking.send(player, cue);
		}
	}

	/** Where a blow meets its foe: on the side facing the striker, at the height of the striker's blade (held within the foe). */
	static Vec3 struckAt(Entity attacker, LivingEntity target) {
		AABB box = target.getBoundingBox();
		Vec3 centre = box.getCenter();
		if (attacker == null) {
			return centre;
		}
		Vec3 toward = attacker.position().subtract(target.position());
		Vec3 flat = new Vec3(toward.x, 0, toward.z);
		Vec3 side = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(target.getBbWidth() * 0.5);
		double y = Math.max(box.minY + 0.25, Math.min(box.maxY - 0.15, attacker.getEyeY() - 0.4));
		return new Vec3(centre.x + side.x, y, centre.z + side.z);
	}

	// ------------------------------------------------------------------ banners

	/** A technique's name over a player, in their aura's colour: {@code kicker} the small line above it (or empty). */
	public static void banner(ServerPlayer player, Component name, Component kicker, AuraFxRules.BannerKind kind) {
		banner(player, name, kicker, Aura.color(player), kind);
	}

	/**
	 * A technique's name over any creature (a player, or a duelist loosing an art of its own), in {@code color}: by the side of the
	 * screen for a player who loosed it, over its head for everyone else.
	 */
	public static void banner(LivingEntity entity, Component name, Component kicker, int color, AuraFxRules.BannerKind kind) {
		send(entity, new Banner(entity.getId(), name, kicker == null ? Component.empty() : kicker, color & 0xFFFFFF, kind.ordinal()), true,
			AuraFxRules.BANNER_SEEN);
	}

	/**
	 * An art's banner: its name, and above it the method's name and which art it is ("Ember Breath · First Art"). The
	 * placeholder arts are named by their ordinal already, so theirs says only the method; an art a player has of their own
	 * (from a string source: a technique they wrote) is a technique. The Final Art's banner is grand.
	 */
	public static void banner(ServerPlayer player, AuraApi.StringArt art) {
		banner(player, Component.translatable(art.nameKey()), kicker(player, art),
			art.stage() >= AuraRules.SOVEREIGN ? AuraFxRules.BannerKind.GRAND : AuraFxRules.BannerKind.ART);
	}

	/** The small line over an art's name. */
	static Component kicker(ServerPlayer player, AuraApi.StringArt art) {
		Component method = Aura.method(player).<Component>map(m -> Component.translatable(m.nameKey())).orElse(Component.empty());
		if (PlaceholderArts.IDS.contains(art.id())) {
			return method;
		}
		Component which = AuraApi.string(art.id()).isPresent() ? Component.translatable(AuraFxRules.ordinalKey(art.stage()))
			: Component.translatable("aura.wildercord.banner.technique");
		return Component.translatable("aura.wildercord.banner.kicker", method, which);
	}

	// ------------------------------------------------------------------ bursts and the body's aura

	/** A burst of light at {@code at} (see {@link Burst}), {@code owner}'s (or nobody's): in the owner's first person it's drawn small. */
	public static void burst(ServerLevel level, Entity owner, Vec3 at, Vec3 facing, int color, float size, int style) {
		send(level, at, new BurstCue(owner == null ? -1 : owner.getId(), at, facing == null ? Vec3.ZERO : facing, color & 0xFFFFFF, size, style),
			AuraFxRules.SEEN);
	}

	/** The player's body aura blazing up for {@code ticks}, {@code strength} (0 to 1) over its flare (an art, a perfect guard). */
	public static void bodyAuraFlare(ServerPlayer player, int ticks, float strength) {
		send(player, new Flare(player.getId(), Math.max(1, ticks), Math.max(0, Math.min(1, strength))), true, AuraFxRules.SEEN);
	}

	/** A blow given or taken: the body's aura flares for a few seconds (renewed only now and then, so a long fight sends little). */
	static void fighting(ServerPlayer player) {
		long now = player.level().getGameTime();
		AuraPresence.Look look = AuraPresence.look(player);
		if (AuraFxRules.renewFight(look.fightUntil(), now)) {
			AuraPresence.look(player, look.fightUntil(now + AuraFxRules.FIGHT_TICKS));
		}
	}

	// ------------------------------------------------------------------ the methods' sounds

	/** Which of a method's sounds: a blade's swing, a blow landing, a technique loosed. */
	public enum Sound {
		SWING,
		IMPACT,
		ART
	}

	/**
	 * A method's sounds: its blade's swing, its blows landing, its techniques loosed (kit names from {@code tools/feel}). Each
	 * built-in method has its own, so a listener can tell Ember from Rime with their eyes shut: Ember roars and crackles, Rime rings
	 * like struck ice, Thunder snaps, Gale whistles, Stone thuds, Verdant rustles, Hollow pulls, Starlit chimes, Hourglass ticks,
	 * Crimson beats.
	 */
	public record SoundFamily(String swing, String impact, String art) {
		/** The family {@code aura_<name>_swing}, {@code _impact} and {@code _art}. */
		public static SoundFamily named(String name) {
			return new SoundFamily("aura_" + name + "_swing", "aura_" + name + "_impact", "aura_" + name + "_art");
		}

		public String of(Sound sound) {
			return switch (sound) {
				case SWING -> swing;
				case IMPACT -> impact;
				case ART -> art;
			};
		}
	}

	/** Plain steel and light, for a method without sounds of its own (an add-on's that gave none). */
	public static final SoundFamily STEEL = SoundFamily.named("steel");

	private static final Map<String, SoundFamily> FAMILIES = new LinkedHashMap<>();

	static {
		for (BreathingMethod method : BreathingMethods.BUILT_IN) {
			FAMILIES.put(method.id(), SoundFamily.named(method.id()));
		}
	}

	/** Gives a method its own sounds (an add-on's: name kit sounds it ships, or another method's). */
	public static synchronized void registerFamily(String methodId, SoundFamily family) {
		FAMILIES.put(methodId, family);
	}

	/** A method's sounds ({@link #STEEL} for one without). */
	public static synchronized SoundFamily family(String methodId) {
		return FAMILIES.getOrDefault(methodId == null ? "" : methodId, STEEL);
	}

	/** Every family there is, by method (the tests check every built-in one's sounds exist). */
	public static synchronized Map<String, SoundFamily> families() {
		return Map.copyOf(FAMILIES);
	}

	/** One of the player's method's sounds, for everyone near (the player too). */
	public static void sound(ServerPlayer player, Sound sound, float volume, float pitch) {
		Feels.sound(player.level(), player.position().add(0, 1.1, 0), family(Aura.data(player).method()).of(sound), volume, pitch);
	}

	/** One of the player's method's sounds for everyone near but the player (whose own client played it already, at once). */
	static void soundForOthers(ServerPlayer player, Sound sound, float volume, float pitch) {
		if (Fx.muted()) {
			return;
		}
		SoundEvent event = WildercordSounds.kit(family(Aura.data(player).method()).of(sound));
		if (event == null) {
			return;
		}
		float jitter = 1.0F + (player.getRandom().nextFloat() - 0.5F) * 0.06F;
		player.level().playSound(player, player.getX(), player.getY() + 1.1, player.getZ(), event, SoundSource.PLAYERS, volume, pitch * jitter);
	}

	// ------------------------------------------------------------------ an art, in one call

	/**
	 * What every art gets, as it's performed (heard through {@link AuraApi#onString}): its banner, the body's aura blazing up (more
	 * for the Final Art) and the method's technique sound. An art adds its own trail, impacts and sounds on top (see {@link Art}).
	 */
	static void performed(ServerPlayer player, AuraApi.StringArt art) {
		banner(player, art);
		boolean grand = art.stage() >= AuraRules.SOVEREIGN;
		bodyAuraFlare(player, grand ? 50 : 30, grand ? 1.0F : 0.7F);
		sound(player, Sound.ART, grand ? 1.1F : 0.85F, grand ? 0.85F : 1.0F);
	}

	/**
	 * A player's art's look, in one place: the swordsman's colour and stage remembered, and each call sends one thing. For example:
	 * <pre>
	 * AuraFx.art(player).trail(Stroke.DRAW).impact(foe).burst(at, 1.2F, Burst.RING);
	 * </pre>
	 * The banner, the body's flare and the method's technique sound come by themselves for an art performed through a sword string.
	 */
	public static Art art(ServerPlayer player) {
		return new Art(player, Aura.color(player), Aura.stage(player));
	}

	/** See {@link #art}. */
	public static final class Art {
		private final ServerPlayer player;
		private int color;
		private final int stage;

		Art(ServerPlayer player, int color, int stage) {
			this.player = player;
			this.color = color;
			this.stage = stage;
		}

		/** The rest in another colour (a counter's parry gold, say). */
		public Art color(int color) {
			this.color = color & 0xFFFFFF;
			return this;
		}

		public int color() {
			return color;
		}

		public Art trail(AuraFxRules.Stroke stroke) {
			return trail(stroke, false, 1.25F);
		}

		public Art trail(AuraFxRules.Stroke stroke, boolean mirror, float power) {
			AuraFx.trail(player, stroke, mirror, color, stage, power);
			return this;
		}

		/** A heavy blow landing on {@code target}. */
		public Art impact(LivingEntity target) {
			return impact(target, AuraFxRules.Weight.HEAVY);
		}

		public Art impact(LivingEntity target, AuraFxRules.Weight weight) {
			AuraFx.impact(player, target, color, stage, weight);
			return this;
		}

		public Art burst(Vec3 at, float size, int style) {
			AuraFx.burst(player.level(), player, at, Vec3.ZERO, color, size, style);
			return this;
		}

		public Art burst(Vec3 at, Vec3 facing, float size, int style) {
			AuraFx.burst(player.level(), player, at, facing, color, size, style);
			return this;
		}

		public Art flare(int ticks, float strength) {
			bodyAuraFlare(player, ticks, strength);
			return this;
		}

		public Art sound(Sound sound, float volume, float pitch) {
			AuraFx.sound(player, sound, volume, pitch);
			return this;
		}

		/** A banner of the art's own making (a finisher's name, say), in its colour. */
		public Art banner(Component name, Component kicker, AuraFxRules.BannerKind kind) {
			AuraFx.banner(player, name, kicker, color, kind);
			return this;
		}
	}
}
