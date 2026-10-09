package dev.wildercord.spell;

/** An explicitly selected release silhouette for every built-in shape, never an element/category fallback. */
public enum ShapeFormation {
	SELF, TOUCH, BOLT, BEAM, BURST, ZONE, RAIN, ARC, CONE, TRAIL, WALL, ORBIT, RING, PILLAR, WAVE, MINE, TOTEM,
	DOMAIN, CRESCENT, BARRAGE, ORB, BLITZ, SPARK, RAY, NOVA, WISP, COMET, RICOCHET, CLUSTER, LANCE, SWEEP,
	PRISM, STREAM, VORTEX, SNARE, CONSTELLATION, GLAIVE, IMPRINT, LATCH, RELAY, REWEAVE,
	// ---- shapes pack
	FURROW, PLOT, SEEDBED, SHAFT, STAIRWELL, CORRIDOR, SEAM, FACADE, DOME, FOOTING, CANOPY, SHORELINE, PERIMETER, SPIRE, PIT,
	CROSSWAY, LODESEEK, VAULT, LAMPLIT, FISSURE, SPIRAL, ROSETTE, STEPSTONES, CAUSEWAY, HEDGEROW, LATTICE, COLLAPSE, FAN, BOBBER,
	HERD, FELLOWSHIP, SADDLE, PACKBOND, NURSERY, SHOAL, REARGUARD, GRUDGE, SENTINEL, AUREOLE, TETHER, FLOCK;
	public static ShapeFormation of(String path) {
		try { return valueOf(path.substring(path.indexOf(':') + 1).toUpperCase(java.util.Locale.ROOT)); }
		catch (IllegalArgumentException e) { return BOLT; }
	}
}
