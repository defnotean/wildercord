package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.config.Config;
import dev.wildercord.net.PacketThrottle;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Techniques of a swordsman's own, at runtime (the rules and every number are {@link TechniqueRules}; a technique performed is
 * {@code aura.arts.TechniqueArts}). What a swordsman has learned and written is their {@link Book} ({@link #BOOK}: saved, kept through
 * death, synced to its owner): the parts learned for good, the techniques written into their slots (one at Edge, two at Form, three at
 * Sovereign), and each technique's record (how far it has been honed, its temper and its edge), kept by its three parts so renaming it,
 * restringing it, or writing it again later brings its rank back.
 *
 * <p>Written techniques play as sword strings through {@link AuraApi#addStringSource}: each is an art of its writer's own, priced and
 * rested by its parts, named by its writer ({@link #name}). The writing page (the client's) asks for a technique written ({@link Write}),
 * erased ({@link Erase}), tempered or edged ({@link Choose}) or set down on a scroll ({@link Inscribe}); the server checks each against
 * everything it knows ({@link #refusal}, the same the page asks first) and does it.</p>
 *
 * <p>Parts are learned from technique scrolls ({@code aura.world.TechniqueScrollItem}), from a duelist beaten ({@link #duelistLesson}),
 * or lent by a Way while its Edge node is in force ({@link #lent}); every blade knows the draw, the cut on the blade and its method's own
 * element once it reaches Edge ({@link TechniqueRules#INNATE}).</p>
 */
public final class Techniques {
	private Techniques() {}

	// ------------------------------------------------------------------ the state

	/**
	 * A technique as written: its name, its three parts, and its string (written out, {@link SwordString#text}). An empty slot is
	 * {@link #NONE}.
	 */
	public record Written(String name, String stroke, String release, String intent, String string) {
		public static final Written NONE = new Written("", "", "", "", "");

		public Written {
			name = name == null ? "" : name;
			stroke = stroke == null ? "" : stroke;
			release = release == null ? "" : release;
			intent = intent == null ? "" : intent;
			string = string == null ? "" : string;
		}

		public static final Codec<Written> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("name", "").forGetter(Written::name),
			Codec.STRING.optionalFieldOf("stroke", "").forGetter(Written::stroke),
			Codec.STRING.optionalFieldOf("release", "").forGetter(Written::release),
			Codec.STRING.optionalFieldOf("intent", "").forGetter(Written::intent),
			Codec.STRING.optionalFieldOf("string", "").forGetter(Written::string)
		).apply(i, Written::new));

		public static final StreamCodec<ByteBuf, Written> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(128), Written::name, ByteBufCodecs.stringUtf8(64), Written::stroke, ByteBufCodecs.stringUtf8(64), Written::release,
			ByteBufCodecs.stringUtf8(64), Written::intent, ByteBufCodecs.stringUtf8(64), Written::string, Written::new);

		/** Whether nothing is written here. */
		public boolean empty() {
			return stroke.isEmpty();
		}

		/** The key its record is kept by (its three parts). */
		public String key() {
			return TechniqueRules.key(stroke, release, intent);
		}

		/** Its string, or empty if what's written isn't one (a damaged record). */
		public Optional<SwordString> sword() {
			try {
				return string.isEmpty() ? Optional.empty() : Optional.of(SwordString.parse(string));
			} catch (IllegalArgumentException e) {
				return Optional.empty();
			}
		}

		/** Its parts, stroke, release and intent. */
		public List<String> parts() {
			return List.of(stroke, release, intent);
		}

		/** Its name as anyone is shown it (made safe again, whatever was stored). */
		public String shownName() {
			return TechniqueRules.cleanName(name);
		}
	}

	/**
	 * How far a technique has been honed, kept by its parts ({@link Written#key}): its experience, what of it came from practice, the
	 * temper and edge its writer chose ("" as written), and when it was last written or used (game time: the oldest records go first).
	 */
	public record Honing(String key, float xp, float practice, String temper, String edge, long used) {
		public Honing {
			key = key == null ? "" : key;
			temper = temper == null ? "" : temper;
			edge = edge == null ? "" : edge;
			xp = Math.max(0, xp);
			practice = Math.max(0, practice);
		}

		public static final Codec<Honing> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("key").forGetter(Honing::key),
			Codec.FLOAT.optionalFieldOf("xp", 0F).forGetter(Honing::xp),
			Codec.FLOAT.optionalFieldOf("practice", 0F).forGetter(Honing::practice),
			Codec.STRING.optionalFieldOf("temper", "").forGetter(Honing::temper),
			Codec.STRING.optionalFieldOf("edge", "").forGetter(Honing::edge),
			Codec.LONG.optionalFieldOf("used", 0L).forGetter(Honing::used)
		).apply(i, Honing::new));

		public static final StreamCodec<ByteBuf, Honing> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(256), Honing::key, ByteBufCodecs.FLOAT, Honing::xp, ByteBufCodecs.FLOAT, Honing::practice,
			ByteBufCodecs.stringUtf8(32), Honing::temper, ByteBufCodecs.stringUtf8(32), Honing::edge, ByteBufCodecs.VAR_LONG, Honing::used, Honing::new);

		public int rank() {
			return TechniqueRules.rank(xp);
		}

		static Honing fresh(String key, long now) {
			return new Honing(key, 0, 0, "", "", now);
		}
	}

	/** Everything a swordsman has of their own techniques: the parts learned for good, their slots, and the techniques' records. */
	public record Book(List<String> learned, List<Written> slots, List<Honing> records) {
		public static final Book EMPTY = new Book(List.of(), List.of(), List.of());

		public Book {
			learned = learned == null ? List.of() : List.copyOf(new LinkedHashSet<>(learned));
			List<Written> s = new ArrayList<>(slots == null ? List.of() : slots);
			while (s.size() < TechniqueRules.MAX_SLOTS) {
				s.add(Written.NONE);
			}
			slots = List.copyOf(s.subList(0, TechniqueRules.MAX_SLOTS));
			records = records == null ? List.of() : List.copyOf(records);
		}

		public static final Codec<Book> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.listOf().optionalFieldOf("learned", List.of()).forGetter(Book::learned),
			Written.CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(Book::slots),
			Honing.CODEC.listOf().optionalFieldOf("records", List.of()).forGetter(Book::records)
		).apply(i, Book::new));

		public static final StreamCodec<ByteBuf, Book> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(64)), Book::learned,
			Written.STREAM_CODEC.apply(ByteBufCodecs.list(TechniqueRules.MAX_SLOTS)), Book::slots,
			Honing.STREAM_CODEC.apply(ByteBufCodecs.list(64)), Book::records, Book::new);

		public Written slot(int i) {
			return i >= 0 && i < slots.size() ? slots.get(i) : Written.NONE;
		}

		/** The record kept for {@code key}, or a fresh one (nothing earned yet). */
		public Honing honing(String key) {
			for (Honing h : records) {
				if (h.key().equals(key)) {
					return h;
				}
			}
			return Honing.fresh(key, 0);
		}

		public boolean hasRecord(String key) {
			return records.stream().anyMatch(h -> h.key().equals(key));
		}

		Book withLearned(String part) {
			List<String> next = new ArrayList<>(learned);
			next.add(part);
			return new Book(next, slots, records);
		}

		Book withoutLearned(String part) {
			List<String> next = new ArrayList<>(learned);
			next.remove(part);
			return new Book(next, slots, records);
		}

		Book withSlot(int i, Written w) {
			List<Written> next = new ArrayList<>(slots);
			next.set(i, w);
			return new Book(learned, next, records);
		}

		/**
		 * The same with {@code h} kept (replacing the record for its key) as the most recent, and the oldest let go past
		 * {@link TechniqueRules#MAX_RECORDS} (never one a slot holds).
		 */
		Book withRecord(Honing h) {
			List<Honing> next = new ArrayList<>();
			for (Honing r : records) {
				if (!r.key().equals(h.key())) {
					next.add(r);
				}
			}
			next.add(h);
			Set<String> held = new HashSet<>();
			for (Written w : slots) {
				if (!w.empty()) {
					held.add(w.key());
				}
			}
			while (next.size() > TechniqueRules.MAX_RECORDS) {
				Honing oldest = null;
				for (Honing r : next) {
					if (!held.contains(r.key()) && (oldest == null || r.used() < oldest.used())) {
						oldest = r;
					}
				}
				if (oldest == null) {
					break;
				}
				next.remove(oldest);
			}
			return new Book(learned, slots, next);
		}
	}

	/** Each swordsman's techniques: saved, kept through death (they're the swordsman's own), synced to its owner (the reader and the page ask). */
	public static final AttachmentType<Book> BOOK = AttachmentRegistry.create(
		Wildercord.id("aura_techniques"),
		builder -> builder
			.initializer(() -> Book.EMPTY)
			.persistent(Book.CODEC)
			.syncWith(Book.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	// ------------------------------------------------------------------ reading (both sides)

	public static Book book(Player player) {
		return player.getAttachedOrElse(BOOK, Book.EMPTY);
	}

	/** Whether techniques work for {@code player}: on on this server (or the one this client is on) and aura working. */
	public static boolean on(Player player) {
		return player != null && Config.techniques(player) && Aura.enabled(player);
	}

	/** How many slots {@code player}'s stage opens. */
	public static int slots(Player player) {
		return TechniqueRules.slots(Aura.stage(player));
	}

	/** Whether a Way lends {@code player} part {@code partId} now (the Way's Edge node in force). */
	public static boolean lent(Player player, String partId) {
		return TechniqueRules.lendingNode(partId).map(node -> Ways.has(player, node)).orElse(false);
	}

	/** Whether {@code partId} is innate to {@code player}: one every blade knows from Edge. */
	public static boolean innate(Player player, String partId) {
		return TechniqueRules.INNATE.contains(partId) && Aura.stage(player) >= TechniqueRules.FROM;
	}

	/** Whether {@code player} has learned {@code partId} for good (a scroll, a duelist, an add-on's lesson). */
	public static boolean learned(Player player, String partId) {
		return book(player).learned().contains(partId);
	}

	/** Whether {@code player} may write with {@code partId} now: learned, innate, or lent by their Way. */
	public static boolean knows(Player player, String partId) {
		return learned(player, partId) || innate(player, partId) || lent(player, partId);
	}

	/** Every part {@code player} may write with now, in the page's order. */
	public static List<String> known(Player player) {
		List<String> out = new ArrayList<>();
		for (String part : TechniqueRules.allParts()) {
			if (knows(player, part)) {
				out.add(part);
			}
		}
		return out;
	}

	/** {@code player}'s method's flavour (their passive), what every technique of theirs carries. */
	public static BreathingMethod.Flavour flavour(Player player) {
		return Aura.method(player).map(BreathingMethod::flavour).orElse(BreathingMethod.Flavour.NONE);
	}

	/** Whether {@code w}'s parts are all real parts of the right kind (an add-on's intent gone leaves a technique that can't be played). */
	public static boolean whole(Written w) {
		return !w.empty() && TechniqueRules.is(w.stroke(), TechniqueRules.Family.STROKE) && TechniqueRules.is(w.release(), TechniqueRules.Family.RELEASE)
			&& TechniqueRules.is(w.intent(), TechniqueRules.Family.INTENT) && w.sword().isPresent();
	}

	/** Whether the technique in {@code player}'s slot {@code i} can be played now: techniques on, the slot open, every part known. */
	public static boolean usable(Player player, int i) {
		if (!on(player) || i < 0 || i >= slots(player)) {
			return false;
		}
		Written w = book(player).slot(i);
		return whole(w) && knows(player, w.stroke()) && knows(player, w.release()) && knows(player, w.intent());
	}

	/** The rank of {@code player}'s technique {@code w} (Raw for one never used). */
	public static int rank(Player player, Written w) {
		return book(player).honing(w.key()).rank();
	}

	/** {@code w} as {@code player} would perform it now: their method, its rank, its temper and its edge. */
	public static TechniqueRules.Profile profile(Player player, Written w) {
		Honing h = book(player).honing(w.key());
		return TechniqueRules.profile(w.stroke(), w.release(), w.intent(), flavour(player), h.rank(), h.temper(), h.edge());
	}

	/** What {@code w} is priced at for {@code player} (its worth at Tempered, with its temper and edge). */
	public static double pricedWorth(Player player, Written w) {
		Honing h = book(player).honing(w.key());
		return TechniqueRules.pricedWorth(w.stroke(), w.release(), w.intent(), flavour(player), h.temper(), h.edge());
	}

	// ------------------------------------------------------------------ the string source (both sides)

	/** What a player's written techniques were made into last, by player object (a client's and a server's player apart). */
	private record Made(Book book, BreathingMethod.Flavour flavour, List<AuraApi.StringArt> arts) {}

	private static final Map<Player, Made> MADE = Collections.synchronizedMap(new WeakHashMap<>());

	/**
	 * {@code player}'s written techniques as arts of their own (see {@link AuraApi#addStringSource}): one for each slot written, each
	 * open only while it can be played ({@link #usable}), under the id of its slot ({@link TechniqueRules#artId}). Made again only when
	 * the book or the method changes.
	 */
	public static List<AuraApi.StringArt> strings(Player player) {
		if (player == null || !on(player) || Aura.stage(player) < TechniqueRules.FROM) {
			return List.of();
		}
		Book book = book(player);
		BreathingMethod.Flavour flavour = flavour(player);
		Made made = MADE.get(player);
		if (made != null && made.book() == book && made.flavour() == flavour) {
			return made.arts();
		}
		List<AuraApi.StringArt> arts = new ArrayList<>();
		for (int i = 0; i < TechniqueRules.MAX_SLOTS; i++) {
			Written w = book.slot(i);
			if (!whole(w)) {
				continue;
			}
			SwordString string = w.sword().orElseThrow();
			Honing h = book.honing(w.key());
			double priced = TechniqueRules.pricedWorth(w.stroke(), w.release(), w.intent(), flavour, h.temper(), h.edge());
			int slot = i;
			arts.add(new AuraApi.StringArt(TechniqueRules.artId(i), string, TechniqueRules.FROM, TechniqueRules.cost(priced, string),
				TechniqueRules.rest(priced, string), p -> usable(p, slot), AuraApi.ArtCondition.ALWAYS, dev.wildercord.aura.arts.TechniqueArts::perform));
		}
		List<AuraApi.StringArt> out = List.copyOf(arts);
		MADE.put(player, new Made(book, flavour, out));
		return out;
	}

	/** The written technique {@code art} plays, for {@code player}, or null when it isn't one of theirs. */
	public static Written written(Player player, AuraApi.StringArt art) {
		int i = art == null ? -1 : TechniqueRules.slotOf(art.id());
		return i < 0 ? null : book(player).slot(i);
	}

	/** {@code art}'s name if it's one of {@code player}'s techniques (as its writer named it, made safe), or null. */
	public static Component name(Player player, AuraApi.StringArt art) {
		Written w = player == null ? null : written(player, art);
		return w == null || w.empty() ? null : Component.literal(w.shownName());
	}

	/** Whether {@code art} is one of {@code player}'s techniques at Peerless (its banner rings grand). */
	public static boolean peerless(Player player, AuraApi.StringArt art) {
		Written w = written(player, art);
		return w != null && !w.empty() && rank(player, w) >= TechniqueRules.PEERLESS;
	}

	/** The rank word over a technique's name in its banner ("Keen technique"), or null when {@code art} isn't one. */
	public static Component kicker(Player player, AuraApi.StringArt art) {
		Written w = written(player, art);
		if (w == null || w.empty()) {
			return null;
		}
		return Component.translatable("aura.wildercord.banner.technique_ranked", Component.translatable(rankKey(rank(player, w))));
	}

	/** The arts' slot a technique's momentum counts as (by its worth), or -1 for an art that isn't one of {@code player}'s techniques. */
	public static int momentumSlot(Player player, AuraApi.StringArt art) {
		Written w = written(player, art);
		return w == null || !whole(w) ? -1 : TechniqueRules.momentumSlot(pricedWorth(player, w));
	}

	public static String rankKey(int rank) {
		return "aura.wildercord.technique_rank." + TechniqueRules.rankId(rank);
	}

	// ------------------------------------------------------------------ what gets in a string's way (both sides)

	/**
	 * What {@code string} would meet if written into {@code player}'s slot {@code i}: every art and technique of theirs (whatever its
	 * stage) whose string is the same, cuts it short or is cut short by it ({@link TechniqueRules#clash}). Empty when it's free.
	 */
	public static List<AuraApi.StringArt> clashes(Player player, int i, SwordString string) {
		List<AuraApi.StringArt> out = new ArrayList<>();
		String self = TechniqueRules.artId(i);
		for (AuraApi.StringArt other : AuraApi.conflicts(player, string, self)) {
			if (TechniqueRules.slotOf(other.id()) < 0) {
				out.add(other);
			}
		}
		for (AuraApi.StringArt other : strings(player)) {
			if (!other.id().equals(self) && TechniqueRules.clash(string, other.string())) {
				out.add(other);
			}
		}
		return out;
	}

	/**
	 * The arts and techniques of {@code player}'s that the same swings could finish along with {@code string} (see
	 * {@link TechniqueRules#overlap}), each with whether it would go first: for the page's warning.
	 */
	public static List<Overlap> overlaps(Player player, int i, SwordString string) {
		List<Overlap> out = new ArrayList<>();
		String self = TechniqueRules.artId(i);
		StringReader.Spelled mine = new StringReader.Spelled() {
			@Override
			public SwordString string() {
				return string;
			}

			@Override
			public int stage() {
				return TechniqueRules.FROM;
			}
		};
		List<AuraApi.StringArt> others = new ArrayList<>(AuraApi.allStringsOf(player));
		for (AuraApi.StringArt art : strings(player)) {
			if (others.stream().noneMatch(o -> o.id().equals(art.id()))) {
				others.add(art);
			}
		}
		for (AuraApi.StringArt other : others) {
			if (other.id().equals(self) || TechniqueRules.clash(string, other.string()) || !TechniqueRules.overlap(string, other.string())
					|| TechniqueRules.incidentalCue(string, other.string())) {
				continue;
			}
			out.add(new Overlap(other, StringReader.compare(other, mine) > 0));
		}
		return out;
	}

	/** Another art the same swings could finish, and whether it would go first (it asks more of the hand). */
	public record Overlap(AuraApi.StringArt art, boolean first) {}

	// ------------------------------------------------------------------ why a technique can't be written (both sides)

	/** Why something can't be done on the writing page: a line's language key and what goes in it. */
	public record Refusal(String key, Object... args) {
		public Component line() {
			return Component.translatable(key, args).withColor(0xA89CC8);
		}
	}

	/**
	 * Why {@code player} can't write the technique given into slot {@code i}, or null when they can: techniques on, Edge reached, the
	 * slot open, each part a part of its kind and known, the string sound by itself and meeting nothing, and (on the server, or as the
	 * client sees it) not in the heat of a fight.
	 */
	public static Refusal refusal(Player player, int i, String stroke, String release, String intent, String string) {
		if (!on(player)) {
			return new Refusal("message.wildercord.aura.technique.off");
		}
		if (Aura.stage(player) < TechniqueRules.FROM) {
			return new Refusal("message.wildercord.aura.technique.edge", stageName(TechniqueRules.FROM));
		}
		if (i < 0 || i >= TechniqueRules.MAX_SLOTS) {
			return new Refusal("message.wildercord.aura.technique.no_slot");
		}
		if (i >= slots(player)) {
			return new Refusal("message.wildercord.aura.technique.slot", stageName(TechniqueRules.slotStage(i)));
		}
		String[] parts = {stroke, release, intent};
		TechniqueRules.Family[] families = TechniqueRules.Family.values();
		for (int k = 0; k < 3; k++) {
			if (!TechniqueRules.is(parts[k], families[k])) {
				return new Refusal("message.wildercord.aura.technique.not_part", Component.translatable("screen.wildercord.aura.writing." + families[k].id + ".word"));
			}
			if (!knows(player, parts[k])) {
				return new Refusal("message.wildercord.aura.technique.unknown", Component.translatable(TechniqueRules.nameKey(parts[k])));
			}
		}
		SwordString sword;
		try {
			sword = SwordString.parse(string);
		} catch (IllegalArgumentException e) {
			return new Refusal("message.wildercord.aura.technique.string_too_short", TechniqueRules.MIN_STRING, TechniqueRules.MAX_STRING);
		}
		Optional<TechniqueRules.StringProblem> problem = TechniqueRules.problem(sword);
		if (problem.isPresent()) {
			return new Refusal("message.wildercord.aura.technique.string_" + problem.get().name().toLowerCase(java.util.Locale.ROOT), TechniqueRules.MIN_STRING,
				TechniqueRules.MAX_STRING);
		}
		List<AuraApi.StringArt> clash = clashes(player, i, sword);
		if (!clash.isEmpty()) {
			return new Refusal("message.wildercord.aura.technique.clash", AuraApi.artName(player, clash.getFirst()));
		}
		if (fighting(player)) {
			return new Refusal("message.wildercord.aura.technique.fight");
		}
		return null;
	}

	/** Whether {@code player} is in the heat of a fight: the server's own record of blows, or on a client what the body's aura shows. */
	public static boolean fighting(Player player) {
		if (player instanceof ServerPlayer server) {
			return Aura.inFight(server);
		}
		return AuraPresence.look(player).fightUntil() > player.level().getGameTime();
	}

	private static Component stageName(int stage) {
		return Component.translatable("aura.wildercord.stage." + AuraStages.id(stage));
	}

	// ------------------------------------------------------------------ payloads

	/** Client to server: write a technique into slot {@code slot} (its name as typed; the server makes it safe). */
	public record Write(int slot, String name, String stroke, String release, String intent, String string) implements CustomPacketPayload {
		public static final Type<Write> TYPE = new Type<>(Wildercord.id("technique_write"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Write> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Write::slot, ByteBufCodecs.stringUtf8(96), Write::name, ByteBufCodecs.stringUtf8(64), Write::stroke,
			ByteBufCodecs.stringUtf8(64), Write::release, ByteBufCodecs.stringUtf8(64), Write::intent, ByteBufCodecs.stringUtf8(64), Write::string,
			Write::new).cast();

		@Override
		public Type<Write> type() {
			return TYPE;
		}
	}

	/** Client to server: erase the technique in slot {@code slot} (its record is kept). */
	public record Erase(int slot) implements CustomPacketPayload {
		public static final Type<Erase> TYPE = new Type<>(Wildercord.id("technique_erase"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Erase> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Erase::slot, Erase::new).cast();

		@Override
		public Type<Erase> type() {
			return TYPE;
		}
	}

	/** Client to server: temper ({@code edge} false) or edge the technique in slot {@code slot} with {@code choice} ("" as written). */
	public record Choose(int slot, boolean edge, String choice) implements CustomPacketPayload {
		public static final Type<Choose> TYPE = new Type<>(Wildercord.id("technique_choose"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Choose> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Choose::slot, ByteBufCodecs.BOOL,
			Choose::edge, ByteBufCodecs.stringUtf8(32), Choose::choice, Choose::new).cast();

		@Override
		public Type<Choose> type() {
			return TYPE;
		}
	}

	/** Client to server: set part {@code part} of the Peerless technique in slot {@code slot} down on a scroll. */
	public record Inscribe(int slot, String part) implements CustomPacketPayload {
		public static final Type<Inscribe> TYPE = new Type<>(Wildercord.id("technique_inscribe"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Inscribe> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Inscribe::slot,
			ByteBufCodecs.stringUtf8(64), Inscribe::part, Inscribe::new).cast();

		@Override
		public Type<Inscribe> type() {
			return TYPE;
		}
	}

	private static final PacketThrottle REQUESTS = new PacketThrottle(4, 10);

	// ------------------------------------------------------------------ writing (server)

	private static void set(ServerPlayer player, Book book) {
		if (!book.equals(book(player))) {
			player.setAttached(BOOK, book);
		}
	}

	/** Writes the technique asked for, or says why not (the page asked the same first, so a refusal here is a race or a cheat). */
	public static boolean write(ServerPlayer player, Write w) {
		String stroke = TechniqueRules.normal(w.stroke());
		String release = TechniqueRules.normal(w.release());
		String intent = TechniqueRules.normal(w.intent());
		Refusal why = refusal(player, w.slot(), stroke, release, intent, w.string());
		if (why != null) {
			player.sendOverlayMessage(why.line());
			return false;
		}
		String name = TechniqueRules.cleanName(w.name());
		if (name.isEmpty()) {
			name = TechniqueRules.autoName(stroke, release, intent, flavour(player));
		}
		Written next = new Written(name, stroke, release, intent, SwordString.parse(w.string()).text());
		Book book = book(player);
		Written before = book.slot(w.slot());
		if (next.equals(before)) {
			return false;
		}
		long now = player.level().getGameTime();
		Honing h = book.honing(next.key());
		Book after = book.withSlot(w.slot(), next).withRecord(new Honing(h.key(), h.xp(), h.practice(), h.temper(), h.edge(), now));
		set(player, after);
		boolean fresh = before.empty() || !before.key().equals(next.key());
		Aura.sound(player, "aura_technique_write", 0.8F, fresh ? 1.0F : 1.15F);
		player.sendOverlayMessage(Component.translatable(fresh ? "message.wildercord.aura.technique.written" : "message.wildercord.aura.technique.rewritten",
			Component.literal(name).withColor(0xFF000000 | Aura.color(player)), Component.translatable(slotKey(w.slot()))).withColor(0xE8D8B0));
		Grimoire.unlock(player, "aura:technique");
		for (AuraApi.TechniqueHook hook : AuraApi.techniqueHooks()) {
			try {
				hook.written(player, w.slot(), next);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A technique hook threw; skipping it", e);
			}
		}
		written++;
		return true;
	}

	/** The language key naming slot {@code i} ("your first slot"). */
	public static String slotKey(int i) {
		return "screen.wildercord.aura.writing.slot_" + (i + 1);
	}

	/** Erases the technique in slot {@code i} (its record is kept, so writing it again brings its rank back). */
	public static boolean erase(ServerPlayer player, int i) {
		Book book = book(player);
		if (i < 0 || i >= TechniqueRules.MAX_SLOTS || book.slot(i).empty()) {
			return false;
		}
		Written gone = book.slot(i);
		set(player, book.withSlot(i, Written.NONE));
		Aura.sound(player, "aura_technique_write", 0.5F, 0.7F);
		player.sendOverlayMessage(Component.translatable("message.wildercord.aura.technique.erased", Component.literal(gone.shownName())).withColor(0xC8B89A));
		return true;
	}

	/**
	 * Tempers ({@code edge} false) or edges the technique in slot {@code i}: from Honed (a temper) or Keen (an edge); the first choice
	 * free, changing one already made {@link TechniqueRules#CHANGE_LEVELS} experience levels (nothing in creative).
	 */
	public static boolean choose(ServerPlayer player, int i, boolean edge, String choice) {
		Book book = book(player);
		Written w = book.slot(i);
		String c = TechniqueRules.normal(choice);
		if (!on(player) || w.empty() || !TechniqueRules.validChoice(edge, c)) {
			return false;
		}
		Honing h = book.honing(w.key());
		int needs = edge ? TechniqueRules.EDGE_RANK : TechniqueRules.TEMPER_RANK;
		if (h.rank() < needs) {
			player.sendOverlayMessage(new Refusal("message.wildercord.aura.technique.choose_rank", Component.translatable(rankKey(needs))).line());
			return false;
		}
		String had = edge ? h.edge() : h.temper();
		if (had.equals(c)) {
			return false;
		}
		if (!had.isEmpty() && !player.isCreative()) {
			if (player.experienceLevel < TechniqueRules.CHANGE_LEVELS) {
				player.sendOverlayMessage(new Refusal("message.wildercord.aura.technique.choose_levels", TechniqueRules.CHANGE_LEVELS).line());
				return false;
			}
			player.giveExperienceLevels(-TechniqueRules.CHANGE_LEVELS);
		}
		Honing next = edge ? new Honing(h.key(), h.xp(), h.practice(), h.temper(), c, player.level().getGameTime())
			: new Honing(h.key(), h.xp(), h.practice(), c, h.edge(), player.level().getGameTime());
		set(player, book.withRecord(next));
		Aura.sound(player, "aura_technique_rank", 0.6F, edge ? 1.2F : 1.05F);
		return true;
	}

	/**
	 * Sets part {@code part} of the Peerless technique in slot {@code i} down on a technique scroll, for another swordsman: a paper and an
	 * Aura Shard from the inventory (nothing in creative). Only a part a scroll can carry (never one every blade knows, nor a Way's own).
	 */
	public static boolean inscribe(ServerPlayer player, int i, String part) {
		Book book = book(player);
		Written w = book.slot(i);
		String p = TechniqueRules.normal(part);
		if (!on(player) || w.empty() || !w.parts().contains(p)) {
			return false;
		}
		if (book.honing(w.key()).rank() < TechniqueRules.INSCRIBE_RANK) {
			player.sendOverlayMessage(new Refusal("message.wildercord.aura.technique.inscribe_rank",
				Component.translatable(rankKey(TechniqueRules.INSCRIBE_RANK))).line());
			return false;
		}
		if (!TechniqueRules.scrollable(p)) {
			player.sendOverlayMessage(new Refusal("message.wildercord.aura.technique.inscribe_part", Component.translatable(TechniqueRules.nameKey(p))).line());
			return false;
		}
		if (!player.isCreative()) {
			int paper = player.getInventory().findSlotMatchingItem(new ItemStack(Items.PAPER));
			int shard = player.getInventory().findSlotMatchingItem(new ItemStack(dev.wildercord.aura.world.AuraWorld.AURA_SHARD));
			if (paper < 0 || shard < 0) {
				player.sendOverlayMessage(new Refusal("message.wildercord.aura.technique.inscribe_needs").line());
				return false;
			}
			player.getInventory().removeItem(paper, 1);
			player.getInventory().removeItem(shard, 1);
		}
		ItemStack scroll = dev.wildercord.aura.world.TechniqueScrollItem.of(p);
		if (!player.getInventory().add(scroll)) {
			player.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), scroll));
		}
		Aura.sound(player, "aura_technique_write", 0.8F, 0.85F);
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.technique.inscribed", Component.translatable(TechniqueRules.nameKey(p)),
			Component.literal(w.shownName())).withColor(0xE8D8B0));
		return true;
	}

	// ------------------------------------------------------------------ learning (server)

	/**
	 * Teaches {@code player} part {@code partId} for good, from {@code source} ("scroll", "duelist", an add-on's own): said in chat, its
	 * voice, the Grimoire the first time, and the hooks. Returns whether it was new (a part already learned is left as it was).
	 */
	public static boolean teach(ServerPlayer player, String partId, String source) {
		String part = TechniqueRules.normal(partId);
		Optional<TechniqueRules.Family> family = TechniqueRules.family(part);
		if (family.isEmpty() || learned(player, part)) {
			return false;
		}
		set(player, book(player).withLearned(part));
		Component name = Component.translatable(TechniqueRules.nameKey(part)).withColor(0xFF000000 | familyColor(family.get()));
		Component kind = Component.translatable("screen.wildercord.aura.writing." + family.get().id + ".word");
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.technique.learned", name, kind).withColor(0xE8D8B0));
		if (Aura.stage(player) < TechniqueRules.FROM) {
			player.sendSystemMessage(Component.translatable("message.wildercord.aura.technique.learned_later",
				stageName(TechniqueRules.FROM)).withColor(0xB8A8D8));
		}
		Aura.sound(player, "aura_technique_learn", 0.9F, 1.0F);
		Grimoire.unlock(player, "aura:technique_part");
		for (AuraApi.TechniqueHook hook : AuraApi.techniqueHooks()) {
			try {
				hook.learned(player, part, source);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A technique hook threw; skipping it", e);
			}
		}
		return true;
	}

	/** Takes part {@code partId} from what {@code player} has learned (an operator's command). Returns whether they had it. */
	public static boolean forget(ServerPlayer player, String partId) {
		String part = TechniqueRules.normal(partId);
		if (!learned(player, part)) {
			return false;
		}
		set(player, book(player).withoutLearned(part));
		return true;
	}

	/**
	 * A duelist of {@code method} beaten by {@code player}: it shows them one part of a technique they don't know yet, its method's
	 * favourites likelier (see {@link TechniqueRules#duelistWeights}). Nothing when techniques are off or they know every part it could
	 * show. Returns the part shown, or empty.
	 */
	public static Optional<String> duelistLesson(ServerPlayer player, BreathingMethod method, Component duelist) {
		if (!on(player) || Aura.stage(player) < AuraRules.GLOW) {
			return Optional.empty();
		}
		Set<String> known = new HashSet<>(book(player).learned());
		Optional<String> part = TechniqueRules.draw(TechniqueRules.duelistWeights(method.flavour()), known, player.getRandom().nextDouble());
		part.ifPresent(p -> {
			player.sendSystemMessage(Component.translatable("message.wildercord.aura.technique.duelist", duelist,
				Component.translatable(TechniqueRules.nameKey(p))).withColor(0xE8D8B0));
			teach(player, p, "duelist");
		});
		return part;
	}

	/** Each family's colour on the page and in chat: a stroke's steel, a release's sky, an intent's gold. */
	public static int familyColor(TechniqueRules.Family family) {
		return switch (family) {
			case STROKE -> 0xE07A5A;
			case RELEASE -> 0x7AB8E8;
			case INTENT -> 0xE8C46A;
		};
	}

	// ------------------------------------------------------------------ ranks (server)

	/**
	 * One use of a technique, as it lands: the experience it has earned so far (held to {@link TechniqueRules#MAX_PER_USE}), the foes it
	 * has struck, and the moment and repetition it was used in (worked out once, as it began).
	 */
	public static final class Use {
		final ServerPlayer player;
		final int slot;
		final String key;
		final double moment;
		final double repeat;
		double earned;
		final Set<UUID> struck = new HashSet<>();

		Use(ServerPlayer player, int slot, String key, double moment, double repeat) {
			this.player = player;
			this.slot = slot;
			this.key = key;
			this.moment = moment;
			this.repeat = repeat;
		}

		/** A strike of the technique took {@code taken} from {@code foe}: experience by what it was and what it took. */
		public void landed(LivingEntity foe, float taken) {
			if (foe == null || taken <= 0 || player.hasDisconnected()) {
				return;
			}
			boolean first = struck.add(foe.getUUID());
			boolean practice = Momentum.practice(player, foe);
			double worth = practice ? 1.0 : AuraCombat.worth(player, foe);
			if (worth <= 0 || !practice && Momentum.helpless(foe)) {
				return;
			}
			double share = taken / Math.max(1.0F, foe.getMaxHealth());
			double xp = first ? TechniqueRules.strike(worth, share, !foe.isAlive())
				: worth * (TechniqueRules.DAMAGE * Math.min(1, share) + (!foe.isAlive() ? TechniqueRules.KILL : 0));
			xp *= moment * repeat * Math.max(0, Config.get().aura().techniques().techniqueXp());
			// A bonded blade's Inkbound Steel: techniques rank faster with it.
			xp *= BladeTraits.techniqueXp(player);
			xp = Math.max(0, Math.min(xp, TechniqueRules.MAX_PER_USE - earned));
			if (xp <= 0) {
				return;
			}
			earned += xp;
			gain(player, slot, key, xp, practice);
		}

		public String key() {
			return key;
		}
	}

	/** Each player's recent uses of each technique by place: how many lately, and when last (for repetition, as spell mastery fades it). */
	private static final Map<UUID, Map<Long, double[]>> PLACES = new HashMap<>();

	/** A use of {@code player}'s technique in slot {@code i} begins: its experience will be counted as it lands. */
	public static Use use(ServerPlayer player, int i) {
		Written w = book(player).slot(i);
		long now = player.level().getGameTime();
		BlockPos pos = player.blockPosition();
		Map<Long, double[]> places = PLACES.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
		int s = Integer.numberOfTrailingZeros(dev.wildercord.spell.MasteryRules.PLACE);
		long where = BlockPos.asLong(pos.getX() >> s, pos.getY() >> s, pos.getZ() >> s) * 31 + w.key().hashCode();
		double[] seen = places.computeIfAbsent(where, k -> new double[] {0, now});
		double recent = dev.wildercord.spell.MasteryRules.forget(seen[0], now - (long) seen[1]);
		seen[0] = recent + 1;
		seen[1] = now;
		if (places.size() > 64) {
			places.entrySet().removeIf(e -> dev.wildercord.spell.MasteryRules.forget(e.getValue()[0], now - (long) e.getValue()[1]) < 0.05);
		}
		return new Use(player, i, w.key(), AuraCombat.moment(player, now), TechniqueRules.repetition(recent));
	}

	/** {@code xp} earned by {@code player}'s technique kept as {@code key} (in slot {@code i}): its record grows, and a rank reached is said. */
	static void gain(ServerPlayer player, int i, String key, double xp, boolean practice) {
		Book book = book(player);
		Honing h = book.honing(key);
		double got = practice ? TechniqueRules.practice(h.practice(), xp) : xp;
		if (got <= 0) {
			return;
		}
		int before = h.rank();
		Honing next = new Honing(key, (float) (h.xp() + got), practice ? (float) (h.practice() + got) : h.practice(), h.temper(), h.edge(),
			player.level().getGameTime());
		set(player, book.withRecord(next));
		int after = next.rank();
		if (after > before) {
			ranked(player, i, book.slot(i), after);
		}
	}

	/** {@code w} reached {@code rank}: said, heard and seen, and what it opens (a temper at Honed, an edge at Keen, scrolls at Peerless). */
	private static void ranked(ServerPlayer player, int i, Written w, int rank) {
		Component name = Component.literal(w.shownName()).withColor(0xFF000000 | Aura.color(player));
		Component rankName = Component.translatable(rankKey(rank)).withColor(0xFFE8C46A);
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.technique.ranked", name, rankName).withColor(0xE8D8B0));
		String opens = switch (rank) {
			case TechniqueRules.HONED -> "message.wildercord.aura.technique.opens_temper";
			case TechniqueRules.TEMPERED -> "message.wildercord.aura.technique.opens_deep";
			case TechniqueRules.KEEN -> "message.wildercord.aura.technique.opens_edge";
			case TechniqueRules.PEERLESS -> "message.wildercord.aura.technique.opens_peerless";
			default -> null;
		};
		if (opens != null) {
			player.sendSystemMessage(Component.translatable(opens).withColor(0xB8A8D8));
		}
		Aura.sound(player, "aura_technique_rank", 1.0F, 0.9F + 0.05F * rank);
		AuraFx.banner(player, name, Component.translatable("aura.wildercord.banner.technique_rank_up", rankName),
			rank >= TechniqueRules.PEERLESS ? AuraFxRules.BannerKind.GRAND : AuraFxRules.BannerKind.ART);
		AuraFx.bodyAuraFlare(player, 30, rank >= TechniqueRules.PEERLESS ? 1.0F : 0.6F);
		if (rank >= TechniqueRules.PEERLESS) {
			Grimoire.unlock(player, "aura:technique_peerless");
		}
		for (AuraApi.TechniqueHook hook : AuraApi.techniqueHooks()) {
			try {
				hook.ranked(player, i, w, rank);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A technique hook threw; skipping it", e);
			}
		}
		rankUps++;
	}

	/** Sets the experience of {@code player}'s technique in slot {@code i} outright (an operator's command and the game tests). */
	public static boolean setXp(ServerPlayer player, int i, double xp) {
		Book book = book(player);
		Written w = book.slot(i);
		if (w.empty()) {
			return false;
		}
		Honing h = book.honing(w.key());
		set(player, book.withRecord(new Honing(h.key(), (float) Math.max(0, xp), h.practice(), h.temper(), h.edge(), player.level().getGameTime())));
		return true;
	}

	/** Writes a technique outright, owning every part it needs or not (an operator's command and the game tests): no checks but its parts and string. */
	public static boolean set(ServerPlayer player, int i, String name, String stroke, String release, String intent, String string) {
		if (i < 0 || i >= TechniqueRules.MAX_SLOTS) {
			return false;
		}
		Written w = new Written(TechniqueRules.cleanName(name).isEmpty() ? TechniqueRules.autoName(stroke, release, intent, flavour(player))
			: TechniqueRules.cleanName(name), stroke, release, intent, SwordString.parse(string).text());
		if (!whole(w)) {
			return false;
		}
		Book book = book(player);
		Honing h = book.honing(w.key());
		set(player, book.withSlot(i, w).withRecord(new Honing(h.key(), h.xp(), h.practice(), h.temper(), h.edge(), player.level().getGameTime())));
		return true;
	}

	/** Clears everything of {@code player}'s techniques (an operator's command and the game tests). */
	public static void clear(ServerPlayer player) {
		player.removeAttached(BOOK);
		PLACES.remove(player.getUUID());
	}

	// ------------------------------------------------------------------ for the game tests

	private static int written;
	private static int rankUps;

	/** Techniques written, and ranks reached, since the server started. */
	public static int writtenCount() {
		return written;
	}

	public static int rankUps() {
		return rankUps;
	}

	// ------------------------------------------------------------------ lifecycle

	static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Write.TYPE, Write.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Erase.TYPE, Erase.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Choose.TYPE, Choose.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Inscribe.TYPE, Inscribe.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Write.TYPE, (payload, context) -> {
			if (REQUESTS.allow(context.player().getUUID(), context.server().getTickCount())) {
				write(context.player(), payload);
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Erase.TYPE, (payload, context) -> {
			if (REQUESTS.allow(context.player().getUUID(), context.server().getTickCount())) {
				erase(context.player(), payload.slot());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Choose.TYPE, (payload, context) -> {
			if (REQUESTS.allow(context.player().getUUID(), context.server().getTickCount())) {
				choose(context.player(), payload.slot(), payload.edge(), payload.choice());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Inscribe.TYPE, (payload, context) -> {
			if (REQUESTS.allow(context.player().getUUID(), context.server().getTickCount())) {
				inscribe(context.player(), payload.slot(), payload.part());
			}
		});
		AuraApi.addStringSource(Techniques::strings);
		AuraApi.nameArts(Techniques::name);
		dev.wildercord.aura.world.TechniqueScrollItem.init();
		ScrollSources.init();
	}

	static void forget(UUID id) {
		PLACES.remove(id);
		REQUESTS.forget(id);
	}

	static void clear() {
		PLACES.clear();
		REQUESTS.clear();
		MADE.clear();
	}
}
