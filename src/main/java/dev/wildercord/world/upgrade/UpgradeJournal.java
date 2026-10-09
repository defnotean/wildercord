package dev.wildercord.world.upgrade;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;

/** Write-ahead recovery manifests, independently forced to disk before world writes. Never a chunk transaction. */
public final class UpgradeJournal {
	private static final int MAGIC=0x57435531, FORMAT=1, MAX_BYTES=2_000_000;
	public enum Phase { PREVIEW, APPROVED, APPLYING, APPLIED, ROLLING_BACK, ROLLED_BACK, CONFLICT }
	public record Entry(UpgradePlan plan,Phase phase,String operator,String backup,String policy,Set<UpgradePlan.Point> edits,int intent) {
		public Entry {
			edits=Set.copyOf(edits);
			if(operator.length()>256 || backup.length()>256 || policy.length()>256 || edits.size()>UpgradePlan.MAX_CELLS || intent < -1 || intent>=plan.writes().size())
				throw new IllegalArgumentException("Invalid manifest");
			if(edits.stream().anyMatch(p -> plan.cells().stream().noneMatch(c -> c.point().equals(p))))throw new IllegalArgumentException("Out of region edit");
		}
		public Entry withPhase(Phase next,int index){return new Entry(plan,next,operator,backup,policy,edits,index);}
		public Entry edited(UpgradePlan.Point p){var next=new HashSet<>(edits);next.add(p);return new Entry(plan,phase,operator,backup,policy,next,intent);}
	}
	private final Path directory;
	public UpgradeJournal(Path directory) throws IOException {
		this.directory=directory;
		Path existing=directory;
		while(!Files.exists(existing))existing=existing.getParent();
		Files.createDirectories(directory);
		// Persist newly created directory entries as well as testing fsync support before authorization.
		for(Path current=directory;;current=current.getParent()) {
			syncDirectory(current);
			if(current.equals(existing))break;
		}
	}
	public Path directory(){return directory;}
	private Path file(String site){if(!site.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid site ID");return directory.resolve(site+".wcu");}
	public Optional<Entry> read(String site) throws IOException {
		Path path=file(site);return Files.exists(path)?Optional.of(decode(path)):Optional.empty();
	}
	public List<Entry> loadAll() throws IOException {
		var result=new ArrayList<Entry>();
		try(var files=Files.newDirectoryStream(directory,"*.wcu")) {
			for(var path:files) {
				if(result.size()>=128)throw new IOException("128-site journal limit; archive worlds, not live tombstones");
				Entry e=decode(path);
				if(!path.equals(file(e.plan().siteId())))throw new IOException("Manifest filename/identity mismatch");
				result.add(e);
			}
		}
		return List.copyOf(result);
	}
	/** Persist both immutable rollback snapshot and current intent, then atomically publish and fsync its directory. */
	public void store(Entry entry) throws IOException {
		var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
		out.writeInt(MAGIC);out.writeInt(FORMAT);entry.plan().write(out);out.writeUTF(entry.plan().hash());
		out.writeUTF(entry.phase().name());out.writeUTF(entry.operator());out.writeUTF(entry.backup());out.writeUTF(entry.policy());out.writeInt(entry.intent());
		var edits=entry.edits().stream().sorted(Comparator.comparingInt(UpgradePlan.Point::x).thenComparingInt(UpgradePlan.Point::y).thenComparingInt(UpgradePlan.Point::z)).toList();
		out.writeInt(edits.size());for(var p:edits)UpgradePlan.writePoint(out,p);out.flush();
		byte[] body=bytes.toByteArray();out.writeUTF(UpgradePlan.digest(body));out.flush();
		Path target=file(entry.plan().siteId()),temp=directory.resolve(entry.plan().siteId()+".tmp");
		try(var channel=FileChannel.open(temp,CREATE,TRUNCATE_EXISTING,WRITE)) {
			ByteBuffer buffer=ByteBuffer.wrap(bytes.toByteArray());while(buffer.hasRemaining())channel.write(buffer);channel.force(true);
		}
		// No non-atomic fallback: an unsupported filesystem disables writes instead of weakening recovery.
		Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);forceDirectory();
	}
	private Entry decode(Path file) throws IOException {
		if(Files.size(file)>MAX_BYTES)throw new IOException("Oversized journal");
		byte[] bytes=Files.readAllBytes(file);var raw=new ByteArrayInputStream(bytes);var in=new DataInputStream(raw);
		try {
			if(in.readInt()!=MAGIC || in.readInt()!=FORMAT)throw new IOException("Unsupported upgrade journal");
			UpgradePlan plan=UpgradePlan.read(in);if(!in.readUTF().equals(plan.hash()))throw new IOException("Plan hash mismatch");
			Phase phase=Phase.valueOf(in.readUTF());String operator=in.readUTF(),backup=in.readUTF(),policy=in.readUTF();int intent=in.readInt(),count=in.readInt();
			if(count<0 || count>UpgradePlan.MAX_CELLS)throw new IOException("Invalid edit count");
			var edits=new HashSet<UpgradePlan.Point>();for(int i=0;i<count;i++)if(!edits.add(UpgradePlan.readPoint(in)))throw new IOException("Duplicate edit");
			int bodySize=bytes.length-raw.available();String checksum=in.readUTF();
			if(raw.available()!=0 || !checksum.equals(UpgradePlan.digest(Arrays.copyOf(bytes,bodySize))))throw new IOException("Journal checksum mismatch");
			return new Entry(plan,phase,operator,backup,policy,edits,intent);
		} catch(IllegalArgumentException ex){throw new IOException("Malformed journal",ex);}
	}
	private void forceDirectory() throws IOException {syncDirectory(directory);}
	private static boolean supportsDirectoryFsync(Path path) {
		return path.getFileSystem().supportedFileAttributeViews().contains("posix");
	}
	private static void syncDirectory(Path path) throws IOException {
		if(!supportsDirectoryFsync(path))return;
		try(var channel=FileChannel.open(path,READ)){channel.force(true);}
		catch(AccessDeniedException | UnsupportedOperationException ignored){}
	}
}
