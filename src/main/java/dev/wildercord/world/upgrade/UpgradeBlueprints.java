package dev.wildercord.world.upgrade;

import java.util.*;

/** Pure reviewed layouts. Guard = (2r+1)^2 columns from a natural foundation (dy=-1) through empty headroom (dy=0..top). */
public final class UpgradeBlueprints {
	private UpgradeBlueprints() {}
	/** Codes: M mossy stone bricks, L stripped oak log, B stone bricks, S smooth stone, C mossy cobblestone, K cobblestone, D coarse dirt, A the family anchor. */
	public record Blueprint(String family,int version,int radius,int top,int writes,UpgradePlan.Point anchor) {
		public int cells(){return (2*radius+1)*(2*radius+1)*(top+2);}
		public char at(int x,int y,int z){return layout(family,x,y,z);}
	}
	public static final List<Blueprint> ALL=List.of(
		new Blueprint(UpgradeCatalog.PAVILION,1,4,7,123,null),
		new Blueprint(UpgradeCatalog.SLEEPING_BLADE,1,3,4,38,new UpgradePlan.Point(0,1,0)),
		new Blueprint(UpgradeCatalog.BATTLEFIELD,1,4,3,58,new UpgradePlan.Point(0,1,0)),
		// The reliquary faces north; its arena (8 south) is (0,1,3), inside the guard with three empty cells above the floor.
		new Blueprint(UpgradeCatalog.SWORD_TOMB,1,6,4,182,new UpgradePlan.Point(0,1,-5)));
	public static Optional<Blueprint> of(String family){return ALL.stream().filter(b->b.family().equals(family)).findFirst();}
	static char layout(String family,int x,int y,int z) {
		int ax=Math.abs(x),az=Math.abs(z);
		switch(family) {
			case UpgradeCatalog.SLEEPING_BLADE -> {
				if(y==0 && ax<=2 && az<=2)return 'M';
				if(y>=0 && y<=2 && ax==3 && az==3)return 'C';
				if(y==1 && x==0 && z==0)return 'A';
			}
			case UpgradeCatalog.BATTLEFIELD -> {
				if(y==0 && ax<=3 && az<=3)return ax==3 || az==3?'K':'D';
				if(y>=0 && y<=1 && (ax==4 && z==0 || az==4 && x==0))return 'C';
				if(y==1 && x==0 && z==0)return 'A';
			}
			case UpgradeCatalog.SWORD_TOMB -> {
				if(y==0)return 'B';
				if(y>=1 && y<=3 && ax==6 && az==6)return 'C';
				if(y==1 && x==0 && z==-5)return 'A';
			}
			default -> {
				if(!family.equals(UpgradeCatalog.PAVILION))throw new IllegalArgumentException("No reviewed blueprint: "+family);
				if(y==0 && ax<=3 && az<=3)return 'M';
				if(y>=1 && y<=4 && ax==2 && az==2)return 'L';
				if(y==5 && ax<=3 && az<=3)return ax==3 || az==3?'B':'S';
				if(y==6 && ax<=1 && az<=1)return 'S';
			}
		}
		return ' ';
	}
}
