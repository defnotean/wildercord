package dev.wildercord.aura.world;

/** Village cadence and finite event bookkeeping, independent of the game runtime. */
public final class TournamentRules {
	private TournamentRules(){}
	public static final int ROUNDS=3, WINDOW=24000, PERIOD=72000, LEDGER=128;
	public static boolean open(long now,long begins,long ends){return begins>=0 && now>=begins && now<ends;}
	public static int method(long site,long event,int round){return Math.floorMod(Math.floorMod(site,10)+Math.floorMod(event/PERIOD,10)+round*3,10);}
	public static boolean award(int wins,boolean won,boolean magic,boolean knockout){return wins==ROUNDS-1 && won && !magic && knockout;}
}
