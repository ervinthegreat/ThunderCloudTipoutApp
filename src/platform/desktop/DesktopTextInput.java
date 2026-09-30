package platform.desktop;

import java.util.ArrayList;
import java.util.List;

import ui.TextField;
import ui.TextInputHost;

/** Types into the focused TextField from the physical keyboard. Call only from the render thread. */
public class DesktopTextInput implements TextInputHost {
	private static final int MAX_LENGTH = 40;

	private final List<TextField> onScreen = new ArrayList<>();
	private TextField focused;

	@Override
	public void sync(List<TextField> fields) {
		onScreen.clear();
		onScreen.addAll(fields);
		if (focused != null && !onScreen.contains(focused)) blurAll();
	}

	@Override
	public void focus(TextField field) {
		if (focused != null) focused.focused = false;
		focused = field;
		field.focused = true;
	}

	@Override
	public void blurAll() {
		if (focused != null) focused.focused = false;
		focused = null;
	}

	public void typed(char ch) {
		if (focused == null) return;
		TextField f = focused;
		if (ch == '\b') {
			if (!f.text.isEmpty()) f.text = f.text.substring(0, f.text.length() - 1);
		} else if (ch == '\n' || ch == '\r' || ch == '\t') {
			if (f.onEnter != null) f.onEnter.run();
		} else if (f.text.length() < MAX_LENGTH && accepts(f.kind, ch)) {
			f.text += ch;
		}
	}

	private static boolean accepts(TextField.Kind kind, char ch) {
		if (kind == TextField.Kind.MONEY) return Character.isDigit(ch) || ch == '.' || ch == ',' || ch == '$';
		return ch >= ' ' && ch != 127;
	}
}
