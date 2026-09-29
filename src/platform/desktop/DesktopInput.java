package platform.desktop;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import platform.input.Keyboard;
import platform.input.Mouse;

/** Feeds AWT keyboard/mouse events into Keyboard and Mouse. When grabbed, the cursor is hidden and recentered. */
public class DesktopInput {

	private static int lastX = -1;
	private static int lastY = -1;
	private static Component componentRef;
	private static Cursor blankCursor;
	private static Cursor defaultCursor;
	private static Robot robot;

	public static void install(Component component) {
		if (component == null) return;
		componentRef = component;
		installKeyboard(component);
		installMouse(component);
	}

	private static void installKeyboard(Component component) {
		component.setFocusable(true);
		component.requestFocusInWindow();
		component.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {Keyboard.press(e.getKeyCode());}
			@Override
			public void keyReleased(KeyEvent e) {Keyboard.release(e.getKeyCode());}
		});
	}

	private static void installMouse(Component component) {
		defaultCursor = component.getCursor();
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		img.setRGB(0, 0, 0x00FFFFFF);
		blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(0, 0), "blank");
		try {
			robot = new Robot();
		} catch (AWTException e) {
			robot = null;
		}

		Mouse.setGrabListener(grabbed -> {
			if (grabbed) {
				componentRef.setCursor(blankCursor != null ? blankCursor : defaultCursor);
				recenter();
			} else {
				componentRef.setCursor(defaultCursor);
				lastX = -1;
				lastY = -1;
			}
		});

		MouseAdapter adapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				component.requestFocusInWindow();
				if (e.getButton() == MouseEvent.BUTTON1)
					Mouse.setGrabbed(true);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1)
					Mouse.setGrabbed(true);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				move(e.getX(), e.getY());
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				if (Mouse.isGrabbed())
					move(e.getX(), e.getY());
			}
		};
		component.addMouseListener(adapter);
		component.addMouseMotionListener(adapter);
	}

	private static void move(int x, int y) {
		if (lastX >= 0) Mouse.addDelta(x - lastX, y - lastY);
		lastX = x;
		lastY = y;
		if (Mouse.isGrabbed())
			recenter();
	}

	private static void recenter() {
		if (componentRef == null || robot == null) return;
		try {
			Point loc = componentRef.getLocationOnScreen();
			int cx = componentRef.getWidth() / 2;
			int cy = componentRef.getHeight() / 2;
			robot.mouseMove(loc.x + cx, loc.y + cy);
			lastX = cx;
			lastY = cy;
		} catch (Exception ignored) {}
	}
}
