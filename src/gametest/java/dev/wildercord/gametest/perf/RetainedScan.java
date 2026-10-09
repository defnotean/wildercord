package dev.wildercord.gametest.perf;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.minecraft.world.entity.Entity;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Walks the mod's already-loaded classes' static maps, collections and arrays (and the mod's own objects in them, a few
 * levels deep) for anything that still names a player: their UUID, their entity, or a string key holding the UUID. Also
 * sizes every static container, so a run can show what grew and what was given back. Client classes are walked on the
 * client thread, the rest on the server thread, each by its owner so nothing is read mid-write.
 */
public final class RetainedScan {
	private RetainedScan() {}

	private static final int DEPTH = 4, BUDGET = 400_000;
	private static List<String> names;

	/** Every class name in the mod's own output, once. */
	static synchronized List<String> names() {
		if (names != null) return names;
		List<String> out = new ArrayList<>();
		for (Path root : FabricLoader.getInstance().getModContainer("wildercord").orElseThrow().getRootPaths()) {
			try (Stream<Path> walk = Files.walk(root)) {
				walk.filter(p -> p.toString().endsWith(".class")).forEach(p -> {
					String n = root.relativize(p).toString().replace(root.getFileSystem().getSeparator(), ".").replace('/', '.');
					n = n.substring(0, n.length() - 6);
					if (n.startsWith("dev.wildercord.") && !n.contains(".mixin.") && !n.endsWith("package-info")) out.add(n);
				});
			} catch (Exception ignored) {}
		}
		return names = out;
	}

	private static List<Field> statics(boolean client) {
		List<Field> out = new ArrayList<>();
		for (String n : names()) {
			if (n.startsWith("dev.wildercord.client.") != client || !FabricLauncherBase.getLauncher().isClassLoaded(n)) continue;
			try {
				Class<?> c = Class.forName(n, false, RetainedScan.class.getClassLoader());
				for (Field f : c.getDeclaredFields()) {
					if (!Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive() || f.isSynthetic()) continue;
					if (f.trySetAccessible()) out.add(f);
				}
			} catch (Throwable ignored) {}
		}
		return out;
	}

	private static String key(Field f) { return f.getDeclaringClass().getName() + "." + f.getName(); }

	/** Size of every non-empty static map, collection or array, by field. */
	public static Map<String, Integer> sizes(boolean client) {
		Map<String, Integer> out = new TreeMap<>();
		for (Field f : statics(client)) {
			try {
				Object v = f.get(null);
				int n = v instanceof Map<?, ?> m ? m.size() : v instanceof Collection<?> c ? c.size() : v != null && v.getClass().isArray() ? Array.getLength(v) : -1;
				if (n > 0) out.put(key(f), n);
			} catch (Throwable ignored) {}
		}
		return out;
	}

	/** Static fields still reaching any of {@code ids} (or their entities), with how many references each holds. */
	public static Map<String, Integer> holding(boolean client, Set<UUID> ids) {
		Set<String> texts = new java.util.HashSet<>();
		for (UUID id : ids) texts.add(id.toString());
		Map<String, Integer> out = new TreeMap<>();
		for (Field f : statics(client)) {
			try {
				Walk w = new Walk(ids, texts);
				w.visit(f.get(null), 0);
				if (w.hits > 0) out.put(key(f), w.hits);
			} catch (Throwable ignored) {}
		}
		return out;
	}

	private static final class Walk {
		final Set<UUID> ids; final Set<String> texts; final Map<Object, Boolean> seen = new IdentityHashMap<>();
		int hits, budget = BUDGET;
		Walk(Set<UUID> ids, Set<String> texts) { this.ids = ids; this.texts = texts; }

		void visit(Object o, int depth) {
			if (o == null || --budget < 0 || o instanceof Reference<?> || o instanceof Class<?> || o instanceof Number || o instanceof Boolean || o instanceof Enum<?>) return;
			if (o instanceof UUID u) { if (ids.contains(u)) hits++; return; }
			if (o instanceof String s) { if (s.length() >= 36) for (String t : texts) if (s.contains(t)) { hits++; break; } return; }
			if (o instanceof Entity e) { if (ids.contains(e.getUUID())) hits++; return; }
			if (depth > DEPTH || seen.put(o, Boolean.TRUE) != null) return;
			try {
				if (o instanceof Map<?, ?> m) { for (Map.Entry<?, ?> en : new ArrayList<>(m.entrySet())) { visit(en.getKey(), depth + 1); visit(en.getValue(), depth + 1); } return; }
				if (o instanceof Collection<?> c) { for (Object x : new ArrayList<>(c)) visit(x, depth + 1); return; }
			} catch (Throwable ignored) { return; }
			Class<?> c = o.getClass();
			if (c.isArray()) { if (!c.getComponentType().isPrimitive()) for (int i = 0, n = Array.getLength(o); i < n; i++) visit(Array.get(o, i), depth + 1); return; }
			if (!c.getName().startsWith("dev.wildercord.")) return;
			for (Class<?> k = c; k != null && k.getName().startsWith("dev.wildercord."); k = k.getSuperclass())
				for (Field f : k.getDeclaredFields()) {
					if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive() || !f.trySetAccessible()) continue;
					try { visit(f.get(o), depth + 1); } catch (Throwable ignored) {}
				}
		}
	}
}
