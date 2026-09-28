package dev.wildercord.familiar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * A player's familiars: every wisp bonded to them, and which one is out. A familiar that isn't out
 * rests in the player's Wisp Lantern, unless it was left waiting at a Wellstone. Saved on the
 * player (so a familiar is never lost with a chunk) and synced to them for the lantern's tooltip.
 *
 * @param out  the id of the familiar that's out, or "" when none is
 * @param last the id of the familiar last out, which the lantern calls first
 */
public record Bonds(List<Bond> bonds, String out, String last) {
	public static final Bonds NONE = new Bonds(List.of(), "", "");

	/**
	 * One familiar.
	 *
	 * @param id      a stable id (the UUID of the wisp it was bonded as)
	 * @param name    its name from a name tag, or "" for none
	 * @param waiting left at a Wellstone: it stays there (saved with the world) until called
	 */
	public record Bond(String id, String element, String name, int xp, boolean waiting) {
		public static final Codec<Bond> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("id").forGetter(Bond::id),
			Codec.STRING.fieldOf("element").forGetter(Bond::element),
			Codec.STRING.optionalFieldOf("name", "").forGetter(Bond::name),
			Codec.INT.optionalFieldOf("xp", 0).forGetter(Bond::xp),
			Codec.BOOL.optionalFieldOf("waiting", false).forGetter(Bond::waiting)
		).apply(i, Bond::new));
		public static final StreamCodec<ByteBuf, Bond> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, Bond::id, ByteBufCodecs.STRING_UTF8, Bond::element, ByteBufCodecs.STRING_UTF8, Bond::name,
			ByteBufCodecs.VAR_INT, Bond::xp, ByteBufCodecs.BOOL, Bond::waiting, Bond::new);

		public int level() {
			return WispRules.level(xp);
		}

		public Bond withXp(int xp) {
			return new Bond(id, element, name, xp, waiting);
		}

		public Bond withName(String name) {
			return new Bond(id, element, name, xp, waiting);
		}

		public Bond withWaiting(boolean waiting) {
			return new Bond(id, element, name, xp, waiting);
		}
	}

	public static final Codec<Bonds> CODEC = RecordCodecBuilder.create(i -> i.group(
		Bond.CODEC.listOf().fieldOf("bonds").forGetter(Bonds::bonds),
		Codec.STRING.optionalFieldOf("out", "").forGetter(Bonds::out),
		Codec.STRING.optionalFieldOf("last", "").forGetter(Bonds::last)
	).apply(i, Bonds::new));
	public static final StreamCodec<ByteBuf, Bonds> STREAM_CODEC = StreamCodec.composite(
		Bond.STREAM_CODEC.apply(ByteBufCodecs.list(WispRules.MAX_BONDS)), Bonds::bonds, ByteBufCodecs.STRING_UTF8, Bonds::out,
		ByteBufCodecs.STRING_UTF8, Bonds::last, Bonds::new);

	public Bonds {
		bonds = List.copyOf(bonds);
	}

	public Bond get(String id) {
		for (Bond bond : bonds) {
			if (bond.id().equals(id)) {
				return bond;
			}
		}
		return null;
	}

	public Bond outBond() {
		return out.isEmpty() ? null : get(out);
	}

	public Bonds with(Bond bond) {
		List<Bond> next = new ArrayList<>(bonds);
		for (int i = 0; i < next.size(); i++) {
			if (next.get(i).id().equals(bond.id())) {
				next.set(i, bond);
				return new Bonds(next, out, last);
			}
		}
		next.add(bond);
		return new Bonds(next, out, last);
	}

	public Bonds withOut(String id) {
		return new Bonds(bonds, id, id.isEmpty() ? last : id);
	}

	/** The elements of every familiar bonded. */
	public List<String> elements() {
		return bonds.stream().map(Bond::element).distinct().toList();
	}

	/** The familiar after {@code id} in the list (wrapping round), for the lantern to call next. */
	public Bond after(String id) {
		if (bonds.isEmpty()) {
			return null;
		}
		for (int i = 0; i < bonds.size(); i++) {
			if (bonds.get(i).id().equals(id)) {
				return bonds.get((i + 1) % bonds.size());
			}
		}
		return bonds.getFirst();
	}
}
