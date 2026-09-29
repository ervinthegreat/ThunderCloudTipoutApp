package platform.desktop;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import platform.io.AssetSource;
import platform.io.Bitmap;

/** Loads assets from the working directory (the project root when launched from Eclipse). */
public class DesktopAssets implements AssetSource {

	@Override
	public Bitmap loadImage(String path) {
		try {
			BufferedImage img = ImageIO.read(new File(path));
			if (img == null) return null;
			int w = img.getWidth();
			int h = img.getHeight();
			return new Bitmap(w, h, img.getRGB(0, 0, w, h, null, 0, w));
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public String loadText(String path) {
		try {
			return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return null;
		}
	}
}
