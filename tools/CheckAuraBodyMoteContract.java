import dev.wildercord.gametest.AuraBodyMoteProbe;
import dev.wildercord.gametest.AuraBodyMoteProbe.Draw;
import dev.wildercord.gametest.AuraBodyMoteProbe.Mode;
import dev.wildercord.gametest.AuraBodyMoteProbe.Receipt;
import dev.wildercord.gametest.AuraBodyMoteProbe.Tick;
import dev.wildercord.gametest.AuraBodyMoteScript;
import java.util.ArrayList;
import java.util.List;

/** Standalone receipt/script controls. These do not replace native Mixin/emitter acceptance. */
public final class CheckAuraBodyMoteContract {
	private static int checks;
	private static void check(boolean condition) { checks++; if (!condition) throw new AssertionError("control " + checks); }
	private static void rejects(Runnable action) {
		checks++;
		try { action.run(); } catch (AssertionError expected) { return; }
		throw new AssertionError("control " + checks + " did not reject");
	}
	private static Tick tick(long time, int emitted, int eligible, boolean paused) {
		return new Tick(time, "THIRD_PERSON_FRONT", paused, false, false, true, 9, "FULL", 1, true, .8F,
			true, 0, 0, false, false, 1, 0, emitted, eligible, emitted);
	}
	private static Draw draw(long time, float sample, int emitted) {
		return new Draw(time, true, sample, .03F + .08F * .8F, emitted, emitted, true,
			emitted == 1 ? "dev.wildercord.client.fx.Glimmer" : "none");
	}
	private static Receipt receipt(Mode mode, List<Tick> ticks, List<Draw> draws, int consumed, boolean exhausted) {
		return new Receipt(mode.name(), 7, "00000000-0000-0000-0000-000000000007", List.copyOf(ticks), List.copyOf(draws), consumed, exhausted, true, List.of());
	}
	private static void validate(Receipt receipt, Mode mode, int ticks) { AuraBodyMoteProbe.validate(receipt, mode, ticks); checks++; }
	public static void main(String[] args) {
		AuraBodyMoteScript script = new AuraBodyMoteScript();
		check(script.nextFloat() == .5F && script.consumed() == 1); // first eligible tick rejects
		check(script.nextFloat() == 0 && script.nextDouble() == .25 && script.nextDouble() == .5);
		check(script.nextInt(14) == 0 && script.nextInt(3) == 1 && script.nextFloat() == .5F && script.nextDouble() == .5);
		check(script.exhausted() && script.consumed() == 8);
		rejects(script::nextFloat); rejects(script::nextDouble); rejects(() -> script.nextInt(14));
		AuraBodyMoteScript wrong = new AuraBodyMoteScript();
		rejects(wrong::nextDouble); rejects(() -> wrong.nextInt(14)); check(wrong.consumed() == 0);
		wrong.nextFloat(); wrong.nextFloat(); wrong.nextDouble(); wrong.nextDouble();
		rejects(() -> wrong.nextInt(13)); check(wrong.consumed() == 4);
		rejects(wrong::fork); rejects(wrong::forkPositional); rejects(() -> wrong.setSeed(1));
		rejects(wrong::nextInt); rejects(wrong::nextLong); rejects(wrong::nextBoolean); rejects(wrong::nextGaussian);
		check(AuraBodyMoteProbe.source(null, wrong) == wrong && wrong.consumed() == 4); // inactive passthrough, no draw
		AuraBodyMoteProbe.room(true); AuraBodyMoteProbe.sample(wrong, .5F); AuraBodyMoteProbe.added(null, null);
		AuraBodyMoteProbe.endMotes(null); AuraBodyMoteProbe.endTick(null, 0);
		check(wrong.consumed() == 4);

		List<Tick> ticks = new ArrayList<>(); List<Draw> draws = new ArrayList<>();
		for (int i = 0; i < 30; i++) { ticks.add(tick(100 + i, 0, 1, false)); draws.add(draw(100 + i, .5F, 0)); }
		validate(receipt(Mode.LIVE, ticks, draws, 0, true), Mode.LIVE, 30); // a valid natural zero sample must pass
		ticks.set(0, tick(100, 1, 1, false)); draws.set(0, draw(100, 0, 1));
		validate(receipt(Mode.LIVE, ticks, draws, 0, true), Mode.LIVE, 30);
		draws.set(0, draw(100, 0, 0)); ticks.set(0, tick(100, 0, 1, false));
		rejects(() -> AuraBodyMoteProbe.validate(receipt(Mode.LIVE, ticks, draws, 0, true), Mode.LIVE, 30)); // missing original add
		draws.set(0, draw(100, .5F, 1)); ticks.set(0, tick(100, 1, 1, false));
		rejects(() -> AuraBodyMoteProbe.validate(receipt(Mode.LIVE, ticks, draws, 0, true), Mode.LIVE, 30)); // spurious add
		draws.set(0, draw(100, .5F, 0)); ticks.set(0, tick(100, 0, 0, false));
		rejects(() -> AuraBodyMoteProbe.validate(receipt(Mode.LIVE, ticks, draws, 0, true), Mode.LIVE, 30)); // hidden ineligibility
		ticks.set(0, tick(100, 0, 1, true));
		rejects(() -> AuraBodyMoteProbe.validate(receipt(Mode.LIVE, ticks, draws, 0, true), Mode.LIVE, 30)); // paused
		ticks.set(0, tick(100, 0, 1, false));
		rejects(() -> AuraBodyMoteProbe.validate(receipt(Mode.LIVE, ticks, draws, 8, true), Mode.LIVE, 30)); // script contamination
		rejects(() -> AuraBodyMoteProbe.validate(receipt(Mode.LIVE, ticks.subList(1, 30), draws, 0, true), Mode.LIVE, 30));
		Receipt branches = receipt(Mode.SCRIPT, List.of(tick(100, 0, 1, false), tick(101, 1, 1, false)),
			List.of(draw(100, .5F, 0), draw(101, 0, 1)), 8, true);
		validate(branches, Mode.SCRIPT, 2);
		rejects(() -> AuraBodyMoteProbe.validate(receipt(Mode.SCRIPT, branches.ticks(), branches.draws(), 7, false), Mode.SCRIPT, 2));
		rejects(() -> AuraBodyMoteProbe.validate(new Receipt(branches.phase(), branches.entityId(), branches.uuid(), branches.ticks(),
			branches.draws(), 8, true, false, List.of()), Mode.SCRIPT, 2));
		Object token = new Object(); int[] lifecycle = new int[3];
		String result = AuraBodyMoteProbe.window(() -> { lifecycle[0]++; return token; }, () -> lifecycle[1]++,
			seen -> { check(seen == token); lifecycle[2]++; return "closed"; }, seen -> check(seen.equals("closed")));
		check(result.equals("closed") && lifecycle[0] == 1 && lifecycle[1] == 1 && lifecycle[2] == 1);
		int[] cleanup = new int[2]; AssertionError injected = new AssertionError("injected native-window failure");
		try {
			AuraBodyMoteProbe.window(() -> token, () -> { throw injected; },
				seen -> { check(seen == token); cleanup[0]++; return "failed-window receipt"; }, seen -> cleanup[1]++);
			throw new IllegalStateException("window failure was hidden");
		} catch (AssertionError failure) { check(failure == injected); }
		check(cleanup[0] == 1 && cleanup[1] == 1);
		rejects(() -> AuraBodyMoteProbe.window(() -> { throw new AssertionError("open failed"); }, () -> {},
			seen -> { throw new IllegalStateException("closed an unopened scope"); }, seen -> {}));
		System.out.println("Aura body mote receipt/script controls passed: " + checks);
	}
}
