package dev.wildercord.net;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Every record payload in the mod survives its own codec: it is filled with sample values, written, read back,
 * and must come out equal with nothing left in the buffer. A codec that drops, reorders or misreads a field fails here.
 */
public final class PayloadCodecRoundTripTest implements FabricClientGameTest {
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(20);
			String report = w.getServer().computeOnServer(s -> roundTrip(s.registryAccess()));
			if (!report.isEmpty()) throw new AssertionError(report);
		}
	}

	private static String roundTrip(RegistryAccess registries) {
		List<String> problems = new ArrayList<>(), skipped = new ArrayList<>();
		int checked = 0;
		for (Class<?> type : payloadRecords()) {
			StreamCodec<RegistryFriendlyByteBuf, Object> codec = codecOf(type);
			if (codec == null) { problems.add(type.getName() + ": no static StreamCodec field"); continue; }
			Object sample;
			try { sample = sample(type, 0); } catch (Unsupported e) { skipped.add(type.getSimpleName() + " (" + e.getMessage() + ")"); continue; }
			try {
				var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
				codec.encode(buf, sample);
				Object back = codec.decode(buf);
				if (!sample.equals(back)) problems.add(type.getName() + ": wrote " + sample + " but read " + back);
				else if (buf.readableBytes() != 0) problems.add(type.getName() + ": " + buf.readableBytes() + " bytes left unread");
				checked++;
			} catch (Throwable e) {
				problems.add(type.getName() + ": " + e);
			}
		}
		if (checked < 60) problems.add("only " + checked + " payloads checked; the class scan is missing most of them (skipped: " + skipped + ")");
		return problems.isEmpty() ? "" : problems.size() + " payload codec problems:\n" + String.join("\n", problems);
	}

	/** Records implementing CustomPacketPayload in the mod's own classes (main and client). */
	private static List<Class<?>> payloadRecords() {
		List<Class<?>> found = new ArrayList<>();
		ClassLoader loader = PayloadCodecRoundTripTest.class.getClassLoader();
		for (Path root : FabricLoader.getInstance().getModContainer("wildercord").orElseThrow().getRootPaths()) {
			Path base = root.resolve("dev/wildercord");
			if (!Files.isDirectory(base)) continue;
			try (Stream<Path> files = Files.walk(base)) {
				for (Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
					String name = root.relativize(file).toString().replace('\\', '/').replace('/', '.');
					name = name.substring(0, name.length() - ".class".length());
					if (name.contains(".mixin.")) continue;
					try {
						Class<?> type = Class.forName(name, false, loader);
						if (type.isRecord() && CustomPacketPayload.class.isAssignableFrom(type)) found.add(type);
					} catch (Throwable ignored) {
						// Client-only or otherwise unloadable classes are not payloads this test can reach.
					}
				}
			} catch (java.io.IOException e) {
				throw new AssertionError("could not scan " + base, e);
			}
		}
		return found;
	}

	@SuppressWarnings("unchecked")
	private static StreamCodec<RegistryFriendlyByteBuf, Object> codecOf(Class<?> type) {
		StreamCodec<RegistryFriendlyByteBuf, Object> any = null;
		for (var field : type.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers()) || !StreamCodec.class.isAssignableFrom(field.getType())) continue;
			try {
				field.setAccessible(true);
				var codec = (StreamCodec<RegistryFriendlyByteBuf, Object>) field.get(null);
				if (field.getName().equals("CODEC") || field.getName().equals("STREAM_CODEC")) return codec;
				any = codec;
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(type.getName() + "." + field.getName(), e);
			}
		}
		return any;
	}

	private static final class Unsupported extends Exception {
		Unsupported(String what) { super(what); }
	}

	/** A value of {@code type} that differs from the defaults, varied by {@code n} so neighbouring fields differ too. */
	private static Object sample(Type type, int n) throws Unsupported {
		if (type instanceof ParameterizedType p && p.getRawType() == List.class) {
			Type element = p.getActualTypeArguments()[0];
			return List.of(sample(element, n + 1), sample(element, n + 2));
		}
		if (!(type instanceof Class<?> k)) throw new Unsupported(type.getTypeName());
		if (k == int.class || k == Integer.class) return 3 + n;
		if (k == long.class || k == Long.class) return 1_234_567_890_123L + n;
		if (k == boolean.class || k == Boolean.class) return true;
		if (k == float.class || k == Float.class) return 1.5F + n;
		if (k == double.class || k == Double.class) return 2.25 + n;
		if (k == String.class) return "s" + n;
		if (k == UUID.class) return new UUID(0x1234L, 0x5678L + n);
		if (k == Vec3.class) return new Vec3(1.5 + n, 64.25, -3.75);
		if (k == BlockPos.class) return new BlockPos(3 + n, 64, -5);
		if (k == Component.class) return Component.literal("c" + n);
		if (k.isRecord()) {
			RecordComponent[] parts = k.getRecordComponents();
			Object[] values = new Object[parts.length];
			Class<?>[] types = new Class<?>[parts.length];
			for (int i = 0; i < parts.length; i++) {
				values[i] = sample(parts[i].getGenericType(), n + i);
				types[i] = parts[i].getType();
			}
			try {
				var constructor = k.getDeclaredConstructor(types);
				constructor.setAccessible(true);
				return constructor.newInstance(values);
			} catch (ReflectiveOperationException e) {
				throw new Unsupported(k.getSimpleName() + " constructor: " + e.getCause());
			}
		}
		throw new Unsupported(k.getSimpleName());
	}
}
