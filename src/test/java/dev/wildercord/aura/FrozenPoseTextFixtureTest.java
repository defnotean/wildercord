package dev.wildercord.aura;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/** Mutate transport bytes independently of the pose goldens, including real directory/JAR extras. */
class FrozenPoseTextFixtureTest {
	private static final String DIRECTORY = "fixture.base64";
	@TempDir Path temporary;
	private Path directory;
	private byte[] plain, gzip;
	private FrozenPoseTextFixture.Receipt receipt;

	@BeforeEach
	void fixture() throws Exception {
		plain = new byte[110_000];
		new Random(720_251).nextBytes(plain);
		var compressed = new ByteArrayOutputStream();
		try (var output = new GZIPOutputStream(compressed)) { output.write(plain); }
		gzip = compressed.toByteArray();
		gzip[8] = 2;
		gzip[9] = 3;
		directory = Files.createDirectory(temporary.resolve(DIRECTORY));
		write(gzip);
	}

	@Test
	void readsTheExactBytesFromAnExplodedResourceDirectory() throws Exception {
		assertArrayEquals(plain, load());
	}

	@Test
	void readsJarResourcesWithoutDirectoryEntries() throws Exception {
		try (var loader = loader(jar("valid.jar"))) {
			assertArrayEquals(plain, FrozenPoseTextFixture.load(loader, DIRECTORY, receipt));
		}
	}

	@Test
	void rejectsMissingFirstAndLaterParts() throws Exception {
		Path first = part(0);
		byte[] saved = Files.readAllBytes(first);
		Files.delete(first);
		reject("Missing");
		Files.write(first, saved);
		Files.delete(part(1));
		reject("Missing");
	}

	@Test
	void rejectsRealExtraFilesAndNestedDirectories() throws Exception {
		Path extra = directory.resolve("part-0099.b64");
		Files.writeString(extra, "AAAA");
		reject("Unexpected");
		Files.delete(extra);
		Files.createDirectory(directory.resolve("nested"));
		reject("Unexpected");
	}

	@Test
	void rejectsExtraAndMissingJarEntries() throws Exception {
		Path extra = directory.resolve("unlisted.txt");
		Files.writeString(extra, "unexpected");
		try (var loader = loader(jar("extra.jar"))) {
			assertThrows(IOException.class, () -> FrozenPoseTextFixture.load(loader, DIRECTORY, receipt));
		}
		Files.delete(extra);
		Files.delete(part(1));
		try (var loader = loader(jar("missing.jar"))) {
			assertThrows(IOException.class, () -> FrozenPoseTextFixture.load(loader, DIRECTORY, receipt));
		}
	}

	@Test
	void rejectsDuplicateJarEntryNames() throws Exception {
		// ZIP writers prohibit duplicate names. Patch the equal-length local and central names.
		Path jar = jar("duplicate.jar");
		byte[] bytes = Files.readAllBytes(jar);
		byte[] from = (DIRECTORY + "/" + FrozenPoseTextFixture.partName(1)).getBytes(StandardCharsets.US_ASCII);
		byte[] to = (DIRECTORY + "/" + FrozenPoseTextFixture.partName(0)).getBytes(StandardCharsets.US_ASCII);
		int replacements = 0;
		for (int offset = 0; offset <= bytes.length - from.length; offset++) {
			if (Arrays.equals(bytes, offset, offset + from.length, from, 0, from.length)) {
				System.arraycopy(to, 0, bytes, offset, to.length);
				replacements++;
			}
		}
		assertEquals(2, replacements);
		Files.write(jar, bytes);
		try (var loader = loader(jar)) {
			var error = assertThrows(IOException.class, () -> FrozenPoseTextFixture.load(loader, DIRECTORY, receipt));
			assertTrue(error.getMessage().contains("duplicate"), error.getMessage());
		}
	}

	@Test
	void rejectsDuplicateClasspathRoots() throws Exception {
		try (var loader = new URLClassLoader(new URL[] {temporary.toUri().toURL(), jar("copy.jar").toUri().toURL()}, null)) {
			var error = assertThrows(IOException.class, () -> FrozenPoseTextFixture.load(loader, DIRECTORY, receipt));
			assertTrue(error.getMessage().contains("Duplicate"), error.getMessage());
		}
	}

	@Test
	void rejectsReorderedAndRepeatedPartContents() throws Exception {
		byte[] first = Files.readAllBytes(part(0)), second = Files.readAllBytes(part(1));
		Files.write(part(0), second);
		Files.write(part(1), first);
		reject("SHA-256");
		Files.write(part(0), first);
		reject("SHA-256");
	}

	@Test
	void rejectsOversizedAndTruncatedParts() throws Exception {
		byte[] first = Files.readAllBytes(part(0));
		Files.write(part(0), Arrays.copyOf(first, first.length + 1));
		reject("part size");
		Files.write(part(0), Arrays.copyOf(first, first.length - 1));
		reject("part size");
	}

