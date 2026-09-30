package platform.desktop;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ConcurrentLinkedQueue;

import platform.input.Keyboard;
import ui.UiLayer;

/**
 * AWT delivers input on its own thread while the render loop runs on main, so events are queued
 * here and applied by drainEvents() on the render thread.
 */
public class DesktopInput {
	private static final ConcurrentLinkedQueue<Runnable> events = new ConcurrentLinkedQueue<>();

	public static void install(Component component, float zoom, UiLayer ui, DesktopTextInput text) {
		component.setFocusable(true);
		component.setFocusTraversalKeysEnabled(false);
		component.requestFocusInWindow();
		component.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				events.add(() -> Keyboard.press(code));
			}

			@Override
			public void keyReleased(KeyEvent e) {
				int code = e.getKeyCode();
				events.add(() -> Keyboard.release(code));
			}

			@Override
			public void keyTyped(KeyEvent e) {
				char ch = e.getKeyChar();
				events.add(() -> text.typed(ch));
			}
		});
		component.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				component.requestFocusInWindow();
				if (e.getButton() != MouseEvent.BUTTON1) return;
				float x = e.getX() / zoom, y = e.getY() / zoom;
				events.add(() -> ui.tap(x, y));
			}
		});
	}

	public static void drainEvents() {
		Runnable r;
		while ((r = events.poll()) != null) r.run();
	}
}
