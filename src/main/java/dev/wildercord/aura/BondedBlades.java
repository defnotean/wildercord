package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.net.PacketThrottle;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The bonded blade at runtime (the rules and every number are {@link BladeRules}; the ceremonies are {@link BladeCeremony}; what its
 * traits do is {@link BladeTraits}). A swordsman from Edge bonds one blade at a ley crossing; from then on the blade carries the bond
 * itself ({@link #BOND}, a {@link BladeBond}), grows by the resonance of every fight it's in ({@link #gain} and the deeds that feed it),
 * takes a name, a trait and its fullest look as it climbs its tiers, and keeps its story.
 *
 * <p><b>The bond holds the blade to its swordsman.</b> It's kept through death (taken out of the inventory before anything drops,
 * {@link #dying}, and given back to the next body, {@link #respawned}); it never breaks (worn to its last point it stays notched);
 * on the ground it can't burn, be blown up or rot away, only its swordsman can pick it up, hoppers and mobs leave it lying, and it
 * comes home from the void ({@link ItemEntity} through {@code mixin.ItemEntityBondMixin}); in a chest only its swordsman can take it
 * out; and in anyone else's hands it carries no aura and slips straight back to its swordsman ({@link #homeward}), or waits for them
 * in the {@link BladeRegistry} if they aren't here. A blade is only ever moved, never copied: there's never a second.</p>
 *
 * <p>The swordsman's own record ({@link #STATE}) is only the bond's id and what the Aura page shows while the blade is away; a death
 * keeps the blade in {@link #KEPT} until the next body; {@link #RITE} tells everyone near a ceremony is under way (its glow, the HUD).</p>
 */
public final class BondedBlades {
	private BondedBlades() {}

	// ------------------------------------------------------------------ the blade itself

	/** The bond, on the blade: who it belongs to, how far it has grown, where it was bonded, its story. */
	public static final DataComponentType<BladeBond> BOND = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Wildercord.id("bonded_blade"),
		DataComponentType.<BladeBond>builder().persistent(BladeBond.CODEC).networkSynchronized(BladeBond.STREAM_CODEC).ignoreSwapAnimation().build());
	/** What a blade remembers of a bond that ended: a line in its tooltip, nothing more. */
	public static final DataComponentType<BladeBond.Former> FORMER = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Wildercord.id("former_bond"),
		DataComponentType.<BladeBond.Former>builder().persistent(BladeBond.Former.CODEC).networkSynchronized(BladeBond.Former.STREAM_CODEC).build());
	/**
	 * What can be bonded: swords, axes, spears and the mace (aura-forged ones too: they're swords, axes and spears), anything a server or
	 * an add-on adds. Not the trident: thrown and left lying, it's out of its swordsman's hand more than in it.
	 */
	public static final TagKey<Item> BONDABLE = TagKey.create(Registries.ITEM, Wildercord.id("bondable_blades"));

	// ------------------------------------------------------------------ the swordsman's own record

	/** What the Aura page shows of the blade while it's away: its item, name, tier, colour, resonance and trait. */
	public record Shown(String item, String model, String name, int tier, int color, float resonance, String trait) {
		public static final Shown NONE = new Shown("", "", "", 0, 0, 0, "");

		public Shown {
			item = item == null ? "" : item;
			model = model == null ? "" : model;
			name = name == null ? "" : name;
			trait = trait == null ? "" : trait;
		}

		static final Codec<Shown> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("item", "").forGetter(Shown::item),
			Codec.STRING.optionalFieldOf("model", "").forGetter(Shown::model),
			Codec.STRING.optionalFieldOf("name", "").forGetter(Shown::name),
			Codec.INT.optionalFieldOf("tier", 0).forGetter(Shown::tier),
			Codec.INT.optionalFieldOf("color", 0).forGetter(Shown::color),
			Codec.FLOAT.optionalFieldOf("resonance", 0F).forGetter(Shown::resonance),
			Codec.STRING.optionalFieldOf("trait", "").forGetter(Shown::trait)
		).apply(i, Shown::new));

		static final StreamCodec<ByteBuf, Shown> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(128), Shown::item, ByteBufCodecs.stringUtf8(128), Shown::model, ByteBufCodecs.stringUtf8(128), Shown::name,
			ByteBufCodecs.VAR_INT, Shown::tier, ByteBufCodecs.INT, Shown::color, ByteBufCodecs.FLOAT, Shown::resonance, ByteBufCodecs.stringUtf8(96),
			Shown::trait, Shown::new);
	}

	/**
	 * A swordsman's bond as they keep it.
	 *
	 * @param bond    the bond's id ("" for none)
	 * @param shown   the blade as it was last seen with them
	 * @param lastDim where it was last with them (dimension id; "" before it ever left)
	 * @param away    whether it's away from them now
	 */
	public record State(String bond, Shown shown, String lastDim, int lastX, int lastY, int lastZ, boolean away) {
		public static final State NONE = new State("", Shown.NONE, "", 0, 0, 0, false);

		public State {
			bond = bond == null ? "" : bond;
			shown = shown == null ? Shown.NONE : shown;
			lastDim = lastDim == null ? "" : lastDim;
		}

		static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("bond", "").forGetter(State::bond),
			Shown.CODEC.optionalFieldOf("shown", Shown.NONE).forGetter(State::shown),
			Codec.STRING.optionalFieldOf("last_dim", "").forGetter(State::lastDim),
			Codec.INT.optionalFieldOf("last_x", 0).forGetter(State::lastX),
			Codec.INT.optionalFieldOf("last_y", 0).forGetter(State::lastY),
			Codec.INT.optionalFieldOf("last_z", 0).forGetter(State::lastZ),
			Codec.BOOL.optionalFieldOf("away", false).forGetter(State::away)
		).apply(i, State::new));

		static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(48), State::bond, Shown.STREAM_CODEC, State::shown, ByteBufCodecs.stringUtf8(128), State::lastDim,
			ByteBufCodecs.VAR_INT, State::lastX, ByteBufCodecs.VAR_INT, State::lastY, ByteBufCodecs.VAR_INT, State::lastZ, ByteBufCodecs.BOOL, State::away,
			State::new);

		public boolean bonded() {
			return !bond.isEmpty();
		}

		/** The bond's id, or null for none (or a damaged record). */
		public UUID bondId() {
			if (bond.isEmpty()) {
				return null;
			}
			try {
				return UUID.fromString(bond);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}

		State with(Shown shown, boolean away, Level where, BlockPos pos) {
			return where == null ? new State(bond, shown, lastDim, lastX, lastY, lastZ, away)
				: new State(bond, shown, where.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), away);
		}
	}

	/** Each swordsman's bond: saved, kept through death, synced to its owner (the page, the reader's prices). */
	public static final AttachmentType<State> STATE = AttachmentRegistry.create(
		Wildercord.id("blade_bond"),
		builder -> builder
			.initializer(() -> State.NONE)
			.persistent(State.CODEC)
			.syncWith(State.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** A blade kept through a death: the slot it was in, and the blade (moved here out of the dying body's inventory, never copied). */
	public record Kept(int slot, ItemStack stack) {
		static final Codec<Kept> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("slot", -1).forGetter(Kept::slot),
			ItemStack.CODEC.fieldOf("stack").forGetter(Kept::stack)
		).apply(i, Kept::new));
	}

	/** The blades a death is keeping for the next body (saved, so a swordsman who leaves on the death screen still gets theirs). */
	public static final AttachmentType<List<Kept>> KEPT = AttachmentRegistry.create(
		Wildercord.id("blade_kept"),
		builder -> builder.persistent(Kept.CODEC.listOf()).copyOnDeath()
	);

	/**
	 * A ceremony under way, as everyone near sees it: {@code kind} 1 a bond, 2 a passing; when it began, how long it runs, and the colour
	 * it kindles in. The blade's glow kindles with it, and its swordsman's HUD counts it down.
	 */
	public record Rite(int kind, long start, int ticks, int color) {
		static final StreamCodec<ByteBuf, Rite> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Rite::kind, ByteBufCodecs.VAR_LONG, Rite::start,
			ByteBufCodecs.VAR_INT, Rite::ticks, ByteBufCodecs.INT, Rite::color, Rite::new);

		public static final int BOND = 1;
		public static final int PASS = 2;

		/** How far along it is at {@code now} (0 to 1). */
		public float progress(long now) {
			return ticks <= 0 ? 0 : Math.max(0, Math.min(1, (now - start) / (float) ticks));
		}
	}

	public static final AttachmentType<Rite> RITE = AttachmentRegistry.create(
		Wildercord.id("blade_rite"),
		builder -> builder.syncWith(Rite.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	// ------------------------------------------------------------------ reading (both sides)

	/** The bond {@code stack} carries, or null. */
	public static BladeBond bond(ItemStack stack) {
		return stack == null || stack.isEmpty() ? null : stack.get(BOND);
	}

	public static boolean bonded(ItemStack stack) {
		return bond(stack) != null;
	}

	/** Whether {@code stack} could be bonded: one bondable weapon that wears, carrying no bond now (a former one's fine). */
	public static boolean canBond(ItemStack stack) {
		return stack != null && !stack.isEmpty() && !bonded(stack) && BladeRules.bondable(stack.is(BONDABLE), stack.isDamageableItem(), stack.getCount());
	}

	public static State state(Player player) {
		return player.getAttachedOrElse(STATE, State.NONE);
	}

	/** Whether bonded blades work for {@code player}: on on this server (or the one this client is on) and aura working. */
	public static boolean on(Player player) {
		return player != null && Config.bonds(player) && Aura.enabled(player);
	}

	/** Whether {@code stack} is bonded to someone other than {@code entity} (a player): in their hands it's only steel. */
	public static boolean foreign(LivingEntity entity, ItemStack stack) {
		if (!(entity instanceof Player)) {
			return false;
		}
		BladeBond b = bond(stack);
		return b != null && !b.ownedBy(entity.getUUID());
	}

	/**
	 * The bond of the blade in {@code player}'s main hand if it's theirs: bonded to them, and (where this side knows their record: the
	 * server, their own client) the bond they hold now. Null otherwise.
	 */
	public static BladeBond held(Player player) {
		if (player == null) {
			return null;
		}
		BladeBond b = bond(player.getMainHandItem());
		if (b == null || !b.ownedBy(player.getUUID())) {
			return null;
		}
		State s = state(player);
		if (s.bonded() && !s.bond().equals(b.id().toString())) {
			return null;
		}
		if (!s.bonded() && !player.level().isClientSide()) {
			return null;
		}
		return b;
	}

	/** The tier the blade in {@code player}'s hand gives them now (0 with none, or one sleeping until their stage reaches it). */
	public static int heldTier(Player player) {
		BladeBond b = held(player);
		return b == null ? 0 : BladeRules.effective(b.tier(), Aura.stage(player));
	}

	/** The trait the blade in {@code player}'s hand gives them now ("" with none, before Awakened, or with traits off). */
	public static String heldTrait(Player player) {
		if (!on(player) || !Config.bladeTraits(player)) {
			return "";
		}
		BladeBond b = held(player);
		if (b == null || BladeRules.effective(b.tier(), Aura.stage(player)) < BladeRules.AWAKENED) {
			return "";
		}
		return b.growth().trait();
	}

	/** How strong the trait in {@code player}'s hand is now (0 with none). */
	public static double heldStrength(Player player) {
		return heldTrait(player).isEmpty() ? 0 : BladeRules.traitStrength(heldTier(player));
	}

	/** Where {@code player}'s own bonded blade is in their inventory (its slot), or -1 when it isn't with them. */
	public static int slotOf(Player player) {
		UUID id = state(player).bondId();
		if (id == null) {
			return -1;
		}
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			BladeBond b = bond(inv.getItem(i));
			if (b != null && b.id().equals(id)) {
				return i;
			}
		}
		return -1;
	}

	/** {@code player}'s own bonded blade if it's with them, or empty. */
	public static ItemStack carried(Player player) {
		int slot = slotOf(player);
		return slot < 0 ? ItemStack.EMPTY : player.getInventory().getItem(slot);
	}

	/** The world's day now (from 1), as a blade's story counts days. */
	public static long day(Level level) {
		return level.getGameTime() / 24000L + 1;
	}

	// ------------------------------------------------------------------ the deeds that grow it (server)

	/** What a swordsman's blade has gathered since it was last written (blades are written about once a second, not every blow). */
	private static final class Pending {
		double resonance;
		final Map<String, Integer> counts = new LinkedHashMap<>();
		final List<String> arts = new ArrayList<>();
		final List<BladeBond.Deed> deeds = new ArrayList<>();
		long since = -1;

		boolean empty() {
			return resonance <= 0 && counts.isEmpty() && arts.isEmpty() && deeds.isEmpty();
		}
	}

	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	/** What each foe has given each swordsman's blade through arts, finishers and the rest (its kill aside), and when last. */
	private static final Map<UUID, Map<UUID, double[]>> FOES = new HashMap<>();
	/** When each swordsman last felled each player (one player's fall counts once a day). */
	private static final Map<UUID, Map<UUID, Long>> FALLEN = new HashMap<>();

	private static Pending pending(ServerPlayer player) {
		Pending p = PENDING.computeIfAbsent(player.getUUID(), k -> new Pending());
		if (p.since < 0) {
			p.since = player.level().getGameTime();
		}
		return p;
	}

	/** Whether {@code player}'s blade gathers now: bonds on, their own blade in hand, and it awake for them (their stage reaches Bonded). */
	private static boolean gathering(ServerPlayer player) {
		return on(player) && heldTier(player) >= BladeRules.BONDED;
	}

	/** Whether {@code player} carries their own blade (anywhere about them), bonds on, and it awake for them. */
	private static boolean carrying(ServerPlayer player) {
		if (!on(player)) {
			return false;
		}
		ItemStack blade = carried(player);
		BladeBond b = bond(blade);
		return b != null && BladeRules.effective(b.tier(), Aura.stage(player)) >= BladeRules.BONDED;
	}

	/**
	 * Resonance for {@code player}'s blade: {@code amount} times the server's {@code resonance_gain}, through the hooks; held to what one
	 * {@code foe} may give when {@code capped}. Returns what it gathered.
	 */
	static double gain(ServerPlayer player, double amount, String source, LivingEntity foe, boolean capped) {
		if (amount <= 0) {
			return 0;
		}
		double a = amount * Math.max(0, Config.get().aura().bonds().resonanceGain());
		for (AuraApi.BladeHook hook : AuraApi.bladeHooks()) {
			try {
				a = hook.resonance(player, a, source);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A bonded blade hook threw; skipping it", e);
			}
		}
		if (capped && foe != null) {
			long now = player.level().getGameTime();
			Map<UUID, double[]> foes = FOES.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
			double[] given = foes.computeIfAbsent(foe.getUUID(), k -> new double[] {0, now});
			double cap = BladeRules.foeCap(Math.max(0.5, AuraCombat.worth(player, foe)), Spirits.isBoss(foe)) * Math.max(0, Config.get().aura().bonds().resonanceGain());
			a = Math.max(0, Math.min(a, cap - given[0]));
			given[0] += a;
			given[1] = now;
			if (foes.size() > 64) {
				foes.values().removeIf(v -> now - (long) v[1] > 6000);
			}
		}
		if (a <= 0) {
			return 0;
		}
		pending(player).resonance += a;
		return a;
	}

	private static void count(ServerPlayer player, String id, int n) {
		pending(player).counts.merge(id, n, Integer::sum);
	}

	private static void deed(ServerPlayer player, String kind, String arg) {
		pending(player).deeds.add(new BladeBond.Deed(kind, arg, day(player.level())));
	}

	/** Whether {@code foe} is a worthy foe for {@code player}'s blade: something to learn from, not practice, not helpless. */
	private static boolean worthy(ServerPlayer player, LivingEntity foe) {
		return foe != null && !Momentum.practice(player, foe) && !Momentum.helpless(foe) && AuraCombat.worth(player, foe) > 0 && !Spars.partners(player, foe);
	}

	/** A foe felled by {@code player}'s blade (a blow or aura off it), from {@code AuraCombat.landed}. */
	static void killed(ServerPlayer player, LivingEntity target, double worth, double repetition, boolean practice) {
		if (practice || worth <= 0 || !gathering(player) || Momentum.helpless(target)) {
			return;
		}
		long now = player.level().getGameTime();
		boolean boss = Spirits.isBoss(target);
		if (target instanceof Player victim) {
			Map<UUID, Long> fallen = FALLEN.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
			Long last = fallen.get(victim.getUUID());
			if (last != null && now - last < BladeRules.PLAYER_KILL_REST) {
				return;
			}
			fallen.put(victim.getUUID(), now);
			count(player, BladeRules.PLAYERS, 1);
		}
		gain(player, BladeRules.kill(worth, repetition, boss), "kill", target, false);
		count(player, BladeRules.KILLS, 1);
		if (boss) {
			count(player, BladeRules.BOSSES, 1);
			deed(player, BladeRules.DEED_BOSS, target.getType().getDescriptionId());
		}
		if (worth >= 1.3 || target.hasAttached(dev.wildercord.player.WildercordAttachments.RUNEBOUND)) {
			count(player, BladeRules.STRONG, 1);
		}
		if (target.is(EntityTypeTags.UNDEAD)) {
			count(player, BladeRules.UNDEAD, 1);
		}
		if (player.level().dimension() == Level.OVERWORLD && !player.level().isBrightOutside()) {
			count(player, BladeRules.NIGHT, 1);
		}
		if (player.getHealth() < player.getMaxHealth() * BladeRules.LAST_LIGHT_BELOW) {
			count(player, BladeRules.LOW, 1);
		}
		if (!player.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(12),
				p -> p != player && p.isAlive() && !p.isSpectator() && WayBanner.ally(player, p)).isEmpty()) {
			count(player, BladeRules.ALLIED, 1);
		}
	}

	/**
	 * A foe felled by a later strike of an art that had already struck it (the first strike answers through {@code AuraCombat.landed},
	 * which counts a kill there; this counts one the art's second or third blow made).
	 */
	public static void artFelled(ServerPlayer player, LivingEntity foe) {
		boolean practice = foe instanceof dev.wildercord.cast.TrainingDummy || player.level().dimension() == dev.wildercord.cast.PracticeRoom.DIMENSION;
		killed(player, foe, practice ? 1.0 : AuraCombat.worth(player, foe), 1.0, practice);
	}

	/** An art (or a technique) of {@code player}'s landing on its {@code n}th foe, from {@code ArtKit.Hits}. */
	public static void artLanded(ServerPlayer player, AuraApi.StringArt art, int n, LivingEntity foe) {
		if (art == null || !gathering(player) || !worthy(player, foe)) {
			return;
		}
		boolean technique = TechniqueRules.slotOf(art.id()) >= 0;
		int slot = technique ? Techniques.momentumSlot(player, art) : AuraApi.ArtSlot.of(art).map(Enum::ordinal).orElse(0);
		gain(player, BladeRules.art(Math.max(0, slot), n), technique ? "technique" : "art", foe, true);
		if (n == 1) {
			if (technique) {
				count(player, BladeRules.TECHNIQUES, 1);
			} else {
				count(player, BladeRules.ARTS, 1);
				pending(player).arts.add(art.id());
			}
		}
	}

	/** A finisher of {@code player}'s landed on {@code target} (an {@code onFinisher} hook). */
	private static void finished(ServerPlayer player, LivingEntity target) {
		if (!gathering(player) || !worthy(player, target)) {
			return;
		}
		gain(player, Spirits.isBoss(target) ? BladeRules.BOSS_FINISHER : BladeRules.FINISHER, "finisher", target, true);
		count(player, BladeRules.FINISHERS, 1);
	}

	/** A foe's stance broken by {@code player} (an {@code onStanceBroken} hook). */
	private static void broke(ServerPlayer player, LivingEntity target) {
		if (!gathering(player) || !worthy(player, target)) {
			return;
		}
		gain(player, BladeRules.BROKEN, "stance", target, true);
		count(player, BladeRules.BROKEN_STANCES, 1);
	}

	/** A perfect guard of {@code player}'s against {@code attacker} (or a shot), from {@code AuraGuard}. */
	static void guarded(ServerPlayer player, LivingEntity attacker) {
		BladeTraits.guarded(player);
		if (!gathering(player)) {
			return;
		}
		count(player, BladeRules.GUARDS, 1);
		if (worthy(player, attacker)) {
			gain(player, BladeRules.GUARD, "guard", attacker, true);
		}
	}

	/** An Aura Step taken with the blade in hand, from {@code AuraStep}. */
	static void stepped(ServerPlayer player) {
		if (gathering(player)) {
			count(player, BladeRules.STEPS, 1);
		}
	}

	/** An Aura Slash loosed with the blade in hand, from {@code AuraSlash}. */
	static void slashed(ServerPlayer player) {
		if (gathering(player)) {
			count(player, BladeRules.SLASHES, 1);
		}
	}

	/** An awakening begun (an {@code onAwakening} hook). */
	private static void awakened(ServerPlayer player) {
		if (gathering(player)) {
			gain(player, BladeRules.AWAKENING, "awakening", null, false);
			count(player, BladeRules.AWAKENINGS, 1);
		}
	}

	/** A breakthrough into {@code stage}, from {@code AuraBreakthroughs}: carried, the blade remembers it. */
	static void brokeThrough(ServerPlayer player, int stage) {
		if (!carrying(player)) {
			return;
		}
		gain(player, BladeRules.breakthrough(stage), "breakthrough", null, false);
		deed(player, BladeRules.DEED_BREAKTHROUGH, AuraStages.id(stage));
		flushSoon(player);
	}

	/** A duelist of {@code method} beaten, from {@code DuelistDuels}. */
	public static void dueled(ServerPlayer player, String method) {
		if (!gathering(player)) {
			return;
		}
		gain(player, BladeRules.DUEL, "duel", null, false);
		count(player, BladeRules.DUELS, 1);
		deed(player, BladeRules.DEED_DUEL, method == null ? "" : method);
		flushSoon(player);
	}

	/** Writes {@code player}'s blade at the next tick rather than within the second (a deed worth seeing at once). */
	private static void flushSoon(ServerPlayer player) {
		dev.wildercord.cast.Scheduler.later(1, () -> {
			if (!player.hasDisconnected()) {
				flush(player);
			}
		});
	}

	// ------------------------------------------------------------------ writing it (server)

	/** How often each swordsman's blade is written (ticks). */
	private static final int WRITE_EVERY = 20;
	/** Gathered resonance waits this long for a blade that has left the hand before it's let go. */
	private static final int PENDING_MOST = 600;

	/** Writes what {@code player}'s blade has gathered onto it, and grows it a tier if that's earned. */
	static void flush(ServerPlayer player) {
		Pending p = PENDING.get(player.getUUID());
		int slot = slotOf(player);
		if (slot < 0) {
			if (p != null && player.level().getGameTime() - p.since > PENDING_MOST) {
				PENDING.remove(player.getUUID());
			}
			return;
		}
		ItemStack blade = player.getInventory().getItem(slot);
		BladeBond b = bond(blade);
		if (b == null) {
			return;
		}
		BladeBond next = b;
		if (p != null && !p.empty()) {
			next = next.withGrowth(next.growth().withResonance((float) (next.growth().resonance() + p.resonance)))
				.withHistory(next.history().plus(p.counts, p.arts, p.deeds));
		}
		PENDING.remove(player.getUUID());
		next = grow(player, blade, next);
		if (!next.equals(b)) {
			blade.set(BOND, next);
		}
	}

	/** {@code b} grown a tier (or more) if its resonance, deeds and its swordsman's stage now reach one; each tier its moment. */
	private static BladeBond grow(ServerPlayer player, ItemStack blade, BladeBond b) {
		int reached = BladeRules.tier(b.growth().resonance(), b.history().count(BladeRules.BOSSES), Aura.stage(player));
		BladeBond next = b;
		for (int t = b.tier() + 1; t <= reached; t++) {
			next = tierUp(player, blade, next, t);
		}
		return next;
	}

	/** {@code b} reaching {@code tier}: what it takes (a name, an offer of traits) and the moment, told to everyone near. */
	private static BladeBond tierUp(ServerPlayer player, ItemStack blade, BladeBond b, int tier) {
		long day = day(player.level());
		BladeBond.Growth g = b.growth().withTier(tier);
		List<BladeBond.Deed> deeds = new ArrayList<>();
		deeds.add(new BladeBond.Deed(BladeRules.DEED_TIER, BladeRules.tierId(tier), day));
		BladeRules.History habits = b.habits(Aura.data(player).method(), Ways.state(player).way());
		switch (tier) {
			case BladeRules.NAMED -> {
				if (g.name().isEmpty()) {
					String name = BladeRules.suggest(b.nameSeed(Aura.element(player), Ways.state(player).way()), 0);
					g = g.withName(name, day);
					deeds.add(new BladeBond.Deed(BladeRules.DEED_NAMED, name, day));
				}
			}
			case BladeRules.AWAKENED -> g = g.withOffer(BladeRules.offer(habits, b.seed()), false);
			case BladeRules.SOULFORGED -> g = g.withOffer(BladeRules.offer(habits, b.seed() ^ 0x5011F0461EL), !g.trait().isEmpty());
			default -> {}
		}
		BladeBond next = b.withGrowth(g).withHistory(b.history().plus(Map.of(), List.of(), deeds));
		BladeCeremony.tierMoment(player, next, tier);
		Grimoire.unlock(player, "aura:blade_" + BladeRules.tierId(tier));
		ItemStack shown = blade.copy();
		shown.set(BOND, next);
		for (AuraApi.BladeHook hook : AuraApi.bladeHooks()) {
			try {
				hook.tiered(player, shown, tier);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A bonded blade hook threw; skipping it", e);
			}
		}
		tierUps++;
		return next;
	}

	/** Keeps {@code player}'s record of their blade current (and the blade's colour and their name on it), about once a second. */
	private static void look(ServerPlayer player) {
		State s = state(player);
		UUID id = s.bondId();
		if (id == null) {
			return;
		}
		int slot = slotOf(player);
		if (slot < 0) {
			if (!s.away()) {
				player.setAttached(STATE, s.with(s.shown(), true, null, null));
			}
			return;
		}
		ItemStack blade = player.getInventory().getItem(slot);
		BladeBond b = bond(blade);
		if (b == null) {
			return;
		}
		// It takes on its swordsman's colour (a new method, a stage's deeper shade), and their name as it is now.
		int color = Aura.color(player);
		BladeBond next = b;
		if (color != 0 && color != b.color()) {
			next = next.withGrowth(next.growth().withColor(color));
		}
		String name = player.getGameProfile().name();
		if (!name.equals(b.who().ownerName())) {
			next = next.withWho(new BladeBond.Who(b.id(), b.owner(), name, b.who().lineage()));
		}
		next = grow(player, blade, next);
		if (!next.equals(b)) {
			blade.set(BOND, next);
		}
		Shown shown = new Shown(BuiltInRegistries.ITEM.getKey(blade.getItem()).toString(),
			Optional.ofNullable(blade.get(DataComponents.ITEM_MODEL)).map(Object::toString).orElse(""), next.shownName(), next.tier(), next.color(),
			next.growth().resonance(), next.growth().trait());
		State updated = s.with(shown, false, player.level(), player.blockPosition());
		if (!updated.equals(s) && (s.away() || !shown.equals(s.shown()) || !updated.lastDim().equals(s.lastDim())
				|| player.blockPosition().distManhattan(new BlockPos(s.lastX(), s.lastY(), s.lastZ())) > 8)) {
			player.setAttached(STATE, updated);
		}
	}

	// ------------------------------------------------------------------ each tick: whose blade is where (server)

	private static void tick(MinecraftServer server) {
		long tick = server.getTickCount();
		BladeRegistry registry = BladeRegistry.of(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive()) {
				continue;
			}
			scan(player, registry, (tick + player.getId()) % 10 == 0);
			if ((tick + player.getId()) % WRITE_EVERY == 0) {
				flush(player);
				look(player);
			}
		}
		BladeCeremony.tick(server);
	}

	/**
	 * Looks through {@code player}'s inventory (and, {@code deep}, inside the boxes and bundles they carry) for bonded blades: someone
	 * else's slips home; one of theirs whose bond is over is only steel now; a second copy of their own (only a creative copy makes one)
	 * is only steel too.
	 */
	public static void scan(ServerPlayer player, BladeRegistry registry, boolean deep) {
		Inventory inv = player.getInventory();
		State s = state(player);
		UUID current = s.bondId();
		UUID me = player.getUUID();
		boolean found = false;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			BladeBond b = bond(stack);
			if (b == null) {
				if (deep && (stack.has(DataComponents.CONTAINER) || stack.has(DataComponents.BUNDLE_CONTENTS))) {
					deep(player, registry, stack);
				}
				continue;
			}
			if (b.ownedBy(me)) {
				if (current != null && b.id().equals(current) && !found) {
					found = true;
				} else if (current == null && registry.activeFor(b.id(), me)) {
					// Theirs by the registry, though their own record lost it (an old save): theirs again.
					player.setAttached(STATE, new State(b.id().toString(), Shown.NONE, "", 0, 0, 0, false));
					current = b.id();
					found = true;
				} else {
					end(stack, b);
				}
			} else if (registry.active(b.id())) {
				ItemStack moving = stack.copy();
				inv.setItem(i, ItemStack.EMPTY);
				homeward(player, moving, b);
			} else {
				end(stack, b);
			}
		}
		// What they hold on the cursor in a menu is theirs to hold only if it's theirs.
		ItemStack carried = player.containerMenu.getCarried();
		BladeBond cb = bond(carried);
		if (cb != null && !cb.ownedBy(me) && registry.active(cb.id())) {
			ItemStack moving = carried.copy();
			player.containerMenu.setCarried(ItemStack.EMPTY);
			homeward(player, moving, cb);
		}
	}

	/** Someone else's bonded blade inside a box or bundle {@code player} carries: taken out and sent home. */
	private static void deep(ServerPlayer player, BladeRegistry registry, ItemStack box) {
		UUID me = player.getUUID();
		ItemContainerContents contents = box.get(DataComponents.CONTAINER);
		if (contents != null && contents.size() > 0) {
			// The box's slots as copies: a blade going home leaves its slot empty, and the box takes the rest back as they were.
			List<ItemStack> slots = new ArrayList<>();
			List<ItemStack> home = new ArrayList<>();
			for (ItemStack inside : contents.itemCopies().toList()) {
				BladeBond b = bond(inside);
				if (b != null && !b.ownedBy(me) && registry.active(b.id())) {
					home.add(inside);
					slots.add(ItemStack.EMPTY);
				} else {
					slots.add(inside);
				}
			}
			if (!home.isEmpty()) {
				box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
				for (ItemStack blade : home) {
					homeward(player, blade, bond(blade));
				}
			}
		}
		BundleContents bundle = box.get(DataComponents.BUNDLE_CONTENTS);
		if (bundle != null && !bundle.isEmpty()) {
			List<ItemStack> keep = new ArrayList<>();
			List<ItemStack> home = new ArrayList<>();
			for (ItemStack inside : bundle.itemCopies().toList()) {
				BladeBond b = bond(inside);
				if (b != null && !b.ownedBy(me) && registry.active(b.id())) {
					home.add(inside);
				} else {
					keep.add(inside);
				}
			}
			if (!home.isEmpty()) {
				box.set(DataComponents.BUNDLE_CONTENTS, bundle.copyWithContents(keep.stream()));
				for (ItemStack blade : home) {
					homeward(player, blade, bond(blade));
				}
			}
		}
	}

	/** {@code stack}'s bond {@code b} is over: it's only steel now, remembering whose it was. */
	static void end(ItemStack stack, BladeBond b) {
		stack.remove(BOND);
		stack.set(FORMER, new BladeBond.Former(b.shownName(), b.who().ownerName(), b.tier()));
	}

	/** A copy of {@code stack} with its bond over (a blade lying somewhere whose bond ended while it was away), or {@code stack} if it has none. */
	public static ItemStack lapse(ItemStack stack) {
		BladeBond b = bond(stack);
		if (b == null) {
			return stack;
		}
		ItemStack copy = stack.copy();
		end(copy, b);
		return copy;
	}

	// ------------------------------------------------------------------ going home (server)

	/**
	 * Someone else's bonded blade, taken out of {@code holder}'s hands (already: it's {@code blade} now, nowhere else): it goes home to
	 * its swordsman, into their inventory (at their feet if it's full), or waits for them in the registry if they aren't here. Both are
	 * told, and everyone near sees it go.
	 */
	static void homeward(ServerPlayer holder, ItemStack blade, BladeBond b) {
		MinecraftServer server = holder.level().getServer();
		ServerPlayer owner = server.getPlayerList().getPlayer(b.owner());
		Vec3 from = holder.position().add(0, 1.1, 0);
		deliver(server, owner, b.owner(), blade);
		Component name = name(blade, b);
		int color = 0xFF000000 | (b.color() == 0 ? 0xC8C0E0 : b.color());
		holder.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.slips", name.copy().withColor(color),
			Component.literal(b.who().ownerName())).withColor(0xC8B8A0));
		if (owner != null && owner != holder) {
			owner.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.came_home", name.copy().withColor(color),
				Component.literal(holder.getGameProfile().name())).withColor(0xE8D8B0));
			BladeCeremony.homeFx(holder.level(), from, owner, b.color());
		} else {
			BladeCeremony.homeFx(holder.level(), from, null, b.color());
		}
		returns++;
	}

	/** Gives {@code blade} to {@code owner} (online: their inventory, or their feet), or keeps it for them ({@code ownerId}) until they're here. */
	static void deliver(MinecraftServer server, ServerPlayer owner, UUID ownerId, ItemStack blade) {
		if (owner != null && owner.isAlive() && !owner.hasDisconnected()) {
			give(owner, blade, -1);
		} else {
			BladeRegistry.of(server).holdFor(ownerId, blade);
		}
	}

	/** Puts {@code blade} in {@code player}'s inventory (slot {@code slot} if it's free), or at their feet, only theirs to pick up. */
	static void give(ServerPlayer player, ItemStack blade, int slot) {
		Inventory inv = player.getInventory();
		if (slot >= 0 && slot < inv.getContainerSize() && inv.getItem(slot).isEmpty()) {
			inv.setItem(slot, blade);
			return;
		}
		if (!inv.add(blade) && !blade.isEmpty()) {
			ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY() + 0.3, player.getZ(), blade, 0, 0.1, 0);
			player.level().addFreshEntity(drop);
		}
	}

	/** The blades waiting for {@code player}, handed over (as they join, or as they come back from the dead). */
	private static void collect(ServerPlayer player) {
		List<ItemStack> waiting = BladeRegistry.of(player.level().getServer()).takeFor(player.getUUID());
		for (ItemStack blade : waiting) {
			give(player, blade, -1);
			BladeBond b = bond(blade);
			if (b != null) {
				player.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.waited", name(blade, b).copy()
					.withColor(0xFF000000 | b.color())).withColor(0xE8D8B0));
			}
		}
	}

	/**
	 * A bonded blade lying on the ground has fallen out of the world: it comes home instead (from {@code mixin.ItemEntityBondMixin}).
	 * Returns whether it did (a blade whose bond is over falls as any other).
	 */
	public static boolean fellOut(ItemEntity entity) {
		BladeBond b = bond(entity.getItem());
		if (b == null || !(entity.level() instanceof ServerLevel level) || !BladeRegistry.of(level.getServer()).active(b.id())) {
			return false;
		}
		ItemStack blade = entity.getItem().copy();
		entity.setItem(ItemStack.EMPTY);
		entity.discard();
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(b.owner());
		deliver(level.getServer(), owner, b.owner(), blade);
		if (owner != null) {
			owner.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.from_the_void", name(blade, b).copy()
				.withColor(0xFF000000 | b.color())).withColor(0xE8D8B0));
			BladeCeremony.homeFx(owner.level(), owner.position().add(0, 6, 0), owner, b.color());
		}
		returns++;
		return true;
	}

	/** Whether a bonded blade on the ground ({@code stack}) is still bonded: protected and kept for its swordsman. Server only. */
	public static boolean stands(ServerLevel level, ItemStack stack) {
		BladeBond b = bond(stack);
		return b != null && BladeRegistry.of(level.getServer()).active(b.id());
	}

	/** Whether {@code player} may pick up the bonded blade {@code stack} lying on the ground: only its swordsman. */
	public static boolean mayPickUp(Player player, ItemStack stack) {
		BladeBond b = bond(stack);
		return b == null || b.ownedBy(player.getUUID());
	}

	/**
	 * Whether {@code player} may take {@code stack} out of a container that isn't their own inventory: anything but someone else's
	 * bonded blade (on the server, one whose bond still stands; a client, not knowing, says no and the server corrects it).
	 */
	public static boolean mayTake(Player player, ItemStack stack) {
		BladeBond b = bond(stack);
		if (b == null || b.ownedBy(player.getUUID()) || player.isCreative()) {
			return true;
		}
		if (player instanceof ServerPlayer server) {
			return !BladeRegistry.of(server.level().getServer()).active(b.id());
		}
		return false;
	}

	// ------------------------------------------------------------------ through death (server)

	/**
	 * The swordsman is dying and their inventory is about to drop (from the head of {@code Player.dropEquipment}, only without
	 * keepInventory): their own blade is taken out first and kept for the next body (Curse of Vanishing or not), and anyone else's blade
	 * they somehow carry goes home now rather than to the ground.
	 */
	public static void dying(Player dying) {
		if (!(dying instanceof ServerPlayer player)) {
			return;
		}
		BladeRegistry registry = BladeRegistry.of(player.level().getServer());
		List<Kept> kept = new ArrayList<>(player.getAttachedOrElse(KEPT, List.of()));
		Inventory inv = player.getInventory();
		UUID current = state(player).bondId();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			BladeBond b = bond(stack);
			if (b == null) {
				continue;
			}
			if (b.ownedBy(player.getUUID()) && (registry.activeFor(b.id(), player.getUUID()) || b.id().equals(current))) {
				kept.add(new Kept(i, stack.copy()));
				inv.setItem(i, ItemStack.EMPTY);
			} else if (!b.ownedBy(player.getUUID()) && registry.active(b.id())) {
				ItemStack moving = stack.copy();
				inv.setItem(i, ItemStack.EMPTY);
				homeward(player, moving, b);
			}
		}
		ItemStack carried = player.containerMenu.getCarried();
		BladeBond cb = bond(carried);
		if (cb != null && cb.ownedBy(player.getUUID()) && registry.activeFor(cb.id(), player.getUUID())) {
			kept.add(new Kept(-1, carried.copy()));
			player.containerMenu.setCarried(ItemStack.EMPTY);
		}
		if (!kept.isEmpty()) {
			player.setAttached(KEPT, List.copyOf(kept));
			deathsKept++;
		}
	}

	/**
	 * A dead swordsman's blade about to be dropped some other way (the cursor's, put down as a menu closes on the dead): kept instead
	 * (from {@code mixin.LivingEntityBondDropMixin}). Returns whether it was.
	 */
	public static boolean keepFromDead(LivingEntity entity, ItemStack stack) {
		if (!(entity instanceof ServerPlayer player) || player.isAlive()) {
			return false;
		}
		BladeBond b = bond(stack);
		if (b == null || !b.ownedBy(player.getUUID()) || !BladeRegistry.of(player.level().getServer()).activeFor(b.id(), player.getUUID())) {
			return false;
		}
		List<Kept> kept = new ArrayList<>(player.getAttachedOrElse(KEPT, List.of()));
		kept.add(new Kept(-1, stack.copy()));
		stack.setCount(0);
		player.setAttached(KEPT, List.copyOf(kept));
		return true;
	}

	/** The new body: the blades its death kept, back where they were; anything left on the old body of theirs comes too. */
	private static void respawned(ServerPlayer old, ServerPlayer body) {
		List<Kept> kept = body.getAttachedOrElse(KEPT, List.of());
		body.removeAttached(KEPT);
		old.removeAttached(KEPT);
		for (Kept k : kept) {
			if (!k.stack().isEmpty()) {
				give(body, k.stack().copy(), k.slot());
			}
		}
		// Put back into the dead body's inventory after it dropped everything (a menu closing on the dead): it comes over too.
		Inventory inv = old.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			BladeBond b = bond(stack);
			if (b != null && b.ownedBy(body.getUUID())) {
				ItemStack moving = stack.copy();
				inv.setItem(i, ItemStack.EMPTY);
				give(body, moving, i);
			}
		}
		collect(body);
	}

	// ------------------------------------------------------------------ bonding, naming, choosing, releasing, passing (server)

	/** Why {@code player} can't bond the blade in their main hand now, as a language key, or null when they can (the ceremony asks). */
	public static String bondRefusal(ServerPlayer player) {
		if (!on(player)) {
			return "message.wildercord.aura.blade.off";
		}
		if (Aura.stage(player) < BladeRules.FROM) {
			return "message.wildercord.aura.blade.edge";
		}
		if (!canBond(player.getMainHandItem())) {
			return "message.wildercord.aura.blade.not_bondable";
		}
		if (standing(player)) {
			return "message.wildercord.aura.blade.already";
		}
		return null;
	}

	/** Whether {@code player} has a bond that still stands (the registry agreeing with their record). */
	public static boolean standing(ServerPlayer player) {
		UUID id = state(player).bondId();
		if (id == null) {
			return false;
		}
		if (BladeRegistry.of(player.level().getServer()).activeFor(id, player.getUUID())) {
			return true;
		}
		// Their record names a bond the world no longer holds: let it go.
		player.setAttached(STATE, State.NONE);
		return false;
	}

	/**
	 * Bonds the blade in {@code player}'s {@code hand} to them now ({@code how}: "ceremony", or an add-on's own word), with every check
	 * but the ceremony itself. Returns whether it was bonded.
	 */
	public static boolean bond(ServerPlayer player, InteractionHand hand, String how) {
		ItemStack stack = player.getItemInHand(hand);
		if (!on(player) || Aura.stage(player) < BladeRules.FROM || !canBond(stack) || standing(player)) {
			return false;
		}
		ServerLevel level = player.level();
		BlockPos at = player.blockPosition();
		String biome = level.getBiome(at).unwrapKey().map(k -> k.identifier().toString()).orElse("");
		BladeBond.Origin origin = new BladeBond.Origin(day(level), biome, level.dimension().identifier().toString(), at.getX(), at.getY(), at.getZ(),
			Aura.data(player).method(), how);
		UUID id = UUID.randomUUID();
		BladeBond b = BladeBond.fresh(id, player.getUUID(), player.getGameProfile().name(), Aura.color(player), origin);
		stack.remove(FORMER);
		stack.set(BOND, b);
		BladeRegistry.of(level.getServer()).register(id, player.getUUID(), player.getGameProfile().name(), level.getGameTime());
		player.setAttached(STATE, new State(id.toString(), Shown.NONE, level.dimension().identifier().toString(), at.getX(), at.getY(), at.getZ(), false));
		PENDING.remove(player.getUUID());
		FOES.remove(player.getUUID());
		Grimoire.unlock(player, "aura:bond");
		look(player);
		for (AuraApi.BladeHook hook : AuraApi.bladeHooks()) {
			try {
				hook.bonded(player, stack);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A bonded blade hook threw; skipping it", e);
			}
		}
		bonds++;
		return true;
	}

	/** Why {@code player} can't name their blade {@code typed}, or null when they can. */
	public static String nameRefusal(ServerPlayer player, String typed) {
		ItemStack blade = carried(player);
		BladeBond b = bond(blade);
		if (b == null) {
			return "message.wildercord.aura.blade.not_with_you";
		}
		if (BladeRules.effective(b.tier(), Aura.stage(player)) < BladeRules.NAMED) {
			return "message.wildercord.aura.blade.name_tier";
		}
		String name = BladeRules.cleanName(typed);
		if (name.isEmpty()) {
			return "message.wildercord.aura.blade.name_empty";
		}
		return null;
	}

	/** Names {@code player}'s blade {@code typed} (made safe), from Named. Returns whether it took the name. */
	public static boolean rename(ServerPlayer player, String typed) {
		String why = nameRefusal(player, typed);
		if (why != null) {
			player.sendOverlayMessage(Component.translatable(why).withColor(0xA89CC8));
			return false;
		}
		flush(player);
		ItemStack blade = carried(player);
		BladeBond b = bond(blade);
		String name = BladeRules.cleanName(typed);
		if (b == null || name.equals(b.growth().name())) {
			return false;
		}
		long day = day(player.level());
		BladeBond next = b.withGrowth(b.growth().withName(name, day))
			.withHistory(b.history().plus(Map.of(), List.of(), List.of(new BladeBond.Deed(BladeRules.DEED_NAMED, name, day))));
		blade.set(BOND, next);
		BladeCeremony.namedMoment(player, next);
		look(player);
		for (AuraApi.BladeHook hook : AuraApi.bladeHooks()) {
			try {
				hook.named(player, blade, name);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A bonded blade hook threw; skipping it", e);
			}
		}
		return true;
	}

	/**
	 * Gives {@code player}'s blade the trait {@code trait} from its offer: the first choice free, and once more free at Soulforged; any
	 * other change {@link BladeRules#RECHOOSE_LEVELS} experience levels (nothing in creative). Returns whether it took it.
	 */
	public static boolean choose(ServerPlayer player, String trait) {
		flush(player);
		ItemStack blade = carried(player);
		BladeBond b = bond(blade);
		if (b == null) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.blade.not_with_you").withColor(0xA89CC8));
			return false;
		}
		if (BladeRules.effective(b.tier(), Aura.stage(player)) < BladeRules.AWAKENED || !b.growth().offer().contains(trait)
				|| BladeRules.trait(trait).isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.blade.trait_not_offered").withColor(0xA89CC8));
			return false;
		}
		BladeBond.Growth g = b.growth();
		if (trait.equals(g.trait())) {
			return false;
		}
		boolean free = g.trait().isEmpty() || g.rechoose();
		if (!free && !player.isCreative()) {
			if (player.experienceLevel < BladeRules.RECHOOSE_LEVELS) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.aura.blade.trait_levels", BladeRules.RECHOOSE_LEVELS).withColor(0xA89CC8));
				return false;
			}
			player.giveExperienceLevels(-BladeRules.RECHOOSE_LEVELS);
		}
		String art = trait.equals(BladeRules.WELL_WORN) ? BladeRules.favourite(b.history().arts()) : "";
		BladeBond next = b.withGrowth(g.withTrait(trait, art, false));
		blade.set(BOND, next);
		BladeCeremony.traitMoment(player, next);
		look(player);
		for (AuraApi.BladeHook hook : AuraApi.bladeHooks()) {
			try {
				hook.traited(player, blade, trait);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A bonded blade hook threw; skipping it", e);
			}
		}
		return true;
	}

	/**
	 * Releases {@code player}'s bond: the registry lets it go, and the blade (here, or whenever it next turns up) is only steel again,
	 * remembering whose it was. Returns whether there was a bond.
	 */
	public static boolean release(ServerPlayer player) {
		State s = state(player);
		UUID id = s.bondId();
		if (id == null) {
			return false;
		}
		BladeRegistry.of(player.level().getServer()).end(id);
		ItemStack blade = carried(player);
		BladeBond b = bond(blade);
		Component name = b == null ? Component.literal(s.shown().name().isEmpty() ? "-" : s.shown().name()) : name(blade, b);
		if (b != null) {
			end(blade, b);
		}
		player.setAttached(STATE, State.NONE);
		PENDING.remove(player.getUUID());
		BladeCeremony.releaseMoment(player, name, b == null ? s.shown().color() : b.color());
		for (AuraApi.BladeHook hook : AuraApi.bladeHooks()) {
			try {
				hook.released(player, id);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A bonded blade hook threw; skipping it", e);
			}
		}
		return true;
	}

	/** Why {@code from} can't pass their blade to {@code to} now, as a language key, or null when they can (the rules asked when {@code rules}). */
	public static String passRefusal(ServerPlayer from, ServerPlayer to, boolean rules) {
		if (to == null || to == from || !to.isAlive() || to.isSpectator()) {
			return "message.wildercord.aura.blade.pass_nobody";
		}
		if (!on(from)) {
			return "message.wildercord.aura.blade.off";
		}
		if (!standing(from) || carried(from).isEmpty()) {
			return "message.wildercord.aura.blade.not_with_you";
		}
		if (standing(to)) {
			return "message.wildercord.aura.blade.pass_bonded";
		}
		if (Aura.stage(to) <= AuraRules.NONE) {
			return "message.wildercord.aura.blade.pass_no_path";
		}
		if (rules && !AuraApi.mayPassBlade(from, to)) {
			return "message.wildercord.aura.blade.pass_not_disciple";
		}
		return null;
	}

	/**
	 * Passes {@code from}'s bonded blade to {@code to} (a disciple, at the end of the passing ceremony, or an add-on's own rite): the
	 * blade is moved from one inventory to the other in one go, bonded to its new swordsman with its tier, name, trait and story kept,
	 * and the passing written into its lineage. {@code rules}: whether the passing rules ({@link AuraApi#allowBladePassing}) must allow
	 * it. Returns whether it passed.
	 */
	public static boolean pass(ServerPlayer from, ServerPlayer to, boolean rules) {
		String why = passRefusal(from, to, rules);
		if (why != null) {
			from.sendOverlayMessage(Component.translatable(why).withColor(0xA89CC8));
			return false;
		}
		flush(from);
		int slot = slotOf(from);
		ItemStack blade = from.getInventory().getItem(slot).copy();
		BladeBond b = bond(blade);
		if (b == null) {
			return false;
		}
		from.getInventory().setItem(slot, ItemStack.EMPTY);
		long day = day(from.level());
		List<String> lineage = new ArrayList<>(b.who().lineage());
		lineage.add(from.getGameProfile().name());
		int color = Aura.color(to) == 0 ? b.color() : Aura.color(to);
		BladeBond next = b.withWho(new BladeBond.Who(b.id(), to.getUUID(), to.getGameProfile().name(), lineage))
			.withGrowth(b.growth().withColor(color))
			.withHistory(b.history().plus(Map.of(), List.of(), List.of(new BladeBond.Deed(BladeRules.DEED_PASSED,
				from.getGameProfile().name() + ">" + to.getGameProfile().name(), day))));
		blade.set(BOND, next);
		BladeRegistry.of(from.level().getServer()).moveTo(b.id(), to.getUUID(), to.getGameProfile().name());
		from.setAttached(STATE, State.NONE);
		to.setAttached(STATE, new State(b.id().toString(), Shown.NONE, "", 0, 0, 0, false));
		PENDING.remove(from.getUUID());
		if (to.getMainHandItem().isEmpty()) {
			to.setItemInHand(InteractionHand.MAIN_HAND, blade);
		} else {
			give(to, blade, -1);
		}
		look(to);
		BladeCeremony.passedMoment(from, to, next);
		for (AuraApi.BladeHook hook : AuraApi.bladeHooks()) {
			try {
				hook.passed(from, to, blade);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A bonded blade hook threw; skipping it", e);
			}
		}
		passes++;
		return true;
	}

	/** {@code b}'s name as shown: its own name in its colour, or the weapon's name before it has one. */
	public static Component name(ItemStack blade, BladeBond b) {
		String name = b.shownName();
		return name.isEmpty() ? Component.translatable(blade.getItem().getDescriptionId()) : Component.literal(name);
	}

	/**
	 * Adds resonance to {@code player}'s blade from outside the fight (an add-on's deed, the world's own places), in hand or not:
	 * {@code amount} through the server's rate and the hooks. Returns what it gathered.
	 */
	public static double addResonance(ServerPlayer player, double amount, String source) {
		if (!carrying(player)) {
			return 0;
		}
		double got = gain(player, amount, source, null, false);
		flushSoon(player);
		return got;
	}

	/** Sets {@code player}'s blade's resonance outright (an operator's command and the game tests). */
	public static boolean setResonance(ServerPlayer player, double resonance) {
		flush(player);
		ItemStack blade = carried(player);
		BladeBond b = bond(blade);
		if (b == null) {
			return false;
		}
		BladeBond next = b.withGrowth(b.growth().withResonance((float) Math.max(0, resonance)));
		blade.set(BOND, grow(player, blade, next));
		look(player);
		return true;
	}

	/** Adds to the counts of {@code player}'s blade outright (the game tests shape a history): {@code counts} and {@code arts} played. */
	public static boolean addHistory(ServerPlayer player, Map<String, Integer> counts, Map<String, Integer> arts) {
		flush(player);
		ItemStack blade = carried(player);
		BladeBond b = bond(blade);
		if (b == null) {
			return false;
		}
		List<String> played = new ArrayList<>();
		arts.forEach((id, n) -> {
			for (int i = 0; i < n; i++) {
				played.add(id);
			}
		});
		blade.set(BOND, b.withHistory(b.history().plus(counts, played, List.of())));
		return true;
	}

	/** Gives {@code player}'s blade {@code trait} outright, offered or not (an operator's command and the game tests). */
	public static boolean forceTrait(ServerPlayer player, String trait) {
		flush(player);
		ItemStack blade = carried(player);
		BladeBond b = bond(blade);
		if (b == null || BladeRules.trait(trait).isEmpty()) {
			return false;
		}
		List<String> offer = new ArrayList<>(b.growth().offer());
		if (!offer.contains(trait)) {
			offer.add(0, trait);
		}
		String art = trait.equals(BladeRules.WELL_WORN) ? BladeRules.favourite(b.history().arts()) : "";
		blade.set(BOND, b.withGrowth(b.growth().withOffer(offer.subList(0, Math.min(BladeRules.OFFER, offer.size())), false).withTrait(trait, art, false)));
		look(player);
		return true;
	}

	/** Takes the trait off {@code player}'s blade and draws its offer again from its history (the game tests and operators). */
	public static boolean reoffer(ServerPlayer player) {
		ItemStack blade = carried(player);
		BladeBond b = bond(blade);
		if (b == null) {
			return false;
		}
		BladeRules.History habits = b.habits(Aura.data(player).method(), Ways.state(player).way());
		blade.set(BOND, b.withGrowth(b.growth().withTrait("", "", false).withOffer(BladeRules.offer(habits, b.seed()), false)));
		return true;
	}

	// ------------------------------------------------------------------ requests from the page

	/** Client to server: name the blade {@code name} (as typed; the server makes it safe). */
	public record Name(String name) implements CustomPacketPayload {
		public static final Type<Name> TYPE = new Type<>(Wildercord.id("blade_name"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Name> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(96), Name::name, Name::new).cast();

		@Override
		public Type<Name> type() {
			return TYPE;
		}
	}

	/** Client to server: give the blade trait {@code trait} from its offer. */
	public record Choose(String trait) implements CustomPacketPayload {
		public static final Type<Choose> TYPE = new Type<>(Wildercord.id("blade_trait"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Choose> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(96), Choose::trait, Choose::new).cast();

		@Override
		public Type<Choose> type() {
			return TYPE;
		}
	}

	/** Client to server: release the bond (the page asks twice first). */
	public record Release(String bond) implements CustomPacketPayload {
		public static final Type<Release> TYPE = new Type<>(Wildercord.id("blade_release"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Release> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(48), Release::bond, Release::new).cast();

		@Override
		public Type<Release> type() {
			return TYPE;
		}
	}

	private static final PacketThrottle REQUESTS = new PacketThrottle(4, 10);

	// ------------------------------------------------------------------ for the game tests

	private static int bonds;
	private static int tierUps;
	private static int returns;
	private static int passes;
	private static int deathsKept;

	/** Since the server started: bonds made, tiers reached, blades sent home, blades passed, deaths a blade was kept through. */
	public static int bondsMade() {
		return bonds;
	}

	public static int tierUps() {
		return tierUps;
	}

	public static int returns() {
		return returns;
	}

	public static int passes() {
		return passes;
	}

	public static int deathsKept() {
		return deathsKept;
	}

	// ------------------------------------------------------------------ lifecycle

	static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Name.TYPE, Name.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Choose.TYPE, Choose.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Release.TYPE, Release.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Name.TYPE, (payload, context) -> {
			if (REQUESTS.allow(context.player().getUUID(), context.server().getTickCount())) {
				rename(context.player(), payload.name());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Choose.TYPE, (payload, context) -> {
			if (REQUESTS.allow(context.player().getUUID(), context.server().getTickCount())) {
				choose(context.player(), payload.trait());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Release.TYPE, (payload, context) -> {
			if (REQUESTS.allow(context.player().getUUID(), context.server().getTickCount()) && payload.bond().equals(state(context.player()).bond())) {
				release(context.player());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(BondedBlades::tick);
		// The deeds heard through aura's own hooks.
		AuraApi.onFinisher(new AuraApi.FinisherHook() {
			@Override
			public void landed(ServerPlayer attacker, LivingEntity target, AuraApi.Finisher finisher, float dealt) {
				finished(attacker, target);
				BladeTraits.finished(attacker, target);
			}
		});
		AuraApi.onStanceBroken(BondedBlades::broke);
		AuraApi.onAwakening(new AuraApi.AwakeningHook() {
			@Override
			public void awakened(ServerPlayer player, int ticks) {
				BondedBlades.awakened(player);
			}
		});
		AuraApi.onTechnique(new AuraApi.TechniqueHook() {
			@Override
			public void ranked(ServerPlayer player, int slot, Techniques.Written technique, int rank) {
				if (rank >= TechniqueRules.PEERLESS && carrying(player)) {
					gain(player, BladeRules.PEERLESS, "peerless", null, false);
					deed(player, BladeRules.DEED_PEERLESS, technique.shownName());
					flushSoon(player);
				}
			}
		});
		AuraApi.onWay(new AuraApi.WayHook() {
			@Override
			public void chosen(ServerPlayer player, AuraApi.Way way, boolean first) {
				if (carrying(player)) {
					deed(player, BladeRules.DEED_WAY, way.id());
					flushSoon(player);
				}
			}
		});
		// Named and Soulforged blades draw a little more aura from blows; Moonwake more by night.
		AuraApi.onGain((player, amount, source) -> source.equals("hit") ? amount * BladeTraits.hitGain(player) : amount);
		AuraApi.onStance(BladeTraits::stance);
		// Through death: the new body gets what the death kept (after Fabric has copied the attachments over).
		ServerPlayerEvents.AFTER_RESPAWN.register(Aura.AFTER_COPY, (oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				respawned(oldPlayer, newPlayer);
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
			if (handler.player.isAlive()) {
				collect(handler.player);
			}
		}));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			flush(handler.player);
			PENDING.remove(id);
			FOES.remove(id);
			REQUESTS.forget(id);
			BladeTraits.forget(id);
			BladeCeremony.forget(id);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PENDING.clear();
			FOES.clear();
			FALLEN.clear();
			REQUESTS.clear();
			BladeTraits.clear();
			BladeCeremony.clear();
		});
	}

	/** Kit sounds the bond plays, for the tests. */
	public static final List<String> SOUNDS = List.of("aura_bond_kindle", "aura_bond_join", "aura_bond_seal", "aura_bond_fail", "aura_bond_tier",
		"aura_bond_name", "aura_bond_trait", "aura_bond_home", "aura_bond_release", "aura_bond_pass");

	/** Plays one of the bond's sounds at {@code at}. */
	static void sound(ServerLevel level, Vec3 at, String name, float volume, float pitch) {
		Feels.sound(level, at, name, volume, pitch);
	}

	/** A puff of the blade's colour at {@code at}. */
	static void motes(ServerLevel level, Vec3 at, int color, int count) {
		Motes.glows(level, at, count, 0.25, color, 0.12, 24, new Vec3(0, 0.02, 0), 0.03);
	}
}
