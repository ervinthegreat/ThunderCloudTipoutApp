package main;

import java.awt.GraphicsEnvironment;

import javax.swing.JFrame;

import app.TipApp;
import platform.desktop.DesktopAssets;
import platform.desktop.DesktopClock;
import platform.desktop.DesktopInput;
import platform.desktop.DesktopTextInput;
import platform.desktop.Screen;
import platform.display.DisplayManager;
import platform.io.Assets;

public class Main {

	   /** iPhone XR screen in CSS pixels, so the desktop window previews exactly what the phone renders. */
	   private static final int PREVIEW_WIDTH = 414;
	   private static final int PREVIEW_HEIGHT = 896;
	   /** Room for the title bar and taskbar when shrinking the window to fit the monitor. */
	   private static final int WINDOW_CHROME = 60;

	   public static void main(String[] args) {
		   int width = PREVIEW_WIDTH, height = PREVIEW_HEIGHT;
		   int usableHeight = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
		   float zoom = Math.min(1f, (float) (usableHeight - WINDOW_CHROME) / height);

		   Assets.setSource(new DesktopAssets());
		   DisplayManager.createDisplay(width, height, DisplayManager.DEFAULT_SCALE);
		   Screen screen = new Screen(width, height, zoom, DisplayManager.getFramebuffer());
		   DisplayManager.setDisplay(screen);
		   DesktopTextInput textInput = new DesktopTextInput();
		   TipApp app = new TipApp(textInput, new DesktopClock());
		   DisplayManager.setUiLayer(app);

		   JFrame jframe = new JFrame();
		   jframe.setTitle("ThunderCloud Tips");
		   jframe.setIgnoreRepaint(true);
		   jframe.add(screen);
		   jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		   jframe.setResizable(false);
		   jframe.pack();
		   jframe.setLocationRelativeTo(null);
		   jframe.setVisible(true);
		   DesktopInput.install(screen, zoom, app, textInput);

		   Engine.init();
		   long tickNanos = (long) (Engine.TICK_SECONDS * 1_000_000_000L);
		   long nextFrame = System.nanoTime();
		   while (true) {
			   DesktopInput.drainEvents();
			   Engine.update();
			   Engine.render();
			   nextFrame += tickNanos;
			   long waitNanos = nextFrame - System.nanoTime();
			   if (waitNanos > 0) {
				   try {Thread.sleep(waitNanos / 1_000_000, (int) (waitNanos % 1_000_000));}
				   catch (InterruptedException e) {e.printStackTrace();}
			   } else {
				   nextFrame = System.nanoTime();
			   }
		   }
	   }
}
