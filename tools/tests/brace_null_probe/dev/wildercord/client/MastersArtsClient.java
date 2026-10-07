package dev.wildercord.client;
public class MastersArtsClient {
	public record Accepted(int entity, int move, long startTick, int windup, int recovery) {}
	public static Accepted accepted = new Accepted(7, 24, 100, 6, 16);
	public static Accepted timeline(Object player) { return accepted; }
}
