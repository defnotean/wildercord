package dev.wildercord.spell;

import java.util.List;

/** Shared circle rules: compiler, renderer and server use the same bounded profile. */
public final class CircleDisciplines {
	private CircleDisciplines() {}
	public enum Design { NEEDLE, BLOOM, GYRE, ANCHOR, RESERVOIR, CRUCIBLE,
		CONFLUENCE, PILGRIM, VIGIL, MERCY, TEMPEST, ECLIPSE }
	public record Profile(Design design, double radius, double speed, double power, double duration) {}
	public static boolean isCircle(RuneDef rune) { return rune.family()==RuneFamily.MODIFIER && rune.needs().equals(Trait.CIRCLE); }
	public static RuneDef selected(List<RuneDef> runes) { return runes.stream().filter(CircleDisciplines::isCircle).findFirst().orElse(null); }
	public static Profile profile(SpellPlan.Group group) { return profile(selected(group.shapeMods)); }
	public static Profile profile(RuneDef rune) {
		if(rune==null) return new Profile(Design.RESERVOIR,1,1,1,1);
		return switch(rune.path()) {
			case "needle_circle" -> new Profile(Design.NEEDLE,.65,1,1.2,1);
			case "bloom_circle" -> new Profile(Design.BLOOM,1.35,1,.8,1);
			case "gyre_circle" -> new Profile(Design.GYRE,1,1.3,.85,1);
			case "anchor_circle" -> new Profile(Design.ANCHOR,1,1,.85,1.4);
			case "reservoir_circle" -> new Profile(Design.RESERVOIR,1,1,.8,.85);
			case "crucible_circle" -> new Profile(Design.CRUCIBLE,1,1,1.15,.75);
			case "confluence_circle" -> new Profile(Design.CONFLUENCE,1,1,1,1);
			case "pilgrim_circle" -> new Profile(Design.PILGRIM,1,1,1,.9);
			case "vigil_circle" -> new Profile(Design.VIGIL,1,.85,1,1);
			case "mercy_circle" -> new Profile(Design.MERCY,1,1,1,1);
			case "tempest_circle" -> new Profile(Design.TEMPEST,1,1.1,1,1);
			case "eclipse_circle" -> new Profile(Design.ECLIPSE,1,1,1,1);
			default -> new Profile(Design.RESERVOIR,1,1,1,1);
		};
	}
	/** Conditions are sampled at group release, rather than re-evaluated by a lingering hit. */
	public static double conditional(SpellPlan.Group group, boolean moving, boolean crouching, boolean wet, boolean night) {
		RuneDef rune=selected(group.shapeMods); if(rune==null)return 1;
		return switch(rune.path()) {
			case "confluence_circle" -> Math.min(1.3,.9+.08*VisualElements.of(group.effects.stream().map(e->e.effect).toList()).stream().distinct().count());
			case "pilgrim_circle" -> moving?1.15:.9;
			case "vigil_circle" -> crouching?1.2:.9;
			case "tempest_circle" -> wet?1.2:.9;
			case "eclipse_circle" -> night?1.2:.9;
			default -> 1;
		};
	}
	public static double role(SpellPlan.Group group, EffectKind kind) {
		RuneDef rune=selected(group.shapeMods);
		return rune!=null && rune.path().equals("mercy_circle") ? (kind==EffectKind.HELPFUL?1.2:.75):1;
	}
	/** Plain spells also get shape-specific layouts, with no hidden gameplay bonus. */
	public static Design design(List<RuneDef> runes) {
		String shape="self"; boolean started=false;
		for(RuneDef rune:runes) {
			if(rune.family()==RuneFamily.LINK)break;
			if(rune.family()==RuneFamily.SHAPE) { if(started)break;shape=rune.path();started=true; }
			else if(rune.family()==RuneFamily.EFFECT)started=true;
			else if(started && isCircle(rune))return profile(rune).design();
		}
		return switch(shape) {
			case "bolt","ray","lance","spark" -> Design.NEEDLE;
			case "burst","nova","zone" -> Design.BLOOM;
			case "vortex","orbit","ricochet","blitz" -> Design.GYRE;
			case "wall","snare","latch","imprint" -> Design.ANCHOR;
			case "self","totem","domain" -> Design.RESERVOIR;
			case "beam","stream","pillar" -> Design.CRUCIBLE;
			case "prism","cluster","constellation" -> Design.CONFLUENCE;
			case "trail","comet","wisp" -> Design.PILGRIM;
			case "mine","touch" -> Design.VIGIL;
			case "crescent","sweep","glaive" -> Design.MERCY;
			case "rain","arc","barrage" -> Design.TEMPEST;
			default -> Design.ECLIPSE;
		};
	}
}