	@Test
	void rejectsWhitespaceInvalidAlphabetAndInteriorPadding() throws Exception {
		byte[] first = Files.readAllBytes(part(0));
		for (byte value : new byte[] {' ', '\n', '\r', '\t', '-', '_', '=', (byte) 0xff}) {
			byte[] changed = first.clone();
			changed[20] = value;
			Files.write(part(0), changed);
			reject("base64");
		}
	}

	@Test
	void rejectsNoncanonicalPaddingBitsAndMissingPadding() throws Exception {
		Path last = part(2);
		byte[] bytes = Files.readAllBytes(last);
		assertEquals('=', bytes[bytes.length - 1]);
		int index = bytes.length - 2;
		if (bytes[index] == '=') index--;
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
		byte[] changed = bytes.clone();
		changed[index] = (byte) alphabet.charAt(alphabet.indexOf(bytes[index]) | 1);
		Files.write(last, changed);
		reject("Noncanonical");
		Files.write(last, Arrays.copyOf(bytes, bytes.length - 1));
		reject("part size");
	}

	@Test
	void rejectsBothHashMismatches() throws Exception {
		var original = receipt;
		receipt = new FrozenPoseTextFixture.Receipt(gzip.length, "0".repeat(64), plain.length, hash(plain));
		reject("gzip SHA-256");
		receipt = new FrozenPoseTextFixture.Receipt(gzip.length, original.gzipSha256(), plain.length, "0".repeat(64));
		reject("plain SHA-256");
	}

	@Test
	void boundsEncodedDecodedAndDecompressedSizes() throws Exception {
		var original = receipt;
		for (int size : new int[] {19, 8 * 1024 * 1024 + 1, Integer.MAX_VALUE}) {
			receipt = new FrozenPoseTextFixture.Receipt(size, original.gzipSha256(), plain.length, original.plainSha256());
			reject("receipt");
		}
		for (int size : new int[] {-1, 32 * 1024 * 1024 + 1, Integer.MAX_VALUE}) {
			receipt = new FrozenPoseTextFixture.Receipt(gzip.length, original.gzipSha256(), size, original.plainSha256());
			reject("receipt");
		}
		// Same encoded length, different declared decoded length.
		receipt = new FrozenPoseTextFixture.Receipt(gzip.length + 1, original.gzipSha256(), plain.length, original.plainSha256());
		reject("gzip size");
		for (int size : new int[] {plain.length - 1, plain.length + 1}) {
			receipt = new FrozenPoseTextFixture.Receipt(gzip.length, original.gzipSha256(), size, original.plainSha256());
			reject("decompressed size");
		}
	}

	@Test
	void rejectsTrailingBytesAndConcatenatedMembersEvenWithMatchingGzipReceipts() throws Exception {
		byte[] original = gzip.clone();
		write(Arrays.copyOf(original, original.length + 1));
		reject("Trailing data");
		byte[] concatenated = Arrays.copyOf(original, original.length * 2);
		System.arraycopy(original, 0, concatenated, original.length, original.length);
		write(concatenated);
		reject("Trailing data");
	}

	@Test
	void rejectsCorruptTrailerAndNoncanonicalHeaderEvenWithMatchingGzipReceipts() throws Exception {
		byte[] original = gzip.clone();
		gzip[gzip.length - 8] ^= 1;
		write(gzip);
		reject("trailer mismatch");
		original[4] = 1;
		write(original);
		reject("gzip header");
	}

	private void write(byte[] compressed) throws Exception {
		gzip = compressed;
		receipt = new FrozenPoseTextFixture.Receipt(gzip.length, hash(gzip), plain.length, hash(plain));
		try (var files = Files.list(directory)) {
			for (Path file : files.toList()) Files.delete(file);
		}
		byte[] encoded = Base64.getEncoder().encode(gzip);
		for (int index = 0, offset = 0; offset < encoded.length; index++, offset += FrozenPoseTextFixture.PART_BYTES)
			Files.write(part(index), Arrays.copyOfRange(encoded, offset, Math.min(encoded.length, offset + FrozenPoseTextFixture.PART_BYTES)));
	}

	private byte[] load() throws Exception {
		try (var loader = loader(temporary)) { return FrozenPoseTextFixture.load(loader, DIRECTORY, receipt); }
	}

	private void reject(String message) {
		var error = assertThrows(IOException.class, this::load);
		assertTrue(error.getMessage().contains(message), error.getMessage());
	}

	private Path part(int index) { return directory.resolve(FrozenPoseTextFixture.partName(index)); }

	private URLClassLoader loader(Path path) throws Exception {
		return new URLClassLoader(new URL[] {path.toUri().toURL()}, null);
	}

	private Path jar(String name) throws Exception {
		Path path = temporary.resolve(name);
		try (var output = new JarOutputStream(Files.newOutputStream(path)); var files = Files.list(directory)) {
			for (Path file : files.sorted().toList()) {
				output.putNextEntry(new JarEntry(DIRECTORY + "/" + file.getFileName()));
				Files.copy(file, output);
				output.closeEntry();
			}
		}
		return path;
	}

	private static String hash(byte[] bytes) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}
}
