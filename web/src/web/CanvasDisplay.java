package web;

import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.typedarrays.Int32Array;

import platform.display.Display;
import platform.graphics.objects.FrameBuffer;
import ui.UiLayer;

/**
 * Presents the framebuffer on a low-resolution canvas that CSS scales up, and paints the UI on a
 * second canvas overlaid at the screen's full pixel density so text stays sharp.
 */
public class CanvasDisplay implements Display {
	private final CanvasRenderingContext2D ctx;
	private final ImageData imageData;
	private final Int32Array out;

	private final HTMLCanvasElement uiCanvas;
	private final CanvasRenderingContext2D uiCtx;
	private final CanvasUiCanvas uiDraw;
	private final int cssWidth, cssHeight;

	public CanvasDisplay(HTMLCanvasElement canvas, HTMLCanvasElement uiCanvas, FrameBuffer fb,
			int cssWidth, int cssHeight) {
		canvas.setWidth(fb.width);
		canvas.setHeight(fb.height);
		ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
		imageData = ctx.createImageData(fb.width, fb.height);
		out = new Int32Array(imageData.getData().getBuffer());

		this.uiCanvas = uiCanvas;
		this.cssWidth = cssWidth;
		this.cssHeight = cssHeight;
		uiCtx = (CanvasRenderingContext2D) uiCanvas.getContext("2d");
		uiDraw = new CanvasUiCanvas(uiCtx);
	}

	@Override
	public void present(FrameBuffer fb, UiLayer ui) {
		int[] src = fb.pixels;
		Int32Array dst = out;
		for (int i = 0, n = src.length; i < n; i++) {
			dst.set(i, WebColor.argbToAbgr(src[i]));
		}
		ctx.putImageData(imageData, 0, 0);

		double dpr = Window.current().getDevicePixelRatio();
		int w = (int) Math.round(cssWidth * dpr);
		int h = (int) Math.round(cssHeight * dpr);
		if (uiCanvas.getWidth() != w || uiCanvas.getHeight() != h) {
			uiCanvas.setWidth(w);
			uiCanvas.setHeight(h);
		}
		uiCtx.setTransform(1, 0, 0, 1, 0, 0);
		uiCtx.clearRect(0, 0, w, h);
		if (ui == null) return;
		uiCtx.setTransform(dpr, 0, 0, dpr, 0, 0);
		ui.paint(uiDraw, cssWidth, cssHeight);
	}
}
