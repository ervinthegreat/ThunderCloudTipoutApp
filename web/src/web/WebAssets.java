package web;

import java.util.HashMap;
import java.util.Map;

import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.Int32Array;

import platform.io.AssetSource;
import platform.io.Bitmap;

/**
 * Browsers can only fetch files asynchronously, so everything the engine will ask for
 * is fetched up front by preload(); loadImage/loadText then answer from memory.
 */
public class WebAssets implements AssetSource {
	private final Map<String, Bitmap> images = new HashMap<>();
	private final Map<String, String> texts = new HashMap<>();
	private int pending;
	private Runnable onReady;

	public void preload(String[] imagePaths, String[] textPaths, Runnable ready) {
		onReady = ready;
		pending = imagePaths.length + textPaths.length;
		if (pending == 0) {
			ready.run();
			return;
		}
		for (String path : imagePaths) fetchImage(path);
		for (String path : textPaths) fetchText(path);
	}

	private void fetchImage(String path) {
		HTMLDocument doc = HTMLDocument.current();
		HTMLImageElement img = (HTMLImageElement) doc.createElement("img");
		img.addEventListener("load", e -> {
			images.put(path, decode(img));
			finishOne();
		});
		img.addEventListener("error", e -> {
			System.err.println("Couldn't load image: " + path);
			finishOne();
		});
		img.setSrc(path);
	}

	private static Bitmap decode(HTMLImageElement img) {
		int w = img.getNaturalWidth();
		int h = img.getNaturalHeight();
		HTMLCanvasElement canvas = (HTMLCanvasElement) HTMLDocument.current().createElement("canvas");
		canvas.setWidth(w);
		canvas.setHeight(h);
		CanvasRenderingContext2D ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
		ctx.drawImage(img, 0, 0);
		ImageData data = ctx.getImageData(0, 0, w, h);
		Int32Array src = new Int32Array(data.getData().getBuffer());
		int[] pixels = new int[w * h];
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = WebColor.argbToAbgr(src.get(i));
		}
		return new Bitmap(w, h, pixels);
	}

	private void fetchText(String path) {
		XMLHttpRequest xhr = new XMLHttpRequest();
		xhr.open("GET", path);
		xhr.onComplete(() -> {
			int status = xhr.getStatus();
			if (status == 200 || status == 0) texts.put(path, xhr.getResponseText());
			else System.err.println("Couldn't load file: " + path + " (" + status + ")");
			finishOne();
		});
		xhr.send();
	}

	private void finishOne() {
		if (--pending == 0 && onReady != null) onReady.run();
	}

	@Override
	public Bitmap loadImage(String path) {return images.get(path);}

	@Override
	public String loadText(String path) {return texts.get(path);}
}
