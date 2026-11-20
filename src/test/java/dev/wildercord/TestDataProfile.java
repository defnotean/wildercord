package dev.wildercord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads {@code minecraft_version} from {@code gradle.properties} so data tests validate the active MC schema. */
public final class TestDataProfile {
	public static final String MC_VERSION = readMcVersion();
	private static final boolean IS_26_2 = MC_VERSION.equals("26.2") || MC_VERSION.startsWith("26.2.");

	private TestDataProfile() {}

	public static boolean is26_2() {
		return IS_26_2;
	}

	public static String featureDirName() {
		return IS_26_2 ? "configured_feature" : "feature";
	}

	private static String readMcVersion() {
		try {
			for (String raw : Files.readAllLines(Path.of("gradle.properties"))) {
				String line = raw.trim();
				if (line.startsWith("minecraft_version=")) {
					return line.substring("minecraft_version=".length()).trim();
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot read gradle.properties", e);
		}
		throw new IllegalStateException("minecraft_version not found in gradle.properties");
	}
}
