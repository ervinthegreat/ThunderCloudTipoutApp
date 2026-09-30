package web;

import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.events.Touch;
import org.teavm.jso.dom.events.TouchEvent;
import org.teavm.jso.dom.html.HTMLElement;

import ui.UiLayer;

/**
 * Delivers taps on the UI surface to the UI straight from the browser event, so the UI can focus a
 * text field (and open the phone keyboard) inside the user gesture.
 */
public class WebInput {
	public static void install(HTMLElement surface, UiLayer ui) {
		surface.addEventListener("touchstart", evt -> evt.preventDefault());
		surface.addEventListener("touchmove", evt -> evt.preventDefault());
		surface.addEventListener("touchend", evt -> {
			TouchEvent e = (TouchEvent) evt;
			e.preventDefault();
			Touch t = e.getChangedTouches().get(0);
			ui.tap((float) t.getClientX(), (float) t.getClientY());
		});
		surface.addEventListener("mousedown", evt -> {
			MouseEvent e = (MouseEvent) evt;
			e.preventDefault();
			ui.tap((float) e.getClientX(), (float) e.getClientY());
		});
	}
}
