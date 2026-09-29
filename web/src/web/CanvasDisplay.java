package web;

import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.typedarrays.Int32Array;

import platform.display.Display;
import platform.graphics.objects.FrameBuffer;

/** Presents the framebuffer on an HTML canvas at native framebuffer resolution; CSS scales it up. */
public class CanvasDisplay implements Display {
	private final CanvasRenderingContext2D ctx;
	private final ImageData imageData;
	private final Int32Array out;

	public CanvasDisplay(HTMLCanvasElement canvas, FrameBuffer fb) {
		canvas.setWidth(fb.width);
		canvas.setHeight(fb.height);
		ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
		imageData = ctx.createImageData(fb.width, fb.height);
		out = new Int32Array(imageData.getData().getBuffer());
	}

	@Override
	public void present(FrameBuffer fb) {
		int[] src = fb.pixels;
		Int32Array dst = out;
		for (int i = 0, n = src.length; i < n; i++) {
			dst.set(i, WebColor.argbToAbgr(src[i]));
		}
		ctx.putImageData(imageData, 0, 0);
	}
}
