package web;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.events.Touch;
import org.teavm.jso.dom.events.TouchEvent;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import platform.input.Keyboard;
import platform.input.Mouse;

/** Touch drag and mouse drag feed Mouse deltas; keyboard feeds Keyboard (for desktop browsers). */
public class WebInput {
	private static int touchId = -1;
	private static double lastX, lastY;
	private static boolean mouseDown;

	public static void install(HTMLElement surface) {
		Mouse.setGrabbed(true);

		surface.addEventListener("touchstart", evt -> {
			TouchEvent e = (TouchEvent) evt;
			e.preventDefault();
			if (touchId >= 0) return;
			Touch t = e.getChangedTouches().get(0);
			touchId = t.getIdentifier();
			lastX = t.getClientX();
			lastY = t.getClientY();
		});
		surface.addEventListener("touchmove", evt -> {
			TouchEvent e = (TouchEvent) evt;
			e.preventDefault();
			for (int i = 0; i < e.getChangedTouches().getLength(); i++) {
				Touch t = e.getChangedTouches().get(i);
				if (t.getIdentifier() != touchId) continue;
				Mouse.addDelta((float) (t.getClientX() - lastX), (float) (t.getClientY() - lastY));
				lastX = t.getClientX();
				lastY = t.getClientY();
			}
		});
		surface.addEventListener("touchend", evt -> endTouch((TouchEvent) evt));
		surface.addEventListener("touchcancel", evt -> endTouch((TouchEvent) evt));

		surface.addEventListener("mousedown", evt -> {
			MouseEvent e = (MouseEvent) evt;
			mouseDown = true;
			lastX = e.getClientX();
			lastY = e.getClientY();
		});
		Window window = Window.current();
		window.addEventListener("mousemove", evt -> {
			if (!mouseDown) return;
			MouseEvent e = (MouseEvent) evt;
			Mouse.addDelta((float) (e.getClientX() - lastX), (float) (e.getClientY() - lastY));
			lastX = e.getClientX();
			lastY = e.getClientY();
		});
		window.addEventListener("mouseup", evt -> mouseDown = false);

		HTMLDocument doc = HTMLDocument.current();
		doc.addEventListener("keydown", evt -> Keyboard.press(((KeyboardEvent) evt).getKeyCode()));
		doc.addEventListener("keyup", evt -> Keyboard.release(((KeyboardEvent) evt).getKeyCode()));
		window.addEventListener("blur", evt -> Keyboard.releaseAll());
	}

	private static void endTouch(TouchEvent e) {
		for (int i = 0; i < e.getChangedTouches().getLength(); i++) {
			if (e.getChangedTouches().get(i).getIdentifier() == touchId) touchId = -1;
		}
	}
}
