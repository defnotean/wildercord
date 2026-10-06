package dev.wildercord.cast;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/** Native-fixture read-only ward observation. Never removes entries, rewrites expiry or retires an owner. */
public final class CastReceiptWardProbe {
	private CastReceiptWardProbe() {}

	public static Map<String, Long> active(ServerPlayer player) {
		Map<String, Long> result = new LinkedHashMap<>();
		for (String name : new String[] {"FORESIGHT", "REVERSAL", "REFLECT", "INFINITY"}) {
			try {
				Object ward = ((Map<?, ?>) field(Wards.class, name).get(null)).get(player.getUUID());
				if (ward == null || name.equals("FORESIGHT") && field(ward.getClass(), "charges").getInt(ward) <= 0) continue;
				long until = ward instanceof Long expiry ? expiry : field(ward.getClass(), "until").getLong(ward);
				if (until >= player.level().getGameTime()) result.put(name, until);
			} catch (ReflectiveOperationException failure) {
				throw new AssertionError("Pinned native ward observation unavailable: " + name, failure);
			}
		}
		return Map.copyOf(result);
	}

	private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(name); field.setAccessible(true); return field;
	}
}
