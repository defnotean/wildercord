package dev.wildercord.travel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Warps: public places anyone can go to with {@code /warp <name>}, set and removed by operators
 * ({@code /setwarp}, {@code /delwarp}). Kept with the overworld's saved data, whichever world each
 * warp is in.
 */
public final class Warps extends SavedData {
	static final Codec<Warps> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(Codec.STRING, Spot.CODEC).optionalFieldOf("warps", Map.of()).forGetter(warps -> warps.byName)
	).apply(i, Warps::new));
	static final SavedDataType<Warps> TYPE = new SavedDataType<>(Wildercord.id("warps"), Warps::new, CODEC, null);

	private final Map<String, Spot> byName = new TreeMap<>();

	public Warps() {
	}

	private Warps(Map<String, Spot> saved) {
		byName.putAll(saved);
	}

	public static Warps of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public Spot get(String name) {
		return byName.get(name);
	}

	/** Every warp's name, in order. */
	public Set<String> names() {
		return java.util.Collections.unmodifiableSet(byName.keySet());
	}

	/** Sets warp {@code name}; true if it moved one already there. */
	boolean put(String name, Spot spot) {
		boolean moved = byName.put(name, spot) != null;
		setDirty();
		return moved;
	}

	boolean remove(String name) {
		if (byName.remove(name) != null) {
			setDirty();
			return true;
		}
		return false;
	}

	// ------------------------------------------------------------------ the commands

	/** {@code /setwarp}: sets (or moves) warp {@code raw} where {@code player} stands. */
	public static int set(ServerPlayer player, String raw) {
		if (!Travel.available(player)) {
			return 0;
		}
		String name = TravelRules.name(raw);
		if (name == null) {
			Travel.fail(player, "bad_name", TravelRules.MAX_NAME);
			return 0;
		}
		boolean moved = of(player.level().getServer()).put(name, Spot.of(player));
		Travel.good(player, moved ? "warp_moved" : "warp_set", Travel.name(name));
		TravelFx.mark(player.level(), player.position());
		return 1;
	}

	/** {@code /warp}: to warp {@code raw}. */
	public static int go(ServerPlayer player, String raw, boolean operator) {
		if (!Travel.available(player)) {
			return 0;
		}
		String name = TravelRules.name(raw);
		Spot spot = name == null ? null : of(player.level().getServer()).get(name);
		if (spot == null) {
			Travel.fail(player, "warp_missing", Travel.name(raw));
			return 0;
		}
		Component where = Travel.text("where.warp", Travel.name(name));
		return Teleports.begin(player, Teleports.Kind.WARP, where, Teleports.to(spot, "unsafe", where), operator);
	}

	/** {@code /delwarp}: removes warp {@code raw}. */
	public static int delete(ServerPlayer player, String raw) {
		if (!Travel.available(player)) {
			return 0;
		}
		String name = TravelRules.name(raw);
		if (name == null || !of(player.level().getServer()).remove(name)) {
			Travel.fail(player, "warp_missing", Travel.name(raw));
			return 0;
		}
		Travel.good(player, "warp_deleted", Travel.name(name));
		return 1;
	}

	/** {@code /warps}: every warp, each a button that goes there. */
	public static int list(ServerPlayer player) {
		if (!Travel.available(player)) {
			return 0;
		}
		Warps warps = of(player.level().getServer());
		if (warps.byName.isEmpty()) {
			Travel.info(player, "warps_none");
			return 0;
		}
		List<MutableComponent> buttons = new ArrayList<>();
		warps.byName.forEach((name, spot) -> buttons.add(Travel.button(Travel.name(name).withStyle(ChatFormatting.UNDERLINE), "/warp " + name,
			Travel.text("go_hover", Travel.text("where.warp", Travel.name(name)), spot.describe()))));
		player.sendSystemMessage(Travel.text("warps_list", warps.byName.size()).withStyle(ChatFormatting.GRAY).append(Travel.row(buttons)));
		return warps.byName.size();
	}
}
