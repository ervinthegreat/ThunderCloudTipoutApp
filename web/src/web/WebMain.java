package web;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import main.Engine;
import platform.display.DisplayManager;
import platform.graphics.objects.FrameBuffer;
import platform.io.Assets;

/** Browser entry point, compiled to JavaScript by TeaVM. */
public class WebMain {
	private static final String[] IMAGES = {};
	private static final String[] TEXTS = modelPaths();
	private static final int DEFAULT_SCALE = 3;
	private static final double TICK_MS = Engine.TICK_SECONDS * 1000.0;

	private static HTMLElement stats;
	private static double lastTime = -1;
	private static double accumulator;
	private static double statsTime;
	private static int statsFrames;
	private static double renderMsTotal;

	private static String[] modelPaths() {
		String[] paths = new String[1 + Engine.GEM_MODELS.length];
		paths[0] = "res/" + Engine.BOLT_MODEL + ".obj";
		for (int i = 0; i < Engine.GEM_MODELS.length; i++) paths[i + 1] = "res/" + Engine.GEM_MODELS[i] + ".obj";
		return paths;
	}

	public static void main(String[] args) {
		WebAssets assets = new WebAssets();
		assets.preload(IMAGES, TEXTS, () -> start(assets));
	}

	private static void start(WebAssets assets) {
		Assets.setSource(assets);
		Window window = Window.current();
		HTMLDocument doc = HTMLDocument.current();

		DisplayManager.createDisplay(window.getInnerWidth(), window.getInnerHeight(), readScale(window));
		FrameBuffer fb = DisplayManager.getFramebuffer();
		HTMLCanvasElement canvas = (HTMLCanvasElement) doc.getElementById("screen");
		DisplayManager.setDisplay(new CanvasDisplay(canvas, fb));
		WebInput.install(canvas);

		stats = doc.getElementById("stats");
		HTMLElement loading = doc.getElementById("loading");
		if (loading != null) loading.getParentNode().removeChild(loading);

		Engine.init();
		Window.requestAnimationFrame(WebMain::frame);
	}

	/** Framebuffer downscale factor; override with ?scale=N in the URL. */
	private static int readScale(Window window) {
		String search = window.getLocation().getSearch();
		int at = search != null ? search.indexOf("scale=") : -1;
		if (at < 0) return DEFAULT_SCALE;
		int end = at + 6;
		while (end < search.length() && Character.isDigit(search.charAt(end))) end++;
		try {
			return Math.max(1, Math.min(16, Integer.parseInt(search.substring(at + 6, end))));
		} catch (NumberFormatException e) {
			return DEFAULT_SCALE;
		}
	}

	private static void frame(double now) {
		if (lastTime < 0) {
			lastTime = now;
			statsTime = now;
		}
		accumulator += Math.min(now - lastTime, 250);
		lastTime = now;

		boolean ticked = false;
		while (accumulator >= TICK_MS) {
			Engine.update();
			accumulator -= TICK_MS;
			ticked = true;
		}
		if (ticked) {
			double t0 = System.nanoTime();
			Engine.render();
			renderMsTotal += (System.nanoTime() - t0) / 1_000_000.0;
			statsFrames++;
		}

		if (stats != null && now - statsTime >= 500) {
			FrameBuffer fb = DisplayManager.getFramebuffer();
			double fps = statsFrames * 1000.0 / (now - statsTime);
			double ms = statsFrames > 0 ? renderMsTotal / statsFrames : 0;
			stats.setInnerText(fb.width + "x" + fb.height + "  " + Math.round(fps) + " fps  "
					+ Math.round(ms * 10) / 10.0 + " ms");
			statsTime = now;
			statsFrames = 0;
			renderMsTotal = 0;
		}
		Window.requestAnimationFrame(WebMain::frame);
	}
}
