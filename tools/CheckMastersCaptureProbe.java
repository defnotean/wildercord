import dev.wildercord.gametest.MastersCaptureProbe;
import java.util.Arrays;
import javax.imageio.ImageIO;
import java.nio.file.Path;

/** Standalone, read-only regressions for the native capture's framebuffer rejection policy. */
public final class CheckMastersCaptureProbe {
	public static void main(String[] args) throws Exception {
		int width = 128, height = 72;
		int[] pixels = new int[width * height];
		Arrays.fill(pixels, 0xff777777);
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, true, "empty hands"));
		Arrays.fill(pixels, width * 60, width * 61, 0xff33ddbb);
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, true, "HUD icon only"));
		Arrays.fill(pixels, width * 35, width * 35 + 12, 0xff33ddbb);
		MastersCaptureProbe.assertPixels(width, height, pixels, true, "visible blade");
		MastersCaptureProbe.assertPixels(width, height, pixels, true, "blade inside its projected bounds",
			new MastersCaptureProbe.BladeBounds(0, .45, .1, .55));
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, true, "cyan outside projected blade",
			new MastersCaptureProbe.BladeBounds(.5, .1, .9, .3)));
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, true, "missing native blade bounds", null));
		Arrays.fill(pixels, 0xffffa022);
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, false, "fire-engulfed body"));
		Arrays.fill(pixels, 0xfffff5d0);
		reject(() -> MastersCaptureProbe.assertPixels(width, height, pixels, false, "pale fire core"));
		for (String argument : args) {
			boolean first = argument.startsWith("first:");
			Path path = Path.of(argument.substring(argument.indexOf(':') + 1));
			var image = ImageIO.read(path.toFile());
			int[] actual = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
			reject(() -> MastersCaptureProbe.assertPixels(image.getWidth(), image.getHeight(), actual, first, path.toString()));
		}
		System.out.println("Native capture pixel regressions passed, including " + args.length + " unchanged rejected native frames.");
	}
	private static void reject(Runnable action) {
		boolean rejected = false;
		try { action.run(); } catch (AssertionError expected) { rejected = true; }
		if (!rejected) throw new AssertionError("An unusable frame was accepted");
	}
}
