package web;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import app.TipApp;
import main.Engine;
import platform.display.DisplayManager;
import platform.graphics.objects.FrameBuffer;
import platform.io.Assets;

/** Browser entry point, compiled to JavaScript by TeaVM. */
public class WebMain {
	private static final String[] IMAGES = {};
	private static final String[] TEXTS = modelPaths();
	private static final int DEFAULT_SCALE = DisplayManager.DEFAULT_SCALE;
	private static final double TICK_MS = Engine.TICK_SECONDS * 1000.0;

	private static HTMLElement stats;
	/** Minimum time between renders; updates still run every tick so motion speed is unchanged. */
	private static double renderIntervalMs;
	private static double renderAccumulator;
	private static double lastTime = -1;
	private static double accumulator;
	private static double statsTime;
	private static int statsFrames;
	private static double renderMsTotal;

	private static String[] modelPaths() {
		return new String[] {"res/" + Engine.BOLT_MODEL + ".obj"};
	}

	public static void main(String[] args) {
		WebAssets assets = new WebAssets();
		assets.preload(IMAGES, TEXTS, () -> start(assets));
	}

	private static void start(WebAssets assets) {
		Assets.setSource(assets);
		Window window = Window.current();
		HTMLDocument doc = HTMLDocument.current();

		String search = window.getLocation().getSearch();
		if (search == null) search = "";
		boolean kiosk = search.contains("kiosk");

		int width = window.getInnerWidth();
		int height = window.getInnerHeight();
		DisplayManager.createDisplay(width, height, readParam(search, "scale", DEFAULT_SCALE, 1, 16));
		FrameBuffer fb = DisplayManager.getFramebuffer();
		HTMLCanvasElement canvas = (HTMLCanvasElement) doc.getElementById("screen");
		HTMLCanvasElement uiCanvas = (HTMLCanvasElement) doc.getElementById("ui");
		DisplayManager.setDisplay(new CanvasDisplay(canvas, uiCanvas, fb, width, height));

		int fps = readParam(search, "fps", Engine.FRAMES_PER_SECOND, 1, Engine.FRAMES_PER_SECOND);
		renderIntervalMs = fps >= Engine.FRAMES_PER_SECOND ? 0 : 1000.0 / fps;

		if (kiosk) {
			keepScreenAwake();
		} else {
			TipApp app = new TipApp(new WebTextInput(doc.getElementById("fields")), new WebClock());
			DisplayManager.setUiLayer(app);
			WebInput.install(uiCanvas, app);
		}

		if (search.contains("stats")) {
			stats = doc.getElementById("stats");
			stats.getStyle().setProperty("display", "block");
		}
		HTMLElement loading = doc.getElementById("loading");
		if (loading != null) loading.getParentNode().removeChild(loading);

		Engine.init();
		Window.requestAnimationFrame(WebMain::frame);
	}

	/** Integer URL option such as ?scale=4 or ?fps=12, clamped to [min, max]. */
	private static int readParam(String search, String name, int fallback, int min, int max) {
		int at = search.indexOf(name + "=");
		if (at < 0) return fallback;
		int begin = at + name.length() + 1;
		int end = begin;
		while (end < search.length() && Character.isDigit(search.charAt(end))) end++;
		try {
			return Math.max(min, Math.min(max, Integer.parseInt(search.substring(begin, end))));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/** Screen Wake Lock, re-requested whenever the page becomes visible again (the browser drops it when hidden). */
	@JSBody(script = "if (!('wakeLock' in navigator)) return;"
			+ "var request = function () { navigator.wakeLock.request('screen').catch(function () {}); };"
			+ "request();"
			+ "document.addEventListener('visibilitychange', function () {"
			+ "  if (document.visibilityState === 'visible') request();"
			+ "});")
	private static native void keepScreenAwake();

	private static void frame(double now) {
		if (lastTime < 0) {
			lastTime = now;
			statsTime = now;
		}
		double elapsed = Math.min(now - lastTime, 250);
		accumulator += elapsed;
		renderAccumulator += elapsed;
		lastTime = now;

		boolean ticked = false;
		while (accumulator >= TICK_MS) {
			Engine.update();
			accumulator -= TICK_MS;
			ticked = true;
		}
		if (ticked && renderAccumulator >= renderIntervalMs) {
			renderAccumulator = Math.min(renderAccumulator - renderIntervalMs, renderIntervalMs);
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
