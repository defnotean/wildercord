package dev.wildercord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads {@code minecraft_version} from {@code gradle.properties} so data tests validate the active MC schema. */
public final class TestDataProfile {
	public static final String MC_VERSION = readMcVersion();
	private static final boolean BEFORE_26_3 = isBefore263Version(MC_VERSION);

	private TestDataProfile() {}

	public static boolean isBefore26_3() {
		return BEFORE_26_3;
	}

	public static boolean is26_2() {
		return BEFORE_26_3;
	}

	public static String featureDirName() {
		return BEFORE_26_3 ? "configured_feature" : "feature";
	}

	private static boolean isBefore263Version(String version) {
		String[] parts = version.trim().split("\\.");
		int major = Integer.parseInt(parts[0]);
		int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
		return major < 26 || (major == 26 && minor < 3);
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
