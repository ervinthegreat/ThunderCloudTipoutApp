package web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

import ui.TextField;
import ui.TextInputHost;

/**
 * Places an invisible native <input> exactly over each drawn TextField, so tapping a field opens
 * the phone's own keyboard. The UI draws the text itself; the input only captures typing.
 */
public class WebTextInput implements TextInputHost {
	private final HTMLElement container;
	private final Map<TextField, HTMLInputElement> inputs = new HashMap<>();

	public WebTextInput(HTMLElement container) {
		this.container = container;
	}

	@Override
	public void sync(List<TextField> fields) {
		for (TextField f : fields) {
			HTMLInputElement el = inputFor(f);
			position(el, f);
			if (!f.focused && !el.getValue().equals(f.text)) el.setValue(f.text);
		}
		for (TextField f : new ArrayList<>(inputs.keySet())) {
			if (fields.contains(f)) continue;
			HTMLInputElement el = inputs.remove(f);
			if (f.focused) el.blur();
			f.focused = false;
			container.removeChild(el);
		}
	}

	@Override
	public void focus(TextField field) {
		HTMLInputElement el = inputFor(field);
		position(el, field);
		el.focus();
	}

	@Override
	public void blurAll() {
		HTMLElement active = HTMLDocument.current().getActiveElement();
		if (active != null) active.blur();
		for (TextField f : inputs.keySet()) f.focused = false;
	}

	private HTMLInputElement inputFor(TextField f) {
		HTMLInputElement el = inputs.get(f);
		if (el != null) return el;
		el = (HTMLInputElement) HTMLDocument.current().createElement("input");
		el.setType("text");
		el.setAttribute("autocomplete", "off");
		el.setAttribute("autocorrect", "off");
		el.setAttribute("spellcheck", "false");
		el.setAttribute("enterkeyhint", "next");
		if (f.kind == TextField.Kind.MONEY) {
			el.setAttribute("inputmode", "decimal");
			el.setAttribute("autocapitalize", "off");
		} else {
			el.setAttribute("autocapitalize", "words");
		}
		HTMLInputElement input = el;
		el.addEventListener("input", e -> {
			String value = input.getValue();
			if (f.kind == TextField.Kind.MONEY) {
				String filtered = value.replaceAll("[^0-9.,$]", "");
				if (!filtered.equals(value)) input.setValue(filtered);
				value = filtered;
			}
			f.text = value;
		});
		el.addEventListener("focus", e -> f.focused = true);
		el.addEventListener("blur", e -> {
			f.focused = false;
			resetScroll();
		});
		el.addEventListener("keydown", e -> {
			if (((KeyboardEvent) e).getKeyCode() == 13) {
				e.preventDefault();
				f.text = input.getValue();
				if (f.onEnter != null) f.onEnter.run();
			}
		});
		el.setValue(f.text);
		container.appendChild(el);
		inputs.put(f, el);
		return el;
	}

	private static void position(HTMLInputElement el, TextField f) {
		el.getStyle().setProperty("left", f.x + "px");
		el.getStyle().setProperty("top", f.y + "px");
		el.getStyle().setProperty("width", f.w + "px");
		el.getStyle().setProperty("height", f.h + "px");
	}

	/** iOS can leave the page scrolled after the keyboard closes; the app never scrolls. */
	@JSBody(script = "window.scrollTo(0, 0);")
	private static native void resetScroll();
}
