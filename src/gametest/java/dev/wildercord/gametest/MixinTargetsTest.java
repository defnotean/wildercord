package dev.wildercord.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Every class the mod's mixins target loads in the real client. Both configs are required with an injector minimum of
 * one, so a mixin whose injection point has moved fails when its target is defined; many targets are never loaded by
 * the other tests (server-only paths, rare screens), so this test defines each of them on purpose.
 */
public final class MixinTargetsTest implements FabricClientGameTest {
	/** Fewer than this means the configs were not found, not that the mod lost its mixins. */
	private static final int MIN_TARGETS = 60;
	private static final Set<String> MOD_PACKAGES = Set.of("dev.wildercord.mixin.", "dev.wildercord.client.mixin.");

	@Override
	public void runTest(ClientGameTestContext c) {
		List<String> failed = new ArrayList<>();
		int checked = c.computeOnClient(mc -> {
			int n = 0;
			ClassLoader loader = MixinTargetsTest.class.getClassLoader();
			for (var config : Mixins.getConfigs()) {
				IMixinConfig mixins = config.getConfig();
				String pkg = mixins.getMixinPackage();
				if (!MOD_PACKAGES.contains(pkg.endsWith(".") ? pkg : pkg + ".")) continue;
				for (String target : mixins.getTargets()) {
					String name = target.replace('/', '.');
					try {
						Class.forName(name, false, loader);
					} catch (Throwable t) {
						failed.add(name + " (" + mixins.getName() + "): " + t);
					}
					n++;
				}
			}
			return n;
		});
		if (!failed.isEmpty()) throw new AssertionError("Mixin targets that failed to load:\n" + String.join("\n", failed));
		if (checked < MIN_TARGETS) throw new AssertionError("Only " + checked + " mixin targets were found");
	}
}
