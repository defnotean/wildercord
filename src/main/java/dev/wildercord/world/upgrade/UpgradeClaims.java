package dev.wildercord.world.upgrade;

import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Read-only claim inventory adapters. Build permissions and operator powers are never claim evidence. */
public final class UpgradeClaims {
	private UpgradeClaims() {}
	public enum Status { CLEAR, CLAIMED, UNKNOWN, NO_PROVIDER_CONFIGURED }
	public interface Provider {
		String id();
		/** Change whenever configuration or authority semantics change. */
		String revision();
		Status query(ServerLevel level,UpgradePlan plan);
	}
	private static final List<Provider> PROVIDERS=new ArrayList<>();
	private static long generation;
	public static synchronized void register(Provider provider) {
		Objects.requireNonNull(provider);
		if(PROVIDERS.stream().anyMatch(p->p.id().equals(provider.id())))throw new IllegalArgumentException("Duplicate claims adapter");
		PROVIDERS.add(provider);generation++;
	}
	/** Provider removal is itself a policy revision and invalidates every existing approval. */
	public static synchronized void unregister(String id) {
		if(PROVIDERS.removeIf(provider->provider.id().equals(id)))generation++;
	}
	public static synchronized String fingerprint() {
		var revisions=new ArrayList<String>();
		for(var provider:PROVIDERS) {
			String id=provider.id(),revision=provider.revision();
			if(id==null || id.isBlank() || revision==null || revision.isBlank())throw new IllegalStateException("Invalid claim-provider identity/revision");
			revisions.add(id+"="+revision);
		}
		Collections.sort(revisions);return generation+":"+revisions;
	}

	public static synchronized boolean configured(){return !PROVIDERS.isEmpty();}
	public static synchronized Status query(ServerLevel level,UpgradePlan plan) {
		if(PROVIDERS.isEmpty())return Status.NO_PROVIDER_CONFIGURED;
		boolean unknown=false;
		for(var provider:PROVIDERS) {
			try {
				if(provider.id()==null || provider.id().isBlank() || provider.revision()==null || provider.revision().isBlank()){unknown=true;continue;}
				Status status=provider.query(level,plan);
				if(status==Status.CLAIMED)return Status.CLAIMED;
				if(status!=Status.CLEAR)unknown=true;
			} catch(RuntimeException e){unknown=true;}
		}
		return unknown?Status.UNKNOWN:Status.CLEAR;
	}
}
