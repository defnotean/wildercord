package dev.wildercord.world.upgrade;

import java.io.IOException;
import java.util.*;
import dev.wildercord.world.upgrade.UpgradeJournal.Entry;
import dev.wildercord.world.upgrade.UpgradeJournal.Phase;

/** Small restartable compare-and-set transactions. All external effects are behind the loaded-only world port. */
public final class UpgradeEngine {
	public interface World {
		/** Empty only while loaded, absent players, clear claims and other exclusions, without loading a chunk. */
		String refusal(UpgradePlan plan,boolean rollback);
		String policy();
		String state(UpgradePlan.Point point);
		boolean set(UpgradePlan.Point point,String expected,String replacement);
	}
	public record Result(Phase phase,int writes,String reason,boolean complete) {
		public Result(Phase phase,int writes,String reason){this(phase,writes,reason,false);}
	}
	private final UpgradeJournal journal;
	private final Map<String,Entry> entries=new LinkedHashMap<>();
	private boolean healthy=true;

	public UpgradeEngine(UpgradeJournal journal) throws IOException {
		this.journal=journal;
		for(var entry:journal.loadAll())entries.put(entry.plan().siteId(),entry);
	}
	public Collection<Entry> entries(){return List.copyOf(entries.values());}
	public Optional<Entry> findHash(String hash){return entries.values().stream().filter(e->e.plan().hash().equals(hash)).findFirst();}
	public boolean healthy(){return healthy;}
	public Entry preview(UpgradePlan plan,String policy) throws IOException {
		if(!healthy)throw new IOException("Journal failed; restart and inspect manifests");
		Entry old=entries.get(plan.siteId());
		// A site survives version bumps, rollback, and later previews as a tombstone.
		if(old!=null && old.phase()!=Phase.PREVIEW)throw new IllegalStateException("Site already reserved: "+old.phase());
		if(old==null && entries.size()>=128)throw new IllegalStateException("128-site bounded journal is full");
		var entry=new Entry(plan,Phase.PREVIEW,"","",policy,Set.of(),-1);persist(entry);return entry;
	}
	/** Approval is exact-hash, exact-region, and policy-epoch bound; provenance stays UNKNOWN. */
	public Entry approve(String hash,String operator,String backup,boolean acceptsUnknownHistory,World world) throws IOException {
		Entry entry=required(hash);
		if(entry.phase()!=Phase.PREVIEW)throw new IllegalStateException("Use reauthorize for recovery; no duplicate approval");
		if(!entry.policy().equals(world.policy()))throw new IllegalStateException("Provider configuration changed; preview again");
		admit(entry,world,false,false);
		return authorize(entry,operator,backup,acceptsUnknownHistory,world,entry.policy());
	}
	/** After restart/config changes an operator must review and explicitly reauthorize the unchanged recovery manifest. */
	public Entry reauthorize(String hash,String operator,String backup,boolean acceptsUnknownHistory,World world) throws IOException {
		Entry entry=required(hash);
		if(entry.phase()==Phase.PREVIEW || entry.phase()==Phase.ROLLED_BACK || entry.phase()==Phase.CONFLICT)
			throw new IllegalStateException("This manifest cannot be reauthorized for placement");
		String expectedPolicy=world.policy();
		admit(entry,world,entry.phase()==Phase.ROLLING_BACK,true);
		return authorize(entry,operator,backup,acceptsUnknownHistory,world,expectedPolicy);
	}
	/** Rollback-only authority may be renewed even for conflicted/tombstoned manifests; edited cells still cannot be written. */
	public Entry reauthorizeRollback(String hash,String operator,String backup,World world) throws IOException {
		Entry entry=required(hash);
		if(entry.phase()==Phase.PREVIEW)throw new IllegalStateException("Preview never wrote blocks");
		String expectedPolicy=world.policy();
		String refusal=world.refusal(entry.plan(),true);if(!refusal.isEmpty())throw new IllegalStateException(refusal);
		if(!expectedPolicy.equals(world.policy()))throw new IllegalStateException("Provider policy changed during recovery authorization");
		if(operator.isBlank() || backup.isBlank())throw new IllegalArgumentException("Operator and offline backup reference required");
		Entry next=new Entry(entry.plan(),Phase.ROLLING_BACK,operator,backup,expectedPolicy,entry.edits(),-1);
		persist(next);return next;
	}
	private Entry authorize(Entry entry,String operator,String backup,boolean accepted,World world,String expectedPolicy) throws IOException {
		if(!accepted || operator.isBlank() || backup.isBlank())throw new IllegalArgumentException("Explicit unknown-history approval and offline backup reference required");
		if(!expectedPolicy.equals(world.policy()))throw new IllegalStateException("Provider policy changed during authorization; review again");
		Entry next=new Entry(entry.plan(),entry.phase()==Phase.PREVIEW?Phase.APPROVED:entry.phase(),operator,backup,expectedPolicy,entry.edits(),entry.intent());
		persist(next);return next;
	}
	/** Each call changes at most the supplied budget (production: two blocks). Unload/player approach merely defers. */
	public Result step(String hash,World world,int budget) throws IOException {
		if(budget<1 || budget>2)throw new IllegalArgumentException("Two writes per tick maximum");
		Entry entry=required(hash);
		if(entry.phase()!=Phase.APPROVED && entry.phase()!=Phase.APPLYING && entry.phase()!=Phase.APPLIED)
			return new Result(entry.phase(),0,"Explicit approval required or transaction stopped",true);
		String refusal=world.refusal(entry.plan(),false);
		if(!refusal.isEmpty())return new Result(entry.phase(),0,refusal);
		if(!entry.policy().equals(world.policy()))return new Result(entry.phase(),0,"Provider configuration changed; explicit reauthorization required");
		if(!entry.edits().isEmpty() || !matches(entry.plan(),world,true))return conflict(entry,"Preview changed or later edit recorded");
		int changed=0;
		for(int i=0;i<entry.plan().writes().size();i++) {
			var cell=entry.plan().writes().get(i);
			if(world.state(cell.point()).equals(cell.after()))continue;
			if(changed==budget)break;
			// Intent and original snapshot reach durable storage BEFORE the world mutation.
			entry=entry.withPhase(Phase.APPLYING,i);persist(entry);
			if(!world.refusal(entry.plan(),false).isEmpty())return new Result(entry.phase(),changed,"Deferred before write");
			if(!entry.policy().equals(world.policy()))return new Result(entry.phase(),changed,"Provider configuration changed");
			if(!matches(entry.plan(),world,true))return conflict(entry,"Guard changed before write",changed);
			changed++;
			boolean wrote=world.set(cell.point(),cell.before(),cell.after());
			// Even a false result may follow an actual mutation and a nested external edit.
			entry=entries.get(entry.plan().siteId());
			if(!wrote)return conflict(entry,"Compare-and-set refused",changed);
			if(!entry.edits().isEmpty())return conflict(entry,"Concurrent external edit",changed);
		}
		if(entry.plan().writes().stream().allMatch(c->world.state(c.point()).equals(c.after()))) {
			entry=entry.withPhase(Phase.APPLIED,-1);persist(entry);
		}
		return new Result(entry.phase(),changed,entry.phase()==Phase.APPLIED?"Observed applied; manifest retained for disk-save reconciliation":"Progress",entry.phase()==Phase.APPLIED);
	}
	/** Recovery is explicit; it never recreates a tombstoned site or performs automatic startup placement. */
	public Result rollback(String hash,World world,int budget) throws IOException {
		if(budget<1 || budget>2)throw new IllegalArgumentException("Two writes per tick maximum");
		Entry entry=required(hash);
		if(entry.phase()==Phase.PREVIEW)return new Result(entry.phase(),0,"Nothing authorized to roll back",true);
		String refusal=world.refusal(entry.plan(),true);
		if(!refusal.isEmpty())return new Result(entry.phase(),0,refusal);
		if(!entry.policy().equals(world.policy()))return new Result(entry.phase(),0,"Provider configuration changed; review recovery first");
		int changed=0;boolean conflict=false,remaining=false;
		var writes=entry.plan().writes();
		for(int i=writes.size()-1;i>=0;i--) {
			var cell=writes.get(i);String current=world.state(cell.point());
			if(entry.edits().contains(cell.point()) || !current.equals(cell.before()) && !current.equals(cell.after())){conflict=true;continue;}
			if(current.equals(cell.before()))continue;
			if(changed==budget){remaining=true;continue;}
			entry=entry.withPhase(Phase.ROLLING_BACK,i);persist(entry);
			if(!world.refusal(entry.plan(),true).isEmpty() || !entry.policy().equals(world.policy()))return new Result(entry.phase(),changed,"Rollback deferred");
			changed++;
			boolean wrote=world.set(cell.point(),cell.after(),cell.before());
			entry=entries.get(entry.plan().siteId());
			if(!wrote){conflict=true;continue;}
		}
		if(!remaining){entry=entry.withPhase(conflict?Phase.CONFLICT:Phase.ROLLED_BACK,-1);persist(entry);}
		return new Result(entry.phase(),changed,conflict?"Later edits preserved; manual conflict review required":remaining?"Rollback progress":"Rollback observed; tombstone retained",!remaining);
	}
	/** Called BEFORE an external mutation of a guarded cell. Failed attempts conservatively fence too. */
	public void fence(String dimension,UpgradePlan.Point point) throws IOException {
		for(var entry:List.copyOf(entries.values())) {
			if(!entry.plan().dimension().equals(dimension) || entry.plan().chunkX()!=Math.floorDiv(point.x(),16) || entry.plan().chunkZ()!=Math.floorDiv(point.z(),16) || entry.edits().contains(point)
				|| entry.plan().cells().stream().noneMatch(c->c.point().equals(point)))continue;
			if(!healthy)throw new IOException("Journal unavailable; protected-region edit refused");
			persist(entry.edited(point));
		}
	}
	private void admit(Entry entry,World world,boolean rollback,boolean mixed) {
		String refusal=world.refusal(entry.plan(),rollback);
		if(!refusal.isEmpty())throw new IllegalStateException(refusal);
		if(!entry.edits().isEmpty() || !matches(entry.plan(),world,mixed))throw new IllegalStateException("Preview changed or later edit recorded");
	}
	private boolean matches(UpgradePlan plan,World world,boolean mixed) {
		for(var c:plan.cells()) {
			String current=world.state(c.point());
			if(!current.equals(c.before()) && !(mixed && c.changes() && current.equals(c.after())))return false;
		}
		return true;
	}
	private Entry required(String hash) throws IOException {
		if(!healthy)throw new IOException("Journal failed; writes disabled until verified restart");
		return findHash(hash).orElseThrow(()->new IllegalArgumentException("Unknown full preview hash"));
	}
	private Result conflict(Entry e,String reason) throws IOException {return conflict(e,reason,0);}
	private Result conflict(Entry e,String reason,int writes) throws IOException {Entry next=e.withPhase(Phase.CONFLICT,-1);persist(next);return new Result(next.phase(),writes,reason,true);}
	private void persist(Entry entry) throws IOException {
		try {journal.store(entry);entries.put(entry.plan().siteId(),entry);}
		catch(IOException ex){healthy=false;throw ex;}
	}
}
