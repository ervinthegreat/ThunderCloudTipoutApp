package web;

import org.teavm.jso.canvas.CanvasRenderingContext2D;

import ui.UiCanvas;

/** UiCanvas on an HTML canvas 2D context, using the phone's system font. */
public class CanvasUiCanvas implements UiCanvas {
	private static final String FONT_FAMILY =
			"px -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif";

	private final CanvasRenderingContext2D ctx;

	public CanvasUiCanvas(CanvasRenderingContext2D ctx) {
		this.ctx = ctx;
	}

	private static String css(int argb) {
		int a = (argb >>> 24) & 0xFF;
		return "rgba(" + ((argb >> 16) & 0xFF) + "," + ((argb >> 8) & 0xFF) + "," + (argb & 0xFF) + ","
				+ (a / 255.0) + ")";
	}

	private void font(float size, boolean bold) {
		ctx.setFont((bold ? "700 " : "400 ") + size + FONT_FAMILY);
	}

	private void roundRectPath(float x, float y, float w, float h, float r) {
		r = Math.min(r, Math.min(w, h) * 0.5f);
		ctx.beginPath();
		ctx.moveTo(x + r, y);
		ctx.arcTo(x + w, y, x + w, y + h, r);
		ctx.arcTo(x + w, y + h, x, y + h, r);
		ctx.arcTo(x, y + h, x, y, r);
		ctx.arcTo(x, y, x + w, y, r);
		ctx.closePath();
	}

	@Override
	public void fillRect(float x, float y, float w, float h, int argb) {
		ctx.setFillStyle(css(argb));
		ctx.fillRect(x, y, w, h);
	}

	@Override
	public void fillRoundRect(float x, float y, float w, float h, float radius, int argb) {
		ctx.setFillStyle(css(argb));
		roundRectPath(x, y, w, h, radius);
		ctx.fill();
	}

	@Override
	public void strokeRoundRect(float x, float y, float w, float h, float radius, float lineWidth, int argb) {
		ctx.setStrokeStyle(css(argb));
		ctx.setLineWidth(lineWidth);
		float inset = lineWidth * 0.5f;
		roundRectPath(x + inset, y + inset, w - lineWidth, h - lineWidth, radius);
		ctx.stroke();
	}

	@Override
	public void drawText(String text, float x, float y, float size, boolean bold, int argb, int align) {
		font(size, bold);
		ctx.setFillStyle(css(argb));
		ctx.setTextBaseline("alphabetic");
		ctx.setTextAlign(align == ALIGN_CENTER ? "center" : align == ALIGN_RIGHT ? "right" : "left");
		ctx.fillText(text, x, y);
	}

	@Override
	public float measureText(String text, float size, boolean bold) {
		font(size, bold);
		return (float) ctx.measureText(text).getWidth();
	}
}
