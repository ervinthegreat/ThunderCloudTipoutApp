package platform.io;

/** Global access point for the active platform's AssetSource. Set once at startup. */
public class Assets {
	private static AssetSource source;

	public static void setSource(AssetSource assetSource) {source = assetSource;}

	public static Bitmap loadImage(String path) {
		return source != null ? source.loadImage(path) : null;
	}

	public static String loadText(String path) {
		return source != null ? source.loadText(path) : null;
	}
}
