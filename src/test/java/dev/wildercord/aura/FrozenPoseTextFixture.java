package dev.wildercord.aura;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** Test-only transport for the original, immutable gzip bytes, with a closed resource inventory. */
final class FrozenPoseTextFixture {
	static final int PART_BYTES = 65_536;
	private static final int MAX_GZIP_BYTES = 8 * 1024 * 1024;
	private static final int MAX_PLAIN_BYTES = 32 * 1024 * 1024;
	private static final byte[] GZIP_HEADER = HexFormat.of().parseHex("1f8b0800000000000203");

	private FrozenPoseTextFixture() {}

	record Receipt(int gzipBytes, String gzipSha256, int plainBytes, String plainSha256) {}

	static byte[] load(Class<?> owner, String baseline, int gzipBytes, String gzipSha256,
			int plainBytes, String plainSha256) throws IOException {
		return load(owner.getClassLoader(), baseline.substring(1) + ".base64",
			new Receipt(gzipBytes, gzipSha256, plainBytes, plainSha256));
	}

	static byte[] load(ClassLoader loader, String directory, Receipt receipt) throws IOException {
		if (receipt.gzipBytes() < 20 || receipt.gzipBytes() > MAX_GZIP_BYTES
				|| receipt.plainBytes() < 0 || receipt.plainBytes() > MAX_PLAIN_BYTES
				|| !receipt.gzipSha256().matches("[0-9a-f]{64}")
				|| !receipt.plainSha256().matches("[0-9a-f]{64}"))
			throw new IOException("Invalid or oversized frozen fixture receipt");
		int encodedBytes = ((receipt.gzipBytes() + 2) / 3) * 4;
		int count = (encodedBytes + PART_BYTES - 1) / PART_BYTES;
		Set<String> expected = new HashSet<>();
		for (int index = 0; index < count; index++) expected.add(partName(index));
		var locations = loader.getResources(directory + "/" + partName(0));
		if (!locations.hasMoreElements()) throw new IOException("Missing frozen fixture part 0");
		var first = locations.nextElement();
		if (locations.hasMoreElements()) throw new IOException("Duplicate frozen fixture resource roots");
		byte[] encoded;
		if (first.getProtocol().equals("file")) {
			try {
				Path root = Path.of(first.toURI()).getParent();
				Set<String> actual = new HashSet<>();
				try (var files = Files.newDirectoryStream(root)) {
					for (Path file : files) {
						if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
								|| !expected.contains(file.getFileName().toString()))
							throw new IOException("Unexpected frozen fixture entry: " + file.getFileName());
						actual.add(file.getFileName().toString());
					}
				}
				if (!actual.equals(expected)) throw new IOException("Missing frozen fixture parts");
				encoded = readParts(encodedBytes, index -> Files.newInputStream(root.resolve(partName(index))));
			} catch (URISyntaxException exception) {
				throw new IOException("Invalid frozen fixture resource URL", exception);
			}
		} else if (first.getProtocol().equals("jar")) {
			var connection = (JarURLConnection) first.openConnection();
			connection.setUseCaches(false);
			try (var jar = connection.getJarFile()) {
				String prefix = directory + "/";
				Set<String> actual = new HashSet<>();
				var entries = jar.entries();
				while (entries.hasMoreElements()) {
					var entry = entries.nextElement();
					if (!entry.getName().startsWith(prefix) || entry.getName().equals(prefix)) continue;
					String name = entry.getName().substring(prefix.length());
					if (entry.isDirectory() || !expected.contains(name) || !actual.add(name))
						throw new IOException("Unexpected or duplicate frozen fixture entry: " + name);
				}
				if (!actual.equals(expected)) throw new IOException("Missing frozen fixture parts");
				encoded = readParts(encodedBytes, index -> jar.getInputStream(jar.getJarEntry(prefix + partName(index))));
			}
		} else {
			throw new IOException("Unsupported frozen fixture resource protocol: " + first.getProtocol());
		}
		byte[] gzip;
		try {
			gzip = Base64.getDecoder().decode(encoded);
		} catch (IllegalArgumentException exception) {
			throw new IOException("Invalid frozen fixture base64", exception);
		}
		// The basic decoder alone accepts missing padding and nonzero unused bits.
		if (!Arrays.equals(encoded, Base64.getEncoder().encode(gzip)))
			throw new IOException("Noncanonical frozen fixture base64");
		if (gzip.length != receipt.gzipBytes()) throw new IOException("Frozen fixture gzip size mismatch");
		checkHash(gzip, receipt.gzipSha256(), "gzip");
		byte[] plain = inflate(gzip, receipt.plainBytes());
		checkHash(plain, receipt.plainSha256(), "plain");
		return plain;
	}

	static String partName(int index) {
		return String.format(Locale.ROOT, "part-%04d.b64", index);
	}

	@FunctionalInterface
	private interface PartReader { InputStream open(int index) throws IOException; }

	private static byte[] readParts(int encodedBytes, PartReader reader) throws IOException {
		var encoded = new ByteArrayOutputStream(encodedBytes);
		for (int offset = 0, index = 0; offset < encodedBytes; offset += PART_BYTES, index++) {
			int expected = Math.min(PART_BYTES, encodedBytes - offset);
			try (var input = reader.open(index)) {
				byte[] part = input.readNBytes(expected + 1);
				if (part.length != expected) throw new IOException("Frozen fixture part size mismatch: " + index);
				encoded.write(part);
			}
		}
		return encoded.toByteArray();
	}

	/** Require exactly one deterministic gzip member; GZIPInputStream can ignore trailing data. */
	private static byte[] inflate(byte[] gzip, int plainBytes) throws IOException {
		if (!Arrays.equals(GZIP_HEADER, Arrays.copyOf(gzip, GZIP_HEADER.length)))
			throw new IOException("Noncanonical frozen fixture gzip header");
		var inflater = new Inflater(true);
		try {
			inflater.setInput(gzip, GZIP_HEADER.length, gzip.length - GZIP_HEADER.length);
			byte[] plain = new byte[plainBytes + 1];
			int offset = 0;
			while (!inflater.finished()) {
				int count = inflater.inflate(plain, offset, plain.length - offset);
				offset += count;
				if (offset > plainBytes) throw new IOException("Frozen fixture decompressed size exceeds receipt");
				if (count == 0 && !inflater.finished()) throw new IOException("Truncated or invalid frozen fixture deflate stream");
			}
			if (offset != plainBytes) throw new IOException("Frozen fixture decompressed size mismatch");
			if (inflater.getRemaining() != 8) throw new IOException("Trailing data or missing frozen fixture gzip trailer");
			var crc = new CRC32();
			crc.update(plain, 0, offset);
			if (littleEndian(gzip, gzip.length - 8) != crc.getValue()
					|| littleEndian(gzip, gzip.length - 4) != offset)
				throw new IOException("Frozen fixture gzip trailer mismatch");
			return Arrays.copyOf(plain, offset);
		} catch (DataFormatException exception) {
			throw new IOException("Invalid frozen fixture deflate stream", exception);
		} finally {
			inflater.end();
		}
	}

	private static long littleEndian(byte[] bytes, int offset) {
		return (bytes[offset] & 255L) | ((bytes[offset + 1] & 255L) << 8)
			| ((bytes[offset + 2] & 255L) << 16) | ((bytes[offset + 3] & 255L) << 24);
	}

	private static void checkHash(byte[] bytes, String expected, String label) throws IOException {
		try {
			if (!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(expected))
				throw new IOException("Frozen fixture " + label + " SHA-256 mismatch");
		} catch (NoSuchAlgorithmException exception) {
			throw new AssertionError(exception);
		}
	}
}
