package main;

import javax.swing.JFrame;

import platform.desktop.DesktopAssets;
import platform.desktop.DesktopInput;
import platform.desktop.Screen;
import platform.display.DisplayManager;
import platform.io.Assets;

public class Main {

	   public static void main(String[] args) {
		   int width = 800, height = 600;
		   Assets.setSource(new DesktopAssets());
		   DisplayManager.createDisplay(width, height, 8);
		   Screen screen = new Screen(width, height, DisplayManager.getFramebuffer());
		   DisplayManager.setDisplay(screen);
		   JFrame jframe = new JFrame();
		   jframe.setTitle("Display");
		   jframe.setIgnoreRepaint(true);
		   jframe.add(screen);
		   jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		   jframe.setResizable(false);
		   jframe.pack();
		   jframe.setLocationRelativeTo(null);
		   jframe.setVisible(true);
		   screen.requestFocusInWindow();
		   DesktopInput.install(screen);

		   Engine.init();
		   long tickMillis = (long) (Engine.TICK_SECONDS * 1000);
		   while (true) {
			   Engine.update();
			   Engine.render();
			   try {Thread.sleep(tickMillis);}
			   catch (InterruptedException e) {e.printStackTrace();}
		   }
	   }
}
