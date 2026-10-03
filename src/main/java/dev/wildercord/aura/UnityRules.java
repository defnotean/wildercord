package dev.wildercord.aura;

/** Temporary, lossy exchange; neither a gain nor an unpaid activation can feed the other pool. */
public final class UnityRules {
	private UnityRules() {}
	public static final int CIRCLES=5, STAGE=4, DURATION=240, REST=2400;
	public static final float MANA_PRICE=12, AURA_PRICE=12, MANA_CAP=24, AURA_CAP=12;
	public static final double MANA_TO_AURA=.25, AURA_TO_MANA=.5;
	public record State(long since,long until,long readyAt,float manaReturned,float auraReturned) {
		public static final State NONE=new State(-1,0,0,0,0);
		public State {
			manaReturned=bounded(manaReturned,MANA_CAP); auraReturned=bounded(auraReturned,AURA_CAP);
		}
		public boolean active(long now) { return since>=0 && now>=since && until>since && until-since<=DURATION && now<until; }
		public State stop() { return new State(since,since,readyAt,manaReturned,auraReturned); }
		public State returned(float mana,float aura) { return new State(since,until,readyAt,manaReturned+mana,auraReturned+aura); }
	}
	private static float bounded(float value,float cap) { return Float.isFinite(value)?Math.max(0,Math.min(cap,value)):cap; }
	public static State begin(long now) {
		return now<0 || now>Long.MAX_VALUE-REST ? State.NONE : new State(now,now+DURATION,now+REST,0,0);
	}
	public static float convert(double paid,double share,float used,float cap,double room) {
		if (!Double.isFinite(paid) || paid<=0 || !Double.isFinite(room) || room<=0 || !Float.isFinite(used)) return 0;
		return (float)Math.max(0,Math.min(room,Math.min(cap-used,paid*share)));
	}
}
