package dev.wildercord.travel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * A saved place: a home, a warp, a waypoint or where to go back to. The dimension is kept as its id
 * (a string), so a place in a world that's since been removed still loads, and just can't be reached.
 */
public record Spot(String dimension, double x, double y, double z, float yaw, float pitch) {
	public static final Codec<Spot> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("dimension").forGetter(Spot::dimension),
		Codec.DOUBLE.fieldOf("x").forGetter(Spot::x),
		Codec.DOUBLE.fieldOf("y").forGetter(Spot::y),
		Codec.DOUBLE.fieldOf("z").forGetter(Spot::z),
		Codec.FLOAT.optionalFieldOf("yaw", 0F).forGetter(Spot::yaw),
		Codec.FLOAT.optionalFieldOf("pitch", 0F).forGetter(Spot::pitch)
	).apply(i, Spot::new));

	/** Where {@code entity} is standing, and the way it faces. */
	public static Spot of(Entity entity) {
		return new Spot(entity.level().dimension().identifier().toString(), entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
	}

	/** A point in {@code level}, facing south. */
	public static Spot at(Level level, Vec3 pos) {
		return new Spot(level.dimension().identifier().toString(), pos.x, pos.y, pos.z, 0F, 0F);
	}

	public Vec3 pos() {
		return new Vec3(x, y, z);
	}

	/** The world it's in, or null if that world doesn't exist (any more). */
	public ServerLevel level(MinecraftServer server) {
		Identifier id = Identifier.tryParse(dimension);
		return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	public boolean in(Level level) {
		return dimension.equals(level.dimension().identifier().toString());
	}

	/** "120, 64, -35 in the Nether". */
	public Component describe() {
		return Component.translatable("message.wildercord.travel.place", (long) Math.floor(x), (long) Math.floor(y), (long) Math.floor(z), dimensionName(dimension));
	}

	/**
	 * A world's name for players: "the Overworld", "the Nether", "the End", or for a world from a
	 * datapack or another mod, its id's path in words ("Deep Dark Caverns" for {@code mod:deep_dark_caverns}).
	 */
	public static Component dimensionName(String dimension) {
		Identifier id = Identifier.tryParse(dimension);
		if (id == null) {
			return Component.literal(dimension);
		}
		StringBuilder words = new StringBuilder();
		for (String word : id.getPath().replace('/', '_').split("_")) {
			if (!word.isEmpty()) {
				words.append(words.isEmpty() ? "" : " ").append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
			}
		}
		if (id.getNamespace().equals("minecraft")) {
			return Component.translatableWithFallback("dimension.wildercord." + id.getPath(), words.toString());
		}
		return Component.literal(words.toString());
	}
}
