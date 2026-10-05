package dev.wildercord.world.upgrade;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Exact immutable region snapshot. Site identity deliberately does not contain the blueprint version. */
public record UpgradePlan(long seed, String dimension, String family, int version, int chunkX, int chunkZ,
		List<Cell> cells) {
	public static final int MAX_CELLS = 1024, MAX_WRITES = 256;
	public record Point(int x, int y, int z) {}
	/** Unchanged guard cells carry equal before/after states and are never written. */
	public record Cell(Point point, String before, String after) {
		public boolean changes() { return !before.equals(after); }
	}

	public UpgradePlan {
		Objects.requireNonNull(dimension); Objects.requireNonNull(family);
		if (dimension.length() > 128 || family.length() > 128 || version < 1 || cells.isEmpty() || cells.size() > MAX_CELLS)
			throw new IllegalArgumentException("Invalid bounded plan");
		cells = List.copyOf(cells);
		var unique = new HashSet<Point>();
		int writes = 0;
		for (var c : cells) {
			if (!unique.add(c.point()) || Math.floorDiv(c.point().x(),16) != chunkX || Math.floorDiv(c.point().z(),16) != chunkZ
					|| c.before().isEmpty() || c.after().isEmpty() || c.before().length() > 512 || c.after().length() > 512)
				throw new IllegalArgumentException("Duplicate, unbounded, or invalid cell");
			if (c.changes() && ++writes > MAX_WRITES) throw new IllegalArgumentException("Write budget exceeded");
		}
		if (writes == 0) throw new IllegalArgumentException("Empty write plan");
	}

	public List<Cell> writes() { return cells.stream().filter(Cell::changes).toList(); }
	public String siteId() {
		return digest((seed + "\n" + dimension + "\n" + family + "\n" + chunkX + "\n" + chunkZ).getBytes(StandardCharsets.UTF_8));
	}
	public String hash() {
		try { var bytes = new ByteArrayOutputStream(); write(new DataOutputStream(bytes)); return digest(bytes.toByteArray()); }
		catch (IOException impossible) { throw new UncheckedIOException(impossible); }
	}
	static String digest(byte[] bytes) {
		try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
		catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
	}
	void write(DataOutput out) throws IOException {
		out.writeLong(seed); out.writeUTF(dimension); out.writeUTF(family); out.writeInt(version);
		out.writeInt(chunkX); out.writeInt(chunkZ); out.writeInt(cells.size());
		for (var c : cells) { writePoint(out,c.point()); out.writeUTF(c.before()); out.writeUTF(c.after()); }
	}
	static void writePoint(DataOutput out,Point p) throws IOException { out.writeInt(p.x()); out.writeInt(p.y()); out.writeInt(p.z()); }
	static Point readPoint(DataInput in) throws IOException { return new Point(in.readInt(),in.readInt(),in.readInt()); }
	static UpgradePlan read(DataInput in) throws IOException {
		long seed=in.readLong(); String dimension=in.readUTF(),family=in.readUTF();
		int version=in.readInt(),x=in.readInt(),z=in.readInt(),count=in.readInt();
		if(count<1 || count>MAX_CELLS)throw new IOException("Invalid plan size");
		var cells=new ArrayList<Cell>();
		for(int i=0;i<count;i++)cells.add(new Cell(readPoint(in),in.readUTF(),in.readUTF()));
		try { return new UpgradePlan(seed,dimension,family,version,x,z,cells); }
		catch(IllegalArgumentException ex) { throw new IOException("Invalid plan",ex); }
	}
}
