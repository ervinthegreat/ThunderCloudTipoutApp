package platform.desktop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import ui.UiCanvas;

/** UiCanvas on Java2D with antialiased shapes and text. */
public class Graphics2DCanvas implements UiCanvas {
	private static final Font REGULAR = new Font("Segoe UI", Font.PLAIN, 12);
	private static final Font BOLD = new Font("Segoe UI", Font.BOLD, 12);

	private Graphics2D g;

	public void begin(Graphics2D graphics) {
		g = graphics;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}

	private static Color color(int argb) {return new Color(argb, true);}

	private static Font font(float size, boolean bold) {return (bold ? BOLD : REGULAR).deriveFont(size);}

	@Override
	public void fillRect(float x, float y, float w, float h, int argb) {
		g.setColor(color(argb));
		g.fill(new java.awt.geom.Rectangle2D.Float(x, y, w, h));
	}

	@Override
	public void fillRoundRect(float x, float y, float w, float h, float radius, int argb) {
		g.setColor(color(argb));
		g.fill(new RoundRectangle2D.Float(x, y, w, h, radius * 2, radius * 2));
	}

	@Override
	public void strokeRoundRect(float x, float y, float w, float h, float radius, float lineWidth, int argb) {
		g.setColor(color(argb));
		g.setStroke(new BasicStroke(lineWidth));
		float inset = lineWidth * 0.5f;
		g.draw(new RoundRectangle2D.Float(x + inset, y + inset, w - lineWidth, h - lineWidth, radius * 2, radius * 2));
	}

	@Override
	public void drawText(String text, float x, float y, float size, boolean bold, int argb, int align) {
		Font f = font(size, bold);
		g.setFont(f);
		g.setColor(color(argb));
		float width = (float) f.getStringBounds(text, g.getFontRenderContext()).getWidth();
		if (align == ALIGN_CENTER) x -= width * 0.5f;
		else if (align == ALIGN_RIGHT) x -= width;
		g.drawString(text, x, y);
	}

	@Override
	public float measureText(String text, float size, boolean bold) {
		return (float) font(size, bold).getStringBounds(text, g.getFontRenderContext()).getWidth();
	}
}
