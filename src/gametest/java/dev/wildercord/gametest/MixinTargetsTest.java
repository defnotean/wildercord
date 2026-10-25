package dev.wildercord.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Every class the mod's mixins target loads in the real client. Both configs are required with an injector minimum of
 * one, so a mixin whose injection point has moved fails when its target is defined; many targets are never loaded by
 * the other tests (server-only paths, rare screens), so this test defines each of them on purpose. The targets are read
 * from the configs and each mixin's {@code @Mixin} annotation, since Mixin forgets its configs once they are applied.
 */
public final class MixinTargetsTest implements FabricClientGameTest {
	/** Fewer than this means the configs were not read, not that the mod lost its mixins. */
	private static final int MIN_TARGETS = 60;
	private static final List<String> CONFIGS = List.of("wildercord.mixins.json", "wildercord.client.mixins.json");
	private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";

	@Override
	public void runTest(ClientGameTestContext c) {
		List<String> failed = new ArrayList<>();
		int checked = c.computeOnClient(mc -> {
			int n = 0;
			ClassLoader loader = MixinTargetsTest.class.getClassLoader();
			for (String config : CONFIGS) {
				JsonObject json = read(loader, config);
				String pkg = json.get("package").getAsString();
				for (String side : List.of("mixins", "client", "server")) {
					if (!json.has(side)) continue;
					for (var mixin : json.getAsJsonArray(side)) {
						String name = pkg + "." + mixin.getAsString();
						for (String target : targets(loader, name)) {
							try {
								Class.forName(target, false, loader);
							} catch (Throwable t) {
								failed.add(target + " (" + name + "): " + t);
							}
							n++;
						}
					}
				}
			}
			return n;
		});
		if (!failed.isEmpty()) throw new AssertionError("Mixin targets that failed to load:\n" + String.join("\n", failed));
		if (checked < MIN_TARGETS) throw new AssertionError("Only " + checked + " mixin targets were found");
	}

	private static JsonObject read(ClassLoader loader, String resource) {
		try (InputStream in = loader.getResourceAsStream(resource)) {
			if (in == null) throw new AssertionError("Mixin config " + resource + " is missing");
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new AssertionError("Mixin config " + resource + " can't be read", e);
		}
	}

	/** The classes named by a mixin's {@code @Mixin} value and targets, read from its bytes without loading it. */
	private static List<String> targets(ClassLoader loader, String mixin) {
		ClassNode node = new ClassNode();
		try (InputStream in = loader.getResourceAsStream(mixin.replace('.', '/') + ".class")) {
			if (in == null) throw new AssertionError("Mixin class " + mixin + " is missing");
			new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
		} catch (java.io.IOException e) {
			throw new AssertionError("Mixin class " + mixin + " can't be read", e);
		}
		List<String> out = new ArrayList<>();
		List<AnnotationNode> annotations = new ArrayList<>();
		if (node.invisibleAnnotations != null) annotations.addAll(node.invisibleAnnotations);
		if (node.visibleAnnotations != null) annotations.addAll(node.visibleAnnotations);
		for (AnnotationNode a : annotations) {
			if (!MIXIN.equals(a.desc) || a.values == null) continue;
			for (int i = 0; i + 1 < a.values.size(); i += 2) {
				if (!(a.values.get(i + 1) instanceof List<?> list)) continue;
				for (Object v : list) {
					if (v instanceof Type t) out.add(t.getClassName());
					else if (v instanceof String s) out.add(s.replace('/', '.'));
				}
			}
		}
		if (out.isEmpty()) throw new AssertionError("Mixin " + mixin + " names no target");
		return out;
	}
}
