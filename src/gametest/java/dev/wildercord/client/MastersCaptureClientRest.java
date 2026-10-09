package dev.wildercord.client;

/** Test-only: the client's string reader, predicted rests and engagement lapse as they would over a long idle. */
public final class MastersCaptureClientRest {
	private MastersCaptureClientRest() {}

	public static void restNow() { SwordStringsClient.reset(); }
}
