package dev.wildercord.wildlife;

import java.util.*;

/** Immutable search geometry and one mob's finite cursor. Never retains worlds, entities or chunk references. */
public final class HabitatSweep {
	public record Offset(int x,int z) {}
	private static final List<Offset> FOOD=columns(4), COVER=columns(6);
	private final List<Offset> offsets;
	private final int radius;
	private int cursor,x,y,z;
	private boolean anchored;
	public HabitatSweep(int radius) {
		if(radius!=4 && radius!=6)throw new IllegalArgumentException("Habitat radius must be four or six");
		this.radius=radius;offsets=radius==4?FOOD:COVER;
	}
	private static List<Offset> columns(int radius) {
		var positions=new ArrayList<Offset>();
		for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)positions.add(new Offset(x,z));
		positions.sort(Comparator.comparingInt((Offset o) -> o.x*o.x+o.z*o.z).thenComparingInt(Offset::x).thenComparingInt(Offset::z));
		return List.copyOf(positions);
	}
	/** Keep the origin during a sweep so ordinary wandering cannot repeatedly restart the nearest cells. */
	public void anchor(int px,int py,int pz) {
		long dx=(long)px-x,dz=(long)pz-z;
		if(!anchored || cursor==offsets.size() || dx*dx+dz*dz>radius*radius || Math.abs((long)py-y)>1) {
			x=px;y=py;z=pz;cursor=0;anchored=true;
		}
	}
	public Offset next() {return cursor<offsets.size()?offsets.get(cursor++):null;}
	public int x() {return x;}
	public int y() {return y;}
	public int z() {return z;}
	public int count() {return offsets.size();}
}
