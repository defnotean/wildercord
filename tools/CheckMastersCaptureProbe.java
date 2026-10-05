import dev.wildercord.gametest.MastersCaptureProbe;
import java.util.Arrays;
import javax.imageio.ImageIO;
import java.nio.file.Path;

/** Standalone, read-only regressions for the native capture's framebuffer rejection policy. */
public final class CheckMastersCaptureProbe {
	private static final MastersCaptureProbe.BladeBounds VIEW = new MastersCaptureProbe.BladeBounds(0, 0, 1, 1);
	public static void main(String[] args) throws Exception {
		int width = 128, height = 72;
		var hud = new MastersCaptureProbe.HudLayout(64, 36, 28);
		int[] pixels = new int[width * height];
		Arrays.fill(pixels, 0xff777777);
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, true, "empty hands", VIEW, hud));
		Arrays.fill(pixels, width * 60, width * 61, 0xff33ddbb);
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, true, "HUD icon only", VIEW, hud));
		Arrays.fill(pixels, width * 35, width * 35 + 32, 0xff33ddbb);
		MastersCaptureProbe.assertPixels(width, height, pixels, true, "visible blade", VIEW, hud);
		MastersCaptureProbe.assertPixels(width, height, pixels, true, "blade inside its projected bounds",
			new MastersCaptureProbe.BladeBounds(0, .45, .25, .55), hud);
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, true, "cyan outside projected blade",
			new MastersCaptureProbe.BladeBounds(.5, .1, .9, .3), hud));
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, true, "missing native blade bounds", null, hud));
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, true, "missing native HUD bounds", VIEW, null));
		Arrays.fill(pixels, 0xff777777);
		Arrays.fill(pixels, width * 55, width * 55 + 32, 0xff33ddbb);
		MastersCaptureProbe.assertPixels(width, height, pixels, true, "readable blade below 75% but above actual HUD", VIEW, hud);
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, true, "same blade under a larger GUI HUD", VIEW,
			new MastersCaptureProbe.HudLayout(32, 18, 13)));
		Arrays.fill(pixels, 0xff777777);
		Arrays.fill(pixels, width * 35, width * 35 + 12, 0xff33ddbb);
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, true, "small isolated crossguard fragment", VIEW, hud));
		Arrays.fill(pixels, width * 35 + 80, width * 35 + 92, 0xff33ddbb);
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, true, "disconnected fragments cannot create a blade span", VIEW, hud));
		Arrays.fill(pixels, 0xffffa022);
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, false, "fire-engulfed body", null, hud));
		Arrays.fill(pixels, 0xfffff5d0);
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, false, "pale fire core", null, hud));
		for (String argument : args) {
			// For previously captured frames without metadata, the caller supplies the independently
			// verified GUI scale. This checks pixel policy only, not the missing historical submit bounds.
			String[] parts = argument.split(":", 3);
			boolean visible = parts[0].equals("visible"), first = !parts[0].equals("body");
			int scale = Integer.parseInt(parts[1]);
			Path path = Path.of(parts[2]);
			var image = ImageIO.read(path.toFile());
			int[] actual = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
			var frameHud = new MastersCaptureProbe.HudLayout(image.getWidth() / scale, image.getHeight() / scale, image.getHeight() / scale - 39);
			Runnable assertion = () -> MastersCaptureProbe.assertPixels(image.getWidth(), image.getHeight(), actual, first, path.toString(), VIEW, frameHud);
			if (visible) assertion.run(); else reject(assertion);
			System.out.println(path.getFileName() + " " + MastersCaptureProbe.measurePixels(image.getWidth(), image.getHeight(), actual, VIEW, frameHud));
		}
		System.out.println("Native capture pixel-policy regressions passed, including " + args.length + " unchanged native frames. No historical projection or native rerun claim.");
	}
	private static void reject(Runnable action) {
		boolean rejected = false;
		try { action.run(); } catch (AssertionError expected) { rejected = true; }
		if (!rejected) throw new AssertionError("An unusable frame was accepted");
	}
}
