package dev.wildercord.world.upgrade;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UpgradeClaimsTest {
	@Test void absentUnknownErrorsAndClaimedAreDistinctAndNeverOverridden() {
		assertEquals(UpgradeClaims.Status.NO_PROVIDER_CONFIGURED,UpgradeClaims.query(null,null));
		var status=new UpgradeClaims.Status[]{UpgradeClaims.Status.UNKNOWN};boolean[] error={false},metadataError={false};String[] revision={"1"};
		var provider=new UpgradeClaims.Provider() {
			public String id(){return "fixture-provider";}
			public String revision(){if(metadataError[0])throw new IllegalStateException("metadata unavailable");return revision[0];}
			public UpgradeClaims.Status query(net.minecraft.server.level.ServerLevel level,UpgradePlan plan){if(error[0])throw new IllegalStateException("provider unavailable");return status[0];}
		};
		String absent=UpgradeClaims.fingerprint();UpgradeClaims.register(provider);
		try {
			assertTrue(UpgradeClaims.configured());assertNotEquals(absent,UpgradeClaims.fingerprint());
			assertEquals(UpgradeClaims.Status.UNKNOWN,UpgradeClaims.query(null,null));
			status[0]=UpgradeClaims.Status.CLEAR;assertEquals(UpgradeClaims.Status.CLEAR,UpgradeClaims.query(null,null));
			error[0]=true;assertEquals(UpgradeClaims.Status.UNKNOWN,UpgradeClaims.query(null,null));error[0]=false;
			status[0]=UpgradeClaims.Status.CLAIMED;assertEquals(UpgradeClaims.Status.CLAIMED,UpgradeClaims.query(null,null));
			status[0]=UpgradeClaims.Status.NO_PROVIDER_CONFIGURED;assertEquals(UpgradeClaims.Status.UNKNOWN,UpgradeClaims.query(null,null),"A configured adapter cannot impersonate missing-provider mode");
			String old=UpgradeClaims.fingerprint();revision[0]="2";assertNotEquals(old,UpgradeClaims.fingerprint());
			metadataError[0]=true;assertThrows(IllegalStateException.class,UpgradeClaims::fingerprint);assertEquals(UpgradeClaims.Status.UNKNOWN,UpgradeClaims.query(null,null));
			metadataError[0]=false;revision[0]="";assertThrows(IllegalStateException.class,UpgradeClaims::fingerprint);assertEquals(UpgradeClaims.Status.UNKNOWN,UpgradeClaims.query(null,null));
		}finally{UpgradeClaims.unregister(provider.id());}
		assertFalse(UpgradeClaims.configured());assertNotEquals(absent,UpgradeClaims.fingerprint(),"Removing and restoring provider configuration still advances policy epoch");
	}
}
